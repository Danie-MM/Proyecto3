CREATE FUNCTION existe_especie_activa_pr(p_codigo text)
RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT EXISTS(
        SELECT 1
        FROM especie e
        WHERE e.codigo = p_codigo
            AND e.activa = true
    );
$$;