# Despliegue en Render (plan gratuito)

Cómo publicar el backend (6 servicios) y la web en Render, sin costo. La base de datos sigue siendo el MySQL de Aiven.

## Qué se crea

| Servicio en Render | Tipo | Qué es | URL |
|---|---|---|---|
| `intiguard-gateway` | Web Service (Docker) | La única URL que usa la web | `https://intiguard-gateway.onrender.com` |
| `intiguard-auth` | Web Service (Docker) | ms-auth | `https://intiguard-auth.onrender.com` |
| `intiguard-catalogo` | Web Service (Docker) | ms-catalogo | `https://intiguard-catalogo.onrender.com` |
| `intiguard-inventario` | Web Service (Docker) | ms-inventario | `https://intiguard-inventario.onrender.com` |
| `intiguard-pedidos` | Web Service (Docker) | ms-pedidos | `https://intiguard-pedidos.onrender.com` |
| `intiguard-comprobantes` | Web Service (Docker) | ms-comprobantes | `https://intiguard-comprobantes.onrender.com` |
| `intiward-web` | Static Site | La app React (repo `DSW_II_PROYECTO_WEB`) | `https://intiward-web.onrender.com` |

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
