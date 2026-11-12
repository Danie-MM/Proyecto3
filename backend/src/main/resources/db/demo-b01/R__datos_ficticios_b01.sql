-- SOLO se carga con el perfil demo-b01, en el esquema separado demo_b01.
-- Estas etiquetas permiten practicar: NO son el catálogo aprobado por el equipo.
INSERT INTO etiqueta_comportamiento (id, nombre, activa) VALUES
    ('90000000-0000-4000-8000-000000000001', 'Sociable (demo)', true),
    ('90000000-0000-4000-8000-000000000002', 'Tranquilo (demo)', true),
    ('90000000-0000-4000-8000-000000000003', 'Juguetón (demo)', true)
ON CONFLICT (id) DO NOTHING;

-- Cada mascota permite comprobar un ámbito diferente sin usar datos personales reales.
INSERT INTO mascota (id, nombre, especie, edad_estimada_meses, tamano, energia,
                     responsable_usuario_id, organizacion_id) VALUES
    ('10000000-0000-4000-8000-000000000001', 'Luna', 'GATO', 8, 'PEQUENO', 'MEDIA',
     '11111111-1111-4111-8111-111111111111', NULL),
    ('10000000-0000-4000-8000-000000000002', 'Sol', 'PERRO', 24, 'MEDIANO', 'BAJA',
     '22222222-2222-4222-8222-222222222222', NULL),
    ('10000000-0000-4000-8000-000000000003', 'Nube', 'GATO', 12, 'PEQUENO', 'MEDIA',
     '33333333-3333-4333-8333-333333333333', 'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'),
    ('10000000-0000-4000-8000-000000000004', 'Coco', 'PERRO', 36, 'GRANDE', 'ALTA',
     '44444444-4444-4444-8444-444444444444', 'bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb')
ON CONFLICT (id) DO NOTHING;

INSERT INTO mascota_comportamiento (mascota_id, etiqueta_id) VALUES
    ('10000000-0000-4000-8000-000000000001', '90000000-0000-4000-8000-000000000003'),
    ('10000000-0000-4000-8000-000000000002', '90000000-0000-4000-8000-000000000002'),
    ('10000000-0000-4000-8000-000000000003', '90000000-0000-4000-8000-000000000001'),
    ('10000000-0000-4000-8000-000000000004', '90000000-0000-4000-8000-000000000003')
ON CONFLICT DO NOTHING;
