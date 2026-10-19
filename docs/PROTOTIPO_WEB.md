# Prototipo web · IntiGuard

> Este documento vive en `DSW_II_PROYECTO_WEB/docs/` junto con [`ARQUITECTURA_WEB.md`](ARQUITECTURA_WEB.md). Las secciones 6 y 7 dicen qué vistas tienen diseño de Stitch y en qué difiere la app construida.

Especificación visual, **vista por vista**, de la tienda en línea IntiGuard y de su panel de administración. Está escrita para generar cada pantalla con una IA de diseño (por ejemplo **Google Stitch**): cada vista describe qué se ve, en qué orden, con qué textos y datos de ejemplo, e incluye un **prompt listo para copiar**.

---

## Cómo usar este documento con Stitch

1. **Crea un proyecto nuevo en Stitch**, en plataforma **Web**. No uses "remix" de otro proyecto: Stitch hereda el sistema de diseño del proyecto original (tipografías y colores que no son los de IntiGuard).
2. **Arma cada pedido con tres bloques, en este orden:**
   1. el [prompt base](#prompt-base-pegar-siempre-primero) (estilo y reglas);
   2. el prompt del **marco** que use la vista: [marco de tienda](#prompt-del-marco-de-tienda) o [marco del panel](#prompt-del-marco-del-panel-de-administración);
   3. el **prompt de la vista**.

   Pégalos juntos en un mismo mensaje. Así la barra superior, el pie y los colores salen siempre iguales. La única excepción es el comprobante imprimible (4.12), que no lleva marco.
3. **Una vista por pedido.**
4. **Revisa el resultado contra la vista de este documento.** Si Stitch agrega algo que no está (un banner, una etiqueta, un dato inventado), pide el cambio puntual: *"Quita la sección X. No agregues nada que no esté en el pedido."*
5. Si un texto sale cambiado, pide: *"Usa exactamente los textos entre comillas, sin cambiarlos."*

## 1. Sistema de diseño

### 1.1 Marca

- **Nombre:** IntiGuard.
- **Qué vende:** equipos de seguridad electrónica para hogares y negocios en Perú: cámaras, alarmas, sensores de movimiento, detectores de humo, cerraduras digitales y control de acceso.
- **Concepto:** **seguridad** (confianza y solidez: azul profundo) + **Inti**, el sol inca (calidez y marca peruana: dorado).
- **Logo:** un escudo con un sol dorado de rayos cortos en el centro, y a su derecha la palabra "IntiGuard": "Inti" en dorado y "Guard" en azul profundo (sobre fondo oscuro, "Guard" en blanco).
- **Tono:** profesional, confiable, cercano. Textos en **español de Perú**, en segunda persona ("Tu carrito", "Tus pedidos").
- **Moneda:** soles, con el formato `S/ 1,199.00`. Los precios **incluyen IGV**.

### 1.2 Paleta de colores

| Rol | Hex | Uso |
|---|---|---|
| Primario | `#1E3A8A` | Barra superior de la tienda, botón principal, enlaces, elementos activos |
| Primario hover | `#1E40AF` | Hover y foco del primario |
| Primario suave | `#DBEAFE` | Fondos de selección, chips de filtro activos, filas seleccionadas |
| Acento (oro Inti) | `#F59E0B` | Insignia del carrito, precios destacados, botón de acción secundaria, detalles de marca |
| Acento hover | `#D97706` | Hover del acento |
| Acento suave | `#FEF3C7` | Fondos de avisos de marca |
| Fondo de página | `#F8FAFC` | Fondo general |
| Superficie | `#FFFFFF` | Tarjetas, tablas, formularios, modales |
| Borde | `#E2E8F0` | Bordes y separadores |
| Texto principal | `#0F172A` | Títulos y texto |
| Texto secundario | `#475569` | Descripciones, etiquetas, fechas |
| Deshabilitado | `#94A3B8` | Controles y textos deshabilitados |
| Barra del panel admin | `#0F172A` | Barra lateral del administrador |
| Texto de la barra admin | `#E2E8F0` | Opciones de la barra lateral |

**Estados**

| Estado | Texto / ícono | Fondo | Ejemplos |
|---|---|---|---|
| Éxito | `#15803D` | `#DCFCE7` | "Confirmada", "En stock", "Activo" |
| Advertencia | `#B45309` | `#FEF3C7` | "Pendiente", "Últimas unidades" |
| Error | `#B91C1C` | `#FEE2E2` | "Anulada", "Agotado", "Bloqueado", errores de formulario |
| Información | `#0369A1` | `#E0F2FE` | Avisos neutros, ayudas |

**Reglas de contraste**
- Sobre azul primario y sobre la barra admin: texto **blanco**.
- Sobre el dorado: texto **oscuro** (`#0F172A`), nunca blanco.
- Un estado nunca se comunica solo con color: siempre con texto o ícono.

### 1.3 Tipografía

**Inter** para toda la interfaz.

| Elemento | Tamaño · peso |
|---|---|
| Título de página | 28 px · 700 |
| Título de sección o tarjeta | 20 px · 600 |
| Texto normal | 16 px · 400 |
| Etiquetas, tablas, ayudas | 14 px · 400–500 |
| Precio en tarjeta de producto | 20 px · 700 |
| Precio en detalle de producto | 32 px · 700 |
| Números de comprobante y códigos | Monoespaciada, 14 px |

### 1.4 Formas, espacio y sombras

- Espaciado en múltiplos de 4 px (8, 12, 16, 24, 32, 48).
- Bordes redondeados: **8 px** en botones y campos, **12 px** en tarjetas y modales, **999 px** en insignias y chips.
- Sombra suave en tarjetas: `0 1px 3px rgba(15, 23, 42, 0.08)`; un poco más marcada al pasar el mouse.
- Contenido centrado con ancho máximo de **1200 px** en la tienda. El panel admin usa todo el ancho disponible.
- Íconos de línea, estilo **Lucide**, de 20 px.

### 1.5 Componentes

| Componente | Cómo se ve |
|---|---|
| **Botón primario** | Fondo azul `#1E3A8A`, texto blanco, 44 px de alto, radio 8 px |
| **Botón de acento** | Fondo dorado `#F59E0B`, texto oscuro |
| **Botón secundario** | Fondo blanco, borde `#E2E8F0`, texto azul |
| **Botón de peligro** | Fondo `#B91C1C`, texto blanco (solo para acciones irreversibles) |
| **Campo de texto** | Etiqueta arriba (14 px · 500), campo de 44 px, borde gris; con error: borde rojo y mensaje rojo debajo |
| **Insignia (badge)** | Píldora con fondo suave y texto del color del estado, 12 px · 600 |
| **Chip de filtro** | Píldora azul suave con texto azul y una "×" para quitarlo |
| **Tarjeta de producto** | Imagen cuadrada arriba (fondo gris muy claro si no hay foto), marca en gris pequeño, nombre en 2 líneas como máximo, precio grande, insignia de stock y botón "Agregar al carrito" de ancho completo |
| **Tabla** | Encabezado con fondo `#F8FAFC` y texto gris en mayúsculas pequeñas; filas de 56 px con separador; acciones a la derecha con íconos; paginación abajo ("Mostrando 1–10 de 48" + flechas) |
| **Modal** | Tarjeta centrada de 480 px sobre un velo oscuro semitransparente; título, contenido, botones a la derecha (secundario "Cancelar" + principal) |
| **Aviso (toast)** | Tarjeta pequeña arriba a la derecha, con ícono y borde izquierdo del color del estado |
| **Estado vacío** | Ícono grande gris, título, una línea de explicación y un botón |
| **Esqueleto de carga** | Bloques grises redondeados en lugar del contenido |
| **Migas de pan** | "Inicio / Catálogo / Cámaras" en gris, la última en negro |

### Prompt base (pegar siempre primero)

```
Diseña UNA pantalla web de escritorio (1440 px de ancho) para "IntiGuard", tienda en línea peruana de equipos de seguridad electrónica. Todo el texto en español de Perú.

ESTILO
- Tipografía: SOLO Inter en todos los textos (no uses Manrope ni ninguna otra).
- Fondo de página #F8FAFC. Tarjetas blancas #FFFFFF con borde 1 px #E2E8F0, radio 12 px y sombra muy suave.
- Color primario azul #1E3A8A (hover #1E40AF). Acento dorado #F59E0B, siempre con texto oscuro #0F172A encima.
- Texto principal #0F172A; texto secundario #475569; bordes #E2E8F0.
- Estados: éxito #15803D sobre #DCFCE7; advertencia #B45309 sobre #FEF3C7; error #B91C1C sobre #FEE2E2; información #0369A1 sobre #E0F2FE.
- Botones de 44 px de alto y radio 8 px. Botón principal: fondo azul #1E3A8A y texto blanco. Botón secundario: fondo blanco, borde #E2E8F0 y texto azul.
- El dorado se usa SOLO donde el pedido lo indique (por ejemplo, la insignia del carrito o el botón "Crear cuenta"). Los botones de compra ("Agregar al carrito", "Confirmar compra") son azules.
- Insignias en forma de píldora: "En stock" verde, "Últimas unidades" ámbar, "Agotado" rojo.
- Íconos de línea, estilo Lucide, de 20 px.
- Moneda: "S/ " con espacio y sin punto (ejemplo "S/ 1,199.00").
- Logo: un escudo con un sol dorado en el centro, seguido UNA sola vez del texto "IntiGuard" ("Inti" en dorado, "Guard" en blanco sobre fondo oscuro o azul, o en azul sobre fondo claro). No repitas el nombre junto al logo.
- Fotos de producto: foto de estudio del equipo sobre fondo blanco o gris muy claro, sin textos dentro de la imagen.

REGLAS (obligatorias)
1. Usa EXACTAMENTE los textos que van entre comillas. No los cambies, no los traduzcas y no los "mejores".
2. Incluye SOLO las secciones que pide el prompt, en el orden indicado. No agregues secciones, banners, carruseles, etiquetas pequeñas encima de los títulos, sellos, contadores, cifras ni enlaces que no estén pedidos.
3. No inventes información del negocio. Está PROHIBIDO mostrar: envíos o delivery, instalación, asesoría técnica, cotizaciones, ofertas, descuentos o cupones, métodos de pago (Visa, Mastercard, Yape, Plin, bancos), años o meses de garantía, direcciones, horarios, WhatsApp, RUC, libro de reclamaciones, redes sociales, cantidad de productos por categoría o descripciones de producto que no estén en el pedido.
4. Las ÚNICAS categorías de la tienda son 6, con estos nombres exactos: "Cámaras", "Alarmas", "Sensores de movimiento", "Detectores de humo", "Cerraduras digitales", "Control de acceso".
5. El año es 2026.
```

---

## 2. Marcos que se repiten

### 2.1 Marco de la tienda

Se usa en todas las vistas de visitante y cliente.

**Barra superior** (72 px, fondo azul `#1E3A8A`, texto blanco, fija al hacer scroll):
- **Izquierda:** logo IntiGuard.
- **Centro:** buscador de 480 px con fondo blanco, ícono de lupa y placeholder "Buscar cámaras, alarmas, cerraduras…".
- **Derecha**, según quién mira:
  - **Visitante:** enlace "Catálogo", botón blanco con borde "Iniciar sesión", botón dorado "Crear cuenta".
  - **Cliente:** enlace "Catálogo", enlace "Mis pedidos", ícono de carrito con una insignia dorada redonda con la cantidad (por ejemplo "3"), y un avatar circular con las iniciales "AT" que abre un menú: "Mi perfil" y "Cerrar sesión".
  - **Administrador:** enlace "Catálogo", botón dorado "Panel de administración" y el avatar con menú. Sin carrito.

**Barra de categorías** (48 px, debajo de la superior, fondo blanco, borde inferior gris): las 6 categorías como enlaces con ícono: Cámaras · Alarmas · Sensores de movimiento · Detectores de humo · Cerraduras digitales · Control de acceso.

**Pie de página** (fondo `#0F172A`, texto gris claro, 4 columnas):
1. Logo en blanco y "Seguridad para tu hogar y tu negocio".
2. **Tienda:** Catálogo · Mis pedidos · Mi perfil.
3. **Ayuda:** Boletas y facturas · Garantía · Preguntas frecuentes.
4. **Contacto:** "ventas@intiguard.pe" · "+51 1 555 0123" · "Lima, Perú".

Línea final: "© 2026 IntiGuard. Todos los precios incluyen IGV."

**En móvil:** la barra superior queda con el logo, la lupa, el carrito y un menú hamburguesa; la barra de categorías pasa a ser un carrusel horizontal; el pie se apila en una columna.

#### Prompt del marco de tienda

```
MARCO DE TIENDA (se repite en todas las vistas de la tienda; usa SOLO la versión que indique la vista)

BARRA SUPERIOR: 72 px de alto, fondo azul #1E3A8A, fija arriba, contenido centrado a 1200 px.
- Izquierda: el logo IntiGuard.
- Centro: buscador de 480 px, fondo blanco, radio 8 px, ícono de lupa y el texto de ejemplo "Buscar cámaras, alarmas, cerraduras…".
- Derecha, según la versión:
  - VERSIÓN VISITANTE: enlace blanco "Catálogo", botón con borde blanco "Iniciar sesión" y botón dorado "Crear cuenta". SIN carrito y SIN avatar.
  - VERSIÓN CLIENTE: enlace blanco "Catálogo", enlace blanco "Mis pedidos", ícono de carrito con una insignia dorada redonda con el número que indique la vista, y avatar circular con las iniciales "AT".
  - VERSIÓN ADMIN: enlace blanco "Catálogo", botón dorado "Panel de administración" y avatar circular con las iniciales "MA". SIN carrito.

BARRA DE CATEGORÍAS: 48 px de alto, fondo blanco, borde inferior #E2E8F0. Exactamente 6 enlaces con ícono, en este orden: "Cámaras", "Alarmas", "Sensores de movimiento", "Detectores de humo", "Cerraduras digitales", "Control de acceso". Ninguna categoría más.

PIE DE PÁGINA: fondo #0F172A, texto #CBD5E1, 4 columnas:
1. Logo IntiGuard y la frase "Seguridad para tu hogar y tu negocio".
2. Título "Tienda" y los enlaces "Catálogo", "Mis pedidos", "Mi perfil".
3. Título "Ayuda" y los enlaces "Boletas y facturas", "Garantía", "Preguntas frecuentes".
4. Título "Contacto" y las líneas "ventas@intiguard.pe", "+51 1 555 0123", "Lima, Perú".
Debajo, una línea separadora y el texto "© 2026 IntiGuard. Todos los precios incluyen IGV."
En el pie NO va nada más: ni dirección, ni horario, ni WhatsApp, ni RUC, ni métodos de pago, ni redes sociales.
```

### 2.2 Marco del panel de administración

Se usa en todas las vistas que empiezan con "Admin".

**Barra lateral izquierda** (260 px, fondo `#0F172A`, texto `#E2E8F0`, alto completo):
- **Arriba:** logo IntiGuard en versión clara y la etiqueta pequeña "Administración".
- **Menú** con ícono + texto:
  - Resumen
  - **Catálogo:** Productos · Marcas y categorías
  - **Inventario:** Stock · Movimientos
  - **Ventas:** Ventas · Comprobantes · Reportes
  - **Usuarios**
- La opción activa tiene fondo `#1E3A8A` y una barra dorada de 3 px a la izquierda.
- **Abajo:** "Ver tienda" (ícono de salida) y "Cerrar sesión".

**Barra superior** (64 px, fondo blanco, borde inferior):
- **Izquierda:** migas de pan ("Administración / Productos").
- **Derecha:** campana de notificaciones con un punto rojo, y el avatar del administrador ("MA", "Mathias · Administrador").

**Área de contenido:** fondo `#F8FAFC`, márgenes de 32 px. Cada vista empieza con el **título de la página** a la izquierda y los **botones de acción** a la derecha (por ejemplo "+ Nuevo producto").

**En móvil:** la barra lateral se oculta y se abre con un botón de menú en la barra superior.

#### Prompt del marco del panel de administración

```
MARCO DEL PANEL DE ADMINISTRACIÓN (se repite en todas las vistas del panel)

BARRA LATERAL IZQUIERDA: 260 px de ancho, alto completo, fondo #0F172A, texto #E2E8F0.
- Arriba: logo IntiGuard en versión clara y, debajo, la etiqueta pequeña "Administración".
- Menú con ícono y texto, en este orden y con estos grupos (los títulos de grupo en gris, pequeños y en mayúsculas):
  "Resumen"
  CATÁLOGO: "Productos", "Marcas y categorías"
  INVENTARIO: "Stock", "Movimientos"
  VENTAS: "Ventas", "Comprobantes", "Reportes"
  "Usuarios"
- La opción activa (la que indique la vista) tiene fondo #1E3A8A y una barra dorada #F59E0B de 3 px a la izquierda.
- Abajo del todo: "Ver tienda" y "Cerrar sesión".

BARRA SUPERIOR: 64 px de alto, fondo blanco, borde inferior #E2E8F0.
- Izquierda: migas de pan con el texto que indique la vista.
- Derecha: ícono de campana con un punto rojo, y avatar circular "MA" con el texto "Mathias" y debajo "Administrador".

ÁREA DE CONTENIDO: fondo #F8FAFC, márgenes de 32 px, usa todo el ancho disponible. Cada vista empieza con el título de la página a la izquierda y sus botones de acción a la derecha.
El panel no tiene pie de página.
```

---

## 3. Datos de ejemplo

Usar estos mismos datos en todas las vistas, para que sean coherentes.

**Productos**

| # | Producto | Marca | Categoría | Precio | Stock |
|---|---|---|---|---|---|
| 1 | Cámara IP Wi-Fi 1080p | Ezviz | Cámaras | S/ 159.90 | 10 |
| 2 | Cámara bala exterior 4 MP | Hikvision | Cámaras | S/ 289.00 | 3 |
| 3 | Kit de 4 cámaras con grabador | Dahua | Cámaras | S/ 1,199.00 | 10 |
| 4 | Alarma Wi-Fi para hogar | Imou | Alarmas | S/ 349.90 | 0 |
| 5 | Sensor de movimiento PIR | Honeywell | Sensores de movimiento | S/ 89.90 | 25 |
| 6 | Detector de humo fotoeléctrico | Honeywell | Detectores de humo | S/ 69.90 | 40 |
| 7 | Cerradura digital con huella | Yale | Cerraduras digitales | S/ 899.00 | 5 |
| 8 | Control de acceso con tarjeta | Hikvision | Control de acceso | S/ 459.00 | 12 |

**Insignias de stock:** "En stock" (verde) con más de 5 unidades · "Últimas unidades" (ámbar) con 1 a 5 · "Agotado" (rojo) con 0.

**Cliente de ejemplo:** Ana Torres · ana.torres@gmail.com · 987 654 321 · Av. Arequipa 1234, Miraflores, Lima · iniciales "AT".

**Administrador de ejemplo:** Mathias Alayo · iniciales "MA".

**Pedido de ejemplo:** pedido **#00042** del 15/10/2026 14:35 · 2 × Cámara bala exterior 4 MP · **Subtotal S/ 489.83 · IGV S/ 88.17 · Total S/ 578.00** · **Boleta B001-00000001** · DNI 45678912 · estado "Confirmada".

**Factura de ejemplo:** F001-00000007 · RUC 20601234567 · "Seguridad Integral del Sur S.A.C.".

---

## 4. Vistas de la tienda

### 4.1 Inicio

**Para qué sirve:** presentar la tienda, inspirar confianza y llevar al catálogo.
**Quién la ve:** todos (el ejemplo es un visitante).
**Marco:** tienda.

**Secciones, de arriba hacia abajo:**
1. **Hero** (480 px de alto, a dos columnas):
   - **Izquierda:** título "Seguridad para tu hogar y tu negocio" (48 px · 700, azul oscuro); subtítulo "Cámaras, alarmas y cerraduras inteligentes con boleta o factura electrónica y stock real."; botones "Ver catálogo" (primario azul) y "Crear cuenta" (secundario).
   - **Derecha:** imagen de una cámara de seguridad moderna y una cerradura digital sobre fondo azul suave, con un sol dorado difuminado detrás.
2. **Categorías:** título "Compra por categoría"; 6 tarjetas en una fila, cada una con un ícono grande azul sobre círculo azul suave, el nombre de la categoría y "Ver productos →".
3. **Productos destacados:** título "Destacados" con el enlace "Ver todo →" a la derecha; 4 tarjetas de producto (Kit de 4 cámaras, Cerradura digital con huella, Cámara bala exterior con "Últimas unidades", Alarma Wi-Fi con "Agotado" y el botón deshabilitado).
4. **Beneficios** (fondo blanco, 4 columnas con ícono dorado): "Boleta o factura electrónica" · "Stock en tiempo real" · "Compra segura" · "Garantía de fábrica".
5. **Banda de llamada a la acción** (fondo azul `#1E3A8A`, texto blanco): "¿Equipas una empresa? Pide tu factura con RUC al comprar." y un botón dorado "Explorar catálogo".

**En móvil:** el hero se apila (imagen debajo del texto); categorías en carrusel horizontal; destacados en 2 columnas.

**Prompt para Stitch:**
```
VISTA: Inicio de la tienda.
MARCO: tienda, VERSIÓN VISITANTE (sin carrito, sin avatar), con barra de categorías y pie de página.

CONTENIDO entre la barra de categorías y el pie: SOLO estas 5 secciones, en este orden.

1. HERO (480 px de alto, dos columnas, fondo #F8FAFC):
   - Columna izquierda: título "Seguridad para tu hogar y tu negocio" (48 px, peso 700, color #1E3A8A); debajo el subtítulo "Cámaras, alarmas y cerraduras inteligentes con boleta o factura electrónica y stock real." (18 px, #475569); debajo dos botones: "Ver catálogo" (principal azul) y "Crear cuenta" (secundario). Nada más en esta columna: sin etiqueta encima del título y sin línea debajo de los botones.
   - Columna derecha: una foto de estudio de una cámara de seguridad blanca y una cerradura digital negra, sobre fondo azul muy suave #DBEAFE, con un sol dorado difuminado detrás. Sin textos, sellos ni recuadros encima de la foto.

2. COMPRA POR CATEGORÍA:
   - Título "Compra por categoría" (28 px, 700). Sin subtítulo y sin enlace "Ver todas".
   - UNA fila de 6 tarjetas blancas iguales. Cada tarjeta tiene SOLO: un ícono azul de 32 px dentro de un círculo #DBEAFE, el nombre de la categoría (16 px, 600) y el enlace azul "Ver productos →".
   - Las 6 tarjetas, en orden: "Cámaras" (ícono de cámara), "Alarmas" (sirena), "Sensores de movimiento" (ondas), "Detectores de humo" (detector), "Cerraduras digitales" (cerradura), "Control de acceso" (tarjeta de identificación).
   - Sin cantidades de productos ni descripciones.

3. DESTACADOS:
   - Título "Destacados" a la izquierda y el enlace azul "Ver todo →" a la derecha. Sin flechas de carrusel y sin etiqueta encima del título.
   - UNA fila de 4 tarjetas de producto. Cada tarjeta, de arriba abajo: foto cuadrada del producto; la marca en mayúsculas pequeñas grises; el nombre (máximo 2 líneas); el precio (20 px, 700); la insignia de stock; un botón de ancho completo. Sin descripción del producto.
     a. Marca "DAHUA", nombre "Kit de 4 cámaras con grabador", precio "S/ 1,199.00", insignia verde "En stock", botón azul "Agregar al carrito".
     b. Marca "YALE", nombre "Cerradura digital con huella", precio "S/ 899.00", insignia ámbar "Últimas unidades", botón azul "Agregar al carrito".
     c. Marca "HIKVISION", nombre "Cámara bala exterior 4 MP", precio "S/ 289.00", insignia ámbar "Últimas unidades", botón azul "Agregar al carrito".
     d. Marca "IMOU", nombre "Alarma Wi-Fi para hogar", precio "S/ 349.90", insignia roja "Agotado", botón gris deshabilitado "Agotado". Una sola insignia: sin cintas ni sellos sobre la foto.

4. BENEFICIOS: una franja blanca con 4 columnas. Cada columna tiene SOLO un ícono dorado de 24 px y un título en negrita, sin texto descriptivo debajo:
   "Boleta o factura electrónica" · "Stock en tiempo real" · "Compra segura" · "Garantía de fábrica".

5. BANDA FINAL: franja de ancho completo con fondo azul #1E3A8A, padding vertical de 48 px. A la izquierda, SOLO el texto blanco "¿Equipas una empresa? Pide tu factura con RUC al comprar." (24 px, 600). A la derecha, el botón dorado con texto oscuro "Explorar catálogo". Sin etiqueta encima y sin párrafo adicional.

NO AGREGUES: carrito en la barra superior, etiquetas encima de los títulos, cantidades de productos, descripciones de productos, testimonios, marcas aliadas, newsletter, envíos, pagos, garantía con plazos, cotizaciones ni ninguna otra sección.
```

### 4.2 Catálogo

**Para qué sirve:** buscar y filtrar productos.
**Quién la ve:** todos (el ejemplo es un cliente con sesión iniciada).
**Marco:** tienda.

**Secciones:**
1. **Migas de pan:** "Inicio / Catálogo / Cámaras".
2. **Encabezado:** título "Cámaras" y, debajo, "3 productos" en gris. A la derecha, el selector "Ordenar por: Relevancia ▾" (opciones: Precio: menor a mayor, Precio: mayor a menor, Nombre A–Z).
3. **Diseño a dos columnas:**
   - **Columna de filtros** (280 px, tarjeta blanca):
     - **Categoría:** las 6 opciones como lista con radio y la cantidad entre paréntesis; "Cámaras (3)" marcada.
     - **Marca:** casillas: Hikvision, Dahua, Ezviz, Imou, Honeywell, Yale.
     - **Precio:** dos campos, "Mín." y "Máx.", y un control deslizante.
     - **Disponibilidad:** casilla "Solo con stock".
     - Botón secundario de ancho completo "Limpiar filtros".
   - **Resultados:**
     - Fila de **chips activos**: "Cámaras ×" y "Solo con stock ×".
     - **Grilla de 3 columnas** de tarjetas de producto: Cámara IP Wi-Fi 1080p, Cámara bala exterior 4 MP ("Últimas unidades"), Kit de 4 cámaras con grabador.
     - **Paginación** centrada: "‹ 1 2 3 ›".

**Variante · sin resultados:** en lugar de la grilla, el estado vacío con ícono de lupa, "No encontramos productos con esos filtros", "Prueba quitando algún filtro o buscando otra palabra." y el botón "Limpiar filtros".

**Variante · cargando:** 6 tarjetas esqueleto.

**En móvil:** los filtros se abren desde un botón "Filtros (2)" en un panel inferior; grilla de 2 columnas.

**Prompt para Stitch:**
```
VISTA: Catálogo de productos.
MARCO: tienda, VERSIÓN CLIENTE con el carrito en "3", con barra de categorías (la categoría "Cámaras" resaltada) y pie de página.

CONTENIDO, SOLO esto y en este orden:
1. Migas de pan: "Inicio / Catálogo / Cámaras".
2. Encabezado: a la izquierda, el título "Cámaras" (28 px, 700) y debajo "3 productos" en gris. A la derecha, un selector con el texto "Ordenar por: Relevancia".
3. Dos columnas:
   - COLUMNA DE FILTROS (280 px, tarjeta blanca), con estos 4 bloques separados por líneas finas:
     · "Categoría": lista de 6 opciones con radio: "Cámaras (3)" (marcada), "Alarmas (1)", "Sensores de movimiento (1)", "Detectores de humo (1)", "Cerraduras digitales (1)", "Control de acceso (1)".
     · "Marca": casillas "Hikvision", "Dahua", "Ezviz", "Imou", "Honeywell", "Yale".
     · "Precio": dos campos pequeños "Mín." y "Máx." en una fila.
     · "Disponibilidad": casilla marcada "Solo con stock".
     · Al final, el botón secundario de ancho completo "Limpiar filtros".
   - RESULTADOS:
     · Una fila de chips azules suaves con "×": "Cámaras" y "Solo con stock".
     · Una grilla de 3 tarjetas de producto (foto, marca en mayúsculas grises, nombre, precio, insignia, botón azul "Agregar al carrito"):
       "EZVIZ" · "Cámara IP Wi-Fi 1080p" · "S/ 159.90" · verde "En stock";
       "HIKVISION" · "Cámara bala exterior 4 MP" · "S/ 289.00" · ámbar "Últimas unidades";
       "DAHUA" · "Kit de 4 cámaras con grabador" · "S/ 1,199.00" · verde "En stock".
     · Debajo, la paginación centrada: "‹ 1 ›".

NO AGREGUES: banners promocionales, descripciones de productos, valoraciones con estrellas, comparadores, filtros distintos de los 4 indicados ni productos de otras categorías.
```

### 4.3 Detalle de producto

**Para qué sirve:** ver toda la información de un producto y agregarlo al carrito.
**Quién la ve:** todos (el ejemplo es un cliente).
**Marco:** tienda.

**Secciones:**
1. **Migas de pan:** "Inicio / Cámaras / Cámara bala exterior 4 MP".
2. **Bloque principal a dos columnas:**
   - **Galería** (izquierda, 560 px): imagen grande cuadrada de una cámara bala blanca para exteriores; debajo, 4 miniaturas.
   - **Información** (derecha):
     - Marca "HIKVISION" en gris pequeño en mayúsculas; insignia "Cámaras".
     - Nombre "Cámara bala exterior 4 MP" (28 px · 700).
     - Precio "S/ 289.00" (32 px · 700) y, al lado, "Incluye IGV" en gris.
     - Insignia ámbar "Últimas unidades: quedan 3".
     - Descripción corta: "Cámara IP67 para exteriores con detección de personas."
     - **Cantidad:** selector con "−", "1", "+" (máximo 3).
     - Botón primario grande "Agregar al carrito" con ícono de carrito.
     - Tres líneas con ícono: "Boleta o factura electrónica" · "Garantía de fábrica" · "Stock reservado al confirmar tu compra".
3. **Pestañas:** "Descripción" (activa) · "Especificaciones". En "Especificaciones", una tabla de dos columnas: Resolución 4 MP · Protección IP67 · Visión nocturna 30 m · Conexión PoE / Wi-Fi.
4. **Productos relacionados:** título "También te puede interesar" y 4 tarjetas de producto.

**Variante · agotado:** insignia roja "Agotado", selector de cantidad deshabilitado y botón gris "No disponible".

**Variante · producto descontinuado:** aviso informativo arriba: "Este producto ya no está disponible", sin selector ni botón de compra.

**En móvil:** una sola columna (galería, información, pestañas); el botón "Agregar al carrito" queda fijo abajo de la pantalla.

**Prompt para Stitch:**
```
VISTA: Detalle de producto.
MARCO: tienda, VERSIÓN CLIENTE con el carrito en "3", con barra de categorías y pie de página.

CONTENIDO, SOLO esto y en este orden:
1. Migas de pan: "Inicio / Cámaras / Cámara bala exterior 4 MP".
2. BLOQUE PRINCIPAL, a dos columnas:
   - Izquierda (560 px): foto grande cuadrada de una cámara bala blanca para exteriores sobre fondo gris muy claro, y debajo 4 miniaturas de la misma cámara desde otros ángulos.
   - Derecha, de arriba abajo:
     · La marca "HIKVISION" en mayúsculas pequeñas grises y, al lado, la insignia gris "Cámaras".
     · El nombre "Cámara bala exterior 4 MP" (28 px, 700).
     · El precio "S/ 289.00" (32 px, 700) y, a su lado en gris, "Incluye IGV".
     · La insignia ámbar "Últimas unidades: quedan 3".
     · El texto "Cámara IP67 para exteriores con detección de personas."
     · La etiqueta "Cantidad" y un selector con "−", "1" y "+".
     · El botón azul grande de ancho completo "Agregar al carrito", con ícono de carrito.
     · Tres líneas con ícono de check verde: "Boleta o factura electrónica", "Garantía de fábrica", "Stock reservado al confirmar tu compra".
3. PESTAÑAS "Descripción" y "Especificaciones", con "Especificaciones" activa y su tabla de 2 columnas y 4 filas: "Resolución" "4 MP" · "Protección" "IP67" · "Visión nocturna" "30 m" · "Conexión" "PoE / Wi-Fi".
4. RELACIONADOS: título "También te puede interesar" y una fila de 4 tarjetas de producto:
   "EZVIZ" "Cámara IP Wi-Fi 1080p" "S/ 159.90" verde "En stock";
   "DAHUA" "Kit de 4 cámaras con grabador" "S/ 1,199.00" verde "En stock";
   "HONEYWELL" "Sensor de movimiento PIR" "S/ 89.90" verde "En stock";
   "HIKVISION" "Control de acceso con tarjeta" "S/ 459.00" verde "En stock".

NO AGREGUES: valoraciones o reseñas, preguntas y respuestas, plazos de garantía, envíos, instalación, cuotas o métodos de pago, ni botones de "comprar ahora" o "cotizar".
```

### 4.4 Iniciar sesión

**Para qué sirve:** entrar con correo y contraseña.
**Quién la ve:** visitante.
**Marco:** pantalla dividida, **sin** la barra de categorías ni el pie.

**Secciones:**
- **Panel izquierdo** (50 %, fondo azul `#1E3A8A` con un sol dorado grande y difuminado): logo en blanco, frase "Protege lo que más importa." (36 px, blanco) y tres líneas con check dorado: "Compra con boleta o factura" · "Sigue tus pedidos" · "Descarga tus comprobantes".
- **Panel derecho** (50 %, fondo blanco, formulario centrado de 400 px):
  - Título "Iniciar sesión" y subtítulo "Ingresa a tu cuenta de IntiGuard".
  - Campo "Correo electrónico" (placeholder "tucorreo@ejemplo.com").
  - Campo "Contraseña" con ícono de ojo para mostrar u ocultar.
  - Botón primario de ancho completo "Iniciar sesión".
  - Texto "¿No tienes cuenta? **Crear cuenta**".
  - Enlace "← Volver a la tienda".

**Variante · credenciales incorrectas:** aviso rojo sobre el botón con ícono: "Correo o contraseña incorrectos." (el mismo mensaje si el correo no existe).

**Variante · cuenta bloqueada:** aviso ámbar con candado: "Tu cuenta está bloqueada por 15 minutos por varios intentos fallidos. Intenta de nuevo más tarde." y el botón deshabilitado.

**Variante · errores de formato:** bordes rojos con "Ingresa un correo válido" y "La contraseña debe tener al menos 8 caracteres" bajo cada campo.

**En móvil:** solo el formulario, con el logo arriba.

**Prompt para Stitch:**
```
VISTA: Iniciar sesión.
MARCO: NINGUNO. Pantalla completa dividida en dos mitades, sin barra superior, sin barra de categorías y sin pie.

1. MITAD IZQUIERDA (50 %): fondo azul #1E3A8A con un sol dorado grande y difuminado en una esquina. Contenido centrado verticalmente:
   - El logo IntiGuard en versión clara.
   - La frase "Protege lo que más importa." (36 px, 700, blanco).
   - Tres líneas con ícono de check dorado y texto blanco: "Compra con boleta o factura", "Sigue tus pedidos", "Descarga tus comprobantes".
2. MITAD DERECHA (50 %): fondo blanco, con un formulario centrado de 400 px:
   - El título "Iniciar sesión" (28 px, 700) y el subtítulo gris "Ingresa a tu cuenta de IntiGuard".
   - Un aviso rojo (fondo #FEE2E2, texto #B91C1C, ícono de alerta): "Correo o contraseña incorrectos."
   - El campo "Correo electrónico" con el texto de ejemplo "tucorreo@ejemplo.com".
   - El campo "Contraseña" con un ícono de ojo a la derecha.
   - El botón azul de ancho completo "Iniciar sesión".
   - El texto "¿No tienes cuenta?" seguido del enlace azul "Crear cuenta".
   - El enlace gris "← Volver a la tienda".

NO AGREGUES: "¿Olvidaste tu contraseña?", "Recordarme", inicio de sesión con Google, Facebook u otras redes, captcha ni textos legales.
```

### 4.5 Crear cuenta

**Para qué sirve:** registrarse como cliente.
**Quién la ve:** visitante.
**Marco:** igual que "Iniciar sesión" (pantalla dividida).

**Secciones del formulario** (panel derecho, 480 px):
- Título "Crear cuenta" y subtítulo "Compra más rápido y sigue tus pedidos".
- Fila de dos campos: "Nombres" · "Apellidos".
- "Correo electrónico".
- Fila de dos campos: "Teléfono (opcional)" · "Dirección (opcional)".
- "Contraseña", con una barra de seguridad de 4 tramos (débil / media / fuerte) y el texto "Mínimo 8 caracteres".
- "Confirmar contraseña".
- Casilla "Acepto los términos y la política de privacidad".
- Botón primario "Crear cuenta".
- "¿Ya tienes cuenta? **Iniciar sesión**".

**Variante · correo ya registrado:** el campo de correo en rojo con "Este correo ya está registrado. ¿Quieres iniciar sesión?".

**Variante · cuenta creada:** aviso verde "¡Tu cuenta fue creada! Ya puedes iniciar sesión." con botón "Iniciar sesión".

**Prompt para Stitch:**
```
VISTA: Crear cuenta.
MARCO: NINGUNO. La misma pantalla dividida de "Iniciar sesión": mitad izquierda azul #1E3A8A con sol dorado difuminado, logo claro, la frase "Protege lo que más importa." y las tres líneas con check dorado "Compra con boleta o factura", "Sigue tus pedidos", "Descarga tus comprobantes".

MITAD DERECHA: fondo blanco, formulario centrado de 480 px, SOLO con esto y en este orden:
1. El título "Crear cuenta" (28 px, 700) y el subtítulo gris "Compra más rápido y sigue tus pedidos".
2. Fila con dos campos: "Nombres" y "Apellidos".
3. El campo "Correo electrónico", en estado de error: borde rojo y, debajo, el texto rojo "Este correo ya está registrado. ¿Quieres iniciar sesión?".
4. Fila con dos campos: "Teléfono (opcional)" y "Dirección (opcional)".
5. El campo "Contraseña", con una barra de seguridad de 4 tramos debajo (2 llenos en ámbar) y la ayuda gris "Mínimo 8 caracteres".
6. El campo "Confirmar contraseña".
7. La casilla "Acepto los términos y la política de privacidad".
8. El botón azul de ancho completo "Crear cuenta".
9. El texto "¿Ya tienes cuenta?" seguido del enlace azul "Iniciar sesión".

NO AGREGUES: registro con Google u otras redes, fecha de nacimiento, documento de identidad, captcha ni campos adicionales.
```

### 4.6 Carrito

**Para qué sirve:** revisar los productos antes de comprar.
**Quién la ve:** cliente.
**Marco:** tienda.

**Secciones:**
1. Título "Tu carrito" y "3 productos" en gris.
2. **Dos columnas:**
   - **Lista de productos** (tarjeta blanca). Cada fila: miniatura de 80 px, nombre y marca, precio unitario, selector de cantidad "− 2 +", importe de la fila en negrita y un ícono de papelera. Filas de ejemplo:
     - Cámara bala exterior 4 MP · Hikvision · S/ 289.00 · 2 · **S/ 578.00**
     - Sensor de movimiento PIR · Honeywell · S/ 89.90 · 1 · **S/ 89.90**
     - Alarma Wi-Fi para hogar · Imou · S/ 349.90 · 1 · marcada con la insignia roja "Agotado" y el texto "Quita este producto para continuar".

     Debajo de la lista, el enlace "← Seguir comprando".
   - **Resumen** (tarjeta de 360 px, fija al hacer scroll):
     - Título "Resumen de compra".
     - Subtotal S/ 566.02 · IGV (18 %) S/ 101.88 · línea separadora · **Total S/ 667.90** (24 px · 700).
     - Nota en gris: "Los precios se confirman al finalizar la compra."
     - Botón primario de ancho completo "Continuar compra", **deshabilitado** mientras haya un producto agotado.

**Variante · carrito vacío:** estado vacío con ícono de carrito, "Tu carrito está vacío", "Explora el catálogo y agrega tus productos." y botón "Ver catálogo".

**En móvil:** filas apiladas; el resumen y el botón "Continuar compra" quedan fijos abajo.

**Prompt para Stitch:**
```
VISTA: Carrito.
MARCO: tienda, VERSIÓN CLIENTE con el carrito en "3", con barra de categorías y pie de página.

CONTENIDO, SOLO esto y en este orden:
1. El título "Tu carrito" (28 px, 700) y debajo "3 productos" en gris.
2. Dos columnas:
   - IZQUIERDA: una tarjeta blanca con 3 filas de producto separadas por líneas finas. Cada fila: miniatura de 80 px, nombre y marca, precio unitario, selector de cantidad "− n +", importe en negrita a la derecha e ícono de papelera.
     · "Cámara bala exterior 4 MP" · "Hikvision" · "S/ 289.00" · cantidad 2 · importe "S/ 578.00".
     · "Sensor de movimiento PIR" · "Honeywell" · "S/ 89.90" · cantidad 1 · importe "S/ 89.90".
     · "Alarma Wi-Fi para hogar" · "Imou" · "S/ 349.90" · con la insignia roja "Agotado" y el texto rojo "Quita este producto para continuar", sin selector de cantidad.
     · Debajo de la tarjeta, el enlace azul "← Seguir comprando".
   - DERECHA: una tarjeta de 360 px con el título "Resumen de compra" y, en filas: "Subtotal" "S/ 566.02"; "IGV (18 %)" "S/ 101.88"; una línea separadora; "Total" "S/ 667.90" (24 px, 700). Debajo, la nota gris "Los precios se confirman al finalizar la compra." y el botón azul de ancho completo "Continuar compra" en estado DESHABILITADO (gris).

NO AGREGUES: cupones o códigos de descuento, costo o cálculo de envío, productos recomendados, métodos de pago ni opciones de regalo.
```

### 4.7 Confirmar compra (checkout)

**Para qué sirve:** elegir el comprobante, revisar el total y confirmar.
**Quién la ve:** cliente.
**Marco:** tienda simplificado: solo el logo y el texto "Compra segura 🔒" en la barra superior, sin buscador, categorías ni pie.

**Secciones:**
1. **Indicador de pasos:** "1 Carrito ✓ — 2 Comprobante (activo) — 3 Confirmación".
2. **Dos columnas:**
   - **Comprobante** (tarjeta):
     - Título "¿Qué comprobante necesitas?".
     - Dos tarjetas seleccionables lado a lado:
       - **"Boleta":** "Para personas. DNI opcional." (seleccionada: borde azul de 2 px y fondo azul suave).
       - **"Factura":** "Para empresas. Necesitas RUC y razón social."
     - Si es **Boleta:** campo "DNI (opcional)", 8 dígitos.
     - Si es **Factura:** campos "RUC" (11 dígitos, ayuda "Empieza con 10, 15, 17 o 20") y "Razón social".
     - **Datos de contacto** (solo lectura, con el enlace "Editar perfil"): Ana Torres · ana.torres@gmail.com · 987 654 321.
   - **Resumen del pedido** (tarjeta de 400 px):
     - Lista compacta: "2 × Cámara bala exterior 4 MP · S/ 578.00", "1 × Sensor de movimiento PIR · S/ 89.90".
     - Subtotal S/ 566.02 · IGV (18 %) S/ 101.88 · **Total S/ 667.90**.
     - Botón primario grande "Confirmar compra · S/ 667.90".
     - Texto pequeño con ícono de escudo: "Tu stock queda reservado mientras confirmamos tu compra."

**Variante · procesando:** el botón muestra un indicador giratorio y "Procesando tu compra…"; el resto queda deshabilitado.

**Variante · factura con errores:** RUC en rojo con "El RUC debe tener 11 dígitos y empezar con 10, 15, 17 o 20" y razón social con "La razón social es obligatoria para factura".

**Prompt para Stitch:**
```
VISTA: Confirmar compra (checkout).
MARCO: tienda SIMPLIFICADO: barra superior azul #1E3A8A de 72 px con SOLO el logo a la izquierda y, a la derecha, un ícono de candado con el texto blanco "Compra segura". Sin buscador, sin enlaces, sin barra de categorías y sin pie de página.

CONTENIDO, SOLO esto y en este orden:
1. Un indicador de 3 pasos centrado: "1 Carrito" (completado, con check verde), "2 Comprobante" (activo, en azul), "3 Confirmación" (pendiente, en gris), unidos por líneas.
2. Dos columnas:
   - IZQUIERDA: una tarjeta con el título "¿Qué comprobante necesitas?" y:
     · Dos tarjetas seleccionables lado a lado: "Boleta" con el texto "Para personas. DNI opcional." (SELECCIONADA: borde azul de 2 px, fondo #DBEAFE y radio marcado) y "Factura" con el texto "Para empresas. Necesitas RUC y razón social." (sin seleccionar).
     · Debajo, el campo "DNI (opcional)" con el texto de ejemplo "8 dígitos".
     · Debajo, el subtítulo "Datos de contacto" y, en solo lectura, "Ana Torres", "ana.torres@gmail.com", "987 654 321", con el enlace azul "Editar perfil".
   - DERECHA: una tarjeta de 400 px con el título "Resumen del pedido" y:
     · "2 × Cámara bala exterior 4 MP" "S/ 578.00" y "1 × Sensor de movimiento PIR" "S/ 89.90".
     · "Subtotal" "S/ 566.02", "IGV (18 %)" "S/ 101.88", una línea separadora y "Total" "S/ 667.90" (24 px, 700).
     · El botón azul grande de ancho completo "Confirmar compra · S/ 667.90".
     · Debajo, con un ícono de escudo y texto gris pequeño: "Tu stock queda reservado mientras confirmamos tu compra."

NO AGREGUES: datos de tarjeta, métodos de pago, dirección o costo de envío, cupones ni pasos adicionales.
```

### 4.8 Compra confirmada

**Para qué sirve:** confirmar que la compra salió bien y dar acceso al comprobante.
**Quién la ve:** cliente.
**Marco:** tienda.

**Secciones** (tarjeta centrada de 640 px):
1. Círculo verde grande con un check blanco.
2. Título "¡Compra confirmada!" y subtítulo "Gracias, Ana. Tu pedido ya está registrado."
3. **Recuadro de datos** (fondo `#F8FAFC`, dos columnas):
   - Pedido **#00042** · Fecha 15/10/2026 14:35
   - Comprobante **Boleta B001-00000001** (monoespaciada) · Total **S/ 578.00**
4. Botones: primario "Descargar comprobante (PDF)" con ícono de descarga · secundario "Ver mi pedido".
5. Enlace "Seguir comprando".

**Prompt para Stitch:**
```
VISTA: Compra confirmada.
MARCO: tienda, VERSIÓN CLIENTE con el carrito en "0", con barra de categorías y pie de página.

CONTENIDO: SOLO una tarjeta blanca centrada de 640 px, con esto y en este orden:
1. Un círculo verde #15803D de 72 px con un check blanco.
2. El título "¡Compra confirmada!" (28 px, 700) y el subtítulo gris "Gracias, Ana. Tu pedido ya está registrado."
3. Un recuadro con fondo #F8FAFC y dos columnas de datos (etiqueta gris arriba, valor en negrita abajo): "Pedido" "#00042" · "Fecha" "15/10/2026 14:35" · "Comprobante" "Boleta B001-00000001" (el número en tipografía monoespaciada) · "Total" "S/ 578.00".
4. Dos botones en fila: el azul "Descargar comprobante (PDF)" con ícono de descarga, y el secundario "Ver mi pedido".
5. El enlace azul "Seguir comprando".

NO AGREGUES: fecha o seguimiento de envío, productos recomendados, encuestas, cupones ni compartir en redes.
```

### 4.9 Compra no completada

**Para qué sirve:** explicar que la compra no se pudo cerrar, sin alarmar.
**Quién la ve:** cliente.
**Marco:** tienda.

**Dos variantes** (tarjeta centrada de 640 px):
- **Sin stock suficiente:** círculo ámbar con un ícono de caja; título "Uno de tus productos se agotó"; texto "Mientras confirmabas, se vendieron las últimas unidades de **Cerradura digital con huella**. No se hizo ningún cargo."; botón primario "Volver al carrito".
- **No se pudo emitir el comprobante:** círculo rojo suave con un ícono de recibo tachado; título "No pudimos completar tu compra"; texto "Tuvimos un problema al emitir tu comprobante. **No se realizó ningún cargo** y los productos volvieron a estar disponibles. Intenta de nuevo en unos minutos."; botones "Intentar de nuevo" (primario) y "Volver al carrito" (secundario).

**Prompt para Stitch:**
```
VISTA: Compra no completada.
MARCO: tienda, VERSIÓN CLIENTE con el carrito en "2", con barra de categorías y pie de página.

CONTENIDO: SOLO una tarjeta blanca centrada de 640 px, con esto y en este orden:
1. Un círculo de 72 px con fondo #FEE2E2 y un ícono de recibo tachado en #B91C1C.
2. El título "No pudimos completar tu compra" (28 px, 700).
3. El texto gris: "Tuvimos un problema al emitir tu comprobante. No se realizó ningún cargo y los productos volvieron a estar disponibles. Intenta de nuevo en unos minutos." (con "No se realizó ningún cargo" en negrita).
4. Dos botones en fila: el azul "Intentar de nuevo" y el secundario "Volver al carrito".

Tono tranquilizador: rojo solo en el ícono, nunca en fondos grandes.
NO AGREGUES: códigos de error, contacto de soporte, teléfonos ni productos recomendados.
```

### 4.10 Mis pedidos

**Para qué sirve:** ver el historial de compras.
**Quién la ve:** cliente.
**Marco:** tienda.

**Secciones:**
1. Título "Mis pedidos" y subtítulo "Revisa tus compras y descarga tus comprobantes".
2. **Filtros:** pestañas "Todos (4)" · "Confirmados (3)" · "Anulados (1)"; a la derecha, el selector "Últimos 3 meses ▾".
3. **Lista de pedidos** (una tarjeta por pedido, en fila):
   - Izquierda: "Pedido #00042" en negrita y "15/10/2026" en gris.
   - Centro: miniaturas apiladas de los productos y "2 productos".
   - Insignia de estado y tipo de comprobante ("Boleta B001-00000001").
   - Derecha: total en negrita y el botón secundario "Ver detalle".

   Ejemplos:
   - #00042 · 15/10/2026 · Confirmada · Boleta B001-00000001 · S/ 578.00
   - #00039 · 02/10/2026 · Confirmada · Factura F001-00000007 · S/ 1,199.00
   - #00031 · 18/09/2026 · Anulada · sin comprobante · S/ 349.90
   - #00027 · 05/09/2026 · Confirmada · Boleta B001-00000003 · S/ 159.90

**Variante · sin pedidos:** estado vacío con ícono de bolsa, "Todavía no has comprado nada" y botón "Ver catálogo".

**Prompt para Stitch:**
```
VISTA: Mis pedidos.
MARCO: tienda, VERSIÓN CLIENTE con el carrito en "0", con barra de categorías y pie de página.

CONTENIDO, SOLO esto y en este orden:
1. El título "Mis pedidos" (28 px, 700) y el subtítulo gris "Revisa tus compras y descarga tus comprobantes".
2. Una fila con pestañas a la izquierda, "Todos (4)" (activa), "Confirmados (3)" y "Anulados (1)", y a la derecha el selector "Últimos 3 meses".
3. Una lista de 4 tarjetas en fila (una por pedido). Cada tarjeta, de izquierda a derecha: número de pedido en negrita con la fecha debajo en gris; miniaturas apiladas de los productos con el texto "n productos"; la insignia de estado; el comprobante; el total en negrita; el botón secundario "Ver detalle".
   · "Pedido #00042" · "15/10/2026" · "1 producto" · verde "Confirmada" · "Boleta B001-00000001" · "S/ 578.00".
   · "Pedido #00039" · "02/10/2026" · "1 producto" · verde "Confirmada" · "Factura F001-00000007" · "S/ 1,199.00".
   · "Pedido #00031" · "18/09/2026" · "1 producto" · roja "Anulada" · "Sin comprobante" · "S/ 349.90".
   · "Pedido #00027" · "05/09/2026" · "1 producto" · verde "Confirmada" · "Boleta B001-00000003" · "S/ 159.90".

NO AGREGUES: seguimiento de envío, "volver a comprar", valoraciones ni estados distintos de Confirmada, Pendiente y Anulada.
```

### 4.11 Detalle de mi pedido

**Para qué sirve:** ver un pedido completo y su comprobante.
**Quién la ve:** cliente.
**Marco:** tienda.

**Secciones:**
1. Migas "Mis pedidos / Pedido #00042".
2. **Encabezado:** título "Pedido #00042", insignia verde "Confirmada", "Realizado el 15/10/2026 a las 14:35". A la derecha, el botón secundario "Descargar comprobante (PDF)".
3. **Línea de tiempo horizontal** de 3 pasos con check verde: "Pedido registrado 14:35" → "Stock reservado 14:35" → "Comprobante emitido 14:35".
4. **Dos columnas:**
   - **Productos** (tarjeta): tabla con producto (miniatura y nombre), precio unitario, cantidad e importe. Una fila: Cámara bala exterior 4 MP · S/ 289.00 · 2 · S/ 578.00. Nota en gris: "Precios del día de tu compra."
   - **Comprobante** (tarjeta):
     - "Boleta electrónica".
     - Número **B001-00000001** grande, en monoespaciada.
     - Cliente: Ana Torres · DNI 45678912.
     - Subtotal S/ 489.83 · IGV (18 %) S/ 88.17 · **Total S/ 578.00**.
     - Botón primario "Descargar PDF".

**Variante · pedido anulado:** insignia roja "Anulada"; la línea de tiempo termina en un paso rojo "Compra anulada: no se realizó ningún cargo"; sin tarjeta de comprobante.

**Prompt para Stitch:**
```
VISTA: Detalle de mi pedido.
MARCO: tienda, VERSIÓN CLIENTE con el carrito en "0", con barra de categorías y pie de página.

CONTENIDO, SOLO esto y en este orden:
1. Migas de pan: "Mis pedidos / Pedido #00042".
2. Encabezado: a la izquierda, el título "Pedido #00042" con la insignia verde "Confirmada" al lado y, debajo en gris, "Realizado el 15/10/2026 a las 14:35". A la derecha, el botón secundario "Descargar comprobante (PDF)".
3. Una línea de tiempo horizontal de 3 pasos completados (círculos verdes con check), unidos por una línea verde: "Pedido registrado", "Stock reservado", "Comprobante emitido", cada uno con "14:35" debajo.
4. Dos columnas:
   - IZQUIERDA: una tarjeta "Productos" con una tabla de columnas "Producto", "Precio unitario", "Cantidad", "Importe" y una fila: miniatura + "Cámara bala exterior 4 MP", "S/ 289.00", "2", "S/ 578.00". Debajo, la nota gris "Precios del día de tu compra."
   - DERECHA: una tarjeta "Comprobante" con: "Boleta electrónica" en gris; el número "B001-00000001" grande en tipografía monoespaciada; "Ana Torres · DNI 45678912"; "Subtotal" "S/ 489.83", "IGV (18 %)" "S/ 88.17", "Total" "S/ 578.00" (en negrita); y el botón azul de ancho completo "Descargar PDF".

NO AGREGUES: envío o dirección de entrega, seguimiento de paquete, devoluciones ni soporte.
```

### 4.12 Comprobante (vista imprimible / PDF)

**Para qué sirve:** cómo se ve el comprobante descargado o impreso.
**Quién la ve:** cliente y administrador.
**Marco:** ninguno. Hoja **A4 vertical**, fondo blanco, en blanco y negro con detalles azules.

**Secciones:**
1. **Cabecera:**
   - **Izquierda:** logo IntiGuard, "IntiGuard S.A.C.", "RUC 20512345678", "Av. Javier Prado Este 1234, San Isidro, Lima".
   - **Derecha:** recuadro con borde azul: "BOLETA DE VENTA ELECTRÓNICA" y "B001-00000001".
2. **Datos:** Fecha de emisión 15/10/2026 · Cliente Ana Torres · DNI 45678912 · Pedido #00042.
3. **Tabla de detalle:** Cant. · Descripción · P. unitario · Importe. Fila: 2 · Cámara bala exterior 4 MP · S/ 289.00 · S/ 578.00.
4. **Totales** (alineados a la derecha): Op. gravada S/ 489.83 · IGV (18 %) S/ 88.17 · **Importe total S/ 578.00** · "Son: QUINIENTOS SETENTA Y OCHO CON 00/100 SOLES".
5. **Pie:** "Representación impresa de la boleta de venta electrónica." y "Gracias por tu compra".

**Variante · factura:** título "FACTURA ELECTRÓNICA", número F001-00000007, cliente "Seguridad Integral del Sur S.A.C." con RUC 20601234567.

**Prompt para Stitch:**
```
VISTA: Comprobante imprimible. NO es una página web: es UNA hoja A4 vertical, fondo blanco, márgenes de 40 px, texto negro con detalles en azul #1E3A8A. Sin barra superior, sin menú y sin pie de página web.

CONTENIDO, SOLO esto y en este orden:
1. CABECERA en dos columnas:
   - Izquierda: el logo IntiGuard y, debajo, "IntiGuard S.A.C.", "RUC 20512345678", "Av. Javier Prado Este 1234, San Isidro, Lima".
   - Derecha: un recuadro con borde azul de 2 px y texto centrado: "BOLETA DE VENTA ELECTRÓNICA" y debajo "B001-00000001" (monoespaciada, en negrita).
2. DATOS, en dos columnas de etiqueta y valor: "Fecha de emisión" "15/10/2026" · "Cliente" "Ana Torres" · "DNI" "45678912" · "Pedido" "#00042".
3. TABLA con encabezado de fondo azul muy suave y columnas "Cant.", "Descripción", "P. unitario", "Importe"; una fila: "2", "Cámara bala exterior 4 MP", "S/ 289.00", "S/ 578.00".
4. TOTALES alineados a la derecha: "Op. gravada" "S/ 489.83" · "IGV (18 %)" "S/ 88.17" · "Importe total" "S/ 578.00" (en negrita). Debajo, a todo el ancho: "Son: QUINIENTOS SETENTA Y OCHO CON 00/100 SOLES".
5. PIE de la hoja, centrado y en gris: "Representación impresa de la boleta de venta electrónica." y "Gracias por tu compra".

NO AGREGUES: código QR, código de barras, firmas, sellos, métodos de pago ni datos de envío.
```

### 4.13 Mi perfil

**Para qué sirve:** ver y editar los datos personales.
**Quién la ve:** cliente y administrador.
**Marco:** tienda.

**Secciones:**
1. Título "Mi perfil".
2. **Dos columnas:**
   - **Tarjeta de identidad** (300 px): avatar circular grande "AT" con el enlace "Cambiar foto", nombre "Ana Torres", correo, insignia "Cliente" y "Miembro desde octubre de 2026".
   - **Formulario "Datos personales"** (tarjeta):
     - Fila: "Nombres" · "Apellidos".
     - "Correo electrónico", **deshabilitado**, con la ayuda "El correo no se puede cambiar".
     - Fila: "Teléfono" · "Dirección".
     - Botones a la derecha: "Cancelar" (secundario) y "Guardar cambios" (primario).
3. **Tarjeta "Seguridad":** "Contraseña", "Última actualización: hace 2 meses" y el botón secundario "Cambiar contraseña".

**Variante · guardado:** aviso verde arriba a la derecha: "Tus datos se guardaron correctamente."

**Prompt para Stitch:**
```
VISTA: Mi perfil.
MARCO: tienda, VERSIÓN CLIENTE con el carrito en "0", con barra de categorías y pie de página.

CONTENIDO, SOLO esto y en este orden:
1. El título "Mi perfil" (28 px, 700).
2. Dos columnas:
   - IZQUIERDA: una tarjeta de 300 px, centrada, con: avatar circular grande "AT" sobre fondo #DBEAFE; el enlace azul "Cambiar foto"; "Ana Torres" en negrita; "ana.torres@gmail.com" en gris; la insignia "Cliente"; y "Miembro desde octubre de 2026" en gris pequeño.
   - DERECHA: una tarjeta "Datos personales" con: fila "Nombres" ("Ana") y "Apellidos" ("Torres"); el campo "Correo electrónico" ("ana.torres@gmail.com") DESHABILITADO, con la ayuda gris "El correo no se puede cambiar"; fila "Teléfono" ("987 654 321") y "Dirección" ("Av. Arequipa 1234, Miraflores, Lima"); y abajo a la derecha los botones "Cancelar" (secundario) y "Guardar cambios" (azul).
3. Debajo de la columna derecha, una tarjeta "Seguridad" con "Contraseña", el texto gris "Última actualización: hace 2 meses" y el botón secundario "Cambiar contraseña".
4. Un aviso verde (toast) arriba a la derecha de la pantalla: "Tus datos se guardaron correctamente."

NO AGREGUES: métodos de pago guardados, direcciones de envío múltiples, puntos, preferencias de notificación ni eliminar cuenta.
```

### 4.14 Páginas de error

**Marco:** tienda.

Tarjeta centrada con una ilustración simple en azul y dorado:

| Página | Título | Texto | Botón |
|---|---|---|---|
| **404** | "No encontramos esta página" | "Puede que el enlace esté mal escrito o que el producto ya no exista." | "Volver al inicio" |
| **Sin permiso** | "No tienes permiso para ver esta página" | "Si crees que es un error, inicia sesión con otra cuenta." | "Ir a la tienda" |
| **Sesión expirada** | "Tu sesión expiró" | "Por seguridad, cerramos tu sesión después de 1 hora." | "Iniciar sesión de nuevo" |
| **Sin conexión** | "Algo salió mal" | "No pudimos conectar con la tienda. Intenta de nuevo en unos minutos." | "Reintentar" |

**Prompt para Stitch:**
```
VISTA: Página de error 404.
MARCO: tienda, VERSIÓN VISITANTE, con barra de categorías y pie de página.

CONTENIDO: SOLO un bloque centrado (ancho 560 px, padding vertical de 80 px) con esto y en este orden:
1. Una ilustración simple de línea: un escudo con un sol dorado y una lupa, en azul #1E3A8A y dorado #F59E0B.
2. El número "404" grande (72 px, 700) en azul suave #DBEAFE con borde azul.
3. El título "No encontramos esta página" (28 px, 700).
4. El texto gris "Puede que el enlace esté mal escrito o que el producto ya no exista."
5. El botón azul "Volver al inicio".

NO AGREGUES: buscador extra, productos sugeridos, contacto ni enlaces adicionales.

(Para las otras páginas de error, usa este mismo prompt cambiando solo el número o ícono, el título, el texto y el botón según la tabla de esta sección.)
```

---

## 5. Vistas del panel de administración

Todas usan el **marco del panel de administración** (sección 2.2).

### 5.1 Resumen (dashboard)

**Para qué sirve:** ver de un vistazo cómo va la tienda.

**Secciones:**
1. Título "Resumen" y subtítulo "Hoy, 15 de octubre de 2026". A la derecha, el selector de período "Hoy ▾".
2. **4 tarjetas de indicadores** (en fila, cada una con ícono en un círculo de color suave):
   - "Ventas de hoy": **S/ 2,847.80** · "12 ventas" · "▲ 18 % vs. ayer" en verde.
   - "Ventas del mes": **S/ 38,420.50** · "164 ventas".
   - "Comprobantes emitidos hoy": **12** · "9 boletas · 3 facturas".
   - "Productos agotados": **1** en rojo · enlace "Ver stock".
3. **Fila de dos tarjetas:**
   - **"Ventas de los últimos 7 días"** (2/3 del ancho): gráfico de barras azules, una por día (Jue a Mié), con el monto al pasar el mouse.
   - **"Stock que necesita atención"** (1/3): lista con insignias: Alarma Wi-Fi para hogar · Agotado; Cámara bala exterior 4 MP · 3 unidades; Cerradura digital con huella · 5 unidades. Enlace "Ir a stock →".
4. **"Últimas ventas"** (tabla de ancho completo): Pedido · Cliente · Fecha · Comprobante · Estado · Total, con 5 filas (#00042 Ana Torres · Boleta B001-00000001 · Confirmada · S/ 578.00, …) y el enlace "Ver todas →".

**Prompt para Stitch:**
```
VISTA: Resumen (dashboard) del panel de administración.
MARCO: panel de administración, opción activa "Resumen", migas "Administración / Resumen".

CONTENIDO, SOLO esto y en este orden:
1. Encabezado: a la izquierda, el título "Resumen" (28 px, 700) y debajo en gris "Hoy, 15 de octubre de 2026". A la derecha, el selector "Hoy".
2. Una fila de 4 tarjetas de indicadores iguales. Cada una: ícono en un círculo de color suave, etiqueta gris, valor grande (28 px, 700) y una línea pequeña debajo.
   · "Ventas de hoy" · "S/ 2,847.80" · "12 ventas · ▲ 18 % vs. ayer" (el porcentaje en verde).
   · "Ventas del mes" · "S/ 38,420.50" · "164 ventas".
   · "Comprobantes emitidos hoy" · "12" · "9 boletas · 3 facturas".
   · "Productos agotados" · "1" (en rojo) · enlace azul "Ver stock".
3. Una fila de dos tarjetas:
   - "Ventas de los últimos 7 días" (2/3 del ancho): gráfico de barras azules #1E3A8A, una barra por día con las etiquetas "Jue", "Vie", "Sáb", "Dom", "Lun", "Mar", "Mié", y el eje vertical en soles.
   - "Stock que necesita atención" (1/3 del ancho): lista de 3 filas con nombre e insignia: "Alarma Wi-Fi para hogar" roja "Agotado"; "Cámara bala exterior 4 MP" ámbar "3 unidades"; "Cerradura digital con huella" ámbar "5 unidades"; y al final el enlace azul "Ir a stock →".
4. Una tarjeta de ancho completo "Últimas ventas", con el enlace azul "Ver todas →" a la derecha del título y una tabla de columnas "Pedido", "Cliente", "Fecha", "Comprobante", "Estado", "Total", con 5 filas:
   "#00043" "Carla Díaz" "15/10/2026 15:02" "—" ámbar "Pendiente" "S/ 159.90";
   "#00042" "Ana Torres" "15/10/2026 14:35" "Boleta B001-00000001" verde "Confirmada" "S/ 578.00";
   "#00041" "Seguridad Integral del Sur S.A.C." "15/10/2026 12:10" "Factura F001-00000008" verde "Confirmada" "S/ 2,398.00";
   "#00040" "Luis Ramos" "14/10/2026 16:20" "—" roja "Anulada" "S/ 899.00";
   "#00039" "Ana Torres" "02/10/2026 11:45" "Factura F001-00000007" verde "Confirmada" "S/ 1,199.00".

NO AGREGUES: mapas, tráfico web, visitantes, conversión, envíos, metas, tareas ni ninguna otra tarjeta o gráfico.
```

### 5.2 Productos

**Para qué sirve:** ver y gestionar todos los productos.

**Secciones:**
1. Título "Productos" y "9 productos" (8 activos y 1 inactivo); a la derecha, el botón primario "+ Nuevo producto".
2. **Barra de herramientas** (tarjeta): buscador "Buscar por nombre…", selector "Categoría: Todas ▾", selector "Marca: Todas ▾", interruptor "Mostrar inactivos" y el botón "Exportar".
3. **Tabla:**
   - Columnas: Producto (miniatura + nombre) · Marca · Categoría · Precio · Stock (número + insignia) · Estado · Acciones.
   - Las 8 filas de los datos de ejemplo; estado "Activo" en verde; una fila extra **"Cámara domo 720p (descontinuada)"** con estado "Inactivo" en gris y texto atenuado.
   - Acciones: lápiz "Editar" y un interruptor "Activo/Inactivo" (nunca papelera: los productos no se borran).
4. **Paginación:** "Mostrando 1–9 de 9".

**Modal "Desactivar producto":** título "¿Desactivar Cámara IP Wi-Fi 1080p?"; texto "Dejará de mostrarse en la tienda. Las ventas pasadas no cambian y puedes volver a activarlo cuando quieras."; botones "Cancelar" y "Desactivar".

**Prompt para Stitch:**
```
VISTA: Productos (lista) del panel de administración.
MARCO: panel de administración, opción activa "Productos", migas "Administración / Catálogo / Productos".

CONTENIDO, SOLO esto y en este orden:
1. Encabezado: a la izquierda, el título "Productos" y debajo en gris "9 productos". A la derecha, el botón azul "+ Nuevo producto".
2. Una tarjeta de herramientas en una fila: buscador "Buscar por nombre…", selector "Categoría: Todas", selector "Marca: Todas", un interruptor ENCENDIDO con el texto "Mostrar inactivos", y el botón secundario "Exportar".
3. Una tarjeta con una tabla de columnas "Producto" (miniatura + nombre), "Marca", "Categoría", "Precio", "Stock", "Estado", "Acciones". En "Stock", el número con la insignia al lado; en "Estado", la insignia verde "Activo" o gris "Inactivo"; en "Acciones", SOLO un ícono de lápiz y un interruptor (sin papelera). Filas:
   "Cámara IP Wi-Fi 1080p" "Ezviz" "Cámaras" "S/ 159.90" "10" "Activo";
   "Cámara bala exterior 4 MP" "Hikvision" "Cámaras" "S/ 289.00" "3" ámbar "Últimas unidades" "Activo";
   "Kit de 4 cámaras con grabador" "Dahua" "Cámaras" "S/ 1,199.00" "10" "Activo";
   "Alarma Wi-Fi para hogar" "Imou" "Alarmas" "S/ 349.90" "0" roja "Agotado" "Activo";
   "Sensor de movimiento PIR" "Honeywell" "Sensores de movimiento" "S/ 89.90" "25" "Activo";
   "Detector de humo fotoeléctrico" "Honeywell" "Detectores de humo" "S/ 69.90" "40" "Activo";
   "Cerradura digital con huella" "Yale" "Cerraduras digitales" "S/ 899.00" "5" ámbar "Últimas unidades" "Activo";
   "Control de acceso con tarjeta" "Hikvision" "Control de acceso" "S/ 459.00" "12" "Activo";
   "Cámara domo 720p (descontinuada)" "Ezviz" "Cámaras" "S/ 99.90" "0" "Inactivo" (fila con texto atenuado en gris e interruptor apagado).
4. La paginación debajo de la tabla: "Mostrando 1–9 de 9".

NO AGREGUES: botón o ícono de eliminar, SKU, códigos de barras, proveedores, costos, márgenes ni columnas distintas de las indicadas.
```

### 5.3 Crear / editar producto

**Para qué sirve:** dar de alta un producto o cambiar sus datos.

**Secciones:**
1. Migas "Productos / Nuevo producto"; título "Nuevo producto"; a la derecha, "Cancelar" (secundario) y "Guardar producto" (primario).
2. **Dos columnas:**
   - **Información** (tarjeta grande):
     - "Nombre del producto" (obligatorio).
     - "Descripción", área de texto de 5 líneas con contador "0 / 2000".
     - Fila: "Marca" (desplegable) · "Categoría" (desplegable).
     - "Precio (S/)" con prefijo "S/" y la ayuda "Incluye IGV. Debe ser mayor que 0."
   - **Imagen y estado** (tarjeta de 360 px):
     - Zona de arrastre punteada con ícono de imagen: "Arrastra una imagen o pega una URL".
     - Campo "URL de la imagen".
     - Vista previa cuadrada.
     - Interruptor "Producto activo" (encendido).
3. **Aviso informativo** (azul suave) debajo: "El producto empieza sin stock. Después de guardarlo, registra una entrada en **Stock** para poder venderlo."

**Variante · editar:** título "Editar producto", campos llenos con "Cámara bala exterior 4 MP", y una línea gris "Creado el 09/10/2026 · Última modificación el 14/10/2026".

**Variante · errores:** "El nombre es obligatorio" y "El precio debe ser mayor que 0" en rojo.

**Prompt para Stitch:**
```
VISTA: Nuevo producto (formulario) del panel de administración.
MARCO: panel de administración, opción activa "Productos", migas "Administración / Productos / Nuevo producto".

CONTENIDO, SOLO esto y en este orden:
1. Encabezado: a la izquierda, el título "Nuevo producto". A la derecha, los botones "Cancelar" (secundario) y "Guardar producto" (azul).
2. Dos columnas:
   - IZQUIERDA: una tarjeta "Información" con: el campo "Nombre del producto *"; el área de texto "Descripción" de 5 líneas con el contador gris "0 / 2000" abajo a la derecha; una fila con los desplegables "Marca *" ("Selecciona una marca") y "Categoría *" ("Selecciona una categoría"); y el campo "Precio (S/) *" con el prefijo "S/" y la ayuda gris "Incluye IGV. Debe ser mayor que 0."
   - DERECHA: una tarjeta de 360 px "Imagen y estado" con: una zona de arrastre con borde punteado, un ícono de imagen y el texto "Arrastra una imagen o pega una URL"; el campo "URL de la imagen"; una vista previa cuadrada vacía en gris claro; y un interruptor ENCENDIDO "Producto activo".
3. Debajo de las dos columnas, un aviso informativo (fondo #E0F2FE, texto #0369A1, ícono de información): "El producto empieza sin stock. Después de guardarlo, registra una entrada en Stock para poder venderlo."

NO AGREGUES: campo de stock o cantidad, SKU, código de barras, costo, proveedor, peso, dimensiones, variantes, etiquetas ni SEO.
```

### 5.4 Marcas y categorías

**Para qué sirve:** mantener las listas de marcas y categorías.

**Secciones:**
1. Título "Marcas y categorías".
2. **Pestañas:** "Marcas (6)" · "Categorías (6)".
3. **Contenido de cada pestaña:**
   - Fila con el campo "Nueva marca" y el botón "+ Agregar".
   - Tabla: Nombre · Productos · Estado · Acciones (renombrar, activar o desactivar).
   - Marcas: Hikvision (2) · Dahua (1) · Ezviz (2) · Imou (1) · Honeywell (2) · Yale (1).
   - Categorías: Cámaras (3) · Alarmas (1) · Sensores de movimiento (1) · Detectores de humo (1) · Cerraduras digitales (1) · Control de acceso (1).

**Prompt para Stitch:**
```
VISTA: Marcas y categorías del panel de administración.
MARCO: panel de administración, opción activa "Marcas y categorías", migas "Administración / Catálogo / Marcas y categorías".

CONTENIDO, SOLO esto y en este orden:
1. El título "Marcas y categorías".
2. Pestañas: "Marcas (6)" (activa) y "Categorías (6)".
3. Una tarjeta con:
   - Arriba, una fila con el campo "Nueva marca" y el botón azul "+ Agregar".
   - Una tabla de columnas "Nombre", "Productos", "Estado", "Acciones" (ícono de lápiz e interruptor; sin papelera). Filas, todas con la insignia verde "Activa":
     "Hikvision" "2" · "Dahua" "1" · "Ezviz" "2" · "Imou" "1" · "Honeywell" "2" · "Yale" "1".

NO AGREGUES: logos de marcas, descripciones, imágenes ni botón de eliminar.
```

### 5.5 Stock

**Para qué sirve:** ver las unidades de cada producto y registrar entradas o ajustes.

**Secciones:**
1. Título "Stock"; a la derecha, el botón primario "+ Registrar entrada".
2. **3 tarjetas resumen:** "Unidades disponibles: 105" · "Reservadas en compras en curso: 2" · "Productos agotados: 1" (en rojo).
3. **Barra de herramientas:** buscador; filtro con botones segmentados "Todos · Con pocas unidades · Agotados".
4. **Tabla:**
   - Columnas: Producto · Disponible · Reservado · Estado · Última actualización · Acciones.
   - Ejemplos:
     - Cámara bala exterior 4 MP · 3 · 0 · Últimas unidades · hoy 10:12
     - Alarma Wi-Fi para hogar · 0 · 0 · Agotado
     - Cerradura digital con huella · 5 · 2 · Últimas unidades

     El resto, "En stock".
   - Acciones por fila: botones pequeños "Entrada" y "Ajuste", y el ícono de historial "Ver movimientos".

**Modal "Registrar entrada":** "Producto" (buscador desplegable) · "Cantidad que llegó" (número mayor que 0) · "Nota (opcional)", por ejemplo "Guía de remisión 001-4521" · botones "Cancelar" y "Registrar entrada".

**Modal "Ajuste de stock":** aviso ámbar "Usa el ajuste solo para corregir el stock después de un conteo físico." · "Producto: Cámara bala exterior 4 MP (disponible: 3)" · "Cantidad a ajustar" con botones segmentados "Sumar / Restar" · **"Motivo" obligatorio** (desplegable: Merma, Rotura, Error de carga, Otro, más un campo de detalle) · botones "Cancelar" y "Guardar ajuste".

**Prompt para Stitch:**
```
VISTA: Stock del panel de administración, con el modal de ajuste abierto.
MARCO: panel de administración, opción activa "Stock", migas "Administración / Inventario / Stock".

CONTENIDO DE LA PÁGINA (detrás del modal), SOLO esto y en este orden:
1. Encabezado: a la izquierda, el título "Stock". A la derecha, el botón azul "+ Registrar entrada".
2. Una fila de 3 tarjetas: "Unidades disponibles" "105"; "Reservadas en compras en curso" "2"; "Productos agotados" "1" (en rojo).
3. Una barra con el buscador "Buscar producto…" y botones segmentados "Todos" (activo), "Con pocas unidades" y "Agotados".
4. Una tarjeta con una tabla de columnas "Producto", "Disponible", "Reservado", "Estado", "Última actualización", "Acciones". En "Acciones": dos botones pequeños secundarios, "Entrada" y "Ajuste", y un ícono de historial. Filas:
   "Cámara bala exterior 4 MP" "3" "0" ámbar "Últimas unidades" "Hoy 10:12";
   "Alarma Wi-Fi para hogar" "0" "0" roja "Agotado" "14/10/2026";
   "Cerradura digital con huella" "5" "2" ámbar "Últimas unidades" "Hoy 14:35";
   "Kit de 4 cámaras con grabador" "10" "0" verde "En stock" "12/10/2026";
   "Sensor de movimiento PIR" "25" "0" verde "En stock" "09/10/2026".

MODAL ABIERTO (centrado, 480 px, sobre un velo oscuro semitransparente):
- El título "Ajuste de stock" y una "×" para cerrar.
- Un aviso ámbar: "Usa el ajuste solo para corregir el stock después de un conteo físico."
- El texto "Producto: Cámara bala exterior 4 MP (disponible: 3)".
- Botones segmentados "Sumar" y "Restar" (activo "Restar").
- El campo "Cantidad a ajustar" con el valor "1".
- El desplegable obligatorio "Motivo *" con la opción "Rotura" seleccionada (opciones: "Merma", "Rotura", "Error de carga", "Otro").
- El campo "Detalle (opcional)".
- Abajo a la derecha: "Cancelar" (secundario) y "Guardar ajuste" (azul).

NO AGREGUES: almacenes o ubicaciones, lotes, proveedores, costos, órdenes de compra ni columnas distintas de las indicadas.
```

### 5.6 Movimientos

**Para qué sirve:** auditar todo lo que le pasó al stock.

**Secciones:**
1. Título "Movimientos de stock" y subtítulo "Historial de entradas, reservas, ventas y ajustes".
2. **Barra de herramientas:** selector "Producto: Todos ▾", selector "Tipo: Todos ▾", rango de fechas "01/10/2026 – 15/10/2026" y botón "Exportar".
3. **Tabla:**
   - Columnas: Fecha y hora · Producto · Tipo · Cantidad · Venta · Motivo.
   - **Insignias por tipo:** Entrada (verde, "+10") · Reserva (ámbar, "−2") · Confirmación (azul) · Liberación (gris, "+2") · Ajuste (morado, "−1").
   - Ejemplos:
     - 15/10 14:35 · Cámara bala exterior 4 MP · Reserva · 2 · #00042
     - 15/10 14:35 · Cámara bala exterior 4 MP · Confirmación · 2 · #00042
     - 14/10 16:20 · Cerradura digital con huella · Liberación · 1 · #00040
     - 12/10 09:00 · Kit de 4 cámaras con grabador · Ajuste · −1 · — · "Rotura en almacén"
     - 09/10 08:30 · Sensor de movimiento PIR · Entrada · 25 · — · "Stock inicial"
4. Paginación.

**Prompt para Stitch:**
```
VISTA: Movimientos de stock del panel de administración.
MARCO: panel de administración, opción activa "Movimientos", migas "Administración / Inventario / Movimientos".

CONTENIDO, SOLO esto y en este orden:
1. El título "Movimientos de stock" y debajo en gris "Historial de entradas, reservas, ventas y ajustes".
2. Una barra con: el selector "Producto: Todos", el selector "Tipo: Todos", el rango de fechas "01/10/2026 – 15/10/2026" y el botón secundario "Exportar".
3. Una tarjeta con una tabla de columnas "Fecha y hora", "Producto", "Tipo", "Cantidad", "Venta", "Motivo". El "Tipo" va como insignia de color: "Entrada" verde, "Reserva" ámbar, "Confirmación" azul, "Liberación" gris, "Ajuste" morado. Filas:
   "15/10/2026 14:35" "Cámara bala exterior 4 MP" "Reserva" "2" "#00042" "—";
   "15/10/2026 14:35" "Cámara bala exterior 4 MP" "Confirmación" "2" "#00042" "—";
   "14/10/2026 16:20" "Cerradura digital con huella" "Liberación" "1" "#00040" "—";
   "12/10/2026 09:00" "Kit de 4 cámaras con grabador" "Ajuste" "−1" "—" "Rotura en almacén";
   "09/10/2026 08:30" "Sensor de movimiento PIR" "Entrada" "25" "—" "Stock inicial".
4. La paginación debajo de la tabla: "Mostrando 1–5 de 5".

NO AGREGUES: usuario responsable, almacenes, costos, gráficos ni columnas distintas de las indicadas.
```

### 5.7 Ventas

**Para qué sirve:** ver todas las ventas y entrar al detalle de cada una.

**Lista:**
1. Título "Ventas" y "164 ventas este mes"; a la derecha, "Exportar".
2. **Barra de herramientas:** buscador "Pedido o cliente…", pestañas de estado "Todas · Confirmadas · Pendientes · Anuladas", selector "Comprobante: Todos ▾" y rango de fechas.
3. **Tabla:** Pedido · Fecha · Cliente (nombre y correo) · Productos · Comprobante · Estado · Total · "Ver". Ejemplos: #00042 Ana Torres Boleta B001-00000001 Confirmada S/ 578.00 · #00041 Seguridad Integral del Sur S.A.C. Factura F001-00000008 Confirmada S/ 2,398.00 · #00040 Luis Ramos — Anulada S/ 899.00 · #00043 Carla Díaz — Pendiente S/ 159.90.

**Detalle de venta:** igual a "Detalle de mi pedido" (vista 4.11) dentro del marco admin, con una tarjeta extra **"Cliente"** (nombre, correo, teléfono, dirección y "Ver usuario") y la línea de tiempo completa (registrada, reservada, comprobante, confirmada).

**Prompt para Stitch:**
```
VISTA: Ventas (lista) del panel de administración.
MARCO: panel de administración, opción activa "Ventas", migas "Administración / Ventas".

CONTENIDO, SOLO esto y en este orden:
1. Encabezado: a la izquierda, el título "Ventas" y debajo en gris "164 ventas este mes". A la derecha, el botón secundario "Exportar".
2. Una barra con: el buscador "Pedido o cliente…", pestañas "Todas" (activa), "Confirmadas", "Pendientes" y "Anuladas", el selector "Comprobante: Todos" y el rango de fechas "01/10/2026 – 15/10/2026".
3. Una tarjeta con una tabla de columnas "Pedido", "Fecha", "Cliente" (nombre y, debajo en gris, el correo), "Productos", "Comprobante", "Estado", "Total" y una última columna con el botón secundario pequeño "Ver". Filas:
   "#00043" "15/10/2026 15:02" "Carla Díaz" "carla.diaz@gmail.com" "1" "—" ámbar "Pendiente" "S/ 159.90";
   "#00042" "15/10/2026 14:35" "Ana Torres" "ana.torres@gmail.com" "2" "Boleta B001-00000001" verde "Confirmada" "S/ 578.00";
   "#00041" "15/10/2026 12:10" "Seguridad Integral del Sur S.A.C." "compras@sisur.pe" "2" "Factura F001-00000008" verde "Confirmada" "S/ 2,398.00";
   "#00040" "14/10/2026 16:20" "Luis Ramos" "luis.ramos@hotmail.com" "1" "—" roja "Anulada" "S/ 899.00".
4. La paginación: "Mostrando 1–4 de 164".

NO AGREGUES: envíos, métodos de pago, vendedor, comisiones ni columnas distintas de las indicadas.
```

### 5.8 Comprobantes

**Para qué sirve:** consultar todos los comprobantes emitidos.

**Secciones:**
1. Título "Comprobantes"; 2 tarjetas resumen: "Boletas emitidas: 128 · última B001-00000128" · "Facturas emitidas: 36 · última F001-00000036".
2. **Barra de herramientas:** buscador "Número, DNI o RUC…", botones segmentados "Todos · Boletas · Facturas", rango de fechas y "Exportar".
3. **Tabla:** Número (monoespaciada) · Tipo · Fecha de emisión · Cliente · Documento · Subtotal · IGV · Total · Acciones (ver, descargar PDF). Ejemplos: B001-00000001 Boleta Ana Torres DNI 45678912 S/ 489.83 S/ 88.17 S/ 578.00 · F001-00000008 Factura Seguridad Integral del Sur S.A.C. RUC 20601234567 S/ 2,032.20 S/ 365.80 S/ 2,398.00.

**Detalle:** la misma hoja de la vista 4.12, mostrada en un panel lateral derecho de 560 px con los botones "Descargar PDF" e "Imprimir".

**Prompt para Stitch:**
```
VISTA: Comprobantes del panel de administración, con el panel de vista previa abierto.
MARCO: panel de administración, opción activa "Comprobantes", migas "Administración / Ventas / Comprobantes".

CONTENIDO, SOLO esto y en este orden:
1. El título "Comprobantes".
2. Dos tarjetas en fila: "Boletas emitidas" "128" con "Última: B001-00000128"; "Facturas emitidas" "36" con "Última: F001-00000036".
3. Una barra con: el buscador "Número, DNI o RUC…", botones segmentados "Todos" (activo), "Boletas" y "Facturas", el rango de fechas y el botón secundario "Exportar".
4. Una tarjeta con una tabla de columnas "Número" (monoespaciada), "Tipo", "Fecha de emisión", "Cliente", "Documento", "Subtotal", "IGV", "Total", "Acciones" (íconos de ojo y de descarga). Filas:
   "B001-00000001" "Boleta" "15/10/2026" "Ana Torres" "DNI 45678912" "S/ 489.83" "S/ 88.17" "S/ 578.00" (fila seleccionada, fondo #DBEAFE);
   "F001-00000008" "Factura" "15/10/2026" "Seguridad Integral del Sur S.A.C." "RUC 20601234567" "S/ 2,032.20" "S/ 365.80" "S/ 2,398.00".
5. A la derecha, un PANEL LATERAL abierto de 560 px de ancho y alto completo, con el título "B001-00000001", una "×" para cerrar, los botones "Descargar PDF" (azul) e "Imprimir" (secundario), y debajo la vista previa reducida de la hoja A4 de la boleta: cabecera con logo y recuadro "BOLETA DE VENTA ELECTRÓNICA B001-00000001", datos del cliente, una fila de detalle y los totales.

NO AGREGUES: estado ante SUNAT, códigos QR, notas de crédito, anulaciones ni columnas distintas de las indicadas.
```

### 5.9 Reportes

**Para qué sirve:** analizar las ventas de un período.

**Secciones:**
1. Título "Reporte de ventas"; a la derecha, el rango "01/10/2026 – 15/10/2026" y el botón primario "Descargar reporte".
2. **4 tarjetas de indicadores:** "Total vendido S/ 38,420.50" · "Ventas 164" · "Ticket promedio S/ 234.27" · "Ventas anuladas 6 (3.5 %)".
3. **Gráfico de líneas** (ancho completo): "Ventas por día", con la línea azul del período y la línea gris punteada del período anterior.
4. **Dos tarjetas:**
   - **"Productos más vendidos":** ranking con barras horizontales: Cámara IP Wi-Fi 1080p 48 u. · Sensor de movimiento PIR 41 u. · Detector de humo fotoeléctrico 37 u. · Kit de 4 cámaras con grabador 22 u. · Cerradura digital con huella 15 u.
   - **"Ventas por categoría":** gráfico de dona con leyenda (Cámaras 46 %, Sensores 18 %, Cerraduras 14 %, Alarmas 10 %, Detectores 8 %, Control de acceso 4 %).
5. **"Boletas y facturas":** dos barras comparativas (128 boletas · 36 facturas).

**Prompt para Stitch:**
```
VISTA: Reporte de ventas del panel de administración.
MARCO: panel de administración, opción activa "Reportes", migas "Administración / Ventas / Reportes".

CONTENIDO, SOLO esto y en este orden:
1. Encabezado: a la izquierda, el título "Reporte de ventas". A la derecha, el rango de fechas "01/10/2026 – 15/10/2026" y el botón azul "Descargar reporte".
2. Una fila de 4 tarjetas de indicadores: "Total vendido" "S/ 38,420.50"; "Ventas" "164"; "Ticket promedio" "S/ 234.27"; "Ventas anuladas" "6" con "3.5 %" en gris.
3. Una tarjeta de ancho completo "Ventas por día" con un gráfico de líneas: línea azul continua "Este período" y línea gris punteada "Período anterior", con su leyenda, eje horizontal con los días del 01/10 al 15/10 y eje vertical en soles.
4. Dos tarjetas en fila:
   - "Productos más vendidos": 5 barras horizontales azules con nombre y cantidad: "Cámara IP Wi-Fi 1080p" "48 u."; "Sensor de movimiento PIR" "41 u."; "Detector de humo fotoeléctrico" "37 u."; "Kit de 4 cámaras con grabador" "22 u."; "Cerradura digital con huella" "15 u.".
   - "Ventas por categoría": un gráfico de dona con leyenda: "Cámaras 46 %", "Sensores de movimiento 18 %", "Cerraduras digitales 14 %", "Alarmas 10 %", "Detectores de humo 8 %", "Control de acceso 4 %".
5. Una tarjeta "Boletas y facturas" con dos barras horizontales comparativas: "Boletas" "128" y "Facturas" "36".

NO AGREGUES: márgenes o utilidades, costos, envíos, métodos de pago, tráfico web, metas ni gráficos distintos de los indicados.
```

### 5.10 Usuarios

**Para qué sirve:** gestionar las cuentas.

**Secciones:**
1. Título "Usuarios" y "52 usuarios"; a la derecha, el botón primario "+ Nuevo usuario".
2. **Barra de herramientas:** buscador "Nombre o correo…", botones segmentados "Todos · Clientes · Administradores" y selector "Estado: Todos ▾".
3. **Tabla:**
   - Columnas: Usuario (avatar con iniciales, nombre y correo) · Rol (insignia: "Administrador" azul, "Cliente" gris) · Estado · Registrado · Último acceso · Acciones (editar, activar o desactivar).
   - Ejemplos:
     - Mathias Alayo · Administrador · Activo
     - Ana Torres · Cliente · Activo
     - Luis Ramos · Cliente · **"Bloqueado hasta 15:20"** (insignia roja con candado)
     - Pedro Quispe · Cliente · Inactivo (gris)

**Modal "Nuevo usuario":** "Nombres" · "Apellidos" · "Correo" · "Rol" (Cliente / Administrador) · "Contraseña temporal" con el botón "Generar" · botones "Cancelar" y "Crear usuario".

**Prompt para Stitch:**
```
VISTA: Usuarios del panel de administración.
MARCO: panel de administración, opción activa "Usuarios", migas "Administración / Usuarios".

CONTENIDO, SOLO esto y en este orden:
1. Encabezado: a la izquierda, el título "Usuarios" y debajo en gris "52 usuarios". A la derecha, el botón azul "+ Nuevo usuario".
2. Una barra con: el buscador "Nombre o correo…", botones segmentados "Todos" (activo), "Clientes" y "Administradores", y el selector "Estado: Todos".
3. Una tarjeta con una tabla de columnas "Usuario" (avatar circular con iniciales, nombre y, debajo en gris, el correo), "Rol", "Estado", "Registrado", "Último acceso", "Acciones" (ícono de lápiz e interruptor; sin papelera). El "Rol" va como insignia: "Administrador" azul o "Cliente" gris. Filas:
   "MA" "Mathias Alayo" "admin@intiguard.pe" "Administrador" verde "Activo" "09/10/2026" "Hoy 09:12";
   "AT" "Ana Torres" "ana.torres@gmail.com" "Cliente" verde "Activo" "10/10/2026" "Hoy 14:30";
   "LR" "Luis Ramos" "luis.ramos@hotmail.com" "Cliente" con la insignia roja y un candado "Bloqueado hasta 15:20" "11/10/2026" "Hoy 15:05";
   "PQ" "Pedro Quispe" "pedro.quispe@gmail.com" "Cliente" gris "Inactivo" "12/10/2026" "13/10/2026" (fila atenuada).
4. La paginación: "Mostrando 1–4 de 52".

NO AGREGUES: botón de eliminar, permisos detallados, equipos, departamentos, último pedido ni columnas distintas de las indicadas.
```

---

## 6. Resumen de vistas

La columna **Stitch** dice si hay diseño exportado en `~/Descargas/stitch_intiguard_security_storefront/`. Todas las vistas están construidas en `DSW_II_PROYECTO_WEB`; las que no tienen diseño se armaron con el texto de este documento y el sistema de diseño de la sección 1.

| # | Vista | Rol | Marco | Ruta | Stitch |
|---|---|---|---|---|---|
| 4.1 | Inicio | Todos | Tienda | `/` | ✅ |
| 4.2 | Catálogo | Todos | Tienda | `/catalogo` | ✅ |
| 4.3 | Detalle de producto | Todos | Tienda | `/productos/:id` | ✅ |
| 4.4 | Iniciar sesión | Visitante | Pantalla dividida | `/login` | ✅ |
| 4.5 | Crear cuenta | Visitante | Pantalla dividida | `/registro` | ✅ |
| 4.6 | Carrito | Cliente | Tienda | `/carrito` | ✅ |
| 4.7 | Confirmar compra | Cliente | Tienda simplificado | `/checkout` | ✅ |
| 4.8 | Compra confirmada | Cliente | Tienda | `/compra/:idVenta/confirmada` | ✅ |
| 4.9 | Compra no completada | Cliente | Tienda | `/compra/no-completada` | ✅ (variante "sin comprobante") |
| 4.10 | Mis pedidos | Cliente | Tienda | `/mis-pedidos` | ✅ |
| 4.11 | Detalle de mi pedido | Cliente | Tienda | `/mis-pedidos/:id` | ✅ |
| 4.12 | Comprobante imprimible | Cliente y Admin | Hoja A4 | `/comprobantes/:id/imprimir` | ✅ |
| 4.13 | Mi perfil | Cliente y Admin | Tienda | `/perfil` | ✅ |
| 4.14 | Páginas de error | Todos | Tienda | `/403`, `/sesion-expirada`, `/sin-conexion`, `*` | Solo 404; las otras usan el mismo diseño |
| 5.1 | Resumen | Admin | Panel | `/admin` | ✅ |
| 5.2 | Productos | Admin | Panel | `/admin/productos` | ✅ (sin el modal "Desactivar") |
| 5.3 | Crear / editar producto | Admin | Panel | `/admin/productos/nuevo`, `/admin/productos/:id/editar` | ✅ (solo "Nuevo") |
| 5.4 | Marcas y categorías | Admin | Panel | `/admin/marcas-categorias` | ✅ |
| 5.5 | Stock | Admin | Panel | `/admin/stock` | **Falta** (armada con este documento) |
| 5.6 | Movimientos | Admin | Panel | `/admin/movimientos` | ✅ |
| 5.7 | Ventas | Admin | Panel | `/admin/ventas`, `/admin/ventas/:id` | ✅ lista · **falta** el detalle |
| 5.8 | Comprobantes | Admin | Panel | `/admin/comprobantes` | ✅ |
| 5.9 | Reportes | Admin | Panel | `/admin/reportes` | ✅ |
| 5.10 | Usuarios | Admin | Panel | `/admin/usuarios` | ✅ (sin el modal "Nuevo usuario") |

Los modales (desactivar producto, registrar entrada, ajuste de stock, nuevo usuario, cambiar contraseña) y las variantes (carrito vacío, sin resultados, cargando, pedido anulado, factura con errores) tampoco tienen diseño exportado: siguen la descripción de cada vista y los componentes de la sección 1.5.

## 7. Diferencias entre este prototipo y la app construida

| Vista | Qué cambia | Por qué |
|---|---|---|
| 4.3 | Una sola imagen, sin las 4 miniaturas | El producto tiene una sola `imagenUrl` |
| 4.3 | La pestaña "Especificaciones" sale del campo `especificaciones` | Campo propuesto a Catálogo |
| 4.4, 4.5, 5.10 | Ojo para mostrar la contraseña en todos los campos de contraseña | Coherencia con el login |
| 4.5 | "Cuenta creada" es el aviso verde en el login, no en el registro | ARQUITECTURA_WEB.md §4 (feature `registro`) |
| 4.8, 4.10, 4.11, 4.12 | El pedido #00042 lleva la boleta B001-00000026, no la B001-00000001 | Los mocks numeran los comprobantes en orden de fecha sobre un historial de ventas generado |
| 4.13 | No muestra "Última actualización: hace 2 meses"; "Cambiar foto" pide una URL | El backend no guarda la fecha del cambio de contraseña ni tiene servicio para subir archivos |
| 5.2, 5.6, 5.7, 5.8, 5.9 | "Exportar" y "Descargar reporte" descargan un CSV | No hay endpoint de exportación |
| 5.3 | La zona de arrastre acepta enlaces a imágenes, no archivos; editar no muestra "Creado el / Última modificación" | Sin servicio de archivos ni esas fechas en el producto |
| 5.1 | La campana lleva a Stock filtrado por agotados y su punto rojo aparece solo si hay alguno | No hay servicio de notificaciones |
| 5.10 | El interruptor está apagado para las cuentas bloqueadas; encenderlo reactiva la cuenta y levanta el bloqueo. No se puede desactivar la propia cuenta | Regla de la app |
