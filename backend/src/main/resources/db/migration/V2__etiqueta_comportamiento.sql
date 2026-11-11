-- Las etiquetas serán incorporadas por una nueva migración cuando G-01 apruebe la lista.
CREATE TABLE etiqueta_comportamiento (
    id uuid PRIMARY KEY,
    nombre varchar(80) NOT NULL CHECK (char_length(nombre) >= 1 AND nombre = btrim(nombre)),
    activa boolean NOT NULL DEFAULT true
);

CREATE FUNCTION ret_all_etiqueta_comportamiento_pr()
RETURNS SETOF etiqueta_comportamiento
LANGUAGE sql STABLE
AS $$
    SELECT etiqueta.* FROM etiqueta_comportamiento etiqueta ORDER BY etiqueta.nombre, etiqueta.id;
$$;
