# ms-comprobantes · Boletas y facturas

| | |
|---|---|
| **Responsable** | Galarza (también lleva ms-auth) · rama `b-galarza` |
| **Puerto** | 8085 |
| **Nombre en Eureka** | `ms-comprobantes` |
| **Schema MySQL** | `db_comprobantes` (usuario `ms_comprobantes`) · script `infra/mysql/05-comprobantes.sql` |
| **Rutas en el gateway** | `/comprobantes/**` |
| **Tarea transversal** | Generación del PDF del comprobante y reporte de ventas |
| **Pantallas web** | Listado y detalle de comprobantes |

## Para qué existe

Emite el comprobante de pago de cada venta con una **numeración única y correlativa**. En el informe convivían números como `B0001` y `C-8618141792` (la numeración se generaba de dos formas). Ahora hay una sola regla:

| Tipo | Serie | Ejemplo | Documento del cliente |
|---|---|---|---|
| BOLETA | `B001` | `B001-00000001` | DNI (8 dígitos) |
| FACTURA | `F001` | `F001-00000001` | RUC (11 dígitos) |

Lo llama **ms-pedidos** en el paso 5 del checkout. Si este servicio no responde en 2 segundos, ms-pedidos libera el stock y anula la venta (circuit breaker).

## Modelo de datos

```
serie_correlativo (serie PK, tipo ENUM(BOLETA, FACTURA), ultimo_correlativo)   UNIQUE(serie, tipo)
comprobante (id_comprobante, id_venta UNIQUE, id_usuario, tipo, serie, correlativo,
             numero GENERATED = serie-LPAD(correlativo, 8),         UNIQUE(serie, correlativo)
             doc_cliente, nombre_razon_social, subtotal, igv, total, pdf_url, emitido_en)
             FK (serie, tipo) → serie_correlativo
```

- **Numeración sin duplicados:** la serie se lee con `SELECT ... FOR UPDATE` (`SerieCorrelativoRepository.findFirstByTipoOrderBySerieAsc`, con `@Lock(PESSIMISTIC_WRITE)`), se avanza con `serie.siguiente()` y se guarda en la misma transacción. `UNIQUE(serie, correlativo)` es la red de seguridad.
- **`numero` lo calcula MySQL** (columna generada): en la entidad es de solo lectura (`@Generated`). Se guardan `serie` y `correlativo`.
- **La BD valida el documento (CHECK):** FACTURA exige RUC (11 dígitos, empieza con 10, 15, 17 o 20) y razón social; BOLETA admite DNI de 8 dígitos o nada. La FK compuesta obliga a que B vaya con BOLETA y F con FACTURA.
- `total = subtotal + igv` (CHECK): se copian los montos de la venta.
- `id_venta UNIQUE`: una venta, un comprobante. Si ms-pedidos reintenta, no se duplica (devolver el existente).

## Guía de negocio

### Tipo de comprobante (`serie_correlativo.tipo` y `comprobante.tipo`)

| Valor | Para quién | Serie | `doc_cliente` | `nombre_razon_social` |
|---|---|---|---|---|
| `BOLETA` | Consumidor final | `B001` | DNI de 8 dígitos, o vacío | Opcional |
| `FACTURA` | Empresa (le sirve para crédito fiscal) | `F001` | RUC de 11 dígitos que empieza con 10, 15, 17 o 20 | **Obligatorio** |

La BD lo hace cumplir (`ck_comprobante_doc` y la FK compuesta serie+tipo), pero **el servicio debe validarlo antes** y responder 400 con un mensaje claro, en vez de dejar que salga el error de MySQL.

### Numeración

- Cada serie lleva su propio contador (`ultimo_correlativo`).
- Para emitir: bloquear la fila de la serie (`SELECT ... FOR UPDATE`), sumarle 1 y guardar el comprobante con ese correlativo, **todo en una transacción**.
- `numero` (`B001-00000001`) lo arma MySQL; el servicio no lo escribe.
- Un número emitido nunca se reutiliza ni se borra.

### Referencias a otros servicios (`-- ref.`)

- **`comprobante.id_venta` → `db_pedidos.venta`.** Lo manda ms-pedidos en el paso 5 del checkout, junto con tipo, documento y montos (`subtotal`, `igv` y `total` se copian de la venta). Una venta tiene un solo comprobante (UNIQUE): si ms-pedidos reintenta con el mismo `idVenta`, **devolver el comprobante que ya existe**, en vez de fallar o emitir otro número.

## Estado actual

| Capa | Archivo | Estado |
|---|---|---|
| Entity | `Comprobante` (`numero` generado por MySQL), `SerieCorrelativo` (PK = serie, `siguiente()`), `TipoComprobante` | ✅ |
| Repository | `ComprobanteRepository.findByIdVenta`, `SerieCorrelativoRepository.findFirstByTipoOrderBySerieAsc` (con bloqueo) | ✅ con test `@DataJpaTest` |
| Service | `listar`, `obtener`, `formatearNumero(serie, correlativo)` | ✅ con test unitario |
| Controller | `GET /comprobantes`, `GET /comprobantes/{id}` | ✅ verificado por el gateway |
| Semilla | Series B001 y F001 en 0 | ✅ |
| Emisión, dueño, reporte y PDF | `POST /comprobantes` (idempotente, serie con bloqueo), control de dueño por `id_usuario`, `GET /comprobantes/reporte`, PDF con OpenPDF (productos vía `PedidosClient` reenviando el token) | ✅ con `ComprobanteIntegracionTest` (6) y `NumeroALetrasTest` |

## Endpoints objetivo

| Método | Ruta | Quién | Estado | Bloqueo |
|---|---|---|---|---|
| POST | `/comprobantes` | Interno (ms-pedidos) | ✅ | ⛔ bloquea el checkout de ms-pedidos |
| GET | `/comprobantes` | ADMIN: todos · CLIENTE: los suyos | ✅ | 🟢 |
| GET | `/comprobantes/{id}` | Dueño o ADMIN | ✅ | 🟢 |
| GET | `/comprobantes/venta/{idVenta}` | Dueño o ADMIN | ✅ | 🟢 |
| GET | `/comprobantes/{id}/pdf` | Dueño o ADMIN | ✅ | 🔗 ms-pedidos (productos del documento) |
| GET | `/comprobantes/reporte?desde=&hasta=` | ADMIN | ✅ | 🟢 |

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

### Objeto `Comprobante` (`ComprobanteResponse`)

```json
{ "id": 40, "idVenta": 43, "tipo": "BOLETA", "numero": "B001-00000040", "docCliente": "45678912",
  "nombreRazonSocial": "Ana Torres", "subtotal": 271.02, "igv": 48.78, "total": 319.8,
  "pdfUrl": "/comprobantes/40/pdf", "emitidoEn": "2026-10-15T14:35:02" }
```

| Atributo | Tipo | Para qué lo usa la web |
|---|---|---|
| `id` | `Long` | Abrir la vista imprimible y descargar el PDF |
| `idVenta` | `Long` | Une el comprobante con su pedido |
| `tipo` | `String`: `BOLETA` · `FACTURA` | Título del documento y filtro |
| `numero` | `String` | Serie y correlativo (`B001-` / `F001-` + 8 dígitos); se muestra y da nombre al PDF |
| `docCliente` | `String` o `null` | DNI (8) o RUC (11). En boleta puede ser `null` |
| `nombreRazonSocial` | `String` | Nombre del cliente o razón social impresa |
| `subtotal` · `igv` · `total` | `BigDecimal` | Montos del documento; `total` se suma en los reportes |
| `pdfUrl` | `String` | Ruta del PDF. Devolver `"/comprobantes/{id}/pdf"`: la columna `pdf_url` está vacía hoy, así que se puede armar en el mapper |
| `emitidoEn` | `LocalDateTime` | Fecha de emisión; filtro del reporte por rango |

### Cómo saber si un comprobante es del cliente

`comprobante` guarda **`id_usuario`** (`BIGINT NOT NULL`, `-- ref. db_auth.usuario`, con índice `ix_comprobante_usuario`). La columna ya existe en Aiven desde el 2026-10-17: falta agregar el campo `idUsuario` a la entidad `Comprobante`. ms-pedidos lo manda al emitir (es el `idUsuario` de la venta), igual que `detalle_venta` copia el nombre y el precio del producto. Así el control de dueño **no necesita llamar a ms-pedidos**:

- **ADMIN** (`usuarioActual.esAdmin()`): ve todo.
- **CLIENTE, lista** (`GET /comprobantes`): `findByIdUsuarioOrderByEmitidoEnDesc(usuarioActual.id())`.
- **CLIENTE, uno** (`/{id}`, `/venta/{idVenta}`, `/{id}/pdf`): si `comprobante.idUsuario` no es `usuarioActual.id()`, responde **403**.

`idUsuario` **no sale en la respuesta**: es un dato interno.

Solo el PDF necesita a ms-pedidos, porque los productos del documento están en la venta.

### `POST /comprobantes` · Interno (ms-pedidos) · ✅ · ⛔ bloquea ms-pedidos

**Para qué:** emitir la boleta o factura en el paso 5 del checkout. La web **nunca** la llama (el gateway responde 403 desde afuera), pero sin ella no hay compra.
**Vistas (indirectas):** 4.7 Checkout · 4.8 Compra confirmada · 4.9 Compra no completada.

| Envía | Tipo | Regla |
|---|---|---|
| `idVenta` | `Long` | Venta PENDIENTE de ms-pedidos |
| `idUsuario` | `Long` | Dueño de la venta. Se guarda para el control de dueño |
| `tipo` | `String`: `BOLETA` · `FACTURA` | |
| `docCliente` | `String` o `null` | Boleta: DNI de 8 dígitos o `null`. Factura: RUC de 11 dígitos que empieza con 10, 15, 17 o 20 |
| `nombreRazonSocial` | `String` | Obligatorio en factura |
| `subtotal` · `igv` · `total` | `BigDecimal` | Copiados de la venta; `total = subtotal + igv` |

Responde **201** `Comprobante` · **200** con el mismo comprobante si esa `idVenta` ya tenía uno (idempotencia) · **400** si el DNI o el RUC no son válidos. Debe responder en **menos de 2 s** (circuit breaker de ms-pedidos). El detalle está en el contrato C7, más abajo.

### `GET /comprobantes` · Con sesión · ✅ · 🟢

**Para qué:** el cliente ve el número de comprobante de cada pedido; el administrador los lista y filtra.
**Vistas:** 4.10 Mis pedidos (número en cada pedido) · 5.1 Resumen · 5.7 Ventas · 5.8 Comprobantes.

Responde **200**: `List<Comprobante>`, **del más nuevo al más antiguo** (`emitidoEn` descendente). Un CLIENTE recibe solo los de sus ventas; el ADMIN, todos.

### `GET /comprobantes/{id}` · Dueño o ADMIN · ✅ · 🟢

**Para qué:** mostrar el comprobante en su vista imprimible.
**Vistas:** 4.12 Comprobante imprimible.

Responde **200**: `Comprobante`. **403** si es de otro cliente. **404**.

### `GET /comprobantes/venta/{idVenta}` · Dueño o ADMIN · ✅ · 🟢

**Para qué:** encontrar el comprobante de un pedido (una venta tiene uno solo).
**Vistas:** 4.8 Compra confirmada · 4.11 Detalle de mi pedido · detalle de venta del panel.

Responde **200**: `Comprobante`. **403**. **404** si la venta no tiene comprobante (por ejemplo, una venta ANULADA).

### `GET /comprobantes/{id}/pdf` · Dueño o ADMIN · ✅ · 🔗 ms-pedidos

**Para qué:** descargar el comprobante en PDF.
**Vistas:** botón "Descargar PDF" en 4.8, 4.11, 4.12, el detalle de venta del panel y 5.8.

Responde **200** con el archivo:
- `Content-Type: application/pdf`
- `Content-Disposition: attachment; filename="B001-00000040.pdf"` (el `numero` del comprobante)

**403**. **404**.

Contenido: el mismo de la vista 4.12 (cabecera, datos del cliente, tabla de productos, totales con el importe en letras y pie). **Los productos no están en este servicio:** se piden a ms-pedidos con `GET /pedidos/{idVenta}` (campo `detalles`), por Feign y reenviando el token. Para eso: copiar la dependencia `spring-cloud-starter-openfeign` del `pom.xml` de ms-pedidos al tuyo, `@EnableFeignClients`, un `PedidosClient` y un `RequestInterceptor` que reenvíe el header `Authorization`. **Mientras no esté:** arma el PDF con Mockito simulando `PedidosClient`.

### `GET /comprobantes/reporte` · ADMIN · ✅ · 🟢

**Para qué:** el reporte de ventas de un período.
**Vistas:** 5.9 Reportes.

| Parámetro | Tipo | Regla |
|---|---|---|
| `desde` | `LocalDate` | Opcional, inclusivo |
| `hasta` | `LocalDate` | Opcional, inclusivo |

Filtran por la fecha de `emitidoEn`. Ejemplo: `/comprobantes/reporte?desde=2026-10-01&hasta=2026-10-15`.

Responde **200**:

```json
{ "desde": "2026-10-01", "hasta": "2026-10-15", "cantidad": 18, "subtotal": 4520.34, "igv": 813.66, "total": 5334.0,
  "comprobantes": [ /* Comprobante */ ] }
```

| Atributo | Tipo | Para qué lo usa la web |
|---|---|---|
| `desde` · `hasta` | `LocalDate` o `null` | Rango aplicado; se muestra en el encabezado del reporte |
| `cantidad` | `Integer` | Tarjeta "Comprobantes emitidos" |
| `subtotal` · `igv` · `total` | `BigDecimal` | Tarjetas de montos del período |
| `comprobantes` | `List<Comprobante>` | Tabla del reporte y desglose boletas / facturas |

## Dependencias con otros servicios

El mapa completo está en [`ARRANQUE_PROYECTO.md`](../ARRANQUE_PROYECTO.md#quién-le-debe-a-quién).

### Lo que debo (me lo piden)

**C7 · Emisión para el checkout de ms-pedidos (Evelyn) — fin de la semana 3.**

| Endpoint | Cuerpo / parámetro | Respuesta |
|---|---|---|
| `POST /comprobantes` | `{ "idVenta": 42, "idUsuario": 7, "tipo": "BOLETA", "docCliente": "12345678", "nombreRazonSocial": null, "subtotal": 338.81, "igv": 60.99, "total": 399.80 }` | **201** `ComprobanteResponse` (con `numero`) · **200** con el mismo comprobante si esa `idVenta` ya tenía uno · **400** si el DNI o RUC no es válido |
| `GET /comprobantes/venta/{idVenta}` | — | **200** `ComprobanteResponse` · **404** si la venta no tiene comprobante. Semana 4 |

Tiene que responder en **menos de 2 s**: ms-pedidos corta la llamada a los 2 s y anula la venta (circuit breaker).

`GET /comprobantes/venta/{idVenta}` sirve para encontrar el comprobante de un pedido. El dueño se controla con `comprobante.id_usuario` (ver "Cómo saber si un comprobante es del cliente").

### Lo que necesito

| Contrato | Quién lo entrega | Semana | Para qué lo uso |
|---|---|---|---|
| **C8** · `idVenta` real, `idUsuario` de la venta, montos y token reenviado | Evelyn | 4 | Emitir y saber de quién es cada comprobante |
| **C2** · `JwtAuthFilter` | Galarza | 3 | Proteger el listado completo y el reporte (solo ADMIN) |

### Cómo avanzar sin esperar

Como `id_venta` no tiene FK, **puedes emitir comprobantes desde Postman con `idVenta` inventados** directo en `localhost:8085`. La numeración, la validación de RUC/DNI, la idempotencia y el PDF no dependen de nadie.

## Pendientes por semana

> **Estado al cierre (19/10/2026):** [x] hecho · ~~tachado~~ no se hizo o se resolvió de otra forma (el motivo va al lado).

### Semana 2–3
- [x] `EmitirComprobanteRequest` con validaciones (DNI de 8 dígitos para boleta, RUC de 11 para factura: `@Pattern` o validación en el service).
- [x] `ComprobanteService.emitir()` en `@Transactional`: bloquear la serie del tipo, `serie.siguiente()`, guardar `serie` + `correlativo` (MySQL arma el `numero`). Traducir la violación de la CHECK de documento a un 400 claro.
- [x] Idempotencia: si ya existe un comprobante para `idVenta`, devolverlo.

### Semana 4
- [x] Acordar el contrato con Evelyn (ms-pedidos) y probar el checkout juntos.

### Semana 5 · PDF
- [x] `GET /comprobantes/{id}/pdf` con **OpenPDF** (`com.github.librepdf:openpdf`, simple y libre): cabecera IntiGuard, número, fecha, cliente, total. Respuesta `application/pdf`.
- [x] Reporte de ventas por rango de fechas (JSON; PDF opcional).

### Semana 6 · Pantallas web
- ~~`/comprobantes` (lista) y `/comprobantes/{id}` (detalle + botón "Descargar PDF") en `web-client`.~~ → no se hizo: la web es la app React de `DSW_II_PROYECTO_WEB`.

### Semana 7 · Pruebas
- [x] Tests de `emitir()`: primer número `B001-00000001`, el segundo `00000002`, factura con RUC inválido → 400, emitir dos veces la misma venta devuelve el mismo.
- ~~`postman/ms-comprobantes.postman_collection.json`.~~ → no se hizo: hay `IntiGuard-smoke` e `IntiGuard-seguridad`; el flujo completo se prueba con `pruebas/prueba-e2e.py`.

## Cómo correrlo

```bash
docker compose up -d ms-comprobantes             # todo en Docker (Eureka + ms-comprobantes)
# o, para depurar desde el IDE o Maven:
docker compose up -d ms-discovery
./mvnw -q install -DskipTests                    # una vez, y cada vez que cambie common-security
./mvnw -pl ms-comprobantes spring-boot:run       # lee AIVEN_* y DB_PASS_COMPROBANTES del .env
curl localhost:8085/comprobantes
```
