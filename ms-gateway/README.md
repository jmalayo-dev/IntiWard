# ms-gateway · API Gateway

| | |
|---|---|
| **Responsable** | Mathias · rama `b-alayo` |
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

> **Regla para el equipo:** si un integrante agrega un recurso con un prefijo nuevo (por ejemplo `/reportes`), tiene que pedirle a Mathias que agregue la ruta aquí.

## Estado actual

| Archivo | Estado |
|---|---|
| `application.yml`: rutas a los 5 servicios con `lb://` | ✅ Funcional y verificado |
| `filter/RequestLogFilter.java`: registra método y ruta de cada petición | ✅ |
| `filter/JwtGatewayFilter.java`: contrato C4 | ✅ Rutas internas → 403 siempre; rutas públicas sin token; token inválido o vencido → 401; sin token → 401 solo si `EXIGIR_TOKEN=true` |
| `filter/GatewaySeguridadProperties.java` | ✅ Lee `intiguard.jwt.secret` e `intiguard.security.*` (rutas públicas e internas en `application.yml`) |
| Tests | ✅ `JwtGatewayFilterTest` (6 casos; incluye `GET /stock?ids=` público) |
| CORS | En desarrollo no hace falta: la app React llama a `/api` y el proxy de Vite lo manda al gateway (un solo origen). Para la nube: pendiente (S3) |

El token **sigue viajando** en `Authorization` hasta el servicio, que lo vuelve a validar con `common-security`. No se agregan cabeceras `X-User`/`X-Rol`: cada servicio lee el usuario del token con `UsuarioActual`, y así las llamadas Feign entre servicios (que no pasan por el gateway) también quedan protegidas.

## Dependencias con otros servicios

El mapa completo está en [`ARRANQUE_PROYECTO.md`](../ARRANQUE_PROYECTO.md#quién-le-debe-a-quién).

### Lo que debo (Mathias)

**C4 · Seguridad en la puerta de entrada — semana 3.** ✅ Hecho en la semana 2.

- **Rutas públicas** (sin token): `POST /auth/login`, `POST /auth/registro`, `GET /productos/**`, `GET /marcas`, `GET /categorias`, `GET /stock` (stock de varios productos con `?ids=`) y `GET /stock/{idProducto}`.
- **El resto exige un JWT válido**: si falta o es inválido, **401** sin llegar al servicio. El token sigue viajando en `Authorization`, porque cada servicio lo vuelve a validar con `common-security`.
- **Rutas internas bloqueadas desde afuera** (**403**): `/stock/reservar`, `/stock/confirmar`, `/stock/liberar` y `POST /comprobantes`. Solo las llama ms-pedidos, por Feign y dentro de la red; si el gateway las dejara pasar, un cliente podría reservar stock o emitir comprobantes a mano.

### Lo que necesito

| Contrato | Quién lo entrega | Semana | Para qué lo uso |
|---|---|---|---|
| **C1** · formato del JWT (`sub`, `rol`, `idUsuario`) | Galarza | 2 | Validar el token |
| **C3** · `POST /auth/login` | Galarza | 3 | Probar el flujo completo con un token real |

**Cómo avanzar sin esperar:** `JwtService` ya funciona. Puedes generar un token de prueba en un test y desarrollar el filtro sin esperar el login.

## Pendientes por semana

### Semana 3 · Seguridad en el borde
- [x] `JwtGatewayFilter` (C4) con rutas públicas e internas configurables, y sus tests.
- [x] Verificado con el sistema completo y `EXIGIR_TOKEN=true`: sin token → 401, token válido → 200, vencido o inventado → 401, rutas internas → 403, y los servicios rechazan sin token aunque se los llame sin pasar por el gateway.
- [x] Flujo completo con un token real del login (C3): login por el gateway → 200 con token → ese token abre `/usuarios/{id}`.
- [ ] Pasar `EXIGIR_TOKEN=true` para todos cuando exista el registro de clientes.

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
