# Enmiendas al backlog v3

`03-backlog-v3.md` es una copia íntegra y no se edita. Lo que el equipo decide distinto del v3 se anota aquí, con fecha y razón; el criterio vigente
es el de esta lista y el de `flujos/`.

## 2026-10-05 — US-07 CA2 (repo US-029): unir mesas

- **v3:** Given dos mesas ocupadas, When las une, Then quedan unidas.
- **Vigente:** se une una mesa **libre sin pedido** a una **principal** (libre o con pedido); una mesa con pedido en curso no se une como secundaria
  (`TIENE_PEDIDO_ABIERTO`, `MESA_NO_LIBRE`).
- **Razón:** "ocupada" ahora significa solo pedido comandado (regla del 2026-10-01, `sprint-06b-lista-de-pedidos.md` §7): unir dos pedidos ya comandados
  obligaría a fusionar dos cuentas con comandas distintas. El dueño aprobó cambiar el criterio y no el código.
- **Cambiado en:** `flujos/05-ep05-pos-mesas-cocina.md` (US-029, CA2).

## 2026-10-05 — "Terminar pedido" queda provisional hasta el sprint 7

- **Brecha señalada** en `plan/04-comparacion-backlog-v3.md` §2: el cierre sin cobro exige solo `venta.modificar` (el mozo lo tiene), no pide motivo y no
  congela el snapshot de costo (US-09 CA3, RN-072, RN-065, RN-005).
- **Decisión del dueño:** no se endurece ahora; se resuelve en el **sprint 7 (cobrar y cerrar)**, donde "Terminar pedido" se reemplaza por el cobro real
  (turno, forma de pago, anulación con permiso y motivo, snapshot de costo).
- **Hasta entonces:** los pedidos `cerrado` sin cobro no cuentan como ventas y no deben usarse en reportes. El sprint 7 debe especificar qué pasa con ellos.
