<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 1786-1986. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

### EP-07 — Delivery propio y recojo programado

---

### [US-038] Registrar pedidos de recojo programado

- **Épica/Módulo:** EP-07 — Delivery y recojo
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Cajero / Mozo
- **Historia de usuario:**
  - Como cajero,
  - quiero registrar manualmente en el POS un pedido de recojo con fecha y hora futura,
  - para preparar el pedido a tiempo y cobrarlo según el tipo de cliente.
- **Descripción funcional:** Registro manual en POS de pedidos recibidos por WhatsApp, guardando fecha y hora futura, nombre y celular de quien recoge. El cliente regular paga por anticipado antes de confirmar; al pensionista se le carga al cerrar el pedido. Se añade táper por cada menú. La cancelación es sin cobro si se hace antes de enviar la comanda.
- **Reglas de negocio asociadas:** RN-076, RN-077, RN-078, RN-009, RN-015.
- **Criterios de aceptación:**
  - **CA1 — Datos del recojo**
    - Given un pedido de recojo
    - When se registra en el POS
    - Then el sistema debe guardar fecha y hora futura, nombre y celular de quien recoge.
  - **CA2 — Pago anticipado del cliente regular**
    - Given un pedido de recojo de un cliente regular
    - When se intenta confirmar sin pago
    - Then el sistema debe impedir la confirmación hasta registrar el pago anticipado.
  - **CA3 — Cargo del pensionista al cierre**
    - Given un pedido de recojo de un pensionista
    - When se cierra el pedido
    - Then el sistema debe generar el cargo en su cuenta corriente en ese momento y no antes.
  - **CA4 — Táper por menú**
    - Given un pedido de recojo con uno o más menús
    - When se calcula el total
    - Then el sistema debe añadir un cargo de táper por cada menú.
  - **CA5 — Cancelación sin cobro**
    - Given un pedido de recojo aún no comandado
    - When se cancela
    - Then el sistema no debe generar cobro.
- **Dependencias:** US-028, US-033, US-018.
- **Datos o entidades involucradas:** PedidoRecojo (fecha/hora, persona que recoge, celular, estado de preparación/retiro), Pedido, Pago, CuentaPensionista.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — el precio del táper (`07`). La integración automática con WhatsApp está excluida: se confirma que el registro es manual — ver RF-026.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Recojo programado; `01-alcance-y-reglas.md` § Canales; `06-modelo-de-datos.md` § Delivery y recojo; `README.md` § Exclusiones confirmadas.
- **Estimación inicial:** L

---

### [US-039] Gestionar el recojo preparado y no retirado

- **Épica/Módulo:** EP-07 — Delivery y recojo
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Cajero / Administrador de local
- **Historia de usuario:**
  - Como cajero,
  - quiero registrar cuando un pedido preparado no fue recogido,
  - para que el inventario refleje la merma y el cobro se aplique según lo definido para cada tipo de cliente.
- **Descripción funcional:** Marcado de un pedido de recojo ya preparado como no recogido, con descuento de inventario. Al cliente regular no se le devuelve el pago; al pensionista se le carga el pedido completo y se emite el comprobante.
- **Reglas de negocio asociadas:** RN-079, RN-035, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Registro de no recogido**
    - Given un pedido de recojo ya preparado
    - When pasa el momento del retiro y no se recoge
    - Then el sistema debe permitir registrarlo como no recogido
    - And debe descontar el inventario correspondiente.
  - **CA2 — Cliente regular**
    - Given un pedido no recogido de un cliente regular con pago anticipado
    - When se registra como no recogido
    - Then el sistema no debe devolver el pago.
  - **CA3 — Pensionista**
    - Given un pedido no recogido de un pensionista
    - When se registra como no recogido
    - Then el sistema debe cargar el pedido completo a su cuenta
    - And debe emitir el comprobante correspondiente.
- **Dependencias:** US-038, US-043, US-037, US-018.
- **Datos o entidades involucradas:** PedidoRecojo (estado de retiro), MovimientoInventario, MovimientoCuenta, ComprobanteElectronico.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Recojo programado; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (13, 14).
- **Estimación inicial:** M

---

### [US-040] Registrar el pedido de delivery y asignar motorizado

- **Épica/Módulo:** EP-07 — Delivery y recojo
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Cajero / Administrador de local
- **Historia de usuario:**
  - Como cajero,
  - quiero registrar manualmente el pedido de delivery, aceptar la dirección y asignar un motorizado,
  - para despachar el pedido y poder seguir su estado.
- **Descripción funcional:** Registro manual del pedido recibido por WhatsApp, con dirección y teléfono. El delivery es propio, inicialmente gratuito y sin zonas: caja decide si acepta la dirección. Administración o caja asigna el motorizado y controla los estados del pedido.
- **Reglas de negocio asociadas:** RN-080, RN-081, RN-009, RN-002.
- **Criterios de aceptación:**
  - **CA1 — Registro manual con dirección**
    - Given un pedido de delivery recibido por WhatsApp
    - When el cajero lo registra en el POS
    - Then el sistema debe guardar cliente, dirección y teléfono.
  - **CA2 — Aceptación de la dirección**
    - Given una dirección ingresada
    - When el cajero decide si la acepta
    - Then el sistema debe permitir aceptarla o rechazarla sin aplicar zonas ni cobro de envío.
  - **CA3 — Asignación de motorizado**
    - Given un pedido de delivery en estado pendiente
    - When administración o caja asigna un motorizado
    - Then el estado debe pasar a asignado y el pedido debe quedar visible para ese motorizado.
  - **CA4 — Táper por menú**
    - Given un pedido de delivery con uno o más menús
    - When se calcula el total
    - Then el sistema debe añadir un cargo de táper por cada menú.
  - **CA5 — Sin envío tarifado**
    - Given un pedido de delivery
    - When se calcula el total
    - Then el sistema no debe añadir un costo de envío.
- **Dependencias:** US-028, US-005 (usuarios motorizados), US-001.
- **Datos o entidades involucradas:** PedidoDelivery (cliente, dirección, motorizado, estados con timestamps), HistorialDelivery, Pedido.
- **Riesgos, supuestos o pendientes:** no se documenta si el cliente regular de delivery paga por anticipado o contra entrega (a diferencia del recojo, donde sí se exige anticipado) — ver RF-020. La ficha del cliente regular no está documentada — ver RF-017.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Delivery propio; `01-alcance-y-reglas.md` § Canales; `06-modelo-de-datos.md` § Delivery y recojo.
- **Estimación inicial:** L

---

### [US-041] Operar la vista móvil del motorizado

- **Épica/Módulo:** EP-07 — Delivery y recojo
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Motorizado
- **Historia de usuario:**
  - Como motorizado,
  - quiero ver mis pedidos asignados con su dirección y teléfono y cambiar su estado,
  - para entregar correctamente y que administración sepa dónde está cada pedido.
- **Descripción funcional:** Vista web móvil para el motorizado con sus pedidos asignados, dirección y teléfono del cliente, y cambio de estados: aceptado, recogido, en ruta, entregado y fallido. Comparte la última ubicación mientras está en ruta. La entrega se confirma solo marcando "entregado".
- **Reglas de negocio asociadas:** RN-081, RN-082, RN-083, RN-088.
- **Criterios de aceptación:**
  - **CA1 — Listado de pedidos asignados**
    - Given un motorizado con pedidos asignados
    - When abre su vista móvil
    - Then el sistema debe mostrar sus pedidos con dirección y teléfono.
  - **CA2 — Transición de estados**
    - Given un pedido asignado
    - When el motorizado cambia su estado
    - Then el sistema debe registrar la transición entre pendiente, asignado, aceptado, recogido, en ruta, entregado y fallido con su marca de tiempo.
  - **CA3 — Confirmación de entrega**
    - Given un pedido en ruta
    - When el motorizado lo marca como entregado
    - Then el sistema debe considerar la entrega confirmada
    - And no debe considerarla confirmada por ningún otro estado.
  - **CA4 — Última ubicación**
    - Given un pedido en estado en ruta
    - When el motorizado se desplaza
    - Then el sistema debe compartir su última ubicación mientras dure ese estado.
  - **CA5 — Sin comisiones**
    - Given la liquidación del motorizado
    - When se calcula su pago
    - Then el sistema no debe calcular comisiones por entrega.
  - **CA6 — Ubicación indisponible**
    - Given una caída de conexión
    - When la ubicación remota no está disponible
    - Then la operación del local debe continuar sin verse afectada.
- **Dependencias:** US-040, US-003/US-002 (acceso del motorizado).
- **Datos o entidades involucradas:** PedidoDelivery, HistorialDelivery (cambios de estado, ubicación, incidencias), Usuario motorizado.
- **Riesgos, supuestos o pendientes:** la optimización automática de rutas está excluida (`README.md`).
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Delivery propio; `05-usuarios-reportes-multilocal-offline.md` § Offline-first; `06-modelo-de-datos.md` § Delivery y recojo; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (15).
- **Estimación inicial:** L

---

### [US-042] Liquidar al motorizado y cerrar la venta de delivery

- **Épica/Módulo:** EP-07 — Delivery y recojo
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Cajero
- **Historia de usuario:**
  - Como cajero,
  - quiero revisar y liquidar lo cobrado por cada motorizado antes de cerrar sus ventas,
  - para controlar el dinero entregado y registrar las incidencias.
- **Descripción funcional:** Revisión y liquidación de los pedidos entregados por motorizado: control del dinero cobrado, registro de incidencias y cierre de la venta en caja después de la liquidación.
- **Reglas de negocio asociadas:** RN-084, RN-081, RN-083.
- **Criterios de aceptación:**
  - **CA1 — Venta no cerrada antes de liquidar**
    - Given un pedido marcado como entregado
    - When aún no se ha liquidado con el motorizado
    - Then el sistema no debe cerrar la venta.
  - **CA2 — Liquidación**
    - Given los pedidos entregados de un motorizado
    - When el cajero revisa y liquida el dinero cobrado
    - Then el sistema debe registrar el importe liquidado por motorizado
    - And debe permitir cerrar las ventas correspondientes.
  - **CA3 — Incidencias**
    - Given un pedido con incidencia o estado fallido
    - When el cajero la registra
    - Then el sistema debe asociarla al motorizado y al pedido.
- **Dependencias:** US-041, US-033, US-036.
- **Datos o entidades involucradas:** PedidoDelivery, HistorialDelivery (incidencias), Pago, TurnoCaja, MovimientoCaja.
- **Riesgos, supuestos o pendientes:** ver RF-020 sobre el momento de pago del cliente regular en delivery.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Delivery propio; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (15).
- **Estimación inicial:** M

---

