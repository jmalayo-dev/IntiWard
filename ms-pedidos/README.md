# ms-pedidos · Carrito, checkout y ventas

| | |
|---|---|
| **Responsable** | Integrante 4 |
| **Puerto** | 8084 |
| **Nombre en Eureka** | `ms-pedidos` |
| **Schema MySQL** | `db_pedidos` (usuario `usr_pedidos`) · script `infra/mysql/04-pedidos.sql` |
| **Rutas en el gateway** | `/carrito/**`, `/pedidos/**` |
| **Tarea transversal** | Clientes Feign, Resilience4j y pruebas de integración del checkout |
| **Pantallas web** | Carrito, confirmación de compra, mis pedidos, listado de ventas |

## Para qué existe

Es el **orquestador** del sistema: el único punto donde se encadenan cuatro servicios. Por eso es el **más crítico** del cronograma: **la semana 4 es la bisagra**. Si el checkout no funciona al cerrarla, se recorta el cliente web, no esto.

## El checkout (lo más importante de este servicio)

```
POST /pedidos/checkout   (usuario sacado del token / cabecera X-User)
 1. Leer el carrito del usuario                       (local)
 2. Por cada ítem: precio vigente                     → ms-catalogo     GET  /productos/{id}
 3. Reservar todo el stock                            → ms-inventario   POST /stock/reservar
 4. Guardar Venta PENDIENTE + DetalleVenta            (local)
 5. Emitir comprobante                                → ms-comprobantes POST /comprobantes
 6. Confirmar stock                                   → ms-inventario   POST /stock/confirmar
 7. Venta CONFIRMADA, guardar idComprobante, vaciar carrito

 Si 3 falla  → 409 "Stock insuficiente", no se guarda nada.
 Si 5 falla o tarda > 2 s (circuit breaker) →
      COMPENSAR: POST /stock/liberar, Venta ANULADA, responder 503 con un mensaje claro.
```

Así nunca queda **una venta a medias con el stock descontado**. Esto es la **Saga por compensación** + **Circuit Breaker**, dos patrones para nombrar en la sustentación.

## Modelo de datos

```
carrito (id, id_usuario UNIQUE, fecha_creacion)
item_carrito (id, id_carrito → carrito, id_producto, cantidad)      UNIQUE(id_carrito, id_producto)
venta (id, id_usuario, fecha, total, estado ENUM(PENDIENTE, CONFIRMADA, ANULADA), id_comprobante)
detalle_venta (id, id_venta → venta, id_producto, nombre_producto, cantidad, precio_unitario, subtotal)
```

El carrito está **en base de datos** (en el informe vivía en la sesión): con servicios sin estado tiene que sobrevivir a un reinicio. `detalle_venta` **copia** nombre y precio: si mañana cambia el catálogo, el pedido histórico no.

## Estado actual

| Capa | Archivo | Estado |
|---|---|---|
| Entity | `Carrito`, `ItemCarrito`, `Venta`, `DetalleVenta`, `EstadoVenta` | ✅ |
| Repository | `CarritoRepository.findByIdUsuario`, `VentaRepository.findByIdUsuarioOrderByFechaDesc` | ✅ con test `@DataJpaTest` (cascada venta→detalles) |
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
| POST | `/pedidos/checkout` `{tipoComprobante, documentoCliente}` | Cliente | ⬜ |
| GET | `/pedidos` | ADMIN: todos · Cliente: los suyos | 🟡 lista todos |
| GET | `/pedidos/{id}` | Dueño o ADMIN | ✅ (sin control de dueño aún) |

## Pendientes por semana

### Semana 2–3
- [ ] `CarritoService` + `CarritoController` (`/carrito/items`), con validaciones `@Positive cantidad`.
- [ ] Obtener el usuario del token (cuando `common-security` esté lista); mientras tanto, cabecera `X-User-Id` o parámetro fijo.

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

## Cómo correrlo

```bash
docker compose up -d mysql ms-discovery ms-catalogo ms-inventario ms-comprobantes
./mvnw -pl ms-pedidos spring-boot:run
curl localhost:8084/pedidos
```
