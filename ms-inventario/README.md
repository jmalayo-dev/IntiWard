# ms-inventario · Stock y movimientos

| | |
|---|---|
| **Responsable** | Integrante 5 (también lleva ms-comprobantes) |
| **Puerto** | 8083 |
| **Nombre en Eureka** | `ms-inventario` |
| **Schema MySQL** | `db_inventario` (usuario `usr_inventario`) · script `infra/mysql/03-inventario.sql` |
| **Rutas en el gateway** | `/stock/**`, `/movimientos/**` |
| **Pantallas web** | Gestión de stock (admin) |

## Para qué existe

Es el dueño de **cuántas unidades hay** de cada producto y de **todo lo que le pasó a ese número**. La tabla de movimientos es lo que hace medible el Objetivo 2 del informe (*reducir errores logísticos en 30 %*): sin historial no hay nada que auditar.

Es el servicio más pequeño (2 tablas, 5 endpoints), por eso el integrante 5 lleva dos servicios.

## Concepto clave: reservar → confirmar / liberar

El checkout no descuenta stock de golpe. Lo hace en dos pasos para poder deshacerlo si algo falla después (**Saga por compensación**):

```
                     ┌── comprobante OK ──▶ POST /stock/confirmar   (cantidad -= n, reservada -= n)
POST /stock/reservar ┤
 (reservada += n)    └── comprobante falla ─▶ POST /stock/liberar   (reservada -= n)

disponible = cantidad - cantidad_reservada
```

Cada operación escribe una fila en `movimiento_stock`.

## Modelo de datos

```
stock (id, id_producto UNIQUE, cantidad, cantidad_reservada)
movimiento_stock (id, id_producto, tipo ENUM(ENTRADA, RESERVA, CONFIRMACION, LIBERACION, AJUSTE),
                  cantidad, referencia, fecha)
```

`id_producto` es el id de ms-catalogo, **sin FK** (está en otro schema: *Database per Service*). Semilla: stock para los productos 1–5.

## Estado actual

| Capa | Archivo | Estado |
|---|---|---|
| Entity | `Stock` (con `getDisponible()`), `MovimientoStock`, `TipoMovimiento` | ✅ |
| Repository | `StockRepository.findByIdProducto`, `MovimientoStockRepository.findByIdProductoOrderByFechaDesc` | ✅ con test `@DataJpaTest` |
| DTO / Mapper | `StockResponse(idProducto, cantidad, reservada, disponible)`, `StockMapper` | ✅ |
| Service / Controller | `GET /stock/{idProducto}` | ✅ verificado por el gateway |

## Endpoints objetivo

| Método | Ruta | Quién | Estado |
|---|---|---|---|
| GET | `/stock/{idProducto}` | Público / interno | ✅ |
| POST | `/stock/reservar` | Interno (ms-pedidos) | ⬜ |
| POST | `/stock/confirmar` | Interno (ms-pedidos) | ⬜ |
| POST | `/stock/liberar` | Interno (ms-pedidos) | ⬜ |
| POST | `/stock/entrada` o `PUT /stock/{idProducto}` | ADMIN (reposición/ajuste) | ⬜ |
| GET | `/movimientos?idProducto=` | ADMIN | ⬜ |

**Cuerpo sugerido** para reservar/confirmar/liberar (acordarlo con el integrante 4):

```json
{ "referencia": "venta-42", "items": [ { "idProducto": 1, "cantidad": 2 }, { "idProducto": 3, "cantidad": 1 } ] }
```

## Reglas de negocio

- Reservar **todo o nada**: si un ítem no alcanza, no se reserva ninguno → **409 Conflict** con el `idProducto` que falló.
- Operaciones con `@Transactional`. Para evitar que dos checkouts reserven la misma última unidad: `@Lock(PESSIMISTIC_WRITE)` en la consulta del stock, o `@Version` en `Stock`.
- Cantidades siempre `> 0` (`@Positive`).
- Liberar o confirmar una referencia dos veces no debe descontar dos veces (idempotencia: revisar el movimiento por `referencia`).

## Pendientes por semana

- [ ] **Semana 2–3**: `reservar`, `confirmar`, `liberar` con su movimiento; `GET /movimientos`; entrada y ajuste de stock (ADMIN); `StockInsuficienteException` → 409.
- [ ] **Semana 4**: acordar el contrato con ms-pedidos y probar el checkout juntos.
- [ ] **Semana 5**: semilla de stock para los 200 productos (coordinar con el integrante 3).
- [ ] **Semana 6**: pantalla `/admin/stock` en `web-client`.
- [ ] **Semana 7**: tests Mockito de `StockService` (reserva OK, stock insuficiente, liberar devuelve el disponible) y `postman/ms-inventario.postman_collection.json`.

## Cómo correrlo

```bash
docker compose up -d mysql ms-discovery
./mvnw -pl ms-inventario spring-boot:run
curl localhost:8083/stock/1
```
