# IntiGuard v2

E-commerce de equipos de seguridad electrónica (cámaras, alarmas, sensores de movimiento, detectores de humo, cerraduras digitales y control de acceso), migrado de un monolito ASP.NET MVC a **microservicios con Spring Boot** y un **cliente web en React**.

Proyecto del curso **Desarrollo de Servicios Web II · Cibertec**. El plan completo (decisiones, cronograma y despliegue) está en [`docs/Plan de ejecución — IntiGuard v2.md`](docs/Plan%20de%20ejecución%20—%20IntiGuard%20v2.md).

> **¿Recién llegas al equipo?** Empieza por [`ARRANQUE_PROYECTO.md`](ARRANQUE_PROYECTO.md): cómo dejar tu microservicio funcionando el primer día, paso a paso, y qué le debe cada integrante a quién.

---

## Qué hace el sistema

Una tienda en línea con tres tipos de usuario:

| Usuario | Qué puede hacer |
|---|---|
| **Visitante** (sin cuenta) | Ver el catálogo con filtros, el detalle de cada producto, su precio y si hay stock. Registrarse e iniciar sesión |
| **Cliente** (rol `CLIENTE`) | Todo lo anterior, más armar su carrito, comprar con **boleta o factura**, ver sus pedidos y sus comprobantes (y descargar el PDF), y editar su perfil |
| **Administrador** (rol `ADMIN`) | Gestionar productos, marcas y categorías; registrar entradas y ajustes de stock y ver los movimientos; ver todas las ventas y comprobantes; ver el reporte de ventas; gestionar usuarios |

**La compra (checkout), de punta a punta:** el cliente confirma su carrito → se toma el precio vigente del catálogo → se registra la venta como **PENDIENTE** → se **apartan** las unidades en inventario (si falta stock de algo, no se aparta nada) → se emite el **comprobante** con número único (B001-00000001) → se **confirma** el stock y la venta queda **CONFIRMADA**. Si el comprobante falla o tarda más de 2 s, se **liberan** las unidades y la venta queda **ANULADA**: nunca queda una venta a medias.

**Reglas de negocio clave:** precios con IGV (18 %) incluido y desglosado en el comprobante · nunca se vende más de lo que hay · los productos no se borran, se desactivan · 5 intentos de login fallidos bloquean la cuenta 15 min · cada cliente ve solo lo suyo. Los casos de uso completos están en [Guía de negocio y casos de uso](#guía-de-negocio-y-casos-de-uso), al final de este documento. El detalle técnico de cada regla está en la sección "Guía de negocio" del README de cada microservicio.

---

## Arquitectura

```
                 ┌──────────────────────────┐
  Navegador ───▶ │  web (React + Vite)      │ :5173 en desarrollo
                 └────────────┬─────────────┘
  Postman ────────────────────┤
                              ▼
                 ┌──────────────┐          ┌──────────────┐
                 │  ms-gateway  │ :8080 ◀─▶│ ms-discovery │ :8761  Eureka
                 └──────┬───────┘          └──────────────┘
         ┌──────────┬───┴───────┬──────────────┬─────────────────┐   (todos se registran en Eureka)
         ▼          ▼           ▼              ▼                 ▼
     ms-auth   ms-catalogo  ms-inventario   ms-pedidos ──Feign──▶ catálogo, inventario, comprobantes
      :8081       :8082        :8083          :8084          ms-comprobantes :8085
         │          │            │              │                 │
     db_auth   db_catalogo  db_inventario   db_pedidos     db_comprobantes      ← MySQL 8.4 en Aiven, un schema por servicio
```

- **Una sola puerta de entrada:** el gateway (8080). El cliente web y Postman solo hablan con él; nadie llama a un microservicio por IP, se buscan por nombre en Eureka.
- **Database per Service:** cada servicio tiene su schema y su usuario MySQL con permisos solo sobre ese schema. Si necesita un dato de otro, lo pide por HTTP.
- **Seguridad con JWT:** el login (ms-auth) emite un token con `idUsuario` y rol. El gateway lo valida y bloquea las rutas internas; cada servicio lo vuelve a validar con `common-security`.
- **Capas en cada servicio:** `controller → service → repository → entity`, con `dto` y `mapper` entre capas y `exception` con un formato único de error (`ApiError`).
- **Cliente web:** se está rehaciendo en **React + Vite** (diseño en [`docs/PROTOTIPO_WEB.md`](docs/PROTOTIPO_WEB.md), arquitectura FSD, endpoints y mocks en [`docs/ARQUITECTURA_WEB.md`](docs/ARQUITECTURA_WEB.md)). El `web-client` actual en Thymeleaf (puerto 8090) queda como referencia hasta que la app React lo reemplace.

---

## Equipo y responsables

| Integrante | Rama | Módulos | Puerto | Schema | README |
|---|---|---|---|---|---|
| 1 · Galarza | `b-galarza` | ms-auth + `common-security` | 8081 | `db_auth` | [ms-auth](ms-auth/README.md) · [common-security](common-security/README.md) |
| 1 · Galarza | `b-galarza` | ms-comprobantes | 8085 | `db_comprobantes` | [ms-comprobantes](ms-comprobantes/README.md) |
| 2 · César | `b-cesar` | ms-catalogo | 8082 | `db_catalogo` | [ms-catalogo](ms-catalogo/README.md) |
| 3 · Arévalo | `b-arevalo` | ms-inventario | 8083 | `db_inventario` | [ms-inventario](ms-inventario/README.md) |
| 4 · Evelyn | `b-guerrero` | ms-pedidos | 8084 | `db_pedidos` | [ms-pedidos](ms-pedidos/README.md) |
| 5 · Mathias | `b-alayo` | Cliente web (React), ms-gateway, ms-discovery, integración, base de datos y despliegue | 5173 · 8080 · 8761 | — | [ms-gateway](ms-gateway/README.md) · [ms-discovery](ms-discovery/README.md) · [prototipo web](docs/PROTOTIPO_WEB.md) |

---

## Stack

**Backend:** Java 21 · Spring Boot 3.5.16 · Spring Cloud 2025.0.3 (Gateway, Eureka, OpenFeign, Resilience4j) · Spring Data JPA · Spring Security + JWT (jjwt 0.12) · Lombok · Maven Wrapper 3.9.16
**Base de datos:** MySQL 8.4 administrado en **Aiven** (un schema por servicio, SSL obligatorio)
**Pruebas:** JUnit 5 + Mockito + H2 (en memoria) · Postman
**Cliente web:** React + Vite (Node.js 22 LTS)
**Infraestructura:** Docker Compose · GitHub Actions (CI)

---

## Requisitos para desarrollar

| Herramienta | Versión | Para qué | Verificar |
|---|---|---|---|
| JDK | **21** | Compilar y correr los microservicios | `java -version` |
| Docker Desktop (o Docker + Compose) | Compose v2+ | Levantar los microservicios | `docker compose version` |
| Git | cualquiera | Código | `git --version` |
| Maven | **no hace falta** | — | Se usa el wrapper: `.\mvnw.cmd` (Windows) o `./mvnw` (Linux/Mac) |
| Node.js | **22 LTS** (mínimo 20.19) | Solo para el cliente web (Vite no funciona con Node 18) | `node -v` |
| DBeaver Community | cualquiera | Ver las tablas de tu schema | — |
| Postman | cualquiera | Probar endpoints y conseguir tokens | — |

- **No hay MySQL local.** La base es un MySQL 8.4 en Aiven, compartido por todo el equipo. Hace falta internet y un archivo **`.env`** en la raíz del proyecto.
- **El `.env`, el certificado `ca.pem` y las contraseñas se reparten por WhatsApp**, nunca por Git. El paso a paso está en [`ARRANQUE_PROYECTO.md`](ARRANQUE_PROYECTO.md).
- RAM: el sistema completo usa ~3–4 GB (cada JVM limitada a 256 MB). Un microservicio solo, mucho menos.
- En Linux, si tu JDK por defecto no es el 21: `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`.

---

## Cómo levantarlo

### Cada integrante: solo su microservicio (lo normal día a día)

La base acepta **46 conexiones en total para todo el equipo**: cada uno levanta **solo lo suyo** (Docker arranca además Eureka).

| Integrante | Comando |
|---|---|
| Galarza | `docker compose up -d --build ms-auth ms-comprobantes` |
| César | `docker compose up -d --build ms-catalogo` |
| Arévalo | `docker compose up -d --build ms-inventario` |
| Evelyn | `docker compose up -d --build ms-pedidos` (carrito)<br>`docker compose up -d --build ms-pedidos ms-catalogo ms-inventario ms-comprobantes` (probar el checkout) |

Verificar con `docker compose ps` (todo en `healthy`) y en el navegador: el puerto del servicio (por ejemplo `http://localhost:8083/stock/1`) y Eureka en `http://localhost:8761`. Al terminar: `docker compose down`.

Para correr un microservicio con Maven o depurarlo desde IntelliJ, el seguimiento de estados, el `--build` y los problemas típicos: ver [`ARRANQUE_PROYECTO.md`](ARRANQUE_PROYECTO.md).

### Integración y demo (Mathias)

```bash
./mvnw verify                          # compila y corre todas las pruebas (H2: no necesitan Aiven ni .env)
docker compose up -d --build           # los 8 contenedores contra Aiven
docker compose ps                      # todos "healthy"
```

- Eureka: `http://localhost:8761` · catálogo por el gateway: `http://localhost:8080/productos`.
- Justo después de levantar, el gateway puede responder **503** unos 30 s, hasta que recibe la lista de Eureka.

### Base de datos en Aiven (solo Mathias)

- **Crear la base desde cero** (una vez): `./infra/scripts/init-aiven.sh`. Ejecuta `infra/mysql/*.sql` y `infra/mysql/inserts-test/*.sql`, y fija las contraseñas reales de los usuarios `ms_*`. Si la base ya existe, no toca nada.
- **Cambiar una tabla o agregar datos de prueba:** el integrante lo pide explicando qué necesita y para qué; Mathias actualiza `infra/` y aplica el cambio en Aiven. La base es compartida: nunca se borra ni se recrea.
- **Usuarios MySQL:** cada servicio entra con `ms_<servicio>` y su `DB_PASS_*`, y solo puede leer y modificar datos de su schema (sin crear ni alterar tablas). `avnadmin` (`AIVEN_ADMIN_*`) es solo para administrar.
- **Administrador de la tienda:** no está en el SQL. ms-auth lo crea al arrancar si `ADMIN_CORREO` y `ADMIN_CLAVE` están en el `.env`.

---

## Estructura del repositorio

```
├─ ARRANQUE_PROYECTO.md    Guía del primer día para cada integrante
├─ pom.xml                 POM padre (versiones en un solo lugar)
├─ docker-compose.yml      Los 8 contenedores contra MySQL de Aiven
├─ .github/                CI (matriz de 8 módulos), CODEOWNERS, plantilla de PR
├─ infra/mysql/            Un .sql por servicio (estructura) + inserts-test/ (datos iniciales)   ← solo Mathias
├─ infra/scripts/          init-aiven.sh (crea la base) y token-dev.sh                          ← solo Mathias
├─ common-security/        Librería JWT que importan los 5 servicios de negocio
├─ ms-*/                   Un microservicio por carpeta, cada uno con su README, Dockerfile y tests
├─ web-client/             Cliente Thymeleaf actual (referencia hasta que lo reemplace la app React)
├─ postman/                Colecciones (smoke y seguridad) + environments
└─ docs/                   Plan de ejecución, prototipo web y arquitectura del cliente web
```

---

## Cómo trabajamos

**Ramas**

| Rama | Regla |
|---|---|
| `main` | Lo entregado y desplegado. Solo entra desde `develop` (integración semanal), con CI verde |
| `develop` | Integración. Solo entra por **Pull Request** desde una rama de integrante, con la aprobación de otro integrante |
| `b-<apellido>` | La rama de trabajo de cada integrante (`b-galarza`, `b-cesar`, `b-arevalo`, `b-guerrero`, `b-alayo`) |

**Flujo de cada día:** `git pull origin develop` al empezar → trabajar y commitear en tu rama → `git push` → Pull Request de tu rama hacia `develop`.

**Commits:** Conventional Commits en español, con el servicio como ámbito.
```
feat(catalogo): endpoint de búsqueda por categoría
fix(pedidos): liberar stock cuando falla la emisión del comprobante
test(auth): pruebas del bloqueo por intentos fallidos
```

**Reglas**
1. Cada integrante trabaja **solo en la carpeta de su microservicio**.
2. **No se tocan** los archivos de Docker (`docker-compose.yml`, los `Dockerfile`), la configuración global (`pom.xml` raíz, `.github/`, `.gitignore`), la carpeta `infra/` ni las carpetas de otros. Si hace falta un cambio, se pide por el grupo.
3. **Si tu trabajo depende de otro microservicio, se lo pides a su dueño**: él te entrega el endpoint y te explica qué datos enviar, qué datos devuelve y qué errores puede dar. Los acuerdos ya escritos están en [Quién le debe a quién](ARRANQUE_PROYECTO.md#10-quién-le-debe-a-quién).
4. Un Pull Request necesita la aprobación de otro integrante.
5. **Cero credenciales en el código:** todo sale del `.env` (que Git ignora). Nunca pegues una contraseña en un archivo del proyecto, en un issue, en el grupo o en una captura.
6. Si cambias un endpoint o un DTO que otro servicio usa, avisa a su dueño **antes** de mergear.

---

## Contratos entre microservicios

| # | Qué se entrega | Lo entrega | Lo necesita | Semana | Estado |
|---|---|---|---|---|---|
| C1 | El JWT trae correo, rol e `idUsuario` | Autenticación | Todos | 2 | ✅ |
| C2 | `JwtAuthFilter` y `UsuarioActual` en `common-security` | Autenticación | Todos | 3 | ✅ |
| C3 | `POST /auth/login` | Autenticación | Gateway, cliente web | 3 | ✅ |
| C4 | Gateway: rutas públicas, 401 sin token, rutas internas bloqueadas | Gateway | Todos | 3 | ✅ |
| C5 | `GET /productos/{id}` con el campo `activo` | Catálogo | Pedidos, inventario | 2 | ✅ |
| C6 | Reservar, confirmar y liberar stock | Inventario | Pedidos | fin de 3 | ✅ |
| C7 | Emitir comprobante y buscarlo por venta | Comprobantes | Pedidos, cliente web | fin de 3 / 4 | ✅ |
| C8 | Venta PENDIENTE antes de reservar, token reenviado, liberar ante fallo | Pedidos | Inventario, comprobantes | 4 | ✅ |
| C9 | Lista de 200 productos para cargar en la base | Catálogo | Mathias | 5 | ✅ |

El detalle de cada contrato (cuerpos, respuestas y códigos HTTP) está en la sección "Dependencias con otros servicios" del README del servicio que lo entrega.

Lo que necesita el cliente web de cada servicio (endpoints, tipos de datos, para qué sirve cada campo y qué vistas lo usan) está en la sección **"Contrato con el cliente web"** de cada README. El avance de cada endpoint se registra en [`docs/SEGUIMIENTO_ENDPOINTS.csv`](docs/SEGUIMIENTO_ENDPOINTS.csv): cada integrante actualiza `cumplido` y `fecha_cumplimiento` de sus filas.

---

## Estado actual · semana 2

✅ Verificado el 2026-10-15: `./mvnw verify` con **38 pruebas en verde**; los microservicios levantan contra Aiven con SSL; Eureka registra a todos; el gateway enruta a los 5 servicios; el login funciona de punta a punta (token, 401, 423 al quinto intento); la colección de Postman de seguridad pasa completa.

| Ya funciona | Detalle |
|---|---|
| Infraestructura | Eureka, gateway con rutas a los 5 servicios, Docker Compose (cada uno levanta solo su servicio), healthchecks |
| Base de datos | MySQL 8.4 en Aiven: 5 schemas, 5 usuarios con permisos solo sobre su schema, modelo completo con reglas en la base (CHECK, claves únicas), vista de conciliación de stock y datos iniciales (roles, 6 marcas, 6 categorías, 8 productos con 10 unidades, series B001/F001) |
| Cada microservicio | Entidades del modelo definitivo, repositorios, DTO, mapper, `GET` de lectura, manejo global de errores (400, 401, 403, 404, 500), perfiles default y cloud, Dockerfile y pruebas de datos |
| Seguridad | JWT con `idUsuario` y rol (C1); `JwtAuthFilter` y `UsuarioActual` en los 5 servicios (C2); **login** con las 6 reglas del plan (C3); gateway con rutas públicas, rutas internas bloqueadas y validación del token (C4); `/usuarios` solo para ADMIN; BCrypt(10). `EXIGIR_TOKEN=false` en desarrollo |
| ms-pedidos | Feign y Resilience4j configurados, `CatalogoClient` de ejemplo, cálculo de IGV |
| Documentación | `ARRANQUE_PROYECTO.md`, "Guía de negocio" y contratos en el README de cada servicio |
| GitHub | CI con matriz de 8 módulos, plantilla de PR, ramas por integrante |

---

## Pendientes por integrante

El detalle (archivos, reglas y checklist por semana) está en el README de cada microservicio.

### Galarza · Autenticación y Comprobantes
- [ ] **S2**: `POST /auth/registro` (rol CLIENTE, 409 si el correo existe) y el CRUD de `/usuarios` (ADMIN).
- [ ] **S3**: `POST /comprobantes` con serie y correlativo (`SELECT … FOR UPDATE`), validación de DNI y RUC e idempotencia por venta (**C7**).
- [ ] **S4**: `GET /comprobantes/venta/{idVenta}`; probar la emisión junto con Pedidos.
- [ ] **S5**: PDF del comprobante y reporte de ventas.
- [ ] **S7**: pruebas del registro y de la emisión; colecciones de Postman.

### César · Catálogo
- [ ] **S2**: agregar `activo` a `GET /productos/{id}` (**C5**).
- [ ] **S2–3**: CRUD de productos (ADMIN) con validaciones, `GET /marcas`, `GET /categorias`, paginación y filtros.
- [ ] **S5**: lista de 200 productos para cargar en la base (**C9**).
- [ ] **S7–8**: pruebas, colección de Postman e imágenes.

### Arévalo · Inventario
- [ ] **S3**: `reservar` / `confirmar` / `liberar` con sus movimientos, todo o nada (409) e idempotentes (**C6**).
- [ ] **S3**: liberación automática de reservas vencidas (15 min); entrada y ajuste de stock (ADMIN); `GET /movimientos`.
- [ ] **S4**: probar las reservas junto con Pedidos.
- [ ] **S7**: pruebas de `StockService` y colección de Postman.

### Evelyn · Pedidos
- [ ] **S2–3**: carrito (`/carrito/items`), con el dueño tomado del token (`UsuarioActual`).
- [ ] **S4**: checkout de punta a punta con Feign (**entrega bisagra**, **C8**).
- [ ] **S5**: circuit breaker (2 s) y compensación (liberar stock, venta ANULADA).
- [ ] **S7**: pruebas del checkout con Mockito y colección "flujo completo".

### Mathias · Cliente web, gateway e integración
- [ ] **S2**: prototipo del cliente web ([`docs/PROTOTIPO_WEB.md`](docs/PROTOTIPO_WEB.md)) y proyecto React + Vite.
- [ ] **S3**: CORS en el gateway para la app web; proxy de Vite en desarrollo.
- [ ] **S6**: pantallas del cliente web conectadas a la API (sesión con JWT, menú por rol).
- [ ] Integración semanal `develop` → `main`; aplicar en Aiven los cambios de tablas que pida el equipo.
- [ ] Proteger `main` y `develop` en GitHub; completar CODEOWNERS con los usuarios de GitHub.
- [ ] **S8**: despliegue (servidor con HTTPS, secretos fuera del código).

---

## Pendientes generales (equipo)

- [ ] **Usuarios de GitHub de los cinco**, para CODEOWNERS y para asignar issues.
- [ ] **Actualizar los README de cada microservicio** al reparto nuevo (nombres en vez de números) y a las reglas de `infra/`.
- [ ] **Registro de clientes listo → pasar a `EXIGIR_TOKEN=true`** para todos.
- [ ] **Fecha real de entrega:** el plan asume 8 semanas. Si son menos, se recorta primero el cliente web.
- [ ] **Preguntar al docente** si Postman cuenta como "cliente que consume la API".
- [ ] **Dónde se despliegan los microservicios** (la base ya está en Aiven).
- [ ] Colección de Postman por servicio + "flujo completo".
- [ ] Ensayo de la sustentación: los cinco deben poder explicar el sistema completo.

---

## Patrones de diseño (para la sustentación)

| Patrón | Dónde se ve |
|---|---|
| API Gateway | `ms-gateway`: rutas en `application.yml` y seguridad en `JwtGatewayFilter` |
| Service Registry y Discovery | `ms-discovery` (`@EnableEurekaServer`) + `lb://` en el gateway |
| Database per Service | Un schema y un usuario MySQL por servicio, con permisos solo sobre su schema |
| DTO y Mapper | `*/dto/*Response.java` + `*/mapper/*Mapper.java`: la entidad nunca sale por HTTP |
| Repository | `*/repository/*Repository.java` (Spring Data JPA) |
| Builder | `@Builder` de Lombok en todas las entidades |
| Circuit Breaker | ms-pedidos, Resilience4j en los clientes Feign (semana 5) |
| Saga por compensación | ms-pedidos, checkout (semanas 4–5) |
| Externalized Configuration | `application.yml` con variables del `.env` + `application-cloud.yml` |
| Manejo global de errores | `*/exception/GlobalExceptionHandler.java` con `ApiError` |
| Auto-Configuration | `common-security`: la seguridad se activa con solo importar la dependencia |

---

## Guía de negocio y casos de uso

Esta sección explica **cómo funciona la tienda según el backend**: quién usa el sistema, qué puede hacer cada uno, qué datos se piden, qué reglas se cumplen y qué respuestas puede recibir el usuario. Sirve para armar cualquier prototipo **sin leer código**. No dice nada del aspecto visual (eso está en [`docs/PROTOTIPO_WEB.md`](docs/PROTOTIPO_WEB.md)).

Estado de cada caso de uso: ✅ ya funciona · 🟡 funciona en parte · ⬜ pendiente.

### Actores

| Actor | Quién es | Cómo se identifica |
|---|---|---|
| **Visitante** | Cualquier persona que entra a la tienda sin iniciar sesión | No se identifica |
| **Cliente** | Persona que se registró para comprar | Correo y contraseña. Todo el que se registra es Cliente |
| **Administrador** | Personal de IntiGuard | Correo y contraseña. Solo otro Administrador puede dar este rol. El primero se crea al instalar el sistema |
| **Sistema** | Tareas automáticas que nadie dispara a mano | No aplica |

Al iniciar sesión el usuario recibe una **sesión de 1 hora** que lleva su identidad y su rol. Mientras dure, el sistema sabe quién es sin volver a pedir la contraseña. Al vencer, debe iniciar sesión otra vez. **Cerrar sesión** es dejar de usar esa sesión en el navegador.

### Conceptos del negocio

| Concepto | Qué es | Datos que tiene |
|---|---|---|
| **Usuario** | Una cuenta, de Cliente o de Administrador | Nombres, apellidos, correo (único), teléfono (opcional), dirección (opcional), foto (opcional), rol, activo/inactivo |
| **Categoría** | Tipo de equipo. Son 6: Cámaras de seguridad, Alarmas, Sensores de movimiento, Detectores de humo, Cerraduras digitales, Control de acceso | Nombre (único), activa/inactiva |
| **Marca** | Fabricante del equipo (Hikvision, Dahua, Ezviz, Imou…) | Nombre (único), activa/inactiva |
| **Producto** | Lo que se vende | Nombre, descripción, marca, categoría, precio con IGV incluido (mayor a 0), imagen, activo/inactivo |
| **Stock** | Cuántas unidades hay de un producto | **Disponible**: se puede comprar ya. **Reservado**: apartado para alguien que está pagando |
| **Reserva** | Unidades apartadas durante una compra en curso | Producto, cantidad, compra a la que pertenece, estado, vence a los 15 min |
| **Movimiento de stock** | Registro de cada cambio del stock (historial auditable) | Tipo, producto, cantidad, compra relacionada, motivo, fecha |
| **Carrito** | Lista de lo que el cliente piensa comprar. Uno por cliente | Por cada producto: la cantidad. **No guarda precios** |
| **Venta (pedido)** | Una compra | Cliente, estado, tipo de comprobante, subtotal, IGV, total, fecha y su detalle (producto, nombre y precio **del momento de la compra**, cantidad) |
| **Comprobante** | Boleta o factura de una venta. Uno por venta | Número único (B001-00000001 o F001-00000001), documento y nombre del cliente, subtotal, IGV, total, fecha, PDF |

### Casos de uso

| # | Caso de uso | Actor | Estado |
|---|---|---|---|
| CU01 | Explorar el catálogo | Visitante, Cliente | ✅ |
| CU02 | Ver el detalle de un producto | Visitante, Cliente | ✅ |
| CU03 | Registrarse | Visitante | ✅ |
| CU04 | Iniciar sesión | Visitante | ✅ |
| CU05 | Ver y editar mi perfil | Cliente | ✅ |
| CU06 | Gestionar mi carrito | Cliente | ✅ |
| CU07 | Comprar (checkout) | Cliente | ✅ |
| CU08 | Ver mis pedidos y mis comprobantes | Cliente | ✅ |
| CU09 | Liberar compras abandonadas | Sistema | ✅ |
| CU10 | Gestionar productos | Administrador | ✅ |
| CU11 | Gestionar marcas y categorías | Administrador | ✅ |
| CU12 | Registrar entrada de mercadería | Administrador | ✅ |
| CU13 | Ajustar stock tras un conteo | Administrador | ✅ |
| CU14 | Consultar movimientos de stock | Administrador | ✅ |
| CU15 | Consultar ventas | Administrador | ✅ |
| CU16 | Consultar comprobantes | Administrador | ✅ |
| CU17 | Ver el reporte de ventas | Administrador | ✅ |
| CU18 | Gestionar usuarios | Administrador | ✅ |

#### CU01 · Explorar el catálogo

- **Qué hace:** lista los productos a la venta, de **12 en 12** (por páginas).
- **Puede filtrar por:** categoría, marca y texto de búsqueda (por ejemplo "camara"). Los filtros se combinan.
- **Muestra:** solo productos **activos** de marcas y categorías activas. Cada producto con su nombre, marca, categoría, precio e imagen.
- **Stock:** se puede consultar cuántas unidades **disponibles** tiene cada producto. Con 0 disponibles el producto se muestra igual, pero **no se puede agregar al carrito**.
- **Sin sesión:** sí, es público.

#### CU02 · Ver el detalle de un producto

- **Muestra:** nombre, descripción, marca, categoría, precio (con IGV incluido) e imagen, más las unidades disponibles.
- **Desde aquí:** el Cliente puede agregarlo al carrito (CU06) eligiendo una cantidad, que no puede superar lo disponible. El Visitante debe iniciar sesión o registrarse primero.
- **Si no existe o está inactivo:** "producto no encontrado".

#### CU03 · Registrarse

- **Pide:** nombres, apellidos, correo y contraseña (mínimo 8 caracteres). Teléfono y dirección son opcionales.
- **Resultado:** se crea la cuenta con rol **Cliente**. Nadie puede registrarse como Administrador.
- **Errores:** datos inválidos (correo mal escrito, contraseña corta) · correo ya registrado.

#### CU04 · Iniciar sesión

- **Pide:** correo y contraseña (mínimo 8 caracteres).
- **Resultado:** sesión de 1 hora con su rol. Según el rol, el usuario va a la tienda (Cliente) o al panel (Administrador).
- **Reglas:**
  1. Si el correo no existe o la contraseña está mal, el mensaje es **el mismo** ("Credenciales inválidas"), para no revelar qué correos están registrados.
  2. Cada contraseña errada cuenta un intento. Al **5.º intento seguido** la cuenta se **bloquea 15 minutos**. Durante el bloqueo se rechaza el ingreso aunque la contraseña sea correcta, con un mensaje distinto que indica el bloqueo.
  3. Un ingreso correcto reinicia el contador.
  4. Una cuenta **desactivada** no puede ingresar.

#### CU05 · Ver y editar mi perfil

- **Puede cambiar:** nombres, apellidos, teléfono, dirección, foto y contraseña.
- **No puede cambiar:** su correo ni su rol.
- **Regla:** cada usuario ve y edita **solo su propio** perfil.
- **Endpoint:** `GET` y `PUT /usuarios/me`.

#### CU06 · Gestionar mi carrito

- **Acciones:** ver el carrito, agregar un producto con una cantidad, cambiar la cantidad y quitar un producto.
- **Reglas:**
  1. Cada cliente tiene **un solo carrito**, que se conserva entre sesiones.
  2. Un producto aparece **una sola vez**: agregarlo de nuevo cambia su cantidad, no crea otra fila.
  3. La cantidad es siempre mayor a 0.
  4. Solo se agregan productos que existen y están activos.
  5. El carrito **no congela precios**: muestra el precio actual del catálogo, y el precio que se cobra es el del momento de comprar.
- **Sin sesión:** no hay carrito. El Visitante debe iniciar sesión.

#### CU07 · Comprar (checkout)

El caso de uso más importante. El cliente confirma su carrito y elige el comprobante:

| Comprobante | Para quién | Datos que pide |
|---|---|---|
| **Boleta** (por defecto) | Persona | DNI de 8 dígitos (opcional) y nombre (opcional) |
| **Factura** | Empresa (le sirve para crédito fiscal) | RUC de 11 dígitos que empieza con 10, 15, 17 o 20 y razón social (ambos obligatorios) |

**Qué pasa por dentro, en orden:**

1. Se lee el carrito del cliente. Si está vacío, no hay compra.
2. Se consulta el **precio vigente** de cada producto en el catálogo. No se usa ningún precio enviado por el navegador.
3. Se registra la venta como **PENDIENTE**, con su detalle y montos.
4. Se **apartan** las unidades de **todos** los productos (todo o nada). Si uno solo no alcanza, no se aparta nada.
5. Se emite el **comprobante** con el siguiente número de su serie.
6. Las unidades apartadas pasan a **vendidas**.
7. La venta queda **CONFIRMADA** y el carrito se vacía.

**Resultados posibles para el cliente:**

| Resultado | Cuándo | Qué queda |
|---|---|---|
| **Compra exitosa** | Todo salió bien | Venta CONFIRMADA, comprobante emitido (con número), carrito vacío |
| **Stock insuficiente** | En el paso 4 falta stock de algún producto. Se indica cuál | Venta ANULADA, no se apartó nada, **el carrito se conserva** para que lo corrija |
| **Servicio no disponible** | En el paso 5 el comprobante falla o tarda más de 2 s | Las unidades se devuelven, venta ANULADA, carrito conservado. Puede reintentar más tarde |
| **Datos inválidos** | DNI, RUC o razón social no cumplen las reglas | No se crea nada |

**Garantía del negocio:** nunca queda una venta a medias con stock descontado ni un comprobante sin venta confirmada.

#### CU08 · Ver mis pedidos y mis comprobantes

- **Mis pedidos:** lista de sus compras, de la más reciente a la más antigua, con fecha, estado, tipo de comprobante y total. El detalle muestra cada producto con el nombre y precio **que tenía al comprar** (aunque el catálogo haya cambiado después) y el desglose subtotal + IGV = total.
- **Mis comprobantes:** número, tipo, fecha y montos de cada uno. Se puede **descargar el PDF**. Desde un pedido se llega a su comprobante.
- **Regla:** el cliente ve **solo lo suyo**. Si intenta abrir un pedido o comprobante ajeno, se le niega el acceso.

#### CU09 · Liberar compras abandonadas (automático)

- Si una compra queda a medias (por ejemplo, se cayó la conexión), sus unidades apartadas se **devuelven a disponible a los 15 minutos** y la venta PENDIENTE pasa a **ANULADA**.
- Así un checkout abandonado nunca bloquea stock para otros clientes.

#### CU10 · Gestionar productos

- **Acciones:** listar (incluye los inactivos), crear, editar y **desactivar**.
- **Datos:** nombre, descripción, marca, categoría, precio con IGV (mayor a 0) e imagen.
- **Reglas:**
  1. Un producto **nunca se borra**, solo se desactiva. Así las ventas pasadas lo siguen mostrando. Se puede volver a activar.
  2. Cambiar el precio **no afecta** ventas pasadas.
  3. Un producto nuevo **nace con 0 unidades**: para venderlo hay que registrar una entrada de mercadería (CU12).

#### CU11 · Gestionar marcas y categorías

- **Acciones:** listar, crear, renombrar y desactivar.
- **Reglas:** el nombre es único. Nunca se borran, se desactivan. Una marca o categoría inactiva deja de aparecer en la tienda junto con sus productos.
- **Endpoint:** `GET`, `POST` y `PUT` de `/marcas` y `/categorias`.

#### CU12 · Registrar entrada de mercadería

- **Pide:** producto, cantidad (mayor a 0) y un motivo opcional (por ejemplo "Compra a proveedor").
- **Resultado:** suben las unidades **disponibles** y queda un movimiento **ENTRADA** en el historial.

#### CU13 · Ajustar stock tras un conteo

- **Para qué:** corregir el stock cuando el conteo físico no coincide (rotura, pérdida, error de carga).
- **Pide:** producto, cantidad (puede ser negativa) y un **motivo obligatorio**.
- **Regla:** el disponible nunca puede quedar por debajo de 0.
- **Resultado:** queda un movimiento **AJUSTE** con el motivo.

#### CU14 · Consultar movimientos de stock

- **Muestra:** el historial de un producto con fecha, tipo, cantidad, compra relacionada y motivo.
- **Tipos de movimiento:**

| Tipo | Quién lo genera | Efecto en el stock |
|---|---|---|
| ENTRADA | Administrador (CU12) | Disponible sube |
| RESERVA | Una compra en curso (CU07) | Disponible baja, reservado sube |
| CONFIRMACION | Una compra exitosa (CU07) | Reservado baja: las unidades salen vendidas |
| LIBERACION | Una compra fallida o abandonada (CU07, CU09) | Reservado baja, disponible sube |
| AJUSTE | Administrador (CU13) | Disponible sube o baja |

- **Conciliación:** el stock actual siempre debe coincidir con la suma de su historial. Si no coincide, es un error que hay que investigar.

#### CU15 · Consultar ventas

- **Muestra:** **todas** las ventas, de todos los clientes, con cliente, fecha, estado, comprobante y total, y el detalle de cada una.
- **Estados de una venta:**

| Estado | Significado |
|---|---|
| PENDIENTE | Compra en curso. Dura como máximo 15 minutos |
| CONFIRMADA | Venta firme, con comprobante |
| ANULADA | Intento fallido (sin stock, comprobante caído o compra abandonada). Se conserva como registro, no se borra |


#### CU16 · Consultar comprobantes

- **Muestra:** todos los comprobantes con su número, tipo, cliente, fecha y montos. Permite buscar el de una venta y descargar el PDF.
- **Reglas de numeración:**
  1. Boletas en la serie **B001** y facturas en la serie **F001**, cada una con su propio contador.
  2. El número es correlativo y **nunca se repite, reutiliza ni borra**.
  3. Una venta tiene **un solo** comprobante. Si se pide dos veces para la misma venta, se devuelve el mismo.

#### CU17 · Ver el reporte de ventas

- **Pide:** rango de fechas (desde y hasta).
- **Muestra:** los comprobantes emitidos en ese rango y sus totales (subtotal, IGV y total acumulados).
- **Regla:** solo cuentan las ventas **confirmadas**, porque son las únicas con comprobante.

#### CU18 · Gestionar usuarios

- **Acciones:** listar, ver el detalle, crear (Cliente o Administrador), editar y **desactivar**.
- **Reglas:**
  1. Un usuario **nunca se borra**: tiene ventas asociadas. Al desactivarlo ya no puede iniciar sesión.
  2. Solo un Administrador puede crear a otro Administrador.
  3. El correo es único.

### Qué puede hacer cada actor

| Acción | Visitante | Cliente | Administrador |
|---|---|---|---|
| Ver catálogo, detalle y stock disponible | ✅ | ✅ | ✅ |
| Registrarse e iniciar sesión | ✅ | — | — |
| Perfil propio | — | ✅ | ✅ |
| Carrito y compra | — | ✅ | — |
| Pedidos y comprobantes | — | Solo los suyos | Todos |
| Productos, marcas y categorías (crear, editar, desactivar) | — | — | ✅ |
| Entradas, ajustes y movimientos de stock | — | — | ✅ |
| Reporte de ventas | — | — | ✅ |
| Usuarios | — | — | ✅ |

### Reglas de negocio

| # | Regla |
|---|---|
| RN01 | Los precios **incluyen IGV (18 %)**. El comprobante lo desglosa: subtotal = total ÷ 1.18 · IGV = total − subtotal |
| RN02 | Se cobra el **precio vigente al comprar**. El precio de cada producto vendido queda guardado en la venta y no cambia después |
| RN03 | **Nunca se vende más de lo disponible.** La compra aparta todo o nada |
| RN04 | Dos clientes no pueden comprar la misma última unidad: el primero que la aparta se la lleva |
| RN05 | Las unidades apartadas se devuelven si la compra falla o se abandona (15 min) |
| RN06 | **Nada se borra**: productos, marcas, categorías y usuarios se desactivan. Ventas y comprobantes se conservan siempre |
| RN07 | Cada cliente ve y opera **solo lo suyo**. Su identidad sale de su sesión, nunca de un dato que pueda escribir |
| RN08 | Boleta: DNI de 8 dígitos opcional. Factura: RUC de 11 dígitos (empieza con 10, 15, 17 o 20) y razón social obligatorios |
| RN09 | Numeración de comprobantes única y correlativa por serie (B001, F001), sin huecos reutilizados |
| RN10 | Un comprobante por venta |
| RN11 | Login: mensaje genérico ante error · 5 intentos fallidos seguidos bloquean 15 min · cuenta desactivada no ingresa · sesión de 1 hora |
| RN12 | Contraseña de al menos 8 caracteres. Nunca se muestra ni se devuelve |
| RN13 | Todo cambio de stock deja un movimiento en el historial. Un ajuste exige motivo |
| RN14 | Un producto nuevo no se puede vender hasta registrar su primera entrada de stock |

### Ejemplo de punta a punta

1. El Administrador crea el producto "Cámara IP Hikvision" a **S/ 199.90** (CU10). Nace con 0 unidades.
2. Registra una entrada de **10 unidades** (CU12): disponible 10.
3. Ana se registra (CU03), inicia sesión (CU04) y agrega **2 cámaras** al carrito (CU06).
4. Ana compra con **boleta** (CU07):
   - Total: 2 × 199.90 = **S/ 399.80**
   - Subtotal: 399.80 ÷ 1.18 = **S/ 338.81**
   - IGV: 399.80 − 338.81 = **S/ 60.99**
   - Se apartan 2 (disponible 8, reservado 2), se emite la boleta **B001-00000001**, las 2 salen vendidas (reservado 0) y la venta queda CONFIRMADA.
5. Luis intenta comprar **9 cámaras**: solo hay 8. Recibe "stock insuficiente", su venta queda ANULADA y su carrito sigue igual.
6. Ana ve su pedido y descarga su boleta en PDF (CU08). El Administrador ve ambas ventas, la de Ana confirmada y la de Luis anulada (CU15), y en el historial del producto: ENTRADA 10, RESERVA 2, CONFIRMACION 2 (CU14).

### Respuestas que el usuario puede recibir

Toda pantalla debe saber qué hacer con estas respuestas del backend:

| Situación | Código | Qué debe entender el usuario |
|---|---|---|
| Éxito | 200 / 201 | La acción se hizo |
| Datos inválidos | 400 | Qué campo está mal y por qué (el backend lo detalla) |
| Credenciales incorrectas o sesión vencida | 401 | "Credenciales inválidas" en el login. En otra pantalla, que debe iniciar sesión de nuevo |
| Sin permiso | 403 | No puede ver o hacer eso (por ejemplo, un pedido ajeno o una opción de administrador) |
| No existe | 404 | El producto, pedido o comprobante no existe |
| Conflicto | 409 | Stock insuficiente (indica el producto) o correo ya registrado |
| Cuenta bloqueada | 423 | Demasiados intentos. Esperar 15 minutos |
| Servicio no disponible | 503 | No se pudo completar la compra. No se cobró nada. Reintentar más tarde |
