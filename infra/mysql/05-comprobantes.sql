-- ms-comprobantes (dueño: integrante 5)
SET NAMES utf8mb4;
USE db_comprobantes;

CREATE TABLE serie_correlativo (
    id                 INT AUTO_INCREMENT PRIMARY KEY,
    tipo               ENUM ('BOLETA','FACTURA') NOT NULL UNIQUE,
    serie              VARCHAR(4) NOT NULL,
    ultimo_correlativo BIGINT     NOT NULL DEFAULT 0,
    version            BIGINT
);

CREATE TABLE comprobante (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_venta          BIGINT         NOT NULL UNIQUE,
    tipo              ENUM ('BOLETA','FACTURA') NOT NULL,
    numero            VARCHAR(13)    NOT NULL UNIQUE,
    documento_cliente VARCHAR(11),
    total             DECIMAL(10, 2) NOT NULL,
    fecha_emision     DATETIME(6)    NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

INSERT INTO serie_correlativo (tipo, serie, ultimo_correlativo, version) VALUES ('BOLETA', 'B001', 0, 0), ('FACTURA', 'F001', 0, 0);
