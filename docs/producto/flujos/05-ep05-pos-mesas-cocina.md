<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 1332-1537. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

### EP-05 — POS, mesas y cocina

---

### [US-027] Visualizar y operar el mapa de mesas

- **Épica/Módulo:** EP-05 — POS y cocina
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Mozo / Cajero / Administrador de local
- **Historia de usuario:**
  - Como mozo,
  - quiero ver el mapa de mesas de mi local con su estado actual,
  - para saber de un vistazo dónde atender y qué mesas están pendientes de cierre.
- **Descripción funcional:** Mapa de mesas por local, accesible desde una PWA responsive para celular, tablet y terminal fija, con los estados libre, ocupada, unida, pendiente de cierre y bloqueada.
- **Reglas de negocio asociadas:** RN-058, RN-059, RN-002.
- **Criterios de aceptación:**
  - **CA1 — Estados visibles**
    - Given un local con mesas configuradas
    - When el mozo abre el mapa de mesas
    - Then el sistema debe mostrar cada mesa en uno de los estados libre, ocupada, unida, pendiente de cierre o bloqueada.
  - **CA2 — Alcance por local**
    - Given un usuario asignado a un local
    - When abre el mapa de mesas
    - Then el sistema debe mostrar únicamente las mesas de ese local.
  - **CA3 — Responsive**
    - Given el POS abierto en celular, tablet o terminal fija
    - When se usa el mapa de mesas
    - Then la interfaz debe ser operable en los tres formatos.
- **Dependencias:** US-001, US-004, US-002/US-003.
- **Datos o entidades involucradas:** Mesa, Local, Pedido.
- **Riesgos, supuestos o pendientes:** la regla de negocio para el estado "bloqueada" (quién y cuándo bloquea una mesa) no está documentada — ver RF-028. La cantidad de mesas por local está pendiente (`07`) — ver RF-010.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § POS y mesas; `06-modelo-de-datos.md` § Organización; `README.md` § Alcance confirmado.
- **Estimación inicial:** M

---

### [US-028] Tomar el pedido asignando productos por persona

- **Épica/Módulo:** EP-05 — POS y cocina
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Mozo
- **Historia de usuario:**
  - Como mozo,
  - quiero registrar el pedido asignando cada producto a una persona de la mesa,
  - para poder cobrar correctamente mesas mixtas con clientes regulares y varios pensionistas.
- **Descripción funcional:** Toma de pedido sobre una mesa o canal, creando cuentas por persona y su detalle. Permite mesas mixtas con clientes regulares y pensionistas. Muestra solo productos disponibles y las opciones programadas del día. Para llevar, recojo y delivery añade el cargo de táper por cada menú. Registra observaciones y tipo de entrega.
- **Reglas de negocio asociadas:** RN-060, RN-038, RN-009, RN-066, RN-042, RN-058, RN-008.
- **Criterios de aceptación:**
  - **CA1 — Asignación por persona**
    - Given una mesa con varios comensales
    - When el mozo registra los productos
    - Then el sistema debe permitir asignar cada producto a una persona identificada de la mesa.
  - **CA2 — Mesa mixta**
    - Given una mesa con clientes regulares y uno o más pensionistas
    - When se toma el pedido
    - Then el sistema debe separar correctamente los consumos de cada uno desde la toma del pedido.
  - **CA3 — Registro de la opción elegida**
    - Given un componente del menú con varias opciones programadas
    - When el mozo informa las opciones y registra la elegida
    - Then el sistema debe guardarla en el detalle
    - And cuando solo exista una opción debe asignarla directamente.
  - **CA4 — Producto agotado oculto**
    - Given un producto marcado como agotado en el local
    - When el mozo abre la lista de productos para un pedido nuevo
    - Then el sistema no debe ofrecerlo.
  - **CA5 — Cargo de táper**
    - Given un pedido en canal para llevar, recojo programado o delivery
    - When se registra un menú
    - Then el sistema debe añadir un cargo de táper por cada menú.
  - **CA6 — Observaciones y tipo de entrega**
    - Given un producto en el pedido
    - When el mozo agrega una observación y define el tipo de entrega
    - Then el sistema debe conservarlas para la comanda.
- **Dependencias:** US-027, US-013, US-014, US-015, US-016, US-008.
- **Datos o entidades involucradas:** Pedido (local, canal, turno, usuario), CuentaPersona, DetallePedido (producto, componentes seleccionados, precio, snapshot de costo/receta), Mesa, Pensionista, Cliente regular.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — el precio del táper por menú (`07`). No existe ficha documentada del cliente regular (qué datos se registran) — ver RF-017. Las reglas de cortesía y de productos compartidos están pendientes (`07`) — ver RF-008.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § POS y mesas; `04-menu-recetas-inventario-compras.md` § Almuerzo; `01-alcance-y-reglas.md` § Canales; `06-modelo-de-datos.md` § Venta; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (7).
- **Estimación inicial:** XL

---

### [US-029] Unir, mover, dividir y trasladar cuentas y productos

- **Épica/Módulo:** EP-05 — POS y cocina
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Mozo / Cajero
- **Historia de usuario:**
  - Como mozo,
  - quiero mover pedidos, unir mesas, dividir cuentas por productos y trasladar productos entre cuentas,
  - para adaptarme a cómo se reacomodan los comensales sin perder el detalle del consumo.
- **Descripción funcional:** Operaciones de reorganización sobre pedidos y cuentas: mover un pedido a otra mesa, unir mesas, dividir la cuenta por productos y trasladar productos entre cuentas de personas, manteniendo la trazabilidad del detalle y del snapshot económico.
- **Reglas de negocio asociadas:** RN-059, RN-060, RN-005.
- **Criterios de aceptación:**
  - **CA1 — Mover pedido**
    - Given un pedido asociado a una mesa
    - When el mozo lo mueve a otra mesa libre
    - Then el sistema debe reasignar el pedido y actualizar los estados de ambas mesas.
  - **CA2 — Unir mesas**
    - Given una mesa principal (libre o con pedido) y otra mesa libre sin pedido
    - When el mozo une la libre a la principal
    - Then el sistema debe marcarlas como unidas y permitir operar sus consumos en conjunto en el pedido de la principal.
    - And una mesa con pedido en curso no puede unirse como secundaria. <!-- Enmendado 2026-10-05, ver prd/05-enmiendas-al-backlog-v3.md -->
  - **CA3 — Dividir por productos**
    - Given una cuenta con varios productos
    - When el mozo la divide por productos
    - Then el sistema debe generar las cuentas resultantes conservando el detalle y el precio de cada producto.
  - **CA4 — Trasladar producto entre cuentas**
    - Given un producto asignado a una persona
    - When se traslada a la cuenta de otra persona
    - Then el sistema debe reflejar el cambio en ambas cuentas antes del cierre.
  - **CA5 — Consumo de pensionista**
    - Given un producto vinculado a la cuenta de un pensionista
    - When se traslada a otra persona
    - Then el sistema debe recalcular a qué cuenta corresponde el cargo al momento del cierre.
- **Dependencias:** US-028, US-027.
- **Datos o entidades involucradas:** Pedido, CuentaPersona, DetallePedido, Mesa.
- **Riesgos, supuestos o pendientes:** las reglas de productos compartidos no están definidas (`07`) — ver RF-008. **Supuesto de análisis** en CA5: la especificación no detalla el recálculo del cargo al trasladar productos entre un pensionista y un cliente regular; debe confirmarse.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § POS y mesas; `06-modelo-de-datos.md` § Venta; `01-alcance-y-reglas.md` § Reglas de historial.
- **Estimación inicial:** L

---

### [US-030] Emitir la comanda a la impresora central de cocina

- **Épica/Módulo:** EP-05 — POS y cocina
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Mozo / Cocina
- **Historia de usuario:**
  - Como mozo,
  - quiero enviar la comanda a la impresora central de cocina con todo el detalle,
  - para que la cocina prepare exactamente lo pedido por cada persona.
- **Descripción funcional:** Impresión de la comanda en la única impresora central de cocina con pedido/mesa/canal, mozo, hora, productos por persona, componentes, observaciones y tipo de entrega. Toda reimpresión se marca como copia. La cocina avisa el pedido listo de forma verbal o por campana, sin cambio digital de estado. La emisión debe funcionar sin Internet.
- **Reglas de negocio asociadas:** RN-062, RN-063, RN-064, RN-086.
- **Criterios de aceptación:**
  - **CA1 — Contenido de la comanda**
    - Given un pedido registrado
    - When el mozo lo comanda
    - Then la impresión debe incluir pedido/mesa/canal, mozo, hora, productos por persona, componentes, observaciones y tipo de entrega.
  - **CA2 — Impresora única**
    - Given el local en operación
    - When se emiten comandas de cualquier canal
    - Then todas deben dirigirse a la única impresora central de cocina.
  - **CA3 — Reimpresión marcada**
    - Given una comanda ya impresa
    - When se reimprime
    - Then el sistema debe marcar la impresión como copia
    - And debe registrar la reimpresión.
  - **CA4 — Sin estado digital de listo**
    - Given una comanda impresa
    - When cocina termina la preparación
    - Then el sistema no debe exigir un cambio digital de estado.
  - **CA5 — Impresión sin Internet**
    - Given un local sin conexión a Internet
    - When el mozo comanda un pedido
    - Then el sistema debe imprimir la comanda normalmente.
- **Dependencias:** US-028, US-058.
- **Datos o entidades involucradas:** Comanda, DetalleComanda, registro de impresiones, Pedido, CuentaPersona.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — el modelo de la impresora central (`07`); la impresión debe probarse con el hardware real (`07` § Validaciones externas). Ver RF-012 y RF-016.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Comandas; `06-modelo-de-datos.md` § Venta; `05-usuarios-reportes-multilocal-offline.md` § Offline-first.
- **Estimación inicial:** L

---

### [US-031] Anular productos antes y después de comandar

- **Épica/Módulo:** EP-05 — POS y cocina
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Mozo / Cajero con permiso de anular
- **Historia de usuario:**
  - Como usuario autorizado,
  - quiero anular un producto del pedido indicando el motivo,
  - para no cobrarlo al cliente y que el inventario refleje si el insumo ya se consumió.
- **Descripción funcional:** Anulación de productos del pedido con efecto diferenciado en inventario: antes de comandar no descuenta; después de comandar no se cobra pero descuenta la receta como consumo o pérdida. Requiere permiso específico y motivo, y queda auditada y reportada.
- **Reglas de negocio asociadas:** RN-065, RN-072, RN-004, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Anulación antes de comandar**
    - Given un producto aún no comandado
    - When se anula
    - Then el sistema no debe cobrarlo
    - And no debe descontar inventario.
  - **CA2 — Anulación después de comandar**
    - Given un producto ya comandado
    - When se anula
    - Then el sistema no debe cobrarlo al cliente
    - And debe descontar la receta del inventario como consumo o pérdida.
  - **CA3 — Permiso y motivo**
    - Given un usuario sin permiso de anular
    - When intenta anular un producto
    - Then el sistema debe impedirlo
    - And cuando el usuario sí tiene permiso, debe exigir el motivo antes de confirmar.
  - **CA4 — Auditoría y reporte**
    - Given una anulación confirmada
    - When se consulta la auditoría o el reporte de productos anulados después de comanda
    - Then el sistema debe mostrar usuario, fecha, hora, local, motivo y el producto anulado.
- **Dependencias:** US-030, US-043/US-044 (inventario), US-012 (motivos), US-004, US-006.
- **Datos o entidades involucradas:** DetallePedido, Comanda, MovimientoInventario (origen venta/ajuste), motivo de anulación, registro de auditoría.
- **Riesgos, supuestos o pendientes:** los motivos iniciales de anulación están pendientes — ver RF-005.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` §§ Comandas, Caja; `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (9).
- **Estimación inicial:** M

---

