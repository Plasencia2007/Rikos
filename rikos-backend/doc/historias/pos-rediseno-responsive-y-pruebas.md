# Rediseño del POS: responsive y pasada de pruebas (2026-10-01)

Especificación: `docs/producto/ui-ux/03-pos-rediseno.md` § 4b.

## Qué se pidió
Probar el POS rediseñado (incluida la pantalla del pedido de la sesión de pedidos), exigir que **todo sea responsive** (móvil, tableta, escritorio), arreglar todas las fallas y documentar con el método de siempre.

## Fallas encontradas y cómo se arreglaron
| # | Falla (a 375 px) | Arreglo |
|---|---|---|
| 1 | El cabezal del shell aplastaba el título: el subtítulo largo se partía en una columna de ~100 px | Por debajo de 700 px se oculta el icono y el subtítulo (`estilos/pos.css`) |
| 2 | El mapa de mesas a escala mostraba 2 mesas y casi todo vacío; no se podía usar | Por debajo de 640 px de ancho de contenedor el mapa pasa a **fichas** (`.mapa-compacto`) en vez del plano |
| 3 | El plano ensanchaba su tarjeta a 1230 px (hijos de un grid no se achican) | `minmax(0, 1fr)` y `min-width: 0` en `.mapa-mesas`, `.mapa-tarjeta`, `.mapa-envoltura` |
| 4 | La barra lateral ocupaba 200 px en móvil | Arranca colapsada por debajo de 900 px (`apps/pos/index.tsx`) |
| 5 | Botón de cerrar de los diálogos de 32 px (objetivo táctil menor a 44) | 44 px en pantallas chicas o táctiles (`global.css`) |
| 6 | Pestañas, tarjetas, filtros y avisos sin adaptación | Reglas por ancho en `estilos/pos.css` y `estilos/mapa.css` |
Además: el `ResizeObserver` del mapa se había atado al elemento que cambia entre plano y fichas; se puso un contenedor estable (`.mapa-envoltura`).

## Cómo se verificó
- Navegador a **375, 768 y 1280 px**: Inicio, Mesas, Órdenes activas, Historial y Pedido, con una auditoría de elementos fuera del ancho y de desplazamiento horizontal de la página: sin hallazgos en los tres anchos.
- Diálogo "Abrir mesa" a 375 px: 343 px de ancho, botones de 44 px.
- Flujo real: Mesas -> mesa libre -> "Abrir mesa" con 2 comensales y cliente; fuera de horario pide el servicio (comportamiento existente) y luego abre el pedido con Persona 1 y Persona 2, mozo, hora y cliente.
- `npx tsc -b --force` limpio; `npm run lint` sin errores (5 advertencias antiguas).

## Pendiente (ver `docs/pendientes/README.md`)
"+ Persona" parte el texto en móvil (pantalla del pedido, de la otra sesión); sin pruebas automáticas de frontend; quedó un pedido de prueba abierto en Mesa 1 en desarrollo.

## Segunda pasada: celular (2026-10-01)
El usuario avisó que **faltaba arreglar el celular**. La pasada anterior solo evitaba desbordes; en un teléfono seguía mal: una columna lateral de 78 px comía el 21 % del ancho, los KPI del Inicio ocupaban una pantalla cada uno y la barra superior del pedido se partía con puntos huérfanos.

| Falla | Arreglo |
|---|---|
| Barra lateral de 78 px en 375 px | A 600 px o menos pasa a **barra inferior de navegación** (`estilos/pos.css`, rótulos cortos con `corto` en `PosShell.tsx`); "Mozo" abre el cambio de mozo; los ítems "Pronto" se ocultan |
| El contenido crecía más que la pantalla (altura 1793 px sin desplazamiento propio) | `min-height: 0` en `.pos-principal` y `100dvh` en la raíz: la barra queda fija y el contenido se desplaza |
| Etiquetas de la barra cortadas ("Cambiar de moz") | Rótulos cortos ("Órdenes", "Mozo") |
| KPI del Inicio a pantalla completa cada uno | Dos columnas compactas |
| Barra superior del pedido en una columna alta con puntos sueltos | Reorden por CSS (`.ped2-*`, sin tocar el TSX de la sesión de pedidos): botones arriba, datos en dos columnas sin puntos, personas y tarjetas de catálogo más angostas |

Verificación: auditoría a **320, 375, 768 y 1280 px** en Inicio, Mesas, Órdenes, Historial y Pedido: sin desbordes ni desplazamiento horizontal; capturas a 375 px de Inicio, Mesas (fichas), Órdenes (con líneas reales) y Pedido. En escritorio (carga limpia) la barra lateral sigue de 250 px a la izquierda. Nota de método: al redimensionar en vivo con el panel oculto la transición del ancho de la barra se queda a medias; las medidas válidas son las de una carga limpia.

## Celular: menú hamburguesa en vez de barra inferior (2026-10-01)

Pedido del usuario: "en móvil, en vez de la barra abajo, ponle el menú hamburguesa arriba". En ≤ 600 px ya no hay barra
inferior: una barra superior de 56 px (hamburguesa, título de la pantalla, local) abre el menú como un **cajón** desde
la izquierda (300 px o 84 % del ancho), con fondo oscuro; se cierra al elegir una pantalla, al tocar el fondo, con la X
o con Esc. El cajón muestra el menú completo (rótulos, detalle, contador de órdenes, mozo y "Cambiar de mozo").
Reemplaza lo descrito arriba sobre la barra inferior. Archivos: `PosShell.tsx` y `pos.css`. Verificado a 375x812:
la barra superior mide 375x56, el cajón abre con todos los ítems, al elegir "Mesas" se cierra y el título cambia; sin
scroll horizontal. `tsc -b --force` y `lint` sin errores.
