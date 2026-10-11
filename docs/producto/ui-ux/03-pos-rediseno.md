# Rediseño del POS (2026-09-30)

**Estado:** diseño aprobado por el usuario en el canvas (tableros `PosDashboard`,
`PosMesas`, `PosOrdenes`, `PosPedido`, `PosRapido`, `PosHistorial`, más
`MapaMesas` y `MapaMesasAbrir`). Se implementa por entregas, ver
`../plan/sprint-06b-pos-rediseno.md`.

**Cómo se llegó a esto:** el usuario trajo capturas de otro sistema de
restaurante y dijo "esta es la idea, cópiala". Tomamos la **estructura**
(barra lateral, cabezal, pantalla dividida catálogo y orden, tarjetas de
órdenes) y descartamos el **estilo** (naranja sobre negro, fotos de
producto, datos que no tenemos). Todo va en el tema verde de RIKOS
(`#2B7A68`, lateral `#0F2724`).

## 1. Contradicción con el mapa de pantallas, resuelta

`01-mapa-de-pantallas.md` § POS dice: *"Pantalla completa, sin barra
lateral. Se usa de pie, con una mano, con prisa."* El rediseño **agrega una
barra lateral**.

**Decisión (usuario, 2026-09-30):** el POS lleva barra lateral.
**Por qué:** separa las tareas que el mozo repite todo el día (mesas,
pedido rápido, órdenes en curso) y deja un lugar para caja y turno cuando
exista el sprint 7.
**Cómo se cuida lo de "una mano":** ítems de 44 px de alto o más, la barra
se puede **colapsar** a solo iconos, y las pantallas de trabajo (mesa,
pedido) no dependen de ella para volver: llevan su propio "← Mesas".

## 2. Estructura común (el "shell")

| Pieza | Qué es |
|---|---|
| Barra lateral | Inicio, Mesas, Pedido rápido, Órdenes activas (con contador), Historial; "Mi sesión" (caja y turno) **atenuada con "Pronto"** hasta el sprint 7. Abajo: mozo activo y "Cambiar de mozo" (vuelve al PIN) |
| Cabezal | Icono + título de la pantalla + subtítulo; a la derecha, datos de contexto (en el pedido: "Pedido #N · ítems · total") y el local |
| Avisos (toast) | Confirman acciones cortas ("Mesa abierta", "Comanda enviada") sin sacar al mozo de la pantalla. Un contexto/hook en `comun`; las pantallas de pedido lo usan en vez de `<Aviso>` donde convenga |
| Inactividad | Sin cambios: el temporizador vuelve al PIN (`apps/pos/index.tsx`) |

## 3. Pantallas

| Pantalla | Qué muestra | Datos de donde salen |
|---|---|---|
| **Inicio** | Mesas ocupadas (n / total, %), pedidos en curso, accesos rápidos, pedidos activos recientes, ocupación por salón. Ventas y efectivo del turno: atenuadas "Pronto" | `GET /mesas`, `GET /pedidos` |
| **Mesas** | El mapa por salón (ver `US-027`/`salones-y-mesas`): pestañas por salón, plano a escala, color por estado, "Abrir mesa" con comensales y cliente | `GET /mesas`, `GET /salones` |
| **Pedido rápido** | Canal **Para llevar** (Delivery atenuado "Pronto"), referencia opcional del cliente, categorías, catálogo en 4 columnas de tarjetas iguales, carrito a la derecha con "Comandar" | menú del día, carta, extras (ya existen) |
| **Órdenes activas** | Tarjetas por pedido en curso, del mismo alto: cabecera de color por tipo, líneas con componentes, tiempo abierto, estado, total, botones Imprimir / Pre-cuenta / Ver pedido. Pestañas Todas / Mesas / Para llevar | `GET /pedidos?situacion=en_curso` (nuevo) |
| **Pedido** | La pantalla de US-028 con cabezal de resumen (mesa, comensales, hora, mozo, cliente, "Cambiar mesa") y botón principal que cambia según el estado. **Su contenido es de la sesión de pedidos; solo el cabezal se alinea** | `GET /pedidos/{id}` |
| **Historial** | Tarjetas de pedidos cerrados, con filtros (texto, tipo, estado, fechas). Acciones: ver y reimprimir comanda. **Antes de "Terminar pedido" estaba vacío; ahora muestra los pedidos cerrados sin cobro (el sprint 7 añade el cobro)** | `GET /pedidos?situacion=cerrados` (nuevo) |

## 4. Decisiones, con su razón

- **Sin "Mostrador".** La referencia traía tres canales (Mostrador, Para
  llevar, Delivery). El usuario decidió quitar Mostrador. *Razón:* el
  backend solo tiene `mesa` y `para_llevar`; Mostrador sería un canal
  nuevo sin flujo que lo respalde. **Delivery** queda visible y atenuado
  ("Pronto") porque es el sprint 12.
- **Sin fotos de producto.** Nuestro menú es corto y el catálogo no
  guarda imagen; las tarjetas vacías con icono de caja se veían pobres.
  Las tarjetas llevan un recuadro de imagen neutro por ahora.
- **Sin "Stock".** Manejamos *agotados* por local (US-015), no stock.
- **Sin papelera en el historial.** Un historial no se borra; las
  anulaciones ya quedan auditadas (US-031).
- **Historial vacío hasta el sprint 7.** El pedido solo tiene los estados
  `abierto`, `comandado` y `pendiente_cierre` (CHECK de la tabla); `cerrado`
  llega con cobrar y cerrar. Tampoco existe *anular un pedido entero*: lo que
  existe (US-031) es anular una **línea**. El tablero `PosHistorial` dibuja
  tarjetas "Cerrada" y "Anulada" como referencia del diseño final; al
  implementar se muestra el estado vacío y la etiqueta "Anulada" se decide
  en el sprint 7.
- **Sin "Cobrar" todavía.** El botón principal de una orden es "Ver
  pedido". Cobrar, Pre-cuenta real y filtro de pagos llegan con el
  sprint 7; los botones de caja se ven **atenuados con "Pronto"**, no
  ocultos, para que el usuario vea hacia dónde va.
- **Estados como en el modelo.** El mapa usa libre, ocupada, "por cobrar"
  (`pendiente_cierre`), bloqueada y unida. No se inventó "Reservada"
  (no existe en el modelo).
- **Color + texto.** Nunca solo color: cada estado o tipo lleva su
  etiqueta, para no depender de distinguir tonos.
- **El tiempo se muestra legible** ("42 min", "1 h 12 min"); la
  referencia mostraba "123531 min" por un cálculo sin formato.

- **Pensionista en la mesa: provisional hoy, ticket después.** Hoy el mozo
  busca al pensionista **por DNI o nombre** y lo asigna a una persona
  (decisión del sprint 6, `plan/sprint-06-vender-en-mesa.md` §2). En el
  sprint 8 (US-019 y US-020) el pensionista saca un **pre-ticket** en el
  kiosco y el mozo **teclea el código del papel** para vincularlo (RN-025 a
  RN-027, RN-094: el pre-ticket no elige platos ni es un consumo cargado; un
  código es de un solo uso; vencido o anulado se rechaza). El tablero
  `PosPedidoPensionista` dibuja el diálogo con dos pestañas: "Por DNI o
  nombre" (activa) y "Código del ticket" (atenuada, "Sprint 8"). El botón del
  panel de la orden se llama **"Pensionista"**, no "Es pensionista", para que
  sirva en las dos etapas. La persona asignada se ve en su tarjeta
  ("Persona 2 · Pensionista") y cobra con la tarifa de su plan; **no existe
  un producto "menú para pensionista"**: come el mismo menú y el motor de
  precios aplica el plan.

- **Mesas igual a la referencia del usuario (2026-10-01).** Barra con las
  pestañas de salón y **"Unir mesas"**; encabezado del salón con "5 de 6
  ocupadas · 83% | 1 libre", **Ajustar vista y zoom (− 100% +)**; plano
  encuadrado sobre las mesas y centrado; leyenda centrada con separadores.
  **El botón "Pedido para llevar" se quitó del mapa** (decisión del
  usuario): el para llevar vive en "Pedido rápido" (entrega 4).
- **Unir y separar mesas se hace desde el mapa** (tableros `PosMesasUnir` y
  `PosMesasUnida`): "Unir mesas" -> tocar la principal (conserva el pedido) ->
  tocar las libres que se le unen -> "Unir N mesas". Una mesa unida se toca
  para **separarla** o para ver el pedido de su principal. Reglas del backend
  (US-029): solo se une una mesa libre, no bloqueada ni unida, a una principal
  activa. El botón "Unir mesa" dentro del pedido se mantiene. Permiso
  `venta.modificar`. **Las mesas unidas se muestran como una sola mesa**
  ("Principal +N", borde punteado, capacidad sumada; las unidas desaparecen del
  plano y reaparecen en su lugar al separar) y se separan desde la principal
  ("Separar todas las mesas") o desde cada unida. Al confirmar la unión se
  abre el diálogo con comensales y cliente (ver historia).

- **Orden de la mesa (2026-10-01):** la orden es de toda la mesa, con etiqueta P1/P2 por línea; en celular, carrito flotante y cajón. Ver historia de la entrega 4.
- **Pantalla del pedido (aprobada 2026-10-01, la hace la sesión de pedidos):**
  barra superior con título ("A + B" si hay mesas unidas), Cambiar mesa y
  Separar mesas; personas como tarjetas; **buscador arriba** y debajo solo las
  pestañas **Todo · Entrada · Segundo · Extra** con Mosaico/Lista; panel de la
  orden con el bloque de pensionista (verificado o "Identificar pensionista"),
  líneas con cantidad, ⋮ y nota, y Terminar pedido / Comandar. Menú del día y
  Menú extra salen como tarjetas de "Todo" y el Extra libre dentro de "Extra";
  ver `plan/sprint-06b-categorias-del-pedido.md`. Tableros `PosPedidoUnida` y
  `PosPedidoSola` en el canvas.

## 4b. Responsive (regla del usuario, 2026-10-01)

Todo el POS debe funcionar en móvil, tableta y escritorio. Criterios y cómo se
resolvió:

| Ancho | Qué pasa |
|---|---|
| ≤ 600 px (celular) | La barra lateral se convierte en una **barra inferior de navegación** (Inicio, Mesas, Órdenes con su contador, Historial y Mozo) con rótulos cortos y objetivos de 56 px, alcanzable con el pulgar; el contenido usa todo el ancho y se desplaza dentro de su zona. Los ítems "Pronto" no aparecen. Los KPI del Inicio van en dos columnas. En la pantalla del pedido, la barra superior se reordena (← Mesas y Cambiar mesa arriba, datos en dos columnas) |
| < 900 px | La barra lateral **arranca colapsada** (78 px, solo iconos) y se puede expandir. Menos relleno en las pantallas |
| < 700 px | El cabezal oculta el icono y el subtítulo (queda título y local); las tarjetas de órdenes e historial pasan a **una columna**; las pestañas se deslizan; los avisos ocupan el ancho; "Ocupación por salón" y "Pedidos activos" van en una columna |
| contenedor < 640 px | El **mapa de mesas pasa a un plano compacto** (cuadrícula de círculos de 3 columnas) con selector Plano / Lista (ver `pos-rediseno-mesas-y-unir.md`): el plano de 1200 px a escala 0,5 no se lee ni se toca bien |
| cualquiera | Sin desplazamiento horizontal de la página; objetivos táctiles de 44 px o más (el botón de cerrar de los diálogos sube de 32 a 44 px en pantallas chicas o táctiles) |

Cómo se prueba: en el navegador a 375, 768 y 1280 px, con una auditoría que
detecta elementos que se salen del ancho y página con desplazamiento
horizontal, más una pasada por Inicio, Mesas, Órdenes, Historial y Pedido.

## 5. Datos de ejemplo

Los tableros usan nombres, precios y cifras **de muestra**. No son datos
del dueño. Los precios reales siguen pendientes (ver `pendientes/`).

## 6. Lo que falta

- Pantalla del pedido con catálogo y orden (`PosPedidoDividido`): falta
  aprobarla y pasarla a la sesión de pedidos, que es dueña de esa pantalla.
- Probar los tableros con un mozo real antes de pulir detalles (el
  backlog ya lo pedía para US-028).
- Modo colapsado de la barra lateral: diseñarlo cuando se implemente.
- Tablero del estado vacío del Historial y de Órdenes activas sin pedidos.
- Pantalla de caja y turno (sprint 7): definir qué reemplaza a "Mi
  sesión — Pronto".
