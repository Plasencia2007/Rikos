# US-017 — Planes tarifarios y parámetros de cobro

Sprint 5, entrega 1 de 4 (2026-09-29). Especificación en
`docs/producto/plan/sprint-05-cuenta-del-pensionista.md` (secciones 3.4 y
3.6). Las entregas siguientes (motor de precios, ficha, cuenta) se
documentan en sus propios archivos.

## Qué se pidió

Configurar planes tarifarios con vigencia e historial, con el precio de
cada comida, sin alterar lo ya consumido; el mismo plan en todos los
locales. Y, por lo conversado con el usuario, un lugar editable para el
**precio del táper** y la **regla del segundo consumo del día**.

## Qué se construyó

**Base de datos — `V26__planes_tarifarios_y_parametros.sql`.**

- `plan_tarifario` (la identidad) y `plan_tarifario_version` (los precios
  con `vigente_desde` / `vigente_hasta`). Índice único parcial: **una sola
  versión abierta por plan**. Precios en céntimos, por comida (desayuno,
  almuerzo, cena), nulos = "sin precio".
- `parametro_global` (id, clave, valor, quién y cuándo), sembrada con
  `taper_precio` **sin monto** y `segundo_consumo_regla = precio_del_plan`.
- Acción de permiso nueva `administrar` (`pensionistas.administrar`) y
  evento de bitácora nuevo `cambio_parametro`.

**Backend (módulo nuevo `pensionistas`).**

- `CrearPlanTarifario`, `CambiarPreciosPlan`, `ConsultaPlanes`
  (`precio(plan, comida, fecha)`: vacío si no hay precio, nunca cero).
- `CambiarParametro` y `ParametrosGlobales` (lectura tipada: `taperPrecio()`
  vacío = no se cobra táper; `reglaSegundoConsumo()`).
- `PlanTarifarioController` (`GET|POST /planes`, `PUT /planes/{id}/precios`) y
  `ParametroGlobalController` (`GET /parametros`, `PUT /parametros/{clave}`).
  Ver es `pensionistas.ver`; cambiar es `pensionistas.administrar`, **solo
  del administrador central** (el de local tiene `pensionistas.modificar`
  para las fichas, y no debe tocar precios).
- Bitácora: "admin creó / cambió los precios del plan "X"", "admin cambió el
  precio del táper de sin monto a S/ 1.00", "… la regla del segundo consumo
  del día de precio del plan a precio normal".

**Frontend.** Pensionistas → **Planes tarifarios** (tarjeta por plan con sus
tres precios, aviso de un cambio programado, historial de versiones,
"Cambiar precios" con fecha de inicio opcional) y Configuración →
**Parámetros de cobro** (precio del táper con aviso "no se cobra táper" si
no hay monto; regla del segundo consumo). Solo lectura sin el permiso. La
bitácora gana los filtros de plan, parámetro y agotados.

## Decisiones de esta entrega

- **Plan + versiones, no una fila con `version`.** Distinto del modelo
  original: así el nombre no se repite en cada versión y el pensionista
  apunta al plan, no a una versión. El bloque 5 del modelo se actualiza.
- **Cambiar un precio nunca reescribe una versión que ya rigió.** Cierra la
  anterior (el día antes) y abre otra. La fecha de inicio no puede ser
  pasada (un cambio retroactivo reescribiría lo ya cobrado, CA3).
- **Excepción: corregir en el día.** Si el cambio empieza el mismo día que
  la última versión (que empezó hoy o todavía no empieza), esa versión se
  corrige en su lugar, para arreglar un error de tipeo sin esperar a
  mañana. Los consumos cobrados guardan su propio precio (RN-022), y el
  cambio queda en la bitácora con el antes y el después.
- **El cierre de la versión anterior se envía a la base antes de insertar
  la nueva** (`saveAndFlush`): Hibernate ejecuta los INSERT antes que los
  UPDATE y el índice único parcial lo rechazaría.
- **Los parámetros viven en `pensionistas`,** como quedó en la
  especificación, aunque existe un módulo `configuracion` vacío del
  andamiaje. Si aparecen parámetros de otros dominios, conviene moverlos.

## Qué se dejó fuera a propósito

- **Renombrar o desactivar un plan:** ningún criterio lo pide; con un solo
  plan no hace falta.
- **El motor de precios** (entrega 2): esta entrega deja listas sus
  lecturas (`precio`, `taperPrecio`, `reglaSegundoConsumo`).
- **Precios reales:** faltan del dueño (RF-002). Los de desarrollo son
  **inventados** (ver abajo).

## Cómo se verificó

**Pruebas automatizadas: 239 en verde, sin contar `ArquitecturaDeModulosTest`**
(suma de los reportes de surefire = conteo de `@Test` del código fuente; 214
al cerrar el sprint 4).

- `PlanTarifarioTest` (10), con "hoy" fijo: crear con tres precios; una
  comida sin precio devuelve vacío; nombre vacío o repetido y precios ≤ 0
  rechazados; no empieza en el pasado; un precio nuevo cierra la versión
  anterior sin reescribirla (hoy rige el viejo, mañana el nuevo); corrección
  en el día; cambio futuro y su corrección; vigencias inválidas y "sin
  cambios"; bitácora con antes y después; consulta.
- `ParametrosGlobalesTest` (5): valores por defecto, táper cambiado y vuelto
  a "sin monto", regla del segundo consumo, valores inválidos, bitácora sin
  duplicar al repetir el valor.
- `PlanesAccesoPorRolTest` (3), con los roles reales: el administrador
  central crea, cambia y ajusta parámetros; administrador de local, cajero y
  mozo ven pero reciben 403 al escribir; cocina y motorizado 403 hasta para
  ver.
- `ListarAuditoriaTest` (+1): las frases nuevas de la bitácora.

**En vivo** (usuario `admin`): la pantalla de planes mostró el estado vacío;
se creó "Plan pensionista" con precios **inventados** (desayuno S/ 5.00,
almuerzo S/ 8.00, cena S/ 8.00); se programó el almuerzo a S/ 9.00 desde
mañana y la tarjeta mostró "Desde el 30 set. 2026 rigen otros precios",
con el historial (hoy vigente a S/ 8.00, mañana programada a S/ 9.00). En
Parámetros de cobro, el aviso "no se cobra táper" desapareció al poner
S/ 1.00 (inventado) y la bitácora registró las tres acciones.

## Encontrado en el camino

- **Una prueba mía dependía de la base de desarrollo.** Pasó al principio,
  falló en cuanto puse el táper en S/ 1.00 desde la pantalla (cambiar a
  "sin monto" ya escribía una fila que la prueba no esperaba). Se rehízo
  para no depender del valor previo. Es la misma familia de problema que
  ya está en `docs/pendientes`: las pruebas comparten la base de
  desarrollo.
- **La V25 no es mía** (identidad visual de locales, agregada en paralelo);
  la mía es la V26. Sin conflicto, pero conviene mirar el número antes de
  crear una migración.
- **El shell no aguanta bloques muy grandes** de código en un solo comando;
  se escriben archivo por archivo.

## Datos de desarrollo (no son reales)

En la base de desarrollo quedaron: **"Plan pensionista"** (desayuno
S/ 5.00, almuerzo S/ 8.00, cena S/ 8.00, con un cambio programado del
almuerzo a S/ 9.00 desde el 30/09/2026) y el **táper en S/ 1.00**. Todo
inventado por pedido del usuario, para poder avanzar. **Hay que
reemplazarlos con los reales del dueño antes de cobrar a un pensionista de
verdad.**

## Parámetros de cobro rediseñados (2026-09-29)

**Qué se pidió.** Rehacer la pantalla en el tema verde (diseño primero en el canvas:
"Parámetros de cobro"): ámbito, cargo por táper con ejemplo, segundo almuerzo, y "Cambios y
auditoría".

**Qué se construyó (solo frontend; el backend no cambió).**
- `PantallaParametrosDeCobro.tsx` + `estilos/parametros-cobro.css`. Las dos tarjetas se editan y
  se guardan juntas con "Guardar cambios" (una llamada `PUT /parametros/{clave}` por cada
  parámetro que cambió); "Descartar cambios" vuelve a lo guardado. El pie muestra "N cambios sin
  guardar".
- Táper: el monto vacío sigue significando "no se cobra táper" (aviso ámbar); el ejemplo
  "2 menús para llevar → S/ X" se recalcula al escribir; un monto inválido bloquea Guardar.
- Segundo almuerzo: dos opciones en tarjeta, "Recomendado" en el precio del plan.
- **Ámbito:** se muestra "Todos los locales" bloqueado. Los parámetros son globales (una fila por
  clave en `parametro_global`); no existen por local. Tenerlos por local es un cambio de modelo
  aparte.
- **Último cambio** (con `auditoria.ver`): sale de `GET /auditoria?evento=cambio_parametro&tamano=1`,
  sin endpoint nuevo. "Ver historial de cambios" abre `/admin/auditoria?evento=cambio_parametro`;
  para eso `PantallaAuditoria` ahora toma el evento inicial de la URL.
- Sin `pensionistas.administrar` la pantalla queda en solo lectura (sin barra de guardado).
- `comun/formato/momento.ts` (`formatearMomento`) se extrajo de la pantalla de Usuarios para
  compartirlo.

**Verificado en vivo** (admin): datos reales (S/ 1.00, precio del plan), monto inválido y vacío,
ejemplo dinámico, contador de cambios, descartar, guardar los dos parámetros (aparecen en la
bitácora), volver a los valores originales, "Último cambio", y el enlace a la bitácora con el
filtro ya elegido. `npx tsc -b --force` sin errores.
