# ms-catalogo · Productos, marcas y categorías

| | |
|---|---|
| **Responsable** | Integrante 3 |
| **Puerto** | 8082 |
| **Nombre en Eureka** | `ms-catalogo` |
| **Schema MySQL** | `db_catalogo` (usuario `ms_catalogo`) · script `infra/mysql/02-catalogo.sql` |
| **Rutas en el gateway** | `/productos/**`, `/marcas/**`, `/categorias/**` |
| **Tarea transversal** | Datos semilla: 200 productos; imágenes en S3 |
| **Pantallas web** | Inicio, catálogo, detalle de producto, CRUD de productos |

## Para qué existe

Es el dueño de **lo que se vende**: nombre, descripción, marca, categoría, precio e imagen. **No guarda el stock**: eso es de ms-inventario. Es el servicio con más lecturas del sistema (todo el que entra a la tienda lo consulta), así que sus `GET` son **públicos**.

ms-pedidos lo consulta por Feign en el checkout para obtener el **precio vigente** (`GET /productos/{id}`): no se le puede creer al carrito el precio.

## Qué cambió respecto al informe

- `producto.stock` se fue a ms-inventario.
- `producto.marca` dejó de ser texto libre: ahora es la tabla `marca` (Hikvision, Dahua, Ezviz, Imou, Honeywell, Yale).
- Nueva tabla `categoria` para filtrar: Cámaras, Alarmas, Sensores de movimiento, Detectores de humo, Cerraduras digitales, Control de acceso.
- `imagen_url` apuntará a S3/CloudFront (en el informe apuntaba a Pixabay y rutas locales).

## Modelo de datos

```
marca (id_marca, nombre UNIQUE, activo)
categoria (id_categoria, nombre UNIQUE, activo)
producto (id_producto, nombre, descripcion TEXT, id_marca → marca, id_categoria → categoria,
          precio DECIMAL(10,2) CHECK > 0 (incluye IGV), imagen_url, activo, creado_en, actualizado_en)
          índices: (activo, id_categoria, id_marca) para filtros · (nombre) para búsqueda
```

Semilla actual: 6 marcas, 6 categorías, **8 productos** (ids 1–8, cada uno con 10 unidades en ms-inventario).

## Guía de negocio

### Estados (`activo`)

Este schema no tiene ENUM: los estados se manejan con `activo`.

| Valor | Significado |
|---|---|
| `activo = true` | Se muestra en la tienda y se puede comprar. |
| `activo = false` | Producto descontinuado (o marca/categoría retirada): no aparece en la tienda, pero sigue existiendo para que las ventas pasadas lo puedan mostrar. |

### Quién referencia a este servicio (`-- ref. db_catalogo.producto`)

| Tabla de otro servicio | Para qué guarda el `id_producto` |
|---|---|
| `db_inventario.stock` | Cuántas unidades hay |
| `db_pedidos.item_carrito` | Qué tiene un cliente en el carrito |
| `db_pedidos.detalle_venta` | Qué se vendió |

Por eso:

- **Nunca borrar un producto con `DELETE`**: usar `activo = false`. Si se borra, el stock, los carritos y las ventas apuntarían a un producto inexistente, y MySQL no lo impide (no hay FK entre schemas).
- **Nunca reutilizar ni cambiar un `id_producto`.**
- **`GET /productos/{id}` es un contrato**: ms-pedidos lee `id`, `nombre` y `precio`. Si cambian esos nombres de campo, el checkout se rompe.
- **Un producto nuevo nace sin stock**: el ADMIN tiene que registrar una ENTRADA en ms-inventario para que se pueda vender.

### Precio

`precio` incluye IGV (18 %). Cambiarlo no afecta ventas pasadas, porque ms-pedidos copia el precio en `detalle_venta` al momento de la compra.

## Estado actual

| Capa | Archivo | Estado |
|---|---|---|
| Entity | `Marca`, `Categoria`, `Producto` | ✅ |
| Repository | `ProductoRepository.findByActivoTrue`, `findByCategoriaIdAndActivoTrue`; `MarcaRepository`, `CategoriaRepository` | ✅ con test `@DataJpaTest` |
| DTO / Mapper | `ProductoResponse` (marca y categoría como texto), `ProductoMapper` | ✅ |
| Service | `ProductoService.listar/obtener` | ✅ |
| Controller | `GET /productos`, `GET /productos/{id}` (404 si no existe) | ✅ verificado por el gateway |
| Errores | `GlobalExceptionHandler` + `ApiError` | ✅ |

## Endpoints objetivo

| Método | Ruta | Quién | Estado |
|---|---|---|---|
| GET | `/productos?page=0&size=12&categoria=1&marca=2&q=camara` | Público | 🟡 existe sin paginación ni filtros |
| GET | `/productos/{id}` | Público | ✅ |
| POST | `/productos` | ADMIN | ⬜ |
| PUT | `/productos/{id}` | ADMIN | ⬜ |
| DELETE | `/productos/{id}` | ADMIN (borrado lógico `activo = false`) | ⬜ |
| GET | `/marcas`, `/categorias` | Público | ⬜ |

## Pendientes por semana

### Semana 2–3
- [ ] `ProductoRequest` con validaciones: `@NotBlank nombre`, `@NotNull @DecimalMin("0.01") precio`, `@NotNull idMarca`, `@NotNull idCategoria`.
- [ ] `POST`, `PUT`, `DELETE` (borrado lógico). Validar que la marca y la categoría existan → 404.
- [ ] `GET /marcas` y `GET /categorias` (para los combos del formulario web).
- [ ] Paginación: `Pageable` + `Page<ProductoResponse>`. Para que el JSON salga estable, agregar `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`.
- [ ] Filtros: por categoría, marca y texto (`findByNombreContainingIgnoreCase` o `@Query`).
- [ ] `@PreAuthorize("hasRole('ADMIN')")` en los métodos de escritura (cuando `common-security` esté lista).

### Semana 5 · Datos semilla
- [ ] Completar `infra/mysql/inserts-test/02-catalogo.sql` hasta **200 productos** (el alcance del informe). Coordinar con el integrante 5: cada producto necesita su stock (`inserts-test/03-inventario.sql` lo genera para todos los productos).
- [ ] La base en Aiven es compartida: los productos nuevos los carga el integrante 1 en Aiven al mergear el PR.

### Semana 6 · Pantallas web (en `web-client`)
- [ ] `/catalogo` (grilla con filtros), `/catalogo/{id}` (detalle + "agregar al carrito"), `/admin/productos` (CRUD). Ya existe `CatalogoApi` en el web-client: extenderla.

### Semana 7 · Pruebas
- [ ] Tests de `ProductoService` con Mockito (crear, actualizar, producto inexistente).
- [ ] Más tests `@DataJpaTest` de los filtros.
- [ ] `postman/ms-catalogo.postman_collection.json`.

### Semana 8 · S3
- [ ] Subir imágenes a S3 (bucket público de lectura o CloudFront) y guardar la URL en `imagen_url`.

## Dependencias con otros servicios

ms-catalogo no depende de otro servicio para su lógica (solo de C2 para proteger las escrituras). El mapa completo está en [`ARRANQUE_PROYECTO.md`](../ARRANQUE_PROYECTO.md#quién-le-debe-a-quién).

### Lo que debo (me lo piden)

| Contrato | A quién | Semana | Detalle |
|---|---|---|---|
| **C5** · `GET /productos/{id}` | Integrantes 4 y 5 | **2** | **200** `{id, nombre, descripcion, precio, imagenUrl, marca, categoria, activo}` · **404** si no existe. Hoy falta el campo **`activo`**: ms-pedidos lo necesita para no dejar agregar al carrito un producto descontinuado. Un producto inactivo responde **200 con `activo: false`** (no 404), para que las ventas pasadas lo puedan mostrar |
| **C9** · 200 productos en `inserts-test/02-catalogo.sql` | Integrante 1 (los carga en Aiven) | 5 | El stock de cada producto se genera solo (`inserts-test/03-inventario.sql`) |
| `GET /productos` (paginado y con filtros), `GET /marcas`, `GET /categorias` | web-client | 3 | Para la grilla y los combos del formulario |

### Lo que necesito

| Contrato | Quién lo entrega | Semana | Para qué lo uso |
|---|---|---|---|
| **C2** · `JwtAuthFilter` | Integrante 2 | 3 | `@PreAuthorize("hasRole('ADMIN')")` en crear, editar y desactivar |

## Contrato con otros servicios (no romperlo sin avisar)

`GET /productos/{id}` lo consumen **ms-pedidos** (integrante 4) e **ms-inventario** (integrante 5). ms-pedidos lee `id`, `nombre`, `precio` y `activo`. Si cambias esos nombres de campo, el checkout se rompe (contrato C5).

## Cómo correrlo

```bash
docker compose up -d ms-catalogo                 # todo en Docker (Eureka + ms-catalogo)
# o, para depurar desde el IDE o Maven:
docker compose up -d ms-discovery
./mvnw -q install -DskipTests                    # una vez, y cada vez que cambie common-security
./mvnw -pl ms-catalogo spring-boot:run           # lee AIVEN_* y DB_PASS_CATALOGO del .env
curl localhost:8082/productos/1
```
