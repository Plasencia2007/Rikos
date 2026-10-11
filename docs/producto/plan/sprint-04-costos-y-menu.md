# Sprint 4 — Costos y menú

**Objetivo (del backlog):** una receta con subrecetas mostrando su costo
por porción, y la semana de menú programada.

**Historias:** US-010 (recetas y subrecetas), US-014 (menú normal y
extra), US-013 (programación semanal), US-015 (agotados).
Especificación en `flujos/02-ep02-catalogos-costeo.md` (US-010) y
`flujos/03-ep03-menu-programacion.md`. Modelo en
`modelo-de-datos/03-recetas-costeo--ep02.md` y
`modelo-de-datos/04-menu-programacion-semanal--ep03.md`.

## Auditoría antes de programar (2026-09-29)

### 1. La numeración de reglas del modelo estaba corrida -- corregido

El modelo citaba reglas con números del backlog v1.0. Contra la lista
consolidada del v2.0 (`flujos/90-reglas-de-negocio-consolidadas.md`):

| El modelo decía | Es en realidad |
|---|---|
| RN-044 (subreceta como ingrediente) | RN-043 |
| RN-045 (ver costos requiere permiso) | RN-012 |
| RN-046 (la receta nueva aplica a ventas nuevas) | RN-045 |
| RN-039 (precio único del menú normal) | RN-037 |
| RN-041 (menú extra sin tarifa reducida) | RN-023 |
| RN-042 (el mozo registra la opción elegida) | RN-038 |
| RN-043 (advertencia por repetición) | RN-041 |
| RN-055 (agotado oculta en pedidos nuevos) | RN-066 |

Corregido en los bloques 3 y 4. **Los bloques 5 en adelante casi seguro
tienen el mismo corrimiento** -- se corrigen cuando llegue su sprint (ver
`docs/pendientes/README.md`). Es el mismo origen que el error de EP-09 en
el bloque 9.

### 2. "Por día y servicio" -- decidido: solo almuerzo

US-013 y RN-039 hablan de programar "por día y servicio", pero RN-042 y
CA7 de la misma historia dicen que desayuno y cena **no** se programan.
El modelo ya lo resolvió bien: `opcion_menu` no tiene columna `servicio`,
porque lo único que se programa es el almuerzo. Desayuno y cena salen de
`producto_servicio`, que ya existe desde el sprint 2.

**Decisión:** se sigue el modelo. Una columna `servicio` que siempre vale
"almuerzo" sería una invitación a que alguien programe una cena.

### 3. La disponibilidad no va en `opcion_menu` -- decidido

US-015 lista "OpcionMenu (disponibilidad por local)" como dato, pero
`opcion_menu` es central y la disponibilidad es por local. El modelo la
separa en `disponibilidad_producto` (local + producto).

**Decisión:** se sigue el modelo. Además hace falta así porque también se
agotan productos de desayuno y cena, que nunca pasan por `opcion_menu`.

### 4. La reactivación de un agotado pide un motivo que no existe

RN-066 exige motivo al reactivar, y `disponibilidad_producto.motivo_id`
apunta a `motivo`. Pero los seis tipos de motivo de US-012 (ajuste de
inventario, anulación, descuento, rechazo de abono, sobrante, reapertura)
no incluyen ninguno para disponibilidad.

**Propuesta:** agregar un séptimo tipo, `disponibilidad`, con una
migración nueva (no se edita V13). Así el motivo sigue siendo elegible de
una lista, homogéneo y reportable, igual que los demás. Se descarta texto
libre porque rompe justo lo que US-012 existe para asegurar.

### 5. El menú normal y el menú extra no tienen dónde vivir

El modelo dice que el precio del menú normal vive en
`producto.precio_venta` "del menú normal" -- o sea, el menú es un
producto. Pero las cinco naturalezas de categoría del catálogo
(`componente_menu`, `producto_fijo`, `extra`, `insumo`, `subreceta`) no
tienen una para "menú". Y `extra` ahí son los agregados sueltos (un
huevo frito), no el menú extra.

**Decisión:** una sexta naturaleza, `menu`, con una categoría "Menú del
día" que contiene dos productos vendibles: "Menú normal" y "Menú extra".
Lo que se elige (qué entrada, qué segundo) se guarda en la línea del
pedido en el sprint 6, apuntando a la `opcion_menu` del día.

## Decisiones tomadas con el usuario (2026-09-29)

### El menú extra tiene un solo precio; lo que cambia son los agregados

El dueño: la mayoría de los menús extra cuestan lo mismo, y lo que cambia
el precio es agregarle algo más al plato.

**Decisión:** "Menú extra" es **un solo producto con un solo precio**.
`opcion_menu.tipo_menu = 'extra'` marca qué segundos del día pertenecen
al extra. Las variaciones de precio se cobran como **agregados** -- la
naturaleza `extra` que ya existe desde el sprint 2 (huevo frito, porción
adicional) -- vendidos como línea aparte en el pedido (sprint 6).

**Descartado:** un precio por cada segundo especial. Agrega una columna
de precio en la programación semanal para cubrir excepciones que el
mecanismo de agregados ya cubre. Si aparece un segundo realmente más
caro, se vende como menú extra más un agregado.

### La semana entra en servicio sola, calculada por fecha

El dueño: que pase sola, pero bien programada.

**Decisión:** el estado guardado es solo `borrador` o `confirmado`. "En
servicio" y "pasada" **no se guardan, se calculan**: una semana
confirmada cuyo rango incluye hoy está en servicio; una cuyo fin ya pasó
es histórica. Corrige el enum de `semana_menu.estado` del modelo (§6.1),
que tenía `en_servicio` como valor guardado.

**Por qué calculado y no un proceso programado:** un proceso que cambia
el estado a las 00:00 del lunes puede no correr -- el nodo del local se
reinicia con cada corte de luz (DT-039). Un estado calculado por fecha
nunca se atrasa ni se pierde, y no hay nada que "programar bien" porque
no hay nada que pueda fallar.

**Consecuencia sobre la edición (RN-040):** se pueden editar los días de
hoy en adelante de una semana confirmada; los días que ya pasaron quedan
cerrados, porque ya son historia de lo que se sirvió.

## Decisiones de la entrega 1, recetas (2026-09-28)

### El costo de una subreceta se reparte por su unidad base, no por porción

La fórmula del modelo (§5.3) multiplicaba la cantidad de una subreceta
usada como ingrediente por su `costo_por_porcion`. Eso solo es correcto
si la cantidad está en porciones. En la cocina no se piensa así: el
seco lleva "300 g de aderezo", no "0,4 porciones de aderezo".

**Decisión:** el rendimiento de una receta se expresa **siempre en la
unidad base de su producto** (el aderezo rinde 2 kg). El costo unitario
de una subreceta es `costo_total / rendimiento`, por unidad base, igual
que el `costo_ultima_compra` de un insumo. Así un ingrediente se costea
de una sola forma, sea insumo o subreceta:

```
costo del ingrediente = cantidad convertida a la unidad base × costo por unidad base
```

`costo_por_porcion = costo_total / porciones` se sigue guardando: es lo
que se mira para el margen del plato vendible.

**Consecuencia:** se quita `unidad_rendimiento_id` del modelo. Una unidad
de rendimiento distinta de la base obligaría a convertir dos veces, y
abre la puerta a que el rendimiento quede en una unidad sin conversión.

### Un plato no pide rendimiento, solo porciones (con el usuario)

Al probar, un plato cuyo producto tenía unidad base "litro" mostraba
"rendimiento en litros". El usuario: "cuando son platos, cómo es eso que
se mide en litros".

**Decisión:** el rendimiento solo se pide en las **subrecetas**, donde
sirve para costear la parte que otra receta usa. En un plato vendible no
cambia nada (costo por porción = total / porciones), así que la pantalla
solo pide porciones y manda `rendimiento = porciones`. Además, al crear
un producto que solo se vende, la unidad base sugerida pasa a `und`.

### La conversión usa `conversion_unidad`, no la presentación de compra

Si la unidad del ingrediente no es la base de su producto, se busca una
conversión de ese producto en cualquiera de los dos sentidos (`1 origen
= factor destino`). Si no existe, **no se guarda la receta** y se dice
cuál ingrediente falla: un costo calculado con una unidad mal convertida
es peor que no tener costo. Las presentaciones de compra (saco de 50 kg)
no entran: nadie escribe una receta en sacos.

### Un insumo sin costo no bloquea la receta, pero la deja "incompleta"

Antes de la primera compra, un insumo puede no tener costo todavía
(DT-048 permite cargarlo a mano, no obliga). Si algún ingrediente del
árbol no tiene costo, la receta se guarda con costo nulo y la pantalla
dice qué ingredientes faltan. Poner cero sería mentir un margen.

### Sin nombre propio: la receta se llama como su producto

Una receta activa por producto; un nombre aparte solo puede
desincronizarse del producto. Se quita `receta.nombre` del modelo.

### Sin versiones históricas todavía (RN-045)

"La receta nueva aplica a ventas nuevas" se cumple cuando la venta copie
lo que consumió (sprint 6). Hasta entonces no hay ventas que proteger:
la receta se edita en su lugar y sube `version`. La bitácora guarda el
antes y el después de cada cambio (CA6).

### Desactivar recetas queda fuera de esta entrega

El permiso `recetas.desactivar` existe, pero ningún criterio de US-010 lo
pide, y desactivar una subreceta deja sin costo a todas las recetas que
la usan -- merece su propio diseño. Anotado en `docs/pendientes/`.

### Queda para más adelante

- **¿El pensionista paga los agregados a precio completo?** RN-023 lo
  dice del menú extra, pero no de los agregados. Hay que resolverlo antes
  del sprint 5 (planes tarifarios). Anotado en `docs/pendientes/`.

## Decisiones de la entrega 2, menú normal y extra (2026-09-29)

### Los dos menús son productos del sistema, marcados con `producto.tipo_menu`

"Menú normal" y "Menú extra" son productos vendibles de la categoría
"Menú del día" (naturaleza `menu`, auditoría §5). Para encontrarlos sin
depender de un nombre o de un id escrito en el código, `producto` gana
`tipo_menu` ('normal' | 'extra', nulo en el resto), con un índice único:
hay exactamente uno de cada uno. Los siembra la migración.

**Nacen sin precio.** RF-002: el dueño no dio los precios definitivos, y
los importes de la especificación son ejemplos. Sembrar S/ 12 inventado
sería peor que un "sin precio" visible en la pantalla.

**Están protegidos:** no se borran, no se desactivan y no dejan de ser
vendibles. Sin ellos no se vende el almuerzo. Tampoco se crean otras
categorías `menu` desde la pantalla de categorías.

### El precio se cambia desde la pantalla del menú, con permiso de menú

`PUT /menu/{tipo}/precio` exige `menu.modificar` y audita `cambio_precio`
igual que el catálogo. La ficha de producto normal **no** puede cambiar
el precio ni los usos de un producto de menú: cocina tiene
`catalogo.modificar`, y no debe poder mover el precio del almuerzo.

### La composición es una regla fija, no una configuración

CA1: el menú es entrada + segundo con guarnición + refresco + postre. La
especificación no pide cambiarla, así que vive en el código
(`TipoMenu.componentes()`), no en una tabla que alguien podría dejar
incompleta. El menú extra tiene la misma composición: lo que cambia es
que su segundo sale de las opciones marcadas como extra en la
programación semanal (entrega 3).

### RN-023 queda escrita donde el sprint 5 la va a buscar

`TipoMenu.admiteTarifaReducida()`: verdadero para el normal, falso para
el extra. Los planes tarifarios la consultan en el sprint 5; aquí se
prueba la regla y se muestra en pantalla.

## Decisiones de la entrega 3, programación semanal (2026-09-29)

### Qué se guarda

- La semana empieza **siempre un lunes** y es única por fecha. Los días
  se crean al programarles la primera opción: un día sin opciones es un
  día sin almuerzo (el local cerrado el domingo, por ejemplo).
- Cada opción es un producto de una categoría `componente_menu`. Su
  `componente` (entrada, segundo, guarnición, refresco, postre) **se
  deriva de la categoría y se guarda** (modelo §6.3): el usuario no lo
  elige.
- `tipo_menu = 'extra'` solo en segundos: marca los segundos del menú
  extra. Un mismo producto no se repite en el mismo día.
- Porciones estimadas opcionales, enteras y mayores que cero (CA2).

### Qué se puede editar y cuándo

- Borrador y confirmado se editan igual (CA5): agregar, cambiar porciones,
  quitar. Nunca se toca el catálogo maestro: solo `opcion_menu`.
- **Los días anteriores a hoy están cerrados** en cualquier estado: ya
  son historia de lo que se sirvió (decisión con el usuario).
- No se crea una semana que ya terminó. Solo se borra una semana en
  **borrador**; una confirmada ya pudo usarla el POS.
- Confirmar queda en la bitácora (`cambio_estado` sobre `semana_menu`).

### La advertencia por repetición (CA3, RN-041)

La fuente la pide "al programar" (CA3) y "al confirmar" (RN-041). Se
muestran las dos veces, **nunca bloquea**: la semana trae la lista de
advertencias y la confirmación la devuelve junto con el éxito.

"Los tres días anteriores" se toma como las **tres fechas de
calendario** previas, cruzando semanas: el segundo del lunes se compara
con el del viernes, sábado y domingo anteriores. Cuenta la repetición en
el normal o en el extra por igual, porque para el cliente es el mismo
plato.

### Qué ve el POS (CA4, CA6)

`GET /menu/dias/{fecha}` devuelve las opciones solo si su semana está
**confirmada**; un borrador no existe para el POS. Exige sesión: no hay
ninguna ruta pública del menú.

### "Hoy" en la zona del local

El sistema gana un `Clock` con zona `America/Lima`. Sin él, un servidor
en UTC cambia de día a las 7 p. m. hora de Lima y cerraría la edición del
día en pleno servicio de la cena.

## Decisiones de la entrega 4, agotados por local (2026-09-29)

### El local es el del token, no uno que se elige

Cocina y el administrador de local marcan en **su local activo** (el del
token, DT-047). Nadie marca un agotado en un local donde no está: CA4 pide
que en otro local siga disponible, y la forma más segura de cumplirlo es
que el local ni siquiera se pueda pasar como dato.

### Marcar es un toque; reactivar pide motivo

Marcar agotado ocurre en pleno servicio: un toque, sin motivo. La
fuente pide el motivo **en la reactivación** (RN-066, CA3), y ahí se
exige, de la lista de motivos con el tipo nuevo `disponibilidad` (auditoría
§4 de este sprint). Si el motivo pide observación, se exige también.

### Un motivo sembrado, a pesar de RF-005

Los motivos nacen vacíos a propósito (sprint 3, RF-005: los define el
dueño). Pero sin ningún motivo de `disponibilidad`, cocina **no podría
reactivar nada**: el agotado quedaría pegado hasta que el administrador
central cargue la lista. Se siembra uno solo, "Se volvió a preparar", que
el dueño puede editar o desactivar.

### Dos eventos en la bitácora

`marcado_agotado` (ya existía en la lista) al marcar, y uno nuevo,
`reactivacion`, al reactivar, con el motivo y la observación. Un solo
evento con "agotado: falso" se leería "marcó agotado" en la bitácora, que
es lo contrario de lo que pasó.

### Un permiso nuevo: `menu.marcar_agotado`

Ninguno de los existentes encaja: `menu.desactivar` es "quitar un ítem
del menú programado" y es central. Lo tienen el administrador central,
el de local y cocina.

### Qué se puede marcar

Cualquier producto **vendible** activo: las opciones del menú, los
platos de carta, los extras. Los insumos no: su falta es un problema de
inventario (sprint 9), no de lo que el mozo ofrece.

### CA1 y CA2 sin pedidos todavía

"Se oculta en pedidos nuevos" es una regla de consulta: el menú del día
(`GET /menu/dias/{fecha}`) ahora marca `agotado` según el local del token,
y el POS del sprint 6 lo oculta. CA2 se cumple por diseño: la
disponibilidad no toca nada guardado en una comanda, y cuando existan las
comandas se agrega la prueba que lo demuestre.

## Contraste con la fuente del dueño

Revisado contra `docs/fuente/especificacion-v0.1/` (`04` § Almuerzo,
§ Programación semanal, § Recetas y costos; `03` § Disponibilidad; `06`
§ Menú y producción). Las decisiones de arriba son compatibles:

- `04` dice "menú extra: tiene precio propio" -- un solo precio lo cumple.
- `04` mantiene los tres estados (borrador, confirmado, en servicio) --
  siguen existiendo; "en servicio" se calcula en vez de guardarse.
- `06` dice que `OpcionMenu` guarda "servicio" y "disponibilidad por
  local". Es el origen de las dos contradicciones de arriba. El mismo
  `06` aclara que "no obliga a usar tablas ni nombres concretos", así que
  separarlas es refinar el modelo conceptual, no contradecir al dueño.
- `03` § Disponibilidad confirma que la reactivación pide motivo.

## Lo que no se puede demostrar en este sprint (alcance honesto)

- **US-010 CA4 (recálculo por compra nueva):** las compras son del sprint
  9. Hoy el costo del insumo es manual (DT-048). Sí se puede demostrar el
  recálculo en cascada al **editar** ese costo manual.
- **US-015 CA1 (oculto en pedidos nuevos):** los pedidos son del sprint
  6. Este sprint deja la disponibilidad por local y la consulta que el
  POS va a usar; ocultarlo en la toma de pedido se prueba cuando exista.

## Entregas verificables

1. **Recetas con subrecetas y costo por porción (US-010).** ✅ **Hecha
   (2026-09-28)**, ver `rikos-backend/doc/historias/US-010-recetas-y-costeo.md`.
   `receta` e `ingrediente_receta`, cálculo con conversión a unidad base,
   rechazo de ciclos al guardar, recálculo en cascada, costos ocultos sin
   `ver_costos`, auditoría `cambio_receta`. *Demostrado:* seco de res con
   300 g de aderezo base → S/ 8.60 por porción (43 % de margen); la
   cebolla a S/ 5.00 lo lleva a S/ 8.90 sin tocar las recetas.
2. **Menú normal y extra (US-014).** ✅ **Hecha (2026-09-29)**, ver
   `rikos-backend/doc/historias/US-014-menu-normal-y-extra.md`. Naturaleza
   `menu`, `producto.tipo_menu` y sus dos productos protegidos.
   *Demostrado:* precio del menú normal puesto desde su pantalla y
   registrado en la bitácora; RN-023 escrita en `TipoMenu` y probada.
3. **Programación semanal (US-013).** ✅ **Hecha (2026-09-29)**, ver
   `rikos-backend/doc/historias/US-013-programacion-semanal.md`. Semana, días y opciones con
   porciones estimadas, borrador → confirmado, advertencia por repetición
   sin bloqueo. *Se demuestra:* una semana armada y confirmada, con la
   advertencia saltando al repetir un segundo.
4. **Agotados por local (US-015).** ✅ **Hecha (2026-09-29)**, ver
   `rikos-backend/doc/historias/US-015-agotados-por-local.md`.
   `disponibilidad_producto`, tipo de motivo `disponibilidad`, permiso
   `menu.marcar_agotado`, auditoría `marcado_agotado` y `reactivacion`.
   *Demostrado:* marcar y reactivar con motivo, el menú del día con
   `agotado` según el local, y las dos filas en la bitácora.

## Cierre del sprint (2026-09-29)

Las cuatro entregas hechas y verificadas. 214 pruebas en verde. Quedan
fuera, con su sprint: la receta que se copia en la venta (sprint 6), el
recálculo por compra (sprint 9), ocultar los agotados en la toma de
pedido y la prueba de comandas intactas (sprint 6), y los precios reales
de los menús (del dueño, RF-002).