# ms-pedidos · Carrito, checkout y ventas

| | |
|---|---|
| **Responsable** | Integrante 4 |
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
| Service / Controller | `GET /pedidos`, `GET /pedidos/{id}` | ✅ verificado por el gateway |
| Feign | `@EnableFeignClients`, `client/CatalogoClient` + `ProductoDto` (ejemplo listo) | ✅ base |
| Dependencias | `spring-cloud-starter-openfeign`, `spring-cloud-starter-circuitbreaker-resilience4j` | ✅ en el pom |
| Carrito, checkout, clientes de inventario y comprobantes | | ⬜ |

## Endpoints objetivo

| Método | Ruta | Quién | Estado |
|---|---|---|---|
| GET | `/carrito` | Cliente | ⬜ |
| POST | `/carrito/items` `{idProducto, cantidad}` | Cliente | ⬜ |
| PUT | `/carrito/items/{idProducto}` `{cantidad}` | Cliente | ⬜ |
| DELETE | `/carrito/items/{idProducto}` | Cliente | ⬜ |
| POST | `/pedidos/checkout` `{tipoComprobante, docCliente, nombreRazonSocial}` | Cliente | ⬜ |
| GET | `/pedidos` | ADMIN: todos · Cliente: los suyos | 🟡 lista todos |
| GET | `/pedidos/{id}` | Dueño o ADMIN | ✅ (sin control de dueño aún) |

## Dependencias con otros servicios

ms-pedidos es el servicio que **más depende de otros**. El mapa completo está en [`ARRANQUE_PROYECTO.md`](../ARRANQUE_PROYECTO.md#quién-le-debe-a-quién).

### Lo que necesito (me lo deben)

| Contrato | Quién lo entrega | Semana | Para qué lo uso |
|---|---|---|---|
| **C1** · el JWT trae el claim `idUsuario` ✅ | Integrante 2 | 2 | Saber de quién es el carrito y la venta |
| **C2** · `UsuarioActual.id()` en `common-security` ✅ | Integrante 2 | 3 | Leer el `idUsuario` del token en el controller |
| **C5** · `GET /productos/{id}` → `{id, nombre, precio, activo, …}`; 404 si no existe | Integrante 3 | 2 | Validar al agregar al carrito; precio vigente en el checkout |
| **C6** · `POST /stock/reservar`, `/confirmar`, `/liberar` | Integrante 5 | fin de la 3 | Pasos 4 y 6 del checkout y la compensación |
| **C7** · `POST /comprobantes` (idempotente por `idVenta`, responde en < 2 s) | Integrante 5 | fin de la 3 | Paso 5 del checkout |

### Lo que debo yo

| Contrato | A quién | Semana | Detalle |
|---|---|---|---|
| **C8** · `idVenta` de una venta PENDIENTE en cada reserva y comprobante | Integrante 5 | 4 | La venta se guarda **antes** de reservar. Ante cualquier fallo posterior a la reserva, **siempre** se llama a `/stock/liberar` |
| **C8** · reenviar el token en las llamadas Feign | Integrante 5 | 4 | Un `RequestInterceptor` que copie la cabecera `Authorization` de la petición del cliente |

### Cómo avanzar sin esperar

- **Carrito (semanas 2–3):** no depende de nadie, salvo C5. Toma el dueño del carrito con `usuarioActual.id()` (ya existe) y pruébalo con un token de desarrollo: `curl -H "Authorization: Bearer $(./infra/scripts/token-dev.sh 7 CLIENTE)" localhost:8084/carrito`.
- **Checkout:** escribe `InventarioClient` y `ComprobantesClient` con los contratos C6 y C7 tal como están en los READMEs del integrante 5, y prueba `CheckoutService` con **Mockito, simulando los clientes**. No necesitas que los endpoints existan para tener el checkout listo y probado.

## Pendientes por semana

### Semana 2–3
- [ ] `CarritoService` + `CarritoController` (`/carrito/items`), con validaciones `@Positive cantidad`.
- [ ] Obtener el usuario con `usuarioActual.id()` (de `common-security`, ya disponible). Para probar sin login: `./infra/scripts/token-dev.sh 7 CLIENTE`.

### Semana 4 · Checkout de punta a punta (entrega bisagra)
- [ ] `client/InventarioClient` (`reservar`, `confirmar`, `liberar`) y `client/ComprobantesClient` (`emitir`). Acordar los DTO con el integrante 5.
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
