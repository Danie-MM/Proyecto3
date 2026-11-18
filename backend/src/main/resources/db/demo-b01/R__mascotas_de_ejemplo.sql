-- Solo para la demo local. Un registro ya existente conserva su perfil y sus etiquetas.
WITH nuevas AS (
    INSERT INTO mascota (id, nombre, especie, edad_estimada_meses, tamano, energia, responsable_usuario_id)
    VALUES ('10000000-0000-4000-8000-000000000005', 'Milo', 'GATO', 18, 'PEQUENO', 'BAJA',
            '11111111-1111-4111-8111-111111111111'),
           ('10000000-0000-4000-8000-000000000006', 'Kira', 'PERRO', 9, 'MEDIANO', 'ALTA',
            '11111111-1111-4111-8111-111111111111')
    ON CONFLICT (id) DO NOTHING
    RETURNING id
)
INSERT INTO mascota_comportamiento (mascota_id, etiqueta_id)
SELECT n.id, etiqueta.id FROM nuevas n
JOIN (VALUES
    ('10000000-0000-4000-8000-000000000005'::uuid, '90000000-0000-4000-8000-000000000002'::uuid),
    ('10000000-0000-4000-8000-000000000006'::uuid, '90000000-0000-4000-8000-000000000003'::uuid)
) AS etiqueta(mascota, id) ON etiqueta.mascota=n.id;
