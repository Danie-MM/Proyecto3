
-- C-02: Registro de reportes de mascotas extraviadas.
CREATE TABLE reporte_extravio (

                                  id uuid PRIMARY KEY,

                                  mascota_id uuid NOT NULL,
                                  creado_por_usuario_id uuid NOT NULL,

                                  fecha_extravio timestamptz NOT NULL,

                                  latitud numeric(9,6) NOT NULL,
                                  longitud numeric(9,6) NOT NULL,

                                  latitud_origen_aviso numeric(9,6) NOT NULL,
                                  longitud_origen_aviso numeric(9,6) NOT NULL,

                                  radio_metros integer NOT NULL DEFAULT 3000,

                                  descripcion varchar(1000) NOT NULL,

                                  tipo_contacto varchar(40) NOT NULL,
                                  contacto varchar(254) NOT NULL,
                                  contacto_autorizado_en timestamptz NOT NULL,

                                  estado varchar(40) NOT NULL DEFAULT 'ACTIVO',

                                  actualizado_en timestamptz NOT NULL,
                                  cerrado_en timestamptz,
                                  cerrado_por_usuario_id uuid,
                                  motivo_cierre varchar(500),

                                  creado_en timestamptz NOT NULL,

                                  version integer NOT NULL DEFAULT 0,

    -- Integración con el módulo B.
                                  CONSTRAINT reporte_extravio_mascota_fk
                                      FOREIGN KEY (mascota_id)
                                          REFERENCES mascota(id),

    -- Integración con el módulo A.
                                  CONSTRAINT reporte_extravio_creador_fk
                                      FOREIGN KEY (creado_por_usuario_id)
                                          REFERENCES usuarios(id_publico),

                                  CONSTRAINT reporte_extravio_usuario_cierre_fk
                                      FOREIGN KEY (cerrado_por_usuario_id)
                                          REFERENCES usuarios(id_publico),

    -- Validación de coordenadas.
                                  CONSTRAINT reporte_extravio_latitud_valida
                                      CHECK (latitud BETWEEN -90 AND 90),

                                  CONSTRAINT reporte_extravio_longitud_valida
                                      CHECK (longitud BETWEEN -180 AND 180),

                                  CONSTRAINT reporte_extravio_latitud_origen_valida
                                      CHECK (latitud_origen_aviso BETWEEN -90 AND 90),

                                  CONSTRAINT reporte_extravio_longitud_origen_valida
                                      CHECK (longitud_origen_aviso BETWEEN -180 AND 180),

    -- Radio de notificación de 3 kilómetros.
                                  CONSTRAINT reporte_extravio_radio_valido
                                      CHECK (radio_metros = 3000),

    -- Validación de descripción.
                                  CONSTRAINT reporte_extravio_descripcion_valida
                                      CHECK (char_length(trim(descripcion)) BETWEEN 10 AND 1000),

    -- Medios de contacto permitidos.
                                  CONSTRAINT reporte_extravio_tipo_contacto_valido
                                      CHECK (tipo_contacto IN ('CORREO', 'TELEFONO')),

                                  CONSTRAINT reporte_extravio_contacto_valido
                                      CHECK (char_length(trim(contacto)) > 0),

    -- Estados permitidos.
                                  CONSTRAINT reporte_extravio_estado_valido
                                      CHECK (estado IN ('ACTIVO', 'ENCONTRADA', 'CANCELADO')),

    -- La fecha del extravío no puede superar la fecha de creación.
                                  CONSTRAINT reporte_extravio_fecha_valida
                                      CHECK (fecha_extravio <= creado_en),

    -- Control de versiones.
                                  CONSTRAINT reporte_extravio_version_valida
                                      CHECK (version >= 0)
);

-- Impide dos reportes activos para una misma mascota.
CREATE UNIQUE INDEX uq_reporte_extravio_activo
    ON reporte_extravio (mascota_id)
    WHERE estado = 'ACTIVO';

-- Facilita la consulta de reportes por usuario.
CREATE INDEX idx_reporte_extravio_creador
    ON reporte_extravio (creado_por_usuario_id);

-- Facilita la consulta del historial de una mascota.
CREATE INDEX idx_reporte_extravio_mascota
    ON reporte_extravio (mascota_id);
