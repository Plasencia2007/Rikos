<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 554-740. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

### EP-02 — Catálogos, datos maestros y costeo

---

### [US-008] Administrar el catálogo central de productos

- **Épica/Módulo:** EP-02 — Catálogos y datos maestros
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador central,
  - quiero mantener un catálogo único de productos clasificados por tipo,
  - para que todos los locales vendan, produzcan y compren sobre la misma base de datos maestra.
- **Descripción funcional:** Alta, edición y desactivación de productos con su clasificación: entradas, segundos/guisos, guarniciones/menestras, refrescos, postres, extras, productos fijos de desayuno/cena, insumos y subrecetas. Un producto puede ser vendible, insumo, preparación o ambos. El catálogo es central y común a todos los locales.
- **Reglas de negocio asociadas:** RN-001, RN-007.
- **Criterios de aceptación:**
  - **CA1 — Clasificación obligatoria**
    - Given el alta de un producto
    - When el administrador lo guarda
    - Then el sistema debe exigir su categoría entre entradas, segundos/guisos, guarniciones/menestras, refrescos, postres, extras, productos fijos de desayuno/cena, insumos y subrecetas.
  - **CA2 — Naturaleza del producto**
    - Given un producto registrado
    - When se define su naturaleza
    - Then el sistema debe permitir marcarlo como vendible, insumo, preparación o combinación de estas.
  - **CA3 — Alcance central**
    - Given un producto creado o modificado
    - When se consulta desde cualquier local
    - Then el sistema debe mostrar la misma definición para todos los locales.
  - **CA4 — Desactivación**
    - Given un producto con movimientos históricos
    - When se desactiva
    - Then el sistema debe impedir su uso en nuevos registros y conservar su historial.
- **Dependencias:** US-001, US-004.
- **Datos o entidades involucradas:** Producto, Categoría.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Catálogo; `01-alcance-y-reglas.md` § Multilocal; `06-modelo-de-datos.md` § Catálogo y costos.
- **Estimación inicial:** M

---

### [US-009] Definir unidades base, presentaciones y conversiones

- **Épica/Módulo:** EP-02 — Catálogos y datos maestros
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador central,
  - quiero definir la unidad base de cada producto y sus presentaciones de compra con factor de conversión,
  - para que compras, recetas e inventario trabajen sobre una misma unidad comparable.
- **Descripción funcional:** Configuración de la unidad base por producto y de las presentaciones de compra con su equivalencia (cajas/unidades, sacos/kg, litros/ml y similares), usadas al registrar compras, recetas y movimientos de inventario.
- **Reglas de negocio asociadas:** RN-001.
- **Criterios de aceptación:**
  - **CA1 — Unidad base obligatoria**
    - Given el alta de un producto insumo
    - When se guarda
    - Then el sistema debe exigir una unidad base.
  - **CA2 — Presentación con conversión**
    - Given un producto con unidad base definida
    - When se registra una presentación de compra
    - Then el sistema debe exigir el factor de conversión a la unidad base.
  - **CA3 — Uso en compra**
    - Given una compra registrada en una presentación
    - When se confirma
    - Then el sistema debe convertir la cantidad a la unidad base para actualizar el stock.
- **Dependencias:** US-008.
- **Datos o entidades involucradas:** Producto, PresentaciónCompra, ConversiónUnidad.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `06-modelo-de-datos.md` § Catálogo y costos.
- **Estimación inicial:** M

---

### [US-010] Crear recetas y subrecetas valorizadas por porción

- **Épica/Módulo:** EP-02 — Catálogos y datos maestros
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador central,
  - quiero registrar la receta valorizada de cada componente, incluyendo subrecetas,
  - para conocer el costo por porción y sostener el control de costos y márgenes.
- **Descripción funcional:** Definición de recetas de productos vendibles y de subrecetas, con insumos y/o subrecetas, cantidades, unidades, rendimiento y porciones. La valorización usa el costo de última compra y se recalcula cuando ingresa una compra nueva. La modificación actualiza la receta maestra y aplica solo a nuevas ventas. El acceso a costos y el mantenimiento de recetas están gobernados por permisos.
- **Reglas de negocio asociadas:** RN-043, RN-044, RN-045, RN-012, RN-004.
- **Criterios de aceptación:**
  - **CA1 — Composición de la receta**
    - Given un producto vendible o una subreceta
    - When el administrador registra su receta
    - Then el sistema debe permitir indicar insumos y/o subrecetas, cantidades, unidades, rendimiento y porciones.
  - **CA2 — Costo por porción**
    - Given una receta con insumos valorizados
    - When se guarda
    - Then el sistema debe calcular y mostrar el costo por porción usando el costo de última compra de cada insumo.
  - **CA3 — Anidamiento de subrecetas**
    - Given una subreceta ya definida
    - When se incluye como ingrediente de otra receta
    - Then el sistema debe incorporar su costo por porción al costo de la receta contenedora.
  - **CA4 — Recálculo por nueva compra**
    - Given una compra confirmada que actualiza el costo de un insumo
    - When se consulta la receta que lo utiliza
    - Then el sistema debe mostrar el costo actualizado y los márgenes futuros recalculados
    - And no debe modificar los costos de ventas ya cerradas.
  - **CA5 — Vigencia de la modificación**
    - Given una receta modificada
    - When se registran nuevas ventas
    - Then el sistema debe aplicar la receta actualizada solo a esas ventas nuevas.
  - **CA6 — Permisos y auditoría**
    - Given un usuario sin permiso de ver costos
    - When abre una receta
    - Then el sistema no debe mostrar los costos
    - And todo cambio de receta debe registrarse en la auditoría.
- **Dependencias:** US-008, US-009, US-004, US-006. Para la valorización real depende de US-050 (compras) que alimenta el costo de última compra.
- **Datos o entidades involucradas:** Receta, IngredienteReceta, Producto, subreceta, rendimiento, porciones, costo por porción.
- **Riesgos, supuestos o pendientes:** la valorización inicial antes de la primera compra no está documentada — **Supuesto de análisis:** requiere un costo de referencia inicial cargado manualmente; debe confirmarse con negocio. Relacionado con RF-002.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Recetas y costos; `06-modelo-de-datos.md` § Catálogo y costos; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (10, 11).
- **Estimación inicial:** XL

---

### [US-011] Administrar el catálogo de proveedores

- **Épica/Módulo:** EP-02 — Catálogos y datos maestros
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador de local / Administrador central
- **Historia de usuario:**
  - Como administrador,
  - quiero mantener el catálogo de proveedores,
  - para poder asociar cada compra a su proveedor y su comprobante.
- **Descripción funcional:** Alta, edición y desactivación de proveedores usados en el registro de compras. No incluye órdenes de compra ni cuentas por pagar.
- **Reglas de negocio asociadas:** RN-007, RN-055.
- **Criterios de aceptación:**
  - **CA1 — Alta de proveedor**
    - Given un usuario con permiso de creación
    - When registra un proveedor
    - Then el sistema debe guardarlo y ponerlo disponible para el registro de compras.
  - **CA2 — Sin órdenes de compra**
    - Given el módulo de proveedores
    - When se revisan sus funciones
    - Then el sistema no debe ofrecer órdenes de compra ni cuentas por pagar.
  - **CA3 — Desactivación**
    - Given un proveedor con compras registradas
    - When se desactiva
    - Then el sistema debe impedir su uso en nuevas compras y conservar las anteriores.
- **Dependencias:** US-004.
- **Datos o entidades involucradas:** Proveedor, Compra.
- **Riesgos, supuestos o pendientes:** los campos opcionales adicionales de proveedores están pendientes de definición (`07`) — ver RF-009.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Compras; `06-modelo-de-datos.md` § Catálogo y costos; `README.md` § Exclusiones confirmadas.
- **Estimación inicial:** S

---

### [US-012] Configurar listas de motivos del sistema

- **Épica/Módulo:** EP-02 — Catálogos y datos maestros
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador central,
  - quiero configurar las listas de motivos de ajuste, anulación y rechazo,
  - para que las acciones sensibles se justifiquen con opciones homogéneas y reportables.
- **Descripción funcional:** Mantenimiento de catálogos de motivos configurables utilizados en ajustes de inventario y, según la especificación, también en anulaciones y rechazos de pagos.
- **Reglas de negocio asociadas:** RN-052, RN-072, RN-004.
- **Criterios de aceptación:**
  - **CA1 — Motivos configurables de ajuste**
    - Given el catálogo de motivos de ajuste
    - When el administrador agrega, edita o desactiva un motivo
    - Then el sistema debe reflejarlo en la lista disponible al registrar ajustes.
  - **CA2 — Uso obligatorio**
    - Given un ajuste de inventario
    - When el usuario intenta confirmarlo sin seleccionar un motivo de la lista
    - Then el sistema debe impedir la confirmación.
  - **CA3 — Observación complementaria**
    - Given un ajuste con motivo seleccionado
    - When el usuario añade una observación
    - Then el sistema debe guardarla junto con el movimiento.
- **Dependencias:** US-004.
- **Datos o entidades involucradas:** MotivoAjuste, motivos de anulación, motivos de rechazo de pago.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — los motivos iniciales de ajuste, anulación y rechazo de pagos no están definidos (`07` § Pendientes funcionales). La funcionalidad es implementable como catálogo vacío configurable. Ver RF-005.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `03-pos-caja-cocina-delivery.md` § Caja; `07-criterios-de-aceptacion-y-pendientes.md` § Pendientes funcionales; `06-modelo-de-datos.md` § Inventario.
- **Estimación inicial:** XS

---

