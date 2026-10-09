# ms-comprobantes · Boletas y facturas

| | |
|---|---|
| **Responsable** | Integrante 5 (también lleva ms-inventario) |
| **Puerto** | 8085 |
| **Nombre en Eureka** | `ms-comprobantes` |
| **Schema MySQL** | `db_comprobantes` (usuario `usr_comprobantes`) · script `infra/mysql/05-comprobantes.sql` |
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
serie_correlativo (id, tipo ENUM(BOLETA, FACTURA) UNIQUE, serie, ultimo_correlativo, version)
comprobante (id, id_venta UNIQUE, tipo, numero UNIQUE, documento_cliente, total, fecha_emision)
```

- `version` (`@Version`): bloqueo optimista para que dos emisiones simultáneas **no saquen el mismo número**.
- `id_venta UNIQUE`: una venta, un comprobante. Si ms-pedidos reintenta, no se duplica (devolver el existente).

## Estado actual

| Capa | Archivo | Estado |
|---|---|---|
| Entity | `Comprobante`, `SerieCorrelativo` (con `@Version`), `TipoComprobante` | ✅ |
| Repository | `ComprobanteRepository.findByIdVenta`, `SerieCorrelativoRepository.findByTipo` | ✅ con test `@DataJpaTest` |
| Service | `listar`, `obtener`, `formatearNumero(serie, correlativo)` | ✅ con test unitario |
| Controller | `GET /comprobantes`, `GET /comprobantes/{id}` | ✅ verificado por el gateway |
| Semilla | Series B001 y F001 en 0 | ✅ |
| Emisión y PDF | | ⬜ |

## Endpoints objetivo

| Método | Ruta | Quién | Estado |
|---|---|---|---|
| POST | `/comprobantes` `{idVenta, tipo, documentoCliente, total}` | Interno (ms-pedidos) | ⬜ |
| GET | `/comprobantes` | ADMIN: todos · Cliente: los suyos | ✅ lista todos |
| GET | `/comprobantes/{id}` | Dueño o ADMIN | ✅ |
| GET | `/comprobantes/{id}/pdf` | Dueño o ADMIN | ⬜ |
| GET | `/comprobantes/reporte?desde=&hasta=` | ADMIN (reporte de ventas) | ⬜ |

## Pendientes por semana

### Semana 2–3
- [ ] `EmitirComprobanteRequest` con validaciones (DNI de 8 dígitos para boleta, RUC de 11 para factura: `@Pattern` o validación en el service).
- [ ] `ComprobanteService.emitir()` en `@Transactional`: tomar la serie por tipo, `ultimoCorrelativo + 1`, guardar, armar el número con `formatearNumero`. Si `@Version` falla por concurrencia, reintentar.
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
docker compose up -d mysql ms-discovery
./mvnw -pl ms-comprobantes spring-boot:run
curl localhost:8085/comprobantes
```
