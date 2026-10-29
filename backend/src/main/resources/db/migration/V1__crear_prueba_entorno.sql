-- Flyway ejecuta esta migración una sola vez en el esquema configurado.
-- Si otro compañero ya la aplicó, los cambios nuevos deben ir en V2, V3, etc.
CREATE TABLE prueba_entorno (
    id uuid PRIMARY KEY,
    texto varchar(80) NOT NULL,
    creado_en timestamptz NOT NULL,
    CONSTRAINT prueba_entorno_texto_valido
        CHECK (char_length(texto) BETWEEN 1 AND 80 AND texto = btrim(texto))
);

-- Los parámetros vienen de Spring; no se construyen consultas concatenando texto.
CREATE PROCEDURE create_prueba_entorno_pr(
    IN p_id uuid,
    IN p_texto varchar(80),
    IN p_creado_en timestamptz
)
LANGUAGE plpgsql
AS $$
BEGIN
    INSERT INTO prueba_entorno (id, texto, creado_en)
    VALUES (p_id, p_texto, p_creado_en);
END;
$$;

-- PostgreSQL devuelve filas mediante una función; conserva el nombre del acuerdo.
CREATE FUNCTION ret_id_prueba_entorno_pr(p_id uuid)
RETURNS TABLE (id uuid, texto varchar(80), creado_en timestamptz)
LANGUAGE sql
STABLE
AS $$
    SELECT registro.id, registro.texto, registro.creado_en
    FROM prueba_entorno AS registro
    WHERE registro.id = p_id;
$$;
