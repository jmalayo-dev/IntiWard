# ms-comprobantes · Boletas y facturas

| | |
|---|---|
| **Responsable** | Integrante 5 (también lleva ms-inventario) |
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
comprobante (id_comprobante, id_venta UNIQUE, tipo, serie, correlativo,
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
| Emisión y PDF | | ⬜ |

## Endpoints objetivo

| Método | Ruta | Quién | Estado |
|---|---|---|---|
| POST | `/comprobantes` `{idVenta, tipo, docCliente, nombreRazonSocial, subtotal, igv, total}` | Interno (ms-pedidos) | ⬜ |
| GET | `/comprobantes` | ADMIN: todos · Cliente: los suyos | ✅ lista todos |
| GET | `/comprobantes/{id}` | Dueño o ADMIN | ✅ |
| GET | `/comprobantes/venta/{idVenta}` | Dueño o ADMIN (lo usan ms-pedidos y el web-client) | ⬜ |
| GET | `/comprobantes/{id}/pdf` | Dueño o ADMIN | ⬜ |
| GET | `/comprobantes/reporte?desde=&hasta=` | ADMIN (reporte de ventas) | ⬜ |

## Dependencias con otros servicios

El mapa completo está en [`ARRANQUE_PROYECTO.md`](../ARRANQUE_PROYECTO.md#quién-le-debe-a-quién).

### Lo que debo (me lo piden)

**C7 · Emisión para el checkout de ms-pedidos (integrante 4) — fin de la semana 3.**

| Endpoint | Cuerpo / parámetro | Respuesta |
|---|---|---|
| `POST /comprobantes` | `{ "idVenta": 42, "tipo": "BOLETA", "docCliente": "12345678", "nombreRazonSocial": null, "subtotal": 338.81, "igv": 60.99, "total": 399.80 }` | **201** `ComprobanteResponse` (con `numero`) · **200** con el mismo comprobante si esa `idVenta` ya tenía uno · **400** si el DNI o RUC no es válido |
| `GET /comprobantes/venta/{idVenta}` | — | **200** `ComprobanteResponse` · **404** si la venta no tiene comprobante. Semana 4 |

Tiene que responder en **menos de 2 s**: ms-pedidos corta la llamada a los 2 s y anula la venta (circuit breaker).

`GET /comprobantes/venta/{idVenta}` existe porque `comprobante` **no guarda `id_usuario`**. Para "mis comprobantes", el cliente pasa por sus pedidos (ms-pedidos sabe qué ventas son suyas) y con cada `idVenta` pide su comprobante.

### Lo que necesito

| Contrato | Quién lo entrega | Semana | Para qué lo uso |
|---|---|---|---|
| **C8** · `idVenta` real, montos de la venta y token reenviado | Integrante 4 | 4 | Emitir |
| **C2** · `JwtAuthFilter` | Integrante 2 | 3 | Proteger el listado completo y el reporte (solo ADMIN) |

### Cómo avanzar sin esperar

Como `id_venta` no tiene FK, **puedes emitir comprobantes desde Postman con `idVenta` inventados** directo en `localhost:8085`. La numeración, la validación de RUC/DNI, la idempotencia y el PDF no dependen de nadie.

## Pendientes por semana

### Semana 2–3
- [ ] `EmitirComprobanteRequest` con validaciones (DNI de 8 dígitos para boleta, RUC de 11 para factura: `@Pattern` o validación en el service).
- [ ] `ComprobanteService.emitir()` en `@Transactional`: bloquear la serie del tipo, `serie.siguiente()`, guardar `serie` + `correlativo` (MySQL arma el `numero`). Traducir la violación de la CHECK de documento a un 400 claro.
- [ ] Idempotencia: si ya existe un comprobante para `idVenta`, devolverlo.

### Semana 4
- [ ] Acordar el contrato con el integrante 4 (ms-pedidos) y probar el checkout juntos.

### Semana 5 · PDF
- [ ] `GET /comprobantes/{id}/pdf` con **OpenPDF** (`com.github.librepdf:openpdf`, simple y libre): cabecera IntiGuard, número, fecha, cliente, total. Respuesta `application/pdf`.
- [ ] Reporte de ventas por rango de fechas (JSON; PDF opcional).

### Semana 6 · Pantallas web
- [ ] `/comprobantes` (lista) y `/comprobantes/{id}` (detalle + botón "Descargar PDF") en `web-client`.

### Semana 7 · Pruebas
- [ ] Tests de `emitir()`: primer número `B001-00000001`, el segundo `00000002`, factura con RUC inválido → 400, emitir dos veces la misma venta devuelve el mismo.
- [ ] `postman/ms-comprobantes.postman_collection.json`.

## Cómo correrlo

```bash
docker compose up -d ms-comprobantes             # todo en Docker (Eureka + ms-comprobantes)
# o, para depurar desde el IDE o Maven:
docker compose up -d ms-discovery
./mvnw -q install -DskipTests                    # una vez, y cada vez que cambie common-security
./mvnw -pl ms-comprobantes spring-boot:run       # lee AIVEN_* y DB_PASS_COMPROBANTES del .env
curl localhost:8085/comprobantes
```
