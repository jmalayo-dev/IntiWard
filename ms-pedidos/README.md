# ms-pedidos · Carrito, checkout y ventas

| | |
|---|---|
| **Responsable** | Evelyn · rama `b-guerrero` |
| **Puerto** | 8084 |
| **Nombre en Eureka** | `ms-pedidos` |
| **Schema MySQL** | `db_pedidos` (usuario `ms_pedidos`) · script `infra/mysql/04-pedidos.sql` |
| **Rutas en el gateway** | `/carrito/**`, `/pedidos/**` |
| **Tarea transversal** | Clientes Feign, Resilience4j y pruebas de integración del checkout |
| **Pantallas web** | Carrito, confirmación de compra, mis pedidos, listado de ventas |

## Para qué existe

Es el **orquestador** del sistema: el único punto donde se encadenan cuatro servicios. Por eso es el **más crítico** del cronograma: **la semana 4 es la bisagra**. Si el checkout no funciona al cerrarla, se recorta el cliente web, no esto.

## El checkout (lo más importante de este servicio)

```
POST /pedidos/checkout   (usuario sacado del token con usuarioActual.id())
 1. Leer el carrito del usuario                       (local)
 2. Por cada ítem: precio vigente                     → ms-catalogo     GET  /productos/{id}
 3. Guardar Venta PENDIENTE + DetalleVenta            (local; así existe el idVenta)
 4. Reservar todo el stock con ese idVenta            → ms-inventario   POST /stock/reservar
 5. Emitir comprobante con ese idVenta                → ms-comprobantes POST /comprobantes
 6. Confirmar stock                                   → ms-inventario   POST /stock/confirmar
 7. Venta CONFIRMADA, vaciar carrito  (el comprobante se busca en ms-comprobantes por idVenta)

 Si 4 falla  → Venta ANULADA, responder 409 "Stock insuficiente".
 Si 5 falla o tarda > 2 s (circuit breaker) →
      COMPENSAR: POST /stock/liberar, Venta ANULADA, responder 503 con un mensaje claro.
```

Así nunca queda **una venta a medias con el stock descontado**. Esto es la **Saga por compensación** + **Circuit Breaker**, dos patrones para nombrar en la sustentación.

## Modelo de datos

```
carrito (id_carrito, id_usuario UNIQUE, creado_en, actualizado_en)
item_carrito (id_item, id_carrito → carrito, id_producto, cantidad CHECK > 0)    UNIQUE(id_carrito, id_producto)
venta (id_venta, id_usuario, estado ENUM(PENDIENTE, CONFIRMADA, ANULADA), tipo_comprobante ENUM(BOLETA, FACTURA),
       subtotal, igv, total, fecha_venta, actualizado_en)       CHECK total = subtotal + igv
detalle_venta (id_detalle_venta, id_venta → venta, id_producto, nombre_producto, precio_unitario, cantidad)
              UNIQUE(id_venta, id_producto)
```

**Montos:** los precios del catálogo incluyen IGV. `total` = suma de precio × cantidad; `subtotal` = total / 1.18; `igv` = total − subtotal. Usar `venta.asignarMontos(total)`, que respeta la CHECK de la BD. El importe de cada línea no se guarda: `DetalleVenta.getImporte()`.

El carrito está **en base de datos** (en el informe vivía en la sesión): con servicios sin estado tiene que sobrevivir a un reinicio. `detalle_venta` **copia** nombre y precio: si mañana cambia el catálogo, el pedido histórico no.

## Guía de negocio

### Estados de una venta (`venta.estado`)

| Valor | Significado | ¿Final? |
|---|---|---|
| `PENDIENTE` | El cliente confirmó la compra y el checkout está en curso: ya hay venta, pero todavía no hay comprobante. | No |
| `CONFIRMADA` | Stock confirmado y comprobante emitido. La venta es firme y se vacía el carrito. | Sí |
| `ANULADA` | El checkout falló (sin stock, comprobante caído o lento) y se compensó: el stock se liberó. Se conserva como registro de un intento fallido. | Sí |

Transiciones válidas: `PENDIENTE → CONFIRMADA` o `PENDIENTE → ANULADA`. Una venta que sigue PENDIENTE más de 15 min se anula, porque su reserva en inventario ya venció.

### Tipo de comprobante (`venta.tipo_comprobante`)

| Valor | Para quién | Datos que exige |
|---|---|---|
| `BOLETA` | Consumidor final (valor por defecto) | DNI opcional |
| `FACTURA` | Empresa que necesita crédito fiscal | RUC y razón social |

Se decide en el checkout y se manda igual a ms-comprobantes.

### Montos

`total` = suma de `precio_unitario × cantidad` (los precios incluyen IGV) · `subtotal = total / 1.18` · `igv = total − subtotal`. Usar `venta.asignarMontos(total)`.

### Referencias a otros servicios (`-- ref.`)

- **`carrito.id_usuario` y `venta.id_usuario` → `db_auth.usuario`.** Se toma **siempre del JWT**, nunca del cuerpo de la petición: así un cliente no puede comprar ni ver pedidos a nombre de otro.
- **`item_carrito.id_producto` → `db_catalogo.producto`.** Al agregar al carrito, validar con ms-catalogo que el producto exista y esté activo. No guardar el precio aquí: se pide en el checkout.
- **`detalle_venta.id_producto` → `db_catalogo.producto`.** Se copian `nombre_producto` y `precio_unitario` al comprar: si el catálogo cambia después, el pedido histórico no cambia.

## Estado actual

| Capa | Archivo | Estado |
|---|---|---|
| Entity | `Carrito`, `ItemCarrito`, `Venta` (con `asignarMontos`), `DetalleVenta`, `EstadoVenta`, `TipoComprobante` | ✅ |
| Repository | `CarritoRepository.findByIdUsuario`, `VentaRepository.findByIdUsuarioOrderByFechaVentaDesc` | ✅ con test `@DataJpaTest` (cascada venta→detalles y desglose del IGV) |
| DTO / Mapper | `VentaResponse`, `VentaMapper` | ✅ |
| Service / Controller | `PedidoService`, `PedidoController` | ✅ |
| Feign | `@EnableFeignClients`, `client/CatalogoClient` + `ProductoDto` (ejemplo listo) | ✅ base |
| Dependencias | `spring-cloud-starter-openfeign`, `spring-cloud-starter-circuitbreaker-resilience4j` | ✅ en el pom |
| Carrito | `CarritoService` + `CarritoController` (solo CLIENTE, precio y datos vigentes desde ms-catalogo) | ✅ |
| Checkout | `CheckoutService` (saga: PENDIENTE → reservar → comprobante con circuit breaker de 2 s → confirmar → CONFIRMADA; compensa liberando y anulando), `VentaRegistro` (pasos locales en su propia transacción) | ✅ |
| Feign | `CatalogoClient`, `InventarioClient` (C6), `ComprobantesClient` (C7); el token del cliente se reenvía como parámetro | ✅ |
| Resilience4j | `CircuitBreakerConfig`: TimeLimiter 2 s y circuito que se abre con 50 % de fallos en 10 llamadas | ✅ |
| Consultas | `GET /pedidos` y `GET /pedidos/{id}` con `detalles` (`@EntityGraph`) y control de dueño | ✅ |
| Tarea programada | `TareaVentasPendientes`: anula las ventas PENDIENTE de más de 15 min (CU09) | ✅ |
| Tests | `PedidosIntegracionTest` (8, HTTP + H2 con los tres servicios simulados: éxito, sin stock, comprobantes caído y lento) | ✅ |

## Endpoints objetivo

| Método | Ruta | Quién | Estado | Bloqueo |
|---|---|---|---|---|
| GET | `/carrito` | CLIENTE | ✅ | 🔗 ms-catalogo |
| POST | `/carrito/items` | CLIENTE | ✅ | 🔗 ms-catalogo |
| PUT | `/carrito/items/{idProducto}` | CLIENTE | ✅ | 🔗 ms-catalogo |
| DELETE | `/carrito/items/{idProducto}` | CLIENTE | ✅ | 🔗 ms-catalogo |
| POST | `/pedidos/checkout` | CLIENTE | ✅ | 🔗 ms-catalogo, ms-inventario, ms-comprobantes |
| GET | `/pedidos` | ADMIN: todas · CLIENTE: las suyas | ✅ | 🟢 |
| GET | `/pedidos/{id}` | Dueño o ADMIN | ✅ | ⛔ bloquea el PDF de ms-comprobantes |

El detalle de cada uno está en "Contrato con el cliente web".

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

### Objeto `Carrito`

```json
{ "items": [{ "idProducto": 1, "nombre": "Cámara IP Wi-Fi 1080p", "marca": "Hikvision", "precio": 159.9,
              "imagenUrl": "/productos/camara-ip.svg", "cantidad": 2, "importe": 319.8 }],
  "total": 319.8 }
```

Vacío: `{ "items": [], "total": 0 }`.

| Atributo | Tipo | Para qué lo usa la web |
|---|---|---|
| `items` | `List<ItemCarrito>` | Una línea por producto (sin duplicados) |
| `items[].idProducto` | `Long` | Cambiar cantidad, quitar y consultar su stock |
| `items[].nombre` | `String` | Nombre en la línea |
| `items[].marca` | `String` | Subtítulo de la línea |
| `items[].precio` | `BigDecimal` | Precio unitario **vigente** |
| `items[].imagenUrl` | `String` o `null` | Miniatura |
| `items[].cantidad` | `Integer` | Selector de cantidad y número del ícono del carrito |
| `items[].importe` | `BigDecimal` | `precio × cantidad` de la línea |
| `total` | `BigDecimal` | Total con IGV. La web calcula subtotal e IGV para el resumen |

`item_carrito` solo guarda `id_producto` y `cantidad`: **nombre, marca, precio e imagen se piden a ms-catalogo** (`GET /productos/{id}`) cada vez que se arma la respuesta. Por eso todo el carrito depende de César.

### Objeto `Venta` (`VentaResponse`)

```json
{ "id": 43, "idUsuario": 1, "fechaVenta": "2026-10-15T14:35:00", "estado": "CONFIRMADA", "tipoComprobante": "BOLETA",
  "subtotal": 271.02, "igv": 48.78, "total": 319.8,
  "detalles": [{ "idProducto": 1, "nombreProducto": "Cámara IP Wi-Fi 1080p", "precioUnitario": 159.9, "cantidad": 2, "importe": 319.8 }] }
```

| Atributo | Tipo | Para qué lo usa la web |
|---|---|---|
| `id` | `Long` | Número de pedido (`#00043`) y enlace al detalle |
| `idUsuario` | `Long` | Nombre del cliente en el panel (cruzado con `/usuarios`) |
| `fechaVenta` | `LocalDateTime` | Columna de fecha, filtros y gráficos por día |
| `estado` | `String`: `PENDIENTE` · `CONFIRMADA` · `ANULADA` | Insignia de estado; los reportes y el resumen solo cuentan confirmadas |
| `tipoComprobante` | `String`: `BOLETA` · `FACTURA` | Insignia y filtro por tipo |
| `subtotal` | `BigDecimal` | Monto sin IGV |
| `igv` | `BigDecimal` | IGV 18 % |
| `total` | `BigDecimal` | Total cobrado; base de ventas, ticket promedio y gráficos |
| `detalles` | `List<DetalleVenta>` · **nuevo** | Productos del pedido. Debe venir **también en `GET /pedidos`** (miniaturas de 4.10 y "más vendidos" de 5.1 y 5.9) |
| `detalles[].idProducto` | `Long` | Miniatura y enlace al producto |
| `detalles[].nombreProducto` | `String` | Nombre guardado al momento de la venta (aunque luego cambie) |
| `detalles[].precioUnitario` | `BigDecimal` | Precio pagado por unidad |
| `detalles[].cantidad` | `Integer` | Unidades; se suman para "productos más vendidos" |
| `detalles[].importe` | `BigDecimal` | `precioUnitario × cantidad` |

`docCliente` y `nombreRazonSocial` **no** van en la venta: viven en el comprobante.

### Reglas del carrito

- Todas las rutas del carrito son **solo para CLIENTE**: un ADMIN recibe **403** (`@PreAuthorize("hasRole('CLIENTE')")`).
- El carrito es el del usuario del token (`usuarioActual.id()`), nunca un id del cuerpo.
- **Mientras ms-catalogo no tenga `activo`:** arma el carrito con un `CatalogoClient` simulado en los tests (Mockito) y completa la validación de "producto activo" al final.

### `GET /carrito` · CLIENTE · ✅ · 🔗 ms-catalogo

**Para qué:** ver el carrito y mostrar el número del ícono del carrito.
**Vistas:** 4.6 Carrito · 4.7 Checkout · encabezado de la tienda (número del ícono).

Responde **200**: `Carrito` (vacío si el cliente no tiene).

### `POST /carrito/items` · CLIENTE · ✅ · 🔗 ms-catalogo

**Para qué:** el botón "Agregar al carrito".
**Vistas:** 4.1 Inicio · 4.2 Catálogo · 4.3 Detalle de producto.

| Envía | Tipo | Regla |
|---|---|---|
| `idProducto` | `Long` | Debe existir y estar activo (se consulta a ms-catalogo) |
| `cantidad` | `Integer` | Mayor que 0, por defecto 1. **Si el producto ya estaba en el carrito, se suma** |

Responde **201**: el `Carrito` completo. **400**. **404** si el producto no existe o está inactivo.

### `PUT /carrito/items/{idProducto}` · CLIENTE · ✅ · 🔗 ms-catalogo

**Para qué:** el selector de cantidad del carrito.
**Vistas:** 4.6 Carrito.

Envía `{ "cantidad": Integer }`: **valor absoluto**, mayor que 0.
Responde **200**: `Carrito`. **400**. **404** si el producto no está en el carrito.

### `DELETE /carrito/items/{idProducto}` · CLIENTE · ✅ · 🔗 ms-catalogo

**Para qué:** quitar un producto del carrito.
**Vistas:** 4.6 Carrito.

Responde **200**: el `Carrito` sin esa línea. Si el producto no estaba, no hace nada y responde igual.

### `POST /pedidos/checkout` · CLIENTE · ✅ · 🔗 ms-catalogo, ms-inventario, ms-comprobantes

**Para qué:** confirmar la compra del carrito con boleta o factura.
**Vistas:** 4.7 Checkout · 4.8 Compra confirmada (si responde 201) · 4.9 Compra no completada (si responde 409 o 503).

| Envía | Tipo | Regla |
|---|---|---|
| `tipoComprobante` | `String`: `BOLETA` · `FACTURA` | Obligatorio |
| `docCliente` | `String` o `null` | Boleta: DNI de 8 dígitos **opcional** (`null` o vacío; si llega vacío, guárdalo como `null`). Factura: RUC de 11 dígitos, obligatorio |
| `nombreRazonSocial` | `String` | **Obligatorio**: nombre del cliente o razón social |

El flujo es el de la sección "El checkout" de este README (venta PENDIENTE → reservar todo o nada → comprobante → confirmar → CONFIRMADA y carrito vacío). **Al emitir el comprobante, manda también el `idUsuario` de la venta**: ms-comprobantes lo guarda para saber de quién es cada comprobante.

Respuestas:
- **201**: la `Venta` CONFIRMADA **con `detalles`**. La web va a `/compra/{id}/confirmada`.
- **400**: `detalles` con `docCliente: …` / `nombreRazonSocial: …`, o `mensaje` `"El carrito está vacío"`.
- **409** `"Stock insuficiente para <nombre del producto>"` con `detalles: ["idProducto: 7"]`. **La web usa ese id** para mostrar el producto agotado en 4.9. La venta queda ANULADA. ms-inventario ya devuelve ese `detalles` en su 409: reenvíalo.
- **503** `"El servicio de comprobantes no está disponible…"`: se libera la reserva y la venta queda ANULADA.


### `GET /pedidos` · Con sesión · ✅ · 🟢

**Para qué:** "Mis pedidos" del cliente, y las ventas, el resumen y los reportes del panel.
**Vistas:** 4.10 Mis pedidos · 5.1 Resumen (ventas, gráficos y más vendidos) · 5.7 Ventas · 5.9 Reportes (ventas por día y más vendidos).

Responde **200**: `List<Venta>` **con `detalles`**, de la más nueva a la más antigua. Un CLIENTE recibe **solo las suyas** (`usuarioActual.id()`); el ADMIN, todas.

### `GET /pedidos/{id}` · Dueño o ADMIN · ✅ · ⛔ bloquea el PDF de ms-comprobantes

**Para qué:** el detalle de un pedido, y los productos que se imprimen en el comprobante.
**Vistas:** 4.8 Compra confirmada · 4.11 Detalle de mi pedido · 4.12 Comprobante imprimible (productos del documento) · detalle de venta del panel · 5.8 Comprobantes (vista previa).

Responde **200**: `Venta` con `detalles`. **403** si es de otro cliente. **404**.

## Dependencias con otros servicios

ms-pedidos es el servicio que **más depende de otros**. El mapa completo está en [`ARRANQUE_PROYECTO.md`](../ARRANQUE_PROYECTO.md#quién-le-debe-a-quién).

### Lo que necesito (me lo deben)

| Contrato | Quién lo entrega | Semana | Para qué lo uso |
|---|---|---|---|
| **C1** · el JWT trae el claim `idUsuario` ✅ | Galarza | 2 | Saber de quién es el carrito y la venta |
| **C2** · `UsuarioActual.id()` en `common-security` ✅ | Galarza | 3 | Leer el `idUsuario` del token en el controller |
| **C5** · `GET /productos/{id}` → `{id, nombre, precio, activo, …}`; 404 si no existe | César | 2 | Validar al agregar al carrito; precio vigente en el checkout |
| **C6** · `POST /stock/reservar`, `/confirmar`, `/liberar` | Arévalo | fin de la 3 | Pasos 4 y 6 del checkout y la compensación |
| **C7** · `POST /comprobantes` (idempotente por `idVenta`, responde en < 2 s) | Galarza | fin de la 3 | Paso 5 del checkout |

### Lo que debo yo

| Contrato | A quién | Semana | Detalle |
|---|---|---|---|
| **C8** · `idVenta` de una venta PENDIENTE en cada reserva y comprobante (al comprobante, también su `idUsuario`) | Arévalo y Galarza | 4 | La venta se guarda **antes** de reservar. Ante cualquier fallo posterior a la reserva, **siempre** se llama a `/stock/liberar` |
| **C8** · reenviar el token en las llamadas Feign | Arévalo y Galarza | 4 | Un `RequestInterceptor` que copie la cabecera `Authorization` de la petición del cliente |

### Cómo avanzar sin esperar

- **Carrito (semanas 2–3):** no depende de nadie, salvo C5. Toma el dueño del carrito con `usuarioActual.id()` (ya existe) y pruébalo con un token de desarrollo: `curl -H "Authorization: Bearer $(./infra/scripts/token-dev.sh 7 CLIENTE)" localhost:8084/carrito`.
- **Checkout:** escribe `InventarioClient` y `ComprobantesClient` con los contratos C6 y C7 tal como están en los READMEs de ms-inventario (Arévalo) y ms-comprobantes (Galarza), y prueba `CheckoutService` con **Mockito, simulando los clientes**. No necesitas que los endpoints existan para tener el checkout listo y probado.

## Pendientes por semana

### Semana 2–3
- [ ] `CarritoService` + `CarritoController` (`/carrito/items`), con validaciones `@Positive cantidad`.
- [ ] Obtener el usuario con `usuarioActual.id()` (de `common-security`, ya disponible). Para probar sin login: `./infra/scripts/token-dev.sh 7 CLIENTE`.

### Semana 4 · Checkout de punta a punta (entrega bisagra)
- [ ] `client/InventarioClient` (`reservar`, `confirmar`, `liberar`) y `client/ComprobantesClient` (`emitir`). Acordar los DTO con Arévalo y Galarza.
- [ ] `CheckoutService.checkout()` con los pasos 1–7.
- [ ] Una compra completa desde Postman a través del gateway.

### Semana 5 · Falla bien
- [ ] Circuit breaker en `ComprobantesClient`. En `application.yml`:
  ```yaml
  spring.cloud.openfeign.circuitbreaker.enabled: true
  resilience4j.timelimiter.instances.ms-comprobantes.timeout-duration: 2s
  ```
  y un `fallback`/`fallbackFactory` que dispare la compensación.
- [ ] Compensación: liberar stock y marcar la venta ANULADA. Probar apagando el contenedor: `docker compose stop ms-comprobantes`.

### Semana 6 · Pantallas web
- [ ] `/carrito`, `/checkout`, `/mis-pedidos`, `/admin/ventas` en `web-client`.

### Semana 7 · Pruebas
- [ ] Tests de `CheckoutService` con Mockito, mockeando los 3 clientes Feign: camino feliz, sin stock, comprobante falla → se llama `liberar`.
- [ ] `postman/ms-pedidos.postman_collection.json` + `postman/flujo-completo.postman_collection.json` (login → carrito → checkout → comprobante).

## Reglas de diseño

- Llamar a otros servicios **solo por Feign con el nombre de Eureka**, nunca con IP ni `localhost`.
- Los DTO de otros servicios se **copian** en `client/` (como `ProductoDto`); no se comparte código de dominio entre servicios.
- El total se calcula con el precio de ms-catalogo **en el momento del checkout**, no con lo que diga el cliente.
- **Las llamadas Feign van fuera de `@Transactional`.** Cada servicio tiene un pool de solo 4–5 conexiones a MySQL (Aiven acepta 46 en total). Si una transacción envuelve una llamada a otro servicio (hasta 2 s), retiene una conexión todo ese tiempo y bastan 4 checkouts simultáneos para agotar el pool. El checkout se arma con transacciones cortas: guardar la venta PENDIENTE (tx) → reservar (Feign) → emitir comprobante (Feign) → confirmar y marcar CONFIRMADA (tx).

## Cómo correrlo

```bash
docker compose up -d ms-pedidos                  # carrito: solo Eureka + ms-pedidos
docker compose up -d ms-catalogo ms-inventario ms-comprobantes   # sumarlos para probar el checkout
# o, para depurar desde el IDE o Maven (con los otros tres ya levantados en Docker):
docker compose stop ms-pedidos                   # libera el puerto 8084
./mvnw -q install -DskipTests                    # una vez, y cada vez que cambie common-security
./mvnw -pl ms-pedidos spring-boot:run            # lee AIVEN_* y DB_PASS_PEDIDOS del .env
curl localhost:8084/pedidos
```
