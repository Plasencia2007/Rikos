# US-034 — Descuentos y anulación en caja (sprint 7, entrega 3)

**Especificado en:** `docs/producto/plan/sprint-07-cobrar-y-cerrar.md`. En el backlog v3 es US-20. Hecho el 2026-10-11, solo backend y contrato de la API.

## Qué se pidió
Que un usuario autorizado pueda aplicar un descuento al cobrar, o anular, dejando constancia de quién lo autorizó y por qué: permiso específico, motivo obligatorio y
auditoría con usuario, fecha, local, valores anterior y nuevo y el motivo (US-034 CA1 a CA3, RN-072).

## Qué se construyó
- **Descuento por línea al cobrar** (decisión 7 del dueño): el cobro (`POST /pedidos/{id}/cobros`) acepta `descuentos:[{lineaId, tipo:'monto'|'porcentaje', valor, motivoId, observacion}]`.
  `monto` va en céntimos; `porcentaje` de 1 a 100 sobre el importe de esa línea (precio y táper por cantidad), redondeado al céntimo. El snapshot de la línea guarda precio original,
  descuento, importe cobrado y el motivo (V42: `motivo_descuento_id`, `observacion_descuento`, y un `CHECK` que impide un descuento sin motivo).
- **Permiso `caja.descontar`** (nuevo, en el catálogo): lo tienen el administrador de local y el central. El cajero **no** lo tiene por defecto; el dueño puede dárselo desde Roles.
  Sin él, un cobro con descuentos responde 403 `SIN_PERMISO_PARA_DESCONTAR` (y sin descuentos el cajero cobra normal).
- **Motivo obligatorio:** debe existir, estar activo y ser de tipo `descuento` (`MOTIVO_NO_ENCONTRADO`, `MOTIVO_INVALIDO`); si el motivo exige observación, también
  (`OBSERVACION_OBLIGATORIA`).
- **Reglas del descuento:** un solo descuento por línea; solo de líneas que se están cobrando ahora; mayor que cero y sin pasar del importe de la línea (`DESCUENTO_INVALIDO`);
  y lo que se cobra no puede quedar en cero (`TOTAL_EN_CERO`). El descuento baja lo que va a medios o al cargo del pensionista, y el vuelto se calcula sobre el nuevo total.
- **Bitácora:** un evento `descuento` por línea, con el importe anterior, el descuento, el importe nuevo, el producto, el pedido, el motivo y la observación.
- **Anulación en caja:** el endpoint de anular (`POST /pedidos/{id}/lineas/{id}/anular`) ya exigía `venta.anular` (cajero y administradores), motivo y bitácora (sprint 6).
  Lo nuevo: **si el pedido ya se cobró en parte y la línea anulada era lo último que quedaba por cobrar, el pedido se cierra solo** y la mesa queda libre. Sin nada cobrado no hay venta:
  el pedido se termina a mano con "Terminar pedido".

## Arreglos de la auditoría del 2026-10-11 sobre el cobro (entrega 2)
La auditoría integral (`docs/pendientes/02-auditoria-integral-sprints-0-7-2026-10-11.md`) encontró cuatro problemas en mi código del cobro; se arreglaron aquí:
- **CAJ-01:** dos pagos en el mismo medio daban 500. Ahora son `MEDIO_REPETIDO` (un pago por medio).
- **CAJ-03:** dos cajeros cobrando las dos últimas líneas a la vez dejaban todo cobrado sin cerrar. El pedido se toma con **bloqueo de escritura** (`bloquearPorIdYLocal`): se turnan
  y el segundo ve lo que el primero cobró. No hay prueba automática de la carrera (las pruebas son de una sola conexión); queda como validación manual con dos cajeros.
- **CAJ-05:** dos primeras consultas a la vez creaban dos "Caja 1" y una fallaba. Ahora es `INSERT ... ON CONFLICT DO NOTHING`.
- **CAJ-06:** `pagos:[null]` daba 500 y los montos descomunales podían desbordar. Ahora `MEDIO_INVALIDO` / `MONTO_INVALIDO` (tope de S/ 1'000,000 por pago).
Siguen abiertos de esa auditoría sobre el cobro: **CAJ-02** (el snapshot no guarda el árbol completo de ingredientes y subrecetas), **CAJ-04** (reintento tras una respuesta perdida)
y **CAJ-07** (el total no distingue lo cobrado de lo pendiente), más **POS-07** ("Terminar pedido" y las líneas aún sin comandar).

## Decisiones tomadas al ejecutar
- **El permiso de descontar no es del cajero por defecto.** US-034 dice "cajero autorizado / administrador": se dejó que el dueño decida a qué cajeros se lo da (pregunta abierta sobre topes por rol).
- **`caja.anular` no se usa para anular líneas**: se mantiene `venta.anular` (ya existía, con motivo y bitácora). `caja.anular` queda reservado para anular un cobro o movimiento de caja (entrega 5).
- **Un solo descuento por línea y no se acumulan**: simple y auditable; un descuento global es "aplicar a todas las líneas marcadas" en la pantalla.
- **El porcentaje se calcula sobre el importe bruto de la línea, táper incluido.**

## Cómo se verificó
- `CobroConDescuentoTest` (11): monto y porcentaje, traza en la bitácora, motivo ausente / de otro tipo / que exige observación, descuentos mal formados, total en cero, una línea con y otra sin
  descuento, y las tres variantes de anular lo último que queda por cobrar.
- `CobrarPedidoTest` +2 (pagos repetidos, nulos y montos descomunales) y `CobroAccesoPorRolTest` +2 (descontar exige su permiso; `pagos:[null]` es 422).
- Resultados y conteo de la suite completa al cierre del documento del sprint.

## Qué queda
La pantalla de caja (necesita diseño), el cierre de turno (entrega 4) y la reapertura (entrega 5). Preguntas abiertas para el dueño: tope de descuento por rol, motivos iniciales de descuento
y si el cajero debe poder descontar.
