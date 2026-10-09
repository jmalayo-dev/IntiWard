# ms-auth · Usuarios, roles y login

| | |
|---|---|
| **Responsable** | Integrante 2 |
| **Puerto** | 8081 |
| **Nombre en Eureka** | `ms-auth` |
| **Schema MySQL** | `db_auth` (usuario `usr_auth`) · script `infra/mysql/01-auth.sql` |
| **Rutas en el gateway** | `/auth/**`, `/usuarios/**` |
| **Tarea transversal** | Librería `common-security` (ver su README) |
| **Pantallas web** | Registro, login, perfil y gestión de usuarios |

## Para qué existe

Es el dueño de **quién es quién**. Registra usuarios, guarda su contraseña con BCrypt, y en el login emite un **JWT con el rol dentro**. Los demás servicios nunca consultan a ms-auth en cada petición: confían en el token.

La fila de la ficha **"Validaciones en el login"** es *obligatoria para nota máxima* y vive aquí: es el servicio más visible en la sustentación.

## Modelo de datos

```
rol (id, nombre UNIQUE)                       ← semilla: ADMIN, CLIENTE
usuario (id, nombre, correo UNIQUE, password(BCrypt), id_rol → rol,
         activo, intentos_fallidos, bloqueado_hasta, fecha_registro)
```

Usuario semilla: **`admin@intiguard.pe` / `Admin12345`** (rol ADMIN). Solo para desarrollo.

## Estado actual

| Capa | Archivo | Estado |
|---|---|---|
| Entity | `Rol`, `Usuario` (con `activo`, `intentosFallidos`, `bloqueadoHasta`) | ✅ |
| Repository | `RolRepository.findByNombre`, `UsuarioRepository.findByCorreo/existsByCorreo` | ✅ con test `@DataJpaTest` |
| DTO | `UsuarioResponse` (sin password), `LoginRequest` (con validaciones), `LoginResponse` | ✅ |
| Mapper | `UsuarioMapper` | ✅ |
| Service | `UsuarioService.listar/obtener` | ✅ |
| Controller | `GET /usuarios`, `GET /usuarios/{id}` | ✅ |
| Config | `PasswordConfig`: `BCryptPasswordEncoder(10)` | ✅ |
| Errores | `GlobalExceptionHandler` + `ApiError` (404, 400, 500) | ✅ |
| Login, registro, CRUD completo | | ⬜ |

## Endpoints objetivo

| Método | Ruta | Quién | Estado |
|---|---|---|---|
| POST | `/auth/registro` | Público | ⬜ |
| POST | `/auth/login` | Público | ⬜ |
| GET | `/usuarios` | ADMIN | ✅ (sin protección aún) |
| GET | `/usuarios/{id}` | ADMIN o el mismo usuario | ✅ (sin protección aún) |
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

## Pendientes por semana

### Semana 2
- [ ] `POST /auth/registro`: `RegistroRequest` con validaciones, verificar `existsByCorreo` → **409** si ya existe, guardar con `passwordEncoder.encode()` y rol CLIENTE.
- [ ] `POST/PUT/DELETE /usuarios` (ADMIN).

### Semana 3 · Entrega clave: login con JWT
- [ ] `AuthService.login(LoginRequest)` con las 6 reglas de arriba.
- [ ] `AuthController`: `POST /auth/login` → `LoginResponse(token, "Bearer", 60, rol)`.
- [ ] Excepciones `CredencialesInvalidasException` (401) y `CuentaBloqueadaException` (423) en el `GlobalExceptionHandler`.
- [ ] Terminar `common-security` (`JwtAuthFilter`) y avisar al equipo.
- [ ] `@PreAuthorize("hasRole('ADMIN')")` en los endpoints de administración.

### Semana 6 · Pantallas web (en `web-client`)
- [ ] `/login`, `/registro`, `/perfil`, `/admin/usuarios`. Guardar el token en la sesión HTTP del web-client.

### Semana 7 · Pruebas
- [ ] Tests de `AuthService` con Mockito: credenciales correctas, correo inexistente, contraseña mala, quinto intento bloquea, cuenta bloqueada responde 423, cuenta inactiva.
- [ ] Colección `postman/ms-auth.postman_collection.json` que guarde el token en `{{token}}` con un script de test.

## Cómo correrlo

```bash
docker compose up -d mysql ms-discovery          # dependencias
./mvnw -pl ms-auth spring-boot:run               # usa MySQL en localhost:3307
curl localhost:8081/usuarios
```

## Qué decir en la sustentación

> "La contraseña se guarda con BCrypt fuerza 10 y nunca sale del servicio. El login responde lo mismo si el correo no existe o si la clave está mal, para no revelar qué correos están registrados. Cinco intentos fallidos bloquean la cuenta quince minutos y devolvemos 423. El token dura una hora y lleva el rol, así el gateway no consulta a ms-auth en cada petición."
