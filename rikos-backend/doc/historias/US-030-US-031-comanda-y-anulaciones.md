# US-030 y US-031 — Comanda y anulaciones

Sprint 6, entrega 3 de 4 (2026-09-30). Plan en
`docs/superpowers/plans/2026-09-30-sprint6-entrega3-comanda-y-anulaciones.md`; especificación en
`docs/producto/plan/sprint-06-vender-en-mesa.md` 3.6 y 3.7.

## Qué se pidió

Mandar el pedido a cocina (comandar) con una vista imprimible que muestre COPIA desde la segunda impresión, y anular
una línea con permiso y motivo, antes o después de comandar, dejando todo en la bitácora.

## Qué se construyó

**Base de datos — `V33__comandas_y_anulaciones.sql`.** `comanda` (número correlativo por pedido, contador de
impresiones, `estado_impresion` pendiente/impresa/fallida) y `detalle_comanda` (la foto: una fila por línea, y una línea
sale en **una sola** comanda). `detalle_pedido` gana `anulado_en`, `anulado_por`, `motivo_anulacion_id`,
`observacion_anulacion` y `anulado_post_comanda`; la base exige motivo, quién y cuándo a toda línea anulada, y que
"post comanda" solo exista si está anulada. Eventos de bitácora `comanda_emitida` y `reimpresion_comanda`.

**Backend (módulo `venta`).**

- `ComandarPedido`: toma las líneas **pendientes de todas las personas**, crea la comanda siguiente, guarda la foto,
  pasa las líneas y el pedido a `comandado`. Idempotente por id de comanda (un id ajeno da `NO_ENCONTRADO`, no se pisa).
  Sin nada pendiente → `SIN_LINEAS_PENDIENTES`. Agregar platos después y comandar crea otra comanda solo con lo nuevo.
- `RegistrarImpresion`: suma la impresión con un **UPDATE atómico** (`impresiones + 1 … RETURNING`); es **copia** si ya
  había al menos una antes. Solo las copias van a la bitácora.
- `ConsultaComandas`: lista por pedido y detalle imprimible (mesa o para llevar, pedido, mozo, servicio, personas con sus
  productos, componentes y notas), leído de la foto.
- `AnularLinea`: motivo activo del tipo `anulacion` (con nota obligatoria si el motivo la exige); la línea queda
  `anulado`, con `anulado_post_comanda` = estaba comandada. **Lo anulado no suma** al total pero sigue visible con su
  motivo. Una pendiente también se puede anular (queda "antes de comandar"); la pantalla solo lo ofrece en las comandadas,
  porque una pendiente se **quita** sin permiso (2A).
- API: `POST/GET /pedidos/{id}/comandas`, `GET /comandas/{id}`, `POST /comandas/{id}/impresiones`,
  `POST /pedidos/{id}/lineas/{lineaId}/anular`. Ver comandas, `venta.ver`; comandar e imprimir, `venta.crear`; anular,
  **`venta.anular`** (cajero y administradores; el mozo no). Los choques de concurrencia se reintentan hasta 3 veces y dan
  409 (`SIN_LINEAS_PENDIENTES`, `NUMERO_DE_COMANDA_REPETIDO`).
- Bitácora en español: "comandó el pedido "0001": 2 productos (comanda 1)", "reimprimió la comanda 1 … (copia 2)",
  "anuló "Chicha" del pedido "0001" (después de comandar): Error del mozo".

**Frontend (`features/venta`).** Botón **Comandar** (habilitado solo con líneas pendientes) que abre `VistaComanda`;
lista "Comandas" del pedido para reabrir o reimprimir; líneas comandadas con la insignia "En cocina" y, con permiso,
**Anular** (`DialogoAnular`: motivo de la lista, nota si se exige); líneas anuladas tachadas con "Anulado después/antes de
comandar · motivo". `VistaComanda` registra la impresión **antes** de `window.print()` y muestra **COPIA** según lo que
diga el servidor; el CSS `@media print` deja solo la hoja.

## Qué se dejó fuera y por qué

- **HT-10** (prueba con la impresora real): sigue pendiente hasta tener el hardware.
- Descuento real de inventario por lo anulado después de comandar (sprint 9); reporte de anulaciones (sprint 14).
- Comandar por persona (se comanda el pedido entero, decisión 1 del plan).
- `fallida` en `estado_impresion` (necesita un agente de impresión).

## Cómo se verificó

**Backend:** 35 pruebas nuevas, todas en verde: `TablasDeComandaTest` (6), `ComandarPedidoTest` (8),
`RegistrarImpresionTest` (3), `ConsultaDeComandasTest` (4), `AnularLineaTest` (9),
`ComandasYAnulacionesAccesoPorRolTest` (5: una por rol y verbo, 404 entre locales, códigos de error) y una en
`ListarAuditoriaTest` (las tres frases de la bitácora). Suite completa: 501 pruebas; los únicos fallos son ajenos a esta
entrega (7 en `MesasAccesoPorRolTest` y `MesasConcurrenciaTest`, que dan 500 al crear una mesa por el trabajo paralelo
de salones/V34, y el ciclo conocido `comun ↔ identidad` de `ArquitecturaDeModulosTest`).

**Navegador (POS, con el backend ya con V33):**

- Como `mozotest` (mozo): mesa 2 → dos productos de carta → **Comandar** → la comanda 1 sale **sin COPIA**; **Imprimir**
  (con `window.print` interceptado) → primera impresión sin COPIA, segunda **con COPIA**; reabrir la comanda 1 ya es
  COPIA. Agregar un producto más y comandar → **comanda 2 solo con lo nuevo**. Las líneas comandadas muestran "En
  cocina", sin "Quitar", sin cantidad editable y **sin "Anular"** (el mozo no tiene el permiso); con todo comandado el
  botón Comandar queda desactivado.
- Como `mtorres` (cajero): "Anular" en las líneas comandadas. Sin motivo → "Elige el motivo de la anulación."; con el
  motivo "Otro" y sin nota → "Este motivo pide que escribas una nota."; anulada, la línea queda tachada con "Anulado
  después de comandar · motivo" y el total baja (S/ 11.50 → S/ 8.00 → S/ 3.00).
- Bitácora: "mozotest comandó el pedido "0002": 2 productos (comanda 1)", "reimprimió la comanda 1 … (copia 2)",
  "mtorres anuló "Gaseosa personal" del pedido "0002" (después de comandar): Error del mozo".
- Móvil (375): sin desborde, botones de 44 px y el diálogo de la comanda cabe en pantalla.
- **No verificado:** que la hoja impresa salga solo con la comanda (el CSS `@media print` está escrito pero no se pudo
  ver la vista previa de impresión desde el navegador de prueba), y la impresora real (HT-10).

## Datos de desarrollo (inventados)

Se crearon dos motivos de anulación: "Error del mozo" y "Otro (escribir nota)" (con nota obligatoria). Los PIN de
`mozotest` y `mtorres` quedaron con un valor aleatorio que nadie conoce: reasignarlos desde el panel para volver a usar
el POS con ellos. El pedido de prueba 0002 se borró; el 0001 (de `jhon`) no se tocó.

## Decisiones tomadas al ejecutar (rulings)

- Los códigos de error de comanda y anulación se agregaron a `ErrorPedido` en vez de un enum aparte: un solo
  controlador y un solo `errorDe`.
- `sumarImpresion` es una consulta nativa `UPDATE … RETURNING`; el caso de uso refresca la entidad ya cargada.
- Una segunda sesión trabajaba en paralelo en el mismo árbol (salones, V34): mis pruebas no se pudieron compilar
  mientras su código estaba a medias; se esperó a que quedara estable, sin tocar sus archivos.
