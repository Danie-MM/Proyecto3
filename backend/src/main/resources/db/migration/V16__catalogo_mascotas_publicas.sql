-- El detalle, la foto pública y el listado comparten la proyección de B.
-- La vigencia de la organización se comprueba con A antes de paginar.
CREATE VIEW mascota_perfil_publicable_v AS
SELECT m.id, m.nombre, m.especie, m.edad_estimada_meses, m.tamano, m.energia,
       m.organizacion_id, m.comportamiento_ids, m.comportamiento_nombres,
       EXISTS (SELECT 1 FROM archivo_mascota a WHERE a.mascota_id = m.id AND a.activa) AS foto
FROM mascota_perfil_privado_v m
WHERE m.activa AND m.disponibilidad = 'DISPONIBLE' AND m.organizacion_id IS NOT NULL;

CREATE OR REPLACE FUNCTION ret_id_mascota_publicable_pr(p_id uuid)
RETURNS TABLE (id uuid,nombre varchar,especie varchar,edad_estimada_meses integer,tamano varchar,
               energia varchar,organizacion_id uuid,comportamiento_ids uuid[],comportamiento_nombres text[],foto boolean)
LANGUAGE sql STABLE AS $$
    SELECT * FROM mascota_perfil_publicable_v m WHERE m.id = p_id;
$$;

CREATE FUNCTION ret_all_mascota_catalogo_pr(p_organizaciones uuid[], p_nombre text, p_especie text,
    p_minimo integer, p_maximo integer, p_tamanos text[], p_energias text[], p_etiquetas uuid[])
RETURNS SETOF mascota_perfil_publicable_v LANGUAGE sql STABLE AS $$
    SELECT m.* FROM mascota_perfil_publicable_v m
    WHERE m.especie IN ('PERRO', 'GATO') AND m.organizacion_id = ANY(p_organizaciones)
      AND (p_nombre = '' OR strpos(lower(m.nombre), lower(p_nombre)) > 0)
      AND (p_especie IS NULL OR m.especie = p_especie)
      AND (p_minimo IS NULL OR m.edad_estimada_meses >= p_minimo)
      AND (p_maximo IS NULL OR m.edad_estimada_meses <= p_maximo)
      AND (cardinality(p_tamanos) = 0 OR m.tamano = ANY(p_tamanos))
      AND (cardinality(p_energias) = 0 OR m.energia = ANY(p_energias))
      AND (cardinality(p_etiquetas) = 0 OR m.comportamiento_ids && p_etiquetas);
$$;
