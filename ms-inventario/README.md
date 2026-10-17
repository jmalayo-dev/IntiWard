# ms-inventario · Stock y movimientos

| | |
|---|---|
| **Responsable** | Arévalo · rama `b-arevalo` |
| **Puerto** | 8083 |
| **Nombre en Eureka** | `ms-inventario` |
| **Schema MySQL** | `db_inventario` (usuario `ms_inventario`) · script `infra/mysql/03-inventario.sql` |
| **Rutas en el gateway** | `/stock/**`, `/movimientos/**` |
| **Pantallas web** | Gestión de stock (admin) |

## Para qué existe

Es el dueño de **cuántas unidades hay** de cada producto y de **todo lo que le pasó a ese número**. La tabla de movimientos es lo que hace medible el Objetivo 2 del informe (*reducir errores logísticos en 30 %*): sin historial no hay nada que auditar.

Es el servicio más pequeño (3 tablas y una vista).

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

| Método | Ruta | Quién | Estado | Bloqueo |
|---|---|---|---|---|
| GET | `/stock/{idProducto}` | Público | 🟡 falta `actualizadoEn` y responder 0 sin fila | 🟢 |
| GET | `/stock?ids=1,2,3` | Público | ⬜ nuevo | ⛔ bloquea el filtro `conStock` de ms-catalogo |
| POST | `/stock/entrada` | ADMIN | ⬜ | 🔗 ms-catalogo |
| PUT | `/stock/{idProducto}` | ADMIN (ajuste) | ⬜ | 🔗 ms-catalogo |
| GET | `/movimientos?idProducto=` | ADMIN | ⬜ | 🟢 |
| POST | `/stock/reservar` | Interno (ms-pedidos) | ⬜ | ⛔ bloquea el checkout de ms-pedidos |
| POST | `/stock/confirmar` | Interno (ms-pedidos) | ⬜ | ⛔ bloquea el checkout de ms-pedidos |
| POST | `/stock/liberar` | Interno (ms-pedidos) | ⬜ | ⛔ bloquea el checkout de ms-pedidos |

Los cuerpos y las respuestas de reservar, confirmar y liberar están en el contrato **C6** (sección "Dependencias con otros servicios"). El resto está en "Contrato con el cliente web".

## Contrato con el cliente web

La app web vive en otro repositorio (`DSW_II_PROYECTO_WEB`). Su archivo `docs/CONTRATO_ENDPOINTS.md` define todo lo que necesita del backend; esta sección copia la parte de **este** servicio. Hoy la web funciona con datos simulados (mocks) que responden exactamente lo que dice aquí: **si tu endpoint responde igual, la web se conecta sin cambios.**

Cuando termines un endpoint, márcalo en [`docs/SEGUIMIENTO_ENDPOINTS.csv`](../docs/SEGUIMIENTO_ENDPOINTS.csv): `SI` en `cumplido` y la fecha en `fecha_cumplimiento` (`AAAA-MM-DD`).

**Reglas comunes a todos los servicios:**
- Rutas relativas al gateway (`http://localhost:8080`). Toda ruta que no sea pública recibe `Authorization: Bearer <token>`.
- **Tipos:** ids `Long`; montos `BigDecimal` con 2 decimales (en el JSON van como número: `199.9`); fechas `LocalDateTime` en ISO sin zona (`"2026-10-15T14:35:00"`); filtros de fecha `LocalDate` (`"2026-10-15"`); enums como texto en mayúsculas (`ADMIN`, `BOLETA`, `CONFIRMADA`).
- **Nunca borrado físico:** `DELETE` pone `activo = false`.
- **Errores:** siempre con `ApiError`, y **`mensaje` siempre presente** (la web lo muestra tal cual). Los errores de validación van en `detalles`, uno por campo, con el formato `"campo: mensaje"` (ya lo hace el `GlobalExceptionHandler`); la web parte el texto por `:` para marcar el campo.
- **Vistas:** los números 4.x (tienda) y 5.x (panel de administración) son las vistas del prototipo web ([`docs/PROTOTIPO_WEB.md`](../docs/PROTOTIPO_WEB.md)).
- **Bloqueo:** ⛔ **BLOQUEANTE** = otro integrante no puede terminar lo suyo sin este endpoint · 🔗 **DEPENDIENTE** = necesita un endpoint de otro servicio · 🟢 **NO DEPENDIENTE** = se hace solo.

### Objeto `Stock` (`StockResponse`)

```json
{ "idProducto": 1, "disponible": 12, "reservado": 2, "actualizadoEn": "2026-10-15T14:35:00" }
```

| Atributo | Tipo | Para qué lo usa la web |
|---|---|---|
| `idProducto` | `Long` | Une el stock con su producto |
| `disponible` | `Integer` | Unidades que se pueden comprar ya. **Lo reservado ya está descontado** (al reservar: `disponible − n`, `reservado + n`) |
| `reservado` | `Integer` | Unidades apartadas por compras en curso; columna en 5.5 |
| `actualizadoEn` | `LocalDateTime` · **nuevo** | Fecha del último cambio; columna "Actualizado" en 5.5. La columna `actualizado_en` ya existe: solo falta agregarla al DTO |

La web muestra "Agotado" con 0 unidades y "Pocas unidades" con 5 o menos, deshabilita "Agregar al carrito" y avisa en el carrito si la cantidad pedida supera lo disponible.

### Objeto `Movimiento`

```json
{ "id": 10, "idProducto": 1, "tipo": "RESERVA", "cantidad": 2, "idVenta": 43, "motivo": null, "creadoEn": "2026-10-15T14:35:00" }
```

| Atributo | Tipo | Para qué lo usa la web |
|---|---|---|
| `id` | `Long` | Clave de la fila |
| `idProducto` | `Long` | Nombre del producto y filtro por producto |
| `tipo` | `String`: `ENTRADA` · `RESERVA` · `CONFIRMACION` · `LIBERACION` · `AJUSTE` | Insignia de color y filtro por tipo |
| `cantidad` | `Integer` | Unidades. En `AJUSTE` lleva signo (`-3` resta); en los demás, siempre positiva |
| `idVenta` | `Long` o `null` | Enlace a la venta en reservas, confirmaciones y liberaciones |
| `motivo` | `String` o `null` | Razón escrita por el admin en entradas y ajustes |
| `creadoEn` | `LocalDateTime` | Columna de fecha y orden |

**Producto sin fila de stock (decidido el 2026-10-17):** catálogo **no** avisa a inventario cuando crea un producto. Un producto sin entradas **tiene stock 0**:
- `GET /stock/{id}` responde **200** `{ "idProducto": 7, "disponible": 0, "reservado": 0, "actualizadoEn": null }`, no 404.
- `GET /stock?ids=` devuelve una entrada por cada id pedido, en 0 si no tiene fila. Sin `ids`, solo las filas que existen.
- La primera ENTRADA o el primer ajuste crea la fila (antes se valida que el producto exista en catálogo).
- Reservar un producto sin fila responde 409, igual que si no alcanzara.

### `GET /stock/{idProducto}` · Público · 🟡 · 🟢

**Para qué:** la disponibilidad de un producto y el tope del selector de cantidad del detalle.
**Vistas:** 4.3 Detalle de producto.

Responde **200**: `Stock`. Si el producto aún no tiene entradas, **200 con todo en 0** (`actualizadoEn: null`), no 404.
Falta: `actualizadoEn` y la respuesta en 0.

### `GET /stock?ids=1,2,3` · Público · ⬜ nuevo · ⛔ bloquea ms-catalogo

**Para qué:** pedir el stock de toda una grilla en **una sola llamada**, en vez de una por tarjeta. **Sin `ids` devuelve todos** (tabla de stock y campana de agotados).
**Vistas:** 4.1 Inicio, 4.2 Catálogo y 4.3 Detalle (insignias de la grilla) · 4.6 Carrito y 4.7 Checkout (aviso de stock insuficiente) · 5.1 Resumen · 5.2 Productos · 5.5 Stock · campana de agotados del panel (sin `ids`).

| Parámetro | Tipo | Regla |
|---|---|---|
| `ids` | `List<Long>` | Opcional. `ids=1,2,3` |

Responde **200**: `List<Stock>` con **una entrada por cada id pedido** (en 0 si no tiene fila). Sin `ids`, todas las filas que existen.

**Ruta pública:** `GET /stock/*` **no** cubre `/stock` sin nada después. En el gateway ya está agregada `GET /stock` (Mathias, 2026-10-17); **falta en tu `application.yml`**: agrega `- GET /stock` en `intiguard.security.rutas-publicas`.

ms-catalogo lo necesita para su filtro `conStock`.

### `POST /stock/entrada` · ADMIN · ⬜ · 🔗 ms-catalogo

**Para qué:** registrar mercadería que llegó del proveedor.
**Vistas:** 5.5 Stock (ventana "Registrar entrada").

| Envía | Tipo | Regla |
|---|---|---|
| `idProducto` | `Long` | Producto que recibe mercadería; debe existir en catálogo |
| `cantidad` | `Integer` | Mayor que 0; **se suma** a `disponible` |
| `motivo` | `String` | Opcional (por ejemplo "Compra a proveedor") |

Responde **200**: `Stock` actualizado. Registra un movimiento `ENTRADA`. Si el producto aún no tenía fila de stock, la crea. **400**. **404** si el producto no existe.

Para validar el producto se llama a ms-catalogo `GET /productos/{id}` (contrato C5, de César). **Mientras no esté:** haz la entrada sin validar y agrega la validación al final.

### `PUT /stock/{idProducto}` · ADMIN · ⬜ · 🔗 ms-catalogo

**Para qué:** corregir el stock después de un conteo físico (rotura, pérdida, error de carga).
**Vistas:** 5.5 Stock (ventana "Ajustar").

| Envía | Tipo | Regla |
|---|---|---|
| `disponible` | `Integer` | **Nuevo valor absoluto** (no una diferencia); 0 o más |
| `motivo` | `String` | Obligatorio (por ejemplo "Conteo físico") |

Responde **200**: `Stock`. Registra un `AJUSTE` con la **diferencia con signo** (nuevo − anterior). Si el producto no tenía fila, la crea (validando con ms-catalogo `GET /productos/{id}` que el producto exista, como en la entrada). **400** si falta el motivo o el valor es negativo. **404** si el producto no existe.

Cuidado: si el valor nuevo es igual al actual, **no registres movimiento**: la base rechaza un movimiento con cantidad 0 (`ck_movimiento_cantidad`).

### `GET /movimientos` · ADMIN · ⬜ · 🟢

**Para qué:** el historial auditable de todo lo que le pasó al stock.
**Vistas:** 5.6 Movimientos.

| Parámetro | Tipo | Regla |
|---|---|---|
| `idProducto` | `Long` | Opcional; sin él devuelve todos |

Responde **200**: `List<Movimiento>`, **del más reciente al más antiguo**.

### `POST /stock/reservar` · `/confirmar` · `/liberar` · Internos · ⬜ · ⛔ bloquean ms-pedidos

**Para qué:** apartar, vender o devolver las unidades durante el checkout. La web **nunca** los llama (el gateway responde 403 desde afuera), pero sin ellos no hay compra.
**Vistas (indirectas):** 4.7 Checkout · 4.8 Compra confirmada · 4.9 Compra no completada.

Cuerpos y respuestas: contrato **C6**, más abajo. Lo que agrega la web: en el **409** de reservar, pon el producto que falló en `detalles` con el formato `"idProducto: 7"`. ms-pedidos lo reenvía tal cual y la web lo usa para mostrar el producto agotado en 4.9.

## Reglas de negocio

- Reservar **todo o nada**: si un ítem no alcanza, no se reserva ninguno → **409 Conflict** con el `idProducto` que falló.
- Operaciones con `@Transactional`. Para evitar que dos checkouts reserven la misma última unidad: `@Lock(PESSIMISTIC_WRITE)` en la consulta del stock. La restricción `UNIQUE(id_venta, id_producto)` de `reserva` impide reservar dos veces lo mismo para una venta.
- Cantidades siempre `> 0` (`@Positive`).
- Liberar o confirmar una referencia dos veces no debe descontar dos veces (idempotencia: revisar el `estado` de la reserva de esa venta).

## Dependencias con otros servicios

El mapa completo está en [`ARRANQUE_PROYECTO.md`](../ARRANQUE_PROYECTO.md#quién-le-debe-a-quién).

### Lo que debo (me lo piden)

**C6 · Reservas para el checkout de ms-pedidos (Evelyn) — fin de la semana 3.** Es la dependencia más crítica del proyecto: sin esto no hay checkout.

| Endpoint | Cuerpo | Respuesta |
|---|---|---|
| `POST /stock/reservar` | `{ "idVenta": 42, "items": [ { "idProducto": 1, "cantidad": 2 } ] }` | **200** `{ "idVenta": 42, "expiraEn": "…" }` · **409** `ApiError` si algún ítem no alcanza: el `mensaje` dice cuál y `detalles` trae `["idProducto: 7"]`. Todo o nada |
| `POST /stock/confirmar` | `{ "idVenta": 42 }` | **200**. Confirma todas las reservas ACTIVAS de esa venta. Si ya estaban confirmadas, responde 200 sin hacer nada |
| `POST /stock/liberar` | `{ "idVenta": 42 }` | **200**. Libera todas las reservas ACTIVAS de esa venta. Si no hay ninguna, responde 200 igual (idempotente) |

Confirmar y liberar solo reciben `idVenta`, porque las cantidades ya quedaron guardadas en `reserva` al reservar.

### Lo que necesito

| Contrato | Quién lo entrega | Semana | Para qué lo uso |
|---|---|---|---|
| **C5** · `GET /productos/{id}` (404 si no existe) | César | 2 | Validar el producto en la primera ENTRADA o ajuste (cuando aún no tiene fila de stock) |
| **C8** · `idVenta` real y token reenviado | Evelyn | 4 | Reservar, confirmar y liberar |
| **C2** · `JwtAuthFilter` | Galarza | 3 | Proteger entrada, ajuste y movimientos (solo ADMIN) |

### Cómo avanzar sin esperar

`id_venta` no tiene FK hacia pedidos, así que **puedes probar reservar, confirmar y liberar con Postman usando `idVenta` inventados** (1, 2, 3…), directo en `localhost:8083`. No necesitas que ms-pedidos exista.

## Pendientes por semana

- [ ] **Semana 2–3**: `reservar`, `confirmar`, `liberar` con su `Reserva` y su movimiento; tarea `@Scheduled` que libera las reservas vencidas; `GET /movimientos`; entrada y ajuste de stock (ADMIN); `StockInsuficienteException` → 409.
- [ ] **Semana 4**: acordar el contrato con ms-pedidos y probar el checkout juntos.
- [ ] **Semana 5**: semilla de stock para los 200 productos (coordinar con César).
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
