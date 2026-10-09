# ms-catalogo · Productos, marcas y categorías

| | |
|---|---|
| **Responsable** | Integrante 3 |
| **Puerto** | 8082 |
| **Nombre en Eureka** | `ms-catalogo` |
| **Schema MySQL** | `db_catalogo` (usuario `usr_catalogo`) · script `infra/mysql/02-catalogo.sql` |
| **Rutas en el gateway** | `/productos/**`, `/marcas/**`, `/categorias/**` |
| **Tarea transversal** | Datos semilla: 200 productos; imágenes en S3 |
| **Pantallas web** | Inicio, catálogo, detalle de producto, CRUD de productos |

## Para qué existe

Es el dueño de **lo que se vende**: nombre, descripción, marca, categoría, precio e imagen. **No guarda el stock**: eso es de ms-inventario. Es el servicio con más lecturas del sistema (todo el que entra a la tienda lo consulta), así que sus `GET` son **públicos**.

ms-pedidos lo consulta por Feign en el checkout para obtener el **precio vigente** (`GET /productos/{id}`): no se le puede creer al carrito el precio.

## Qué cambió respecto al informe

- `producto.stock` se fue a ms-inventario.
- `producto.marca` dejó de ser texto libre: ahora es la tabla `marca` (Dahua, Honeywell, Samsung, Nest, Sony).
- Nueva tabla `categoria` para filtrar: Cámaras, Alarmas, Sensores de humo, Cerraduras digitales, Control de acceso.
- `imagen_url` apuntará a S3/CloudFront (en el informe apuntaba a Pixabay y rutas locales).

## Modelo de datos

```
marca (id, nombre UNIQUE)
categoria (id, nombre UNIQUE)
producto (id, nombre, descripcion, precio DECIMAL(10,2), imagen_url,
          id_marca → marca, id_categoria → categoria, activo)
```

Semilla actual: 5 marcas, 5 categorías, **5 productos** (ids 1–5, con stock en ms-inventario).

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
- [ ] Completar `infra/mysql/02-catalogo.sql` hasta **200 productos** (el alcance del informe). Coordinar con el integrante 5: cada producto necesita su fila en `03-inventario.sql`.
- [ ] Al cambiar los `.sql`, recrear la base: `docker compose down -v && docker compose up -d`.

### Semana 6 · Pantallas web (en `web-client`)
- [ ] `/catalogo` (grilla con filtros), `/catalogo/{id}` (detalle + "agregar al carrito"), `/admin/productos` (CRUD). Ya existe `CatalogoApi` en el web-client: extenderla.

### Semana 7 · Pruebas
- [ ] Tests de `ProductoService` con Mockito (crear, actualizar, producto inexistente).
- [ ] Más tests `@DataJpaTest` de los filtros.
- [ ] `postman/ms-catalogo.postman_collection.json`.

### Semana 8 · S3
- [ ] Subir imágenes a S3 (bucket público de lectura o CloudFront) y guardar la URL en `imagen_url`.

## Contrato con otros servicios (no romperlo sin avisar)

`GET /productos/{id}` lo consume **ms-pedidos** (integrante 4) y espera como mínimo `id`, `nombre` y `precio`. Si cambias esos nombres de campo, el checkout se rompe.

## Cómo correrlo

```bash
docker compose up -d mysql ms-discovery
./mvnw -pl ms-catalogo spring-boot:run
curl localhost:8082/productos/1
```
