-- El ID numérico de A se conserva; los módulos reciben un UUID estable.
ALTER TABLE usuarios ADD COLUMN id_publico uuid NOT NULL DEFAULT gen_random_uuid();
ALTER TABLE usuarios ADD CONSTRAINT usuarios_id_publico_unico UNIQUE (id_publico);
