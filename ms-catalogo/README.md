# ms-catalogo · Productos, marcas y categorías

| | |
|---|---|
| **Responsable** | César · rama `b-cesar` |
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
producto_especificacion (id_especificacion, id_producto → producto, nombre, valor, orden)
          UNIQUE(id_producto, nombre)
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
| Entity nueva | `ProductoEspecificacion` (`@OneToMany` con `orphanRemoval`, `@BatchSize`) | ✅ |
| DTO / Mapper | `ProductoResponse` (con `activo`, `idMarca`, `idCategoria`, `especificaciones`), `NombreActivoResponse`, `ProductoMapper` | ✅ |
| Service | `ProductoService` (filtros con `Specification`, `conStock` vía `InventarioClient`, CRUD), `MarcaCategoriaService` (contador de activos, nombre único) | ✅ |
| Controller | `ProductoController` (página `VIA_DTO`, `sort` precio/nombre, `size` hasta 100), `MarcaCategoriaController` | ✅ con `CatalogoIntegracionTest` (7, HTTP + H2) |
| Errores | `GlobalExceptionHandler` + `ApiError` | ✅ |

## Endpoints objetivo

| Método | Ruta | Quién | Estado | Bloqueo |
|---|---|---|---|---|
| GET | `/productos` | Público (`inactivos` solo ADMIN) | ✅ | 🔗 ms-inventario (filtro `conStock`) |
| GET | `/productos/{id}` | Público | ✅ | ⛔ bloquea ms-pedidos e ms-inventario |
| POST | `/productos` | ADMIN | ✅ | 🟢 |
| PUT | `/productos/{id}` | ADMIN | ✅ | 🟢 |
| DELETE | `/productos/{id}` | ADMIN (borrado lógico `activo = false`) | ✅ | 🟢 |
| GET | `/marcas` | Público (ADMIN ve también las inactivas) | ✅ | 🟢 |
| GET | `/categorias` | Público (ADMIN ve también las inactivas) | ✅ | 🟢 |
| POST | `/marcas` | ADMIN | ✅ | 🟢 |
| POST | `/categorias` | ADMIN | ✅ | 🟢 |
| PUT | `/marcas/{id}` | ADMIN | ✅ | 🟢 |
| PUT | `/categorias/{id}` | ADMIN | ✅ | 🟢 |

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

### Objeto `Producto` (`ProductoResponse`)

```json
{ "id": 1, "nombre": "Cámara IP Wi-Fi 1080p", "descripcion": "Cámara interior Full HD…", "precio": 159.9,
  "imagenUrl": "/productos/camara-ip.svg", "marca": "Hikvision", "categoria": "Cámaras",
  "activo": true, "idMarca": 1, "idCategoria": 1,
  "especificaciones": [{ "nombre": "Resolución", "valor": "1080p" }] }
```

| Atributo | Tipo | Para qué lo usa la web |
|---|---|---|
| `id` | `Long` | Enlace al detalle, carrito, stock y edición |
| `nombre` | `String` | Título de la tarjeta, del detalle y de las tablas; base del buscador |
| `descripcion` | `String` | Pestaña "Descripción" del detalle (4.3) |
| `precio` | `BigDecimal` | Precio con IGV en tarjetas, detalle y carrito |
| `imagenUrl` | `String` o `null` | Foto del producto; con `null` se muestra un ícono |
| `marca` | `String` | Nombre de la marca en la tarjeta y en la tabla de productos |
| `categoria` | `String` | Etiqueta, miga de pan y "productos relacionados" del detalle |
| `activo` | `Boolean` · **nuevo** | Interruptor en 5.2. La tienda nunca recibe inactivos. También es el contrato C5 con ms-pedidos |
| `idMarca` | `Long` · **nuevo** | Preselecciona la marca al editar (5.3) |
| `idCategoria` | `Long` · **nuevo** | Preselecciona la categoría al editar (5.3) |
| `especificaciones` | `List<{nombre: String, valor: String}>` · **nuevo** | Pestaña "Especificaciones" del detalle (4.3); puede venir vacía. Se guardan en la tabla `producto_especificacion` (ya existe en Aiven desde el 2026-10-17), ordenadas por `orden`. En la entidad: `@OneToMany(mappedBy = "producto", cascade = ALL, orphanRemoval = true)` |

### Objeto `Página` (lista paginada)

Formato `VIA_DTO` de Spring Data: se activa con `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)` y el controller devuelve `Page<ProductoResponse>`.

```json
{ "content": [ /* Producto */ ], "page": { "size": 12, "number": 0, "totalElements": 31, "totalPages": 3 } }
```

| Atributo | Tipo | Para qué lo usa la web |
|---|---|---|
| `content` | `List<Producto>` | Los productos de la página actual |
| `page.size` | `Integer` | Tamaño pedido |
| `page.number` | `Integer` | Página actual (desde 0), para el paginador |
| `page.totalElements` | `Long` | Texto "31 productos" y contadores del panel |
| `page.totalPages` | `Integer` | Cuántos botones tiene el paginador |

### Objeto `Marca` / `Categoria`

```json
{ "id": 1, "nombre": "Hikvision", "activo": true, "productos": 3 }
```

| Atributo | Tipo | Para qué lo usa la web |
|---|---|---|
| `id` | `Long` | Editar y filtrar |
| `nombre` | `String` | Texto del filtro, del buscador y de la tabla de 5.4 |
| `activo` | `Boolean` | Interruptor de 5.4; la tienda solo recibe las activas |
| `productos` | `Long` | Cantidad de productos **activos** de esa marca o categoría: contador al lado de cada filtro (4.2) y columna en 5.4 |

### `GET /productos` · Público · ✅ · 🔗 ms-inventario

**Para qué:** la grilla de la tienda, el buscador y las tablas del panel. Es el endpoint que más se usa.
**Vistas:** 4.1 Inicio (`size=24`) · 4.2 Catálogo (todos los filtros, `size=9`) · 4.3 Detalle (relacionados: `categoria&size=5`) · buscador del encabezado (sugerencias: `q&size=5`) · 5.1 Resumen · 5.2 Productos (filtros, `inactivos`, y `size=1` solo para leer `totalElements`) · 5.5 Stock · 5.6 Movimientos · 5.9 Reportes · campana de agotados del panel.

| Parámetro | Tipo | Regla |
|---|---|---|
| `page` | `Integer` | Página desde 0. Por defecto 0 |
| `size` | `Integer` | Por defecto 12. La web pide hasta **100** para tablas y gráficos del panel |
| `categoria` | `String` | Nombre **o** id de la categoría |
| `marca` | `String` · **nuevo** | Una o varias marcas separadas por coma (nombre o id): `marca=Hikvision,Dahua` |
| `q` | `String` | Texto contenido en el nombre, sin distinguir mayúsculas ni tildes (con `LIKE`, la collation `utf8mb4_0900_ai_ci` de la base ya las ignora) |
| `precioMin` · `precioMax` | `BigDecimal` · **nuevo** | Rango de precio |
| `conStock` | `Boolean` · **nuevo** | `true`: solo productos con stock (`disponible > 0`). Catálogo no tiene el stock: lo pide a ms-inventario (`GET /stock`). Necesita Feign en este servicio (copiar la dependencia `spring-cloud-starter-openfeign` del `pom.xml` de ms-pedidos y `@EnableFeignClients`) |
| `sort` | `String` · **nuevo** | `precio,asc` · `precio,desc` · `nombre,asc` (formato de Spring `Pageable`). Sin `sort`: por id |
| `inactivos` | `Boolean` · **nuevo** | `true` **y** ADMIN: incluye los inactivos. Sin admin se ignora. Aunque la ruta es pública, si llega un token se lee igual: `usuarioActual.esAdmin()` funciona |

Los filtros se combinan entre sí (Y). Responde **200**: `Página` de `Producto`.


### `GET /productos/{id}` · Público · ✅ · ⛔ bloquea ms-pedidos e ms-inventario

**Para qué:** el detalle de un producto, y los datos de un producto puntual en pedidos, carrito y edición.
**Vistas:** 4.3 Detalle de producto · 4.9 Compra no completada (nombre del producto agotado) · 4.10 Mis pedidos y 4.11 Detalle de mi pedido (miniaturas) · detalle de venta del panel · 5.3 Editar producto.

Responde **200**: `Producto`. **404** si no existe.

**Producto inactivo (decidido el 2026-10-17):** responde **200 con `activo: false`** a todos, también al público (contrato C5). ms-pedidos lo necesita para distinguir "no existe" de "descontinuado", y los pedidos pasados lo siguen mostrando. Es la web la que muestra "no encontrado" a quien no es admin. **404 solo si el id no existe.**

Lo usan ms-pedidos (carrito y checkout) e ms-inventario (validar la entrada de stock): **es lo primero que hay que completar** (campo `activo`).

### `POST /productos` · ADMIN · ✅ · 🟢

**Para qué:** crear un producto desde el panel.
**Vistas:** 5.3 Crear producto.

| Envía (`ProductoRequest`) | Tipo | Regla |
|---|---|---|
| `nombre` | `String` | Obligatorio |
| `descripcion` | `String` | Opcional |
| `precio` | `BigDecimal` | Obligatorio, mayor que 0 (con IGV) |
| `imagenUrl` | `String` | Opcional |
| `idMarca` | `Long` | Obligatorio, debe existir |
| `idCategoria` | `Long` | Obligatorio, debe existir |
| `activo` | `Boolean` | Opcional, por defecto `true` |
| `especificaciones` | `List<{nombre: String, valor: String}>` | Opcional. En cada una, `nombre` y `valor` son obligatorios; sin nombres repetidos en el mismo producto. Se guardan en el orden recibido (`orden` = posición) |

Responde **201**: `Producto`. **400** con `detalles` (si la marca o la categoría no existen: `"idMarca: no existe"`; especificaciones inválidas: `"especificaciones: hay nombres repetidos"`, para que la web marque el campo).

**Stock del producto nuevo (decidido el 2026-10-17):** este endpoint **no llama a inventario**. Un producto sin entradas tiene stock 0; ms-inventario crea su fila con la primera entrada.

### `PUT /productos/{id}` · ADMIN · ✅ · 🟢

**Para qué:** editar un producto, o reactivarlo desde el interruptor de la tabla.
**Vistas:** 5.3 Editar producto · 5.2 Productos (interruptor: reactivar).

Envía los mismos campos que `POST`. También acepta **solo** `{ "activo": true }` para reactivar: valida únicamente los campos que llegan. Si viene `especificaciones`, **la lista enviada reemplaza a la anterior** (`orphanRemoval` borra las que ya no están); si no viene, no se tocan.
Responde **200**: `Producto`. **400**. **404**.

### `DELETE /productos/{id}` · ADMIN · ✅ · 🟢

**Para qué:** desactivar un producto (deja de verse en la tienda, pero las ventas pasadas lo siguen mostrando).
**Vistas:** 5.2 Productos (interruptor: desactivar).

Pone `activo = false`. Responde **204** sin cuerpo. **404**.

### `GET /marcas` · `GET /categorias` · Público · ✅ · 🟢

**Para qué:** los filtros del catálogo con su contador, las listas desplegables del formulario y la tabla de 5.4.
**Vistas:** 4.2 Catálogo (filtros con contador) · buscador del encabezado (sugerencias) · 5.2 Productos (filtros) · 5.3 Crear/editar producto (listas desplegables) · 5.4 Marcas y categorías.

Responde **200**: `List<Marca>` / `List<Categoria>`. El público recibe **solo las activas**; el ADMIN recibe **todas**.

### `POST /marcas` · `POST /categorias` · ADMIN · ✅ · 🟢

**Para qué:** agregar una marca o una categoría.
**Vistas:** 5.4 Marcas y categorías.

Envía `{ "nombre": String }`. Responde **201**: `Marca` / `Categoria` con `activo: true`. **400** si el nombre está vacío. **409** si el nombre ya existe (sin distinguir mayúsculas).

### `PUT /marcas/{id}` · `PUT /categorias/{id}` · ADMIN · ✅ · 🟢

**Para qué:** renombrar, activar o desactivar una marca o categoría.
**Vistas:** 5.4 Marcas y categorías.

Envía `{ "nombre": String }` para renombrar y/o `{ "activo": Boolean }` para activar o desactivar. Responde **200**: `Marca` / `Categoria`. **400**. **404**. **409** si el nuevo nombre ya existe.

Las rutas `/marcas/**` y `/categorias/**` ya están en el gateway, y sus `GET` ya son públicos en este servicio.

## Pendientes por semana

### Semana 2–3
- [ ] `ProductoRequest` con validaciones: `@NotBlank nombre`, `@NotNull @DecimalMin("0.01") precio`, `@NotNull idMarca`, `@NotNull idCategoria`.
- [ ] `POST`, `PUT`, `DELETE` (borrado lógico). Validar que la marca y la categoría existan → 400 con `detalles` (`"idMarca: no existe"`).
- [ ] `GET /marcas` y `GET /categorias` (para los combos del formulario web).
- [ ] Paginación: `Pageable` + `Page<ProductoResponse>`. Para que el JSON salga estable, agregar `@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)`.
- [ ] Filtros: por categoría, marca y texto (`findByNombreContainingIgnoreCase` o `@Query`).
- [ ] `@PreAuthorize("hasRole('ADMIN')")` en los métodos de escritura (cuando `common-security` esté lista).

### Semana 5 · Datos semilla
- [ ] Completar `infra/mysql/inserts-test/02-catalogo.sql` hasta **200 productos** (el alcance del informe). Coordinar con Arévalo (inventario): cada producto necesita su stock (`inserts-test/03-inventario.sql` lo genera para todos los productos).
- [ ] La base en Aiven es compartida: los productos nuevos los carga Mathias en Aiven al mergear el PR.

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
| **C5** · `GET /productos/{id}` | Evelyn (pedidos) y Arévalo (inventario) | **2** | **200** `{id, nombre, descripcion, precio, imagenUrl, marca, categoria, activo}` · **404** si no existe. Hoy falta el campo **`activo`**: ms-pedidos lo necesita para no dejar agregar al carrito un producto descontinuado. Un producto inactivo responde **200 con `activo: false`** (no 404), para que las ventas pasadas lo puedan mostrar |
| **C9** · 200 productos en `inserts-test/02-catalogo.sql` | Mathias (los carga en Aiven) | 5 | El stock de cada producto se genera solo (`inserts-test/03-inventario.sql`) |
| `GET /productos` (paginado y con filtros), `GET /marcas`, `GET /categorias` | web-client | 3 | Para la grilla y los combos del formulario |

### Lo que necesito

| Contrato | Quién lo entrega | Semana | Para qué lo uso |
|---|---|---|---|
| **C2** · `JwtAuthFilter` | Galarza | 3 | `@PreAuthorize("hasRole('ADMIN')")` en crear, editar y desactivar |

## Contrato con otros servicios (no romperlo sin avisar)

`GET /productos/{id}` lo consumen **ms-pedidos** (Evelyn) e **ms-inventario** (Arévalo). ms-pedidos lee `id`, `nombre`, `precio` y `activo`. Si cambias esos nombres de campo, el checkout se rompe (contrato C5).

## Cómo correrlo

```bash
docker compose up -d ms-catalogo                 # todo en Docker (Eureka + ms-catalogo)
# o, para depurar desde el IDE o Maven:
docker compose up -d ms-discovery
./mvnw -q install -DskipTests                    # una vez, y cada vez que cambie common-security
./mvnw -pl ms-catalogo spring-boot:run           # lee AIVEN_* y DB_PASS_CATALOGO del .env
curl localhost:8082/productos/1
```
