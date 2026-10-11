# Sprint 7 — Cobrar y cerrar caja

**Historias (del backlog):** US-032 (abrir turno), US-033 (cobrar con medios divididos y cierres parciales), US-034 (descuentos y anulaciones con
autorización y motivo), US-035 (reabrir dentro del turno), US-036 (cerrar el turno con conciliación). En el backlog v3 son US-18 a US-22. Flujos en
`flujos/06-ep06-caja-facturacion.md`; modelo en `modelo-de-datos/07-caja-turnos-pagos--ep06.md`.

**Estado:** especificado y aprobado el 2026-10-05; **entrega 1 (cajas y turnos) hecha el 2026-10-05** (`US-032-cajas-y-turnos.md`) **entrega 2 (cobro)** (`US-033-cobro.md`) y **entrega 3 (descuentos y anulación en caja)** (`US-034-descuentos-y-anulacion-en-caja.md`) y **entregas 4 (cierre de turno) y 5 (reapertura)** (`US-035-US-036-cierre-de-turno-y-reapertura.md`) hechas el 2026-10-11 en backend. **El sprint 7 está completo** y tiene una pantalla de caja básica y provisional en el POS (`US-032-036-pantalla-de-caja.md`); el diseño definitivo lo hace la sesión de diseño.

**Cómo se llegó a esto:** brainstorming con el usuario el 2026-10-05, una decisión por vez. Lo que dijo el usuario va marcado como tal; lo que asumí yo, como supuesto.

## 1. Qué queremos lograr

Que un cajero cobre de verdad: abre su turno con un fondo, cobra lo que el cliente pide pagar (aunque sea una parte de la mesa) repartiendo el pago entre medios, carga al pensionista lo suyo
a su cuenta, aplica un descuento o anula con permiso y motivo, y al final del turno concilia lo esperado con lo contado. Cobrar reemplaza al "Terminar pedido" provisional del sprint 6.

**Éxito del sprint:** en una mesa mixta (un cliente normal y un pensionista) el cajero marca las líneas de los dos, el sistema separa solo el pago en medios y el cargo a la cuenta, el cliente
normal paga mitad efectivo y mitad Yape con vuelto, queda una línea sin cobrar y la mesa **no** se libera; después se cobra esa línea y la mesa se libera. Al cerrar el turno el efectivo esperado
coincide con lo cobrado en efectivo más el fondo, y una diferencia de S/ 0,01 alerta sin bloquear.

## 2. Decisiones del usuario (2026-10-05)

| # | Decisión | Detalle |
|---|---|---|
| 1 | **Cajas** | El modelo soporta varias cajas por local (2 o 3), cada una con su turno y su dinero, **sin cruzarse**. Al inicio se siembra **una** por local. El cajero cierra su turno cuando quiera: fin de su turno, fin del día o en cualquier momento. **No hay corte automático por horario.** Cierra RF-021. |
| 2 | **Quién cobra** | Solo un **cajero con turno abierto**; el cobro entra a **su** caja. El mozo no cobra, solo pide. |
| 3 | **Qué se cobra** | **Por líneas.** El cajero marca los productos que se pagan ahora, aunque sean de distintas personas. Una cuenta queda cobrada cuando todas sus líneas están cobradas o anuladas. |
| 4 | **Mezcla con pensionistas** | Un mismo cobro puede incluir líneas de clientes normales y de pensionistas: el sistema **separa solo** (las normales al pago en medios, las del pensionista a su cuenta corriente). |
| 5 | **"Terminar pedido"** | Queda **solo para pedidos sin nada que cobrar** (vacíos o con todas las líneas anuladas). Con productos comandados hay que cobrar o anular con motivo y permiso. |
| 6 | **Medios de pago** | Efectivo con **monto recibido y vuelto** (el vuelto no es ingreso). Yape, Plin y tarjeta con **número de operación opcional**. Sin propinas. |
| 7 | **Descuentos** | **Por línea** (monto fijo o porcentaje), con permiso específico, motivo y auditoría. Atajo "aplicar a todas las marcadas". |
| 8 | **Reapertura** | Solo **dentro del turno abierto de origen**. La **compensación** después del cierre se **posterga al sprint 11** (RF-014, con el contador y las notas de crédito). |
| 9 | **Cierre del turno** | Un **solo total contado** más observación opcional. |

### 2 bis. Decisiones del dueño al cerrar el backend (2026-10-11)

- **El fondo inicial siempre lo ingresa el cajero al abrir el turno** (no hay un fondo por defecto; ya es obligatorio, puede ser cero).
- **La caja se rotula "Caja de <nombre del cajero>"** mientras tiene un turno abierto (campo `etiqueta` de la API; el nombre de la caja sigue siendo el de la lista, "Caja 1" por defecto).
- **El cajero puede descontar siempre y cuando haya un descuento habilitado** (un motivo de tipo descuento activo): `caja.descontar` es también del cajero (V45).

## 3. Qué queda fuera, y por qué

- **Descuento de inventario al vender** (US-033 CA5): depende del inventario (sprint 9). El snapshot de la venta **sí** guarda la receta aplicada, para que el sprint 9 la use.
- **Comprobantes electrónicos y su envío por correo** (US-037 y US-062): sprint 11. En este sprint el cobro registra los datos que el comprobante necesitará (pensionista con nombre y DNI).
- **Abono en efectivo del pensionista** (US-021): sprint 8. El modelo ya prevé `movimiento_caja.tipo = 'abono_pensionista'`, pero no se construye aquí.
- **Compensación posterior al cierre**: sprint 11 (decisión 8).
- **Desglose del conteo por denominaciones**: no se pidió; se puede añadir después sin tocar cierres ya hechos.
- **Pantalla de caja del POS**: la diseña la otra sesión (diseño); aquí se entrega el contrato de la API.

## 4. Decisiones técnicas, con su razón

### 4.1 Módulo `caja`
Cajas, turnos, cobros, pagos y cierres van en un módulo propio `caja`. Depende de `venta` (pedido y líneas), `pensionistas` (cuenta corriente y motor de precios), `identidad` y `catalogo` (motivos).
Las dependencias se exponen con `@NamedInterface` y las vigila `ArquitecturaDeModulosTest`.

### 4.2 Un cobro no cabe en la tabla `pago` del modelo tal cual
El modelo enlaza `pago` a `cuenta_persona` (cobro por persona). Como el cobro es **por líneas**, se agrega:

- `cobro`: un acto de cobrar sobre un pedido (pedido, turno, caja, cajero, momento, total).
- `cobro_linea`: una fila por línea cobrada, con el **snapshot**: precio original, descuento, importe cobrado, costo, receta aplicada, utilidad y margen, y a qué medio fue (normal o cuenta del pensionista).
- `pago` (como el modelo) pasa a colgar del `cobro` y a llevar `monto_recibido` y `vuelto` en efectivo.

**Por qué:** una línea cobrada es el hecho que el snapshot y los reportes necesitan; el pago es solo cómo se pagó. Esto es una **desviación del modelo de datos**, que se corrige en el mismo sprint
(`modelo-de-datos/07`) y se registra como DT.

### 4.2 bis Lo que cambió al construir la entrega 2 (2026-10-11)

- **No hay tabla `medio_pago`:** el medio es un texto con `CHECK` (efectivo, yape, plin, tarjeta); "cuenta del pensionista" es un cargo (`cobro_cargo`), no un medio de caja.
- **Solo se cobra lo comandado** (una línea pendiente da `LINEA_SIN_COMANDAR`).
- **Costo y utilidad del snapshot en soles**, precios e importes en céntimos.
- **"Terminar pedido"** cierra también un pedido con líneas aún sin comandar (no hay nada que cobrar de ellas); con líneas comandadas da `PEDIDO_CON_LINEAS_POR_COBRAR`.
- `venta.aplicacion` y `venta.infraestructura` se exponen con `@NamedInterface`; el cierre del pedido lo comparten `TerminarPedido` y el cobro (`CerrarPedido`).

### 4.3 Estado de las líneas: se agrega `cobrado`
`detalle_pedido.estado` hoy es pendiente, comandado o anulado. Se agrega `cobrado`. Una línea cobrada es inmodificable salvo por la reapertura dentro del turno.

### 4.4 La mesa se libera por cálculo, no por una acción
Se mantiene el principio del sprint 6 (estado derivado, DT-055): cuando el pedido no tiene líneas pendientes ni comandadas sin cobrar, queda `cerrado` y la mesa queda libre. Mientras quede una, el
pedido sigue comandado (y la mesa ocupada).

### 4.5 Cargo al pensionista
Las líneas de una persona pensionista **no entran al pago en medios**: generan un `movimiento_cuenta` de cargo en su cuenta corriente (módulo `pensionistas`), aplicando primero el saldo a favor; si se supera el límite se **alerta sin bloquear**
(RN-015, RN-016, RN-017, RN-018). El cargo se hace en la **misma transacción** que el cobro.

### 4.6 Restricciones que sostiene la base de datos, no solo el código
- **Un solo turno abierto por caja y por cajero**: dos índices únicos parciales (`WHERE estado = 'abierto'`). Sin eso no se puede decidir a qué turno pertenece una venta (RN-073).
- **`movimiento_caja.tipo`** con exactamente dos valores (`venta`, `abono_pensionista`) y **ningún** endpoint de gasto, retiro o ingreso extraordinario (RN-069).
- **Cierre inalterable**: `cierre_caja` es de solo agregado; no hay `UPDATE` ni `DELETE` desde la aplicación, y un test lo verifica.
- **La suma de los pagos en medios = lo cobrado a clientes normales** (menos el vuelto en efectivo): se comprueba en el caso de uso y con un test.

### 4.7 Permisos
Ya sembrados: `caja.ver`, `caja.crear`, `caja.anular`, `caja.reabrir`. Mapeo: abrir turno, cobrar y cerrar turno = `caja.crear`; anular en caja = `caja.anular`; reabrir = `caja.reabrir`; **descuento** = permiso nuevo
`caja.descontar` (RN-012 lo lista como acción; se siembra en una migración y se asigna a administrador y cajero según el criterio de los permisos predeterminados). Cada endpoint lleva su test de acceso por rol.

### 4.8 Concurrencia
El cobro actualiza líneas (que ya tienen `@Version`, DT-056). Dos cajeros cobrando la misma línea: el segundo falla con 409 y la pantalla recarga. El cobro se hace bajo el mismo `protegido()` de los controladores de venta.

### 4.9 Auditoría
Eventos nuevos (cada uno con su fila en `auditoria_evento_check`): `turno_abierto`, `cobro_registrado`, `descuento_aplicado`, `linea_anulada_en_caja`, `venta_reabierta`, `turno_cerrado`. Todos con usuario, local, caja y valores anterior y nuevo; descuentos y anulaciones con motivo.

## 5. Entregas

Cada entrega termina verificable y con su historia en `rikos-backend/doc/historias/`.

| # | Entrega | Qué se puede demostrar | Historias |
|---|---|---|---|
| 1 | **Cajas y turnos** | Abrir un turno con fondo en una caja; no se puede abrir un segundo en la misma caja; dos cajas del mismo local tienen turnos independientes. | US-032 |
| 2 | **Cobro** (backend hecho el 2026-10-11; falta la pantalla) | La mesa mixta del "éxito": marcar líneas, pago dividido con vuelto, cargo al pensionista, cierre parcial sin liberar la mesa, snapshot por línea, "Terminar pedido" acotado. | US-033, decisión 5 |
| 3 | **Descuentos y anulación en caja** (backend hecho el 2026-10-11; el permiso de descontar es `caja.descontar` y anular usa `venta.anular`, ver `US-034-descuentos-y-anulacion-en-caja.md`) | Descuento por línea con permiso y motivo; anular lo último que queda por cobrar cierra el pedido; todo en la bitácora. | US-034 |
| 4 | **Cierre de turno** (backend hecho el 2026-10-11) | Efectivo esperado visible antes de contar, conciliación, alerta desde S/ 0,01, cierre inalterable y auditado. | US-036 |
| 5 | **Reapertura dentro del turno** (backend hecho el 2026-10-11) | Reabrir una venta con `caja.reabrir`, solo con el turno de origen abierto; revierte el cargo al pensionista y el snapshot. | US-035 (sin compensación) |

**Orden:** 1 → 2 → 3 → 4 → 5. La entrega 4 va antes que la 5 porque define cuándo un turno "ya cerró", que es lo que la reapertura necesita.

## 6. Pruebas (TDD, test antes o junto con el código)

- **Turnos:** un solo turno abierto por caja (a nivel de base de datos); cajas distintas del mismo local no se cruzan; abrir con fondo; un cajero sin permiso no abre.
- **Cobro:** suma de pagos = total de las líneas normales; vuelto solo en efectivo; mezcla normal + pensionista separa; cargo al pensionista con saldo a favor, remanente a deuda, alerta sobre el límite sin bloquear; cierre parcial no libera la mesa; última línea cobrada cierra el pedido y libera la mesa; línea ya cobrada no se cobra dos veces (concurrencia → 409); mozo no cobra (acceso por rol).
- **Snapshot:** una línea cobrada conserva precio, descuento, costo y receta aunque después cambien el precio o la receta.
- **Descuentos y anulación:** sin permiso no; sin motivo no; auditoría con valores anterior y nuevo.
- **Cierre:** el esperado = fondo + efectivo cobrado − vuelto; diferencia negativa y positiva; S/ 0,01 alerta y deja cerrar; el cierre no admite modificación.
- **Reapertura:** dentro del turno abierto sí; con el turno cerrado no; sin permiso no; revierte el cargo.
- **Arquitectura:** `ArquitecturaDeModulosTest` con el módulo nuevo (sin agregar ciclos).
- **Pantalla:** verificada en el navegador a 1280 y 375 px, con el usuario de desarrollo documentado.

## 7. Migraciones previstas

V39 en adelante, sin editar ninguna aplicada: `caja` (la caja "Caja 1" se crea sola la primera vez que un local la pide, no por SQL), `turno_caja` (con el índice único parcial), `medio_pago` (efectivo, yape, plin, tarjeta, cuenta_pensionista), `cobro`, `cobro_linea`, `pago`,
`movimiento_caja`, `cierre_caja`; `detalle_pedido.estado` con `cobrado`; permiso `caja.descontar`; eventos de auditoría nuevos.

## 8. Preguntas que siguen abiertas

- **Números reales:** fondo inicial habitual por caja y nombres de las cajas (la sembrada se llama "Caja 1" hasta que el dueño diga otra cosa).
- **Reglas del descuento:** tope máximo permitido por rol, si lo hay (hoy: ninguno, solo permiso y motivo).
- **Motivos iniciales** de descuento y de anulación en caja (RF-005): hay que sembrar una lista de partida con el dueño; hasta entonces se usa la de desarrollo.
- **Qué hacer con los pedidos "cerrados sin cobro"** que ya existen en desarrollo: quedan marcados como tales y fuera de los reportes (propuesta; confirmar al llegar al sprint 14).
- **Diseño:** la pantalla de caja necesita un diseño de la sesión de diseño antes de la entrega 2.

## 9. Riesgos

- **El cobro por líneas con pensionistas es la parte más delicada** (cargo + snapshot + liberación de mesa en una sola transacción). Se hace primero el caso de un cliente normal y luego el pensionista, con prueba de transacción atómica.
- **El snapshot de costo depende de que el costo de la receta exista** (DT-054: nulo si faltan costos de insumos). Una línea sin costo se cobra igual y su costo queda nulo; el reporte del sprint 14 debe tratarlo como "sin costo", nunca como cero.
- **Dos cajeros, mismo pedido:** cubierto por el bloqueo optimista, pero la pantalla debe recargar bien tras el 409.
