# US-033 — Cobro por líneas (sprint 7, entrega 2)

**Especificado en:** `docs/producto/plan/sprint-07-cobrar-y-cerrar.md`. En el backlog v3 es US-19. Hecho el 2026-10-11, solo backend y contrato de la API
(la pantalla de caja necesita el diseño de la otra sesión).

## Qué se pidió
Que un cajero con turno abierto cobre lo que el cliente quiere pagar ahora: las **líneas** que marque (aunque sean de varias personas), repartiendo el pago
entre efectivo, Yape, Plin y tarjeta; que las líneas de un **pensionista** se carguen a su cuenta corriente en vez de pedir medios; que el cierre parcial
no libere la mesa; y que quede el snapshot económico de cada línea cobrada. Cobrar reemplaza al "Terminar pedido" provisional del sprint 6.

## Qué se construyó
- **Migración V41:** `cobro`, `cobro_linea` (el snapshot), `pago`, `movimiento_caja` y `cobro_cargo`; el estado `cobrado` de `detalle_pedido`; el evento
  de bitácora `cobro_registrado`. `cobro_linea_una_vez` (único por línea) impide cobrar dos veces una misma línea aunque dos cajeros lo intenten a la vez.
- **`CobrarPedido`** (`caja/aplicacion`), todo en **una transacción**: valida, escribe el cobro, el snapshot de cada línea, los pagos, los movimientos de caja,
  los cargos a cuentas y, si ya no queda nada por cobrar, cierra el pedido (la mesa queda libre porque su estado se deriva de los pedidos, DT-055).
- **`CerrarPedido`** (`venta/aplicacion`, público): el cierre que comparten "Terminar pedido" y el cobro (cierra el pedido y separa las mesas unidas).
  `venta.aplicacion` y `venta.infraestructura` ahora se exponen con `@NamedInterface` para que `caja` los use.
- **API:** `POST /pedidos/{id}/cobros` con `{lineaIds, pagos:[{medio, monto, referencia}]}` (permiso `caja.crear`; el mozo, cocina y motorizado reciben 403).
  Responde 201 con el cobro, los pagos, el vuelto, los cargos (con `superaTope`) y `pedidoCerrado`.
- **Reglas de los pagos:** en efectivo `monto` es lo **recibido** y el servidor calcula el vuelto; la caja registra lo que entra, no lo que entregó el cliente.
  Solo el efectivo puede dar vuelto: lo digital, sumado, no puede pasar del total (`PAGO_EXCEDE_EL_TOTAL`). Si la suma no cubre el total, `PAGO_INSUFICIENTE`.
- **Pensionistas:** las líneas de una persona pensionista no entran al pago en medios: generan un cargo en su cuenta con `RegistrarConsumo` (el saldo a favor
  se consume solo porque el saldo es la suma del libro). Si la deuda pasa del tope, `superaTope=true` y **nunca bloquea**. Un pensionista desactivado no
  consume (`PENSIONISTA_INACTIVO`) y no se cobra ninguna línea.
- **Una línea cobrada es inmodificable:** no se modifica, no se quita, no se anula ni se traslada (`LINEA_COBRADA`); la reapertura es de la entrega 5.
- **"Terminar pedido" acotado** (decisión 5 del dueño): rechaza con `PEDIDO_CON_LINEAS_POR_COBRAR` si hay líneas comandadas. Para cerrar un pedido así hay que
  cobrar o anular con motivo y permiso. Cierra los pedidos vacíos, con todo anulado o con líneas aún sin comandar (no hay nada que cobrar de ellas).
- **Snapshot por línea:** nombre, cantidad, precio, taper, descuento (cero hasta la entrega 3), importe, **costo unitario de la receta** y utilidad, y la composición
  de la receta (en un menú, la de cada componente). Sin receta o con un componente sin costo, el costo queda **nulo**, nunca cero.
- **Bitácora:** `cobro_registrado` ("X cobró el pedido N por S/ …").

## Decisiones tomadas al ejecutar
- **Solo se cobra lo comandado.** Una línea pendiente da `LINEA_SIN_COMANDAR`: el consumo se registra cuando salió a cocina. No estaba escrito; se asumió.
- **No hay tabla `medio_pago`** (el modelo original la tenía): el medio es un texto con `CHECK` (efectivo, yape, plin, tarjeta). "Cuenta del pensionista" no es un
  medio de caja sino un cargo (`cobro_cargo`), porque no mueve dinero del turno. Desviación del modelo de datos 9.5, registrada como DT.
- **Costo y utilidad van en soles** (como los costos del catálogo, DT-054); los precios e importes, en céntimos. Es una mezcla de unidades a propósito: se
  convierte al reportar (sprint 14).
- **El cajero cobra en SU turno.** Quien cobra debe tener un turno abierto en el local activo; el cobro entra a la caja de ese turno.
- **Un cobro no tiene id del cliente** (idempotencia por reintento): no hace falta todavía porque el servidor valida cada línea y el `UNIQUE` evita el doble cobro.

## Cómo se verificó
- `CobrarPedidoTest` (23): vuelto, pago dividido, pago exacto, suma insuficiente, vuelto solo en efectivo, validación de medios y montos antes de escribir, sin turno,
  turno de otro local, cierre parcial, líneas de varias personas, línea sin comandar / ya cobrada / anulada / inexistente, mesa libre al cerrar, snapshot,
  línea cobrada inmodificable, "Terminar pedido" acotado, y el pensionista (cargo, mesa mixta, tope, nada que pagar en medios, desactivado).
- `CobroAccesoPorRolTest` (7): acceso por rol con los roles reales (cajero y administradores cobran; mozo, cocina y motorizado no), 409 sin turno, 422/404/409 con su código,
  la mesa queda libre en `GET /mesas`, y "Terminar pedido" 409 con líneas comandadas.
- Pruebas existentes ajustadas: `TerminarPedidoTest` (anula antes de terminar). `ArquitecturaDeModulosTest` solo falla por el ciclo conocido `comun` ↔ `identidad`.
- Frontend: `tsc -b` limpio con el tipo nuevo `cobrado` y las funciones `listarCajas`, `abrirTurnoDeCaja`, `turnoActualDeCaja` y `cobrarPedido` en `api/cliente.ts`. Sin pantalla.

## Qué queda
- La **pantalla de caja** (abrir turno y cobrar por líneas): necesita el diseño de la sesión de diseño.
- Entrega 3 (descuentos y anulación en caja, que llena la columna `descuento`), entrega 4 (cierre del turno) y entrega 5 (reapertura, que revierte el cargo y el snapshot).
- Los pedidos "cerrados sin cobro" del sprint 6 siguen en la base sin filas en `cobro`: se reconocen por eso y no cuentan como ventas.
