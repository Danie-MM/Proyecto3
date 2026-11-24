-- Datos iniciales necesarios para registrar mascotas sin activar demo-b01.
-- No se modifica V2: Flyway conserva las migraciones ya aplicadas.
-- Se respetan etiquetas existentes y no se reactivan opciones retiradas.
INSERT INTO etiqueta_comportamiento (id, nombre, activa)
SELECT inicial.id, inicial.nombre, true
FROM (VALUES
    ('b0100000-0000-4000-8000-000000000001'::uuid, 'Sociable'),
    ('b0100000-0000-4000-8000-000000000002'::uuid, 'Tranquilo'),
    ('b0100000-0000-4000-8000-000000000003'::uuid, 'Juguetón')
) AS inicial(id, nombre)
WHERE NOT EXISTS (
    SELECT 1
    FROM etiqueta_comportamiento existente
    WHERE lower(existente.nombre) = lower(inicial.nombre)
)
ON CONFLICT (id) DO NOTHING;
