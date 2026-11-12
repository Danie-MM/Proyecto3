-- Catálogo de especies que utilizará el módulo de mascotas.
CREATE TABLE especie (
                         id uuid NOT NULL DEFAULT gen_random_uuid(),
                         codigo varchar(30) NOT NULL,
                         nombre varchar(80) NOT NULL,
                         activa boolean NOT NULL DEFAULT true,

                         CONSTRAINT pk_especie PRIMARY KEY (id),
                         CONSTRAINT uq_especie_codigo UNIQUE (codigo),

                         CONSTRAINT chk_especie_codigo
                             CHECK (codigo ~ '^[A-Z][A-Z0-9_]*$'),

    CONSTRAINT chk_especie_nombre
        CHECK (nombre = btrim(nombre) AND char_length(nombre) > 0)
);

-- Opciones iniciales del alcance actual del proyecto.
INSERT INTO especie (codigo, nombre)
VALUES
    ('PERRO', 'Perro'),
    ('GATO', 'Gato');