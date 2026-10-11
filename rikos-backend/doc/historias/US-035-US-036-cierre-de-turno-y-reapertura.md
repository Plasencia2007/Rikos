# US-036 y US-035 — Cierre de turno y reapertura (sprint 7, entregas 4 y 5)

**Especificado en:** `docs/producto/plan/sprint-07-cobrar-y-cerrar.md`. En el backlog v3 son US-22 y US-21. Hechas el 2026-10-11, solo backend y contrato de la API
(la pantalla de caja necesita el diseño de la otra sesión). Con estas dos entregas el sprint 7 queda completo en backend.

## Entrega 4 — Cerrar el turno (US-036)

### Qué se pidió
Que el cajero cierre su turno conciliando lo esperado con lo contado: ver el efectivo esperado **antes** de contar (RN-067), alerta desde S/ 0,01 de diferencia sin
bloquear el cierre (RN-070), y un cierre inalterable y auditado (RN-006).

### Qué se construyó
- **V43:** `cierre_caja` (esperado, contado, diferencia, totales de Yape, Plin y tarjeta, observación, quién y cuándo), único por turno, con un `CHECK`
  `diferencia = contado - esperado`, y un **trigger** que rechaza cualquier `UPDATE` o `DELETE` (inalterable incluso desde SQL). `turno_caja` guarda `cerrado_por`.
- **Efectivo esperado = fondo inicial + efectivo cobrado en el turno.** El vuelto no cuenta (la caja registra lo que entra) y los cobros reabiertos tampoco. Los totales digitales
  salen de los movimientos de caja del turno.
- **`ConsultaCierre.previo`** (el resumen antes de contar) y **`CerrarTurno`**: el cajero cierra **su** turno cuando quiera (fin de su turno, fin del día o en cualquier momento; no hay
  corte automático). Un pedido a medio cobrar no impide cerrar. Al cerrar, el turno queda `cerrado`, la caja puede abrir otro y no se cobra más en él.
- **API:** `GET /turnos/actual/resumen` (caja.ver), `POST /turnos/actual/cierre` con `{efectivoContado, observacion}` (caja.crear), `GET /turnos/{id}/cierre` (caja.ver).
  El cierre responde con la `diferencia` (negativa si falta dinero) y `alerta` (true si hay cualquier diferencia).
- **Bitácora:** evento `cierre_caja` con esperado, contado y diferencia.

## Entrega 5 — Reabrir un cobro (US-035)

### Qué se pidió
Reabrir una venta solo mientras el turno de origen siga abierto (RN-073), con permiso específico, motivo y auditoría; después del cierre, la corrección es una operación compensatoria.

### Qué se construyó
- **V44:** el cobro guarda `revertido_en/por`, el motivo y la observación de la reapertura; `cobro_linea` gana `vigente` y el único por línea pasa a ser un índice parcial
  (`WHERE vigente`), así la misma línea se puede cobrar de nuevo.
- **`ReabrirCobro`** (una transacción): las líneas del cobro vuelven a `comandado`, el pedido vuelve a `comandado` si el cobro lo había cerrado (la mesa se ocupa otra vez), el cargo al
  pensionista se revierte con un **ajuste a favor** en su cuenta (el libro es de solo agregado; motivo "Corrección de cobro") y el dinero del cobro **sale del efectivo esperado**. El cobro no
  se borra: queda revertido con quién, cuándo y por qué.
- **Reglas:** el turno de origen debe seguir abierto (`TURNO_CERRADO`: la compensación queda para el sprint 11); el cobro no puede estar ya reabierto (`COBRO_YA_REVERTIDO`); la mesa no puede estar
  ocupada por otro pedido (`MESA_OCUPADA`); motivo obligatorio de tipo `reapertura` y observación si el motivo la exige.
- **API:** `POST /cobros/{id}/reapertura` con `{motivoId, observacion}` (permiso `caja.reabrir`: cajero y administradores) y `GET /pedidos/{id}/cobros` (caja.ver), con las líneas de cada
  cobro y si ya se reabrió.
- **Bitácora:** evento `reapertura` con el motivo y la observación.

## Decisiones tomadas al ejecutar
- **El cajero cierra solo su turno** (no un administrador el de otro): lo pidió la decisión 1 (el cajero decide cuándo). Si el dueño quiere que un administrador pueda cerrar el turno de otro
  (por ejemplo si el cajero se fue), es una pregunta abierta.
- **Se reabre un COBRO, no "una venta" entera:** el cobro es la unidad que ya tiene su turno, su dinero y su snapshot; reabrir el pedido completo sería reabrir varios cobros.
- **Las mesas unidas que se separaron al cerrar el pedido no se vuelven a unir** al reabrirlo.
- **El ajuste a favor del pensionista usa el motivo sembrado "Corrección de cobro"** (V28), no un motivo elegido: el motivo de la reapertura queda en el cobro y en la bitácora.

## Cómo se verificó
- `CerrarTurnoTest` (10): resumen previo, conteo exacto, diferencia de S/ 0,01 a favor y en contra con alerta, totales por medio, turno cerrado (no se cobra y se puede abrir otro),
  conteo inválido y sin turno, cierre inalterable con `UPDATE` desde SQL, bitácora y consulta del cierre.
- `ReabrirCobroTest` (10): líneas, pedido y mesa, dinero fuera del esperado, volver a cobrar, devolución del cargo al pensionista, turno cerrado, ya reabierto, motivo, mesa ocupada,
  otro local y bitácora.
- `CierreYReaperturaAccesoPorRolTest` (5): acceso por rol con los roles reales y el recorrido completo por HTTP.
- Suite completa al cierre: ver `ESTADO.md`.

## Qué queda del sprint 7
La **pantalla de caja** (abrir turno, cobrar por líneas con descuentos, cerrar con conteo y reabrir) necesita el diseño de la sesión de diseño. Preguntas para el dueño: fondo inicial habitual,
nombres de las cajas, tope de descuento por rol y quién puede descontar, motivos iniciales de descuento y reapertura, y si un administrador puede cerrar el turno de otro cajero.
