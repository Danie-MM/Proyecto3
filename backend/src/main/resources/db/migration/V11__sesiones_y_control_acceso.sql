
-- Tabla para guardar las sesiones de los usuarios
CREATE TABLE sesiones_acceso (
                                 id VARCHAR(36) PRIMARY KEY,
                                 email VARCHAR(255) NOT NULL,
                                 ultimo_acceso TIMESTAMP WITH TIME ZONE NOT NULL,
                                 expira TIMESTAMP WITH TIME ZONE NOT NULL,
                                 revocada BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_sesiones_acceso_email
    ON sesiones_acceso(email);


-- Tabla para controlar los intentos fallidos de inicio de sesión
CREATE TABLE intentos_acceso (
                                 origen VARCHAR(128) PRIMARY KEY,
                                 intentos INTEGER NOT NULL,
                                 inicio_ventana TIMESTAMP WITH TIME ZONE NOT NULL,
                                 bloqueado_hasta TIMESTAMP WITH TIME ZONE
);
