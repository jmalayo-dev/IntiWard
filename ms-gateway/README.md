# ms-gateway · API Gateway

| | |
|---|---|
| **Responsable** | Integrante 1 · Mathias |
| **Puerto** | 8080 (la **única** URL pública del sistema) |
| **Nombre en Eureka** | `ms-gateway` |
| **Base de datos** | Ninguna |
| **Tecnología** | Spring Cloud Gateway (WebFlux, reactivo) |
| **Patrón** | API Gateway |

## Para qué existe

El cliente web y Postman conocen **una sola URL**: la del gateway. El gateway:

1. Recibe la petición.
2. **Valida el JWT una sola vez** (pendiente, semana 3).
3. Mira el prefijo de la ruta y la reenvía al servicio correcto, preguntándole a Eureka dónde está.
4. Propaga el usuario y el rol en cabeceras para que los servicios no tengan que volver a consultar a ms-auth.

## Tabla de rutas (ya configurada en `application.yml`)

| Prefijo | Servicio destino |
|---|---|
| `/auth/**`, `/usuarios/**` | `lb://ms-auth` |
| `/productos/**`, `/marcas/**`, `/categorias/**` | `lb://ms-catalogo` |
| `/stock/**`, `/movimientos/**` | `lb://ms-inventario` |
| `/carrito/**`, `/pedidos/**` | `lb://ms-pedidos` |
| `/comprobantes/**` | `lb://ms-comprobantes` |

> **Regla para el equipo:** si un integrante agrega un recurso con un prefijo nuevo (por ejemplo `/reportes`), tiene que pedirle al integrante 1 que agregue la ruta aquí.

## Estado actual

| Archivo | Estado |
|---|---|
| `application.yml`: rutas a los 5 servicios con `lb://` | ✅ Funcional y verificado |
| `filter/RequestLogFilter.java`: registra método y ruta de cada petición | ✅ |
| Validación del JWT | ⬜ Pendiente |
| Propagación de cabeceras `X-User` / `X-Rol` | ⬜ Pendiente |
| CORS | ⬜ Pendiente (solo si el cliente web llama desde el navegador) |

## Pendientes por semana

### Semana 3 · Seguridad en el borde
- [ ] Agregar `jjwt-api`, `jjwt-impl` y `jjwt-jackson` al `pom.xml` (las versiones ya están en el POM padre).
- [ ] Crear `filter/JwtGatewayFilter.java` (`GlobalFilter`, orden después del log):
  - Rutas públicas: `POST /auth/login`, `POST /auth/registro`, `GET /productos/**`, `GET /categorias/**`, `GET /marcas/**`, `/actuator/health`.
  - Resto: leer `Authorization: Bearer <token>`, validar firma y expiración con `JWT_SECRET` y responder **401** si falla.
  - Si es válido: agregar las cabeceras `X-User` (subject = correo) y `X-Rol` (claim `rol`) a la petición reenviada.
- [ ] Probar con Postman: sin token → 401; con token de ms-auth → 200.

### Semana 5
- [ ] (Opcional) `RequestRateLimiter` o timeouts globales: `spring.cloud.gateway.server.webflux.httpclient.response-timeout`.

### Semana 8 · Nube
- [ ] Perfil `cloud`: el gateway queda detrás de Nginx con HTTPS (Let's Encrypt). Solo Nginx expone 80 y 443.
- [ ] `JWT_SECRET` desde SSM Parameter Store.

## Cómo correrlo y probarlo

```bash
docker compose up -d --build
curl localhost:8080/productos          # → JSON del catálogo, a través del gateway
curl localhost:8080/actuator/gateway/routes   # → rutas cargadas
```

Si recibes **503** apenas levantas: el gateway todavía no recibió la lista de Eureka. Espera unos 30 s.

## Detalle técnico importante

El gateway es **reactivo (WebFlux)**, no servlet. Por eso **no** importa `common-security` (que es para servlet): la validación del JWT se implementa como `GlobalFilter` con `Mono`/`ServerWebExchange`, no con `OncePerRequestFilter`.

## Qué decir en la sustentación

> "El cliente nunca habla con un servicio directo. El gateway es la única puerta: valida el token una vez, lo convierte en cabeceras de usuario y rol, y enruta por prefijo usando Eureka. Eso nos da un solo punto para seguridad, logs y HTTPS."
