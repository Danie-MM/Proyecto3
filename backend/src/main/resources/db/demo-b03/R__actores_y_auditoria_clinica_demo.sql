-- Exclusivo de la demostración local o de un esquema aislado de pruebas.
DO $$
BEGIN
    IF current_schema() <> 'demo_b01' AND current_schema() !~ '^pruebas_[a-z0-9_]+$' THEN
        RAISE EXCEPTION 'Los datos clínicos de demostración requieren un esquema de demo o pruebas';
    END IF;
END;
$$;

-- Estos UUID coinciden con el selector de B-01. Cumplen las FK del expediente.
-- Los roles persistidos son ordinarios. Solo el adaptador de demo habilita al quinto actor.
INSERT INTO usuarios(id_publico,nombre_completo,email,rol) VALUES
    ('11111111-1111-4111-8111-111111111111','Responsable de Luna','luna-b01@example.invalid','ADOPTANTE'),
    ('22222222-2222-4222-8222-222222222222','Otro responsable','sol-b01@example.invalid','ADOPTANTE'),
    ('33333333-3333-4333-8333-333333333333','Representante del Refugio A','refugioa-b01@example.invalid','REFUGIO'),
    ('44444444-4444-4444-8444-444444444444','Representante del Refugio B','refugiob-b01@example.invalid','REFUGIO'),
    ('55555555-5555-4555-8555-555555555555','Veterinario de prueba','veterinario-b03@example.invalid','ADOPTANTE')
ON CONFLICT (id_publico) DO NOTHING;

CREATE TABLE IF NOT EXISTS auditoria_permiso_clinico_demo (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    fecha timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accion text NOT NULL CHECK (accion IN ('CONCEDER_PERMISO_CLINICO','REVOCAR_PERMISO_CLINICO')),
    actor_id uuid NOT NULL REFERENCES usuarios(id_publico),
    permiso_id uuid NOT NULL REFERENCES permiso_clinico(id),
    mascota_id uuid NOT NULL REFERENCES mascota(id),
    UNIQUE (permiso_id,accion)
);
