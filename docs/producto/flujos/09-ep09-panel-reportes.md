<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 2319-2536. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

### EP-09 — Panel de administración y reportes

---

### [US-052] Consultar el panel del administrador con filtros

- **Épica/Módulo:** EP-09 — Reportes
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Administrador central / Administrador de local
- **Historia de usuario:**
  - Como administrador,
  - quiero un panel que priorice ventas y rentabilidad, caja, pensionistas e inventario con filtros,
  - para revisar el estado del negocio sin abrir un reporte por cada pregunta.
- **Descripción funcional:** Panel con las cuatro prioridades definidas (ventas y rentabilidad; caja y diferencias; pensionistas y saldos; inventario y costos) y filtros por día, semana, mes, turno, local, canal, producto, plan y tipo de cliente.
- **Reglas de negocio asociadas:** RN-012, RN-085, RN-002.
- **Criterios de aceptación:**
  - **CA1 — Prioridades del panel**
    - Given un administrador con permisos
    - When abre el panel
    - Then el sistema debe mostrar ventas y rentabilidad, caja y diferencias, pensionistas y saldos, e inventario y costos.
  - **CA2 — Filtros disponibles**
    - Given el panel abierto
    - When se aplican filtros
    - Then el sistema debe permitir filtrar por día, semana, mes, turno, local, canal, producto, plan y tipo de cliente.
  - **CA3 — Restricción por permisos y locales**
    - Given un usuario sin permiso de ver costos o asignado a un solo local
    - When abre el panel
    - Then el sistema no debe mostrarle costos ni datos de locales no asignados.
- **Dependencias:** US-053, US-054, US-055, US-056, US-057.
- **Datos o entidades involucradas:** agregados de Pedido, Pago, TurnoCaja, CuentaPensionista, Existencia, MovimientoInventario.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Panel del administrador.
- **Estimación inicial:** L

---

### [US-053] Reportar ventas, costo histórico, utilidad y margen

- **Épica/Módulo:** EP-09 — Reportes
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Administrador central / Administrador de local
- **Historia de usuario:**
  - Como administrador,
  - quiero reportes de ventas por local, canal, servicio, producto y usuario con su rentabilidad,
  - para saber qué se vende y cuánto deja realmente.
- **Descripción funcional:** Reportes de ventas segmentables por local, canal, servicio, producto y usuario, mostrando precio, costo histórico, utilidad y margen a partir del snapshot guardado en cada venta. Exportables a Excel y PDF.
- **Reglas de negocio asociadas:** RN-005, RN-046, RN-085, RN-012.
- **Criterios de aceptación:**
  - **CA1 — Segmentación**
    - Given ventas registradas
    - When se consulta el reporte
    - Then el sistema debe permitir agrupar por local, canal, servicio, producto y usuario.
  - **CA2 — Rentabilidad con costo histórico**
    - Given una venta cerrada
    - When se calcula su utilidad y margen en el reporte
    - Then el sistema debe usar el costo histórico guardado en la venta y no el costo actual.
  - **CA3 — Exportación**
    - Given un reporte generado
    - When el usuario con permiso de exportar lo descarga
    - Then el sistema debe entregarlo en Excel y en PDF.
  - **CA4 — Permiso de costos**
    - Given un usuario sin permiso de ver costos
    - When abre el reporte
    - Then el sistema no debe mostrar costo, utilidad ni margen.
- **Dependencias:** US-033, US-048, US-010, US-004.
- **Datos o entidades involucradas:** Pedido, DetallePedido (snapshot de costo/receta), Pago, Producto, Usuario, Local, Canal.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` §§ Reportes mínimos, Panel del administrador; `04-menu-recetas-inventario-compras.md` § Recetas y costos; `01-alcance-y-reglas.md` § Reglas de historial.
- **Estimación inicial:** L

---

### [US-054] Reportar cierres y diferencias de caja

- **Épica/Módulo:** EP-09 — Reportes
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Administrador
- **Historia de usuario:**
  - Como administrador,
  - quiero un reporte de cierres de caja con sus diferencias,
  - para controlar el manejo de efectivo por turno, caja y cajero.
- **Descripción funcional:** Reporte de cierres de caja con esperado, contado, diferencia y observación, filtrable por local, turno y cajero, exportable a Excel y PDF.
- **Reglas de negocio asociadas:** RN-068, RN-070, RN-085.
- **Criterios de aceptación:**
  - **CA1 — Contenido**
    - Given cierres de caja realizados
    - When se consulta el reporte
    - Then debe mostrar esperado, contado, diferencia y observación por turno, caja, cajero y local.
  - **CA2 — Diferencias visibles**
    - Given un cierre con diferencia distinta de cero
    - When se consulta el reporte
    - Then el sistema debe destacar la diferencia.
  - **CA3 — Exportación**
    - Given el reporte generado
    - When se exporta
    - Then debe estar disponible en Excel y PDF.
- **Dependencias:** US-036.
- **Datos o entidades involucradas:** CierreCaja, TurnoCaja, MovimientoCaja, Usuario, Local.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos; `03-pos-caja-cocina-delivery.md` § Caja.
- **Estimación inicial:** M

---

### [US-055] Reportar pensionistas, saldos, estados y abonos

- **Épica/Módulo:** EP-09 — Reportes
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Administrador
- **Historia de usuario:**
  - Como administrador,
  - quiero reportes de deuda, saldo a favor, límite, crédito disponible, estados mensuales y abonos por estado,
  - para gestionar la cobranza de los pensionistas.
- **Descripción funcional:** Reportes de la cartera de pensionistas: deuda, saldo a favor, límite y crédito disponible; estados de cuenta mensuales; y abonos pendientes, aprobados y rechazados. Exportables a Excel y PDF, conservando el PDF fijo de cada estado mensual cerrado.
- **Reglas de negocio asociadas:** RN-016, RN-085, RN-031, RN-003.
- **Criterios de aceptación:**
  - **CA1 — Cartera de pensionistas**
    - Given pensionistas con movimientos
    - When se consulta el reporte
    - Then debe mostrar deuda, saldo a favor, límite y crédito disponible por pensionista.
  - **CA2 — Abonos por estado**
    - Given abonos registrados
    - When se consulta el reporte
    - Then debe permitir filtrar por pendientes, aprobados y rechazados.
  - **CA3 — Estados mensuales**
    - Given estados mensuales cerrados
    - When se consultan
    - Then el sistema debe entregar el PDF fijo conservado en el cierre.
  - **CA4 — Movimientos multilocal**
    - Given un pensionista con consumos en varios locales
    - When se consulta su detalle
    - Then el sistema debe mostrar el local de cada movimiento.
- **Dependencias:** US-018, US-024, US-025.
- **Datos o entidades involucradas:** CuentaPensionista, MovimientoCuenta, EstadoCuentaMensual, Abono, PlanTarifario.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos; `02-pensionistas.md` §§ Cuenta corriente, Abonos; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (20).
- **Estimación inicial:** M

---

### [US-056] Reportar inventario, Kardex y producción

- **Épica/Módulo:** EP-09 — Reportes
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Administrador
- **Historia de usuario:**
  - Como administrador,
  - quiero reportes de inventario, Kardex, stock negativo, stock mínimo, ajustes y producción,
  - para controlar existencias y detectar desviaciones entre lo planificado y lo vendido.
- **Descripción funcional:** Reportes de existencias, Kardex, alertas de stock negativo y de stock mínimo, ajustes realizados, y producción con porciones planificadas, preparadas, vendidas y sobrantes. Exportables a Excel y PDF.
- **Reglas de negocio asociadas:** RN-050, RN-051, RN-052, RN-048, RN-085.
- **Criterios de aceptación:**
  - **CA1 — Inventario y Kardex**
    - Given movimientos registrados
    - When se consulta el reporte
    - Then debe mostrar existencias por local y el Kardex con origen de cada movimiento.
  - **CA2 — Alertas**
    - Given productos en stock negativo o bajo el mínimo
    - When se consulta el reporte
    - Then el sistema debe listarlos identificando la condición.
  - **CA3 — Producción**
    - Given producciones registradas
    - When se consulta el reporte
    - Then debe mostrar porciones planificadas, preparadas, vendidas y sobrantes.
  - **CA4 — Exportación**
    - Given cualquiera de estos reportes
    - When se exporta
    - Then debe estar disponible en Excel y PDF.
- **Dependencias:** US-044, US-045, US-048, US-049.
- **Datos o entidades involucradas:** Existencia, MovimientoInventario, Ajuste, Produccion, Producto, Local.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos; `04-menu-recetas-inventario-compras.md` §§ Inventario y Kardex, Producción.
- **Estimación inicial:** M

---

### [US-057] Reportar delivery, anulaciones y consumo interno

- **Épica/Módulo:** EP-09 — Reportes
- **Prioridad:** Could
- **Release sugerido:** Post-MVP
- **Actor:** Administrador
- **Historia de usuario:**
  - Como administrador,
  - quiero reportes de delivery e incidencias, de productos anulados después de comanda y de consumo interno,
  - para controlar las pérdidas y el desempeño de la operación no presencial.
- **Descripción funcional:** Reportes operativos: delivery, entregas, incidencias y liquidación de motorizados; productos anulados después de comanda; consumos internos. Exportables a Excel y PDF.
- **Reglas de negocio asociadas:** RN-084, RN-065, RN-056, RN-085.
- **Criterios de aceptación:**
  - **CA1 — Delivery**
    - Given pedidos de delivery registrados
    - When se consulta el reporte
    - Then debe mostrar entregas, incidencias y liquidación por motorizado.
  - **CA2 — Anulaciones después de comanda**
    - Given productos anulados después de comandar
    - When se consulta el reporte
    - Then debe listarlos con producto, motivo, usuario, fecha y local.
  - **CA3 — Consumo interno**
    - Given consumos internos registrados
    - When se consulta el reporte
    - Then debe mostrarlos separados de las ventas con trabajador, platos, turno y costo.
  - **CA4 — Exportación**
    - Given cualquiera de estos reportes
    - When se exporta
    - Then debe estar disponible en Excel y PDF.
- **Dependencias:** US-031, US-042, US-051.
- **Datos o entidades involucradas:** PedidoDelivery, HistorialDelivery, DetallePedido anulado, consumo interno.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos; `03-pos-caja-cocina-delivery.md` §§ Comandas, Delivery propio; `04-menu-recetas-inventario-compras.md` § Consumo interno.
- **Estimación inicial:** M

---

