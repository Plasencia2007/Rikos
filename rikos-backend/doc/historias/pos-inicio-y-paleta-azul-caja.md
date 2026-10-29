# Nuevo inicio del POS y paleta "Azul caja" (web)

Hecho el 2026-10-28. Mejora de diseño sobre lo ya construido; no agrega funciones.

## Qué se pidió

1. Rehacer el inicio del POS con un diseño que trajo el equipo: «Resumen del local»,
   indicadores, «Comienza una nueva atención», pedidos activos y ocupación por salón.
2. Dejar el verde como color de marca, elegir un color principal y usarlo en el POS y en
   el panel administrativo, con barras laterales claras, al estilo de Factivo.

## Qué se construyó

- **Guía visual RIKOS UX/UI** (artifact privado: https://claude.ai/artifact/QNYzGR5JomChNdrmAdLfY1),
  con la organización de la guía de Don Brassa: paleta, tipografía, botones, formularios,
  estados, el inicio del POS, mesas, productos, caja y panel. El equipo eligió la paleta
  **Azul caja** entre cuatro direcciones.
- **Inicio del POS** (`features/pos/PantallaInicio.tsx`):
  - Título «Resumen del local», con un subtítulo según los pedidos en curso.
  - «Nuevo pedido» abre dos opciones: en una mesa o para llevar.
  - Tres indicadores: mesas con barra de ocupación, pedidos en curso, y ventas del turno
    marcadas «Próximamente».
  - «Comienza una nueva atención».
  - Pedidos activos con pestañas Todos / En salón / Para llevar (cuenta hasta 50 pedidos en curso).
  - Ocupación por salón con un anillo y una insignia por salón: Disponible, Casi lleno o Lleno.
- **Marco del POS** (`features/pos/PosShell.tsx`):
  - Rótulo «Operación» en el menú y punto verde en el mozo activo.
  - Cabezal con la ruta «Punto de venta / …», el local y el aviso «Sistema en línea»
    (cambia a «Sin conexión» según `navigator.onLine`).
  - Pie de estado.
  - `IconoDelTitulo` se reemplazó por `tituloEnContenido`: el inicio trae su propio h1.
- **Paleta Azul caja en toda la web** (salvo el kiosco):
  - Tokens en `estilos/tokens.css`: `--color-marca: #2563eb`, nuevo `--color-marca-tinta`,
    neutros gris azulado y la barra lateral del panel en blanco.
  - Los colores escritos a mano en las hojas de estilo y en las pantallas de login y PIN
    se cambiaron con una tabla fija de equivalencias (verde de marca a azul, grises verdosos
    a grises azulados): unos 740 valores en total.
  - La barra lateral del POS (`estilos/pos.css`) y la del panel (`.panel-lateral-*` en
    `estilos/global.css`) quedaron blancas: ítem activo en azul suave con una franja azul a
    la izquierda, y textos oscuros.

## Decisiones

- **El verde queda solo como significado**: libre, éxito, comandado, activo. En el mapa de
  mesas, «libre» usaba el verde de marca: ahora usa `#047857`, el verde de «libre» de la app
  móvil, para que el estado no se confunda con la marca.
- **El kiosco no se tocó**: tiene su propia hoja (`kiosco.css`) y queda para una pasada aparte.
- **La tipografía no cambió** (system-ui). La guía usa Plus Jakarta Sans, la misma de la app;
  pasarla a la web queda pendiente.

## Cómo se verificó

- `npx tsc -b --force` sin errores.
- `eslint` sobre los archivos tocados: sin errores nuevos. Los 3 errores `no-explicit-any` de
  `features/compras` ya estaban; ahí solo cambiaron dos colores.
- Revisado en el navegador a 1280 px, con el backend sin Docker y los datos de prueba:
  - Pantalla de PIN y login del panel.
  - Inicio del POS con la barra lateral contraída y expandida.
  - Panel en «Usuarios», con los estilos calculados del ítem activo (fondo `#eff4ff`,
    texto `#1e3a8a`, franja `#2563eb`).
- No se probó en celular.

## Pendiente

- `docs/producto/ui-ux/03-pos-rediseno.md` y `04-kiosco-pensionista.md` todavía describen el verde.
- El kiosco y la tipografía de la web, como se dijo arriba.
- La app móvil usa `#0B7A6C` como color principal: coordinar con Andrey si también pasa a azul.
- HT-07 (56 px y teclado único) sigue abierta; este cambio no la cierra.

## Gestión de mesas y logo (2026-10-28)

### Qué se pidió

- Rehacer «Gestión de mesas» del POS (plano, lista y unir mesas) según tres diseños del usuario, con la paleta azul.
- Después, con la pantalla a la vista, el usuario corrigió:
  - Sin panel «Mesa seleccionada»: tocar una mesa abre el diálogo de siempre.
  - Sin panel «Resumen de unión»: «Fusionar» va junto a «Cancelar unión».
  - El diálogo de fusión suma los comensales de las mesas, y al fusionar se queda en el mapa.
  - El plano tiene que parecerse al diseño: una sala, no el piso beige.
- Usar el logo de RIKO'S en las dos barras laterales, en lugar del cuadro «R» con el texto. Más adelante el restaurante lo
  podrá cambiar desde Configuración → Mi restaurante, que hoy dice «Próximamente».

### Qué se construyó

- **`features/venta/MapaDeMesas.tsx` y `estilos/mapa.css`:**
  - Arriba: título «Gestión de mesas», pestañas por salón con su número de mesas, buscador (atenúa las que no
    coinciden) y Plano/Lista.
  - La lista es una tabla y solo aparece en pantalla ancha (≥ 1180 px).
  - El plano es una sala:
    - Piso gris claro, pared con una puerta abajo, «ACCESO» y plantas en las esquinas.
    - Las mesas se agrandan hasta llenar el ancho (hasta 1,5×).
    - Zoom y «Ajustar vista» flotan dentro del plano.
  - En el plano ya no se usa el `colorPiso` del salón.
- **Unir mesas:**
  - Se tocan las mesas: la primera es la principal.
  - «Fusionar N mesas» se activa con dos o más.
  - El diálogo se titula «Mesa 1 + Mesa 2 + …» y propone como comensales la suma de las capacidades (máximo 30, el
    límite del servidor).
  - Al fusionar se queda en el mapa; el pedido se toma tocando la mesa fusionada.
- **Logo:**
  - `public/marca/logo-predeterminado.svg` es el logo que entregó el usuario (vectorial, sin scripts ni referencias
    externas).
  - `comun/componentes/LogoDelRestaurante.tsx` lo muestra y define `MEDIDAS_DEL_LOGO` (caja de 170 × 40 px).
  - Barras del POS y del panel: logo con la barra expandida; la inicial «R» con la barra contraída, donde el logo
    no cabe.

- **Título en el cabezal (pedido del usuario el mismo día):**
  - En Mesas, el cabezal del POS ya no muestra «Punto de venta / Mesas» ni el botón de inicio. En su lugar va
    «Gestión de mesas» con su subtítulo, y se mantienen el local y «Sistema en línea».
  - `PosShell` recibe `tituloDePantalla` y ofrece el lugar con `comun/hooks/ranuraDelCabezal.ts`; la pantalla pone su
    título ahí con un portal.
  - Fuera del POS (Salón y mesas del panel) no hay lugar en el cabezal y el título se dibuja en el contenido.
  - Pestañas, buscador, Plano/Lista y «Unir mesas» (o «Cancelar unión» / «Fusionar») quedan en una sola fila fija.
  - Solo el panel del salón se desplaza.

- **Tamaño de las mesas en el editor de plano del panel (`features/salones/PantallaPlano.tsx`, `salones.css`):**
  - Con «Mover mesas» activo, un clic elige la mesa (ya no abre propiedades; eso es con doble clic).
  - La esquina de la mesa elegida se arrastra para cambiar el tamaño. Redondas y cuadradas quedan parejas, con un
    mínimo de 40 px, y la mesa no se sale del plano.
  - Abajo, en lugar de «Añadir mesa», aparece la barra de la mesa elegida: −/+ (de 10 % en 10 %), tamaños rápidos
    Chica / Mediana / Grande (0,8×, 1× y 1,25× del tamaño de fábrica) e «Igualar las N mesas».
  - «Igualar» copia el ancho y el alto a todas las mesas activas del salón. Así lo eligió el usuario, sabiendo que una
    rectangular copiada de una cuadrada queda cuadrada. Una redonda o cuadrada copiada de una rectangular toma el lado
    corto, para seguir pareja.
  - Como antes, nada se guarda hasta «Guardar plano».

- **La sala del plano como el diseño del usuario (`features/venta/DecoradoDeSala.tsx`):**
  - Paredes grises con ventanas, puerta arriba con piso de madera y dos jardineras, y plantas en las cuatro esquinas.
  - Abajo, el hueco del acceso con un triángulo verde. El piso es claro.
  - Mesas libres verde menta con borde verde y sillas en píldora del mismo tono; los otros estados usan su color.
  - Las sillas las dibuja `sillasDelPlano`: una por persona, una en cada cabecera desde 3 personas y el resto arriba y
    abajo. El diseño muestra 8 sillas en las mesas de 6; aquí se respeta la capacidad.
- **Barras laterales (POS y panel) en celeste suave:** fondo `#eaf3fd`, ítem activo en blanco con la franja azul,
  hover `#dbeafb`. Se cambió con los tokens `--color-lateral-*`, que ahora también usa el POS.

### Cómo se verificó

- `npx tsc -b --force` y `eslint` sobre los archivos tocados, sin errores.
- Plano del POS a 1440 px: se ven la sala, las mesas y las sillas.
  - Las mesas de la base local estaban guardadas chicas (69 px) y pegadas; con el texto fijo se salía de la mesa y las
    sillas de la 7 y la 8 se tocaban.
  - Arreglo: el texto y las sillas ahora se miden contra el lado corto de cada mesa (`--lado` y `sillasDelPlano`).
  - Además, las 8 mesas del Salón principal se separaron como en el diseño, en `datos-de-prueba.sql` y con un
    `UPDATE` en la base local (posiciones 150/390/630/870 × 120/360; mesas de 84 px, o 160 × 84 las rectangulares).
  - El zoom y «Ajustar» quedaron debajo de la pared de abajo, junto al acceso, para no tapar las plantas.
- Editor de plano, a 1440 px:
  - Arrastrar la esquina llevó la mesa 1 de 84 a 114 px.
  - «Grande» dejó la mesa en 105 × 105.
  - «Igualar las 8 mesas» las dejó todas en 105 × 105, también las dos rectangulares.
  - Después se usó «Descartar»: el plano de prueba quedó como estaba.
- En el navegador a 1440 px (y en el panel angosto):
  - La fila de pestañas, buscador, vistas y unir queda en una sola línea, también en modo unión.
  - Solo el panel se desplaza (804 px de contenido en 691 px visibles); la página no se mueve.
  - Inicio conserva su ruta.
  - Tocar una mesa abre el diálogo «Abrir Mesa 1».
  - En modo unión, los dos botones quedan en una fila.
  - Con tres mesas de 4, el diálogo propone 12 comensales.
  - El logo mide 165–169 × 40 px en las dos barras.
- **No se confirmó una fusión real**, para no dejar un pedido de prueba en la base local. Que después de fusionar se
  quede en el mapa no se vio en pantalla, solo en el código.

### Pendiente

- Subir el logo desde Configuración → Mi restaurante (hoy «Próximamente»). Antes de subirlo, la pantalla debe recomendar
  `MEDIDAS_DEL_LOGO`:
  - Horizontal, de proporción cercana a 4:1.
  - Fondo transparente.
  - SVG, o PNG de al menos 680 × 160 px.
  
  El logo subido se ajusta a la caja sin deformarse. Para guardarlo ya existe `local.logo_url` (V12), que es por local. Falta decidir si el logo es del restaurante o de cada local, y cómo se sube el archivo.
- La pantalla de PIN, el login y el kiosco siguen con el texto «RIKO'S», no con el logo.

## Pantalla del pedido (2026-10-28)

### Qué se pidió

Dejar la pantalla de tomar el pedido igual al diseño que entregó el usuario («debe ser tal cual»), con la paleta azul.

### Qué se construyó

Clase `.ped5` sobre la pantalla existente: no cambia ninguna llamada a la API ni ningún diálogo. El CSS está en un
bloque propio al final de `pos.css`.

- **Cabecera (`PantallaPedido.tsx`):**
  - «← Volver a mesas» (o «Volver» para llevar).
  - «Mesa 04» grande con la pastilla «En atención» (o «Cerrado»).
  - La línea «Pedido #0001 · Almuerzo · 1 comensal · Mozo: …», más el cliente si lo hay. Se quitó «Desde HH:MM»,
    que el diseño no muestra.
  - «Cambiar mesa» con borde e ícono.
- **Comensales:** rótulo «COMENSALES», cada persona en una tarjeta con ícono, nombre e importe, y «+ Añadir persona»
  con borde punteado.
- **Catálogo (`CatalogoDelPedido.tsx`):**
  - Buscador «Buscar producto por nombre…» con **Ctrl + K** (también Cmd + K), y Mosaico/Lista al lado.
  - Categorías en píldoras con ícono: Todo, Entrada (hoja), Segundo (campana) y Extra (estrella).
  - Título «Catálogo».
  - Tarjetas con la foto arriba (o un fondo con cubiertos si el producto no tiene foto), nombre, detalle, precio en
    azul y un botón redondo «+».
- **Orden (`OrdenDeLaMesa.tsx`):**
  - Círculo azul, «Orden de la mesa» y «Mesa 04 · Pedido #0001».
  - La persona con avatar y «Identificar pensionista» con borde.
  - Vacía: dibujo de una boleta con «Tu pedido está vacío / Selecciona un producto para agregarlo a Persona 1.».
  - Subtotal, total grande y la acción principal a todo el ancho, con la ayuda debajo.

### Decisiones

- **El botón grande dice «Comandar», no «Terminar pedido».**
  - En el diseño, ese botón está apagado con la orden vacía y es el paso siguiente. En el sistema, «Terminar pedido» es
    otra cosa: cerrar sin cobrar un pedido sin productos (DT-059).
  - Por eso el botón grande comanda: «Comandar N ítems nuevos →», y con la orden vacía queda apagado («Comandar
    pedido», con «Agrega productos para continuar»).
  - «Terminar pedido sin productos» quedó como enlace chico debajo, solo cuando aplica.
- «Cobrar» se mantiene. Es la acción principal cuando todo está en cocina, y secundaria si además hay algo por comandar.

### Cómo se verificó

- `npx tsc -b --force` y `eslint` sin errores.
- A 1440 px, con el pedido 0001 de la mesa 4:
  - El catálogo queda en 4 columnas y la orden vacía con su dibujo.
  - Se agregó «Huevo frito»: aparece la línea y «Comandar 1 ítem nuevo» se activa. Después se quitó: la orden quedó
    vacía como estaba.
- A 390 px: carrito flotante y catálogo en 2 columnas, sin desborde horizontal.
- Corrección pedida por el usuario: tarjetas parejas y foto más alta. La foto pasó a proporción 25:23. El texto va en
  una caja de alto fijo: nombre en una línea (con «…» y el nombre completo al pasar el mouse), detalle en dos líneas y
  el precio siempre abajo. Medido a 1440 px: las 8 tarjetas miden 285 px y el «+» queda a la misma altura en todas.
- Los productos de prueba no tienen foto, así que se ve el fondo con cubiertos. Con foto (subida en la carta) se ve la
  imagen.

## Diálogo del menú del día (2026-10-29)

- Pedido del usuario: «más ancho y que se vea mejor».
- `Modal` acepta `ancho="amplio"` (hasta 880 px). Lo usa `DialogoMenu`, que se rehízo:
  - Arriba, el precio y cuántas partes faltan elegir («1 de 2 elegidas»).
  - Cada parte con opciones es un paso numerado («1 Elige la entrada», «2 Elige el segundo»), lado a lado.
  - Las opciones son tarjetas con ícono y marca. El número del paso pasa a ✓ al elegir.
  - Lo de una sola opción va en «Incluye» (refresco, postre) y la nota para cocina es un área de texto con ejemplo.
  - El botón dice «Agregar al pedido · S/ 12.00».
- La lógica no cambió: las mismas validaciones y la misma llamada.
- Verificado a 1440 px en el navegador (abrir, elegir la entrada, cerrar sin agregar). `tsc` y `eslint` sin errores.

## Pedido de la mesa por persona (2026-10-29)

- Pedido del usuario: el panel de la orden «idéntico» al diseño, con «Identificar pensionista» en el menú ⋮ de cada
  persona, y las tarjetas de comensales más chicas y delgadas.
- **`OrdenDeLaMesa.tsx` reescrito:**
  - Cabecera «Pedido de la mesa · Mesa 04 · N productos» con «Por persona / Todos».
  - «Por persona»: una tarjeta plegable por comensal (P1, P2…) con «N productos · S/ total».
    - Su ⋮ lleva «Identificar / Cambiar pensionista»; como antes, solo mientras la persona no tenga productos.
    - Tocar la cabecera elige a esa persona (los productos del catálogo van a ella) y la tarjeta se resalta.
  - Cada línea tiene foto, detalle (opciones del menú, plan, táper), nota, estado (Por enviar / En cocina / Anulado),
    precio y cantidad −/+. Con cantidad 1, el «−» pasa a ser un tacho que quita la línea.
  - El lápiz abre las acciones de la línea: nota para cocina, pasar a otra persona, anular y quitar. Así siguen
    disponibles todas las que había antes.
  - «+ Agregar nota a cocina» edita la nota del último producto por enviar de esa persona.
  - «Todos» es una sola lista con la sigla de cada persona.
  - Pie: «N productos · N personas», total de la mesa y «Enviar a cocina • N productos →» (es el mismo «Comandar»,
    contado en unidades) con «Revisa el pedido antes de enviarlo». Se mantienen Cobrar y «Terminar pedido sin
    productos».
- **Comensales:** tarjetas de 46 px de alto (antes 60), texto de 13,5 px y el tacho centrado.
- **Verificado:**
  - A 1440 px: se agregaron papas y huevo a la Persona 1 y café a la Persona 2. Se probaron el ⋮, las acciones de
    la línea y «Todos». Después se quitaron las tres líneas (el total volvió a S/ 0.00).
  - A 390 px: el cajón del carrito abre con las tarjetas por persona.
  - `tsc` y `eslint` sin errores.

## Identificar al pensionista con productos ya cargados (2026-10-29)

### Qué se pidió

El usuario preguntó por qué, después de agregar productos, ya no se podía identificar al pensionista. Pidió permitirlo y
recalcular los precios.

### Por qué no se podía

`AsignarPensionista` y `VincularPreTicket` rechazaban a una persona con líneas (`PERSONA_CON_LINEAS`), porque en US-028
todavía no había recalculo: se dejó para la entrega 4 (US-029 CA5). Esa entrega hizo el recalculo solo para
`TrasladarLineas` y nadie volvió a quitar esta regla.

### Qué se construyó

- **`RecalculoAlIdentificar`** (`venta/aplicacion`) aplica las mismas reglas que pasar productos a un pensionista:
  - El menú normal y la comida del plan van al precio de su plan, con «segundo del día» y táper.
  - La carta, el menú extra y el extra libre conservan su precio.
  - Un menú o una comida del plan de varias unidades da `CANTIDAD_INVALIDA`, y algo ya cobrado da `LINEA_COBRADA`.
  - Las líneas anuladas no cuentan.
  - Primero calcula todo sin tocar nada. Si algo falla, la persona sigue sin identificar y ningún precio cambia (el
    pre-ticket tampoco queda vinculado).
  - Si todo sale bien, guarda los precios y deja en la bitácora (`transferencia`, tipo
    `identificacion_de_pensionista`) cuánto cambió el total (`ajuste`).
- **Casos de uso:** `AsignarPensionista` y `VincularPreTicket` lo usan en lugar del rechazo. `PERSONA_CON_LINEAS` queda
  solo para quitar a una persona.
- **Pantalla:** el ⋮ de la persona permite identificar aunque tenga productos, salvo que tenga algo cobrado. Avisa que
  sus productos se recalculan al precio de su plan. Hay mensajes nuevos para `CANTIDAD_INVALIDA`, `PLAN_SIN_PRECIO` y
  `LINEA_COBRADA` (también en el pre-ticket).
- La comanda que ya salió no cambia (es una foto); solo cambia el precio de la cuenta.

### Cómo se verificó

- `RecalculoAlIdentificarPensionistaTest` (6 pruebas nuevas):
  - El menú pasa a S/ 8.00 del plan, con un ajuste de −400 en la bitácora, y la carta conserva su precio.
  - Un menú ×2 impide identificar sin cambiar nada; lo mismo si el plan no tiene precio.
  - Lo cobrado impide identificar y una línea anulada no lo impide.
- `VincularPreTicketTest`: la prueba que esperaba el rechazo ahora comprueba que vincula y que la carta conserva su
  precio.
- Siguen pasando `PersonasDelPedidoTest`, `RecalculoAlTrasladarTest` y `VincularPreTicketTest`.
- Suite completa: 817 pruebas, 9 fallan.
  - 8 ya existían y son ajenas a este cambio:
    - `ArquitecturaDeModulosTest`: el ciclo conocido.
    - `PensionistaTest` y `PensionistasAccesoPorRolTest`: esperan un PIN de 4 dígitos y desde el sprint 8 es de 6.
    - `KioscoControllerTest`: le falta el limitador de solicitudes.
  - **La novena sí era de este cambio** y se pasó por alto al leer la lista:
    `LineasDelPedidoTest.no_se_asigna_un_pensionista_a_una_persona_que_ya_tiene_lineas`, que esperaba la regla vieja.
    Se corrigió en el cambio siguiente (juntar líneas iguales).
- En el POS, pedido 0001 de la mesa 4: se agregó un menú del día a la Persona 3 (S/ 12.00) y se la identificó como
  Ana Lucia Mendoza (pensionista de prueba). El menú pasó a S/ 10.00 «Precio de su plan». Después se quitó el menú; la
  Persona 3 quedó identificada como Ana Lucia, sin productos.

## Agregar lo mismo otra vez suma la cantidad (2026-10-29)

### Qué se pidió

Al agregar dos veces el mismo producto (dos huevos fritos), salían dos líneas iguales. El usuario quiere una sola línea
con cantidad 2. Un menú del día con otras opciones sí debe ir en otra línea.

### Qué se construyó

- **`AgregarLinea`:** antes de crear la línea busca una igual en la misma persona (`lineaIgual`). Si la encuentra, le
  suma la cantidad y devuelve esa línea. Es igual si cumple todo esto:
  - Está por enviar (pendiente).
  - Es del mismo tipo y el mismo producto.
  - No tiene nota, ni la tiene la nueva.
  - Tiene el mismo precio, táper y origen de tarifa: si cambió el precio de la carta o del plan, va aparte.
  - Si es un menú, tiene exactamente las mismas opciones elegidas.
- **No se juntan:**
  - El extra libre (precio a mano).
  - El menú y la comida del plan de un pensionista, que van uno por línea para contar el segundo consumo del día.
  - Lo que ya fue a cocina.
  - Lo de otra persona.
  - Una línea con id propio del cliente: así reintentar un pedido sin conexión no suma dos veces.
- La pantalla no cambió: el servidor devuelve el pedido y el contador muestra la cantidad.

### Cómo se verificó

- `JuntarLineasIgualesTest` (5 pruebas): el mismo producto dos veces da cantidad 2; 2 + 3 da 5; con nota va aparte; lo
  comandado no se junta; cada persona tiene su línea.
- `MenuDelDiaEnPedidoTest` (2 pruebas nuevas): el mismo menú con las mismas opciones suma; con otra opción va aparte;
  el de un pensionista no se junta.
- Antes de escribir el código, 3 de estas pruebas fallaban (rojo); después pasan.
- Suite completa: 824 pruebas; fallan solo las 8 que ya fallaban (Modulith, kiosco y PIN de pensionistas).
- En el POS: dos «Café pasado» a la misma persona quedaron en una línea con cantidad 2 (S/ 8.00). Después se quitaron.
- De paso: tras recargar, la tarjeta de una persona identificada decía «Pensionista». Ahora usa la etiqueta que deja el
  servidor (el nombre del pensionista).

## Comensales parejos y línea compacta en celular y tableta (2026-10-29)

- **Pensionista:** su tarjeta de comensal mide lo mismo que las demás. Lleva otro ícono (persona con check, azul), el
  nombre con «…» si es largo y «S/ 0.00 · Pensionista» en la segunda línea. Antes se repetía el nombre («etiqueta ·
  nombre») y la insignia la hacía más alta.
- **Comensales:** van en una sola fila que se desliza, en todas las pantallas; en celular el rótulo va arriba. Antes,
  con 4 o 5 personas, quedaban en varias filas de tamaños distintos. En los datos del pedido, el «·» queda al final de
  la línea y no al comienzo de la siguiente.
- **Línea del pedido:** foto, nombre y detalle, precio; debajo, el estado a la izquierda y el lápiz y la cantidad a la
  derecha.
  - Del menú se muestra solo lo que se elige (entrada, segundo); el refresco y el postre incluidos ya no se repiten.
  - El detalle se corta en 2 líneas.
  - La línea de un menú bajó de más de 250 px a 124 px en un celular de 390 px.
- **Cajón en celular:** «Cerrar» es una X redonda en la fila del título, sin tapar «Pedido de la mesa».
- Verificado a 390 y 1440 px: las 5 tarjetas de comensales en una fila de 46 px y sin desborde de la página.

