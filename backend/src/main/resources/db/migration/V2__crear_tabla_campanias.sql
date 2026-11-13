CREATE TABLE campanias (
                           id UUID PRIMARY KEY,
                           organizacion_id UUID NOT NULL,
                           creado_por_id UUID NOT NULL,
                           nombre VARCHAR(150) NOT NULL,
                           descripcion VARCHAR(2000) NOT NULL,
                           fecha_inicio DATE NOT NULL,
                           fecha_fin DATE NOT NULL,
                           estado VARCHAR(20) NOT NULL,
                           creado_en TIMESTAMP WITH TIME ZONE NOT NULL,
                           version BIGINT NOT NULL DEFAULT 0,

                           CONSTRAINT chk_fechas CHECK (fecha_fin >= fecha_inicio),
                           CONSTRAINT chk_nombre_largo CHECK (char_length(nombre) >= 3)
);