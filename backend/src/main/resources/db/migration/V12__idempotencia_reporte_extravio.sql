
-- C-02: Control de idempotencia en la publicación
-- de reportes de mascotas extraviadas.

CREATE TABLE reporte_extravio_intento_creacion (
                                                   usuario_id uuid NOT NULL,
                                                   clave uuid NOT NULL,
                                                   huella varchar(64) NOT NULL,
                                                   reporte_id uuid,
                                                   respuesta_json text,
                                                   estado_http integer,
                                                   creado_en timestamptz NOT NULL DEFAULT current_timestamp,

                                                   CONSTRAINT pk_reporte_extravio_intento
                                                       PRIMARY KEY (usuario_id, clave),

                                                   CONSTRAINT fk_reporte_extravio_intento_reporte
                                                       FOREIGN KEY (reporte_id)
                                                           REFERENCES reporte_extravio(id),

                                                   CONSTRAINT chk_reporte_extravio_intento_respuesta
                                                       CHECK (
                                                           (
                                                               reporte_id IS NULL
                                                                   AND respuesta_json IS NULL
                                                                   AND estado_http IS NULL
                                                               )
                                                               OR
                                                           (
                                                               reporte_id IS NOT NULL
                                                                   AND respuesta_json IS NOT NULL
                                                                   AND estado_http IS NOT NULL
                                                                   AND estado_http IN (200, 201)
                                                               )
                                                           )
);

CREATE INDEX idx_reporte_extravio_intento_reporte
    ON reporte_extravio_intento_creacion (reporte_id);
