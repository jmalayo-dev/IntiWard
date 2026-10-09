-- =====================================================================
-- IntiGuard v2 · Schemas y usuarios (dueño: integrante 1)
-- Un schema por servicio y un usuario MySQL por servicio con permisos solo sobre su schema.
-- Las contraseñas "*_local" son SOLO para desarrollo. En RDS se crean con otras y viven en Parameter Store.
-- =====================================================================
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS db_auth         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_catalogo     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_inventario   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_pedidos      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_comprobantes CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'usr_auth'@'%'         IDENTIFIED BY 'usr_auth_local';
CREATE USER IF NOT EXISTS 'usr_catalogo'@'%'     IDENTIFIED BY 'usr_catalogo_local';
CREATE USER IF NOT EXISTS 'usr_inventario'@'%'   IDENTIFIED BY 'usr_inventario_local';
CREATE USER IF NOT EXISTS 'usr_pedidos'@'%'      IDENTIFIED BY 'usr_pedidos_local';
CREATE USER IF NOT EXISTS 'usr_comprobantes'@'%' IDENTIFIED BY 'usr_comprobantes_local';

GRANT SELECT, INSERT, UPDATE, DELETE ON db_auth.*         TO 'usr_auth'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_catalogo.*     TO 'usr_catalogo'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_inventario.*   TO 'usr_inventario'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_pedidos.*      TO 'usr_pedidos'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON db_comprobantes.* TO 'usr_comprobantes'@'%';
FLUSH PRIVILEGES;
