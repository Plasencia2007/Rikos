<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 741-869. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

### EP-03 — Menú, programación semanal y disponibilidad

---

### [US-013] Programar el menú semanal del almuerzo

- **Épica/Módulo:** EP-03 — Menú y programación
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador central,
  - quiero programar una semana completa de almuerzos por día y servicio con sus opciones y porciones estimadas,
  - para que el POS sepa exactamente qué se ofrece cada día sin alterar el catálogo maestro.
- **Descripción funcional:** Creación de la semana de menú con sus días, y por cada día y servicio las opciones clasificadas por componente (entrada, segundo, guarnición, refresco, postre) con porciones estimadas. Estados: borrador, confirmado y en servicio. El menú confirmado sigue siendo editable sin modificar el catálogo maestro. El sistema advierte, sin bloquear, si un segundo o guiso se repite respecto de los tres días anteriores. El menú solo se publica dentro del POS.
- **Reglas de negocio asociadas:** RN-039, RN-040, RN-041, RN-042, RN-001.
- **Criterios de aceptación:**
  - **CA1 — Registro de opciones del día**
    - Given que la semana de menú se encuentra en estado borrador
    - When el usuario autorizado registra las opciones de entrada, segundo, guarnición, refresco y postre para un día y servicio
    - Then el sistema debe guardar la configuración asociada a ese día y servicio.
  - **CA2 — Porciones estimadas**
    - Given una opción de menú registrada
    - When el usuario indica las porciones estimadas
    - Then el sistema debe guardarlas junto con la opción.
  - **CA3 — Advertencia por repetición**
    - Given que se está programando un segundo o guiso para un día
    - When el producto seleccionado aparece en cualquiera de los tres días anteriores
    - Then el sistema debe mostrar una advertencia
    - And debe permitir confirmar la programación de todos modos.
  - **CA4 — Transición de estados**
    - Given una semana en borrador
    - When el usuario la confirma
    - Then el estado debe pasar a confirmado
    - And el menú debe quedar disponible para el POS.
  - **CA5 — Edición del menú confirmado**
    - Given una semana confirmada
    - When el usuario modifica una opción
    - Then el sistema debe aplicar el cambio al menú programado
    - And no debe modificar el catálogo maestro de productos.
  - **CA6 — Publicación solo interna**
    - Given un menú confirmado o en servicio
    - When se revisan los canales de publicación
    - Then el sistema debe exponerlo únicamente dentro del POS y no publicarlo de forma pública.
  - **CA7 — Desayuno y cena sin programación semanal**
    - Given los servicios de desayuno y cena
    - When se accede a la programación semanal
    - Then el sistema no debe exigir programarlos
    - And debe permitir elegir sus productos fijos individualmente en el pedido.
- **Dependencias:** US-008, US-014.
- **Datos o entidades involucradas:** SemanaMenu, DiaMenu, OpcionMenu (componente, servicio, porciones estimadas, estado, disponibilidad por local), Producto.
- **Riesgos, supuestos o pendientes:** la transición al estado "en servicio" no está documentada (manual o automática por horario) — ver RF-025.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` §§ Programación semanal, Almuerzo; `06-modelo-de-datos.md` § Menú y producción; `README.md` § Alcance confirmado.
- **Estimación inicial:** L

---

### [US-014] Configurar el menú normal y el menú extra

- **Épica/Módulo:** EP-03 — Menú y programación
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador central,
  - quiero definir la composición y el precio del menú normal y del menú extra,
  - para que el cobro sea consistente sin importar qué opciones elija el cliente.
- **Descripción funcional:** Definición del producto vendible "menú normal" (entrada + segundo con guarnición + refresco + postre) con precio único independiente de las opciones elegidas, y del "menú extra", que es un menú completo cuyo segundo es especial y tiene precio propio.
- **Reglas de negocio asociadas:** RN-037, RN-023, RN-001.
- **Criterios de aceptación:**
  - **CA1 — Composición del menú normal**
    - Given la configuración del menú normal
    - When se revisa su composición
    - Then el sistema debe considerar entrada, segundo con guarnición, refresco y postre.
  - **CA2 — Precio único**
    - Given un menú normal vendido
    - When el cliente elige cualquier combinación de opciones programadas
    - Then el sistema debe cobrar el mismo precio único.
  - **CA3 — Menú extra con precio propio**
    - Given un menú extra
    - When se vende
    - Then el sistema debe cobrar su precio propio completo
    - And no debe aplicar la tarifa reducida del plan del pensionista.
- **Dependencias:** US-008, US-010.
- **Datos o entidades involucradas:** Producto vendible (menú normal, menú extra), componentes, precio.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — los precios definitivos no han sido proporcionados y los importes de los documentos son ejemplos (`README.md` § Convenciones, `07` § Pendientes funcionales). Ver RF-002.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Almuerzo; `02-pensionistas.md` § Planes tarifarios; `README.md` § Convenciones.
- **Estimación inicial:** M

---

### [US-015] Marcar productos agotados y reactivarlos

- **Épica/Módulo:** EP-03 — Menú y programación
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Cocina / Administrador de local
- **Historia de usuario:**
  - Como personal de cocina o administrador de local,
  - quiero marcar manualmente un producto como agotado y luego reactivarlo,
  - para que los mozos dejen de ofrecerlo sin afectar las comandas ya emitidas.
- **Descripción funcional:** Marcado manual de disponibilidad por local. Un producto agotado se oculta en pedidos nuevos. Las comandas ya emitidas no se modifican. La reactivación registra usuario, hora y motivo.
- **Reglas de negocio asociadas:** RN-066, RN-002, RN-004.
- **Criterios de aceptación:**
  - **CA1 — Marcado de agotado**
    - Given un producto disponible en un local
    - When cocina o administración lo marca como agotado
    - Then el sistema debe ocultarlo en los pedidos nuevos de ese local.
  - **CA2 — Comandas existentes intactas**
    - Given comandas ya emitidas que incluyen ese producto
    - When el producto se marca como agotado
    - Then el sistema no debe modificar ni cancelar esas comandas.
  - **CA3 — Reactivación auditada**
    - Given un producto marcado como agotado
    - When se reactiva
    - Then el sistema debe registrar usuario, hora y motivo
    - And debe volver a mostrarlo en los pedidos nuevos.
  - **CA4 — Alcance por local**
    - Given un producto agotado en un local
    - When se consulta la disponibilidad en otro local
    - Then el sistema debe mostrarlo disponible si allí no fue marcado.
- **Dependencias:** US-008, US-006, US-028 (POS que consume la disponibilidad).
- **Datos o entidades involucradas:** Producto, OpcionMenu (disponibilidad por local), Local, registro de auditoría.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `03-pos-caja-cocina-delivery.md` § Disponibilidad; `06-modelo-de-datos.md` § Menú y producción; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (8).
- **Estimación inicial:** S

---

