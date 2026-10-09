-- ms-pedidos (dueño: integrante 4)
SET NAMES utf8mb4;
USE db_pedidos;

CREATE TABLE carrito (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario     BIGINT      NOT NULL UNIQUE,
    fecha_creacion DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE item_carrito (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_carrito  BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    cantidad    INT    NOT NULL,
    CONSTRAINT fk_item_carrito FOREIGN KEY (id_carrito) REFERENCES carrito (id) ON DELETE CASCADE,
    CONSTRAINT uq_item_carrito_producto UNIQUE (id_carrito, id_producto)
);

CREATE TABLE venta (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_usuario     BIGINT         NOT NULL,
    fecha          DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    total          DECIMAL(10, 2) NOT NULL,
    estado         ENUM ('PENDIENTE','CONFIRMADA','ANULADA') NOT NULL DEFAULT 'PENDIENTE',
    id_comprobante BIGINT,
    INDEX idx_venta_usuario (id_usuario)
);

CREATE TABLE detalle_venta (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_venta        BIGINT         NOT NULL,
    id_producto     BIGINT         NOT NULL,
    nombre_producto VARCHAR(150)   NOT NULL,
    cantidad        INT            NOT NULL,
    precio_unitario DECIMAL(10, 2) NOT NULL,
    subtotal        DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_detalle_venta FOREIGN KEY (id_venta) REFERENCES venta (id) ON DELETE CASCADE
);
