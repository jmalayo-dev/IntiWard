# ms-auth · Usuarios, roles y login

| | |
|---|---|
| **Responsable** | Integrante 2 |
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
         activo, creado_en, actualizado_en)
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
| Registro, CRUD de usuarios | | ⬜ |

> El login (C3) lo hizo el integrante 1 en la semana 2, a pedido del equipo. Verificado contra Aiven por el gateway: 200 con token, 401 genérico, 400 con detalle, 423 al quinto intento y con la clave correcta mientras dura el bloqueo, y el contador guardado en la base.

> **Detalle importante:** `login()` usa `@Transactional(noRollbackFor = …)`. Sin eso, la excepción del 401 desharía el intento fallido recién sumado y la cuenta **nunca** se bloquearía. `LoginIntegracionTest` lo comprueba.

## Endpoints objetivo

| Método | Ruta | Quién | Estado |
|---|---|---|---|
| POST | `/auth/registro` | Público | ⬜ |
| POST | `/auth/login` `{correo, password}` → `{token, tipo, expiraEnMinutos, rol}` | Público | ✅ |
| GET | `/usuarios` | ADMIN | ✅ |
| GET | `/usuarios/{id}` | ADMIN o el mismo usuario | ✅ |
| POST | `/usuarios` | ADMIN | ⬜ |
| PUT | `/usuarios/{id}` | ADMIN | ⬜ |
| DELETE | `/usuarios/{id}` | ADMIN (borrado lógico: `activo = false`) | ⬜ |

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
| **C3** · `POST /auth/login` | Integrante 1 (gateway), web-client, Postman | 3 | ✅ Cuerpo `{correo, password}` → **200** `{token, tipo: "Bearer", expiraEnMinutos, rol}` · **400** formato inválido · **401** credenciales malas · **423** cuenta bloqueada |
| **C2** · `JwtAuthFilter` + `UsuarioActual` | Integrantes 3, 4 y 5 | 3 | ✅ Ver [`common-security/README.md`](../common-security/README.md) |

C1, C2 y C3 los adelantó el integrante 1 en la semana 2. **Lo que queda de tu lado: registro, CRUD de usuarios, pantallas y la colección de Postman.**

## Pendientes por semana

### Semana 2
- [ ] `POST /auth/registro`: `RegistroRequest` con validaciones, verificar `existsByCorreo` → **409** si ya existe, guardar con `passwordEncoder.encode()` y rol CLIENTE.
- [ ] `POST/PUT/DELETE /usuarios` (ADMIN).

### Semana 3 · Entrega clave: login con JWT
- [x] `AuthService.login(LoginRequest)` con las 6 reglas de arriba.
- [x] `AuthController`: `POST /auth/login` → `LoginResponse(token, "Bearer", 60, rol)`.
- [x] Excepciones `CredencialesInvalidasException` (401) y `CuentaBloqueadaException` (423) en el `GlobalExceptionHandler`.
- [x] `common-security` (`JwtAuthFilter`, `UsuarioActual`): lo hizo el integrante 1. Cuando el login funcione, avisar al equipo para pasar a `EXIGIR_TOKEN=true`.
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
