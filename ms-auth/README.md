# ms-auth · Usuarios, roles y login

| | |
|---|---|
| **Responsable** | Galarza (también lleva ms-comprobantes) · rama `b-galarza` |
| **Puerto** | 8081 |
| **Nombre en Eureka** | `ms-auth` |
| **Schema MySQL** | `db_auth` (usuario `ms_auth`) · script `infra/mysql/01-auth.sql` |
| **Rutas en el gateway** | `/auth/**`, `/usuarios/**` |
| **Tarea transversal** | Librería `common-security` (ver su README) |
| **Pantallas web** | Registro, login, perfil y gestión de usuarios |

## Para qué existe

Es el dueño de **quién es quién**. Registra usuarios, guarda su contraseña con BCrypt, y en el login emite un **JWT con el rol dentro**. Los demás servicios nunca consultan a ms-auth en cada petición: confían en el token.

La fila de la ficha **"Validaciones en el login"** es *obligatoria para nota máxima* y vive aquí: es el servicio más visible en la sustentación.

## Modelo de datos

```
rol (id_rol, nombre UNIQUE)                   ← semilla: ADMIN, CLIENTE
usuario (id_usuario, nombres, apellidos, correo UNIQUE, telefono, direccion, foto_url,
         clave_hash (BCrypt), id_rol → rol, intentos_fallidos CHECK 0..5, bloqueado_hasta,
         ultimo_acceso, activo, creado_en, actualizado_en)
```

La BD valida el formato del correo y que `intentos_fallidos` esté entre 0 y 5 (CHECK).

**Administrador inicial:** no está en el SQL. `config/AdminInicializador` lo crea al arrancar si no existe, con `intiguard.admin.*`. Los valores salen de `ADMIN_CORREO` y `ADMIN_CLAVE` del `.env` (en el servidor, de Parameter Store). Si faltan, ms-auth arranca igual, avisa en el log y no crea el admin. Como la base es compartida, basta con que se cree una vez.

## Guía de negocio

### Roles (`rol.nombre`)

| Valor | Quién es | Qué puede hacer |
|---|---|---|
| `ADMIN` | Personal de IntiGuard | Gestiona productos, stock y usuarios; ve todas las ventas y comprobantes. Solo otro ADMIN puede asignar este rol (el primero lo crea `AdminInicializador`). |
| `CLIENTE` | Persona que compra en la tienda | Compra y ve solo lo suyo. Es el rol que recibe todo el que se registra por `/auth/registro`. |

El rol viaja **dentro del JWT**: los demás servicios lo leen del token y nunca consultan esta tabla.

### Quién referencia a este servicio (`-- ref. db_auth.usuario`)

`db_pedidos.carrito.id_usuario` y `db_pedidos.venta.id_usuario` guardan el id de un usuario de aquí. Por eso:

- **El `id_usuario` va dentro del JWT** (como claim), para que ms-pedidos sepa de quién es el carrito sin llamar a ms-auth.
- **Nunca borrar un usuario con `DELETE`**: hay ventas que lo referencian. Se desactiva con `activo = false` y el login lo rechaza.
- **Nunca cambiar el `id_usuario` de nadie**: sus pedidos quedarían huérfanos.

### Bloqueo de cuenta

Cada clave errada suma 1 a `intentos_fallidos`. Al llegar a 5, se fija `bloqueado_hasta = ahora + 15 min` y se responde **423**. Un login correcto vuelve el contador a 0. La BD no deja pasar de 5 (CHECK).

## Estado actual

| Capa | Archivo | Estado |
|---|---|---|
| Entity | `Rol`, `Usuario` (con `activo`, `intentosFallidos`, `bloqueadoHasta`, auditoría `creadoEn`/`actualizadoEn`) | ✅ |
| Repository | `RolRepository.findByNombre`, `UsuarioRepository.findByCorreo/existsByCorreo` | ✅ con test `@DataJpaTest` |
| DTO | `UsuarioResponse` (sin clave), `LoginRequest` (con validaciones), `LoginResponse` | ✅ |
| Mapper | `UsuarioMapper` | ✅ |
| Service | `UsuarioService.listar/obtener`; **`AuthService.login`** con las 6 reglas | ✅ |
| Controller | **`AuthController`: `POST /auth/login`**; `UsuarioController`: `GET /usuarios` (ADMIN), `GET /usuarios/{id}` (ADMIN o uno mismo) | ✅ |
| Config | `PasswordConfig`: `BCryptPasswordEncoder(10)`; `AdminInicializador` + `AdminProperties` | ✅ |
| Errores | `GlobalExceptionHandler` + `ApiError` (400, 401, 403, 404, **423**, 500); `CredencialesInvalidasException`, `CuentaBloqueadaException` | ✅ |
| Tests | `AuthServiceTest` (7, Mockito: una prueba por regla) y `LoginIntegracionTest` (5, HTTP + H2: el contador de intentos se guarda, el token sirve, `/usuarios` solo ADMIN) | ✅ |
| Registro, perfil y CRUD de usuarios | `POST /auth/registro`, `GET`/`PUT /usuarios/me`, `POST`/`PUT`/`DELETE /usuarios` (borrado lógico); `ultimoAcceso` y `bloqueadoHasta` en la respuesta | ✅ con `UsuarioIntegracionTest` (6, HTTP + H2) |

> El login (C3) lo hizo Mathias en la semana 2, a pedido del equipo. Verificado contra Aiven por el gateway: 200 con token, 401 genérico, 400 con detalle, 423 al quinto intento y con la clave correcta mientras dura el bloqueo, y el contador guardado en la base.

> **Detalle importante:** `login()` usa `@Transactional(noRollbackFor = …)`. Sin eso, la excepción del 401 desharía el intento fallido recién sumado y la cuenta **nunca** se bloquearía. `LoginIntegracionTest` lo comprueba.

## Endpoints objetivo

| Método | Ruta | Quién | Estado | Bloqueo |
|---|---|---|---|---|
| POST | `/auth/login` | Público | ✅ | 🟢 |
| POST | `/auth/registro` | Público | ✅ | 🟢 |
| GET | `/usuarios/me` | Con sesión | ✅ | 🟢 |
| PUT | `/usuarios/me` | Con sesión | ✅ | 🟢 |
| GET | `/usuarios` | ADMIN | ✅ | 🟢 |
| GET | `/usuarios/{id}` | ADMIN o el mismo usuario | ✅ | 🟢 |
| POST | `/usuarios` | ADMIN | ✅ | 🟢 |
| PUT | `/usuarios/{id}` | ADMIN | ✅ | 🟢 |
| DELETE | `/usuarios/{id}` | ADMIN (borrado lógico: `activo = false`) | ✅ | 🟢 |

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

### Objeto `Usuario` (`UsuarioResponse`)

Lo devuelven `POST /auth/registro`, `/usuarios/me` y todas las rutas de `/usuarios`. **Nunca lleva la clave.**

```json
{ "id": 1, "nombres": "Ana", "apellidos": "Torres", "correo": "ana.torres@gmail.com", "telefono": "987654321",
  "direccion": "Av. Arequipa 123, Lima", "fotoUrl": null, "rol": "CLIENTE", "activo": true,
  "creadoEn": "2026-09-01T10:00:00", "bloqueadoHasta": null, "ultimoAcceso": "2026-10-15T09:12:00" }
```

| Atributo | Tipo | Para qué lo usa la web |
|---|---|---|
| `id` | `Long` | Editar, desactivar o abrir al usuario; enlazar una venta con su cliente |
| `nombres` | `String` | Saludo del encabezado, avatar con iniciales, tablas |
| `apellidos` | `String` | Nombre completo en tablas, ventas y checkout |
| `correo` | `String` | Identificador visible: perfil y tablas |
| `telefono` | `String` o `null` | Formulario de perfil y de usuario |
| `direccion` | `String` o `null` | Perfil, y dato del cliente en el detalle de venta |
| `fotoUrl` | `String` o `null` | Foto del avatar; con `null` se muestran las iniciales |
| `rol` | `String`: `ADMIN` · `CLIENTE` | Insignia de rol en la tabla de usuarios y filtro por rol |
| `activo` | `Boolean` | Interruptor activo/inactivo; un inactivo no puede iniciar sesión |
| `creadoEn` | `LocalDateTime` | Columna "Registrado" y cálculo de clientes nuevos en el resumen (5.1) |
| `bloqueadoHasta` | `LocalDateTime` o `null` · **nuevo** | Insignia "Bloqueado hasta HH:mm" (5.10). **`null`** si no está bloqueado **o si el bloqueo ya venció** (no devolver una fecha pasada). La columna ya existe |
| `ultimoAcceso` | `LocalDateTime` o `null` · **nuevo** | Columna "Último acceso" (5.10). Se actualiza en cada login correcto. La columna `usuario.ultimo_acceso` (`DATETIME`, puede ser null) **ya existe en Aiven** desde el 2026-10-17: falta agregar el campo `ultimoAcceso` a la entidad `Usuario` |

### `POST /auth/login` · Público · ✅ · 🟢

**Para qué:** iniciar sesión y obtener el token que la web manda en cada petición.
**Vistas:** 4.4 Iniciar sesión.

| Envía | Tipo | Regla |
|---|---|---|
| `correo` | `String` | Obligatorio, formato de correo |
| `password` | `String` | Obligatorio, mínimo 8 caracteres |

Responde **200** (`LoginResponse`):

| Atributo | Tipo | Para qué lo usa la web |
|---|---|---|
| `token` | `String` | Se guarda y va en cada petición como `Authorization` |
| `tipo` | `String` (`"Bearer"`) | Prefijo del header |
| `expiraEnMinutos` | `long` (60) | Informativo; la expiración real se lee del claim `exp` |
| `rol` | `String` | Redirige: `ADMIN` → `/admin`, `CLIENTE` → la página de origen o el inicio |

Errores: **400** formato inválido · **401** `"Credenciales inválidas"` (el mismo texto si el correo no existe, si la clave está mal o si la cuenta está inactiva) · **423** al 5.º intento fallido (bloqueo de 15 min).


### `POST /auth/registro` · Público · ✅ · 🟢

**Para qué:** que un visitante cree su cuenta de cliente para poder comprar.
**Vistas:** 4.5 Crear cuenta.

| Envía (`RegistroRequest`) | Tipo | Regla |
|---|---|---|
| `nombres` | `String` | Obligatorio |
| `apellidos` | `String` | Obligatorio |
| `correo` | `String` | Obligatorio, formato de correo, único |
| `telefono` | `String` | Opcional |
| `direccion` | `String` | Opcional |
| `password` | `String` | Obligatorio, mínimo 8 caracteres. Se guarda con BCrypt |

Responde **201**: `Usuario`, **siempre con rol `CLIENTE`** aunque el cuerpo diga otra cosa.
Errores: **400** con `detalles` por campo · **409** `"El correo ya está registrado"`.

Ya es ruta pública en el gateway y en ms-auth.

### `GET /usuarios/me` · Con sesión · ✅ · 🟢

**Para qué:** que la web conozca el nombre, la foto y los datos de quien inició sesión. El token solo trae correo, rol e id.
**Vistas:** 4.7 Checkout (prellena el nombre) · 4.8 Compra confirmada · 4.13 Mi perfil · encabezado de la tienda (avatar y nombre, en todas las páginas con sesión) · barra lateral del panel (tarjeta de la cuenta, en todas las páginas `/admin`).

Responde **200**: el `Usuario` del token (`usuarioActual.id()`). **401** sin token.

Nota técnica: declara `@GetMapping("/me")` en `UsuarioController`. Spring prioriza la ruta literal sobre `/{id}`, así que no choca.

### `PUT /usuarios/me` · Con sesión · ✅ · 🟢

**Para qué:** que el usuario edite sus datos personales, su foto y su contraseña.
**Vistas:** 4.13 Mi perfil.

Envía **solo los campos que cambian** (todos opcionales):

| Campo | Tipo | Regla |
|---|---|---|
| `nombres` · `apellidos` · `telefono` · `direccion` | `String` | Datos personales |
| `fotoUrl` | `String` o `null` | Nueva foto (URL); **`null` la quita** |
| `password` | `String` | Si viene, mínimo 8 caracteres y se guarda con BCrypt |

`correo` y `rol` **no** se cambian por aquí: si vienen, se ignoran.
Responde **200**: el `Usuario` actualizado. **400** con `detalles`. **401**.

Nota técnica: para distinguir "no vino `fotoUrl`" de "vino `fotoUrl: null`", recibe el cuerpo como `Map<String, Object>` y usa `containsKey`.

### `GET /usuarios` · ADMIN · ✅ · 🟢

**Para qué:** la tabla de usuarios del panel, y poner el nombre del cliente al lado de cada venta.
**Vistas:** 5.1 Resumen (clientes nuevos) · 5.7 Ventas (nombre del cliente) · 5.10 Usuarios.

Responde **200**: `List<Usuario>` con **todos, incluidos los inactivos**. **403** si no es ADMIN.

### `GET /usuarios/{id}` · ADMIN o el mismo usuario · ✅ · 🟢

**Para qué:** los datos del cliente en el detalle de una venta del panel.
**Vistas:** detalle de venta del panel (`/admin/ventas/:id`).

Responde **200**: `Usuario`. **403** si es otro usuario y quien pide no es ADMIN. **404**.

### `POST /usuarios` · ADMIN · ✅ · 🟢

**Para qué:** que el administrador cree cuentas: otro administrador o un cliente.
**Vistas:** 5.10 Usuarios (ventana "Nuevo usuario").

Envía lo mismo que el registro, más `rol` (`String`: `ADMIN` o `CLIENTE`; cualquier otro valor se toma como `CLIENTE`).
Responde **201**: `Usuario`. **400** con `detalles`. **409** si el correo ya existe.

### `PUT /usuarios/{id}` · ADMIN · ✅ · 🟢

**Para qué:** editar los datos de una cuenta, cambiar su rol o reactivarla.
**Vistas:** 5.10 Usuarios (editar y reactivar).

Envía cualquiera de estos campos: `nombres`, `apellidos`, `correo`, `telefono`, `direccion`, `fotoUrl` (`String`), `rol` (`String`), `activo` (`Boolean`), `password` (`String`, opcional, mínimo 8).

- **`{ "activo": true }` reactiva la cuenta y también levanta el bloqueo por intentos** (`intentos_fallidos = 0`, `bloqueado_hasta = null`).

Responde **200**: `Usuario`. **400**. **404**. **409** si el nuevo correo ya existe.

### `DELETE /usuarios/{id}` · ADMIN · ✅ · 🟢

**Para qué:** desactivar una cuenta sin borrarla (puede tener ventas).
**Vistas:** 5.10 Usuarios (desactivar).

Pone `activo = false`. Responde **204** sin cuerpo. **404**.

## Reglas del login (del plan, al pie de la letra)

1. `@Email`, `@NotBlank`, `@Size(min = 8)` en `LoginRequest` → **400** con detalle (ya está el DTO).
2. Mensaje **genérico** "Credenciales inválidas" tanto si el correo no existe como si la contraseña está mal → **401**.
3. **5 intentos fallidos seguidos → bloqueo de 15 minutos** (`intentosFallidos`, `bloqueadoHasta`) → **423 Locked**. Un login correcto reinicia el contador.
4. Cuenta con `activo = false` → rechazar el login.
5. JWT de **1 hora** con el rol en el claim `rol` (`JwtService.generarToken(correo, rol)` ya existe).
6. La contraseña **nunca** se devuelve ni se escribe en el log.

## Dependencias con otros servicios

ms-auth no depende de nadie, pero **todo el equipo depende de ms-auth**. El mapa completo está en [`ARRANQUE_PROYECTO.md`](../ARRANQUE_PROYECTO.md#quién-le-debe-a-quién).

### Lo que debo (me lo piden)

| Contrato | A quién | Semana | Detalle |
|---|---|---|---|
| **C1** · formato del JWT | Todos | **2** | ✅ Claims: `sub` = correo, `rol` = `ADMIN`/`CLIENTE`, **`idUsuario`** = id numérico. En el login: `jwtService.generarToken(usuario.getId(), usuario.getCorreo(), rol)`. Viaja como `Authorization: Bearer <token>` y vence a los 60 min |
| **C3** · `POST /auth/login` | Mathias (gateway y cliente web), Postman | 3 | ✅ Cuerpo `{correo, password}` → **200** `{token, tipo: "Bearer", expiraEnMinutos, rol}` · **400** formato inválido · **401** credenciales malas · **423** cuenta bloqueada |
| **C2** · `JwtAuthFilter` + `UsuarioActual` | César, Arévalo, Evelyn y Galarza (comprobantes) | 3 | ✅ Ver [`common-security/README.md`](../common-security/README.md) |

C1, C2 y C3 los adelantó Mathias en la semana 2. **Lo que queda de tu lado: registro, CRUD de usuarios, pantallas y la colección de Postman.**

## Pendientes por semana

### Semana 2
- [ ] `POST /auth/registro`: `RegistroRequest` con validaciones, verificar `existsByCorreo` → **409** si ya existe, guardar con `passwordEncoder.encode()` y rol CLIENTE.
- [ ] `POST/PUT/DELETE /usuarios` (ADMIN).

### Semana 3 · Entrega clave: login con JWT
- [x] `AuthService.login(LoginRequest)` con las 6 reglas de arriba.
- [x] `AuthController`: `POST /auth/login` → `LoginResponse(token, "Bearer", 60, rol)`.
- [x] Excepciones `CredencialesInvalidasException` (401) y `CuentaBloqueadaException` (423) en el `GlobalExceptionHandler`.
- [x] `common-security` (`JwtAuthFilter`, `UsuarioActual`): lo hizo Mathias. Cuando el login funcione, avisar al equipo para pasar a `EXIGIR_TOKEN=true`.
- [x] `@PreAuthorize("hasRole('ADMIN')")` en `GET /usuarios`; `GET /usuarios/{id}` solo ADMIN o uno mismo.
- [ ] Lo mismo en `POST`, `PUT` y `DELETE /usuarios` cuando existan.

### Semana 6 · Pantallas web (en `web-client`)
- [ ] `/login`, `/registro`, `/perfil`, `/admin/usuarios`. Guardar el token en la sesión HTTP del web-client.

### Semana 7 · Pruebas
- [x] Tests de `AuthService` con Mockito: credenciales correctas, correo inexistente, contraseña mala, quinto intento bloquea, cuenta bloqueada responde 423, cuenta inactiva.
- [ ] Tests del registro.
- [ ] Colección `postman/ms-auth.postman_collection.json` que guarde el token en `{{token}}` con un script de test.

## Cómo correrlo

```bash
docker compose up -d ms-auth                     # todo en Docker (Eureka + ms-auth)
# o, para depurar desde el IDE o Maven:
docker compose up -d ms-discovery
./mvnw -q install -DskipTests                    # una vez, y cada vez que cambie common-security
./mvnw -pl ms-auth spring-boot:run               # lee AIVEN_*, DB_PASS_AUTH y ADMIN_* del .env
curl localhost:8081/usuarios
```

## Qué decir en la sustentación

> "La contraseña se guarda con BCrypt fuerza 10 y nunca sale del servicio. El login responde lo mismo si el correo no existe o si la clave está mal, para no revelar qué correos están registrados. Cinco intentos fallidos bloquean la cuenta quince minutos y devolvemos 423. El token dura una hora y lleva el rol, así el gateway no consulta a ms-auth en cada petición."
