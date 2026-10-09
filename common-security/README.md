# common-security · Librería común de seguridad (JWT)

| | |
|---|---|
| **Responsable** | Integrante 2 (dueño de ms-auth) |
| **Tipo** | Librería Maven, **no** es un servicio: no tiene puerto ni `main` |
| **La importan** | ms-auth, ms-catalogo, ms-inventario, ms-pedidos, ms-comprobantes |

## Para qué existe

Para que los cinco servicios validen el token **de la misma forma** sin copiar código. Basta con poner la dependencia en el `pom.xml` y la configuración se activa sola (Spring Boot Auto-Configuration).

## Estado actual

| Archivo | Estado |
|---|---|
| `JwtProperties` | ✅ Lee `intiguard.jwt.secret` y `intiguard.jwt.expiration-minutes` (60) |
| `JwtService` | ✅ `generarToken(correo, rol)` y `validar(token)`, con test |
| `IntiGuardSecurityAutoConfiguration` | ✅ Registra `JwtService` y un `SecurityFilterChain` **que por ahora deja pasar todo** (`permitAll`) |
| `JwtAuthFilter` | ⬜ Pendiente |

> **Por qué deja pasar todo:** para que los demás integrantes puedan construir y probar sus CRUD desde la semana 2 sin esperar el login. El día que esta librería exija token, todos los servicios quedan protegidos a la vez.

> **Ojo con el orden:** la autoconfiguración se declara `before` de la de Spring Security **y** de la del actuator. Si se quita eso, gana la cadena por defecto de Spring y todos los endpoints piden Basic Auth (401).

## Pendientes (semana 3)

- [ ] Crear `JwtAuthFilter extends OncePerRequestFilter`:
  - Leer `Authorization: Bearer <token>` (o las cabeceras `X-User`/`X-Rol` que pone el gateway, a decidir con el integrante 1).
  - `jwtService.validar(token)` → crear `UsernamePasswordAuthenticationToken(correo, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)))` y ponerlo en el `SecurityContextHolder`.
  - Si el token es inválido, no autenticar: Spring responde 401.
- [ ] En `securityFilterChain`:
  - `addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)`.
  - Públicos: `/actuator/health`, `/auth/login`, `/auth/registro`, `GET /productos/**`, `GET /categorias/**`, `GET /marcas/**`.
  - Resto: `.anyRequest().authenticated()`.
- [ ] Respuesta 401/403 en JSON con el mismo formato que `ApiError` (`AuthenticationEntryPoint` y `AccessDeniedHandler`).
- [ ] Quitar el aviso "Using generated security password" del log (aparece porque no hay `UserDetailsService`): excluir `UserDetailsServiceAutoConfiguration`.
- [ ] Tests: token válido → autenticado con rol; token vencido → 401; sin token en ruta pública → 200.
- [ ] Avisar por el grupo cuando se active, porque desde ese momento Postman necesita `Authorization: Bearer {{token}}`.

Con esto, cualquier servicio puede proteger un endpoint así:

```java
@PreAuthorize("hasRole('ADMIN')")   // @EnableMethodSecurity ya está activado en la librería
@DeleteMapping("/{id}")
public ResponseEntity<Void> eliminar(@PathVariable Long id) { ... }
```

## Probar

```bash
./mvnw -pl common-security test
```
