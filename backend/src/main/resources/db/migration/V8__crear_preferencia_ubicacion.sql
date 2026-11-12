
-- C-01: se integra después de usuarios, conservando V1–V6 de B.
CREATE TABLE preferencia_ubicacion (
                                       id uuid PRIMARY KEY,
                                       usuario_id uuid NOT NULL UNIQUE,

                                       latitud numeric(9,6),
                                       longitud numeric(9,6),
                                       referencia varchar(200),

                                       acepta_avisos boolean NOT NULL DEFAULT false,
                                       consentimiento_en timestamptz,
                                       actualizada_en timestamptz NOT NULL,
                                       creado_en timestamptz NOT NULL,

                                       version integer NOT NULL DEFAULT 0,

                                       CONSTRAINT preferencia_ubicacion_coordenadas_completas
                                           CHECK (
                                               (latitud IS NULL AND longitud IS NULL)
                                                   OR
                                               (latitud IS NOT NULL AND longitud IS NOT NULL)
                                               ),

                                       CONSTRAINT preferencia_ubicacion_latitud_valida
                                           CHECK (latitud BETWEEN -90 AND 90),

                                       CONSTRAINT preferencia_ubicacion_longitud_valida
                                           CHECK (longitud BETWEEN -180 AND 180)
);
