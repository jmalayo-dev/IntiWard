# common-security · Librería común de seguridad (JWT)

| | |
|---|---|
| **Responsable** | Galarza (dueño de ms-auth) |
| **Tipo** | Librería Maven, **no** es un servicio: no tiene puerto ni `main` |
| **La importan** | ms-auth, ms-catalogo, ms-inventario, ms-pedidos, ms-comprobantes |

## Para qué existe

Para que los cinco servicios validen el token **de la misma forma** sin copiar código. Basta con poner la dependencia en el `pom.xml` y la configuración se activa sola (Spring Boot Auto-Configuration).

## Estado actual

| Archivo | Estado |
|---|---|
| `JwtProperties` | ✅ Lee `intiguard.jwt.secret` y `intiguard.jwt.expiration-minutes` (60) |
| `JwtService` | ✅ `generarToken(idUsuario, correo, rol)` y `validar(token)` (contrato C1) |
| `JwtAuthFilter` | ✅ Lee `Authorization: Bearer`, valida y deja el usuario y su rol en el `SecurityContext`. Token inválido o vencido → **401** (C2) |
| `UsuarioActual` / `UsuarioAutenticado` | ✅ `id()`, `correo()`, `rol()`, `esAdmin()` del usuario de la petición (C2) |
| `RespuestaErrorJson` | ✅ 401 y 403 en el mismo formato que `ApiError` |
| `SeguridadProperties` | ✅ `intiguard.security.exigir-token` y `intiguard.security.rutas-publicas` por servicio |
| `IntiGuardSecurityAutoConfiguration` | ✅ Arma la cadena de seguridad con todo lo anterior y apaga el usuario y la clave que Spring genera por defecto |
| Tests | ✅ `JwtServiceTest` (2) y `JwtAuthFilterTest` (8: pública, 401, token válido, token inválido, 403 por rol, ADMIN, y modo sin token obligatorio) |

> Lo construyó Mathias en la semana 2 para desbloquear al equipo (C1 y C2). Galarza sigue siendo el dueño: el login (C3) usa `JwtService.generarToken(usuario.getId(), correo, rol)`.

> **Ojo con el orden:** la autoconfiguración se declara `before` de la de Spring Security, de la de `UserDetailsService` **y** de la del actuator. Si se quita eso, gana la cadena por defecto de Spring y todos los endpoints piden Basic Auth (401).

> **`JwtAuthFilter` no es un bean a propósito.** Se crea dentro de `securityFilterChain`. Si fuera un `@Bean`, Spring Boot también lo registraría como filtro normal del servidor: correría fuera de Spring Security y el usuario se perdería.

## Cómo usarlo en tu servicio

**1. Declarar las rutas públicas** en tu `application.yml` (ya está hecho en los 5 servicios):

```yaml
intiguard:
  security:
    exigir-token: ${EXIGIR_TOKEN:false}
    rutas-publicas:           # "METODO /ruta/**" o "/ruta/**" para cualquier método
      - GET /productos/**
```

`/actuator/health` y `/error` siempre son públicas.

**2. `EXIGIR_TOKEN`** (variable del `.env`):

| Valor | Comportamiento |
|---|---|
| `false` (por defecto, mientras no exista el login) | Si llega un token, se valida y se usa (`UsuarioActual` funciona). Si no llega, la petición pasa igual. Un token inválido **sí** se rechaza (401) |
| `true` (perfil `cloud`, y para todos cuando exista C3) | Todo lo que no sea ruta pública exige un token válido: 401 si falta |

**3. Saber quién hace la petición:**

```java
private final UsuarioActual usuarioActual;   // se inyecta como cualquier bean

Long idUsuario = usuarioActual.id();        // 401 si no hay token
boolean admin = usuarioActual.esAdmin();
usuarioActual.obtener()                     // Optional, para rutas donde el token es opcional
```

**4. Exigir un rol:**

```java
@PreAuthorize("hasRole('ADMIN')")   // @EnableMethodSecurity ya está activado en la librería
@DeleteMapping("/{id}")
public ResponseEntity<Void> eliminar(@PathVariable Long id) { ... }
```

Sin token → **401**; con token de CLIENTE → **403**. Los `GlobalExceptionHandler` de los 5 servicios ya traducen estas excepciones a 401 y 403 (antes salían como 500).

**5. Conseguir un token mientras no exista el login:**

```bash
./infra/scripts/token-dev.sh 1 ADMIN          # idUsuario 1, rol ADMIN, válido 60 min
curl -H "Authorization: Bearer $(./infra/scripts/token-dev.sh 7 CLIENTE)" localhost:8084/pedidos
```

Firma con la `JWT_SECRET` del `.env` (o la de desarrollo), igual que lo hará el login.

## Dependencias con otros servicios

Esta librería la importan los 5 servicios de negocio. El mapa completo está en [`ARRANQUE_PROYECTO.md`](../ARRANQUE_PROYECTO.md#quién-le-debe-a-quién).

### Lo que debo (Galarza)

| Contrato | A quién | Semana | Detalle |
|---|---|---|---|
| **C1** · claim `idUsuario` en el token | Todos | 2 | ✅ `generarToken(idUsuario, correo, rol)` y la constante `CLAIM_ID_USUARIO = "idUsuario"` |
| **C2** · `JwtAuthFilter` | César, Arévalo, Evelyn y Galarza (comprobantes) | 3 | ✅ Rutas públicas sin token; el resto exige `Authorization: Bearer` cuando `EXIGIR_TOKEN=true`. Responde **401** (sin token o token inválido) y **403** (rol insuficiente) en formato `ApiError` |
| **C2** · `UsuarioActual` | Evelyn, Arévalo y Galarza (comprobantes) | 3 | ✅ `id()`, `correo()`, `rol()` y `esAdmin()` del usuario autenticado, leídos del `SecurityContext`. Nadie parsea el token por su cuenta |

Cuando el login (C3) esté listo, se pasa `EXIGIR_TOKEN=true` para todos y **se avisa por el grupo**: desde ese momento Postman necesita el token en todas las rutas protegidas.

## Pendientes

> **Estado al cierre (19/10/2026):** [x] hecho · ~~tachado~~ no se hizo o se resolvió de otra forma (el motivo va al lado).

- ~~El login (C3) ya existe. Cuando exista también el registro de clientes: pasar `EXIGIR_TOKEN=true` en los `.env` del equipo y en `docker-compose.yml`, y avisar al equipo.~~ → en Render el perfil `cloud` exige el token siempre; en local sigue en `false`.

## Probar

```bash
./mvnw -pl common-security test
```
