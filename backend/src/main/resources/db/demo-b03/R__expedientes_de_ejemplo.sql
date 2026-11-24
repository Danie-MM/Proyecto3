-- Datos ficticios únicamente para el perfil de demostración de B.
DO $$
BEGIN
    IF current_schema() <> 'demo_b01' AND current_schema() !~ '^pruebas_[a-z0-9_]+$' THEN
        RAISE EXCEPTION 'El expediente de demostración requiere un esquema de demo o pruebas';
    END IF;
END;
$$;

-- Una visita histórica con permiso de escritura explica quién registró el refuerzo.
-- El permiso actual es de lectura. Si el usuario ya gestionó permisos para esa pareja,
-- se conserva su historial completo y no se restituye ningún acceso.
WITH pareja_sin_historial AS (
    SELECT m.id AS mascota, m.responsable_usuario_id AS titular, v.id_publico AS profesional
      FROM mascota m JOIN usuarios v ON v.id_publico = '55555555-5555-4555-8555-555555555555' AND v.activo
     WHERE m.id = '10000000-0000-4000-8000-000000000001' AND m.activa AND m.organizacion_id IS NULL
       AND m.responsable_usuario_id = '11111111-1111-4111-8111-111111111111'
       AND NOT EXISTS (SELECT 1 FROM permiso_clinico p
                        WHERE p.mascota_id = m.id AND p.beneficiario_usuario_id = v.id_publico)
), permisos_nuevos AS (
    INSERT INTO permiso_clinico(id, mascota_id, beneficiario_usuario_id, fundamento, alcance,
        otorgado_por_usuario_id, creado_en, version, activo, revocado_en, revocado_por_usuario_id, motivo_revocacion)
    SELECT p.id, pareja.mascota, pareja.profesional, 'VETERINARIO_AUTORIZADO', p.alcance,
        pareja.titular, p.otorgado, CASE WHEN p.retirado IS NULL THEN 0 ELSE 1 END, p.retirado IS NULL,
        p.retirado, CASE WHEN p.retirado IS NOT NULL THEN pareja.titular END, p.motivo
      FROM pareja_sin_historial pareja CROSS JOIN (VALUES
          ('b0300000-0000-4000-8000-000000000101'::uuid, 'LECTURA_ESCRITURA',
           '2026-10-12 08:00-06'::timestamptz, '2026-10-19 13:00-06'::timestamptz, 'Finalizó la visita de control.'),
          ('b0300000-0000-4000-8000-000000000102'::uuid, 'LECTURA',
           '2026-10-20 09:00-06'::timestamptz, NULL::timestamptz, NULL::text)
      ) p(id,alcance,otorgado,retirado,motivo)
    ON CONFLICT DO NOTHING
    RETURNING *
)
INSERT INTO auditoria_permiso_clinico_demo(fecha,accion,actor_id,permiso_id,mascota_id)
SELECT a.fecha, a.accion, p.otorgado_por_usuario_id, p.id, p.mascota_id
FROM permisos_nuevos p CROSS JOIN LATERAL (VALUES
    (p.creado_en, 'CONCEDER_PERMISO_CLINICO'), (p.revocado_en, 'REVOCAR_PERMISO_CLINICO')
) a(fecha,accion)
WHERE a.fecha IS NOT NULL;

-- Los UUID estables distinguen estos ejemplos de cualquier vacuna que registró el usuario.
-- Si un ensayo previo dejó otros permisos, la ficha del refuerzo se importa como declarada
-- por el titular, en vez de atribuir al profesional una visita sin autorización demostrable.
WITH muestras(id, mascota_id, titular, fecha, nombre, descripcion, visita_profesional) AS (VALUES
    ('b0300000-0000-4000-8000-000000000001'::uuid, '10000000-0000-4000-8000-000000000001'::uuid,
     '11111111-1111-4111-8111-111111111111'::uuid, DATE '2026-06-12', 'Triple felina', 'Primera anotación del carné de Luna.', false),
    ('b0300000-0000-4000-8000-000000000002', '10000000-0000-4000-8000-000000000001',
     '11111111-1111-4111-8111-111111111111', DATE '2026-08-07', 'Antirrábica', 'Fecha transcrita del comprobante de vacunación.', false),
    ('b0300000-0000-4000-8000-000000000003', '10000000-0000-4000-8000-000000000001',
     '11111111-1111-4111-8111-111111111111', DATE '2026-10-16', 'Refuerzo de triple felina', 'Registro de la visita de control de octubre.', true),
    ('b0300000-0000-4000-8000-000000000004', '10000000-0000-4000-8000-000000000002',
     '22222222-2222-4222-8222-222222222222', DATE '2026-09-08', 'Múltiple canina', 'Anotada en el carné de Sol.', false),
    ('b0300000-0000-4000-8000-000000000005', '10000000-0000-4000-8000-000000000003',
     '33333333-3333-4333-8333-333333333333', DATE '2026-10-02', 'Triple felina', 'El refugio conservó el comprobante de Nube.', false),
    ('b0300000-0000-4000-8000-000000000006', '10000000-0000-4000-8000-000000000004',
     '44444444-4444-4444-8444-444444444444', DATE '2026-09-24', 'Antirrábica', 'El refugio incorporó el carné de Coco a su expediente.', false)
)
INSERT INTO evento_veterinario(id,mascota_id,tipo,fecha,nombre,descripcion,autor_usuario_id,origen,creado_en)
SELECT m.id, m.mascota_id, 'VACUNA', m.fecha, m.nombre, m.descripcion,
       coalesce(p.beneficiario_usuario_id, m.titular),
       CASE WHEN p.id IS NULL THEN 'DECLARADO_RESPONSABLE' ELSE 'REGISTRADO_VETERINARIO' END,
       (m.fecha + TIME '10:00') AT TIME ZONE 'America/Costa_Rica'
FROM muestras m
JOIN mascota mascota_actual ON mascota_actual.id = m.mascota_id AND mascota_actual.responsable_usuario_id = m.titular
LEFT JOIN permiso_clinico p ON m.visita_profesional AND p.id = 'b0300000-0000-4000-8000-000000000101'
  AND p.mascota_id = m.mascota_id AND p.alcance = 'LECTURA_ESCRITURA'
  AND (m.fecha + TIME '10:00') AT TIME ZONE 'America/Costa_Rica' BETWEEN p.creado_en AND p.revocado_en
ON CONFLICT (id) DO NOTHING;
