-- PostgreSQL guarda los bytes en el mismo volumen privado
-- que el perfil. Reemplazo, retiro, versión y clave se confirman juntos.
CREATE TABLE archivo_mascota (
    mascota_id uuid NOT NULL REFERENCES mascota(id),
    id uuid PRIMARY KEY,
    clave_almacenamiento uuid NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    subida_por_usuario_id uuid NOT NULL,
    sha256 bytea NOT NULL CHECK (octet_length(sha256)=32),
    activa boolean NOT NULL DEFAULT true,
    creada_en timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mime_verificado varchar(10) NOT NULL CHECK (mime_verificado IN ('image/jpeg', 'image/png')),
    contenido bytea NOT NULL CHECK (octet_length(contenido) BETWEEN 1 AND 5000000)
);
CREATE UNIQUE INDEX uq_archivo_activo_mascota ON archivo_mascota (mascota_id) WHERE activa;

-- El recibo conserva la respuesta original incluso después de un reemplazo o retiro.
CREATE TABLE operacion_foto_mascota (
    actor_id uuid NOT NULL,
    clave uuid NOT NULL,
    mascota_id uuid NOT NULL REFERENCES mascota(id),
    version_esperada integer NOT NULL,
    huella bytea NOT NULL,
    foto_id uuid,
    mime varchar(10),
    tamano integer,
    PRIMARY KEY (actor_id, clave)
);

-- El bloqueo del perfil se comparte con la edición y desactivación B-01.
CREATE FUNCTION ret_id_mascota_para_publicacion_pr(p_id uuid,p_actor uuid,p_organizaciones uuid[])
RETURNS TABLE(version integer,activa boolean,disponibilidad varchar,organizacion_id uuid)
LANGUAGE sql AS $$
    SELECT m.version,m.activa,m.disponibilidad,m.organizacion_id FROM mascota m
    WHERE m.id=p_id AND b_mascota_autorizada_pr(m.responsable_usuario_id,m.organizacion_id,p_actor,p_organizaciones)
    FOR UPDATE OF m;
$$;

CREATE PROCEDURE update_mascota_disponibilidad_pr(p_id uuid,p_valor text)
LANGUAGE sql AS $$
    UPDATE mascota SET disponibilidad=p_valor,version=version+1 WHERE id=p_id;
$$;

CREATE FUNCTION ret_id_mascota_publicable_pr(p_id uuid)
RETURNS TABLE (id uuid,nombre varchar,especie varchar,edad_estimada_meses integer,tamano varchar,
               energia varchar,organizacion_id uuid,comportamiento_ids uuid[],comportamiento_nombres text[],foto boolean)
LANGUAGE sql STABLE AS $$
    SELECT m.id,m.nombre,m.especie,m.edad_estimada_meses,m.tamano,m.energia,m.organizacion_id,
           m.comportamiento_ids,m.comportamiento_nombres,
           EXISTS (SELECT 1 FROM archivo_mascota a WHERE a.mascota_id=m.id AND a.activa)
    FROM mascota_perfil_privado_v m
    WHERE m.id=p_id AND m.activa AND m.disponibilidad='DISPONIBLE' AND m.organizacion_id IS NOT NULL;
$$;

CREATE FUNCTION ret_id_archivo_mascota_privado_pr(p_id uuid,p_actor uuid,p_organizaciones uuid[])
RETURNS TABLE(contenido bytea,mime_verificado varchar) LANGUAGE sql STABLE AS $$
    SELECT a.contenido,a.mime_verificado FROM archivo_mascota a JOIN mascota m ON m.id=a.mascota_id
    WHERE a.mascota_id=p_id AND a.activa
      AND b_mascota_autorizada_pr(m.responsable_usuario_id,m.organizacion_id,p_actor,p_organizaciones);
$$;

-- Estado del perfil y bytes de la foto corresponden a la misma lectura.
CREATE FUNCTION ret_id_archivo_mascota_publico_pr(p_id uuid)
RETURNS TABLE(contenido bytea,mime_verificado varchar,organizacion_id uuid) LANGUAGE sql STABLE AS $$
    SELECT a.contenido,a.mime_verificado,p.organizacion_id FROM ret_id_mascota_publicable_pr(p_id) p
    JOIN archivo_mascota a ON a.mascota_id=p.id AND a.activa;
$$;

CREATE FUNCTION create_operacion_foto_pr(p_actor uuid,p_clave uuid,p_mascota uuid,p_version integer,
                              p_huella bytea,p_foto uuid,p_mime varchar,p_tamano integer)
RETURNS boolean LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO operacion_foto_mascota(actor_id,clave,mascota_id,version_esperada,huella,foto_id,mime,tamano)
    VALUES (p_actor,p_clave,p_mascota,p_version,p_huella,p_foto,p_mime,p_tamano)
    ON CONFLICT (actor_id,clave) DO NOTHING;
    RETURN FOUND;
END;
$$;

CREATE FUNCTION ret_id_operacion_foto_pr(p_actor uuid,p_clave uuid)
RETURNS SETOF operacion_foto_mascota LANGUAGE sql STABLE AS $$
    SELECT o.* FROM operacion_foto_mascota o WHERE o.actor_id=p_actor AND o.clave=p_clave;
$$;

CREATE PROCEDURE update_archivo_mascota_pr(p_mascota uuid,p_actor uuid,p_foto uuid,
                                          p_mime varchar,p_huella bytea,p_contenido bytea)
LANGUAGE plpgsql AS $$
BEGIN
    UPDATE archivo_mascota SET activa=false WHERE mascota_id=p_mascota AND activa;
    IF p_foto IS NOT NULL THEN
        INSERT INTO archivo_mascota(id,mascota_id,subida_por_usuario_id,mime_verificado,sha256,contenido)
        VALUES (p_foto,p_mascota,p_actor,p_mime,p_huella,p_contenido);
    END IF;
    UPDATE mascota SET version=version+1 WHERE id=p_mascota;
END;
$$;
