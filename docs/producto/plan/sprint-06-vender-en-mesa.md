# Sprint 6 — Vender en mesa

**Historias (del backlog):** US-027 (mapa de mesas), US-028 (tomar el
pedido), US-029 (reorganizar cuentas), US-030 (comanda), US-031 (anular)
y HT-10 (prueba con hardware real). Flujos en
`flujos/05-ep05-pos-mesas-cocina.md`; modelo en
`modelo-de-datos/06-venta-pos-comandas--ep05.md`.

**Estado:** especificado y APROBADO el 2026-09-29; **entrega 1 (mesas y mapa) hecha el 2026-09-30**, ver `US-027-mapa-de-mesas.md`. Antes: pendiente de aprobación del
usuario** antes de programar. HT-10 queda **pendiente a propósito**
(ver 3.6): no hay impresora todavía.

**Cómo se llegó a esto:** brainstorming con el usuario el 2026-09-29, una
parte a la vez (impresora, pensionista en la mesa, mesas y bloqueo,
canales, estado de la mesa, toma del pedido y precios, comanda y
anulaciones, entregas). Lo que dijo el usuario va marcado como tal; lo
que asumí yo, como supuesto.

## 1. Qué queremos lograr

Que el mozo tome pedidos de verdad desde el POS: ver las mesas de su
local, abrir un pedido, asignar cada producto a una persona (con mesas
mixtas de clientes normales y pensionistas), mandarlo a cocina en una
comanda imprimible y corregirlo (anular, mover, dividir) **sin cobrar
todavía**. Cobrar y cerrar es del sprint 7.

**Éxito del sprint:** en una mesa mixta con un cliente normal y un
pensionista, cada uno ve su precio correcto (el del plan para el
pensionista, el normal para el otro); se comanda, se agrega algo, se
reimprime marcado como copia, se anula una línea con motivo y se divide
la cuenta, todo sin perder el detalle.

## 2. Contexto que dio el usuario

- **Impresora (respuesta del usuario, opción b):** todavía no la tienen.
  La comanda se hace como **vista imprimible desde el navegador**; HT-10
  queda pendiente hasta tener el hardware.
- **Pensionista en la mesa (opción a):** el mozo lo **busca por nombre o
  DNI** y lo asigna a una persona de la mesa. Es un paso provisional: el
  ticket del kiosco (US-019 y US-020, sprint 8) lo reemplaza, y la
  búsqueda queda como respaldo si el kiosco falla.
- **Bloqueo de mesas:** **el mozo también puede bloquear y liberar**, no
  solo el administrador y el cajero. Siempre con motivo.
- **Canales (opción b):** entran **mesa y para llevar**. Recojo
  programado y delivery son del sprint 12; "canal" es un dato del pedido,
  así que no habrá que rehacer nada.
- **Estado de la mesa (opción B):** derivado, no guardado (3.2).
- De sprints anteriores sigue valiendo: el pensionista se reconoce por
  su ticket; el táper y el segundo consumo del día se configuran en
  Parámetros de cobro; el pensionista come en cualquier local.

## 3. Decisiones, con su razón

### 3.1 Un módulo propio `venta`

Mesas, pedidos, personas, líneas y comandas van en el módulo `venta`.

**Por qué:** es el centro de todo lo que sigue (caja, delivery,
inventario, reportes) y nadie depende de él todavía. Depende de
`pensionistas` (motor de precios, búsqueda), `catalogo` (menú, agotados,
productos y motivos) e `identidad` (locales, usuarios). Esas
dependencias se exponen con `@NamedInterface` y las vigila
`ArquitecturaDeModulosTest`; no se asume acceso libre.

### 3.2 El estado de la mesa se **deriva**, no se guarda

**Decisión del usuario (opción B).** La mesa guarda solo lo que es dato
propio: número, nombre, capacidad, activa, **si está bloqueada** (con
motivo, quién y cuándo) y **a qué mesa principal está unida**. Los
estados se calculan al consultar:

| Estado | Cuándo |
|---|---|
| bloqueada | está marcada como bloqueada |
| unida | apunta a una mesa principal |
| pendiente de cierre | su pedido está pendiente de cierre |
| ocupada | tiene un pedido **comandado** *(cambio del 2026-10-01, ver abajo; antes: abierto o comandado)* |
| libre | ninguna de las anteriores |

Orden de prioridad: bloqueada, unida, pendiente de cierre, ocupada,
libre. Una mesa bloqueada con un pedido abierto **no se puede**: para
bloquear tiene que estar libre.

**Cambio de regla (usuario, 2026-10-01): una mesa solo está "ocupada" cuando
su pedido se comanda.** Abrir una mesa y entrar a tomar el pedido sin
comandar **no la ocupa** (tampoco al unir mesas). *Por qué:* al probar el
mapa, unir 3 mesas abría el pedido de la principal y la dejaba "ocupada" sin
haber pedido nada; para el local, la mesa está ocupada cuando la cocina ya
recibió algo. *Casos decididos con el usuario:*
1. Si dos mozos tocan una mesa "libre" que ya tiene un pedido abierto sin
   comandar, el segundo **entra a ese mismo pedido** (no se crea otro ni hay
   error).
2. Una mesa libre con pedido abierto se dibuja libre **con una marca "tomando
   pedido"**, para que nadie la ofrezca a otro cliente.
3. Un pedido abierto sin comandar sigue apareciendo en Órdenes activas como
   "Abierto"; se limpia con "Terminar pedido".
4. Al separar mesas unidas, la principal con pedido vacío vuelve a quedar como
   estaba (el arreglo de cerrar el pedido vacío se simplifica).
5. Una mesa con pedido abierto sin comandar **no se bloquea** (hay que
   terminar el pedido primero).
Orden de prioridad de estados: bloqueada, unida, pendiente de cierre,
ocupada (comandado), libre.

**Por qué (el modelo derivado):** el modelo original guardaba `mesa.estado` "porque calcularlo
sería caro". A escala de un local (decenas de mesas) no lo es, y guardar
lo mismo en dos lugares puede desincronizarse, sobre todo cuando el
local trabaje sin internet y sincronice (sprint 10). Es el mismo criterio
del saldo del pensionista y de los estados de la programación semanal.

**Bloquear (RF-028, que estaba sin definir):** solo una mesa **libre**, con
motivo obligatorio (texto libre; ej. "mesa rota", "reservada"), por
mozo, cajero o administrador. Liberar la deja libre. Ambas cosas quedan
en la bitácora. Una mesa **unida** no se bloquea: primero se separa.

### 3.3 Pedido, personas y líneas

- **Pedido:** canal (`mesa` o `para_llevar`), servicio (desayuno,
  almuerzo o cena), mesa (solo en canal mesa), mozo, estado
  (`abierto` → `comandado` → `pendiente_cierre`; `cerrado` y `anulado`
  llegan con la caja) y número correlativo por local y día.
- **Persona de la cuenta:** "Persona 1", "Persona 2"…, o un pensionista.
  Aquí se separan los consumos de una mesa mixta **desde la toma del
  pedido** (RN-060), no al cobrar.
- **Línea:** producto, cantidad, observación, tipo de menú, estado
  (`pendiente`, `comandado`, `anulado`), el **precio unitario y su
  origen** (público, plan o extra) y, si es menú, las opciones elegidas
  por componente.
- **Ids que genera el cliente (ULID)**, igual que el libro de la
  cuenta: reenviar el mismo pedido o línea dos veces no duplica. Es lo
  que necesita el trabajo sin internet del sprint 10.
- **Un producto pertenece a una sola persona.** Compartir un plato no
  está definido (RF-008): se resuelve dividiendo la línea a mano.
- **Un pensionista inactivo no se asigna** (RN-034). Un mismo
  pensionista no puede estar dos veces en el mismo pedido.

**Por qué el precio se guarda al agregar:** la caja necesita el total en
pantalla y el mozo no debe ver que un precio se mueve solo. Se
recalcula únicamente cuando cambia quién consume esa línea (3.8). El
**costo y la receta** del snapshot se congelan **al cerrar**, en el
sprint 7, como manda el modelo (RN-005).

### 3.4 Cómo se decide el precio de cada línea

Siempre con el motor del sprint 5; el mozo **nunca** escribe un precio.

- **Persona normal:** precio normal del producto.
- **Pensionista:** precio de su plan para la comida del servicio del
  pedido; el desayuno y la cena son la comida fija del pensionista.
- **Segundo consumo del día:** se cuenta buscando los menús ya pedidos
  por ese pensionista, ese día y ese servicio, **en cualquier local**
  (la cuenta es única), sin contar las líneas anuladas. Se aplica la
  regla de Parámetros de cobro (por defecto, precio del plan).
- **Menú extra:** precio completo para todos (RN-023).
- **Agregados y carta:** precio normal, sin táper.
- **Para llevar:** el motor suma el **táper por cada menú** (RN-009);
  el pedido guarda el total de táper.
- **Falta un dato** (producto sin precio, plan sin precio, táper sin
  monto): el motor lo avisa. Un producto sin precio **no se puede
  agregar** ("no tiene precio"); un táper sin monto se agrega sin cargo
  y se avisa. Nunca queda un precio en cero inventado.

### 3.5 Qué se ofrece, y qué servicio es

- **Menú del día:** solo el de la programación semanal **confirmada**
  para hoy (`ConsultaMenu.menuConfirmadoDel`). Un componente con varias
  opciones pide elegir; con una sola se asigna y **la fila se guarda
  igual** (RN-038). Un día sin menú confirmado ofrece solo la carta y lo
  avisa.
- **Agotados:** los del **local de la sesión** no se ofrecen
  (RN-066); las líneas ya comandadas no cambian.
- **Servicio del pedido:** el horario de servicio del local a la hora
  en que se abre (los horarios que ya cargó cada local). Fuera de todo
  horario, el mozo elige uno al abrir el pedido. Supuesto mío.

### 3.6 La comanda (US-030) y HT-10

- **Comandar** toma las líneas pendientes, crea una comanda numerada
  por pedido con **una foto** de esas líneas (`detalle_comanda`) y las
  pasa a comandadas. Agregar platos después crea **otra** comanda solo
  con lo nuevo (RN-062).
- **Vista imprimible** en el navegador: pedido, mesa o canal, mozo,
  hora, productos por persona, componentes elegidos, observaciones y
  tipo de entrega. Funciona con cualquier impresora y sin internet.
- **Cada impresión se registra** (`impresiones`, última impresión). Desde
  la segunda sale con la leyenda **COPIA** (RN-063).
- **Sin estado "listo"**: cocina avisa de palabra o con campana; no se
  modela un estado digital de preparado (RN-064).
- **Una sola impresora por local:** hoy es un dato del local, sin
  modelo de hardware. `estado_impresion` se usa como `pendiente` /
  `impresa`; `fallida` queda para cuando exista un agente de impresión.
- **HT-10 queda pendiente** hasta tener la impresora (RF-012, RF-016).
  Si la prueba real cambia el diseño, se abre una historia nueva; no
  bloquea este sprint.

### 3.7 Anular (US-031)

- **Permiso `venta.anular`** (ya existe: administradores y cajero; el
  mozo no, salvo que se lo den desde Roles) y **motivo** de los
  configurables, tipo `anulacion` (RN-072). Sin permiso, el sistema lo
  impide; con permiso, exige el motivo antes de confirmar.
- **Antes de comandar:** la línea se anula, no se cobra y no toca
  inventario.
- **Después de comandar:** no se cobra, pero queda marcada **anulada
  después de comandar** porque la receta ya se gastó (RN-065). El
  descuento real de inventario lo hace el sprint 9; este sprint deja la
  marca y el dato listos.
- **Bitácora:** usuario, fecha, hora, local, motivo y producto. El
  reporte dedicado de anulaciones es del sprint 14.

### 3.8 Reorganizar (US-029)

Una operación básica y dos derivadas:

- **Trasladar líneas** de una persona a otra del mismo pedido (o a una
  persona nueva). **Dividir por productos** es esto mismo con una
  persona nueva, conservando detalle y precio de cada línea.
- **Recálculo (CA5):** si la persona de destino es de otro tipo (de
  cliente normal a pensionista o al revés), el precio se recalcula con
  el motor para la nueva persona. Una línea ya comandada conserva su
  lugar en la comanda que salió.
- **Mover el pedido** a otra mesa **libre**: se reasigna y ambas mesas
  cambian de estado solas.
- **Unir mesas:** una mesa (la que no tiene pedido, o cualquiera si
  ninguna lo tiene) se une a una principal, y el pedido se opera en
  conjunto. **Separar** las deja independientes.
- Todo solo **antes del cierre** (sprint 7).

### 3.9 Canales

Solo `mesa` y `para_llevar`. Para llevar es un pedido **sin mesa**, con
el táper por menú. Recojo y delivery llegan en el sprint 12 sobre el
mismo pedido.

### 3.10 Permisos

Se usan los existentes y se agregan dos:

| Permiso | Para qué | Roles |
|---|---|---|
| `venta.ver` (ya existe) | ver mapa, pedidos y comandas | todos los de sala |
| `venta.crear` (ya existe) | abrir pedidos y agregar líneas | mozo, cajero, administradores |
| `venta.modificar` (ya existe) | reorganizar cuentas | **se agrega al mozo**; cajero y administradores ya lo tienen |
| `venta.anular` (ya existe) | anular líneas | cajero y administradores |
| `venta.bloquear_mesa` (**nuevo**) | bloquear y liberar mesas | mozo, cajero, administradores |
| `venta.administrar` (**nuevo**) | crear y editar mesas de un local | administradores |

`venta.modificar` en el mozo es un cambio de rol: reorganizar cuentas es
parte de su trabajo (US-029). Cocina y motorizado no entran al pedido.

### 3.11 Decisiones de la entrega 2 (2026-09-30)

- **Desayuno y cena del pensionista** (respuesta del usuario): el pensionista paga el precio de su plan por
  "lo que hay ese día"; si pide algo más (el ejemplo del usuario: un huevo de más), eso es un **extra** y es
  poco frecuente. Se modela con dos tipos de línea: **"Desayuno del plan" / "Cena del plan"** (solo para una
  persona pensionista y solo en ese servicio; sin producto de la carta, precio del plan) y **"Extra"**.
- **El extra lo escribe el mozo** (descripción y precio): es el **único** caso en que el mozo pone un precio.
  Queda marcado como manual (`tarifa_origen = 'manual'`) y en la bitácora (evento `precio_manual`), para poder
  revisarlo. Un producto de la carta sigue cobrándose a su precio normal y sin táper. Supuesto mío: el usuario
  dijo "colocar el precio, creo yo"; si prefiere que el extra salga de un producto con precio, se cambia.
- **Almuerzo:** "Menú del día" y "Menú extra" son líneas con producto del sistema (menú normal y menú extra) y
  sus componentes elegidos; solo en servicio almuerzo. Un pensionista pide **una** comida del plan por línea
  (para poder contar el segundo consumo del día, entre locales).
- **Se derivan, no se guardan:** subtotal, táper y total del pedido y de cada persona (como el saldo de la
  cuenta). El precio de cada línea **sí** se guarda al agregarla.
- **Personas:** un pensionista se asigna a una persona sin líneas (recalcular al cambiarla es de la entrega 4).
- **Dos planes de implementación:** 2A (backend: pedido, personas, líneas, precios, API) y 2B (la pantalla del
  POS), para que la 2B se escriba con la API ya fija.

## 4. Auditoría antes de programar

### 4.1 Cambios al modelo del bloque 6

- **`mesa.estado` deja de ser columna.** Se reemplaza por `bloqueada`
  (booleano), `motivo_bloqueo`, `bloqueada_por` y `bloqueada_en`. Los
  estados se derivan (3.2). RN-058 se mantiene: siguen siendo cinco.
- **`pedido.estado`:** en este sprint solo se usan `abierto`,
  `comandado` y `pendiente_cierre`.
- **`pedido.cargo_taper`:** se llena al agregar líneas de menú en
  para llevar.
- **Ids generados por el cliente** en `pedido`, `cuenta_persona`,
  `detalle_pedido`, `comanda` (idempotencia).
- **`comanda.estado_impresion`:** sin uso de `fallida` por ahora.
- **Sin `turno_caja_id`, comprobante ni receptor** todavía: son del
  sprint 7 y se agregan con su migración, sin tocar las de este sprint.

### 4.2 La numeración de reglas del modelo vuelve a estar corrida

Contra `flujos/90-reglas-de-negocio-consolidadas.md`:

| El modelo o las historias decían | Es en realidad |
|---|---|
| RN-064 (anular después de comandar descuenta receta) | RN-065 (la RN-064 real es "cocina avisa de palabra") |
| RN-069 (anular requiere permiso y motivo) | RN-072 |
| RN-042 (opciones del menú, mesa) | RN-038 |
| RN-059 (estados de mesa) | RN-059 (coincide) |
| RN-060, RN-062, RN-063, RN-066 | coinciden |

Se corrige en el modelo al escribir esta especificación.

### 4.3 Contraste con la fuente del dueño

`07` dice que faltan: la cantidad de mesas por local (RF-010), quién y
cuándo bloquea una mesa (RF-028), el precio del táper, la ficha del
cliente regular (RF-017), los productos compartidos (RF-008) y el modelo
de la impresora (RF-012). Este sprint decide **RF-028** (3.2, pedido del
usuario), **no** modela RF-008 (3.3), **no** crea ficha de cliente
regular (la persona normal es solo una etiqueta) y difiere RF-012. La
cantidad de mesas la carga el administrador; para probar se cargan
mesas de ejemplo.

## 5. Las cuatro entregas

Estándar de cada entrega (el del sprint 4 y 5): prueba antes o junto con
el código, una prueba por rol en cada endpoint nuevo, bitácora al
cambiar algo, pantallas con sus cuatro estados (cargando, vacío, error,
con datos) y el documento de la historia en
`rikos-backend/doc/historias/`.

1. **Mesas y mapa (US-027).** ✅ **Hecha (2026-09-30)**, ver `rikos-backend/doc/historias/US-027-mapa-de-mesas.md`. Migración con `mesa`; crear y editar mesas
   de un local (`venta.administrar`); mapa con los cinco estados;
   bloquear y liberar con motivo (`venta.bloquear_mesa`). Pantalla
   admin "Mesas" y mapa en el POS (celular, tablet y escritorio). *Se
   demuestra:* crear mesas, bloquear una y ver el mapa en tres tamaños.
2. **Tomar el pedido (US-028).** Pedido, personas, líneas y
   componentes; menú del día con opciones, carta, agotados ocultos,
   para llevar con táper, observaciones, pensionista por búsqueda y el
   motor de precios. *Se demuestra:* una mesa mixta con un cliente
   normal y un pensionista con su precio, y un para llevar con táper;
   y la prueba de dos locales sobre el mismo pensionista para el segundo
   consumo del día.
3. **Comanda y anulaciones (US-030 y US-031).** ✅ **Hecha (2026-09-30)**, ver `rikos-backend/doc/historias/US-030-US-031-comanda-y-anulaciones.md`. Comandar, agregar
   después, vista imprimible con COPIA en reimpresiones, y anular con
   permiso y motivo antes y después de comandar; eventos de bitácora
   nuevos. *Se demuestra:* imprimir, reimprimir y una anulación
   posterior a la comanda en la bitácora.
4. **Reorganizar (US-029).** ✅ **Hecha (2026-09-30)**, ver `rikos-backend/doc/historias/US-029-reorganizar-cuentas.md`. Trasladar y dividir líneas con recálculo,
   mover el pedido a una mesa libre y unir o separar mesas. *Se
   demuestra:* dividir una cuenta y pasar una línea de un cliente normal
   a un pensionista con su precio recalculado.

## 6. Fuera de alcance, y dónde queda

| Tema | Dónde |
|---|---|
| Cobrar, cerrar, cierres parciales, reabrir | Sprint 7 (US-032 a US-036) |
| Cargo del consumo a la cuenta del pensionista | Sprint 7 (RN-015) |
| Ticket y pre-ticket, kiosco | Sprint 8 (US-019, US-020) |
| Descuento real de inventario por anulación | Sprint 9 |
| Sincronización y trabajo sin internet | Sprint 10 |
| Recojo programado y delivery | Sprint 12 |
| Reporte de anulaciones | Sprint 14 |
| Prueba con la impresora real (HT-10) | Pendiente hasta tener el hardware |
| Ficha del cliente regular, productos compartidos | Sin definir por el dueño (RF-017, RF-008) |

## 7. Riesgos y cosas abiertas

- **La cantidad y disposición real de mesas** (RF-010): la carga el
  administrador; los datos de desarrollo son inventados.
- **Impresora real:** la vista imprimible del navegador puede no
  ajustarse a papel térmico de comandera. Se sabrá con HT-10.
- **Segundo consumo entre locales** depende de que el pedido guarde
  al pensionista; con el trabajo sin internet (sprint 10) hay que
  cuidar el orden de llegada.
- **Precios reales del plan y del táper**: siguen inventados; sin
  ellos no se puede vender a un pensionista de verdad.
- **El tamaño del sprint:** es el más grande hasta ahora (XL + M + L +
  L + M). Si una entrega se alarga, se mueve la siguiente al sprint
  siguiente antes que apurar la calidad.
