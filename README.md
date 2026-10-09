# IntiGuard v2

E-commerce de equipos de seguridad electrónica (cámaras, alarmas, sensores de humo, cerraduras digitales y control de acceso), migrado de un monolito ASP.NET MVC a **microservicios con Spring Boot**.

Proyecto del curso **Desarrollo de Servicios Web II · Cibertec**. El plan completo (decisiones, cronograma y despliegue) está en [`docs/Plan de ejecución — IntiGuard v2.md`](docs/Plan%20de%20ejecución%20—%20IntiGuard%20v2.md).

## Arquitectura

```
                 ┌──────────────┐
  Navegador ───▶ │  web-client  │ :8090  Thymeleaf
                 └──────┬───────┘
  Postman ──────────────┤
                        ▼
                 ┌──────────────┐        ┌──────────────┐
                 │  ms-gateway  │ :8080 ◀─▶│ ms-discovery │ :8761  Eureka
                 └──────┬───────┘        └──────────────┘
         ┌──────────┬───┴───────┬──────────────┬─────────────────┐   (todos se registran en Eureka)
         ▼          ▼           ▼              ▼                 ▼
     ms-auth   ms-catalogo  ms-inventario   ms-pedidos ──Feign──▶ catálogo, inventario, comprobantes
      :8081       :8082        :8083          :8084          ms-comprobantes :8085
         │          │            │              │                 │
     db_auth   db_catalogo  db_inventario   db_pedidos     db_comprobantes      ← MySQL 8, un schema por servicio
```

- **Una sola URL pública**: el gateway. Nadie llama a un servicio por IP: se buscan por nombre en Eureka.
- **Database per Service**: ningún servicio entra al schema de otro; si necesita un dato, lo pide por HTTP.
- **Capas en cada servicio**: `controller → service → repository → entity`, con `dto` y `mapper` entre capas y `exception` con un formato único de error.
- **Checkout** (ms-pedidos): catálogo → reservar stock → venta → comprobante → confirmar stock. Si el comprobante falla o tarda más de 2 s, se libera el stock (saga por compensación + circuit breaker).

| Módulo | Puerto | Schema | Responsable | README |
|---|---|---|---|---|
| ms-discovery | 8761 | — | Integrante 1 · Mathias | [ver](ms-discovery/README.md) |
| ms-gateway | 8080 | — | Integrante 1 · Mathias | [ver](ms-gateway/README.md) |
| ms-auth | 8081 | db_auth | Integrante 2 | [ver](ms-auth/README.md) |
| common-security (librería) | — | — | Integrante 2 | [ver](common-security/README.md) |
| ms-catalogo | 8082 | db_catalogo | Integrante 3 | [ver](ms-catalogo/README.md) |
| ms-inventario | 8083 | db_inventario | Integrante 5 | [ver](ms-inventario/README.md) |
| ms-pedidos | 8084 | db_pedidos | Integrante 4 | [ver](ms-pedidos/README.md) |
| ms-comprobantes | 8085 | db_comprobantes | Integrante 5 | [ver](ms-comprobantes/README.md) |
| web-client | 8090 | — | Integrante 1 (layout) + todos | [ver](web-client/README.md) |

## Stack

Java 21 · Spring Boot 3.5.16 · Spring Cloud 2025.0.3 (Gateway, Eureka, OpenFeign, Resilience4j) · Spring Data JPA · Spring Security + JWT (jjwt 0.12) · Lombok · MySQL 8.4 · JUnit 5 + Mockito + H2 · Thymeleaf + Bootstrap 5 · Docker Compose · Maven Wrapper 3.9.16

## Requisitos para desarrollar

| Herramienta | Versión | Verificar | Instalar (Ubuntu/Mint) |
|---|---|---|---|
| JDK | **21** | `javac -version` | `sudo apt install openjdk-21-jdk` |
| Docker + Compose | 24+ / v2+ | `docker compose version` | [docs.docker.com/engine/install](https://docs.docker.com/engine/install/) y `sudo usermod -aG docker $USER` |
| Git | cualquiera | `git --version` | `sudo apt install git` |
| Maven | **no hace falta** | — | Se usa `./mvnw` (wrapper incluido) |
| Postman | cualquiera | — | postman.com/downloads |

> Si tienes otro JDK por defecto (por ejemplo 25), exporta antes `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.
> RAM: el sistema completo usa ~3–4 GB (cada JVM limitada a 256 MB).
> El MySQL de Docker se publica en el puerto **3307** del host para no chocar con un MySQL local en 3306.

## Levantar todo

```bash
git clone git@github.com:jmalayo-dev/IntiWard.git && cd IntiWard

# 1. Compilar y correr las pruebas de los 9 módulos
./mvnw verify

# 2. Levantar MySQL + 8 servicios (la primera vez tarda ~3 min en construir las imágenes)
docker compose up -d --build
docker compose ps                      # todos deben quedar "healthy"

# 3. Verificar
open http://localhost:8761             # panel de Eureka
curl localhost:8080/productos          # catálogo a través del gateway
open http://localhost:8090             # cliente web
```

> Justo después de levantar, el gateway puede responder **503** unos 30 s, hasta que recibe la lista de Eureka.

**Trabajar en un solo servicio** (lo normal día a día): levanta las dependencias en Docker y corre tu servicio desde el IDE o con Maven. Por defecto apunta a `localhost:3307` y a Eureka en `localhost:8761`.

```bash
docker compose up -d mysql ms-discovery ms-gateway
docker compose stop ms-catalogo                  # si estaba corriendo en Docker
./mvnw -pl ms-catalogo spring-boot:run
```

**Reiniciar la base** (después de cambiar algo en `infra/mysql/*.sql`): `docker compose down -v && docker compose up -d`

**Credenciales de desarrollo**: usuario `admin@intiguard.pe` / `Admin12345`. MySQL root: `root_local`. Cada servicio tiene su usuario: `usr_<servicio>` / `usr_<servicio>_local`.

## Estructura del repositorio

```
├─ pom.xml                 POM padre (versiones en un solo lugar)          ← integrante 1
├─ docker-compose.yml      MySQL + 8 servicios con healthchecks y orden    ← integrante 1
├─ .github/                CI (matriz de 8 módulos), CODEOWNERS, plantilla de PR
├─ infra/mysql/            00-schemas-usuarios.sql + un .sql por servicio (tablas y semillas)
├─ common-security/        Librería JWT que importan los 5 servicios de negocio
├─ ms-*/                   Un servicio por carpeta, cada uno con su README, Dockerfile y tests
├─ web-client/             Thymeleaf
├─ postman/                Colección smoke + environments local y cloud
└─ docs/                   Plan de ejecución
```

## Cómo trabajamos

**Ramas**

| Rama | Regla |
|---|---|
| `main` | Lo entregado y desplegado. Solo entra por PR desde `develop`, con CI verde y una aprobación |
| `develop` | Integración. Solo entra por PR desde una rama de trabajo |
| `feature/<servicio>/<tarea>` | Trabajo diario. Ej.: `feature/catalogo/crud-productos` |
| `fix/<servicio>/<tarea>` | Correcciones |

**Commits**: Conventional Commits con el servicio como ámbito.
```
feat(catalogo): endpoint de búsqueda por categoría
fix(pedidos): liberar stock cuando falla la emisión del comprobante
test(auth): pruebas de la capa de datos del repositorio de usuarios
```

**Reglas**
1. Cada integrante trabaja **solo en su carpeta** y en su `infra/mysql/0X-<servicio>.sql`.
2. Los archivos compartidos (`pom.xml` raíz, `docker-compose.yml`, `.github/`, rutas del gateway) los edita **solo el integrante 1**. Si necesitas algo, pídelo por el grupo.
3. Un PR necesita la aprobación de otro integrante.
4. **Cero credenciales en el código**: todo entra por variable de entorno (`DB_PASSWORD`, `JWT_SECRET`…). Los valores por defecto de `application.yml` son solo para desarrollo local.
5. Si cambias un endpoint o DTO que otro servicio consume, avisa al dueño **antes** de mergear.

## Estado actual (bases · semana 1)

✅ Verificado el 2026-10-09: `./mvnw verify` en verde (9 módulos y sus tests), `docker compose up` con los 9 contenedores *healthy*, los 6 clientes registrados en Eureka, el gateway enrutando a los 5 servicios con datos reales de MySQL, 404 con formato `ApiError`, y el web-client mostrando el catálogo a través del gateway.

| Ya funciona | Detalle |
|---|---|
| Infraestructura | Eureka, gateway con rutas a los 5 servicios, Docker Compose con orden de arranque y healthchecks |
| Base de datos | 5 schemas, 5 usuarios con permisos solo sobre su schema, todas las tablas del modelo y semillas (admin, 5 marcas, 5 categorías, 5 productos con stock, series B001/F001) |
| Cada servicio de negocio | Entidades completas del modelo, repositorios, DTO, mapper, service, `GET` de lectura, manejo global de errores, perfiles default/cloud, Dockerfile, test `@DataJpaTest` |
| Seguridad | `JwtService` (generar/validar, con test), BCrypt(10) en ms-auth. El filtro aún deja pasar todo (`permitAll`) |
| ms-pedidos | Feign y Resilience4j configurados, `CatalogoClient` de ejemplo |
| web-client | Layout base + inicio con el catálogo real |
| GitHub | CI con matriz de 8 módulos, CODEOWNERS, plantilla de PR |

## Pendientes por integrante

El detalle (archivos, reglas y checklist por semana) está en el README de cada servicio.

### Integrante 1 · Mathias (ms-gateway, ms-discovery, integración y despliegue)
- [ ] Proteger `main` y `develop` en GitHub (Settings → Branches: exigir PR, CI verde y una aprobación).
- [ ] Reemplazar `@integranteN` en `.github/CODEOWNERS` cuando se conozcan los usuarios de GitHub.
- [ ] Crear el tablero de GitHub Projects (Backlog, En curso, En revisión, Hecho) y los milestones por semana.
- [ ] **S3**: `JwtGatewayFilter` (validar el token, rutas públicas y cabeceras `X-User`/`X-Rol`).
- [ ] **S6**: web-client: sesión con JWT, interceptor `Authorization`, menú por rol.
- [ ] **S8**: EC2 + RDS + S3, Nginx con HTTPS, secretos en Parameter Store, `docker-compose` de nube, publicar imágenes en Docker Hub desde el CI.
- [ ] Integración semanal: `develop` → `main` con todo en verde.

### Integrante 2 (ms-auth + common-security)
- [ ] **S2**: `POST /auth/registro` y el CRUD de `/usuarios`.
- [ ] **S3**: `POST /auth/login` con las 6 validaciones (genérico 401, bloqueo 423 tras 5 intentos, cuenta inactiva, JWT de 1 h).
- [ ] **S3**: `JwtAuthFilter` en `common-security` y pasar de `permitAll` a `authenticated`.
- [ ] **S6**: pantallas de login, registro, perfil y gestión de usuarios.
- [ ] **S7**: tests de `AuthService` y colección de Postman que guarde `{{token}}`.

### Integrante 3 (ms-catalogo)
- [ ] **S2–3**: CRUD de productos con validaciones, `GET /marcas`, `GET /categorias`, paginación y filtros.
- [ ] **S5**: semilla de 200 productos (coordinar el stock con el integrante 5).
- [ ] **S6**: pantallas de catálogo, detalle y CRUD de productos.
- [ ] **S7–8**: tests, colección de Postman e imágenes en S3.

### Integrante 4 (ms-pedidos)
- [ ] **S2–3**: carrito (`/carrito/items`).
- [ ] **S4**: checkout de punta a punta con Feign (**entrega bisagra**).
- [ ] **S5**: circuit breaker (2 s) y compensación (liberar stock, venta ANULADA).
- [ ] **S6**: pantallas de carrito, checkout, mis pedidos y ventas.
- [ ] **S7**: tests del checkout con Mockito y colección "flujo completo".

### Integrante 5 (ms-inventario + ms-comprobantes)
- [ ] **S2–3**: `reservar` / `confirmar` / `liberar` con movimientos (409 si no alcanza).
- [ ] **S2–3**: `POST /comprobantes` con serie y correlativo (`@Version`, idempotente por venta).
- [ ] **S4**: acordar los contratos con el integrante 4.
- [ ] **S5**: PDF del comprobante (OpenPDF), reporte de ventas y semilla de stock para 200 productos.
- [ ] **S6–7**: pantallas de stock y comprobantes, tests y colecciones de Postman.

## Pendientes generales (equipo)

- [ ] **Nombres y usuarios de GitHub de los integrantes 2 a 5**: pendiente a elección. Hacen falta para CODEOWNERS, asignar issues y la tabla de este README.
- [ ] **Fecha real de entrega**: el plan asume 8 semanas. Si son 6, se recorta el web-client; si son menos, ms-inventario se fusiona con ms-catalogo.
- [ ] **Preguntar al docente** si Postman cuenta como "cliente que consume la API" (si sí, se ahorra una semana).
- [ ] **Cuenta de AWS con tarjeta** (resolverlo en la semana 1, no en la 8). Alternativa: Oracle Cloud Always Free (probarla temprano).
- [ ] **Decidir H2 o Testcontainers** para las pruebas de datos (hoy: H2, el CI es más rápido).
- [ ] Contratos entre servicios: reservar/confirmar/liberar (integrantes 4 y 5) y emitir comprobante (integrantes 4 y 5). Escribirlos en los README antes de la semana 4.
- [ ] Colección de Postman por servicio + "flujo completo" + environment de nube.
- [ ] Ensayo de la sustentación: los cinco deben poder explicar el sistema completo (los README de cada servicio tienen una sección "Qué decir en la sustentación").

## Patrones de diseño (para la sustentación)

| Patrón | Dónde se ve |
|---|---|
| API Gateway | `ms-gateway/src/main/resources/application.yml` |
| Service Registry y Discovery | `ms-discovery` (`@EnableEurekaServer`) + `lb://` en el gateway |
| Database per Service | `infra/mysql/00-schemas-usuarios.sql`: un schema y un usuario por servicio |
| DTO y Mapper | `*/dto/*Response.java` + `*/mapper/*Mapper.java`: la entidad nunca sale por HTTP |
| Repository | `*/repository/*Repository.java` (Spring Data JPA) |
| Builder | `@Builder` de Lombok en todas las entidades |
| Circuit Breaker | ms-pedidos, Resilience4j en los clientes Feign (semana 5) |
| Saga por compensación | `ms-pedidos/.../service/PedidoService.java` (checkout, semana 4–5) |
| Externalized Configuration | `application.yml` con `${VAR:default}` + `application-cloud.yml` |
| Manejo global de errores | `*/exception/GlobalExceptionHandler.java` con `ApiError` |
| Auto-Configuration | `common-security`: la seguridad se activa con solo importar la dependencia |
