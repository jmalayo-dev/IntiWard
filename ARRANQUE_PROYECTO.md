# Arranque del proyecto · IntiGuard v2

Guía para el primer día de cada integrante: qué instalar, cómo levantar tu servicio, cómo trabajar con el equipo y **qué le debes a quién** para no bloquear a nadie.

> Antes de empezar necesitas que el integrante 1 te pase **por privado** los datos de conexión a la base (`AIVEN_HOST`, `AIVEN_PORT`) y la contraseña de **tu** servicio (`DB_PASS_<SERVICIO>`).

---

## 1. Qué servicio es tuyo

| Integrante | Carpeta(s) | Puerto | Variables que necesitas en `.env` |
|---|---|---|---|
| 1 · Mathias | `ms-gateway/`, `ms-discovery/`, `docker-compose.yml`, `pom.xml`, `.github/`, layout de `web-client/` | 8080, 8761 | Todas |
| 2 | `ms-auth/`, `common-security/` | 8081 | `DB_PASS_AUTH`, `ADMIN_CORREO`, `ADMIN_CLAVE` |
| 3 | `ms-catalogo/` | 8082 | `DB_PASS_CATALOGO` |
| 4 | `ms-pedidos/` | 8084 | `DB_PASS_PEDIDOS` (y desde la semana 4, las de catálogo, inventario y comprobantes para probar el checkout) |
| 5 | `ms-inventario/`, `ms-comprobantes/` | 8083, 8085 | `DB_PASS_INVENTARIO`, `DB_PASS_COMPROBANTES` |

Todos necesitan además `AIVEN_HOST` y `AIVEN_PORT`. **El README de tu carpeta es tu guía principal**: modelo de datos, guía de negocio, endpoints, dependencias y tareas por semana.

---

## 2. Requisitos (una sola vez)

| Herramienta | Verificar | Instalar (Ubuntu/Mint) |
|---|---|---|
| JDK **21** | `javac -version` | `sudo apt install openjdk-21-jdk` |
| Docker + Compose | `docker compose version` | [docs.docker.com/engine/install](https://docs.docker.com/engine/install/) y `sudo usermod -aG docker $USER` (cerrar sesión y volver a entrar) |
| Git | `git --version` | `sudo apt install git` |
| Postman | — | postman.com/downloads |

Maven no hace falta: se usa `./mvnw`. Si tu JDK por defecto no es el 21, antes de usar `./mvnw`:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

---

## 3. Primer día, paso a paso

Los ejemplos usan **ms-catalogo**: cambia el nombre y el puerto por los de tu servicio.

```bash
# 1. Clonar y pasarte a develop (la rama de trabajo del equipo)
git clone git@github.com:jmalayo-dev/IntiWard.git
cd IntiWard
git switch develop

# 2. Credenciales: el integrante 1 te pasa por privado tu archivo .env.
#    Guárdalo en la raíz del repo (al lado de docker-compose.yml) con el nombre exacto .env

# 3. Compilar y correr las pruebas (usan H2 en memoria: no necesitan la base ni el .env)
./mvnw verify                                    # debe terminar en BUILD SUCCESS

# 4. Levantar tu servicio (la primera vez tarda unos minutos en construir la imagen)
docker compose up -d --build ms-catalogo
docker compose ps                                # ms-discovery y ms-catalogo en "healthy"
curl localhost:8082/productos                    # debes ver los productos de la base

# 5. Al terminar el día
docker compose down
```

**Para programar y depurar desde el IDE** (IntelliJ, VS Code) o con Maven:

```bash
docker compose up -d ms-discovery                # solo Eureka en Docker
docker compose stop ms-catalogo                  # si estaba corriendo en Docker (libera el puerto)
./mvnw -q install -DskipTests                    # una vez, y cada vez que cambie common-security
./mvnw -pl ms-catalogo spring-boot:run           # o "Run" de CatalogoApplication en el IDE
```

El servicio lee solo el `.env` de la raíz del repo. No hace falta exportar variables.

---

## 4. Reglas que no se negocian

**Credenciales**
- El `.env` **nunca se sube** (Git ya lo ignora). No pegues su contenido en el grupo, en un issue ni en el código.
- Cada uno tiene solo la contraseña de su servicio. Si necesitas otra, pídesela al integrante 1.

**Base de datos compartida (Aiven)**
- Hay **una sola base para los cinco**, con un máximo de **46 conexiones**. Levanta solo tu servicio y apágalo con `docker compose down` al terminar.
- No borres ni modifiques datos de otros servicios. Tu usuario MySQL igual no puede: solo tiene permisos sobre tu schema.
- **Para cambiar una tabla**: actualiza tu `infra/mysql/0X-<servicio>.sql` en el PR y avisa al integrante 1, que aplica el cambio en Aiven al mergear. Nadie recrea la base.
- Los datos de prueba nuevos van en `infra/mysql/inserts-test/0X-<servicio>.sql`.

**Git**
- Ramas: `feature/<servicio>/<tarea>` creadas **desde `develop`**. Ej.: `feature/catalogo/crud-productos`.
- Commits en español, con el servicio como ámbito: `feat(catalogo): filtros por marca y categoría`.
- Todo entra por **Pull Request a `develop`**, con CI en verde y la aprobación de otro integrante.
- Toca solo tu carpeta y tus `.sql`. `docker-compose.yml`, el `pom.xml` raíz, `.github/` y las rutas del gateway los edita solo el integrante 1: si necesitas un cambio, pídelo.
- **Si cambias un endpoint o un DTO que otro servicio consume, avisa al dueño antes de mergear** (ver la sección 5).

---

## 5. Quién le debe a quién

Cada servicio es de una persona, pero varios necesitan endpoints de otros para avanzar. Estos son los **contratos**: lo que cada uno se compromete a entregar, con su forma exacta y la semana límite. El detalle de cada contrato (cuerpos, respuestas, códigos HTTP) está en la sección "Dependencias con otros servicios" del README del servicio que lo entrega.

```
                 C1 formato JWT (S2) · C2 filtro + UsuarioActual (S3) · C3 login (S3)
  Integrante 2 ─────────────────────────────────────────────────────────▶ todos
  (auth + common-security)

  Integrante 1 ── C4 gateway valida JWT y bloquea rutas internas (S3) ──▶ todos
  (gateway)

  Integrante 3 ── C5 GET /productos/{id} con "activo" (S2) ────────────▶ integrantes 4 y 5
  (catálogo)   ── C9 200 productos (S5) ──────────────────────────────▶ integrante 1 (carga en Aiven)

  Integrante 5 ── C6 reservar / confirmar / liberar (fin S3) ──────────▶ integrante 4
  (inventario  ── C7 POST /comprobantes (fin S3) ──────────────────────▶ integrante 4
   comprobantes)   GET /comprobantes/venta/{idVenta} (S4)

  Integrante 4 ── C8 idVenta real + token reenviado + liberar ante fallo (S4) ──▶ integrante 5
  (pedidos)
```

| # | Contrato | Lo entrega | Lo necesita | Semana | Estado | README con el detalle |
|---|---|---|---|---|---|---|
| C1 | El JWT trae `sub` (correo), `rol` e **`idUsuario`** | 2 (lo adelantó el 1) | Todos | **2** | ✅ | `ms-auth`, `common-security` |
| C2 | `JwtAuthFilter` (401/403 en JSON) y `UsuarioActual.id()/correo()/rol()` | 2 (lo adelantó el 1) | 3, 4, 5 | 3 | ✅ | `common-security` |
| C3 | `POST /auth/login` → `{token, tipo, expiraEnMinutos, rol}` · 401 · 423 | 2 (lo adelantó el 1) | 1, web-client | 3 | ✅ | `ms-auth` |
| C4 | Gateway: rutas públicas, 401 sin token, 403 en rutas internas | 1 | Todos | 3 | ✅ | `ms-gateway` |
| C5 | `GET /productos/{id}` con `activo`; 404 si no existe | 3 | 4, 5 | **2** | ⬜ | `ms-catalogo` |
| C6 | `POST /stock/reservar` (todo o nada, 409), `/confirmar` y `/liberar` por `idVenta` (idempotentes) | 5 | 4 | fin de 3 | ⬜ | `ms-inventario` |
| C7 | `POST /comprobantes` (idempotente por `idVenta`, < 2 s, 400 si el documento es inválido) y `GET /comprobantes/venta/{idVenta}` | 5 | 4, web-client | fin de 3 / 4 | ⬜ | `ms-comprobantes` |
| C8 | Venta PENDIENTE antes de reservar, token reenviado por Feign, `liberar` ante cualquier fallo | 4 | 5 | 4 | ⬜ | `ms-pedidos` |
| C9 | 200 productos en `inserts-test/02-catalogo.sql` | 3 | 1 | 5 | ⬜ | `ms-catalogo` |

**Los dos caminos críticos:**
1. **Seguridad:** C1 a C4 están listos. Falta el **registro de clientes** (integrante 2) para que haya usuarios además del admin; mientras tanto el sistema corre con `EXIGIR_TOKEN=false` (ver la sección 6).
2. **Checkout:** C5 + C6 + C7 → checkout del integrante 4 en la **semana 4**, la semana bisagra del cronograma. Si C6 o C7 se atrasan, se atrasa todo.

**Si vas a cambiar algo de un contrato** (un nombre de campo, un código HTTP, una ruta): avisa por el grupo, actualiza el README y espera el visto bueno de quien lo consume. Romper un contrato sin avisar rompe el trabajo de otro.

---

## 6. Cómo avanzar mientras esperas una dependencia

**Tokens para probar.** El login real existe (`POST /auth/login`), pero solo funciona con usuarios de `db_auth` y para eso hay que levantar ms-auth, que necesita `DB_PASS_AUTH`. Para el día a día, genera un token de desarrollo (no necesita ms-auth ni la base). El `.env` trae `EXIGIR_TOKEN=false`: los endpoints responden sin token, pero si mandas uno se valida y `UsuarioActual` funciona:

```bash
./infra/scripts/token-dev.sh 7 CLIENTE        # idUsuario 7, rol CLIENTE
curl -H "Authorization: Bearer $(./infra/scripts/token-dev.sh 1 ADMIN)" localhost:8081/usuarios
```

Las rutas internas (`POST /stock/reservar|confirmar|liberar`, `POST /comprobantes`) responden **403 a través del gateway** siempre; pruébalas directo en el puerto del servicio.

| Si eres… | Y te falta… | Haz esto mientras tanto |
|---|---|---|
| Integrante 3, 4 o 5 | Un usuario real en `db_auth` | Usa `@PreAuthorize` y `UsuarioActual` desde ya, y pruébalos con `token-dev.sh` |
| Integrante 4 | Tokens de usuarios reales | `usuarioActual.id()` con un token de `token-dev.sh`; el `idUsuario` que pongas es el dueño del carrito |
| Integrante 4 | C6 y C7 | Escribe `InventarioClient` y `ComprobantesClient` con los contratos de los READMEs y prueba el checkout con **Mockito**, simulando los clientes |
| Integrante 5 | C8 (ventas reales) | Prueba reservar y emitir con **`idVenta` inventados** desde Postman: `id_venta` no tiene FK, así que funciona |
| Integrante 1 | C3 (login) | Genera tokens de prueba con `JwtService` en un test y desarrolla el filtro del gateway |

---

## 7. Problemas típicos

| Síntoma | Causa | Solución |
|---|---|---|
| `Falta AIVEN_HOST en .env` al usar `docker compose` | No hay `.env` en la raíz o le falta la variable | Pide tu `.env` al integrante 1 y guárdalo en la raíz del repo |
| El servicio no arranca: `Access denied for user 'ms_…'` | `DB_PASS_<SERVICIO>` vacía o mal copiada | Revisa el `.env` (sin espacios ni comillas) |
| `Too many connections` | La base compartida llegó a 46 conexiones | Apaga lo que no uses (`docker compose down`) y avisa por el grupo |
| `Could not find artifact pe.intiguard:common-security` | No se instaló la librería común | `./mvnw -q install -DskipTests` |
| `release version 21 not supported` o errores raros de compilación | Maven usa otro JDK | `export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` |
| `Port 8082 is already in use` | Tu servicio sigue corriendo en Docker | `docker compose stop ms-catalogo` |
| El gateway responde **503** recién levantado | Todavía no recibió la lista de Eureka | Espera unos 30 s |
| Todo responde **401** con `WWW-Authenticate: Basic` | Se rompió el orden de la autoconfiguración de `common-security` | Ver "Ojo con el orden" en `common-security/README.md` |
| **401** `"Token inválido o vencido"` | El token venció (60 min) o se firmó con otra `JWT_SECRET` | Genera otro con `token-dev.sh`; todos deben usar la misma `JWT_SECRET` |
| **403** `"Ruta de uso interno entre servicios"` | Llamaste a reservar, confirmar, liberar o emitir a través del gateway | Es a propósito: pruébalas directo en el puerto del servicio (8083, 8085) |
| Un endpoint con `@PreAuthorize` responde **401** sin token | Pide un rol y no hay usuario | Manda un token: `token-dev.sh 1 ADMIN` |

Si nada de esto aplica, manda al grupo la salida de `docker compose logs <tu-servicio>`.

---

## 8. Dónde está cada cosa

| Necesito… | Archivo |
|---|---|
| Mis tareas, modelo, endpoints y dependencias | `<mi-servicio>/README.md` |
| Arquitectura, stack y estado general | `README.md` |
| El plan completo y el cronograma de 8 semanas | `docs/Plan de ejecución — IntiGuard v2.md` |
| La estructura de mis tablas | `infra/mysql/0X-<servicio>.sql` |
| Mis datos de prueba | `infra/mysql/inserts-test/0X-<servicio>.sql` |
| Las variables del `.env` | Te las pasa el integrante 1 por privado |
| Probar endpoints | `postman/` |
