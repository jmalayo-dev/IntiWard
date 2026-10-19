# web-client · Cliente web (Thymeleaf)

> **Sin terminar y en desuso (19/10/2026).** La web de IntiGuard es la app React del repositorio `DSW_II_PROYECTO_WEB`, publicada en https://intiward-web.onrender.com. Este módulo queda solo como registro.

| | |
|---|---|
| **Responsable del layout** | Integrante 1 (layout base, menú, sesión) |
| **Pantallas** | Repartidas: cada integrante hace las de su servicio |
| **Puerto** | 8090 · http://localhost:8090 |
| **Consume** | **Solo** el gateway (`GATEWAY_URL`, por defecto `http://localhost:8080`) |

## Para qué existe

Cubre la fila de la ficha "consumo de la API por un cliente" y deja explícita la **arquitectura cliente-servidor**: el navegador habla con el web-client, el web-client habla con el gateway y nunca toca un servicio directamente. No tiene base de datos ni se registra en Eureka.

> ⚠️ Decisión abierta: preguntar al docente si una colección de Postman cuenta como cliente. Si la respuesta es sí, este módulo es lo primero que se recorta.

## Estado actual

| Archivo | Estado |
|---|---|
| `config/GatewayConfig`: `RestClient` apuntando al gateway | ✅ |
| `client/CatalogoApi` + `ProductoView`: lista productos; si el gateway cae, muestra un aviso | ✅ |
| `controller/HomeController`: `GET /` | ✅ muestra los productos reales |
| `templates/fragments/layout.html`: `head`, `navbar`, `footer` (Bootstrap 5) | ✅ base |
| `templates/index.html` | ✅ grilla del catálogo |

## Cómo agregar una pantalla (convención para todos)

1. Un `XxxApi` en `client/` que llame al gateway con `RestClient` (como `CatalogoApi`).
2. Un `XxxController` en `controller/` con `@Controller`.
3. Templates en `templates/<modulo>/` (por ejemplo `templates/pedidos/carrito.html`) que reutilicen el layout:
   ```html
   <head th:replace="~{fragments/layout :: head('Carrito')}"></head>
   <nav th:replace="~{fragments/layout :: navbar}"></nav>
   ...
   <footer th:replace="~{fragments/layout :: footer}"></footer>
   ```

## Reparto de pantallas (semana 6)

| Integrante | Pantallas | Estado |
|---|---|---|
| 1 | Layout, menú según rol, manejo de sesión (guardar el JWT en `HttpSession` y mandarlo como `Authorization` en cada llamada) | 🟡 layout listo; sesión pendiente |
| 2 | `/login`, `/registro`, `/perfil`, `/admin/usuarios` | ⬜ |
| 3 | `/` y `/catalogo` con filtros, `/catalogo/{id}`, `/admin/productos` | 🟡 inicio listo |
| 4 | `/carrito`, `/checkout`, `/mis-pedidos`, `/admin/ventas` | ⬜ |
| 5 | `/admin/stock`, `/comprobantes`, `/comprobantes/{id}` (+ descarga PDF) | ⬜ |

## Dependencias con otros servicios

Cada pantalla depende de los endpoints del servicio que muestra: la grilla del catálogo necesita `GET /productos` con filtros (integrante 3), el login necesita C3 (integrante 2), "mis comprobantes" necesita `GET /comprobantes/venta/{idVenta}` (integrante 5), etc. El mapa completo, con semanas, está en [`ARRANQUE_PROYECTO.md`](../ARRANQUE_PROYECTO.md#quién-le-debe-a-quién). Las pantallas son de la semana 6, cuando esos endpoints ya deberían existir.

## Pendientes del integrante 1

- [ ] Interceptor del `RestClient` que agregue `Authorization: Bearer <token>` desde la sesión.
- [ ] Menú que cambie según el rol (ADMIN o CLIENTE) y botón "Salir".
- [ ] Página de error amigable para 401 (redirigir a `/login`) y 5xx.

## Cómo correrlo

```bash
docker compose up -d                       # todo el sistema
# o en local, con el gateway ya arriba:
./mvnw -pl web-client spring-boot:run      # http://localhost:8090
```
