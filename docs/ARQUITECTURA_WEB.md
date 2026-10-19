# Arquitectura del cliente web · React + Vite (JavaScript) con FSD

Este documento define **cómo se construye la base del cliente web**: tecnologías, estructura de carpetas con **Feature-Sliced Design (FSD)**, dónde viven los **endpoints** del backend, dónde viven los **mocks** y cómo se pasa de los mocks al backend real, servicio por servicio.

No trata el diseño visual (eso es [`PROTOTIPO_WEB.md`](PROTOTIPO_WEB.md)) ni las reglas del negocio (eso es la sección "Guía de negocio y casos de uso" del [`README` del backend](../../DSW_II_PROYECTO/README.md#guía-de-negocio-y-casos-de-uso)). Aquí solo está **la arquitectura**.

> **Dónde vive:** el cliente web es su propio proyecto, `DSW_II_PROYECTO_WEB/` (la raíz es la app de Vite, no una carpeta `web/` dentro del backend). Este documento y `PROTOTIPO_WEB.md` viven en su carpeta `docs/` y describen lo que está construido. **Estado (17/10/2026):** las 24 vistas están hechas y funcionan contra los mocks; falta conectar el backend real (paso 8 de la sección 11).

---

## 1. Decisiones

| Tema | Decisión | Por qué |
|---|---|---|
| Lenguaje | **JavaScript** (`.js` / `.jsx`), sin TypeScript. Tipos documentados con JSDoc donde ayude | Pedido del equipo |
| Empaquetador | **Vite** con la plantilla `react` | Arranque instantáneo, proxy de desarrollo integrado |
| Arquitectura | **Feature-Sliced Design** (capas `app → pages → widgets → features → entities → shared`) | Cada cosa tiene un lugar fijo y las dependencias van en un solo sentido |
| Rutas del navegador | `react-router-dom` (`createBrowserRouter`) | Rutas anidadas para los dos marcos (tienda y panel) y guardas por rol |
| Llamadas HTTP | `axios`, **una sola instancia** en `shared/api/http.js` | Un solo lugar para el token, la URL base y el formato de error |
| Datos del servidor | `@tanstack/react-query` | Caché, reintentos, estados de carga y error sin escribirlos a mano |
| Sesión | Contexto de React en `entities/sesion` + token en `localStorage` | El JWT ya trae `rol` e `idUsuario`: no hace falta otra librería |
| Mocks | **MSW** (Mock Service Worker) en `shared/mocks/` | Intercepta las peticiones reales en el navegador: el código no sabe si habla con un mock o con el gateway |
| Estilos | **CSS Modules** + variables de `tokens.css`; **Tailwind CSS v4** para componentes copiados de catálogos (Aceternity UI, shadcn/ui), sin su reset (Preflight) y en capas `@layer` | Las vistas propias siguen en CSS Modules; Tailwind permite pegar componentes externos sin traducirlos. Si chocan, gana el CSS Module (no tiene capa) |
| Puerta al backend | **Solo el gateway** (`:8080`), siempre con el prefijo `/api` | El navegador nunca habla con un microservicio directo |

Dependencias completas:

```
react, react-dom              (las trae la plantilla)
react-router-dom
axios
@tanstack/react-query
msw                           (solo desarrollo)
lottie-web                    (animación del 404; se usa la versión light y se descarga solo al mostrarla)
tailwindcss, @tailwindcss/vite (v4; configurado en src/app/styles/tailwind.css)
clsx, tailwind-merge          (helper cn de shadcn/ui, en shared/lib/cn.js)
```

No se agrega nada más sin acordarlo (Tailwind y lottie-web se acordaron el 17/10/2026): ni librerías de estado global, ni de formularios, ni de componentes. Los formularios son pocos y se resuelven con `useState` y los validadores de `shared/lib`.

---

## 2. Cómo conecta con el backend

```
Navegador (localhost:5173)
   │  fetch /api/productos?page=0
   ▼
┌───────────────────────────────┐
│ ¿Mocks activos para catálogo? │── sí ──▶ MSW responde desde shared/mocks (nunca sale del navegador)
└──────────────┬────────────────┘
               no
               ▼
Proxy de Vite: quita "/api" ──▶ http://localhost:8080/productos?page=0 ──▶ gateway ──▶ ms-catalogo
```

**Por qué el prefijo `/api`:** las rutas de la tienda y del backend chocan (`/productos/7` es una página **y** un endpoint). Con `/api` delante, todo lo que empieza con `/api` va al backend y lo demás es una página de React. El proxy quita el prefijo antes de llegar al gateway, así que **el backend no cambia nada**.

**CORS:** en desarrollo no hace falta, porque el proxy hace que el navegador vea un solo origen. En la nube se resuelve en el gateway (pendiente S3 de Mathias) o sirviendo la app y el gateway detrás del mismo dominio.

### Lo que manda el backend (y el front debe respetar)

| Tema | Formato |
|---|---|
| Token | `POST /auth/login` → `{ token, tipo: "Bearer", expiraEnMinutos: 60, rol }`. Se envía en cada petición como `Authorization: Bearer <token>` |
| Claims del JWT | `sub` (correo), `rol` (`ADMIN` / `CLIENTE`), `idUsuario`, `iat`, `exp` (segundos) |
| Error | Siempre `{ fecha, status, error, mensaje, ruta, detalles: [] }` (`ApiError` de cada servicio y `RespuestaErrorJson` de `common-security`) |
| Montos | Números JSON (`BigDecimal`): `199.9`. El formato `S/ 199.90` lo pone el front |
| Fechas | Texto ISO sin zona (`LocalDateTime`): `"2026-10-15T14:35:00"` |
| Rutas internas | `POST /stock/reservar`, `/stock/confirmar`, `/stock/liberar` y `POST /comprobantes` responden **403** desde afuera. El front **nunca** las llama |

---

## 3. Crear el proyecto

Requisito: Node 22 (`nvm use 22`; la terminal por defecto trae Node 18, con el que `npm create vite` falla). Así se creó el proyecto, junto a la carpeta del backend:

```bash
npm create vite@latest DSW_II_PROYECTO_WEB -- --template react
cd DSW_II_PROYECTO_WEB
npm install
npm install react-router-dom axios @tanstack/react-query
npm install -D msw
npx msw init public/ --save
```

- `--template react` es la plantilla **JavaScript** (la de TypeScript es `react-ts`; no usarla).
- `npx msw init` crea `public/mockServiceWorker.js`. Se sube al repo y no se edita.
- La plantilla trae su `.gitignore` con `node_modules`, `dist` y `*.local`.

Después de crear el proyecto:

1. Borrar `src/App.jsx`, `src/App.css`, `src/index.css`, `src/main.jsx` y `src/assets/`: se reemplazan por la estructura FSD de la sección 4.
2. En `index.html`, cambiar `/src/main.jsx` por `/src/app/main.jsx`.
3. Crear los archivos de configuración de la sección 5.

Comandos del día a día (desde la raíz de `DSW_II_PROYECTO_WEB/`):

| Comando | Para qué |
|---|---|
| `npm run dev` | Levanta la app en `http://localhost:5173` |
| `npm run build` | Genera `dist/` para desplegar |
| `npm run preview` | Sirve `dist/` para probar el build |
| `npm run lint` | oxlint (`.oxlintrc.json`) |

---

## 4. Estructura FSD

### Las capas

Cada capa **solo puede importar de las capas de abajo**. Nunca hacia arriba ni hacia los lados (un slice no importa a otro slice de su misma capa).

```
app        ↓  arranque: providers, router, guardas, estilos globales
pages      ↓  una carpeta por vista (las de PROTOTIPO_WEB.md)
widgets    ↓  bloques grandes y autónomos: encabezado, menú del panel, grilla de productos
features   ↓  una acción del usuario: iniciar sesión, agregar al carrito, confirmar compra
entities   ↓  conceptos del negocio: producto, venta, comprobante… (datos, consultas y su UI mínima)
shared        sin negocio: cliente HTTP, endpoints, mocks, utilidades, componentes base
```

| Pregunta | Capa |
|---|---|
| ¿Es una URL del navegador? | `pages` |
| ¿Se repite en varias páginas y combina varias cosas? | `widgets` |
| ¿Es un verbo que hace el usuario y cambia algo? | `features` |
| ¿Es un sustantivo del negocio (y cómo se lee del backend)? | `entities` |
| ¿Serviría igual en cualquier otro proyecto? | `shared` |

### Árbol completo

```
DSW_II_PROYECTO_WEB/
├─ docs/                          ARQUITECTURA_WEB.md (este) y PROTOTIPO_WEB.md
├─ index.html
├─ package.json
├─ vite.config.js                 alias @ y proxy /api → gateway
├─ jsconfig.json                  alias @ para que IntelliJ lo entienda
├─ .env.development               valores comunes de desarrollo (se sube)
├─ .env.development.local         valores personales (NO se sube)
├─ public/
│  ├─ mockServiceWorker.js        generado por msw init
│  ├─ productos/                  ilustraciones de producto (salen de los diseños de Stitch); las usa el mock como imagenUrl
│  └─ ilustraciones/hero.svg      imagen del hero del inicio
└─ src/
   ├─ app/
   │  ├─ main.jsx                 enciende los mocks (si corresponde) y monta la app
   │  ├─ App.jsx                  providers + router
   │  ├─ providers/
   │  │  ├─ QueryProvider.jsx
   │  │  └─ index.js
   │  ├─ router/
   │  │  ├─ router.jsx            todas las rutas
   │  │  └─ guardas.jsx           RequiereSesion, RequiereRol, SoloInvitado
   │  └─ styles/
   │     ├─ tokens.css            colores, tipografía y espacios de PROTOTIPO_WEB.md §1
   │     └─ global.css
   │
   ├─ pages/                      una carpeta por vista
   │  ├─ inicio/                  4.1
   │  ├─ catalogo/                4.2
   │  ├─ producto-detalle/        4.3
   │  ├─ login/                   4.4
   │  ├─ registro/                4.5
   │  ├─ carrito/                 4.6
   │  ├─ checkout/                4.7
   │  ├─ compra-confirmada/       4.8
   │  ├─ compra-no-completada/    4.9
   │  ├─ mis-pedidos/             4.10
   │  ├─ pedido-detalle/          4.11
   │  ├─ comprobante-imprimible/  4.12
   │  ├─ perfil/                  4.13
   │  ├─ error/                   4.14 (404 y 403)
   │  ├─ admin-resumen/           5.1
   │  ├─ admin-productos/         5.2
   │  ├─ admin-producto-form/     5.3
   │  ├─ admin-marcas-categorias/ 5.4
   │  ├─ admin-stock/             5.5
   │  ├─ admin-movimientos/       5.6
   │  ├─ admin-ventas/            5.7 (lista)
   │  ├─ admin-venta-detalle/     5.7 (detalle de una venta)
   │  ├─ admin-comprobantes/      5.8
   │  ├─ admin-reportes/          5.9
   │  └─ admin-usuarios/          5.10
   │
   ├─ widgets/
   │  ├─ tienda-layout/           marco de la tienda: encabezado (3 versiones) + barra de categorías + pie + <Outlet/>
   │  ├─ admin-layout/            marco del panel: menú lateral + barra superior + <Outlet/>
   │  ├─ grilla-productos/        tarjetas de producto + paginación + agregar al carrito
   │  ├─ resumen-carrito/         líneas del carrito + subtotal/IGV/total
   │  ├─ detalle-venta/           línea de tiempo, tabla de productos y tarjeta del comprobante (cliente y admin)
   │  ├─ tabla-movimientos/       historial de stock con filtros
   │  ├─ hoja-comprobante/        hoja A4 del comprobante (vista 4.12 y panel lateral de la 5.8)
   │  └─ pantalla-error/          404, 403, sesión expirada y sin conexión (vista 4.14)
   │
   ├─ features/
   │  ├─ login/
   │  ├─ registro/
   │  ├─ logout/
   │  ├─ filtrar-catalogo/        categoría, marca y búsqueda en la URL
   │  ├─ buscar-productos/        buscador con sugerencias y búsquedas recientes
   │  ├─ agregar-al-carrito/
   │  ├─ cambiar-cantidad-carrito/
   │  ├─ quitar-del-carrito/
   │  ├─ confirmar-compra/        formulario boleta/factura + checkout
   │  ├─ editar-perfil/
   │  ├─ descargar-comprobante/
   │  ├─ guardar-producto/        crear y editar (admin)
   │  ├─ desactivar-producto/
   │  ├─ guardar-marca-categoria/
   │  ├─ registrar-entrada-stock/
   │  ├─ ajustar-stock/
   │  ├─ filtrar-reporte/
   │  ├─ guardar-usuario/
   │  └─ desactivar-usuario/
   │
   ├─ entities/
   │  ├─ sesion/                  token, usuario actual, rol (contexto de React)
   │  ├─ usuario/
   │  ├─ producto/
   │  ├─ marca/
   │  ├─ categoria/
   │  ├─ stock/
   │  ├─ movimiento/
   │  ├─ carrito/
   │  ├─ venta/
   │  └─ comprobante/
   │
   └─ shared/
      ├─ api/
      │  ├─ endpoints.js          ★ catálogo único de rutas del backend
      │  ├─ http.js               instancia de axios: URL base, token, errores
      │  ├─ token.js              guardar / leer / borrar el JWT
      │  ├─ errores.js            clase ApiError del front
      │  ├─ paginacion.js         normaliza las páginas de Spring
      │  └─ index.js
      ├─ config/
      │  ├─ env.js                lee import.meta.env en un solo lugar
      │  └─ rutas.js              ★ rutas del navegador (las URLs de las páginas)
      ├─ lib/
      │  ├─ formato.js            moneda S/, fechas es-PE, número de pedido #00042
      │  ├─ fechas.js             fechas ISO locales, "15 de octubre de 2026", "Hoy 14:30"
      │  ├─ numeroALetras.js      "QUINIENTOS SETENTA Y OCHO CON 00/100 SOLES"
      │  ├─ csv.js                botones "Exportar" y "Descargar reporte"
      │  ├─ lista.js              paginar y buscar (sin tildes) en listas que el backend da completas
      │  ├─ useTitulo.js · useDemorado.js
      │  ├─ igv.js                subtotal e IGV desde el total (igual que el backend)
      │  ├─ jwt.js                leer los claims del token
      │  └─ validadores.js        correo, contraseña, DNI, RUC
      ├─ ui/                      Boton, Campo, AreaTexto, Selector, Casilla, Buscador, Interruptor, SelectorCantidad,
      │                           Insignia, Aviso, Toast, Tarjeta, Indicador, TarjetaResultado, EncabezadoPagina, Migas,
      │                           Pestanas, RangoFechas, Modal, PanelLateral, Tabla, Paginacion, Cargando, EstadoVacio,
      │                           MensajeError, Icono (Lucide inline), Logo y gráficos SVG (barras, líneas, dona, progreso)
      │  └─ externos/             componentes copiados de catálogos (equivale a components/ui de shadcn), en Tailwind:
      │                           AuroraBackground (Aceternity UI) y GlowCard (spotlight-card de 21st.dev)
      └─ mocks/                   ★ todo lo simulado (sección 7)
```

### Segmentos dentro de cada slice

Todos los slices de `entities`, `features` y `widgets` usan los mismos segmentos. Solo se crean los que hacen falta.

```
entities/producto/
├─ api/
│  ├─ productoApi.js       funciones que llaman al backend (usan shared/api)
│  └─ queries.js           claves y hooks de react-query: useProductos, useProducto
├─ model/
│  ├─ adaptadores.js       respuesta del backend → objeto que usa la UI
│  └─ constantes.js        umbrales de stock, estados, textos
├─ ui/
│  └─ ProductoCard.jsx     cómo se ve un producto (sin acciones: las acciones las inyecta quien lo usa)
└─ index.js                ★ API pública: lo único que otros pueden importar
```

**Regla de la API pública:** desde afuera solo se importa el `index.js` del slice.

```js
import { useProductos, ProductoCard } from '@/entities/producto';        // ✅
import { ProductoCard } from '@/entities/producto/ui/ProductoCard';      // ❌ salta la API pública
```

### Qué va en cada entidad

| Entidad | Endpoints que usa | Hooks de lectura | UI mínima |
|---|---|---|---|
| `sesion` | (lee el token, no llama al backend) | `useSesion()` → `{ usuario, rol, esAdmin, estaAutenticado, iniciar, cerrar }` | — |
| `usuario` | `/usuarios`, `/usuarios/{id}`, `/usuarios/me` | `useUsuarios`, `useUsuario(id)`, `usePerfil` | `AvatarIniciales`, `RolInsignia`, `EstadoCuentaInsignia` |
| `producto` | `/productos`, `/productos/{id}` | `useProductos(filtros)`, `useProducto(id)` | `ProductoCard`, `ProductoImagen`, `PrecioTexto` |
| `marca` | `/marcas` | `useMarcas` | — |
| `categoria` | `/categorias` | `useCategorias` | `CategoriaIcono` |
| `stock` | `/stock/{idProducto}`, `/stock?ids=` | `useStock(idProducto)`, `useStocks(ids)` (siempre frescos: `staleTime: 0`) | `StockInsignia` (En stock / Últimas unidades / Agotado) |
| `movimiento` | `/movimientos` | `useMovimientos(idProducto)` | `TipoMovimientoInsignia` |
| `carrito` | `/carrito` | `useCarrito` | `ContadorCarrito` |
| `venta` | `/pedidos`, `/pedidos/{id}` | `useVentas`, `useVenta(id)` | `EstadoVentaInsignia` |
| `comprobante` | `/comprobantes`, `/comprobantes/{id}`, `/comprobantes/venta/{idVenta}`, `/comprobantes/reporte` | `useComprobantes`, `useComprobante(id)`, `useComprobanteDeVenta(idVenta)`, `useReporte(rango)` | `TipoComprobanteInsignia` |

Las **escrituras** (POST, PUT, DELETE) viven en la `api/` de la entidad como funciones, pero el **hook de mutación** y el formulario viven en la `feature` que las dispara. Así `entities` solo sabe leer y `features` sabe actuar.

### Qué va en cada feature

| Feature | Llama a | Después de hacerlo |
|---|---|---|
| `login` | `POST /auth/login` | Guarda el token, redirige a `/` (CLIENTE) o `/admin` (ADMIN) |
| `registro` | `POST /auth/registro` | Redirige al login con aviso de cuenta creada |
| `logout` | — | Borra el token y la caché de react-query (hacia la tienda navega primero, para que la guarda de una página privada no mande al login) |
| `filtrar-catalogo` | — | Escribe `categoria`, `marca`, `q`, `page` en la URL |
| `buscar-productos` | `GET /productos?q=` (el mismo filtro del catálogo) | Buscador del encabezado con sugerencias (productos, categorías y marcas) y búsquedas recientes guardadas solo en el navegador (`localStorage`) |
| `agregar-al-carrito` | `POST /carrito/items` | Invalida `carrito` |
| `cambiar-cantidad-carrito` | `PUT /carrito/items/{idProducto}` | Invalida `carrito` |
| `quitar-del-carrito` | `DELETE /carrito/items/{idProducto}` | Invalida `carrito` |
| `confirmar-compra` | `POST /pedidos/checkout` | 201 → `/compra/{id}/confirmada` · 409 / 503 → `/compra/no-completada` con el motivo · invalida `carrito`, `ventas` y `stock` |
| `editar-perfil` | `PUT /usuarios/me` (propuesta) | Invalida `usuario` |
| `descargar-comprobante` | `GET /comprobantes/{id}/pdf` | Descarga el archivo |
| `guardar-producto` | `POST` / `PUT /productos` | Invalida `productos` |
| `desactivar-producto` | `DELETE /productos/{id}` (y `PUT` con `activo: true` para reactivar) | Invalida `productos` |
| `guardar-marca-categoria` | `POST` / `PUT /marcas` y `/categorias` (propuesta) | Invalida `marcas` / `categorias` y `productos` |
| `registrar-entrada-stock` | `POST /stock/entrada` | Invalida `stock` y `movimientos` |
| `ajustar-stock` | `PUT /stock/{idProducto}` | Invalida `stock` y `movimientos` |
| `filtrar-reporte` | — | Escribe `desde` y `hasta` en la URL |
| `guardar-usuario` | `POST` / `PUT /usuarios` | Invalida `usuarios` |
| `desactivar-usuario` | `DELETE /usuarios/{id}` (y `PUT` con `activo: true` para reactivar) | Invalida `usuarios` |

---

## 5. Configuración base

### `vite.config.js`

```js
import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    proxy: {
      // Todo lo que empieza con /api va al gateway sin el prefijo
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (ruta) => ruta.replace(/^\/api/, ''),
      },
    },
  },
});
```

### `jsconfig.json`

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": { "@/*": ["src/*"] },
    "jsx": "react-jsx"
  },
  "include": ["src"]
}
```

### Variables de entorno

Vite solo expone las variables que empiezan con `VITE_`. Ninguna es secreta: todo lo que está aquí lo puede ver cualquiera en el navegador.

`.env.development` (se sube al repo):

```properties
# URL base del backend vista desde el navegador (el proxy la manda al gateway)
VITE_API_URL=/api
# Servicios que responden con mocks. Se van quitando a medida que cada servicio esté listo.
VITE_MOCKS=auth,catalogo,inventario,pedidos,comprobantes
# true: el checkout simulado responde 503 (para probar la vista 4.9)
VITE_MOCK_FALLA_COMPROBANTE=false
```

`.env.development.local` (personal, no se sube): para probar contra el backend real sin tocar el archivo común.

```properties
VITE_MOCKS=
```

### `src/shared/config/env.js`

```js
export const API_URL = import.meta.env.VITE_API_URL ?? '/api';

/** Servicios simulados con MSW, por ejemplo ['auth', 'catalogo']. Vacío = todo real. */
export const SERVICIOS_MOCK = (import.meta.env.VITE_MOCKS ?? '')
  .split(',')
  .map((s) => s.trim())
  .filter(Boolean);

export const MOCK_FALLA_COMPROBANTE = import.meta.env.VITE_MOCK_FALLA_COMPROBANTE === 'true';
```

---

## 6. Dónde viven los endpoints

Hay **dos catálogos de rutas** y ninguno se escribe a mano en otro lugar:

| Archivo | Qué contiene | Quién lo usa |
|---|---|---|
| `src/shared/api/endpoints.js` | Las rutas **del backend** (gateway) | Las `api/` de cada entidad y los handlers de los mocks |
| `src/shared/config/rutas.js` | Las rutas **del navegador** (páginas) | El router, los `<Link>` y las redirecciones |

### `src/shared/api/endpoints.js`

Es el espejo de las rutas públicas del gateway (`ms-gateway/src/main/resources/application.yml`). **No incluye las rutas internas.** El servicio dueño va al lado de cada grupo; los mocks lo usan para saber qué simular.

```js
/**
 * Rutas del backend, relativas al gateway. Única fuente de verdad del front:
 * si una ruta cambia en un microservicio, se cambia solo aquí.
 * No incluye las rutas internas (/stock/reservar, /stock/confirmar, /stock/liberar, POST /comprobantes).
 */
export const ENDPOINTS = {
  // ms-auth
  auth: {
    login: '/auth/login',
    registro: '/auth/registro',
  },
  usuarios: {
    lista: '/usuarios',
    detalle: (id) => `/usuarios/${id}`,
    yo: '/usuarios/me', // propuesta → Galarza: el cliente lee y edita su propio perfil
  },

  // ms-catalogo
  productos: {
    lista: '/productos',
    detalle: (id) => `/productos/${id}`,
  },
  // Crear y editar marcas y categorías: propuesta → César
  marcas: { lista: '/marcas', detalle: (id) => `/marcas/${id}` },
  categorias: { lista: '/categorias', detalle: (id) => `/categorias/${id}` },

  // ms-inventario
  stock: {
    lista: '/stock', // propuesta → Arévalo: GET /stock?ids=1,2,3 (una llamada por grilla, no una por tarjeta)
    detalle: (idProducto) => `/stock/${idProducto}`,
    entrada: '/stock/entrada',
  },
  movimientos: { lista: '/movimientos' },

  // ms-pedidos
  carrito: {
    ver: '/carrito',
    items: '/carrito/items',
    item: (idProducto) => `/carrito/items/${idProducto}`,
  },
  pedidos: {
    lista: '/pedidos',
    detalle: (id) => `/pedidos/${id}`,
    checkout: '/pedidos/checkout',
  },

  // ms-comprobantes
  comprobantes: {
    lista: '/comprobantes',
    detalle: (id) => `/comprobantes/${id}`,
    porVenta: (idVenta) => `/comprobantes/venta/${idVenta}`,
    pdf: (id) => `/comprobantes/${id}/pdf`,
    reporte: '/comprobantes/reporte',
  },
};
```

### Tabla de endpoints

| Servicio | Método y ruta | Quién | Backend | Mock |
|---|---|---|---|---|
| auth | `POST /auth/login` | Público | ✅ | ✅ |
| auth | `POST /auth/registro` | Público | ✅ | ✅ |
| auth | `GET /usuarios` · `GET /usuarios/{id}` | ADMIN (o el mismo usuario) | ✅ | ✅ |
| auth | `POST /usuarios` · `PUT /usuarios/{id}` · `DELETE /usuarios/{id}` | ADMIN | ✅ | ✅ |
| auth | `GET` / `PUT /usuarios/me` (propuesta) | El mismo usuario | ✅ | ✅ |
| catalogo | `GET /productos?page&size&categoria&marca&q` + `precioMin`, `precioMax`, `conStock`, `sort`, `inactivos` (propuesta) | Público (`inactivos` solo ADMIN) | ✅ | ✅ |
| catalogo | `GET /productos/{id}` (200 también si está inactivo, con `activo: false`) | Público | ✅ | ✅ |
| catalogo | `POST /productos` · `PUT /productos/{id}` (con `especificaciones`) · `DELETE /productos/{id}` | ADMIN | ✅ | ✅ |
| catalogo | `GET /marcas` · `GET /categorias` | Público | ✅ | ✅ |
| catalogo | `POST /marcas` · `PUT /marcas/{id}` · `POST /categorias` · `PUT /categorias/{id}` (propuesta) | ADMIN | ✅ | ✅ |
| inventario | `GET /stock/{idProducto}` (sin entradas: 200 en 0) | Público | ✅ | ✅ |
| inventario | `GET /stock?ids=1,2,3` (propuesta; una entrada por id, en 0 si no tiene stock; sin `ids`, todos) | Público | ✅ | ✅ |
| inventario | `POST /stock/entrada` · `PUT /stock/{idProducto}` | ADMIN | ✅ | ✅ |
| inventario | `GET /movimientos?idProducto=` | ADMIN | ✅ | ✅ |
| pedidos | `GET /carrito` · `POST /carrito/items` · `PUT` / `DELETE /carrito/items/{idProducto}` | CLIENTE | ✅ | ✅ |
| pedidos | `POST /pedidos/checkout` | CLIENTE | ✅ | ✅ |
| pedidos | `GET /pedidos` · `GET /pedidos/{id}` | Dueño o ADMIN | ✅ | ✅ |
| comprobantes | `GET /comprobantes` · `GET /comprobantes/{id}` | Dueño o ADMIN | ✅ | ✅ |
| comprobantes | `GET /comprobantes/venta/{idVenta}` · `GET /comprobantes/{id}/pdf` | Dueño o ADMIN | ✅ | ✅ |
| comprobantes | `GET /comprobantes/reporte?desde&hasta` | ADMIN | ✅ | ✅ |

### Formas de las respuestas

Las marcadas **real** salen tal cual del DTO del backend hoy. Las marcadas **propuesta** aún no existen: el mock las define y se le pasan al responsable para que el backend responda igual. Si el backend termina distinto, solo se toca el `adaptadores.js` de la entidad.

| Recurso | Forma | Origen |
|---|---|---|
| Login | `{ token, tipo, expiraEnMinutos, rol }` | real (`LoginResponse`) |
| Usuario | `{ id, nombres, apellidos, correo, telefono, direccion, fotoUrl, rol, activo, creadoEn }` | real (`UsuarioResponse`) |
| Usuario (lo que falta) | `+ bloqueadoHasta`, `+ ultimoAcceso` (vista 5.10; columna nueva `usuario.ultimo_acceso`) | propuesta → Galarza |
| Producto | `{ id, nombre, descripcion, precio, imagenUrl, marca, categoria }` | real (`ProductoResponse`) |
| Producto (lo que falta) | `+ activo` (contrato C5), `+ idMarca`, `+ idCategoria` (para editar en 5.3), `+ especificaciones: [{ nombre, valor }]` (pestaña de la 4.3; tabla nueva `producto_especificacion`, se envían en `POST` / `PUT`) | propuesta → César |
| Página | `{ content: [...], page: { size, number, totalElements, totalPages } }` | propuesta → César (formato `VIA_DTO` de Spring Data) |
| Marca / categoría | `{ id, nombre, activo, productos }` (`productos` = cantidad de activos, para los filtros y la 5.4) | propuesta → César |
| Stock | `{ idProducto, disponible, reservado }`. `disponible` ya descuenta lo reservado: es lo que se puede vender | real (`StockResponse`) |
| Stock (lo que falta) | `+ actualizadoEn` (fecha del último movimiento, vista 5.5) | propuesta → Arévalo |
| Movimiento | `{ id, idProducto, tipo, cantidad, idVenta, motivo, creadoEn }` con `tipo` = `ENTRADA` · `RESERVA` · `CONFIRMACION` · `LIBERACION` · `AJUSTE` (este con signo) | propuesta → Arévalo |
| Carrito | `{ items: [{ idProducto, nombre, marca, precio, imagenUrl, cantidad, importe }], total }` | propuesta → Evelyn |
| Checkout (pide) | `{ tipoComprobante, docCliente, nombreRazonSocial }`; en boleta `docCliente` puede ser `null` (DNI opcional) | propuesta (README de ms-pedidos) |
| Venta | `{ id, idUsuario, fechaVenta, estado, tipoComprobante, subtotal, igv, total }` | real (`VentaResponse`) |
| Venta (lo que falta) | `+ detalles: [{ idProducto, nombreProducto, precioUnitario, cantidad, importe }]` en `GET /pedidos/{id}` **y en `GET /pedidos`** (miniaturas de 4.10, reportes 5.9) | propuesta → Evelyn |
| Comprobante | `{ id, idVenta, tipo, numero, docCliente, nombreRazonSocial, subtotal, igv, total, pdfUrl, emitidoEn }`. El servicio guarda además `idUsuario` (copiado de la venta) para el control de dueño; no sale en la respuesta | real (`ComprobanteResponse`) |
| Reporte | `{ desde, hasta, cantidad, subtotal, igv, total, comprobantes: [...] }` | propuesta → Galarza |
| Error | `{ fecha, status, error, mensaje, ruta, detalles }` | real (`ApiError`) |

**Huecos del contrato que el front necesita** (pedírselos al responsable; hoy solo los responde el mock):

1. **César (catálogo):** `activo`, `idMarca`, `idCategoria` y `especificaciones` en el producto; paginación y filtros (`marca` con varias separadas por coma, `precioMin`, `precioMax`, `sort`, `inactivos` solo para ADMIN, y `conStock`, que obliga a catálogo a consultar a inventario); `GET /marcas` y `/categorias` con la cantidad de productos; crear, renombrar y activar/desactivar marcas y categorías. Un producto inactivo responde 200 con `activo: false`; al crear un producto no se llama a inventario.
2. **Arévalo (inventario):** `GET /stock?ids=1,2,3` para pedir el stock de una grilla en una sola llamada (sin `ids`, todos, para la vista 5.5); `actualizadoEn` en el stock; los tipos de movimiento `RESERVA`, `CONFIRMACION` y `LIBERACION`; un producto sin entradas responde stock 0 (no 404); `GET /stock` como ruta pública.
3. **Evelyn (pedidos):** `detalles` en `GET /pedidos` y `GET /pedidos/{id}`; la forma del carrito con `marca`; en boleta el DNI es opcional; mandar el `idUsuario` de la venta al emitir el comprobante.
4. **Galarza (auth):** `POST /auth/registro` (con 409 si el correo existe); `GET` / `PUT /usuarios/me` para que el cliente edite su perfil, su foto (URL) y su contraseña; `bloqueadoHasta` y `ultimoAcceso` en `UsuarioResponse`; reactivar una cuenta con `PUT` `{ activo: true }` también levanta el bloqueo por intentos. En comprobantes: filtrar por `comprobante.id_usuario` para el control de dueño.

### `src/shared/api/http.js`

```js
import axios from 'axios';
import { API_URL } from '@/shared/config/env';
import { ApiError } from './errores';
import { leerToken } from './token';

export const http = axios.create({ baseURL: API_URL });

let alNoAutorizado = () => {};

/** La capa app registra aquí qué hacer con un 401 (cerrar la sesión y llevar al login). */
export function registrarAlNoAutorizado(fn) {
  alNoAutorizado = fn;
}

http.interceptors.request.use((config) => {
  const token = leerToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

http.interceptors.response.use(
  (respuesta) => respuesta.data,
  (error) => {
    const apiError = ApiError.desdeAxios(error);
    // En el login, el 401 es "credenciales inválidas", no una sesión vencida
    if (apiError.status === 401 && !error.config?.url?.endsWith('/auth/login')) alNoAutorizado();
    return Promise.reject(apiError);
  },
);
```

Por el interceptor de respuesta, **todas las funciones `api/` devuelven directamente el cuerpo** (no el objeto de axios) y **todos los errores son `ApiError`**.

### `src/shared/api/errores.js`

```js
/** Mismo formato que el ApiError del backend, más el caso "sin conexión" (status 0). */
export class ApiError extends Error {
  constructor({ status, error, mensaje, detalles = [], ruta = '' }) {
    super(mensaje);
    this.status = status;
    this.error = error;
    this.detalles = detalles;
    this.ruta = ruta;
  }

  static desdeAxios(error) {
    const cuerpo = error.response?.data;
    if (cuerpo?.mensaje) return new ApiError(cuerpo);
    if (error.response) {
      return new ApiError({ status: error.response.status, error: error.response.statusText, mensaje: 'Ocurrió un error inesperado' });
    }
    return new ApiError({ status: 0, error: 'Sin conexión', mensaje: 'No se pudo conectar con el servidor' });
  }
}
```

### `src/shared/api/token.js`

```js
const CLAVE = 'intiguard.token';

export function leerToken() {
  try {
    return localStorage.getItem(CLAVE);
  } catch {
    return null;
  }
}

export function guardarToken(token) {
  try {
    localStorage.setItem(CLAVE, token);
  } catch {
    // Navegación privada sin almacenamiento: la sesión dura lo que dure la pestaña
  }
}

export function borrarToken() {
  try {
    localStorage.removeItem(CLAVE);
  } catch {
    // Nada que borrar
  }
}
```

### `src/shared/api/paginacion.js`

```js
/** Acepta la página de Spring en formato VIA_DTO ({content, page}) o el clásico (PageImpl). */
export function aPagina(datos) {
  if (Array.isArray(datos)) {
    return { items: datos, pagina: 0, tamano: datos.length, total: datos.length, totalPaginas: 1 };
  }
  const meta = datos.page ?? datos;
  return {
    items: datos.content ?? [],
    pagina: meta.number ?? 0,
    tamano: meta.size ?? 0,
    total: meta.totalElements ?? 0,
    totalPaginas: meta.totalPages ?? 1,
  };
}
```

Acepta también una lista simple, que es lo que devuelve hoy `GET /productos`: el front funciona antes y después de que César agregue la paginación.

### `src/shared/api/index.js`

```js
export { ENDPOINTS } from './endpoints';
export { http, registrarAlNoAutorizado } from './http';
export { ApiError } from './errores';
export { leerToken, guardarToken, borrarToken } from './token';
export { aPagina } from './paginacion';
```

### `src/shared/lib/jwt.js`

```js
/** Lee los claims del JWT sin validarlo (la validación la hace el backend). */
export function leerClaims(token) {
  try {
    const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      [...atob(base64)].map((c) => `%${c.charCodeAt(0).toString(16).padStart(2, '0')}`).join(''),
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

export function estaVencido(claims) {
  return !claims?.exp || claims.exp * 1000 <= Date.now();
}
```

### `src/shared/lib/igv.js`

```js
/** Igual que Venta.asignarMontos del backend: los precios incluyen IGV. */
export function desglosarIgv(total) {
  const subtotal = Math.round((total / 1.18) * 100) / 100;
  const igv = Math.round((total - subtotal) * 100) / 100;
  return { subtotal, igv, total };
}
```

Solo sirve para **mostrar** el desglose del carrito antes de comprar. Los montos de una venta o un comprobante siempre se leen del backend.

### `src/shared/lib/validadores.js`

Las mismas reglas que valida el backend, para avisar antes de enviar:

```js
export const esCorreo = (v) => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v ?? '');
export const esClaveValida = (v) => (v ?? '').length >= 8;
export const esDni = (v) => /^\d{8}$/.test(v ?? '');
export const esRuc = (v) => /^(10|15|17|20)\d{9}$/.test(v ?? '');
```

### Ejemplo de entidad: `src/entities/producto/`

`api/productoApi.js`:

```js
import { ENDPOINTS, http, aPagina } from '@/shared/api';

export async function listarProductos({ page = 0, size = 12, categoria, marca, q } = {}) {
  const datos = await http.get(ENDPOINTS.productos.lista, { params: { page, size, categoria, marca, q } });
  return aPagina(datos);
}

export const obtenerProducto = (id) => http.get(ENDPOINTS.productos.detalle(id));

// Escrituras: las usan las features guardar-producto y desactivar-producto
export const crearProducto = (datos) => http.post(ENDPOINTS.productos.lista, datos);
export const editarProducto = (id, datos) => http.put(ENDPOINTS.productos.detalle(id), datos);
export const desactivarProducto = (id) => http.delete(ENDPOINTS.productos.detalle(id));
```

`api/queries.js`:

```js
import { useQuery } from '@tanstack/react-query';
import { listarProductos, obtenerProducto } from './productoApi';

export const productoKeys = {
  todos: ['productos'],
  lista: (filtros) => ['productos', 'lista', filtros],
  detalle: (id) => ['productos', 'detalle', id],
};

export const useProductos = (filtros) =>
  useQuery({ queryKey: productoKeys.lista(filtros), queryFn: () => listarProductos(filtros) });

export const useProducto = (id) =>
  useQuery({ queryKey: productoKeys.detalle(id), queryFn: () => obtenerProducto(id), enabled: Boolean(id) });
```

`index.js`:

```js
export { useProductos, useProducto, productoKeys } from './api/queries';
export { crearProducto, editarProducto, desactivarProducto } from './api/productoApi';
export { ProductoCard } from './ui/ProductoCard';
```

### Ejemplo de feature: `src/features/agregar-al-carrito/`

```js
// model/useAgregarAlCarrito.js
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { agregarItem, carritoKeys } from '@/entities/carrito';

export function useAgregarAlCarrito() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ idProducto, cantidad }) => agregarItem(idProducto, cantidad),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: carritoKeys.todos }),
  });
}
```

```jsx
// ui/BotonAgregarAlCarrito.jsx
import { Boton } from '@/shared/ui';
import { useAgregarAlCarrito } from '../model/useAgregarAlCarrito';

export function BotonAgregarAlCarrito({ idProducto, cantidad = 1, agotado }) {
  const { mutate, isPending } = useAgregarAlCarrito();
  if (agotado) return <Boton disabled>Agotado</Boton>;
  return (
    <Boton cargando={isPending} onClick={() => mutate({ idProducto, cantidad })}>
      Agregar al carrito
    </Boton>
  );
}
```

El widget `grilla-productos` junta `ProductoCard` (entidad) con `BotonAgregarAlCarrito` (feature). La tarjeta no sabe nada del carrito: recibe el botón como hijo.

### Sesión: `src/entities/sesion/`

```jsx
// model/SesionProvider.jsx
import { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { borrarToken, guardarToken, leerToken } from '@/shared/api';
import { estaVencido, leerClaims } from '@/shared/lib/jwt';

const SesionContext = createContext(null);

function claimsVigentes() {
  const claims = leerClaims(leerToken());
  return claims && !estaVencido(claims) ? claims : null;
}

export function SesionProvider({ children }) {
  const [claims, setClaims] = useState(claimsVigentes);

  const iniciar = useCallback((token) => {
    guardarToken(token);
    setClaims(leerClaims(token));
  }, []);

  const cerrar = useCallback(() => {
    borrarToken();
    setClaims(null);
  }, []);

  const valor = useMemo(
    () => ({
      usuario: claims && { id: claims.idUsuario, correo: claims.sub },
      rol: claims?.rol ?? null,
      estaAutenticado: Boolean(claims),
      esAdmin: claims?.rol === 'ADMIN',
      iniciar,
      cerrar,
    }),
    [claims, iniciar, cerrar],
  );

  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>;
}

export const useSesion = () => useContext(SesionContext);
```

Los nombres y la foto del usuario no están en el token: el encabezado los pide con `useUsuario(usuario.id)`.

---

## 7. Dónde viven los mocks

Todo lo simulado está en **`src/shared/mocks/`**. Ningún otro archivo del proyecto sabe que existen: los mocks interceptan las mismas rutas de `ENDPOINTS` que usa el código real.

```
src/shared/mocks/
├─ browser.js                 arranca MSW solo con los servicios de VITE_MOCKS
├─ handlers/                  un archivo por microservicio, igual que el backend
│  ├─ auth.js                 /auth/*, /usuarios/*
│  ├─ catalogo.js             /productos/*, /marcas, /categorias
│  ├─ inventario.js           /stock/*, /movimientos
│  ├─ pedidos.js              /carrito/*, /pedidos/*
│  ├─ comprobantes.js         /comprobantes/*
│  └─ index.js                { auth, catalogo, inventario, pedidos, comprobantes }
├─ db/                        datos en memoria (se reinician al recargar la página)
│  ├─ usuarios.js
│  ├─ marcas.js
│  ├─ categorias.js
│  ├─ productos.js
│  ├─ stock.js
│  ├─ movimientos.js
│  ├─ carritos.js
│  ├─ ventas.js
│  └─ comprobantes.js
└─ lib/
   ├─ respuestas.js           error() con el formato ApiError, ruta() con el prefijo /api
   ├─ token.js                genera un JWT falso con sub, rol, idUsuario, iat y exp
   └─ sesion.js               lee el usuario del header Authorization; responde 401 / 403
```

### Reglas de los mocks

1. **Mismas rutas** (las de `ENDPOINTS`), **mismos códigos HTTP** y **mismo formato** de respuesta y de error que el backend.
2. **Mismas reglas de negocio** que la guía del README: login con bloqueo al 5.º intento, solo productos activos en la tienda, carrito de un solo producto por línea, checkout todo o nada, cada cliente ve solo lo suyo, numeración B001/F001 correlativa.
3. **Mismas rutas protegidas:** sin token → 401; CLIENTE en ruta de ADMIN → 403.
4. **Datos semilla = los de `PROTOTIPO_WEB.md` §3** (8 productos + uno inactivo, Ana Torres, pedido #00042 con 2 cámaras bala). Además, `db/ventas.js` genera con semilla fija un historial de ventas desde el 01/09/2026 hasta hoy, y `db/comprobantes.js` y `db/movimientos.js` salen de esas ventas (numeración B001/F001 en orden de fecha y stock inicial calculado para que cuadre con `db/stock.js`). Por eso la boleta del #00042 no es la B001-00000001 del prototipo.
5. **Latencia simulada** de 300 ms, para ver los estados de carga.
6. **Nunca se importan desde `features`, `entities` ni `pages`.** Solo `app/main.jsx` los enciende.
7. **No llegan a producción.** `main.jsx` los enciende solo en desarrollo o, para una demo publicada (por ejemplo en Render), con `VITE_MOCKS_EN_BUILD=true` al hacer el build; sin esa variable, Vite descarta todo su código. En la demo, si no se define `VITE_MOCKS`, se simulan los cinco servicios.

### Usuarios de los mocks

Solo existen en el navegador, no en Aiven:

| Correo | Contraseña | Rol |
|---|---|---|
| `ana.torres@gmail.com` | `cliente123` | CLIENTE |
| `admin@intiguard.pe` | `admin1234` | ADMIN |
| `luis.ramos@hotmail.com` | `cliente123` | CLIENTE bloqueado por intentos (10 min desde que carga la página) |
| `pedro.quispe@gmail.com` | `cliente123` | CLIENTE inactivo |

Hay otros clientes (Carla Díaz, Jorge Salas de Seguridad Integral del Sur, María Fernández, Diego Huamán) que solo aparecen en las ventas. **Recargar la página reinicia todos los datos**: para probar un flujo largo, navegar dentro de la app.

### `src/shared/mocks/browser.js`

```js
import { setupWorker } from 'msw/browser';
import { SERVICIOS_MOCK } from '@/shared/config/env';
import * as handlers from './handlers';

const activos = SERVICIOS_MOCK.flatMap((servicio) => handlers[servicio] ?? []);

export const worker = setupWorker(...activos);
export const hayMocks = activos.length > 0;
```

### `src/shared/mocks/lib/respuestas.js`

```js
import { HttpResponse } from 'msw';
import { API_URL } from '@/shared/config/env';

/** Ruta del mock = prefijo /api + ruta de ENDPOINTS. */
export const ruta = (endpoint) => `${API_URL}${endpoint}`;

/** Ruta con parámetro en formato MSW: rutaParam(ENDPOINTS.productos.detalle) → /api/productos/:id */
export const rutaParam = (fn, nombre = 'id') => ruta(fn(`:${nombre}`));

const FRASES = { 400: 'Bad Request', 401: 'Unauthorized', 403: 'Forbidden', 404: 'Not Found', 409: 'Conflict', 423: 'Locked', 503: 'Service Unavailable' };

/** Error con el mismo formato que el ApiError del backend. */
export function error(status, mensaje, request, detalles = []) {
  return HttpResponse.json(
    { fecha: new Date().toISOString().slice(0, 19), status, error: FRASES[status], mensaje, ruta: new URL(request.url).pathname.replace(API_URL, ''), detalles },
    { status },
  );
}
```

### `src/shared/mocks/lib/token.js`

```js
const base64Url = (obj) => btoa(JSON.stringify(obj)).replace(/=+$/, '').replace(/\+/g, '-').replace(/\//g, '_');

/** JWT falso: la firma no vale, pero los claims son los mismos que emite ms-auth. */
export function crearTokenFalso(usuario, minutos = 60) {
  const ahora = Math.floor(Date.now() / 1000);
  const claims = { sub: usuario.correo, rol: usuario.rol, idUsuario: usuario.id, iat: ahora, exp: ahora + minutos * 60 };
  return `${base64Url({ alg: 'HS256' })}.${base64Url(claims)}.firma-de-mock`;
}
```

### Ejemplo de handler: `src/shared/mocks/handlers/auth.js`

```js
import { delay, http, HttpResponse } from 'msw';
import { ENDPOINTS } from '@/shared/api/endpoints';
import { usuarios } from '../db/usuarios';
import { error, ruta } from '../lib/respuestas';
import { crearTokenFalso } from '../lib/token';

const MAX_INTENTOS = 5;
const MINUTOS_BLOQUEO = 15;
// Textos exactos de CredencialesInvalidasException y CuentaBloqueadaException (ms-auth)
const MENSAJE_401 = 'Credenciales inválidas';
const MENSAJE_423 = 'Cuenta bloqueada temporalmente por intentos fallidos. Intente más tarde';

export const auth = [
  http.post(ruta(ENDPOINTS.auth.login), async ({ request }) => {
    await delay(300);
    const { correo, password } = await request.json();
    const usuario = usuarios.find((u) => u.correo === correo);
    if (!usuario) return error(401, MENSAJE_401, request);

    if (usuario.bloqueadoHasta) {
      if (usuario.bloqueadoHasta > Date.now()) return error(423, MENSAJE_423, request);
      // El bloqueo ya venció: se empieza de cero
      usuario.bloqueadoHasta = null;
      usuario.intentosFallidos = 0;
    }

    if (!usuario.activo) return error(401, MENSAJE_401, request);

    if (usuario.clave !== password) {
      usuario.intentosFallidos += 1;
      if (usuario.intentosFallidos >= MAX_INTENTOS) {
        usuario.bloqueadoHasta = Date.now() + MINUTOS_BLOQUEO * 60_000;
        return error(423, MENSAJE_423, request);
      }
      return error(401, MENSAJE_401, request);
    }

    usuario.intentosFallidos = 0;
    return HttpResponse.json({ token: crearTokenFalso(usuario), tipo: 'Bearer', expiraEnMinutos: 60, rol: usuario.rol });
  }),
];
```

El orden de las validaciones y los textos son los mismos de `AuthService.login` en ms-auth: si el backend los cambia, se cambian aquí.

### Qué debe simular cada handler

| Handler | Comportamientos obligatorios |
|---|---|
| `auth` | Login con bloqueo (401 genérico, 423 al 5.º intento) · registro con rol CLIENTE y 409 si el correo existe · usuarios solo para ADMIN, desactivar en vez de borrar |
| `catalogo` | Lista paginada de 12 con filtros `categoria`, `marca` y `q` combinables · la tienda solo ve activos · 404 para un producto inexistente · CRUD solo ADMIN, `DELETE` pone `activo: false` |
| `inventario` | Stock por producto · entrada suma disponible · ajuste exige motivo y no deja disponible negativo (400) · cada cambio agrega un movimiento |
| `pedidos` | Carrito por usuario del token, una línea por producto · checkout: venta PENDIENTE → reserva todo o nada (409 con el producto que falló, venta ANULADA) → comprobante (503 si `VITE_MOCK_FALLA_COMPROBANTE=true`, se libera y venta ANULADA) → venta CONFIRMADA y carrito vacío · `GET /pedidos` filtra por dueño salvo ADMIN · 403 si un cliente pide un pedido ajeno |
| `comprobantes` | Numeración B001 / F001 correlativa · uno por venta · filtra por dueño salvo ADMIN · PDF devuelve un archivo de ejemplo · reporte suma los comprobantes del rango |

El checkout toca stock, ventas y comprobantes a la vez. Como las tablas en memoria (`db/`) son compartidas, el handler de `pedidos` las modifica directamente. **En el mock no hay llamadas entre servicios.**

### `src/app/main.jsx`

```jsx
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import './styles/tokens.css';
import './styles/global.css';

async function encenderMocks() {
  const { worker, hayMocks } = await import('@/shared/mocks/browser');
  if (hayMocks) await worker.start({ onUnhandledRequest: 'bypass' });
}

const mocks = import.meta.env.DEV ? encenderMocks() : Promise.resolve();

mocks.then(() => {
  createRoot(document.getElementById('root')).render(
    <StrictMode>
      <App />
    </StrictMode>,
  );
});
```

- `import.meta.env.DEV` es `false` en `npm run build`, así que **los mocks nunca llegan a producción** (Vite descarta el import dinámico).
- `onUnhandledRequest: 'bypass'`: lo que no tiene mock pasa al proxy y llega al gateway. Por eso se puede tener `catalogo` real y `pedidos` simulado al mismo tiempo.

---

## 8. Router y guardas

### `src/shared/config/rutas.js`

```js
/** Rutas del navegador (páginas). Única fuente de verdad para el router, los <Link> y las redirecciones. */
export const RUTAS = {
  inicio: '/',
  catalogo: '/catalogo',
  catalogoDe: (categoria) => `/catalogo?categoria=${encodeURIComponent(categoria)}`,
  producto: (id) => `/productos/${id}`,
  login: '/login',
  registro: '/registro',
  carrito: '/carrito',
  checkout: '/checkout',
  compraConfirmada: (idVenta) => `/compra/${idVenta}/confirmada`,
  compraNoCompletada: '/compra/no-completada',
  misPedidos: '/mis-pedidos',
  miPedido: (id) => `/mis-pedidos/${id}`,
  comprobanteImprimible: (id) => `/comprobantes/${id}/imprimir`,
  perfil: '/perfil',
  sinPermiso: '/403',
  sinConexion: '/sin-conexion',
  sesionExpirada: '/sesion-expirada',

  admin: '/admin',
  adminProductos: '/admin/productos',
  adminProductoNuevo: '/admin/productos/nuevo',
  adminProductoEditar: (id) => `/admin/productos/${id}/editar`,
  adminMarcasCategorias: '/admin/marcas-categorias',
  adminStock: '/admin/stock',
  adminMovimientos: '/admin/movimientos',
  adminVentas: '/admin/ventas',
  adminVenta: (id) => `/admin/ventas/${id}`,
  adminComprobantes: '/admin/comprobantes',
  adminReportes: '/admin/reportes',
  adminUsuarios: '/admin/usuarios',
};
```

### Mapa de rutas

| Ruta | Página | Vista | Guarda |
|---|---|---|---|
| `/` | `inicio` | 4.1 | — |
| `/catalogo?categoria&marca&q&page` | `catalogo` | 4.2 | — |
| `/productos/:id` | `producto-detalle` | 4.3 | — |
| `/login` | `login` | 4.4 | `SoloInvitado` |
| `/registro` | `registro` | 4.5 | `SoloInvitado` |
| `/carrito` | `carrito` | 4.6 | `RequiereRol CLIENTE` |
| `/checkout` | `checkout` | 4.7 | `RequiereRol CLIENTE` (marco simplificado `TiendaSimpleLayout`) |
| `/compra/:idVenta/confirmada` | `compra-confirmada` | 4.8 | `RequiereRol CLIENTE` |
| `/compra/no-completada` | `compra-no-completada` | 4.9 | `RequiereRol CLIENTE` |
| `/mis-pedidos` | `mis-pedidos` | 4.10 | `RequiereRol CLIENTE` |
| `/mis-pedidos/:id` | `pedido-detalle` | 4.11 | `RequiereRol CLIENTE` |
| `/comprobantes/:id/imprimir` | `comprobante-imprimible` | 4.12 | `RequiereSesion` (sin marco) |
| `/perfil` | `perfil` | 4.13 | `RequiereSesion` |
| `/403`, `/sesion-expirada`, `/sin-conexion` y `*` | `error` | 4.14 | — |
| `/admin` | `admin-resumen` | 5.1 | `RequiereRol ADMIN` |
| `/admin/productos` | `admin-productos` | 5.2 | `RequiereRol ADMIN` |
| `/admin/productos/nuevo` · `/admin/productos/:id/editar` | `admin-producto-form` | 5.3 | `RequiereRol ADMIN` |
| `/admin/marcas-categorias` | `admin-marcas-categorias` | 5.4 | `RequiereRol ADMIN` |
| `/admin/stock` | `admin-stock` | 5.5 | `RequiereRol ADMIN` |
| `/admin/movimientos?idProducto` | `admin-movimientos` | 5.6 | `RequiereRol ADMIN` |
| `/admin/ventas` | `admin-ventas` | 5.7 | `RequiereRol ADMIN` |
| `/admin/ventas/:id` | `admin-venta-detalle` | 5.7 | `RequiereRol ADMIN` |
| `/admin/comprobantes` | `admin-comprobantes` | 5.8 | `RequiereRol ADMIN` |
| `/admin/reportes?desde&hasta` | `admin-reportes` | 5.9 | `RequiereRol ADMIN` |
| `/admin/usuarios` | `admin-usuarios` | 5.10 | `RequiereRol ADMIN` |

Las rutas de la tienda van dentro de `tienda-layout` y las de `/admin` dentro de `admin-layout`. Cada ruta del panel lleva en `handle` sus migas (`{ migas: [{ texto, a }] }`); el panel las lee con `useMatches()` y usa la última como título de la pestaña de esa página.

### `src/app/router/guardas.jsx`

```jsx
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useSesion } from '@/entities/sesion';
import { RUTAS } from '@/shared/config/rutas';

/** Sin sesión → al login, recordando a dónde quería ir. */
export function RequiereSesion() {
  const { estaAutenticado } = useSesion();
  const location = useLocation();
  if (!estaAutenticado) return <Navigate to={RUTAS.login} replace state={{ desde: location.pathname }} />;
  return <Outlet />;
}

/** Con sesión pero otro rol → 403. */
export function RequiereRol({ rol }) {
  const sesion = useSesion();
  const location = useLocation();
  if (!sesion.estaAutenticado) return <Navigate to={RUTAS.login} replace state={{ desde: location.pathname }} />;
  if (sesion.rol !== rol) return <Navigate to={RUTAS.sinPermiso} replace />;
  return <Outlet />;
}

/** Con sesión no tiene sentido ver el login ni el registro. */
export function SoloInvitado() {
  const { estaAutenticado, esAdmin } = useSesion();
  if (estaAutenticado) return <Navigate to={esAdmin ? RUTAS.admin : RUTAS.inicio} replace />;
  return <Outlet />;
}
```

Las guardas solo ordenan la navegación. **La seguridad real la pone el backend:** aunque alguien salte una guarda, el gateway y el servicio responden 401 o 403.

### `src/app/App.jsx`

```jsx
import { useEffect } from 'react';
import { RouterProvider } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { SesionProvider, useSesion } from '@/entities/sesion';
import { registrarAlNoAutorizado } from '@/shared/api';
import { QueryProvider } from './providers';
import { router } from './router/router';

function SesionVencida() {
  const { cerrar } = useSesion();
  const queryClient = useQueryClient();
  useEffect(() => {
    registrarAlNoAutorizado(() => {
      cerrar();
      queryClient.clear();
      router.navigate('/login');
    });
  }, [cerrar, queryClient]);
  return null;
}

export function App() {
  return (
    <QueryProvider>
      <SesionProvider>
        <SesionVencida />
        <RouterProvider router={router} />
      </SesionProvider>
    </QueryProvider>
  );
}
```

### `src/app/providers/QueryProvider.jsx`

```jsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      // Reintentar solo fallas de red o 5xx; un 4xx no se arregla reintentando
      retry: (intentos, error) => (error.status === 0 || error.status >= 500) && intentos < 2,
      refetchOnWindowFocus: false,
    },
  },
});

export function QueryProvider({ children }) {
  return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
}
```

---

## 9. Manejo de errores en la UI

Cada error llega como `ApiError` con `status`, `mensaje` y `detalles`. Quién lo resuelve:

| Status | Dónde se resuelve | Qué se hace |
|---|---|---|
| 0 | `shared/ui/MensajeError` | "No se pudo conectar con el servidor" + botón Reintentar |
| 400 | La feature del formulario | Muestra `mensaje` y marca los campos de `detalles` |
| 401 en el login | `features/login` | "Credenciales inválidas" |
| 401 en otra ruta | `http.js` → `App.jsx` | Cierra la sesión y lleva al login |
| 403 | La página | Lleva a `/403` |
| 404 | La página | Lleva a la vista 4.14 |
| 409 en el checkout | `features/confirmar-compra` | Lleva a 4.9 con "stock insuficiente" y el producto que falló |
| 409 en el registro | `features/registro` | "El correo ya está registrado" en el campo correo |
| 423 | `features/login` | Muestra el `mensaje` de bloqueo |
| 503 en el checkout | `features/confirmar-compra` | Lleva a 4.9 con "servicio no disponible, no se cobró nada" |

---

## 10. Reglas de trabajo

1. **Importar solo hacia abajo** (`app → pages → widgets → features → entities → shared`) y solo por el `index.js` de cada slice.
2. **Ninguna URL del backend fuera de `shared/api/endpoints.js`.** Ninguna URL de página fuera de `shared/config/rutas.js`.
3. **Ningún `axios` ni `fetch` fuera de `shared/api/http.js`.**
4. **Las respuestas del backend se adaptan en `entities/*/model/adaptadores.js`.** Si el backend cambia un campo, se arregla ahí y no en cada componente.
5. **Los montos de ventas y comprobantes se leen del backend**, nunca se recalculan. `igv.js` es solo para la vista previa del carrito.
6. **El id del usuario nunca se manda en el cuerpo.** El backend lo saca del token.
7. **Los mocks imitan al backend, no al revés.** Si un mock y el backend reales difieren, se corrige el mock.
8. **Un servicio sale de `VITE_MOCKS` cuando su endpoint está ✅** en la tabla de la sección 6 y responde igual que el mock.
9. **Nombres en español**, como el backend: `listarProductos`, `useVenta`, `BotonAgregarAlCarrito`. Componentes en `PascalCase.jsx`, el resto en `camelCase.js`, carpetas en `kebab-case`.

---

## 11. Orden de construcción

| Paso | Qué | Resultado verificable | Estado |
|---|---|---|---|
| 1 | Crear el proyecto (sección 3) y la configuración (sección 5) | `npm run dev` abre una página en blanco sin errores | ✅ |
| 2 | `shared/`: `api`, `config`, `lib` y los componentes base de `ui` con los colores de `tokens.css` | Una página de prueba muestra botones, campos e insignias | ✅ |
| 3 | `shared/mocks/` con `auth` y `catalogo` y sus datos semilla | En la pestaña Red del navegador, `/api/productos` responde "from service worker" | ✅ |
| 4 | `entities/sesion`, `producto`, `marca`, `categoria`, `stock` | Los hooks devuelven los 8 productos del mock | ✅ |
| 5 | `widgets/tienda-layout` + `app/router` + páginas 4.1 a 4.5 | Se navega la tienda y se inicia sesión como Ana o como admin | ✅ |
| 6 | Mocks de `pedidos` y `comprobantes` + `entities/carrito`, `venta`, `comprobante` + features de carrito y compra + páginas 4.6 a 4.14 | Compra completa con boleta; con `VITE_MOCK_FALLA_COMPROBANTE=true`, la vista 4.9 | ✅ |
| 7 | `widgets/admin-layout` + páginas 5.1 a 5.10 | El admin gestiona productos, stock, ventas y usuarios en el mock | ✅ |
| 8 | Conectar el backend real, un servicio a la vez: quitarlo de `VITE_MOCKS` en `.env.development.local` y levantar gateway + Eureka + ese servicio | La misma pantalla funciona igual contra Aiven | ⬜ |

Para el paso 8, el backend se levanta como dice [`ARRANQUE_PROYECTO.md`](../../DSW_II_PROYECTO/ARRANQUE_PROYECTO.md). Con `EXIGIR_TOKEN=false` (desarrollo) el gateway deja pasar peticiones sin token: para probar los 401 y 403 reales hay que levantarlo con `EXIGIR_TOKEN=true`.
