-- B-01: completar el perfil privado sin reescribir las migraciones ya aplicadas.
-- A suministra el actor y sus organizaciones vigentes. B comprueba además cada mascota.

-- 1. Una especie pertenece al catálogo; agregar otra especie no requiere cambiar Java.
ALTER TABLE mascota DROP CONSTRAINT mascota_especie_check;
ALTER TABLE mascota ALTER COLUMN especie TYPE varchar(30);
ALTER TABLE mascota ADD CONSTRAINT fk_mascota_especie
    FOREIGN KEY (especie) REFERENCES especie(codigo);
CREATE INDEX ix_mascota_responsable ON mascota(responsable_usuario_id, creado_en DESC, id);
CREATE INDEX ix_mascota_organizacion ON mascota(organizacion_id, creado_en DESC, id);

-- 2. Las claves evitan duplicar un alta o una baja cuando el navegador reintenta.
-- Solo guardamos una huella y una referencia; no copiamos el perfil privado.
CREATE TABLE mascota_operacion_idempotente (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_usuario_id uuid NOT NULL,
    operacion varchar(150) NOT NULL,
    clave uuid NOT NULL,
    huella_solicitud varchar(64) NOT NULL,
    recurso_id uuid REFERENCES mascota(id),
    creado_en timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_mascota_operacion UNIQUE (actor_usuario_id, operacion, clave)
);

CREATE TABLE mascota_desactivacion (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    mascota_id uuid NOT NULL REFERENCES mascota(id),
    actor_usuario_id uuid NOT NULL,
    fecha timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo varchar(500) NOT NULL CHECK (char_length(btrim(motivo)) > 0),
    version_anterior integer NOT NULL,
    version_nueva integer NOT NULL
);

-- 3. La misma condición de acceso se aplica a detalle, lista, edición y reintentos.
CREATE FUNCTION b_mascota_autorizada_pr(
    p_responsable uuid, p_organizacion uuid, p_actor uuid, p_organizaciones uuid[]
) RETURNS boolean LANGUAGE sql IMMUTABLE AS $$
    -- En organizaciones manda la representación vigente, también para el creador.
    SELECT p_actor IS NOT NULL AND CASE
        WHEN p_organizacion IS NULL THEN COALESCE(p_responsable = p_actor, false)
        ELSE COALESCE(p_organizacion = ANY(COALESCE(p_organizaciones, ARRAY[]::uuid[])), false)
    END;
$$;

-- Esta vista es interna: el controlador devuelve un DTO privado, nunca la vista al público.
CREATE VIEW mascota_perfil_privado_v AS
SELECT m.*, e.id AS especie_id, e.nombre AS especie_nombre, e.activa AS especie_activa,
       COALESCE(c.ids, ARRAY[]::uuid[]) AS comportamiento_ids,
       COALESCE(c.nombres, ARRAY[]::text[]) AS comportamiento_nombres,
       COALESCE(c.activas, ARRAY[]::boolean[]) AS comportamiento_activas
FROM mascota m
JOIN especie e ON e.codigo = m.especie
LEFT JOIN LATERAL (
    SELECT array_agg(ec.id ORDER BY ec.id) AS ids,
           array_agg(ec.nombre::text ORDER BY ec.id) AS nombres,
           array_agg(ec.activa ORDER BY ec.id) AS activas
    FROM mascota_comportamiento mc
    JOIN etiqueta_comportamiento ec ON ec.id = mc.etiqueta_id
    WHERE mc.mascota_id = m.id
) c ON true;

CREATE FUNCTION ret_id_mascota_autorizada_pr(p_id uuid, p_actor uuid, p_organizaciones uuid[])
RETURNS SETOF mascota_perfil_privado_v LANGUAGE sql STABLE AS $$
    SELECT m.* FROM mascota_perfil_privado_v m
    WHERE m.id = p_id
      AND b_mascota_autorizada_pr(m.responsable_usuario_id, m.organizacion_id, p_actor, p_organizaciones);
$$;

CREATE FUNCTION ret_all_mascota_pr(
    p_actor uuid, p_organizaciones uuid[], p_organizacion uuid, p_activas boolean,
    p_pagina integer, p_limite integer
) RETURNS SETOF mascota_perfil_privado_v LANGUAGE plpgsql STABLE AS $$
BEGIN
    IF p_pagina IS NULL OR p_pagina < 0 OR p_limite IS NULL OR p_limite < 1 OR p_limite > 100 THEN
        RAISE EXCEPTION 'La página y el límite no son válidos.' USING ERRCODE = 'P0400';
    END IF;
    RETURN QUERY SELECT m.* FROM mascota_perfil_privado_v m
    WHERE b_mascota_autorizada_pr(m.responsable_usuario_id, m.organizacion_id, p_actor, p_organizaciones)
      AND (p_organizacion IS NULL OR m.organizacion_id = p_organizacion)
      AND (p_activas IS NULL OR m.activa = p_activas)
    ORDER BY m.creado_en DESC, m.id ASC
    LIMIT p_limite OFFSET (p_pagina::bigint * p_limite);
END;
$$;

CREATE FUNCTION count_mascota_pr(
    p_actor uuid, p_organizaciones uuid[], p_organizacion uuid, p_activas boolean
) RETURNS bigint LANGUAGE sql STABLE AS $$
    SELECT count(*) FROM mascota m
    WHERE b_mascota_autorizada_pr(m.responsable_usuario_id, m.organizacion_id, p_actor, p_organizaciones)
      AND (p_organizacion IS NULL OR m.organizacion_id = p_organizacion)
      AND (p_activas IS NULL OR m.activa = p_activas);
$$;

CREATE FUNCTION ret_all_especie_pr()
RETURNS SETOF especie LANGUAGE sql STABLE AS $$
    SELECT e.* FROM especie e ORDER BY e.codigo;
$$;

-- 4. La base vuelve a validar: ni una llamada directa puede saltarse el catálogo.
-- Al editar se permiten referencias retiradas que ya pertenecían a esa mascota.
CREATE PROCEDURE b_validar_perfil_mascota_pr(
    p_nombre text, p_especie text, p_edad integer, p_tamano text, p_energia text,
    p_etiquetas uuid[], p_mascota uuid
) LANGUAGE plpgsql AS $$
DECLARE
    v_especie especie%ROWTYPE;
    v_especie_anterior text;
    v_total integer;
BEGIN
    IF p_nombre IS NULL OR char_length(p_nombre) NOT BETWEEN 1 AND 100
       OR p_nombre ~ '^[[:space:]]|[[:space:]]$'
       OR p_edad IS NULL OR p_edad < 0
       OR p_tamano IS NULL OR p_tamano NOT IN ('PEQUENO', 'MEDIANO', 'GRANDE')
       OR p_energia IS NULL OR p_energia NOT IN ('BAJA', 'MEDIA', 'ALTA') THEN
        RAISE EXCEPTION 'Revisá los datos obligatorios del perfil.' USING ERRCODE = 'P0400';
    END IF;
    SELECT * INTO v_especie FROM especie WHERE codigo = p_especie FOR SHARE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'La especie no pertenece al catálogo.' USING ERRCODE = 'P0400';
    END IF;
    SELECT especie INTO v_especie_anterior FROM mascota WHERE id = p_mascota;
    -- Reutilizamos la función escrita en V5; el bloqueo anterior impide retirar
    -- la especie entre esta comprobación y el guardado de la mascota.
    IF NOT existe_especie_activa_pr(p_especie) AND p_especie IS DISTINCT FROM v_especie_anterior THEN
        RAISE EXCEPTION 'La especie elegida ya no está activa.' USING ERRCODE = 'P0400';
    END IF;
    IF p_etiquetas IS NULL OR cardinality(p_etiquetas) = 0
       OR array_position(p_etiquetas, NULL) IS NOT NULL
       OR cardinality(p_etiquetas) <> (SELECT count(DISTINCT x) FROM unnest(p_etiquetas) x) THEN
        RAISE EXCEPTION 'Elegí al menos una etiqueta, sin duplicados.' USING ERRCODE = 'P0400';
    END IF;
    -- Ordenar los bloqueos evita que dos solicitudes tomen etiquetas en orden distinto.
    PERFORM id FROM etiqueta_comportamiento e WHERE e.id = ANY(p_etiquetas)
        AND (e.activa OR EXISTS (
            SELECT 1 FROM mascota_comportamiento mc
            WHERE mc.mascota_id = p_mascota AND mc.etiqueta_id = e.id
        )) ORDER BY id FOR SHARE;
    GET DIAGNOSTICS v_total = ROW_COUNT;
    IF v_total <> cardinality(p_etiquetas) THEN
        RAISE EXCEPTION 'Una etiqueta no existe o ya no admite nuevas asignaciones.' USING ERRCODE = 'P0400';
    END IF;
END;
$$;

-- 5. Alta atómica: clave, mascota y relaciones se confirman juntas o se revierten juntas.
CREATE PROCEDURE create_mascota_privada_pr(
    IN p_actor uuid, IN p_organizaciones uuid[], IN p_clave uuid,
    IN p_nombre text, IN p_especie text, IN p_edad integer, IN p_tamano text,
    IN p_energia text, IN p_etiquetas uuid[], IN p_organizacion uuid, OUT p_id uuid
) LANGUAGE plpgsql AS $$
DECLARE
    v_nombre text := regexp_replace(p_nombre, '^[[:space:]]+|[[:space:]]+$', '', 'g');
    v_huella text;
    v_operacion mascota_operacion_idempotente%ROWTYPE;
BEGIN
    IF p_actor IS NULL OR p_clave IS NULL THEN
        RAISE EXCEPTION 'Falta el actor o la clave de la operación.' USING ERRCODE = 'P0400';
    END IF;
    IF p_organizacion IS NOT NULL AND NOT COALESCE(p_organizacion = ANY(p_organizaciones), false) THEN
        RAISE EXCEPTION 'No encontramos una organización autorizada.' USING ERRCODE = 'P0404';
    END IF;
    -- El orden de los comportamientos no cambia la intención de la solicitud.
    v_huella := encode(sha256(convert_to(jsonb_build_object(
        'nombre', v_nombre, 'especie', p_especie, 'edad', p_edad, 'tamano', p_tamano,
        'energia', p_energia, 'etiquetas', ARRAY(SELECT x FROM unnest(p_etiquetas) x ORDER BY x),
        'organizacion', p_organizacion
    )::text, 'UTF8')), 'hex');
    INSERT INTO mascota_operacion_idempotente(actor_usuario_id, operacion, clave, huella_solicitud)
    VALUES (p_actor, 'MASCOTA_CREAR', p_clave, v_huella) ON CONFLICT DO NOTHING;
    SELECT * INTO v_operacion FROM mascota_operacion_idempotente
        WHERE actor_usuario_id = p_actor AND operacion = 'MASCOTA_CREAR' AND clave = p_clave FOR UPDATE;
    IF v_operacion.recurso_id IS NOT NULL THEN
        -- Repetir una petición no permite recuperar un objeto cuyo permiso se perdió.
        PERFORM 1 FROM mascota m WHERE m.id = v_operacion.recurso_id
            AND b_mascota_autorizada_pr(m.responsable_usuario_id, m.organizacion_id, p_actor, p_organizaciones);
        IF NOT FOUND THEN
            RAISE EXCEPTION 'No encontramos esa mascota.' USING ERRCODE = 'P0404';
        END IF;
        IF v_operacion.huella_solicitud <> v_huella THEN
            RAISE EXCEPTION 'La clave ya fue usada con otros datos.' USING ERRCODE = 'P0410';
        END IF;
        p_id := v_operacion.recurso_id;
        RETURN;
    END IF;
    CALL b_validar_perfil_mascota_pr(v_nombre, p_especie, p_edad, p_tamano, p_energia, p_etiquetas, NULL);
    INSERT INTO mascota(responsable_usuario_id, organizacion_id, nombre, especie, edad_estimada_meses, tamano, energia)
    VALUES (p_actor, p_organizacion, v_nombre, p_especie, p_edad, p_tamano, p_energia)
    RETURNING id INTO p_id;
    INSERT INTO mascota_comportamiento(mascota_id, etiqueta_id)
        SELECT p_id, x FROM unnest(p_etiquetas) x;
    UPDATE mascota_operacion_idempotente SET recurso_id = p_id WHERE id = v_operacion.id;
END;
$$;

-- 6. La edición nunca transfiere dueño ni cambia disponibilidad.
-- La versión evita sobrescribir los cambios hechos desde otra pestaña/persona.
CREATE PROCEDURE update_mascota_privada_pr(
    IN p_id uuid, IN p_actor uuid, IN p_organizaciones uuid[], IN p_nombre text,
    IN p_especie text, IN p_edad integer, IN p_tamano text, IN p_energia text,
    IN p_etiquetas uuid[], IN p_version integer, OUT p_resultado uuid
) LANGUAGE plpgsql AS $$
DECLARE
    v_mascota mascota%ROWTYPE;
    v_nombre text := regexp_replace(p_nombre, '^[[:space:]]+|[[:space:]]+$', '', 'g');
BEGIN
    SELECT * INTO v_mascota FROM mascota m WHERE m.id = p_id
        AND b_mascota_autorizada_pr(m.responsable_usuario_id, m.organizacion_id, p_actor, p_organizaciones)
        FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No encontramos esa mascota.' USING ERRCODE = 'P0404';
    END IF;
    IF p_version IS NULL OR p_version < 0 THEN
        RAISE EXCEPTION 'Indicá una versión válida.' USING ERRCODE = 'P0400';
    END IF;
    IF v_mascota.version <> p_version THEN
        RAISE EXCEPTION 'El perfil cambió. Recargá su versión vigente.' USING ERRCODE = 'P0409';
    END IF;
    IF NOT v_mascota.activa THEN
        RAISE EXCEPTION 'Una mascota desactivada conserva su historial y no admite edición.' USING ERRCODE = 'P0411';
    END IF;
    CALL b_validar_perfil_mascota_pr(v_nombre, p_especie, p_edad, p_tamano, p_energia, p_etiquetas, p_id);
    UPDATE mascota SET nombre = v_nombre, especie = p_especie, edad_estimada_meses = p_edad,
        tamano = p_tamano, energia = p_energia, version = version + 1 WHERE id = p_id;
    -- PUT representa la selección completa. Retirar una etiqueta del catálogo no la
    -- borra automáticamente del perfil; el responsable sí puede quitarla al editar.
    DELETE FROM mascota_comportamiento mc
        WHERE mc.mascota_id = p_id AND NOT (mc.etiqueta_id = ANY(p_etiquetas));
    INSERT INTO mascota_comportamiento(mascota_id, etiqueta_id)
        SELECT p_id, x FROM unnest(p_etiquetas) x ON CONFLICT DO NOTHING;
    p_resultado := p_id;
END;
$$;

-- 7. Desactivación lógica con motivo y auditoría. Ningún expediente se elimina.
CREATE PROCEDURE deactivate_mascota_pr(
    IN p_id uuid, IN p_actor uuid, IN p_organizaciones uuid[], IN p_clave uuid,
    IN p_motivo text, IN p_version integer, OUT p_resultado uuid
) LANGUAGE plpgsql AS $$
DECLARE
    v_mascota mascota%ROWTYPE;
    v_operacion mascota_operacion_idempotente%ROWTYPE;
    v_motivo text := regexp_replace(p_motivo, '^[[:space:]]+|[[:space:]]+$', '', 'g');
    v_huella text;
BEGIN
    SELECT * INTO v_mascota FROM mascota m WHERE m.id = p_id
        AND b_mascota_autorizada_pr(m.responsable_usuario_id, m.organizacion_id, p_actor, p_organizaciones)
        FOR UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'No encontramos esa mascota.' USING ERRCODE = 'P0404';
    END IF;
    IF p_clave IS NULL OR v_motivo IS NULL OR char_length(v_motivo) NOT BETWEEN 1 AND 500
       OR p_version IS NULL OR p_version < 0 THEN
        RAISE EXCEPTION 'Indicá clave, motivo y versión válidos.' USING ERRCODE = 'P0400';
    END IF;
    v_huella := encode(sha256(convert_to(jsonb_build_object(
        'mascota', p_id, 'motivo', v_motivo, 'version', p_version
    )::text, 'UTF8')), 'hex');
    INSERT INTO mascota_operacion_idempotente(actor_usuario_id, operacion, clave, huella_solicitud)
        VALUES (p_actor, 'MASCOTA_DESACTIVAR', p_clave, v_huella) ON CONFLICT DO NOTHING;
    SELECT * INTO v_operacion FROM mascota_operacion_idempotente
        WHERE actor_usuario_id = p_actor AND operacion = 'MASCOTA_DESACTIVAR' AND clave = p_clave FOR UPDATE;
    IF v_operacion.huella_solicitud <> v_huella THEN
        RAISE EXCEPTION 'La clave ya fue usada con otros datos.' USING ERRCODE = 'P0410';
    END IF;
    IF v_operacion.recurso_id IS NOT NULL THEN
        p_resultado := v_operacion.recurso_id;
        RETURN;
    END IF;
    IF v_mascota.version <> p_version THEN
        RAISE EXCEPTION 'El perfil cambió. Recargá su versión vigente.' USING ERRCODE = 'P0409';
    END IF;
    IF NOT v_mascota.activa THEN
        RAISE EXCEPTION 'La mascota ya está desactivada.' USING ERRCODE = 'P0411';
    END IF;
    UPDATE mascota SET activa = false, version = version + 1 WHERE id = p_id;
    INSERT INTO mascota_desactivacion(mascota_id, actor_usuario_id, motivo, version_anterior, version_nueva)
        VALUES (p_id, p_actor, v_motivo, p_version, p_version + 1);
    UPDATE mascota_operacion_idempotente SET recurso_id = p_id WHERE id = v_operacion.id;
    p_resultado := p_id;
END;
$$;

-- Compatibilidad con el primer repositorio individual: usa ahora las mismas validaciones.
CREATE OR REPLACE PROCEDURE create_mascota_pr(
    IN p_actor_id uuid, IN p_nombre text, IN p_especie text, IN p_edad_meses integer,
    IN p_tamano text, IN p_energia text, IN p_etiquetas uuid[], OUT p_id uuid
) LANGUAGE plpgsql AS $$
BEGIN
    CALL create_mascota_privada_pr(p_actor_id, ARRAY[]::uuid[], gen_random_uuid(), p_nombre,
        p_especie, p_edad_meses, p_tamano, p_energia, p_etiquetas, NULL, p_id);
EXCEPTION WHEN SQLSTATE 'P0400' THEN
    -- El método inicial ya traducía las validaciones a violación de integridad.
    RAISE EXCEPTION 'Revisá el perfil y las etiquetas vigentes.' USING ERRCODE = '23514';
END;
$$;
