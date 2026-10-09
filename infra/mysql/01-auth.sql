-- ms-auth (dueño: integrante 2)
SET NAMES utf8mb4;
USE db_auth;

CREATE TABLE rol (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE usuario (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre            VARCHAR(100) NOT NULL,
    correo            VARCHAR(120) NOT NULL UNIQUE,
    password          VARCHAR(100) NOT NULL,
    id_rol            INT          NOT NULL,
    activo            BIT(1)       NOT NULL DEFAULT 1,
    intentos_fallidos INT          NOT NULL DEFAULT 0,
    bloqueado_hasta   DATETIME(6),
    fecha_registro    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id)
);

INSERT INTO rol (nombre) VALUES ('ADMIN'), ('CLIENTE');

-- Usuario semilla: admin@intiguard.pe / Admin12345 (hash BCrypt fuerza 10). Cambiarlo en la nube.
INSERT INTO usuario (nombre, correo, password, id_rol)
VALUES ('Administrador', 'admin@intiguard.pe', '$2a$10$HBFvaQHAyu5kYzFohVMqX.FcPG0B2vVpOYPTLbWkvTju/prDjwDw6', 1);
