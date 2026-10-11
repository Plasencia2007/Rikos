# Rediseño del POS, entrega 4: Pedido rápido y pantalla del pedido a pantalla completa (2026-10-01)

Especificación: `docs/producto/ui-ux/03-pos-rediseno.md`, `docs/producto/plan/sprint-06b-pos-rediseno.md`
y `docs/producto/plan/sprint-06b-categorias-del-pedido.md`.

## Qué se pidió
(1) Hacer yo mismo el diseño de las pantallas del pedido y de Pedido rápido en el código; (2) la **orden (el carrito) a todo el alto**, como en la referencia; (3) **personas más pequeñas**; (4) decir qué elementos no aparecen en la imagen para decidir dónde ponerlos.

## Qué se construyó (solo frontend)
- **Pantalla del pedido a pantalla completa** (>= 901 px), solo CSS sobre las clases `ped2`/`ped3` de la sesión de pedidos, en `estilos/pos.css`: la página ya no se desplaza entera; arriba barra y personas; a la izquierda el catálogo con buscador y pestañas **fijos** y las tarjetas con **desplazamiento propio**; a la derecha la **orden a todo el alto**, con las líneas desplazables y el **total y "Comandar" siempre a la vista**. Antes la página medía 2.200 px y "Comandar" quedaba fuera de pantalla.
- **Personas compactas:** 44 px de alto y 84 a 124 px de ancho (antes 56 × 190).
- Botón "Agregar" de las tarjetas a todo el ancho de la tarjeta.
- **Pedido rápido** (`features/pos/PantallaPedidoRapido.tsx`): canal **Para llevar** (Delivery atenuado "Pronto"; Mostrador no existe), referencia opcional del cliente (Enter abre), y "Abrir pedido", que crea un pedido `para_llevar` y entra a la misma pantalla del pedido titulada "Para llevar · referencia". Si el servidor pide el servicio (fuera de horario) lo avisa en amarillo y deja elegir. El menú lateral lo habilita y el Inicio lo enlaza.

## Cómo se verificó
- `tsc` y lint sin errores. Navegador a 1280 × 800: la página ya no se desplaza (800/800), el catálogo se desplaza dentro de su columna, la orden mide 648 px de alto y "Comandar" queda a 767 de 800 px; personas de 51 px.
- Pedido rápido: Pedido rápido -> "Juan prueba" -> fuera de horario (aviso) -> Abrir -> pedido 0025 "Para llevar · Juan prueba" -> agregar Chaufa -> línea "Por enviar" S/ 18,00 -> Terminar pedido (sin borrar nada).
- Error aprendido: un comentario CSS con `*/` dentro del texto (`ped2-*/ped3-*`) cerró el comentario y descartó toda la regla siguiente; se detectó midiendo.

## Elementos que no están en la imagen de referencia (dónde ponerlos)
Ver la lista y la propuesta en `docs/pendientes/README.md`.

## Carrito más grande (2026-10-01)

Pedido del usuario: "el carrito que sea más grande". La orden ocupa todo el alto de la pantalla del pedido y su
ancho pasó a `clamp(340px, 31vw, 460px)` (460 px a 1500 px de pantalla). Textos y botones mayores: título 19 px,
líneas 16 px, total 32 px, "Comandar" de 64 px de alto. `.pos-raiz` lleva `overflow: hidden` en escritorio para que
la página no haga scroll; solo se desplazan el catálogo y las líneas de la orden. Solo CSS (`pos.css`).
Verificado a 1500x1040 con 17 personas: orden de 878 px de alto y 460 de ancho, sin scroll de documento.
`tsc -b --force` sin errores; `lint` sin errores (5 avisos que ya existían).
Si en otro navegador se ve el diseño viejo, es caché: recargar con Ctrl+F5.

### Carrito de arriba a abajo (2026-10-01)

El usuario pidió que la orden llegue hasta el borde superior, al lado del título y de las personas (como la referencia).
Solo CSS en `pos.css` (≥ 901 px): `.ped3` pasa a ser una grilla de dos columnas; `.ped2-cuerpo` queda con
`display: contents` y la orden ocupa `grid-row: 1 / -1`. Título, personas y catálogo quedan en la columna izquierda.
Verificado a 1500x900: orden y=12, alto 872, ancho 460; sin scroll de documento. Móvil y tableta (< 901 px) no cambian.

### Tarjetas del catálogo recortadas (2026-10-01)

El botón "Agregar" se cortaba: las filas de la rejilla quedaban en 230 px (el `min-height` de `.ped3-tarjeta`) y el
contenido medía 250. Ahora las filas miden lo que mide su contenido (`grid-auto-rows: max-content`, sin alto fijo
en la tarjeta) y solo se desplaza la lista. Verificado a 1200x800: 19 tarjetas, ninguna recortada, todas de 266 px.
Solo CSS en `pos.css`.

### Se quita "Seleccionar varias" del carrito (2026-10-01)

Pedido del usuario: "borra eso del carrito, no funcionará". Se quitó el enlace de la cabecera de la orden en
`OrdenDeLaPersona.tsx`. La lógica de selección múltiple (casillas y "Pasar N líneas a…") sigue en el código pero ya
no tiene cómo activarse; pasar líneas de una persona a otra queda solo por el menú ⋮ de cada línea. `tsc -b --force`
y `lint` sin errores. Archivo de la sesión de pedidos: avisarle.

## Orden de la mesa y carrito flotante (2026-10-01)

Diseño aprobado por el usuario en el canvas (`PosPedidoMovil`, `PosPedidoMovilLista`, `PosPedidoMovilOrden`, y el
panel derecho de `PosPedidoUnida`/`PosPedidoSola`). **Las personas solo separan**: la orden pasó de ser "la de la persona
elegida" a ser **la de toda la mesa**.

- Componente nuevo `OrdenDeLaMesa.tsx` (reemplaza el uso de `OrdenDeLaPersona.tsx` en `PantallaPedido.tsx`; ese archivo
  solo conserva `nombreDeLinea` y `siglaDePersona`). Una lista con todas las líneas de todas las personas, cada una con su
  etiqueta (P1, P2…), sin títulos de persona. Pestañas "Mesa completa / Persona N" que filtran; con una persona elegida
  aparece su bloque de pensionista (identificar o verificado). Un solo "Comandar N ítems nuevos" para toda la mesa,
  "Subtotal · N ítems" y "Total de la mesa". Sin backend nuevo: cada línea ya pertenecía a una persona.
- Escritorio (≥ 901 px): panel derecho de arriba a abajo. Celular y tableta vertical (≤ 900 px): botón flotante verde
  "Ver orden de la mesa" (contador, ítems nuevos, personas, total) que abre la orden como cajón desde el costado, con
  fondo oscuro y botón "Cerrar".
- "Pasar a otra persona" sigue en el ⋮ de cada línea y ahora toma como origen a la persona dueña de la línea.
- "Seleccionar varias" se quitó (pedido del usuario: "no funcionará"); el traslado de varias líneas (US-029) queda solo
  línea por línea. Si hace falta de nuevo, hay que rediseñarlo.
- CSS en `pos.css` (clases `ped4-*`). Verificado a 390x844 (cajón, filtro por persona, cierre) y a 1500x900 (panel de
  460 px a todo el alto). `tsc -b --force` y `lint` sin errores. Pedido de prueba terminado, no borrado.

### Sin pestañas "Mesa completa / Persona N" (2026-10-01)

Pedido del usuario: "quita eso, que siempre se vea la mesa completa". Se quitaron las pestañas de filtro de la orden
(`OrdenDeLaMesa.tsx`, `PantallaPedido.tsx`): la lista siempre muestra todas las líneas de la mesa. El bloque de
pensionista (identificar o verificado) ahora es el de la persona elegida en la fila de personas de arriba, que sigue
siendo quien recibe el próximo producto. `tsc -b --force` y `lint` sin errores.

### Sin barras de desplazamiento (2026-10-01)

Pedido del usuario: "quita estas barras". Dentro del POS (`.pos-raiz`) ya no se ven las barras de desplazamiento (catálogo,
líneas de la orden, lista de personas, mapa); se sigue desplazando con rueda, trackpad o dedo. Solo CSS en `pos.css`.
Los diálogos que se abren sobre el POS fuera de `.pos-raiz` conservan su barra.

### Título "Productos" en celular (2026-10-01)

Faltaba el título "Productos" del diseño móvil. `CatalogoDelPedido.tsx` lleva un `h3.ped4-productos` junto al selector
Mosaico/Lista; en ≤ 900 px las pestañas ocupan su propia fila y debajo van el título y el selector. En escritorio no se
ve (pestañas y selector siguen en una fila). Archivo de la sesión de pedidos (cambio de una línea).

### Quitar a una persona que ya pidió + "+ Persona" a la vista (2026-10-01)

- Pedido del usuario: "alguien pide y se va, para borrarlo nomás". El basurero de la persona ya no exige que esté vacía: aparece
  mientras **nada suyo haya ido a cocina** (todas sus líneas "por enviar"). Si tiene productos, pide confirmación ("Se quita con
  sus N productos por enviar") y quita primero sus líneas y luego a la persona (sin backend nuevo: `QuitarPersona` exige que no
  tenga líneas). Si algo ya se comandó, no hay basurero: eso se anula línea por línea con motivo (US-031).
- "+ Persona" fijo al final: se probó y el usuario lo rechazó (tapaba a las personas); se volvió a como estaba.

### "+ Persona" centrado (2026-10-01)

El botón heredaba el ancho mínimo (84 px) de las tarjetas de persona compactas y su texto se salía de la caja ("se ve movido").
Ahora tiene ancho propio (mín. 118 px) y el texto centrado. Solo CSS en `pos.css`.

### Cabecera del pedido más cuidada en celular (2026-10-01)

Pedido del usuario: "esto no lo puedes dejar así, hazlo mejor, más bonito". La cabecera (← Mesas, Cambiar mesa, título y
datos en filas sueltas) pasó, en ≤ 900 px, a una **tarjeta blanca**: arriba los botones ("← Mesas" en verde suave y
"Cambiar mesa" a la derecha), debajo el título grande (24 px) y los datos como **chips** (Pedido, servicio, comensales, hora,
mozo/cliente; el del pedido en verde). Solo CSS en `pos.css`. Verificado a 390x844 con un pedido para llevar de prueba
(terminado, no borrado). Escritorio sin cambios.

### Diálogos por encima del carrito flotante (2026-10-01)

El carrito flotante (z-index 900) tapaba los botones de los diálogos ("Separar mesas": Cancelar y Separar quedaban debajo).
Los modales del POS pasan a z-index 1300, por encima del carrito (900) y del cajón de la orden (1100). Solo CSS en `pos.css`.

### Pedido en celular, versión 2 (2026-10-01)

Diseño aprobado en el canvas (`PosPedidoMovil`, `PosPedidoMovilLista`, `PosPedidoMovilOrden`), a partir de una referencia del usuario.
Solo ≤ 600 px; escritorio y tableta no cambian.

- **Barra superior del POS:** hamburguesa, el título (la mesa o "Para llevar") y debajo el local con su pin (antes una píldora
  aparte). `PosShell.tsx`.
- **Cabecera del pedido:** sin tarjeta propia ni título repetido; una **barra verde suave partida** "← Mesas | ⇄ Cambiar mesa"
  (sola y a todo el ancho en un pedido para llevar) y debajo una **tarjeta de datos en dos filas** con icono (pedido, servicio,
  comensales / desde, mozo o cliente). `PantallaPedido.tsx` (solo iconos y clases) y `pos.css`.
- **Personas:** icono de persona delante del nombre. **Aviso de menú del día** como alerta informativa azul. **Selector
  mosaico/lista** con el activo en verde oscuro.
- Verificado a 390x844 con un pedido para llevar (se vio la barra, la tarjeta de datos, el aviso y el carrito). No se vio con una
  mesa con "Cambiar mesa" ni con mesas unidas ("Separar mesas"). `tsc -b --force` y `lint` sin errores.

### "Agregar" más chico en la lista (2026-10-01)

En la vista de lista el botón "Agregar" era demasiado grande y el precio quedaba al lado del nombre. En ≤ 900 px la fila pasa a:
imagen, nombre y detalle, **precio debajo** y un botón compacto (44 px de alto, 13 px) a la derecha. Solo CSS en `pos.css`.

### Lista del catálogo más ordenada (2026-10-01)

Pedido del usuario: acomodar mejor el precio y el resto de la fila. En ≤ 900 px cada fila lleva imagen de 60 px, nombre,
detalle y **precio en verde (16 px)** en una columna, y un botón **"+" cuadrado de 48 px** a la derecha (el texto "Agregar" se
oculta y queda en su `aria-label`). Solo CSS en `pos.css`.

### Acciones de la línea como iconos (2026-10-01)

Pedido del usuario: el menú de tres puntitos "es muy poco profesional". En `OrdenDeLaMesa.tsx`, junto a la etiqueta de la persona
(P1, P2…), cada línea muestra **iconos**: nota (hoja; reemplaza al "Editar", que solo editaba la nota), pasar a otra persona
(flechas cruzadas), quitar (tacho, línea por enviar) y anular (círculo tachado, línea ya comandada, con permiso). Cada uno con su
`title` y `aria-label`. Se quitó el enlace "Agregar nota" (la nota ya escrita se sigue viendo en la línea). Mismos permisos y
reglas que antes. Mismo diseño en escritorio y celular (40 px de toque en celular). Verificado a 390x844 con dos líneas
(pedido de prueba terminado, no borrado); no se vio el icono de anular (hace falta una línea comandada). `tsc -b --force` y `lint`
sin errores.
