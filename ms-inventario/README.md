# ms-inventario · Stock y movimientos

| | |
|---|---|
| **Responsable** | Integrante 5 (también lleva ms-comprobantes) |
| **Puerto** | 8083 |
| **Nombre en Eureka** | `ms-inventario` |
| **Schema MySQL** | `db_inventario` (usuario `ms_inventario`) · script `infra/mysql/03-inventario.sql` |
| **Rutas en el gateway** | `/stock/**`, `/movimientos/**` |
| **Pantallas web** | Gestión de stock (admin) |

## Para qué existe

Es el dueño de **cuántas unidades hay** de cada producto y de **todo lo que le pasó a ese número**. La tabla de movimientos es lo que hace medible el Objetivo 2 del informe (*reducir errores logísticos en 30 %*): sin historial no hay nada que auditar.

Es el servicio más pequeño (3 tablas y una vista, 5 endpoints), por eso el integrante 5 lleva dos servicios.

## Concepto clave: reservar → confirmar / liberar

El checkout no descuenta stock de golpe. Lo hace en dos pasos para poder deshacerlo si algo falla después (**Saga por compensación**):

```
POST /stock/reservar           ┌── comprobante OK ──▶ POST /stock/confirmar   (reservado -= n)
 (disponible -= n,             ┤                         reserva → CONFIRMADA
  reservado  += n,             └── comprobante falla ─▶ POST /stock/liberar   (disponible += n, reservado -= n)
  reserva ACTIVA, expira 15 min)                         reserva → LIBERADA
```

`disponible` y `reservado` se guardan por separado. Cada operación escribe una fila en `movimiento_stock`. Una reserva ACTIVA que pasa su `expira_en` la libera una tarea programada (CU09), así un checkout abandonado no deja stock bloqueado.

## Modelo de datos

```
stock (id_producto PK, disponible CHECK >= 0, reservado CHECK >= 0, actualizado_en)
reserva (id_reserva, id_venta, id_producto → stock, cantidad CHECK > 0,
         estado ENUM(ACTIVA, CONFIRMADA, LIBERADA), expira_en, creado_en, actualizado_en)
         UNIQUE(id_venta, id_producto) · índice (estado, expira_en)
movimiento_stock (id_movimiento, id_producto → stock, tipo ENUM(ENTRADA, RESERVA, CONFIRMACION, LIBERACION, AJUSTE),
                  cantidad, id_venta, motivo, creado_en)
v_conciliacion_stock   (vista: stock actual vs. stock esperado según el historial)
```

- `id_producto` es el id de ms-catalogo y `id_venta` el de ms-pedidos, **sin FK hacia otro schema** (*Database per Service*).
- Las CHECK son la última barrera contra la sobreventa: aunque el código falle, MySQL rechaza un stock negativo.
- Reglas de `movimiento_stock` (CHECK): cantidad positiva salvo en AJUSTE; `motivo` obligatorio en AJUSTE; `id_venta` obligatorio en RESERVA, CONFIRMACION y LIBERACION.
- **Conciliación (Objetivo 1):** `SELECT * FROM v_conciliacion_stock WHERE disponible_actual <> disponible_esperado OR reservado_actual <> reservado_esperado` tiene que devolver 0 filas.
- Semilla: 10 unidades de cada producto del catálogo, con su movimiento de ENTRADA.

## Guía de negocio

### Contadores de `stock`

| Columna | Significado |
|---|---|
| `disponible` | Unidades que un cliente puede comprar ahora. |
| `reservado` | Unidades apartadas para un cliente que está pagando. Siguen en el almacén, pero no se le venden a nadie más. |

### Estados de una reserva (`reserva.estado`)

| Valor | Significado | ¿Final? |
|---|---|---|
| `ACTIVA` | Se apartaron unidades para una venta en curso. Dura 15 min (`expira_en`). Mientras esté activa, nadie más puede comprarlas. | No |
| `CONFIRMADA` | El pago y el comprobante salieron bien: las unidades se vendieron y salen del inventario. | Sí |
| `LIBERADA` | La compra no se completó (otro ítem sin stock, falló el comprobante o venció el tiempo): las unidades vuelven a disponible. | Sí |

Transiciones válidas: `ACTIVA → CONFIRMADA` o `ACTIVA → LIBERADA`. Una reserva CONFIRMADA o LIBERADA no cambia más: si llega otra orden para ella, se responde OK sin tocar el stock (idempotencia).

### Tipos de movimiento (`movimiento_stock.tipo`)

Cada cambio de stock deja una fila. La cantidad siempre es positiva, salvo en AJUSTE.

| Valor | Cuándo ocurre | Efecto | `id_venta` | `motivo` |
|---|---|---|---|---|
| `ENTRADA` | Llegó mercadería del proveedor (ADMIN) | disponible + n | No | Opcional |
| `RESERVA` | Un checkout apartó unidades | disponible − n, reservado + n | **Sí** | Opcional |
| `CONFIRMACION` | La venta se cerró: lo apartado se entrega | reservado − n | **Sí** | Opcional |
| `LIBERACION` | La venta no se cerró: lo apartado vuelve al estante | disponible + n, reservado − n | **Sí** | Opcional |
| `AJUSTE` | Corrección tras un conteo físico (merma, rotura, error de carga). Puede ser negativa | disponible ± n | No | **Sí** |

**Regla de oro:** `stock` y `movimiento_stock` se actualizan en la **misma transacción**. Si no, `v_conciliacion_stock` muestra el descuadre.

### Referencias a otros servicios (`-- ref.`)

- **`stock.id_producto` → `db_catalogo.producto`.** La fila de stock se crea con la primera ENTRADA de un producto. Antes, validar que el producto exista (`GET /productos/{id}`).
- **`reserva.id_venta` y `movimiento_stock.id_venta` → `db_pedidos.venta`.** ms-pedidos crea la venta PENDIENTE primero y manda su `idVenta` al reservar. Inventario no valida la venta: confía en ms-pedidos y usa el `idVenta` para confirmar o liberar exactamente lo que se reservó. `UNIQUE(id_venta, id_producto)` impide reservar dos veces lo mismo.

### Conciliación (`v_conciliacion_stock`)

Compara el stock actual (la tabla `stock`) con el que resulta de sumar el historial (`movimiento_stock`). Ejemplo de un día:

| Hora | Qué pasa | Movimiento | Disponible | Reservado |
|---|---|---|---|---|
| 08:00 | Llegan 10 cámaras | ENTRADA 10 | 10 | 0 |
| 10:00 | Ana aparta 2 y va a pagar | RESERVA 2 | 8 | 2 |
| 10:01 | El pago de Ana sale bien | CONFIRMACION 2 | 8 | 0 |
| 11:00 | Luis aparta 3 | RESERVA 3 | 5 | 3 |
| 11:01 | Falla el comprobante de Luis | LIBERACION 3 | 8 | 0 |
| 15:00 | Aparece 1 cámara rota | AJUSTE −1 | 7 | 0 |

Si `stock` dice 7 y 0, cuadra. Una fila donde lo actual no coincide con lo esperado significa unidades que se movieron sin registro: es un error del código. La consulta debe devolver 0 filas.

## Estado actual

| Capa | Archivo | Estado |
|---|---|---|
| Entity | `Stock` (PK = `idProducto`), `Reserva` + `EstadoReserva`, `MovimientoStock` + `TipoMovimiento` | ✅ |
| Repository | `StockRepository.findById(idProducto)`, `ReservaRepository.findByIdVenta/findByEstadoAndExpiraEnBefore`, `MovimientoStockRepository.findByIdProductoOrderByCreadoEnDesc` | ✅ con test `@DataJpaTest` |
| DTO / Mapper | `StockResponse(idProducto, disponible, reservado)`, `StockMapper` | ✅ |
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

Los cuerpos y las respuestas de reservar, confirmar y liberar están en el contrato **C6** (sección "Dependencias con otros servicios").

## Reglas de negocio

- Reservar **todo o nada**: si un ítem no alcanza, no se reserva ninguno → **409 Conflict** con el `idProducto` que falló.
- Operaciones con `@Transactional`. Para evitar que dos checkouts reserven la misma última unidad: `@Lock(PESSIMISTIC_WRITE)` en la consulta del stock. La restricción `UNIQUE(id_venta, id_producto)` de `reserva` impide reservar dos veces lo mismo para una venta.
- Cantidades siempre `> 0` (`@Positive`).
- Liberar o confirmar una referencia dos veces no debe descontar dos veces (idempotencia: revisar el `estado` de la reserva de esa venta).

## Dependencias con otros servicios

El mapa completo está en [`ARRANQUE_PROYECTO.md`](../ARRANQUE_PROYECTO.md#quién-le-debe-a-quién).

### Lo que debo (me lo piden)

**C6 · Reservas para el checkout de ms-pedidos (integrante 4) — fin de la semana 3.** Es la dependencia más crítica del proyecto: sin esto no hay checkout.

| Endpoint | Cuerpo | Respuesta |
|---|---|---|
| `POST /stock/reservar` | `{ "idVenta": 42, "items": [ { "idProducto": 1, "cantidad": 2 } ] }` | **200** `{ "idVenta": 42, "expiraEn": "…" }` · **409** `ApiError` si algún ítem no alcanza (el mensaje dice cuál `idProducto`). Todo o nada |
| `POST /stock/confirmar` | `{ "idVenta": 42 }` | **200**. Confirma todas las reservas ACTIVAS de esa venta. Si ya estaban confirmadas, responde 200 sin hacer nada |
| `POST /stock/liberar` | `{ "idVenta": 42 }` | **200**. Libera todas las reservas ACTIVAS de esa venta. Si no hay ninguna, responde 200 igual (idempotente) |

Confirmar y liberar solo reciben `idVenta`, porque las cantidades ya quedaron guardadas en `reserva` al reservar.

### Lo que necesito

| Contrato | Quién lo entrega | Semana | Para qué lo uso |
|---|---|---|---|
| **C5** · `GET /productos/{id}` (404 si no existe) | Integrante 3 | 2 | Validar el producto al registrar una ENTRADA |
| **C8** · `idVenta` real y token reenviado | Integrante 4 | 4 | Reservar, confirmar y liberar |
| **C2** · `JwtAuthFilter` | Integrante 2 | 3 | Proteger entrada, ajuste y movimientos (solo ADMIN) |

### Cómo avanzar sin esperar

`id_venta` no tiene FK hacia pedidos, así que **puedes probar reservar, confirmar y liberar con Postman usando `idVenta` inventados** (1, 2, 3…), directo en `localhost:8083`. No necesitas que ms-pedidos exista.

## Pendientes por semana

- [ ] **Semana 2–3**: `reservar`, `confirmar`, `liberar` con su `Reserva` y su movimiento; tarea `@Scheduled` que libera las reservas vencidas; `GET /movimientos`; entrada y ajuste de stock (ADMIN); `StockInsuficienteException` → 409.
- [ ] **Semana 4**: acordar el contrato con ms-pedidos y probar el checkout juntos.
- [ ] **Semana 5**: semilla de stock para los 200 productos (coordinar con el integrante 3).
- [ ] **Semana 6**: pantalla `/admin/stock` en `web-client`.
- [ ] **Semana 7**: tests Mockito de `StockService` (reserva OK, stock insuficiente, liberar devuelve el disponible) y `postman/ms-inventario.postman_collection.json`.

## Cómo correrlo

```bash
docker compose up -d ms-inventario               # todo en Docker (Eureka + ms-inventario)
# o, para depurar desde el IDE o Maven:
docker compose up -d ms-discovery
./mvnw -q install -DskipTests                    # una vez, y cada vez que cambie common-security
./mvnw -pl ms-inventario spring-boot:run         # lee AIVEN_* y DB_PASS_INVENTARIO del .env
curl localhost:8083/stock/1
```
