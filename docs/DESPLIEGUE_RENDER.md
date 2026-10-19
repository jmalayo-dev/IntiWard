# Despliegue en Render (plan gratuito)

Cómo publicar el backend (6 servicios) y la web en Render, sin costo. La base de datos sigue siendo el MySQL de Aiven.

> **Publicado el 18–19/10/2026** y verificado con la prueba completa (52 + 14 verificaciones en verde contra Render).
> Lo aprendido en el despliegue real está en [Operación](#operación): por qué aparece un 502, cómo saber si un servicio
> está dormido y cómo desplegar con Render CLI.

## Qué se crea

| Servicio en Render | Tipo | Qué es | URL |
|---|---|---|---|
| `intiguard-gateway` | Web Service (Docker) | La única URL que usa la web | `https://intiguard-gateway.onrender.com` |
| `intiguard-auth` | Web Service (Docker) | ms-auth | `https://intiguard-auth.onrender.com` |
| `intiguard-catalogo` | Web Service (Docker) | ms-catalogo | `https://intiguard-catalogo.onrender.com` |
| `intiguard-inventario` | Web Service (Docker) | ms-inventario | `https://intiguard-inventario.onrender.com` |
| `intiguard-pedidos` | Web Service (Docker) | ms-pedidos | `https://intiguard-pedidos.onrender.com` |
| `intiguard-comprobantes` | Web Service (Docker) | ms-comprobantes | `https://intiguard-comprobantes.onrender.com` |
| `IntiWard_Web` | Static Site | La app React (repo `DSW_II_PROYECTO_WEB`), creada a mano en el Dashboard | `https://intiward-web.onrender.com` |

**Por qué así:**

- **Web Service con Docker:** cada microservicio ya tiene su `Dockerfile`. Render lo construye desde la raíz del repo.
- **Sin Eureka:** en el plan gratuito los servicios **no pueden recibir tráfico de la red privada** de Render, así que se
  llaman entre sí por su URL pública (`MS_*_URL`). Con `EUREKA_ENABLED=false`, el gateway y los clientes Feign usan esas
  URLs en vez de preguntarle a Eureka. En local todo sigue igual que antes (con Eureka).
- **Rutas internas protegidas:** como cada servicio tiene URL pública, `POST /stock/reservar`, `/confirmar`, `/liberar` y
  `POST /comprobantes` exigen la cabecera `X-Clave-Interna` con `CLAVE_INTERNA`. Solo ms-pedidos la conoce. Sin ella: 403.
- **Static Site para la web:** gratis y no se duerme.

**Límites del plan gratuito** ([render.com/docs/free](https://render.com/docs/free)):

- Cada servicio **se duerme tras 15 minutos sin tráfico** y tarda **~1 minuto** en despertar. Antes de una demo, despiértalos (paso 7).
- **750 horas al mes** compartidas entre todos los servicios gratuitos. Un servicio dormido no gasta horas. Con 6 servicios
  despiertos a la vez alcanza para unas 125 horas de uso al mes: suficiente para desarrollo y demos, no para dejarlo 24/7.
- 512 MB por servicio. Cada uno usa ~250–300 MB (medido con `-Xmx256m`).

## Paso 0 · Que `main` tenga el código

El Blueprint despliega la rama **`main`**. Antes de empezar, `develop` tiene que estar mergeada en `main`
(y cada vez que quieras actualizar lo publicado, vuelve a mergear: Render redespliega solo).

## Paso 1 · Región y Aiven

1. En la consola de Aiven, mira en qué **región** está el servicio MySQL (por ejemplo `google-us-east1` o `aws-us-east-1`).
2. `render.yaml` ya usa `region: virginia`: la base de Aiven está en DigitalOcean, Nueva York/Nueva Jersey (este de EE. UU.).
   Si algún día mueves la base, cambia la región de los 6 servicios (`oregon`, `ohio`, `virginia`, `frankfurt` o
   `singapore`) antes de crearlos. **La región no se puede cambiar después.** Cada consulta cruza a Aiven, así que la
   cercanía se nota en el checkout.
3. En Aiven → tu servicio MySQL → **Allowed IP addresses**: si no está abierto (`0.0.0.0/0`), agrega las IP de salida de
   Render (Dashboard de Render → cualquier servicio → **Connect → Outbound**).

## Paso 2 · Crear el Blueprint

1. En [dashboard.render.com](https://dashboard.render.com): **New → Blueprint**.
2. Conecta GitHub y elige el repositorio del backend (`jmalayo-dev/IntiWard`). Render encuentra `render.yaml`.
3. Te pedirá los valores marcados como secretos (no están en el repo). Sácalos de tu `.env`:

| Variable que pide Render | Servicios | Valor |
|---|---|---|
| `DB_HOST` | auth, catalogo, inventario, pedidos, comprobantes | el valor de `AIVEN_HOST` |
| `DB_PORT` | los mismos 5 | el valor de `AIVEN_PORT` |
| `DB_PASSWORD` | `intiguard-auth` | `DB_PASS_AUTH` |
| `DB_PASSWORD` | `intiguard-catalogo` | `DB_PASS_CATALOGO` |
| `DB_PASSWORD` | `intiguard-inventario` | `DB_PASS_INVENTARIO` |
| `DB_PASSWORD` | `intiguard-pedidos` | `DB_PASS_PEDIDOS` |
| `DB_PASSWORD` | `intiguard-comprobantes` | `DB_PASS_COMPROBANTES` |
| `ADMIN_CORREO` · `ADMIN_CLAVE` | `intiguard-auth` | El administrador del panel. Si ese correo no existe en Aiven, se crea al arrancar |

4. **Apply**. Render construye los 6 (unos 2–4 minutos cada uno).

## Paso 3 · Variables que ya vienen en `render.yaml`

No hay que tocarlas, salvo que algo cambie. Están en el grupo **`intiguard-comun`** (Dashboard → Env Groups), que comparten los 6:

| Variable | Valor | Para qué |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `cloud` | Perfil de nube: el token es obligatorio y las credenciales salen de las variables |
| `EUREKA_ENABLED` | `false` | Sin Eureka: se usan las `MS_*_URL` |
| `JWT_SECRET` | generada por Render | Firma y valida el token; **la misma en los 6** |
| `CLAVE_INTERNA` | generada por Render | Cabecera `X-Clave-Interna` de las rutas internas |
| `MS_AUTH_URL` … `MS_COMPROBANTES_URL` | `https://intiguard-*.onrender.com` | A dónde enruta el gateway y a dónde llama cada cliente Feign |
| `CORS_ORIGENES` | `https://intiward-web.onrender.com,http://localhost:5173` | Dominios que pueden llamar al gateway desde el navegador |
| `FEIGN_CONNECT_TIMEOUT_MS` · `FEIGN_READ_TIMEOUT_MS` | `10000` · `70000` | Un servicio dormido tarda ~1 min en despertar: las llamadas entre servicios lo esperan |
| `JAVA_OPTS` | `-Xmx256m … -Duser.timezone=America/Lima` | Memoria de la JVM para caber en 512 MB, y hora de Lima (Render corre en UTC) |

Y en cada servicio: `PORT` (el de siempre: 8080 a 8085), `DB_NAME` y `DB_USER`.

Opcional: `CHECKOUT_TIMEOUT_COMPROBANTE_MS` (por defecto `2000`), el tiempo máximo que el checkout espera a ms-comprobantes.

## Paso 4 · Revisar las URLs

Cuando terminen, revisa que cada servicio tenga exactamente la URL de la tabla de arriba. Si Render le dio otra a alguno
(por ejemplo `intiguard-auth-x1y2.onrender.com`), corrige la `MS_*_URL` correspondiente en el grupo `intiguard-comun`.
Render redespliega solo.

## Paso 5 · Comprobar que responde

```bash
curl https://intiguard-gateway.onrender.com/actuator/health          # {"status":"UP"}
curl "https://intiguard-gateway.onrender.com/productos?size=2"        # dos productos
```

Si un servicio no arranca, revisa sus **Logs** en Render. Los errores típicos:

| En el log | Causa |
|---|---|
| `JWT_SECRET … menos de 32 caracteres` | La variable falta o es corta en el grupo |
| `Communications link failure` / `Access denied` | `DB_HOST`, `DB_PORT` o `DB_PASSWORD` mal copiados, o Aiven no deja entrar la IP de Render (paso 1) |
| `Could not resolve placeholder 'ADMIN_CORREO'` | Falta `ADMIN_CORREO` o `ADMIN_CLAVE` en `intiguard-auth` |
| `Schema-validation: missing column` | Aiven no tiene el esquema al día |

## Paso 6 · La web

1. En el repositorio `DSW_II_PROYECTO_WEB`, mergea a `main` (ya trae `render.yaml` y `.env.production`).
2. Render: **New → Blueprint** → ese repositorio. Crea el Static Site `intiward-web`.
3. La web llama a `https://intiguard-gateway.onrender.com` (definido en `.env.production`). Si la URL del gateway o de la
   web es otra, corrige `.env.production` en la web y `CORS_ORIGENES` en el backend.

## Paso 7 · Antes de una demo

```bash
bash pruebas/despertar-render.sh      # despierta los 6 y espera a que respondan (~1–2 min)
```

Y la prueba completa contra lo publicado (crea datos de prueba; la fase `web` los desactiva al final):

```bash
GW=https://intiguard-gateway.onrender.com ADMIN_CORREO=... ADMIN_CLAVE=... python3 -I pruebas/prueba-e2e.py
GW=https://intiguard-gateway.onrender.com ADMIN_CORREO=... ADMIN_CLAVE=... python3 -I pruebas/prueba-e2e.py web
```

## Operación

### Ids de los servicios (Render CLI)

| Servicio | Id |
|---|---|
| intiguard-gateway | `srv-dbaprbc9v7es739fiseg` |
| intiguard-auth | `srv-dbaprbc9v7es739fism0` |
| intiguard-catalogo | `srv-dbaprbc9v7es739fisg0` |
| intiguard-inventario | `srv-dbaprbc9v7es739fisf0` |
| intiguard-pedidos | `srv-dbaprbc9v7es739fisfg` |
| intiguard-comprobantes | `srv-dbaprbc9v7es739fise0` |
| IntiWard_Web | `srv-db9so3m1egvs738g9560` |

Render CLI (`render`, v2.30) se instala en `~/.local/bin`. Primera vez: `render login` y `render workspace set`.

```bash
render services                                        # los 7 servicios
render deploys list <id> -o json                       # qué commit está desplegado
render deploys create <id> --commit <sha> --wait       # desplegar a mano (si el auto-deploy no se dispara)
render logs -r <id> --limit 50                         # logs
render services instances <id> -o json                 # [] = dormido · [{…}] = despierto
```

### Dormido, despierto y el 502

- Un servicio gratuito **se duerme tras 15 min sin tráfico de fuera de Render**. No es un tiempo fijo: con tráfico sigue despierto.
- **El gateway no despierta a los demás.** Las llamadas entre servicios de Render no cuentan como tráfico de fuera: si
  ms-catalogo está dormido, el gateway recibe al instante la página de error 502 de Render y la web muestra el error.
  Comprobado el 19/10/2026: el catálogo solo arrancó al llamarlo directo desde fuera.
- **Despertarlos:** cualquier petición desde fuera a cada servicio (`curl https://intiguard-<servicio>.onrender.com/actuator/health`),
  o los 6 a la vez con `bash pruebas/despertar-render.sh`. Tardan ~1–1,5 min. `render restart` no sirve para esto.
- **Mantenerlos despiertos durante un uso largo:** el contenedor de `DSW_II_PROYECTO_PING_RENDER` llama a los 6 cada
  10 min, y al bajarlo los suspende (API de Render). Gasta 6 de las 750 horas del mes por cada hora prendido.

### Desplegar cambios

- **Backend:** merge a `main`; cada servicio se redespliega solo si cambió su carpeta (`buildFilter`).
- **Variables del grupo `intiguard-comun`:** cambiarlas en `render.yaml` **no** se aplica solo. Hace falta
  Dashboard → Blueprints → IntiWard → **Manual Sync**, o editarlas en Environment Groups.
- **Web:** push a `main` del repo de la web. El 19/10/2026 el auto-deploy no se disparó una vez; si pasa,
  `render deploys create srv-db9so3m1egvs738g9560 --commit <sha> --wait`.
- **Caché del CDN:** la web publicada puede mostrar la versión anterior hasta 5 min después del despliegue
  (`s-maxage=300`). Recargar con `Ctrl+Shift+R`.

### Datos

- Base compartida entre local y Render (Aiven). El 19/10/2026 se borraron los 8 productos iniciales sin imagen y todos
  los datos de prueba (ventas, comprobantes, reservas y movimientos); el respaldo está en `infra/respaldos/` (fuera de Git).
  La numeración de comprobantes volvió a empezar en `B001-00000001` / `F001-00000001`.
- `pruebas/prueba-e2e.py` crea datos de prueba cada vez que corre (un producto, dos clientes y ventas); la fase `web`
  desactiva el producto y los clientes, pero las ventas quedan.
