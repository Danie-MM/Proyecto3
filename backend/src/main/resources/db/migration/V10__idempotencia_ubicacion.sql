-- La intención de alta sobrevive a retirar la preferencia: un reintento no la recrea.
CREATE TABLE ubicacion_intento_creacion (
    usuario_id uuid NOT NULL,
    clave uuid NOT NULL,
    huella varchar(64) NOT NULL,
    preferencia_id uuid,
    respuesta_json text,
    creado_en timestamptz NOT NULL DEFAULT current_timestamp,
    CONSTRAINT pk_ubicacion_intento_creacion PRIMARY KEY (usuario_id, clave),
    CONSTRAINT chk_ubicacion_intento_respuesta CHECK (
        (preferencia_id IS NULL AND respuesta_json IS NULL)
        OR (preferencia_id IS NOT NULL AND respuesta_json IS NOT NULL)
    )
);

-- No FK con borrado en cascada: se conserva el intento aun si se retira la ubicación.
CREATE INDEX idx_ubicacion_intento_preferencia ON ubicacion_intento_creacion (preferencia_id);
