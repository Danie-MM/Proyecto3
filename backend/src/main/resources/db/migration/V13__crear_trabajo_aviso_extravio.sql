
-- Registra el trabajo pendiente de avisos por proximidad
-- asociado a la publicación de un reporte de extravío.

CREATE TABLE reporte_extravio_trabajo_aviso (

                                                reporte_id uuid PRIMARY KEY,

                                                estado varchar(30) NOT NULL DEFAULT 'PENDIENTE',

                                                intentos integer NOT NULL DEFAULT 0,

                                                creado_en timestamptz NOT NULL DEFAULT current_timestamp,

                                                actualizado_en timestamptz NOT NULL DEFAULT current_timestamp,

                                                ultimo_error varchar(500),

                                                CONSTRAINT trabajo_aviso_reporte_fk
                                                    FOREIGN KEY (reporte_id)
                                                        REFERENCES reporte_extravio(id),

                                                CONSTRAINT trabajo_aviso_estado_valido
                                                    CHECK (estado IN (
                                                                      'PENDIENTE',
                                                                      'PROCESANDO',
                                                                      'COMPLETADO',
                                                                      'FALLIDO',
                                                                      'CANCELADO'
                                                        )),

                                                CONSTRAINT trabajo_aviso_intentos_validos
                                                    CHECK (intentos >= 0)
);

CREATE INDEX idx_trabajo_aviso_estado
    ON reporte_extravio_trabajo_aviso (estado);
