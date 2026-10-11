# US-028 — Tomar el pedido (pantalla del POS)

Sprint 6, entrega 2B de 4 (2026-09-30). Plan en
`docs/superpowers/plans/2026-09-30-sprint6-entrega2b-pantalla-del-pedido.md`; la API que consume está en
`US-028-tomar-el-pedido-backend.md`.

## Qué se pidió

Que el mozo, desde el POS, abra un pedido en una mesa libre (o para llevar), lo reparta entre personas, asigne
pensionistas y agregue líneas viendo el precio y el total.

## Qué se construyó

Todo en `rikos-web` (no hubo cambios de backend):

- **`comun/formato/dinero.ts`**: `enSoles`, `aCentimos` y `desdeCentimos` viven ahora aquí (el archivo ya existía con
  `soles` y `formatearMoneda`; se agregaron sin tocar lo anterior). `features/pensionistas/dinero.ts` los reexporta,
  así `venta` no importa de `pensionistas`.
- **`api/cliente.ts`**: tipos (`Pedido`, `PersonaDelPedido`, `LineaDelPedido`, `OfertaDelDia`…) y las funciones de
  pedidos, más `buscarPensionistas(token, q)`.
- **`features/venta/mensajes.ts`**: los códigos de error de pedidos en español (nunca se ve un código crudo).
- **`PantallaPedido`**: encabezado con mesa, número, servicio y total; una pestaña por persona con su total y "+ Persona";
  acciones (Menú del día, Menú extra, Carta, Extra, "Es pensionista", "Desayuno/Cena del plan" solo para pensionistas
  en ese servicio, "Quitar esta persona"); lista de líneas con cantidad (− / +), nota y quitar; pie con subtotal, táper y
  total; aviso de táper sin monto; "Comandar (próximamente)" desactivado. **La pantalla no calcula nada**: cada
  escritura devuelve el pedido completo y se pinta tal cual.
- **Diálogos**: `DialogoMenu` (una opción se muestra "incluida", varias se eligen; no deja agregar sin elegir),
  `DialogoCarta` (con búsqueda), `DialogoExtra` (descripción, precio y cantidad; el único precio que escribe el mozo),
  `DialogoPensionista` (búsqueda por nombre o DNI con espera de 300 ms) y `DialogoNota`.
- **`MapaDeMesas`** recibe `onAbrirPedido` (solo el POS lo pasa): mesa libre → "Abrir pedido"; ocupada o pendiente de
  cierre → "Ver pedido"; botón "Pedido para llevar". Fuera de horario aparece el selector de servicio. El panel del
  administrador sigue viendo solo estado y bloqueo.
- **`apps/pos`**: vista mapa o pedido; cambiar de mozo o la inactividad limpian el pedido abierto en pantalla (el pedido
  vive en el servidor y se recupera con "Ver pedido").

## Qué se dejó fuera y por qué

Comandar, anular (entrega 3), trasladar y dividir (entrega 4), pedir desde el panel del administrador y el ticket del
kiosco (sprint 8). No hay pruebas de componentes (el proyecto no tiene Vitest); ver "Cómo se verificó".

## Cómo se verificó

`npx tsc -b --force` limpio; `npm run lint` sin nada nuevo (queda el error ajeno de `features/locales/HorarioSemanal.tsx`).
En el navegador, como `mozotest` (con un PIN aleatorio asignado desde la página):

- Abrir la mesa 1 (fuera de horario pidió el servicio) → pedido 0001 · Almuerzo.
- Menú sin elegir → "Elige una opción de: Entrada, Segundo."; eligiendo → S/ 12.00.
- Carta: gaseosa S/ 3.50, subir a 2 → S/ 7.00; extra "Huevo frito" 2 × S/ 3 → "precio puesto a mano". Total S/ 25.00.
- Pensionista (Luis Alberto Quispe Rojas): la pestaña toma su nombre; menú a **S/ 9.00 "precio de su plan"**; un segundo
  menú el mismo día queda marcado "segundo del día"; sin botones de cantidad (una comida del plan por línea).
- Nota "sin cebolla" y quitar línea bajan/actualizan el total; quitar una persona vacía.
- Volver al mapa: mesa 1 **Ocupada**, su diálogo solo ofrece "Ver pedido", que recupera el pedido intacto tras recargar.
- Para llevar: la línea muestra "táper S/ 1.00" y el pie la fila Táper (total S/ 13.00).
- Móvil 375 y tablet 768: sin desborde horizontal; los botones de líneas se agrandaron a 44 px.

## Decisiones tomadas al ejecutar (rulings)

- El menú de hoy se cargó agregando opciones a la semana **ya confirmada** (el sistema lo permitió).
- "+ Persona" selecciona a la persona recién creada (lo encontró la verificación en el navegador).
- El aviso de error de una escritura recarga el pedido para no mostrar datos viejos.

## Datos de desarrollo (inventados)

Menú de hoy 2026-09-30: entradas Papa a la huancaína y Sopa criolla; segundos Ají de gallina, Lomo saltado y Seco de
res; segundo extra Arroz con pollo; refresco Chicha morada; postre Mazamorra morada; menú extra a S/ 18.00. Carta:
Gaseosa personal S/ 3.50, Jugo de naranja S/ 5.00, Huevo frito adicional S/ 3.00 (los códigos `CARTA-03` y `CARTA-04`,
en las categorías Bebidas calientes y Sándwiches, dieron 500 al crearse y no existen; queda por investigar). El PIN de
`mozotest` quedó con un valor aleatorio que nadie conoce: reasignarlo desde el panel para usarlo. Los pedidos de prueba
se borraron.

## Ajuste del 2026-09-30: comensales, nombre del cliente y hora de apertura (para el mapa nuevo)

El rediseño del mapa de mesas (otra sesión, `features/venta/MapaDeMesas.tsx` y `estilos/mapa.css`) pide tres datos que el
backend no daba. Se agregaron:

- **`POST /pedidos`** acepta `comensales` (1 a 30, por defecto 1: crea "Persona 1" a "Persona N") y `nombreCliente`
  (opcional, hasta 120; en blanco = sin nombre). Errores 422 `COMENSALES_INVALIDOS` y `NOMBRE_DE_CLIENTE_LARGO`.
  Migración **V35** (`pedido.nombre_cliente`). La respuesta del pedido trae `nombreCliente` y la pantalla del pedido lo
  muestra junto al número ("Pedido 0004 · Almuerzo · Familia García").
- **`GET /mesas`** trae `abiertaEn` (la hora del pedido en curso, o null) para el "42 min" de cada mesa.
- Pruebas nuevas: 3 en `AbrirPedidoTest` y 1 en `MesasConPedidoTest`; todas en verde junto con las de mesas y pedidos.
- Verificado en el navegador: el diálogo "Abrir Ventana" con 2 comensales y "Familia García" crea el pedido con dos personas y el nombre
  guardado; el mapa muestra el tiempo de las mesas ocupadas.

Ruling: el nombre del cliente es solo una etiqueta del pedido, no una ficha de cliente (RF-017 sigue sin definir).

## Rediseño de la pantalla del pedido, pantalla partida (2026-10-01)

Hecho a pedido de la sesión de diseño del POS (`docs/producto/ui-ux/03-pos-rediseno.md`, tablero `PosPedidoDividido`).
Solo cambia el aspecto y la disposición; la lógica, los diálogos y la API siguen igual.

- **Pantalla partida:** a la izquierda el catálogo con pestañas (Menú del día, Menú extra, Carta, Extra) y a la derecha la
  **orden de la persona elegida** (panel de 340 px; en pantallas de menos de 900 px las dos zonas se apilan).
- **Barra superior:** `← Mesas`, el título y, debajo, los datos del pedido (número, servicio, comensales, hora de apertura y
  cuánto lleva, mozo y cliente) y **"Cambiar mesa"** (mover el pedido). Usa `mozoNombre` y `abiertoEn` que ya trae el pedido.
- **Personas como tarjetas con su monto**; la que tiene pensionista lleva "· Pensionista".
- **Catálogo con tarjetas del mismo tamaño y un "+"**: el menú del día y el menú extra abren su diálogo (tienen opciones);
  un producto de la carta se agrega directo (hay buscador); "Extra" abre su diálogo. Las comidas del plan aparecen como tarjeta
  para un pensionista en su servicio. Los estados vacíos se dicen ("Hoy no hay menú programado", "La carta está vacía").
- **Orden de la persona:** cada línea con cantidad − +, "Editar" (la nota), "Quitar" o "Anular", su etiqueta de estado
  (**Por enviar**, **En cocina**, **Anulado**), la casilla para pasar productos, y abajo los botones **Pensionista**, **Mover mesa**
  y **Unir mesa**, subtotal, total y **"Comandar (N ítems nuevos)"**.
- **Avisos flotantes** (`useAvisos`): "Comanda 1 enviada (2 ítems)", "Productos pasados", "Pedido movido a la mesa 3", "Mesa 4 unida",
  "Producto anulado". Los errores siguen saliendo en el aviso fijo de la pantalla.
- **Diálogo del pensionista:** ahora se llama "Pensionista" y tiene dos pestañas: "Por DNI o nombre" (activa) y "Código del
  ticket" (atenuada, "Sprint 8": allí se tecleará el código del pre-ticket, US-019/US-020).
- `DialogoCarta` ya no se usa en la pantalla (la carta es una rejilla con buscador); el archivo queda.

**Verificado en el navegador** (pedido de prueba con 2 comensales, la persona 2 pensionista): la carta agrega directo (Gaseosa,
"Por enviar"), el menú abre su diálogo y suma S/ 12.00, el botón dice "Comandar (2 ítems nuevos)", al comandar sale el aviso y
las líneas pasan a "En cocina"; la persona pensionista muestra "Precio de su plan" en la tarjeta del menú y no ofrece el botón
"Pensionista"; el diálogo muestra las dos pestañas. `tsc` y lint sin errores. En móvil (375) no hay desborde ni botones menores de
44 px; la barra lateral del shell (otra sesión) ocupa 200 px y deja poco espacio: se le avisó.

## Separar mesas desde el pedido (2026-10-01)

Pedido del usuario por medio de la sesión de diseño: al tocar en el mapa una mesa ya unida se abre directo el pedido de la principal (el
título trae todas las mesas, por ejemplo "Mesa 1 + Salón", y no se recorta), y la opción de **separar** vive dentro del pedido.

- Con las mesas unidas a la de este pedido (las de `GET /mesas` con `mesaPrincipalId` = esta mesa), el panel de acciones muestra
  **"Separar mesas"** junto a Mover mesa y Unir mesa, con confirmación ("Las mesas volverán a su lugar. Si el pedido está vacío se
  cierra y la mesa queda libre; si ya tiene productos, los conserva.").
- **Con productos:** separa todas (un `POST /mesas/{id}/separar` por cada una), el título pasa a solo la mesa principal, el botón
  desaparece y sale el aviso "Mesas separadas".
- **Vacío** (nadie pidió nada): separa y **termina el pedido** (cierre sin cobro) y vuelve al mapa, con el aviso "Mesas separadas; el
  pedido vacío se cerró". Es la misma regla que usa el mapa.
- Unir o separar desde el diálogo "Unir mesas" también refresca la lista, así el botón aparece o desaparece solo.

**Verificado en el navegador:** un pedido con una línea en una mesa unida a otra: se separa, queda "Mesa 1", la línea sigue y ambas mesas
quedan libres (la principal "libre + tomando pedido"); un pedido vacío en una mesa unida: se separa, el pedido queda `cerrado`, se vuelve al
mapa y las dos mesas quedan libres. `tsc` y lint sin errores.

## Rediseño completo de la pantalla del pedido (2026-10-01)

El dueño aprobó el diseño completo (láminas PosPedidoUnida / PosPedidoSola, ver `docs/producto/ui-ux/03-pos-rediseno.md`). La pantalla se
partió en tres archivos de `features/venta`: `PantallaPedido.tsx` (estado, llamadas a la API y diálogos), `CatalogoDelPedido.tsx` (izquierda) y
`OrdenDeLaPersona.tsx` (derecha). CSS nuevo con prefijo **`ped3-*`** al final de `estilos/global.css`; las clases `ped2-*` que usa el CSS móvil de
`pos.css` (barra, resumen, titulo, datos, persona, persona-nueva, rejilla) se conservan.

- **Barra superior:** "← Mesas", título sin recortar ("Mesa 1 + Mesa 2"), línea "Pedido N · servicio · N comensales · Desde hora · Mozo · Cliente",
  "Cambiar mesa" y, solo con mesas unidas, "Separar mesas" (borde verde, nota "Disponible antes de comandar"; deshabilitado si el pedido
  ya no está `abierto`). Ya no hay botones de mesa en el panel de la orden.
- **Personas:** tarjetas P1, P2… con monto, la activa más ancha y con borde verde, insignia verde "Pensionista" (más el nombre si se identificó
  en esta pantalla), papelera para quitar una persona vacía (si hay más de una), "+ Persona" punteada, scroll horizontal.
- **Catálogo:** buscador "Buscar productos…" arriba, pestañas **Todo · Entrada · Segundo · Extra** y alternar Mosaico/Lista. "Todo" empieza
  con las tarjetas "Menú del día" y "Menú extra" (solo si hay menú confirmado; abren `DialogoMenu`) y la comida del plan del pensionista, y sigue con toda
  la carta (bebidas y postres solo salen aquí). Entrada/Segundo/Extra filtran por `grupo` (calculado por el servidor, ver §8 de
  `sprint-06b-lista-de-pedidos.md`); "Extra" trae primero la tarjeta "Extra" libre (`DialogoExtra`, precio manual). Tarjetas del mismo alto
  (3 columnas; 2 en móvil) con imagen de relleno, nombre, subtítulo (`categoria`), precio y "Agregar".
- **Orden de la persona (340 px):** círculo "P1", "Orden de <nombre>" u "Orden · Persona 1", "Pedido N · servicio", "N ítems"; bloque del
  pensionista ("Nombre · Pensionista verificado — DNI validado" con "Cambiar", o "Sin identificar" con "Identificar pensionista"; ambos solo mientras
  la persona no tiene líneas, porque el servidor rechaza `PERSONA_CON_LINEAS`); líneas con miniatura, subtítulo, cantidad − +, menú ⋮
  (Editar, Pasar a otra persona…, Quitar/Anular), "Agregar nota" y precio; subtotal, total grande, "Terminar pedido" y "Comandar".
- **Varias líneas a la vez (US-029) se conserva:** "Seleccionar varias" en la cabecera de la orden muestra casillas y la barra "Pasar N líneas a…"
  (dividir en persona nueva incluido). El ⋮ de una línea abre el mismo diálogo para esa sola línea.
- **Responsive:** bajo 900 px la orden pasa debajo del catálogo; a 375 px las tarjetas van en 2 columnas, sin scroll horizontal de la página.
- **Pendiente conocido:** el nombre del pensionista solo se ve si se identificó en esta pantalla (el pedido trae únicamente `pensionistaId`); al recargar sale
  "Pensionista". Las imágenes son un ícono de relleno hasta que haya fotos de producto.

**Verificado en el navegador** (dev, admin): mesa abierta con 2 personas; Todo trae menú del día y menú extra primero; Entrada = Ceviche, Ensalada, Papa a
la huancaína, Sopa criolla; Segundo = Ají de gallina, Arroz con pollo, Lomo saltado, Seco de res; Extra = tarjeta libre + Chaufa, Huevo frito, Milanesa,
Pollo frito; el buscador filtra; Lista muestra una fila por producto; "Seleccionar varias" marca 2 líneas y ofrece "Pasar 2 líneas a…"; el ⋮ ofrece
Editar / Pasar a otra persona… / Quitar. A 1280 px las tarjetas van a 3 columnas; a 375 px a 2, sin scroll horizontal. `tsc -b` y lint sin errores.
No se pudo sacar captura (el panel del navegador estaba oculto): la verificación fue por DOM y estilos calculados.

### Adenda (2026-10-01): "Orden de la mesa"

La otra sesión (diseño POS) cambió el panel derecho: ya no muestra solo la orden de una persona sino la **orden de toda la mesa**
(`OrdenDeLaMesa.tsx`, que reemplaza a `OrdenDeLaPersona.tsx`; de este último solo quedan `nombreDeLinea` y `siglaDePersona`). Cada línea lleva su etiqueta
P1/P2, hay pestañas "Mesa completa / Persona N" y un solo "Comandar". Hasta 900 px la orden es un carrito flotante con cajón (clases `ped4-*`
en `pos.css`). Detalle en `pos-rediseno-entrega-4-*.md`.

- **"Seleccionar varias" se quitó** por decisión del usuario. El traslado de **varias líneas a la vez** (US-029) hoy es solo línea por línea desde el ⋮
  ("Pasar a otra persona…"). El backend (`POST /pedidos/{id}/lineas/trasladar`) sigue aceptando varias; si se quiere de vuelta en pantalla hay que
  rediseñarlo en el canvas.
- Lo descrito arriba sobre el panel de la orden por persona (bloque del pensionista, ⋮, notas, totales) quedó como estaba salvo lo que cambia esta adenda.

### Adenda (2026-10-11): Comandar, Terminar pedido y Cobrar solo cuando corresponden
En "Orden de la mesa": **Comandar** aparece solo cuando hay productos nuevos sin enviar a cocina (con el aviso "Se enviarán los productos nuevos..."); **Terminar pedido** aparece solo mientras no se pidió nada (o todo se anuló), porque con productos hay que comandarlos y cobrarlos (el servidor ya lo exigía: `PEDIDO_CON_LINEAS_POR_COBRAR`); y **Cobrar** aparece cuando hay productos comandados y el usuario tiene `caja.crear`. Pedido de la usuaria/o: el botón Comandar deshabilitado y el Terminar pedido siempre visible confundían. Verificado en el navegador con un pedido de prueba: vacío → solo Terminar; con un producto nuevo → Comandar; comandado → Cobrar.
