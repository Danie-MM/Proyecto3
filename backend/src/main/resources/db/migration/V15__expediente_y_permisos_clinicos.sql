-- B-03. El UUID de cuenta es el identificador público estable entregado por A.
CREATE TABLE permiso_clinico (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    mascota_id uuid NOT NULL REFERENCES mascota(id),
    beneficiario_usuario_id uuid NOT NULL REFERENCES usuarios(id_publico),
    fundamento text NOT NULL CHECK (fundamento = 'VETERINARIO_AUTORIZADO'),
    alcance text NOT NULL CHECK (alcance IN ('LECTURA', 'LECTURA_ESCRITURA')),
    otorgado_por_usuario_id uuid NOT NULL REFERENCES usuarios(id_publico),
    creado_en timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version integer NOT NULL DEFAULT 0 CHECK (version >= 0),
    activo boolean NOT NULL DEFAULT true,
    revocado_en timestamptz,
    revocado_por_usuario_id uuid REFERENCES usuarios(id_publico),
    motivo_revocacion text CHECK (char_length(btrim(motivo_revocacion)) BETWEEN 1 AND 500),
    CHECK ((activo AND revocado_en IS NULL AND revocado_por_usuario_id IS NULL AND motivo_revocacion IS NULL)
        OR (NOT activo AND revocado_en IS NOT NULL AND revocado_por_usuario_id IS NOT NULL AND motivo_revocacion IS NOT NULL))
);
CREATE UNIQUE INDEX uq_permiso_clinico_activo ON permiso_clinico(mascota_id, beneficiario_usuario_id) WHERE activo;
CREATE INDEX ix_permiso_clinico_pagina ON permiso_clinico(mascota_id, creado_en DESC, id DESC);

CREATE TABLE evento_veterinario (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    mascota_id uuid NOT NULL REFERENCES mascota(id),
    tipo text NOT NULL CHECK (tipo = 'VACUNA'),
    fecha date NOT NULL,
    nombre text NOT NULL CHECK (char_length(btrim(nombre)) BETWEEN 1 AND 2000),
    descripcion text CHECK (char_length(btrim(descripcion)) BETWEEN 1 AND 2000),
    autor_usuario_id uuid NOT NULL REFERENCES usuarios(id_publico),
    origen text NOT NULL CHECK (origen IN ('DECLARADO_RESPONSABLE','REGISTRADO_VETERINARIO')),
    creado_en timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version integer NOT NULL DEFAULT 0 CHECK (version >= 0),
    numero_revision integer NOT NULL DEFAULT 0 CHECK (numero_revision >= 0)
);
CREATE INDEX ix_evento_veterinario_pagina ON evento_veterinario(mascota_id, fecha DESC, creado_en DESC, id DESC);

-- Se retiene la huella de la solicitud, sin copiar textos clínicos a la bitácora de reenvíos.
CREATE TABLE operacion_clinica (
    actor_id uuid NOT NULL REFERENCES usuarios(id_publico),
    operacion text NOT NULL,
    clave uuid NOT NULL,
    huella bytea NOT NULL,
    recurso_id uuid,
    PRIMARY KEY (actor_id, operacion, clave)
);

CREATE FUNCTION b03_reservar_operacion(p_actor uuid, p_operacion text, p_clave uuid, p_datos jsonb)
RETURNS uuid LANGUAGE plpgsql AS $$
DECLARE
    previa operacion_clinica%ROWTYPE;
    firma bytea := sha256(convert_to(p_datos::text, 'UTF8'));
BEGIN
    INSERT INTO operacion_clinica(actor_id, operacion, clave, huella)
        VALUES (p_actor, p_operacion, p_clave, firma) ON CONFLICT DO NOTHING;
    SELECT * INTO STRICT previa FROM operacion_clinica
        WHERE actor_id = p_actor AND operacion = p_operacion AND clave = p_clave FOR UPDATE;
    IF previa.huella <> firma THEN
        RAISE EXCEPTION 'Clave utilizada con datos diferentes' USING ERRCODE = 'P0410';
    END IF;
    RETURN previa.recurso_id;
END;
$$;

CREATE PROCEDURE create_evento_veterinario_pr(p_mascota uuid, p_actor uuid, p_origen text,
    p_fecha date, p_nombre text, p_descripcion text, p_clave uuid, OUT p_id uuid)
LANGUAGE plpgsql AS $$
BEGIN
    IF p_fecha IS NULL OR p_fecha > (CURRENT_TIMESTAMP AT TIME ZONE 'America/Costa_Rica')::date THEN
        RAISE EXCEPTION 'Fecha inválida' USING ERRCODE = 'P0400';
    END IF;
    p_id := b03_reservar_operacion(p_actor, 'VACUNA', p_clave,
        jsonb_build_array(p_mascota, p_fecha, p_nombre, p_descripcion));
    IF p_id IS NOT NULL THEN RETURN; END IF;
    INSERT INTO evento_veterinario(mascota_id,tipo,fecha,nombre,descripcion,autor_usuario_id,origen)
        VALUES(p_mascota,'VACUNA',p_fecha,p_nombre,p_descripcion,p_actor,p_origen) RETURNING id INTO p_id;
    UPDATE operacion_clinica SET recurso_id = p_id
        WHERE actor_id = p_actor AND operacion = 'VACUNA' AND clave = p_clave;
END;
$$;

CREATE PROCEDURE create_permiso_clinico_pr(p_mascota uuid, p_actor uuid, p_beneficiario uuid,
    p_alcance text, p_clave uuid, OUT p_id uuid, OUT p_cambio boolean)
LANGUAGE plpgsql AS $$
BEGIN
    p_id := b03_reservar_operacion(p_actor, 'CONCESION', p_clave,
        jsonb_build_array(p_mascota,p_beneficiario,p_alcance));
    p_cambio := p_id IS NULL;
    IF NOT p_cambio THEN RETURN; END IF;
    INSERT INTO permiso_clinico(mascota_id,beneficiario_usuario_id,fundamento,alcance,otorgado_por_usuario_id)
        VALUES(p_mascota,p_beneficiario,'VETERINARIO_AUTORIZADO',p_alcance,p_actor)
        ON CONFLICT (mascota_id,beneficiario_usuario_id) WHERE activo DO NOTHING RETURNING id INTO p_id;
    IF p_id IS NULL THEN
        RAISE EXCEPTION 'Ya hay un permiso activo' USING ERRCODE = 'P0411';
    END IF;
    UPDATE operacion_clinica SET recurso_id = p_id
        WHERE actor_id = p_actor AND operacion = 'CONCESION' AND clave = p_clave;
END;
$$;

CREATE PROCEDURE deactivate_permiso_clinico_pr(p_permiso uuid, p_actor uuid, p_motivo text,
    p_version integer, p_clave uuid, OUT p_id uuid, OUT p_cambio boolean)
LANGUAGE plpgsql AS $$
BEGIN
    p_id := b03_reservar_operacion(p_actor, 'REVOCACION', p_clave,
        jsonb_build_array(p_permiso,p_version,p_motivo));
    p_cambio := p_id IS NULL;
    IF NOT p_cambio THEN RETURN; END IF;
    UPDATE permiso_clinico SET activo = false, revocado_en = CURRENT_TIMESTAMP,
        revocado_por_usuario_id = p_actor, motivo_revocacion = p_motivo, version = version + 1
        WHERE id = p_permiso AND activo AND version = p_version RETURNING id INTO p_id;
    IF p_id IS NULL THEN
        RAISE EXCEPTION 'El permiso ya cambió' USING ERRCODE = 'P0409';
    END IF;
    UPDATE operacion_clinica SET recurso_id = p_id
        WHERE actor_id = p_actor AND operacion = 'REVOCACION' AND clave = p_clave;
END;
$$;

CREATE FUNCTION ret_id_evento_veterinario_pr(p_id uuid) RETURNS SETOF evento_veterinario LANGUAGE sql STABLE AS $$
    SELECT * FROM evento_veterinario WHERE id = p_id;
$$;
CREATE FUNCTION ret_id_permiso_clinico_pr(p_id uuid) RETURNS SETOF permiso_clinico LANGUAGE sql STABLE AS $$
    SELECT * FROM permiso_clinico WHERE id = p_id;
$$;
CREATE FUNCTION ret_all_evento_veterinario_pr(p_mascota uuid,p_desde date,p_hasta date,p_tipo text)
RETURNS SETOF evento_veterinario LANGUAGE sql STABLE AS $$
    SELECT * FROM evento_veterinario WHERE mascota_id = p_mascota
        AND (p_desde IS NULL OR fecha >= p_desde) AND (p_hasta IS NULL OR fecha <= p_hasta)
        AND (p_tipo IS NULL OR tipo = p_tipo);
$$;
CREATE FUNCTION ret_all_permiso_clinico_pr(p_mascota uuid,p_solo_activos boolean)
RETURNS SETOF permiso_clinico LANGUAGE sql STABLE AS $$
    SELECT * FROM permiso_clinico WHERE mascota_id = p_mascota AND (NOT p_solo_activos OR activo);
$$;
