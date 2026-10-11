# Sprint 6b — La lista de pedidos (backend del rediseño del POS)

2026-09-30. Especificación en `docs/producto/plan/sprint-06b-lista-de-pedidos.md`. El rediseño del POS (otra sesión,
`docs/producto/plan/sprint-06b-pos-rediseno.md`) necesita listar pedidos para "Órdenes activas", "Historial" y "Inicio".

## Qué se pidió

Poder listar, sin una consulta por pedido: las órdenes en curso del local (tarjetas con sus líneas), el historial con filtros
y paginación, y los conteos del menú lateral. Y que el pedido traiga el nombre del mozo y la hora de apertura.

## Qué se construyó

- **`GET /pedidos`** (`venta.ver`): `situacion=en_curso|cerrados` (por defecto en curso), `canal`, `estado`, `q`, `desde`,
  `hasta` (fechas operativas, ambas incluidas), `pagina` (desde 0) y `tamano` (1 a 100, por defecto 20). Devuelve
  `{contenido, pagina, tamano, total, totalPaginas}`; cada resumen trae mesa (número, nombre y salón; null si es para
  llevar), `nombreCliente`, `comensales`, `mozoNombre`, `abiertoEn`, `total` y, **solo en curso**, las `lineas` sin las
  anuladas, cada una con su `estado` (`pendiente` o `comandado`). Más reciente primero, con el id de desempate.
- **`GET /pedidos/resumen`** → `{enCurso, porCobrar}` (por cobrar = `pendiente_cierre`, hoy 0).
- **`GET /pedidos/{id}` y las escrituras** ganan `mozoNombre` y `abiertoEn`.
- `ListaDePedidos` (caso de uso, solo lectura): una consulta paginada (`PedidoRepositorio.buscar`, sin parámetros nulos) y
  después personas, líneas, componentes, mesas, salones y mozos **por lote** para toda la página (sin una consulta por
  pedido). El total se deriva de las líneas no anuladas. Filtros inválidos dan 422 `FILTRO_INVALIDO`.

## Qué se dejó fuera y por qué

- **Historial real:** `cerrado` y `anulado` de pedido no existen todavía (el CHECK solo admite `abierto`, `comandado` y
  `pendiente_cierre`): `situacion=cerrados` devuelve una página vacía hasta el sprint 7, que los trae. Es correcto, no un error.
- Forma de pago / "Pagada" (sprint 7); Mostrador y Delivery (sin especificar / sprint 12); las pantallas (otra sesión).

## Cómo se verificó

17 pruebas nuevas en verde: `ListaDePedidosTest` (12: campos, anuladas fuera y sin sumar, para llevar sin mesa, filtros de canal,
estado y texto, fechas operativas con extremos incluidos, orden y paginación estable, cerrados vacíos, pendiente de cierre,
aislamiento por local, filtros inválidos, conteos y sin local activo) y `ListaDePedidosAccesoPorRolTest` (5: mozo, cajero,
administradores, cocina y motorizado ven; sin sesión no entra nadie; otro local no aparece; códigos de error). Por API contra la
base de desarrollo: `resumen` = 3 en curso, la lista con `tamano=2` da 2 de 3 pedidos con `totalPaginas` 2, `cerrados` 200 vacío
y `situacion=x` 422.

## Decisiones tomadas al ejecutar (rulings)

- `mozoNombre` es el nombre de usuario (el mismo dato que la comanda).
- La consulta evita parámetros nulos (Postgres no infiere su tipo): comodín `%` para el texto, extremos abiertos para las fechas
  y las listas completas de estados y canales.
- El texto se escapa (`%`, `_`, `\`) para que buscar "50%" no sea un comodín.

## Adenda (2026-10-01): "Terminar pedido", un cierre sin cobro

Especificación en `docs/producto/plan/sprint-06b-lista-de-pedidos.md` (sección 6). Lo pidió el usuario por medio de la sesión de diseño:
poder liberar las mesas sin borrar pedidos de la base.

- **Migración V38:** `pedido.estado` admite `cerrado`, con `cerrado_en` y `cerrado_por` (la base exige las dos cuando está cerrado) y el
  evento de bitácora `pedido_terminado`. Es el mismo estado que el sprint 7 necesita: lo completará con el cobro, no lo reemplaza.
- **`TerminarPedido`** + **`POST /pedidos/{id}/terminar`** (`venta.modificar`): solo desde `abierto`, `comandado` o `pendiente_cierre`; la mesa
  queda libre (se deriva), **las mesas unidas a ella se separan solas**, y no toca la cuenta de ningún pensionista. Errores: 404
  `NO_ENCONTRADO`, 409 `PEDIDO_NO_TERMINABLE`.
- **Un pedido cerrado no se modifica:** agregar o cambiar líneas y personas, asignar un pensionista y anular dan 409 `PEDIDO_CERRADO`. De
  paso queda resuelto el pendiente de que ninguna escritura revisaba el estado del pedido. Reimprimir su comanda sí se puede.
- **Historial:** `GET /pedidos?situacion=cerrados` ya devuelve estos pedidos (con su total y sin líneas).
- **Pantalla:** botón "Terminar pedido" (con confirmación: "Se cierra sin cobrar y la mesa queda libre…") y aviso al terminar; un pedido
  cerrado abierto desde el Historial se ve en solo lectura.
- **Bitácora:** "admin terminó el pedido "0004" sin cobrar (mesa 3 libre)".

**Pruebas nuevas:** `TerminarPedidoTest` (8), dos en `ReorganizarAccesoPorRolTest` (cuatro roles terminan; cocina y motorizado 403; otro local
404; segunda vez 409; el historial los lista) y una en `ListarAuditoriaTest`.

## Adenda (2026-10-01): una mesa solo está "ocupada" cuando su pedido se comanda

Especificación en `docs/producto/plan/sprint-06b-lista-de-pedidos.md` (sección 7); la decisión y su razón, en
`sprint-06-vender-en-mesa.md` §3.2. Lo confirmó el usuario: al probar, unir mesas abría el pedido de la principal y la dejaba
"ocupada" sin haber pedido nada.

- **Estado derivado:** `ocupada` = pedido **comandado**; `pendiente_cierre` igual; un pedido solo **abierto** deja la mesa **libre**
  (`CalculadorDeEstadoDeMesa` y `ConsultaMesas`). Prioridad: bloqueada, unida, pendiente de cierre, ocupada, libre.
- **`tomandoPedido`** (booleano) en cada mesa de `GET /mesas`: libre con un pedido abierto sin comandar (la principal de mesas unidas
  incluida; las secundarias salen `unida` y `false`). Es lo que el mapa dibuja como "libre + tomando pedido".
- **`POST /pedidos` sobre una mesa con pedido abierto sin comandar devuelve ESE pedido** (200, antes 409 `MESA_OCUPADA`); un pedido
  nuevo sigue siendo 201; con un pedido comandado o pendiente de cierre sigue siendo 409 `MESA_OCUPADA`. **Concurrencia:** la base sigue
  garantizando un solo pedido en curso por mesa; si dos aperturas chocan (`ux_pedido_mesa_en_curso`), la perdedora se **reintenta** (hasta
  3 veces) y encuentra el pedido que ganó en vez de recibir un 409.
- **Bloquear o desactivar** una mesa con pedido abierto sin comandar da el error nuevo **`TIENE_PEDIDO_ABIERTO`** (409: primero hay que
  terminar el pedido); con pedido comandado sigue siendo `MESA_NO_LIBRE`. **Unir:** una mesa con pedido abierto sin comandar no se une
  como secundaria (`TIENE_PEDIDO_ABIERTO`); como principal sí.
- **Mover el pedido** a una mesa con cualquier pedido en curso sigue rechazándose (`MESA_OCUPADA`). Mis diálogos de mover y unir ya no
  ofrecen una mesa con `tomandoPedido`.

**Pruebas:** nuevas (estado derivado, `tomandoPedido`, apertura idempotente y su reintento concurrente, bloquear y desactivar con pedido
abierto o comandado, unir con principal abierta, y el JSON del mapa) y 9 pruebas existentes que suponían "abierto = ocupada" ajustadas
(ahora comandan primero).
