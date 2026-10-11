# Sprint 6b — La lista de pedidos (backend del rediseño del POS)

**Estado:** ✅ hecho (2026-09-30), ver `rikos-backend/doc/historias/sprint-06b-lista-de-pedidos.md`. Lo pidió el rediseño del POS (`sprint-06b-pos-rediseno.md`, hecho por la sesión de
diseño): las pantallas "Órdenes activas", "Historial" y "Inicio" necesitan **listar** pedidos, y hoy el backend solo sabe dar
**uno** (`GET /pedidos/{id}`) o el de una mesa. Este documento es solo la parte de backend; el diseño y las pantallas son del
otro documento.

## 1. Qué queremos lograr

Que el POS pueda mostrar, sin una consulta por pedido, (a) las órdenes en curso del local como tarjetas, (b) el historial con
filtros y paginación y (c) los conteos del menú lateral. Solo lectura: no cambia ninguna regla del sprint 6.

## 2. Decisiones, con su razón

1. **Un solo endpoint de lista con `situacion`**, en vez de dos. "En curso" y "cerrados" son el mismo recurso (pedidos) con
   otro filtro; dos endpoints duplicarían filtros y paginación. *Descartado:* `/pedidos/activos` y `/pedidos/historial`.
2. **En curso = `abierto`, `comandado` y `pendiente_cierre`; cerrados = `cerrado` y `anulado`.** Hoy el CHECK de
   `pedido.estado` solo admite los tres primeros: `cerrado` y `anulado` los trae el sprint 7 (cobrar y cerrar) con una
   migración. Hasta entonces `situacion=cerrados` devuelve una página vacía, y eso es correcto, no un error. *Descartado:*
   adelantar esos estados ahora, porque ningún caso de uso los usa y dejaría estados sin dueño.
3. **Las líneas viajan solo en la lista "en curso"** (las tarjetas las muestran) y **sin las anuladas**. El historial no las
   necesita y cargarlas multiplicaría el costo de cada página. Las líneas no traen precios: la tarjeta muestra qué se pidió;
   el total va aparte.
4. **El total se deriva al leer**, igual que en `GET /pedidos/{id}` (subtotal + táper de las líneas no anuladas): no se guarda.
   Se calcula por lote para toda la página, no pedido por pedido.
5. **Paginación simple por número de página** (`pagina` desde 0, `tamano` 1 a 100, por defecto 20), con `total` y
   `totalPaginas`. El orden es el más reciente primero (`abierto_en` descendente, con el id como desempate para que la
   paginación sea estable). *Descartado:* paginación por cursor; a escala de un local no hace falta.
6. **`q` busca en el número del pedido, el nombre del cliente y el número o nombre de la mesa** (sin distinguir mayúsculas).
   Los filtros de fecha (`desde`, `hasta`, ambos inclusivos) usan la **fecha operativa** del pedido, no la hora de apertura,
   porque una cena que cruza la medianoche es del día de servicio.
7. **Todo del local de la sesión**, como el resto del módulo: un pedido de otro local no aparece nunca. Permiso `venta.ver`.
8. **`mozoNombre` es el nombre de usuario** del mozo que abrió el pedido (`mozotest`), el mismo dato que ya muestra la comanda.
9. **`GET /pedidos/resumen` da los conteos** `{ enCurso, porCobrar }` (`porCobrar` = `pendiente_cierre`, hoy siempre 0) para
   el menú lateral y el Inicio, sin traer la lista. Es barato y evita que el front cuente páginas.
10. **`GET /pedidos/{id}` y las respuestas de escritura ganan `mozoNombre` y `abiertoEn`.** Es el mismo objeto que ya devuelven;
    el encabezado nuevo del pedido los muestra.

## 3. Contrato

`GET /pedidos?situacion=en_curso|cerrados&canal=mesa|para_llevar&estado=…&q=…&desde=AAAA-MM-DD&hasta=AAAA-MM-DD&pagina=0&tamano=20`

```json
{
  "contenido": [{
    "id": "…", "numero": "0004", "canal": "mesa", "servicio": "almuerzo", "estado": "comandado",
    "mesaId": "…", "mesaNumero": "3", "mesaNombre": "Ventana", "salonNombre": "Principal",
    "nombreCliente": "Familia García", "comensales": 3, "mozoNombre": "mozotest",
    "abiertoEn": "2026-09-30T19:28:41Z", "total": 1900,
    "lineas": [{ "cantidad": 2, "nombre": "Gaseosa personal", "componentes": [], "estado": "comandado" }]
  }],
  "pagina": 0, "tamano": 20, "total": 1, "totalPaginas": 1
}
```

- `estado`, `canal` y `situacion` inválidos dan **422** (`FILTRO_INVALIDO`), no una lista vacía engañosa. `estado` tiene que
  ser compatible con la `situacion`.
- `mesaId`, `mesaNumero`, `mesaNombre` y `salonNombre` son `null` en un pedido para llevar. `lineas` es `null` en
  `situacion=cerrados`.
- `comensales` = cantidad de personas del pedido. Todo en céntimos.

`GET /pedidos/resumen` → `{ "enCurso": 6, "porCobrar": 0 }`.

## 4. Fuera de alcance

| Tema | Dónde |
|---|---|
| Estados `cerrado` y `anulado` del pedido, anular un pedido entero | Sprint 7 |
| Filtro y estado por forma de pago ("Pagada") | Sprint 7 |
| Mostrador y Delivery en el Pedido rápido | Pendiente de especificar (sprint 12 para delivery) |
| Las pantallas y el shell del POS | `sprint-06b-pos-rediseno.md` (otra sesión) |

## 5. Cómo se prueba

Pruebas de caso de uso (`ListaDePedidos`): filtros por situación, canal, estado, texto y fechas; orden y paginación estable; total
sin anuladas; líneas solo en curso y sin anuladas; aislamiento por local; filtros inválidos. Prueba por rol del endpoint
(mozo, cajero y administradores 200; cocina y motorizado ven porque tienen `venta.ver`; sin sesión 401/403) y de la respuesta
(`mozoNombre`, `abiertoEn`). Verificación por API contra la base de desarrollo.

---

## 6. Adenda (2026-10-01): "Terminar pedido", un cierre sin cobro (provisional hasta el sprint 7)

**Qué se pidió.** Liberar mesas sin borrar pedidos: hoy un pedido solo es `abierto`, `comandado` o `pendiente_cierre` y la mesa
ocupada se deriva de eso, así que no hay forma de dejar una mesa libre salvo borrando filas de la base. El usuario pide una
función mínima "Terminar pedido" **sin cobro**.

**Decisiones, con su razón.**

1. **Un estado nuevo `cerrado` del pedido**, con `cerrado_en` y `cerrado_por` (la base exige las dos cuando está cerrado). Es el mismo
   estado que el sprint 7 necesita para cobrar y cerrar, así que no se tira nada: el sprint 7 **completa** este cierre agregando el
   cobro (turno de caja, forma de pago, cargo a la cuenta del pensionista). *Descartado:* un estado aparte "terminado": habría dos
   formas de estar cerrado y el historial tendría que distinguirlas.
2. **El nombre deja claro que no se cobra:** el caso de uso es `TerminarPedido`, el evento de bitácora es `pedido_terminado` ("terminó el
   pedido "0004" sin cobrar") y la pantalla pide confirmación ("Se cierra sin cobrar; la mesa queda libre"). No toca la cuenta de ningún
   pensionista ni genera comprobante.
3. **Qué pedidos se pueden terminar:** `abierto`, `comandado` y `pendiente_cierre`, de mesa o para llevar. Uno ya cerrado da
   `PEDIDO_NO_TERMINABLE` (409).
4. **Las mesas unidas se separan solas.** Si la mesa del pedido era principal de mesas unidas, al terminar se separan (todas quedan libres):
   rechazar obligaría a separarlas a mano una por una justo cuando el mozo quiere liberar la mesa. Queda anotado en la bitácora.
5. **Un pedido cerrado no se modifica.** Agregar o cambiar líneas y personas, asignar un pensionista y anular dan `PEDIDO_CERRADO` (409).
   Esto cierra de paso un pendiente: ninguna escritura revisaba el estado del pedido. Comandar, trasladar y mover ya lo revisaban.
   Reimprimir una comanda de un pedido cerrado **sí** se puede (el Historial lo ofrece).
6. **Aparece en el Historial.** `GET /pedidos?situacion=cerrados` devuelve los cerrados (ya los hay desde la adenda "Terminar pedido") (con su total y sin líneas, como se especificó);
   `GET /pedidos/{id}` de un cerrado sigue funcionando. La mesa queda libre en el mapa y se puede abrir otro pedido en ella.
7. **Permiso `venta.modificar`** (el mozo ya lo tiene), el mismo de reorganizar.

**Contrato.** `POST /pedidos/{id}/terminar` → 200 con el pedido completo (`estado: "cerrado"`). Errores: 404 `NO_ENCONTRADO` (otro local o
inexistente), 409 `PEDIDO_NO_TERMINABLE`.

**Fuera de alcance.** Cobrar, turno, comprobante, descuento de inventario y el cargo al pensionista (sprint 7); reabrir un pedido cerrado
(sprint 7, `reapertura`).

---

## 7. Adenda (2026-10-01): una mesa solo está "ocupada" cuando su pedido se comanda

**Cambio de regla confirmado por el usuario** (la decisión y su razón están en `sprint-06-vender-en-mesa.md` §3.2, "Cambio de
regla"). Aquí queda lo que cambia en el backend. *Por qué:* abrir una mesa y entrar a tomar el pedido sin comandar no debe
ocuparla; para el local la mesa está ocupada cuando la cocina ya recibió algo.

1. **Estado derivado:** `ocupada` = el pedido está **comandado**; `pendiente_cierre` sigue igual. Un pedido solo **abierto** deja la
   mesa **libre**. Prioridad: bloqueada, unida, pendiente de cierre, ocupada, libre.
2. **`tomandoPedido` en `GET /mesas`** (booleano, en cada mesa del mapa y de la lista de administración): `true` si la mesa está libre y
   tiene un pedido **abierto sin comandar** (la principal de mesas unidas incluida, porque el pedido es suyo). Es la marca "libre, pero
   alguien está tomando el pedido": evita que se la ofrezcan a otro cliente. Las secundarias unidas salen `unida` y `false`.
3. **Abrir una mesa que ya tiene un pedido abierto sin comandar devuelve ESE pedido** (200, idempotente; antes daba 409 `MESA_OCUPADA`).
   Un pedido nuevo sigue dando 201. Si el pedido de la mesa está comandado o pendiente de cierre, sigue siendo 409 `MESA_OCUPADA`.
   *Por qué:* dos mozos que tocan la misma mesa "libre" deben entrar al mismo pedido, no pelearse. La unicidad la sigue garantizando la
   base (un solo pedido en curso por mesa); si dos aperturas chocan a la vez, la perdedora se reintenta y encuentra el pedido que ganó.
4. **Bloquear o desactivar una mesa con un pedido abierto sin comandar se rechaza** con un error propio, `TIENE_PEDIDO_ABIERTO` (409:
   "primero hay que terminar el pedido"); con un pedido comandado sigue siendo `MESA_NO_LIBRE`.
5. **Unir:** una mesa libre con pedido abierto sin comandar **no** se une como secundaria (`TIENE_PEDIDO_ABIERTO`); como **principal** sí.
   Mover el pedido a una mesa con cualquier pedido en curso sigue rechazándose (`MESA_OCUPADA`).
6. Un pedido solo abierto sigue contando "en curso" para las Órdenes activas y los conteos; se limpia con "Terminar pedido".

**Nombre del campo:** `tomandoPedido`. **Pruebas:** estado derivado (abierto → libre + `tomandoPedido`, comandado → ocupada), apertura
idempotente y su concurrencia, bloquear/desactivar/unir con pedido abierto y con pedido comandado, y las pruebas existentes de mesas y
mapa que suponían "abierto = ocupada".

## 8. Categorías en la oferta (`grupo` y `categoria`)

`GET /pedidos/{id}/oferta` devuelve cada producto de la carta como `{ id, nombre, precio, grupo, categoria }` para las pestañas del catálogo
(Entrada / Segundo / Extra). `grupo` es `entrada`, `segundo` o `extra` según la categoría **raíz** del producto (nunca por su nombre) y `null` para
bebidas, postres y guarniciones (solo salen en "Todo"); `categoria` es el nombre de la subcategoría. La carta sale ordenada por grupo
(entrada, segundo, extra, otros) y nombre. Además la carta ahora incluye las categorías de naturaleza `componente_menu`, que antes quedaban fuera
(las entradas y segundos sueltos no se podían pedir). Pruebas: `OfertaDeLaCartaTest` (7).
