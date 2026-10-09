-- ms-inventario (dueño: integrante 5)
SET NAMES utf8mb4;
USE db_inventario;

-- id_producto apunta a db_catalogo.producto, pero SIN FK: cada servicio es dueño de su schema.
CREATE TABLE stock (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_producto        BIGINT NOT NULL UNIQUE,
    cantidad           INT    NOT NULL,
    cantidad_reservada INT    NOT NULL DEFAULT 0
);

CREATE TABLE movimiento_stock (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_producto BIGINT      NOT NULL,
    tipo        ENUM ('ENTRADA','RESERVA','CONFIRMACION','LIBERACION','AJUSTE') NOT NULL,
    cantidad    INT         NOT NULL,
    referencia  VARCHAR(60),
    fecha       DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_movimiento_producto (id_producto)
);

INSERT INTO stock (id_producto, cantidad) VALUES (1, 25), (2, 10), (3, 8), (4, 40), (5, 30);
INSERT INTO movimiento_stock (id_producto, tipo, cantidad, referencia) VALUES
(1, 'ENTRADA', 25, 'carga-inicial'), (2, 'ENTRADA', 10, 'carga-inicial'), (3, 'ENTRADA', 8, 'carga-inicial'),
(4, 'ENTRADA', 40, 'carga-inicial'), (5, 'ENTRADA', 30, 'carga-inicial');
