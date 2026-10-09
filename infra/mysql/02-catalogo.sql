-- ms-catalogo (dueño: integrante 3). PENDIENTE: completar a 200 productos (semana 5).
SET NAMES utf8mb4;
USE db_catalogo;

CREATE TABLE marca (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE categoria (
    id     INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(60) NOT NULL UNIQUE
);

CREATE TABLE producto (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(150)   NOT NULL,
    descripcion  VARCHAR(500),
    precio       DECIMAL(10, 2) NOT NULL,
    imagen_url   VARCHAR(300),
    id_marca     INT            NOT NULL,
    id_categoria INT            NOT NULL,
    activo       BIT(1)         NOT NULL DEFAULT 1,
    CONSTRAINT fk_producto_marca     FOREIGN KEY (id_marca)     REFERENCES marca (id),
    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria (id)
);

INSERT INTO marca (nombre) VALUES ('Dahua'), ('Honeywell'), ('Samsung'), ('Nest'), ('Sony');
INSERT INTO categoria (nombre) VALUES ('Cámaras'), ('Alarmas'), ('Sensores de humo'), ('Cerraduras digitales'), ('Control de acceso');

INSERT INTO producto (nombre, descripcion, precio, id_marca, id_categoria) VALUES
('Cámara IP Dahua 4MP',           'Cámara bullet exterior con visión nocturna de 30 m',   289.90, 1, 1),
('Panel de alarma Honeywell Vista','Panel cableado de 8 zonas con teclado',               749.00, 2, 2),
('Cerradura digital Samsung SHP', 'Cerradura con huella, clave y tarjeta RFID',           1199.00, 3, 4),
('Detector de humo Nest Protect', 'Sensor de humo y monóxido de carbono con Wi-Fi',        459.90, 4, 3),
('Cámara domo Sony 1080p',        'Cámara domo interior con audio bidireccional',          219.50, 5, 1);
