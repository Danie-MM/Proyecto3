CREATE TABLE mascota (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    creado_en timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version integer NOT NULL DEFAULT 0 CHECK (version >= 0),
    nombre varchar(100) NOT NULL CHECK (char_length(nombre) > 0 AND nombre !~ '^[[:space:]]|[[:space:]]$'),
    especie varchar(5) NOT NULL CHECK (especie IN ('PERRO', 'GATO')),
    edad_estimada_meses integer NOT NULL CHECK (edad_estimada_meses >= 0),
    tamano varchar(7) NOT NULL CHECK (tamano IN ('PEQUENO', 'MEDIANO', 'GRANDE')),
    energia varchar(5) NOT NULL CHECK (energia IN ('BAJA', 'MEDIA', 'ALTA')),
    -- Las FK de estos campos se incorporan al adoptar las tablas de identidad de A.
    responsable_usuario_id uuid NOT NULL,
    organizacion_id uuid,
    disponibilidad varchar(13) NOT NULL DEFAULT 'NO_DISPONIBLE'
        CHECK (disponibilidad IN ('NO_DISPONIBLE', 'DISPONIBLE', 'ADOPTADA')),
    activa boolean NOT NULL DEFAULT true
);

CREATE TABLE mascota_comportamiento (
    mascota_id uuid NOT NULL REFERENCES mascota(id),
    etiqueta_id uuid NOT NULL REFERENCES etiqueta_comportamiento(id),
    PRIMARY KEY (mascota_id, etiqueta_id)
);

-- Alta individual. La capa de servicio deberá resolver el actor desde la sesión de A.
CREATE PROCEDURE create_mascota_pr(
    IN p_actor_id uuid, IN p_nombre text, IN p_especie text, IN p_edad_meses integer,
    IN p_tamano text, IN p_energia text, IN p_etiquetas uuid[], OUT p_id uuid
)
LANGUAGE plpgsql AS $$
DECLARE
    total_vigentes integer;
BEGIN
    -- Un bloqueo compartido evita que se retire una etiqueta entre validación y alta.
    PERFORM id FROM etiqueta_comportamiento
        WHERE activa AND id = ANY(p_etiquetas) ORDER BY id FOR SHARE;
    GET DIAGNOSTICS total_vigentes = ROW_COUNT;
    IF total_vigentes = 0 OR total_vigentes <> cardinality(p_etiquetas) THEN
        RAISE EXCEPTION 'Se requiere al menos una etiqueta vigente y sin duplicados.'
            USING ERRCODE = '23514';
    END IF;

    INSERT INTO mascota (responsable_usuario_id, nombre, especie, edad_estimada_meses, tamano, energia)
    VALUES (p_actor_id, regexp_replace(p_nombre, '^[[:space:]]+|[[:space:]]+$', '', 'g'),
            p_especie, p_edad_meses, p_tamano, p_energia)
    RETURNING id INTO p_id;

    INSERT INTO mascota_comportamiento (mascota_id, etiqueta_id)
        SELECT p_id, etiqueta FROM unnest(p_etiquetas) AS etiqueta;
END;
$$;

-- Perfil privado individual. La consulta ajena devuelve el mismo resultado que un ID inexistente.
CREATE FUNCTION ret_id_mascota_pr(p_mascota_id uuid, p_actor_id uuid)
RETURNS TABLE (
    id uuid, creado_en timestamptz, version integer, nombre varchar, especie varchar,
    edad_estimada_meses integer, tamano varchar, energia varchar, responsable_usuario_id uuid,
    organizacion_id uuid, disponibilidad varchar, activa boolean,
    comportamiento_ids uuid[], comportamientos jsonb
)
LANGUAGE sql STABLE AS $$
    SELECT m.id, m.creado_en, m.version, m.nombre, m.especie, m.edad_estimada_meses,
           m.tamano, m.energia, m.responsable_usuario_id, m.organizacion_id, m.disponibilidad,
           m.activa, array_agg(e.id ORDER BY e.id),
           jsonb_agg(jsonb_build_object('id', e.id, 'nombre', e.nombre) ORDER BY e.id)
    FROM mascota m
    JOIN mascota_comportamiento mc ON mc.mascota_id = m.id
    JOIN etiqueta_comportamiento e ON e.id = mc.etiqueta_id
    WHERE m.id = p_mascota_id AND m.responsable_usuario_id = p_actor_id
      AND m.organizacion_id IS NULL
    GROUP BY m.id;
$$;
