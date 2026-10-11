<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 1538-1785. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

### EP-06 — Caja, cobro y facturación electrónica

---

### [US-032] Abrir el turno de caja con fondo inicial

- **Épica/Módulo:** EP-06 — Caja y facturación
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Cajero
- **Historia de usuario:**
  - Como cajero,
  - quiero abrir mi turno registrando el fondo inicial,
  - para que todos los cobros y abonos del turno queden asociados a mí y a esa caja.
- **Descripción funcional:** Apertura del turno de caja por caja, local y cajero, con registro del fondo inicial. Todas las operaciones de cobro y abono en efectivo del periodo se asocian al turno abierto.
- **Reglas de negocio asociadas:** RN-067, RN-002, RN-069.
- **Criterios de aceptación:**
  - **CA1 — Apertura**
    - Given un cajero con permiso
    - When abre el turno indicando el fondo inicial
    - Then el sistema debe crear el turno asociado a la caja, el local y el cajero.
  - **CA2 — Asociación de movimientos**
    - Given un turno abierto
    - When se registra un cobro o un abono en efectivo
    - Then el sistema debe asociar el movimiento a ese turno.
  - **CA3 — Solo ventas y abonos**
    - Given un turno abierto
    - When se revisan los tipos de movimiento permitidos
    - Then el sistema debe aceptar únicamente ventas y abonos en efectivo de pensionista.
- **Dependencias:** US-001, US-004, US-002.
- **Datos o entidades involucradas:** TurnoCaja (caja, local, cajero), MovimientoCaja, fondo inicial.
- **Riesgos, supuestos o pendientes:** la relación entre turnos de caja y horarios de servicio, y la cantidad de cajas por local, no están documentadas — ver RF-021.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Caja; `06-modelo-de-datos.md` § Caja.
- **Estimación inicial:** S

---

### [US-033] Cobrar la cuenta con medios divididos y cierres parciales

- **Épica/Módulo:** EP-06 — Caja y facturación
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Cajero
- **Historia de usuario:**
  - Como cajero,
  - quiero cerrar la cuenta dividiendo libremente el pago entre medios y permitiendo cierres parciales,
  - para cobrar como el cliente lo necesite sin liberar la mesa antes de tiempo.
- **Descripción funcional:** Cierre de la cuenta en caja: cálculo del total, división libre del pago entre efectivo, Yape, Plin y tarjeta, uso del saldo del pensionista para los consumos vinculados a su cuenta, y cierres parciales sin liberar la mesa mientras haya consumos pendientes. El cierre es el evento que genera el cargo en la cuenta del pensionista, el snapshot económico de la venta, el descuento de inventario por receta y la emisión del comprobante.
- **Reglas de negocio asociadas:** RN-071, RN-061, RN-015, RN-005, RN-046, RN-016, RN-017, RN-018, RN-035, RN-047.
- **Criterios de aceptación:**
  - **CA1 — División de pago**
    - Given una cuenta lista para cobrar
    - When el cajero reparte el importe entre efectivo, Yape, Plin y tarjeta
    - Then el sistema debe aceptar la división libre siempre que la suma cubra el total.
  - **CA2 — Cierre parcial**
    - Given una mesa con varios consumos
    - When se cierra la cuenta de una persona y quedan consumos pendientes
    - Then el sistema debe cerrar solo esa cuenta
    - And no debe liberar la mesa.
  - **CA3 — Cargo al pensionista**
    - Given consumos vinculados a la cuenta de un pensionista
    - When el cajero cierra la cuenta
    - Then el sistema debe generar el cargo en su cuenta corriente aplicando primero el saldo a favor
    - And debe alertar si se supera el límite sin bloquear el cierre.
  - **CA4 — Snapshot económico**
    - Given una venta cerrada
    - When se consulta posteriormente
    - Then el sistema debe conservar precio, costo, composición, receta aplicada, utilidad y margen históricos e inmutables.
  - **CA5 — Descuento de inventario**
    - Given una venta cerrada de un plato con receta
    - When se confirma el cierre
    - Then el sistema debe descontar los insumos de su receta, incluidas las cantidades de subrecetas, sin duplicar consumos.
  - **CA6 — Sin movimientos extraordinarios**
    - Given la pantalla de caja
    - When el cajero busca registrar un gasto, retiro o ingreso extraordinario
    - Then el sistema no debe ofrecer esa opción, salvo el abono en efectivo de pensionista.
- **Dependencias:** US-032, US-028, US-018, US-010, US-043, US-037.
- **Datos o entidades involucradas:** Pedido, CuentaPersona, DetallePedido, Pago, MedioPago, TurnoCaja, MovimientoCaja, MovimientoCuenta, MovimientoInventario, ComprobanteElectronico.
- **Riesgos, supuestos o pendientes:** el precio del táper y los precios definitivos están pendientes — ver RF-002 y RF-003.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` §§ Caja, POS y mesas; `02-pensionistas.md` §§ Cuenta corriente, Facturación; `04-menu-recetas-inventario-compras.md` §§ Recetas y costos, Producción; `06-modelo-de-datos.md` §§ Venta, Caja; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (1, 4, 11).
- **Estimación inicial:** XL

---

### [US-034] Aplicar descuentos y anulaciones con autorización y motivo

- **Épica/Módulo:** EP-06 — Caja y facturación
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Cajero / Administrador autorizado
- **Historia de usuario:**
  - Como cajero autorizado,
  - quiero aplicar un descuento o anular una venta indicando el motivo,
  - para resolver situaciones excepcionales dejando constancia de quién lo autorizó.
- **Descripción funcional:** Aplicación de descuentos y anulaciones en caja sujeta a permisos específicos y registro obligatorio del motivo, con auditoría completa.
- **Reglas de negocio asociadas:** RN-072, RN-004, RN-012.
- **Criterios de aceptación:**
  - **CA1 — Permiso requerido**
    - Given un usuario sin el permiso correspondiente
    - When intenta aplicar un descuento o anular una venta
    - Then el sistema debe impedirlo.
  - **CA2 — Motivo obligatorio**
    - Given un usuario con permiso
    - When aplica un descuento o una anulación sin indicar motivo
    - Then el sistema debe impedir la confirmación.
  - **CA3 — Auditoría**
    - Given un descuento o anulación confirmada
    - When se consulta la auditoría
    - Then el sistema debe mostrar usuario, fecha, hora, local, valores anterior y nuevo y el motivo.
- **Dependencias:** US-033, US-004, US-012, US-006.
- **Datos o entidades involucradas:** Pedido, Pago, descuento, motivo, registro de auditoría.
- **Riesgos, supuestos o pendientes:** motivos iniciales pendientes — ver RF-005.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Caja; `01-alcance-y-reglas.md` § Auditoría transversal; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (19).
- **Estimación inicial:** M

---

### [US-035] Reabrir una venta dentro del turno o compensarla después

- **Épica/Módulo:** EP-06 — Caja y facturación
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador autorizado
- **Historia de usuario:**
  - Como administrador autorizado,
  - quiero reabrir una venta solo mientras su turno siga abierto y, si ya cerró, corregirla con una operación compensatoria,
  - para arreglar errores sin alterar cierres ya realizados.
- **Descripción funcional:** Reapertura controlada de ventas cerradas con permiso específico y solo mientras el turno de origen esté abierto. Después del cierre del turno, la corrección se hace con una operación compensatoria o documento relacionado que no altera el cierre original. Todo queda auditado.
- **Reglas de negocio asociadas:** RN-073, RN-074, RN-006, RN-004.
- **Criterios de aceptación:**
  - **CA1 — Reapertura permitida**
    - Given una venta cerrada cuyo turno de origen sigue abierto
    - When un administrador autorizado la reabre
    - Then el sistema debe permitirlo y registrar el evento en la auditoría.
  - **CA2 — Reapertura impedida**
    - Given una venta cuyo turno de origen ya fue cerrado
    - When se intenta reabrirla
    - Then el sistema debe impedirlo.
  - **CA3 — Operación compensatoria**
    - Given una venta de un turno cerrado que requiere corrección
    - When el administrador registra la corrección
    - Then el sistema debe generar una operación compensatoria o documento relacionado
    - And no debe alterar el cierre original.
  - **CA4 — Permiso específico**
    - Given un usuario sin el permiso de reabrir
    - When intenta reabrir una venta
    - Then el sistema debe impedirlo.
- **Dependencias:** US-033, US-036, US-004, US-006.
- **Datos o entidades involucradas:** Pedido, TurnoCaja, CierreCaja, documento relacionado, registro de auditoría.
- **Riesgos, supuestos o pendientes:** el tratamiento de notas y documentos relacionados debe validarse con el contador — ver RF-014.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Caja; `01-alcance-y-reglas.md` § Reglas de historial; `06-modelo-de-datos.md` § Reglas de integridad esenciales; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (17).
- **Estimación inicial:** M

---

### [US-036] Cerrar el turno de caja con conciliación y alertas

- **Épica/Módulo:** EP-06 — Caja y facturación
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Cajero
- **Historia de usuario:**
  - Como cajero,
  - quiero cerrar mi turno conciliando lo esperado con lo contado,
  - para dejar registrada cualquier diferencia y cerrar de forma definitiva.
- **Descripción funcional:** Cierre del turno con conciliación de fondo inicial, cobros, abonos en efectivo y medios digitales/tarjeta. El cajero puede ver el efectivo esperado antes de contar. Cualquier diferencia desde S/0.01 genera alerta sin bloquear el cierre. El cierre conserva esperado, contado, diferencia y observación, y es inalterable.
- **Reglas de negocio asociadas:** RN-067, RN-068, RN-070, RN-006, RN-004.
- **Criterios de aceptación:**
  - **CA1 — Efectivo esperado visible**
    - Given un turno abierto
    - When el cajero inicia el cierre
    - Then el sistema debe mostrarle el efectivo esperado antes de registrar el conteo.
  - **CA2 — Conciliación completa**
    - Given el cierre en curso
    - When se calcula el resultado
    - Then el sistema debe conciliar fondo inicial, cobros, abonos en efectivo y medios digitales/tarjeta.
  - **CA3 — Alerta desde un céntimo**
    - Given una diferencia de S/0.01 entre esperado y contado
    - When el cajero confirma el conteo
    - Then el sistema debe mostrar una alerta
    - And debe permitir completar el cierre.
  - **CA4 — Contenido y cierre inalterable**
    - Given un cierre confirmado
    - When se consulta después
    - Then el sistema debe conservar esperado, contado, diferencia y observación
    - And no debe permitir su modificación.
  - **CA5 — Auditoría del cierre**
    - Given un cierre confirmado
    - When se consulta la auditoría
    - Then el sistema debe registrar usuario, fecha, hora y local.
- **Dependencias:** US-032, US-033, US-021.
- **Datos o entidades involucradas:** TurnoCaja, MovimientoCaja, CierreCaja (esperado, contado, diferencia, observación).
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Caja; `01-alcance-y-reglas.md` §§ Auditoría transversal, Reglas de historial; `06-modelo-de-datos.md` § Caja; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (18).
- **Estimación inicial:** L

---

### [US-037] Emitir comprobantes electrónicos por proveedor API

- **Épica/Módulo:** EP-06 — Caja y facturación
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Cajero (emisión) / Administrador (consulta)
- **Historia de usuario:**
  - Como cajero,
  - quiero que el sistema emita el comprobante electrónico al cerrar la venta y lo reintente si no hay Internet,
  - para cumplir con la facturación sin detener la atención ni duplicar documentos.
- **Descripción funcional:** Integración con un proveedor externo de facturación por API para enviar boletas, facturas y notas, guardando respuesta, estado y errores. Incluye cola offline, reintentos idempotentes, prevención de duplicados y consulta/reimpresión de la representación del comprobante. Para pensionistas se emite un comprobante por cada consumo, a su nombre y DNI.
- **Reglas de negocio asociadas:** RN-075, RN-035, RN-088, RN-087.
- **Criterios de aceptación:**
  - **CA1 — Emisión al cerrar**
    - Given una venta cerrada en caja
    - When se confirma el cierre
    - Then el sistema debe generar y enviar el comprobante electrónico correspondiente.
  - **CA2 — Comprobante del pensionista**
    - Given un consumo de pensionista cerrado en caja
    - When se emite el comprobante
    - Then debe emitirse una boleta a nombre del pensionista y con su DNI, una por cada consumo.
  - **CA3 — Cola offline y reintentos**
    - Given el proveedor de facturación no disponible o sin Internet
    - When se cierra una venta
    - Then el sistema debe encolar el comprobante y reintentar el envío de forma idempotente al restablecerse la conexión.
  - **CA4 — Prevención de duplicados**
    - Given un comprobante ya aceptado por el proveedor
    - When el reintento se ejecuta de nuevo
    - Then el sistema no debe generar un comprobante duplicado.
  - **CA5 — Registro de respuesta**
    - Given una respuesta del proveedor
    - When se recibe
    - Then el sistema debe guardar respuesta, estado y errores asociados al comprobante.
  - **CA6 — Consulta y reimpresión**
    - Given un comprobante emitido
    - When un usuario autorizado lo consulta
    - Then el sistema debe permitir ver y reimprimir su representación.
  - **CA7 — El abono no genera venta**
    - Given un abono registrado
    - When se procesa
    - Then el sistema no debe emitir un comprobante de venta por el abono
    - And puede generar una constancia interna.
- **Dependencias:** US-033, US-018, US-059 (idempotencia de sincronización).
- **Datos o entidades involucradas:** ComprobanteElectronico (venta, respuestas del proveedor, estado, errores), Pedido, Pensionista, cola de envío.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — datos fiscales y proveedor de facturación API (`07` § Pendientes funcionales); endpoints, estados, contingencia e idempotencia deben validarse con el proveedor (`07` § Validaciones externas). Ver RF-004 y RF-015. Esta historia no puede completarse sin esas definiciones.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Facturación electrónica; `02-pensionistas.md` § Facturación; `05-usuarios-reportes-multilocal-offline.md` § Offline-first; `06-modelo-de-datos.md` § Venta; `07-criterios-de-aceptacion-y-pendientes.md` §§ Casos críticos (16), Validaciones externas.
- **Estimación inicial:** XL

---

