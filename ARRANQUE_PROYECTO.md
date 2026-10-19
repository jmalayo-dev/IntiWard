# Arranque del proyecto · IntiGuard v2

Guía para que cualquier integrante deje su microservicio funcionando, **paso a paso y sin saltarse nada**. Está escrita para **Windows + IntelliJ IDEA**. Si algo no coincide con lo que ves en tu pantalla, ve a la sección [12. Problemas típicos](#12-problemas-típicos).

---

## 1° Paso

**Por privado (WhatsApp)** se envió:

1. **El archivo base** `.env`, con las credenciales. Lo pegas en el proyecto en la sección 4.
2. **El certificado** `ca.pem`, para conectarte a la base con DBeaver en la sección 8.
3. **La lista de contraseñas** (como mensaje de texto, no dentro del `.env`), una por microservicio. Busca la de **tu** microservicio: la copias dentro de tu `.env` en la sección 4.4 y la usas otra vez al conectarte con DBeaver en la sección 8. Si llevas dos microservicios, son dos contraseñas.

Además:

- **Tu rama** es la que lleva tu apellido. Ramas disponibles:
  - `b-galarza` → Galarza (Autenticación y Comprobantes)
  - `b-cesar` → César (Catálogo)
  - `b-arevalo` → Arévalo (Inventario)
  - `b-guerrero` → Evelyn (Pedidos)
  - `b-alayo` → Mathias (Web, gateway e integración)
- **Acceso al repositorio**: te llega una invitación de GitHub a tu correo. Acéptala antes de clonar; si no, Git te dirá `Repository not found`.

---



## 1. Que verificar y lugar de trabajo

### 1.1 Lugar de trabajo de cada integrante


| Integrante  | Servicio                                                        | Carpeta                                       | Puerto           |
| ----------- | --------------------------------------------------------------- | --------------------------------------------- | ---------------- |
| 1 · Galarza | Autenticación                                                   | `ms-auth/`                                    | 8081             |
| 1 · Galarza | Comprobantes                                                    | `ms-comprobantes/`                            | 8085             |
| 2 · César   | Catálogo                                                        | `ms-catalogo/`                                | 8082             |
| 3 · Arévalo | Inventario                                                      | `ms-inventario/`                              | 8083             |
| 4 · Evelyn  | Pedidos                                                         | `ms-pedidos/`                                 | 8084             |
| 5 · Mathias | Web (`web-client`), gateway, Eureka, integración y credenciales | `web-client/`, `ms-gateway/`, `ms-discovery/` | 8090, 8080, 8761 |


- **Eureka** (`ms-discovery`, puerto 8761) lo levanta Docker solo junto con tu servicio: no tienes que hacer nada.
- **No modifiques archivos que no sean de tu microservicio**: los de Docker (`docker-compose.yml` y los `Dockerfile`, incluido el de tu carpeta), la configuración global del proyecto (`pom.xml` de la raíz, `.github/`, `.gitignore`), la carpeta `infra/` (base de datos y scripts) ni las carpetas de otros servicios. Si necesitas un cambio en alguno, pídelo por el grupo.
- **Si tu trabajo depende de otro microservicio, pídeselo a su dueño: no lo programes tú en el suyo ni lo resuelvas por tu cuenta en el tuyo** (por ejemplo, copiando sus datos o consultando su base). El dueño te entrega **el endpoint que vas a usar** y te explica:
  - **la ruta y el método** (por ejemplo `GET /productos/{id}`);
  - **qué datos tienes que enviarle**;
  - **qué datos te devuelve** (los campos y un ejemplo de respuesta);
  - **qué respuestas de error puede darte** (por ejemplo 404 si no existe).

  Mientras no esté listo, acuerden esos datos entre los dos y avanza con ellos (ver la sección 11). Los pedidos ya acordados están en la sección 10, "Quién le debe a quién".
- **El README de tu carpeta es tu guía de trabajo**: modelo de datos, reglas de negocio, endpoints, dependencias y tareas por semana.



### 1.2 Qué verificar en tu `.env`

Cada servicio tiene su propia contraseña en el `.env` y su propio usuario y base de datos en MySQL. Estos datos los usas en la sección 4 (revisar el `.env`) y en la sección 8 (DBeaver).

**Las contraseñas no están en este documento: se enviaron por WhatsApp como una lista en texto.** Este documento se sube a GitHub, así que nunca escribas una contraseña aquí ni en ningún archivo del proyecto.

| Servicio | Variable en tu `.env` | Usuario MySQL | Base de datos | Contraseña |
| --- | --- | --- | --- | --- |
| Autenticación | `DB_PASS_AUTH` | `ms_auth` | `db_auth` | La de la lista de WhatsApp; la copias en `DB_PASS_AUTH=` de tu `.env` |
| Comprobantes | `DB_PASS_COMPROBANTES` | `ms_comprobantes` | `db_comprobantes` | La de la lista de WhatsApp; la copias en `DB_PASS_COMPROBANTES=` de tu `.env` |
| Catálogo | `DB_PASS_CATALOGO` | `ms_catalogo` | `db_catalogo` | La de la lista de WhatsApp; la copias en `DB_PASS_CATALOGO=` de tu `.env` |
| Inventario | `DB_PASS_INVENTARIO` | `ms_inventario` | `db_inventario` | La de la lista de WhatsApp; la copias en `DB_PASS_INVENTARIO=` de tu `.env` |
| Pedidos | `DB_PASS_PEDIDOS` | `ms_pedidos` | `db_pedidos` | La de la lista de WhatsApp; la copias en `DB_PASS_PEDIDOS=` de tu `.env` |

- **Galarza lleva dos microservicios**: su `.env` debe tener **las dos** líneas, `DB_PASS_AUTH` y `DB_PASS_COMPROBANTES`.
- **Ojo con los nombres de las bases: llevan guion bajo (**`db_catalogo`**), nunca guion medio (**`db-catalogo`**).**

---



## 2. Instalar las herramientas (una sola vez)


| Herramienta           | Para qué                         | Dónde se descarga                                      | Cómo comprobar que quedó              |
| --------------------- | -------------------------------- | ------------------------------------------------------ | ------------------------------------- |
| **Git**               | Bajar y subir el código          | 🌐 git-scm.com/download/win                            | 🖥️ `git --version`                   |
| **Docker Desktop**    | Levantar los microservicios      | 🌐 docker.com/products/docker-desktop                  | 🖥️ `docker compose version`          |
| **JDK 21**            | Compilar y correr desde IntelliJ | 🌐 adoptium.net (Temurin 21, instalador `.msi`)        | 🖥️ `java -version` → debe decir `21` |
| **IntelliJ IDEA**     | Programar                        | 🌐 jetbrains.com/idea                                  | —                                     |
| **DBeaver Community** | Ver tus tablas                   | 🌐 dbeaver.io/download (versión **Community**, gratis) | —                                     |
| **Postman**           | Probar endpoints                 | 🌐 postman.com/downloads                               | —                                     |


**Docker Desktop tiene que estar abierto** (ícono de la ballena en la barra de tareas, en estado *running*) cada vez que vayas a usar `docker`. Si no, los comandos fallan con `Cannot connect to the Docker daemon` o `error during connect`.

---



## 3. Clonar el proyecto y pararte en tu rama

🖥️ **Terminal de IntelliJ** (o PowerShell), en la carpeta donde quieras guardar el proyecto:

```powershell
git clone https://github.com/jmalayo-dev/IntiWard.git
cd IntiWard
git switch b-tuapellido
git pull origin develop
git branch --show-current
```

- La primera vez, Windows abre una ventana para iniciar sesión en GitHub: inicia sesión con tu cuenta.
- `git switch b-tuapellido` → cambia `tuapellido` por el tuyo (`b-cesar`, `b-galarza`…).
- `git pull origin develop` trae lo último que entró al proyecto.
- El último comando debe mostrar **tu rama**. En IntelliJ también la ves arriba, al lado del nombre del proyecto.

🧩 **IntelliJ**: **File → Open** → elige la carpeta `IntiWard` → **Trust Project**. Espera a que termine de cargar (barra de progreso abajo a la derecha).

> **Regla:** siempre trabajas en **tu** rama `b-<apellido>`. Nunca en `develop` ni en `main`.

---



## 4. Tu archivo `.env` (credenciales)

El `.env` le dice a tu microservicio cómo entrar a la base de datos. **Es personal: no se sube a GitHub ni se comparte.** Git ya lo ignora.

### 4.1 Descargar el archivo base y pegarlo en la raíz del proyecto

1. 💬 **Por privado (WhatsApp)**: descarga el archivo `.env` que te envía Mathias. Normalmente queda en tu carpeta **Descargas**.
2. 📁 **Explorador de Windows**: abre **Descargas**, selecciona el archivo y cópialo (**Ctrl+C**).
3. 🧩 **IntelliJ**: en el árbol del proyecto (panel izquierdo), haz clic en la **carpeta raíz**: la primera de arriba, con el nombre del proyecto (`IntiWard` o el que le hayas puesto). **No** dentro de `ms-catalogo`, `infra` ni otra subcarpeta.
4. 🧩 **IntelliJ**: pégalo (**Ctrl+V**). Si aparece la ventana **Copy**, deja el nombre como está y pulsa **OK**.
5. 🧩 **IntelliJ**: el archivo aparece en la raíz, al lado de `docker-compose.yml` y `pom.xml`, pero con el nombre `env`**, sin el punto**. Es normal: al copiarlo, el sistema de archivos le quita el punto. Lo arreglas en el paso siguiente.



### 4.2 Renombrarlo a `.env` con Refactor → Rename

1. 🧩 **IntelliJ**: clic derecho sobre el archivo `env` → **Refactor → Rename…** (o selecciónalo y pulsa **Shift+F6**).
2. En la ventana **Rename**, escribe el nombre nuevo: `.env`. Asegúrate de que tenga el **punto al inicio** y nada más: sin espacios, sin `.txt`, sin `(1)`.
3. **Desmarca las dos casillas de búsqueda**, para que IntelliJ no toque otros archivos:
  - ☐ **Search in comments and strings**
  - ☐ **Search for text occurrences**
4. Pulsa **Refactor**.
5. **Si aparece la ventana "Refactoring Preview"** con una lista de "occurrences" en otros archivos (por ejemplo `docker-compose.yml`), **no pulses "Do Refactor"**. Ciérrala (**Cancel**) y repite desde el paso 1 con las dos casillas desmarcadas. Si la aplicas, IntelliJ modifica otros archivos del proyecto.



### 4.3 Comprobar que quedó bien

🧩 **IntelliJ**: en el árbol del proyecto debe verse `.env` en la raíz, al lado de `.gitignore`. Si IntelliJ ofrece *"Plugins supporting .env files found"*, puedes ignorarlo.

🖥️ **Terminal de IntelliJ**:

```powershell
Get-ChildItem -Force -Filter "*env*"
git status
```

El primer comando debe mostrar **exactamente** `.env`. Estos nombres están **mal** y hacen que tu servicio no arranque:


| Aparece                | Problema                               | Arreglo                                         |
| ---------------------- | -------------------------------------- | ----------------------------------------------- |
| `env`                  | Falta el punto: el Rename no se aplicó | Repetir la sección 4.2                          |
| `.env.txt`             | Se le agregó `.txt`                    | Refactor → Rename a `.env`                      |
| `env (1)` o `.env (1)` | Se descargó dos veces                  | Borrar el repetido y renombrar el otro a `.env` |


`git status` **no** debe mostrar el `.env` (Git lo ignora) **ni ningún otro archivo modificado**. Si aparece, por ejemplo, `docker-compose.yml` como modificado sin que lo hayas tocado, el Rename cambió algo por error: deshazlo con `git checkout -- docker-compose.yml` (cambia el nombre por el archivo que aparezca).

### 4.4 Comprobar el contenido

🧩 **IntelliJ**: abre `.env` con doble clic.

**Primero, pon tu contraseña.** En la lista de contraseñas que llegó por WhatsApp, busca la de **tu** microservicio, cópiala y pégala justo después del `=` de su línea, sin espacios. Por ejemplo, para Catálogo:

```
DB_PASS_CATALOGO=aquí-pegas-la-contraseña-de-catálogo
```

Si llevas dos microservicios, completa **las dos líneas**, cada una con su propia contraseña. Las líneas `DB_PASS_…` de servicios que no son tuyos pueden quedar vacías o puedes borrarlas.

**Después, revisa que el archivo tenga estas líneas y nada raro:**

```
AIVEN_HOST=mysql-2ab2661d-jmalayo-7c9a.f.aivencloud.com
AIVEN_PORT=10751
DB_PASS_CATALOGO=<la contraseña de tu microservicio, de la lista de WhatsApp>
JWT_SECRET=clave-local-solo-para-desarrollo-no-usar-en-nube
EXIGIR_TOKEN=false
```

Revisa uno por uno:

- [ ] `AIVEN_HOST` **sin** `http://` al inicio y **sin** `/` al final.
- [ ] `AIVEN_PORT=10751`.
- [ ] **La línea** `DB_PASS_…` **de tu microservicio**, con el nombre exacto de la tabla de la sección 1.2 (por ejemplo `DB_PASS_CATALOGO`) y **la contraseña de tu microservicio** copiada de la lista (no la de otro servicio). Si vas a levantar dos microservicios (por ejemplo inventario y comprobantes), van **las dos líneas completas**, cada una con su contraseña.
- [ ] `JWT_SECRET` **exactamente** `clave-local-solo-para-desarrollo-no-usar-en-nube`. Si es más corta (menos de 32 caracteres) o está vacía, **tu servicio se apaga apenas arranca**.
- [ ] Sin espacios alrededor del `=` y sin comillas.
- [ ] Una sola línea por variable (si `JWT_SECRET` aparece dos veces, borra la repetida).
- [ ] No hacen falta `ADMIN_CORREO`, `ADMIN_CLAVE` ni `AIVEN_ADMIN_…`. Si están, puedes borrarlas.

**Nunca copies tu** `.env` **a un chat, una captura, un issue o un archivo del proyecto.** Si compartes pantalla, ciérralo antes.

---



## 5. Levantar tu microservicio con Docker



### 5.1 La primera vez: limpiar lo viejo

Si alguna vez levantaste versiones anteriores del proyecto, quedan contenedores viejos (incluido un MySQL local que ya no se usa).

🖥️ **Terminal de IntelliJ**:

```powershell
docker compose down --remove-orphans
docker rm -f intiguard-mysql
docker ps
```

- Si no tenías el MySQL viejo, el segundo comando no muestra nada (o dice `No such container`): está bien.
- `docker ps` debe mostrar **solo los títulos** (ningún contenedor de IntiGuard corriendo).



### 5.2 Levantar **solo** tu servicio

La base de datos es **compartida por todo el equipo y acepta pocas conexiones**: cada uno levanta **solo lo suyo**. Docker levanta además Eureka (`ms-discovery`) automáticamente.

🖥️ **Terminal de IntelliJ**, el comando de tu servicio:


| Servicio                                      | Comando                                                                                                                                  |
| --------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------- |
| Autenticación                                 | `docker compose up -d --build ms-auth`                                                                                                   |
| Catálogo                                      | `docker compose up -d --build ms-catalogo`                                                                                               |
| Inventario                                    | `docker compose up -d --build ms-inventario`                                                                                             |
| Pedidos (carrito)                             | `docker compose up -d --build ms-pedidos`                                                                                                |
| Pedidos (probar el checkout)                  | `docker compose up -d --build ms-pedidos ms-catalogo ms-inventario ms-comprobantes` *(necesita en tu* `.env` *las cuatro* `DB_PASS_…`*)* |
| Comprobantes                                  | `docker compose up -d --build ms-comprobantes`                                                                                           |
| Dos servicios (ej. inventario y comprobantes) | `docker compose up -d --build ms-inventario ms-comprobantes`                                                                             |


- La **primera vez tarda varios minutos** (descarga y compila). Al final debe decir `Started` o `Healthy` para cada contenedor.
- **No uses** `docker compose up -d` **a secas**: levanta los 8 servicios y gasta conexiones de todo el equipo. Solo lo usa Mathias para la integración.

---



## 6. Seguimiento: ¿está corriendo de verdad?



### 6.1 Ver el estado

🖥️ **Terminal de IntelliJ**:

```powershell
docker compose ps
```

Mira la columna **STATUS** (y compárala con **CREATED**):


| STATUS                                                                                          | Qué significa                                                                                           | Qué hacer                             |
| ----------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------- | ------------------------------------- |
| `Up 20 seconds (health: starting)`                                                              | Está arrancando. Tu servicio espera a que Eureka esté listo, así que puede tardar **hasta 1–2 minutos** | Esperar y repetir `docker compose ps` |
| `Up 2 minutes (healthy)`                                                                        | **Funciona** ✅                                                                                          | Seguir con 6.2                        |
| `Up 5 seconds` cuando CREATED dice `3 minutes ago`, y al repetir el número **vuelve a empezar** | **Se está cayendo y reiniciando en bucle** ❌                                                            | Ver el error con 6.3                  |
| `Restarting` o `Exited`                                                                         | Se cayó ❌                                                                                               | Ver el error con 6.3                  |
| `(unhealthy)`                                                                                   | Arrancó pero no responde bien ❌                                                                         | Ver el error con 6.3                  |
| No aparece                                                                                      | No se levantó                                                                                           | Repetir el comando de 5.2             |




### 6.2 Probar que responde

🖥️ **Terminal de IntelliJ** (en PowerShell se escribe `curl.exe`, no `curl`):


| Servicio      | Comando                                          | Respuesta correcta                               |
| ------------- | ------------------------------------------------ | ------------------------------------------------ |
| Autenticación | `curl.exe http://localhost:8081/actuator/health` | `{"status":"UP"}`                                |
| Catálogo      | `curl.exe http://localhost:8082/productos`       | La lista de productos                            |
| Inventario    | `curl.exe http://localhost:8083/stock/1`         | `{"idProducto":1,"disponible":10,"reservado":0}` |
| Pedidos       | `curl.exe http://localhost:8084/pedidos`         | `[]`                                             |
| Comprobantes  | `curl.exe http://localhost:8085/comprobantes`    | `[]`                                             |


🌐 **Navegador**: también puedes abrir esas mismas direcciones.

🌐 **Navegador**: **[http://localhost:8761](http://localhost:8761)** (Eureka). En la tabla de arriba, **"Instances currently registered with Eureka"**, debe aparecer tu servicio en mayúsculas (por ejemplo `MS-CATALOGO`) con estado **UP**. Tarda unos 30 segundos en aparecer.

- Lo de más abajo ("General Info", `unavailable-replicas`) es normal: no indica ningún error.
- El gateway **no** hace falta para que tu servicio aparezca.



### 6.3 Ver el error cuando algo falla

🖥️ **Terminal de IntelliJ**:

```powershell
docker compose logs ms-catalogo --tail 50
```

(Cambia `ms-catalogo` por tu servicio.) Busca líneas con `ERROR`, `Exception` o `Caused by` y compáralas con la sección [12](#12-problemas-típicos). Si no lo resuelves, manda esas líneas a Mathias.

Para ver el log **en vivo** mientras pruebas: `docker compose logs -f ms-catalogo` (se sale con `Ctrl+C`).

---



## 6.1 Probar el flujo completo (todos los servicios)

Con Eureka, el gateway y los cinco servicios arriba (`docker compose up -d`) y un administrador en Aiven
(`ADMIN_CORREO` y `ADMIN_CLAVE` en el `.env` antes de levantar ms-auth):

```bash
set -a; . ./.env; set +a
python3 -I pruebas/prueba-e2e.py          # 52 verificaciones: catálogo, registro, perfil, seguridad, panel, carrito, checkout, comprobantes y PDF
docker compose stop ms-comprobantes
python3 -I pruebas/prueba-e2e.py caida    # el checkout responde 503, libera el stock y anula la venta
docker compose start ms-comprobantes
python3 -I pruebas/prueba-e2e.py web      # llamadas del panel con todo el catálogo y limpieza de los datos de prueba
```

Para probarlo a través de la web, agrega `GW=http://localhost:5173/api` con `npm run dev` corriendo en `DSW_II_PROYECTO_WEB`.
El script crea un producto y dos clientes de prueba (`e2e.*@intiguard.pe`); la fase `web` los desactiva.

## 7. El día a día con Docker



### ¿Con `--build` o sin `--build`?


| Situación                                                           | Comando                                                                       |
| ------------------------------------------------------------------- | ----------------------------------------------------------------------------- |
| **Primera vez**                                                     | `docker compose up -d --build ms-<servicio>`                                  |
| Después de `git pull` (llegó código nuevo)                          | `docker compose up -d --build ms-<servicio>`                                  |
| Después de **cambiar código** de tu servicio o de `common-security` | `docker compose up -d --build ms-<servicio>`                                  |
| Solo **volver a encenderlo** (no cambió nada)                       | `docker compose up -d ms-<servicio>` *(sin* `--build`*, arranca en segundos)* |
| Cambiaste algo en tu `.env`                                         | `docker compose up -d ms-<servicio>` *(lo recrea con los valores nuevos)*     |


**Regla simple:** si cambió el **código**, va `--build`. Si no, no hace falta.

### Al terminar el día

🖥️ **Terminal de IntelliJ**:

```powershell
docker compose down
```

Libera las conexiones de la base compartida. **No dejes servicios corriendo sin usarlos.**

### Después de reiniciar la PC

1. Abre **Docker Desktop** y espera a que diga *running*.
2. 🖥️ **Terminal de IntelliJ**: `docker compose ps`
  - Si no hiciste `down` antes de apagar, tus contenedores **vuelven a arrancar solos**: espera a que queden `(healthy)`.
  - Si no aparece nada: `docker compose up -d ms-<servicio>` (sin `--build`).
3. Comprueba con el `curl.exe` de la sección 6.2.



### Traer lo nuevo del equipo

🖥️ **Terminal de IntelliJ**, al empezar cada día:

```powershell
git pull origin develop
docker compose up -d --build ms-<servicio>
```



### Correr tu servicio desde IntelliJ (para depurar)

Más rápido para probar cambios y poner puntos de parada:

1. 🖥️ **Terminal de IntelliJ**:
  ```powershell
   docker compose up -d ms-discovery
   docker compose stop ms-catalogo
  ```
   (El segundo libera el puerto si tu servicio estaba en Docker.)
2. 🧩 **IntelliJ**: abre la clase principal de tu servicio (por ejemplo `ms-catalogo/src/main/java/pe/intiguard/catalogo/CatalogoApplication.java`) y pulsa el **▶ verde** al lado de `public class` → **Run** (o **Debug**). Lee solo el `.env`.
3. Si IntelliJ marca errores de `pe.intiguard.security` (no encuentra `common-security`), 🖥️ **Terminal de IntelliJ**: `.\mvnw.cmd -q install -DskipTests` y vuelve a correrlo.

> En la terminal de Windows el Maven del proyecto se llama `.\mvnw.cmd` (no `./mvnw`).

---



## 8. Ver tus tablas con DBeaver

🐬 **DBeaver**:

1. **Base de datos → Nueva conexión** (o el ícono del enchufe con `+`).
2. Elige **MySQL** (el ícono del delfín). Si te muestra varias opciones, elige la que dice solo **MySQL**: no "MySQL 5 (Legacy)", ni "Old", ni "Cluster". → **Siguiente**.
3. Pestaña **Principal (Main)**. Selecciona **Conectar por: Host** y completa:

  | Campo                  | Valor                                                                                                                   |
  | ---------------------- | ----------------------------------------------------------------------------------------------------------------------- |
  | **Server Host**        | `mysql-2ab2661d-jmalayo-7c9a.f.aivencloud.com` — el `AIVEN_HOST` de tu `.env`, **sin** `http://` y **sin** `/` al final |
  | **Port**               | `10751`                                                                                                                 |
  | **Database**           | `db_<tu servicio>`, por ejemplo `db_catalogo` (**guion bajo**)                                                          |
  | **Username**           | `ms_<tu servicio>`, por ejemplo `ms_catalogo`                                                                           |
  | **Password**           | La `DB_PASS_…` de tu `.env` (copiar y pegar, sin espacios)                                                              |
  | **Show all databases** | **Desmarcado**                                                                                                          |

   El campo **URL** de arriba debe quedar así: `jdbc:mysql://mysql-2ab2661d-jmalayo-7c9a.f.aivencloud.com:10751/db_catalogo`
4. Pestaña **SSL**:
  - Marca **Use SSL** y **Require SSL**.
  - En **CA Certificate**, elige el archivo `ca.pem` que te mandaron por WhatsApp (ver abajo dónde guardarlo).
  - Marca **Verify server certificate**.
    > **Dónde guardar** `ca.pem`**:** 📁 muévelo de **Descargas** a una carpeta fija, por ejemplo **Documentos**, y elígelo desde ahí. DBeaver lo usa cada vez que se conecta: si lo borras o lo mueves, la conexión deja de funcionar. **No lo pegues dentro del proyecto.**
5. Si DBeaver pide **descargar el driver**, acepta (**Download**).
6. **Probar conexión (Test Connection)** → debe decir **Conectado**. → **Finalizar**.
7. En el panel izquierdo: tu conexión → `db_<tu servicio>` → **Tablas**. Doble clic en una tabla → pestaña **Datos**.

Si **Test Connection** falla, busca el mensaje en la sección [12](#12-problemas-típicos). Si después de varios intentos no conecta, avisa a Mathias.

> Tu usuario solo puede **ver y modificar datos** de tu base. No puede crear, cambiar ni borrar tablas, ni ver las bases de otros servicios. Si necesitas cambiar una tabla, sigue la regla de la sección 9.

---



## 9. Reglas que no se negocian

**Credenciales**

- El `.env` **nunca se sube** (Git ya lo ignora) ni se comparte: ni en el grupo, ni en capturas, ni en pantallas compartidas.
- Cada uno tiene solo la contraseña de su servicio. Si necesitas otra, pídesela a Mathias.

**Base de datos compartida (Aiven)**

- Hay **una sola base para los cinco**, con un máximo de **46 conexiones**. Levanta solo tu servicio y apágalo con `docker compose down` al terminar.
- No borres datos de prueba de otros.
- **Para cambiar una tabla o agregar datos de prueba:** no tienes acceso a `infra/`. Pídeselo a Mathias explicando qué tabla, qué columna o qué datos necesitas y para qué. Él actualiza `infra/` y aplica el cambio en la base. Nadie recrea la base.

**Git**

- Trabajas en **tu rama** `b-<apellido>`.
- Commits en español con el servicio entre paréntesis: `feat(catalogo): filtros por marca y categoría`.
- Antes de empezar cada día: `git pull origin develop`.
- Para entregar: `git push` y abre un **Pull Request de tu rama hacia** `develop` en GitHub. Otro integrante lo revisa antes de entrar.
- Toca solo tu carpeta y tus `.sql`. `docker-compose.yml`, el `pom.xml` raíz, `.github/` y el gateway los edita solo Mathias: si necesitas un cambio, pídelo.
- **Si cambias un endpoint o un DTO que otro servicio usa, avisa a su dueño antes** (ver la sección 10).

---



## 10. Quién le debe a quién

Cada servicio es de una persona, pero varios necesitan endpoints de otros para avanzar. Estos son los **contratos**: lo que cada uno se compromete a entregar, con su forma exacta y la semana límite. El detalle (cuerpos, respuestas, códigos HTTP) está en la sección "Dependencias con otros servicios" del README del servicio que lo entrega.

```
                 C1 formato JWT (S2) · C2 filtro + UsuarioActual (S3) · C3 login (S3)
  Autenticación ────────────────────────────────────────────────────────▶ todos

  Gateway ────── C4 gateway valida JWT y bloquea rutas internas (S3) ───▶ todos

  Catálogo ───── C5 GET /productos/{id} con "activo" (S2) ─────────────▶ pedidos, inventario
           ───── C9 200 productos (S5) ───────────────────────────────▶ Mathias (carga en Aiven)

  Inventario ─── C6 reservar / confirmar / liberar (fin S3) ───────────▶ pedidos
  Comprobantes ─ C7 POST /comprobantes (fin S3) · GET /comprobantes/venta/{idVenta} (S4) ▶ pedidos

  Pedidos ────── C8 idVenta real + token reenviado + liberar ante fallo (S4) ──▶ inventario, comprobantes
```


| #   | Contrato                                                                                                                       | Lo entrega    | Lo necesita                                 | Semana       | Estado | README con el detalle        |
| --- | ------------------------------------------------------------------------------------------------------------------------------ | ------------- | ------------------------------------------- | ------------ | ------ | ---------------------------- |
| C1  | El JWT trae `sub` (correo), `rol` e `idUsuario`                                                                                | Autenticación | Todos                                       | **2**        | ✅      | `ms-auth`, `common-security` |
| C2  | `JwtAuthFilter` (401/403 en JSON) y `UsuarioActual.id()/correo()/rol()`                                                        | Autenticación | Catálogo, inventario, pedidos, comprobantes | 3            | ✅      | `common-security`            |
| C3  | `POST /auth/login` → `{token, tipo, expiraEnMinutos, rol}` · 401 · 423                                                         | Autenticación | Gateway, web-client                         | 3            | ✅      | `ms-auth`                    |
| C4  | Gateway: rutas públicas, 401 sin token, 403 en rutas internas                                                                  | Gateway       | Todos                                       | 3            | ✅      | `ms-gateway`                 |
| C5  | `GET /productos/{id}` con `activo`; 404 si no existe                                                                           | Catálogo      | Pedidos, inventario                         | **2**        | ✅      | `ms-catalogo`                |
| C6  | `POST /stock/reservar` (todo o nada, 409), `/confirmar` y `/liberar` por `idVenta`                                             | Inventario    | Pedidos                                     | fin de 3     | ✅      | `ms-inventario`              |
| C7  | `POST /comprobantes` (idempotente por `idVenta`, < 2 s, 400 si el documento es inválido) y `GET /comprobantes/venta/{idVenta}` | Comprobantes  | Pedidos, web-client                         | fin de 3 / 4 | ✅      | `ms-comprobantes`            |
| C8  | Venta PENDIENTE antes de reservar, token reenviado por Feign, `liberar` ante cualquier fallo                                   | Pedidos       | Inventario, comprobantes                    | 4            | ✅      | `ms-pedidos`                 |
| C9  | Lista de 200 productos (nombre, descripción, marca, categoría y precio, en una hoja de cálculo) para cargar en la base                                                                                | Catálogo      | Mathias                                     | 5            | ✅      | `ms-catalogo`                |


**Los dos caminos críticos:**

1. **Seguridad:** C1 a C4 están listos. Falta el **registro de clientes** (autenticación) para que haya usuarios además del admin; mientras tanto el sistema corre con `EXIGIR_TOKEN=false` (ver la sección 11).
2. **Checkout:** C5 + C6 + C7 → checkout de pedidos en la **semana 4**, la semana bisagra del cronograma. Si C6 o C7 se atrasan, se atrasa todo.

**Si vas a cambiar algo de un contrato** (un nombre de campo, un código HTTP, una ruta): avisa por el grupo, actualiza el README y espera el visto bueno de quien lo consume.

---



## 11. Cómo avanzar mientras esperas una dependencia

**Tokens para probar endpoints protegidos.** Hoy el `.env` trae `EXIGIR_TOKEN=false`: tus endpoints responden **sin token**. Si mandas uno, se valida y `UsuarioActual` sabe quién eres. Para conseguir un token sin depender de nadie:

- 📮 **Postman**: importa `postman/IntiGuard-seguridad.postman_collection.json`. La colección genera sola tokens de CLIENTE (`{{tokenCliente}}`, idUsuario 7) y de ADMIN (`{{tokenAdmin}}`, idUsuario 1). Úsalos en la pestaña **Authorization → Bearer Token**.

Las rutas internas (`POST /stock/reservar|confirmar|liberar`, `POST /comprobantes`) responden **403 si pasas por el gateway (8080)**: pruébalas directo en el puerto de tu servicio.


| Si eres de…                                  | Y te falta…                  | Haz esto mientras tanto                                                                                                                         |
| -------------------------------------------- | ---------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------- |
| Catálogo, inventario, pedidos o comprobantes | Usuarios reales en `db_auth` | Usa `@PreAuthorize` y `UsuarioActual` desde ya, y pruébalos con los tokens de Postman                                                           |
| Pedidos                                      | C6 y C7                      | Escribe `InventarioClient` y `ComprobantesClient` con los contratos de los READMEs y prueba el checkout con **Mockito**, simulando los clientes |
| Inventario o comprobantes                    | C8 (ventas reales)           | Prueba reservar y emitir con `idVenta` **inventados** desde Postman: `id_venta` no tiene relación forzada con pedidos, así que funciona         |


---



## 12. Problemas típicos



### Git e IntelliJ


| Síntoma                                                                               | Causa                                                 | Solución                                                                                                                                                                        |
| ------------------------------------------------------------------------------------- | ----------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `git clone` pide permisos o dice `Repository not found`                               | No eres colaborador del repositorio                   | Pide a Mathias que te agregue en GitHub                                                                                                                                         |
| Ventana **"Refactoring Preview"** con muchas "occurrences" al renombrar `env`         | Quedaron marcadas las casillas de búsqueda del Rename | **Cancelar** y repetir la sección 4.2 con las dos casillas desmarcadas. Luego 🖥️ `git status`: si aparece algún archivo modificado que no tocaste, `git checkout -- <archivo>` |
| `git status` muestra `docker-compose.yml` u otros archivos modificados que no tocaste | Se cambiaron por accidente                            | 🖥️ `git checkout -- <archivo>`                                                                                                                                                 |
| IntelliJ no reconoce `pe.intiguard.security`                                          | Falta instalar la librería común                      | 🖥️ `.\mvnw.cmd -q install -DskipTests`                                                                                                                                         |
| `./mvnw` no se reconoce                                                               | En Windows es otro archivo                            | Usa `.\mvnw.cmd`                                                                                                                                                                |




### `.env` y arranque del servicio

Primero mira el error con 🖥️ `docker compose logs ms-<servicio> --tail 50`.


| Síntoma en el log o en `docker compose ps`                        | Causa                                                             | Solución                                                                                                                    |
| ----------------------------------------------------------------- | ----------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------- |
| `JWT_SECRET … falta o tiene menos de 32 caracteres`               | `JWT_SECRET` corta, vacía o repetida                              | Déjala exactamente `JWT_SECRET=clave-local-solo-para-desarrollo-no-usar-en-nube` y 🖥️ `docker compose up -d ms-<servicio>` |
| `Access denied for user 'ms_catalogo'`                            | Contraseña mal copiada (espacio, comilla, otra variable)          | Copia de nuevo la línea `DB_PASS_…` de tu archivo original                                                                  |
| La URL de la base sale vacía (`jdbc:mysql://:/…`) o el host vacío | No hay `.env` en la raíz, o se llama distinto (`env`, `.env.txt`) | Secciones 4.1 a 4.3                                                                                                         |
| El servicio se reinicia en bucle (`Up` vuelve a pocos segundos)   | Casi siempre, uno de los tres de arriba                           | Ver el log y corregir                                                                                                       |
| `Too many connections`                                            | La base compartida llegó al límite                                | Apaga lo que no uses (`docker compose down`) y avisa por el grupo                                                           |
| `Communications link failure`                                     | Sin internet, o tu red bloquea el puerto 10751                    | Prueba otra red. Si sigue, avisa a Mathias (puede ser la lista de IPs permitidas en Aiven)                                  |




### Docker


| Síntoma                                                        | Causa                                                                                        | Solución                                                   |
| -------------------------------------------------------------- | -------------------------------------------------------------------------------------------- | ---------------------------------------------------------- |
| `Cannot connect to the Docker daemon` / `error during connect` | Docker Desktop cerrado                                                                       | Abrirlo y esperar a que diga *running*                     |
| Sigue apareciendo `intiguard-mysql` en `docker ps`             | Contenedor viejo de antes de Aiven                                                           | 🖥️ `docker rm -f intiguard-mysql`                         |
| `port is already allocated` (8082, 8761…)                      | Algo ya usa ese puerto (tu servicio en Docker y en IntelliJ a la vez, o un contenedor viejo) | 🖥️ `docker compose down` y vuelve a levantar solo lo tuyo |
| Cambié código pero el servicio sigue igual                     | Levantaste sin `--build`                                                                     | 🖥️ `docker compose up -d --build ms-<servicio>`           |
| `curl` responde algo raro en PowerShell                        | En PowerShell `curl` es otro comando                                                         | Usa `curl.exe`                                             |
| El gateway (8080) responde **503** recién levantado            | Todavía no recibió la lista de Eureka                                                        | Espera unos 30 s                                           |
| Mi servicio no aparece en Eureka                               | Lleva menos de 30 s, o no está `(healthy)`                                                   | 🖥️ `docker compose ps` (sección 6.1)                      |




### DBeaver


| Mensaje                                                         | Causa                                          | Solución                                                                                                                  |
| --------------------------------------------------------------- | ---------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------- |
| `Malformed database URL, failed to parse the main URL sections` | El host empieza con `http://` o termina en `/` | Server Host solo con el nombre: `mysql-2ab2661d-jmalayo-7c9a.f.aivencloud.com`                                            |
| `Access denied for user 'ms_…' to database 'db-…'`              | La base se escribió con **guion medio**        | Database: `db_<servicio>` con **guion bajo**                                                                              |
| `Access denied for user 'ms_…' (using password: YES)`           | Contraseña o usuario incorrectos               | Usuario `ms_<servicio>`; contraseña copiada de tu `.env`                                                                  |
| Errores de permisos al abrir la conexión                        | **Show all databases** marcado                 | Desmarcarlo                                                                                                               |
| `Public Key Retrieval is not allowed`                           | Falta una opción del driver                    | Pestaña **Driver properties** → `allowPublicKeyRetrieval` = `true`                                                        |
| Errores de SSL o certificado                                    | Falta el CA o se eligió otro archivo           | Pestaña SSL: CA Certificate = el `ca.pem` que te mandaron por WhatsApp. Si no lo tienes o lo borraste, pídeselo a Mathias |
| `Communications link failure`                                   | Red o firewall                                 | Ver la misma fila en la tabla de `.env` y arranque                                                                        |




### Seguridad y tokens


| Respuesta                                           | Causa                                                                 | Solución                                                                                 |
| --------------------------------------------------- | --------------------------------------------------------------------- | ---------------------------------------------------------------------------------------- |
| **401** `"Token inválido o vencido"`                | El token venció (dura 60 min) o se firmó con otra `JWT_SECRET`        | Genera otro (Postman lo hace solo en cada envío). Todos deben usar la misma `JWT_SECRET` |
| **401** en un endpoint con `@PreAuthorize`          | Pide un rol y no mandaste token                                       | Manda `{{tokenAdmin}}` o `{{tokenCliente}}`                                              |
| **403** `"Ruta de uso interno entre servicios"`     | Llamaste a reservar, confirmar, liberar o emitir a través del gateway | Es a propósito: pruébalas directo en el puerto del servicio (8083, 8085)                 |
| Todo responde **401** con `WWW-Authenticate: Basic` | Se rompió la configuración de `common-security`                       | Ver "Ojo con el orden" en `common-security/README.md`                                    |


Si nada de esto aplica, manda a Mathias la salida de 🖥️ `docker compose logs ms-<servicio> --tail 50`.

---



## 13. Dónde está cada cosa


| Necesito…                                                       | Dónde                                                                                |
| --------------------------------------------------------------- | ------------------------------------------------------------------------------------ |
| Mis tareas, modelo, reglas de negocio, endpoints y dependencias | `<mi-servicio>/README.md`                                                            |
| Arquitectura, stack y estado general                            | `README.md`                                                                          |
| El plan completo y el cronograma de 8 semanas                   | `docs/Plan de ejecución — IntiGuard v2.md`                                           |
| La estructura de mis tablas | El README de mi servicio (sección "Modelo de datos") o DBeaver (sección 8) |
| Mis datos de prueba | DBeaver (sección 8). Para agregar más, pedírselo a Mathias |
| El certificado para DBeaver                                     | El `ca.pem` que me mandó Mathias por WhatsApp (guardado en Documentos)               |
| Mis credenciales                                                | Mi `.env` en la raíz del proyecto (el archivo base me lo mandó Mathias por WhatsApp) |
| Probar endpoints y conseguir tokens                             | `postman/`                                                                           |


