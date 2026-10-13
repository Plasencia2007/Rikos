<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 1987-2318. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

### EP-08 — Inventario, producción y compras

---

### [US-043] Mantener existencias por local con stock mínimo y alertas

- **Épica/Módulo:** EP-08 — Inventario y compras
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador de local
- **Historia de usuario:**
  - Como administrador de local,
  - quiero consultar las existencias de mi local con su stock mínimo configurado,
  - para saber qué falta sin que el sistema me bloquee la operación.
- **Descripción funcional:** Existencia identificada por local y producto, con stock mínimo configurable por producto y local. El stock negativo se permite con alerta y no bloquea movimientos.
- **Reglas de negocio asociadas:** RN-002, RN-050, RN-051, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Existencia por local y producto**
    - Given un producto y un local
    - When se consulta la existencia
    - Then el sistema debe mostrar una única existencia identificada por esa combinación.
  - **CA2 — Stock mínimo configurable**
    - Given un producto en un local
    - When el administrador define su stock mínimo
    - Then el sistema debe guardarlo para ese producto y local.
  - **CA3 — Alerta de stock mínimo**
    - Given una existencia que baja del stock mínimo
    - When se registra el movimiento
    - Then el sistema debe alertar sin bloquear la operación.
  - **CA4 — Stock negativo permitido con alerta**
    - Given una existencia insuficiente
    - When se registra una venta o consumo que la deja en negativo
    - Then el sistema debe permitir el movimiento y mostrar una alerta.
- **Dependencias:** US-008, US-009, US-001.
- **Datos o entidades involucradas:** Existencia (Local + Producto), stock mínimo, MovimientoInventario.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `01-alcance-y-reglas.md` § Multilocal; `06-modelo-de-datos.md` § Inventario.
- **Estimación inicial:** M

---

### [US-044] Consultar el Kardex con origen rastreable

- **Épica/Módulo:** EP-08 — Inventario y compras
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador de local / Administrador central
- **Historia de usuario:**
  - Como administrador,
  - quiero ver el Kardex de cada producto con el origen de cada movimiento,
  - para poder explicar cualquier variación de stock.
- **Descripción funcional:** Registro y consulta de todos los movimientos de inventario con su origen: compra, producción, venta, ajuste, transferencia, conteo o consumo interno, incluyendo stock anterior y nuevo, diferencia, costo, usuario, fecha y local.
- **Reglas de negocio asociadas:** RN-057, RN-052, RN-002.
- **Criterios de aceptación:**
  - **CA1 — Origen obligatorio**
    - Given cualquier movimiento de inventario
    - When se registra
    - Then el sistema debe asociarlo a un origen entre compra, producción, venta, ajuste, transferencia, conteo o consumo interno.
  - **CA2 — Contenido del movimiento**
    - Given un movimiento registrado
    - When se consulta el Kardex
    - Then debe mostrar stock anterior, stock nuevo, diferencia, costo, usuario, fecha y local.
  - **CA3 — Filtros**
    - Given el Kardex de un local
    - When se filtra por producto o rango de fechas
    - Then el sistema debe mostrar los movimientos correspondientes en orden cronológico.
- **Dependencias:** US-043.
- **Datos o entidades involucradas:** MovimientoInventario, Existencia, Producto, Local, Usuario.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `06-modelo-de-datos.md` §§ Inventario, Reglas de integridad esenciales; `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos.
- **Estimación inicial:** L

---

### [US-045] Registrar ajustes de inventario con motivo

- **Épica/Módulo:** EP-08 — Inventario y compras
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador de local con permiso
- **Historia de usuario:**
  - Como administrador de local,
  - quiero registrar ajustes positivos o negativos indicando motivo y observación,
  - para corregir el stock dejando constancia de por qué cambió.
- **Descripción funcional:** Registro de ajustes de inventario con motivo tomado de una lista configurable y observación, guardando stock anterior y nuevo, diferencia, costo, usuario, fecha y local. Los ajustes se auditan.
- **Reglas de negocio asociadas:** RN-052, RN-004, RN-012, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Ajuste positivo y negativo**
    - Given una existencia
    - When el usuario registra un ajuste positivo o negativo
    - Then el sistema debe aplicar la variación al stock del local.
  - **CA2 — Motivo obligatorio**
    - Given un ajuste en curso
    - When se intenta confirmar sin motivo de la lista configurable
    - Then el sistema debe impedir la confirmación.
  - **CA3 — Datos guardados**
    - Given un ajuste confirmado
    - When se consulta
    - Then debe mostrar stock anterior, stock nuevo, diferencia, costo, usuario, fecha, local, motivo y observación.
  - **CA4 — Permiso**
    - Given un usuario sin permiso de ajustar inventario
    - When intenta registrar un ajuste
    - Then el sistema debe impedirlo.
- **Estado:** Implementado (2026-10-13)
- **Dependencias:** US-043, US-012, US-004, US-006.
- **Datos o entidades involucradas:** MovimientoInventario, Motivo, Existencia, Auditoria.
- **Riesgos, supuestos o pendientes:** Resuelto — motivos de `ajuste_inventario` configurables en BD con validación de observación obligatoria (CA2).
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `01-alcance-y-reglas.md` § Auditoría transversal; `06-modelo-de-datos.md` § Inventario; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (19).
- **Estimación inicial:** M

---

### [US-046] Transferir productos entre locales

- **Épica/Módulo:** EP-08 — Inventario y compras
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Administrador de local / Administrador central
- **Historia de usuario:**
  - Como administrador,
  - quiero transferir productos de un local a otro,
  - para cubrir faltantes manteniendo el stock de ambos locales correcto.
- **Descripción funcional:** Transferencia simple entre locales que, al confirmarse, genera dos ajustes enlazados aplicados simultáneamente: salida en el local origen y entrada en el local destino. Queda auditada.
- **Reglas de negocio asociadas:** RN-010, RN-004, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Ajustes enlazados**
    - Given una transferencia entre dos locales
    - When se confirma
    - Then el sistema debe generar simultáneamente un movimiento de salida en el origen y uno de entrada en el destino, enlazados entre sí.
  - **CA2 — Trazabilidad**
    - Given una transferencia confirmada
    - When se consulta el Kardex de cualquiera de los dos locales
    - Then el movimiento debe identificarse con origen transferencia y referenciar su contraparte.
  - **CA3 — Auditoría**
    - Given una transferencia confirmada
    - When se consulta la auditoría
    - Then debe registrar usuario, fecha, hora, locales y cantidades.
- **Dependencias:** US-045, US-043, US-006.
- **Datos o entidades involucradas:** Transferencia, MovimientoInventario (salida y entrada), Existencia, Local.
- **Riesgos, supuestos o pendientes:** no se documenta quién confirma la transferencia ni si el local receptor debe aceptarla — ver RF-024.
- **Trazabilidad:** `01-alcance-y-reglas.md` § Multilocal; `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `06-modelo-de-datos.md` § Inventario.
- **Estimación inicial:** M

---

### [US-047] Ejecutar conteos físicos sin detener la operación

- **Épica/Módulo:** EP-08 — Inventario y compras
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Administrador de local
- **Historia de usuario:**
  - Como administrador de local,
  - quiero hacer un conteo físico total mientras el local sigue vendiendo,
  - para cuadrar el inventario sin cerrar la operación.
- **Descripción funcional:** Conteo físico total y periódico que toma un snapshot inicial, permite registrar el detalle contado y concilia las compras, ventas, producción y ajustes ocurridos durante el conteo.
- **Reglas de negocio asociadas:** RN-053, RN-057, RN-052.
- **Criterios de aceptación:**
  - **CA1 — Snapshot inicial**
    - Given el inicio de un conteo físico
    - When se confirma su apertura
    - Then el sistema debe guardar el snapshot de existencias en ese momento.
  - **CA2 — Operación simultánea**
    - Given un conteo físico en curso
    - When se registran ventas, compras, producción o ajustes
    - Then el sistema debe permitir esas operaciones sin interrumpir el conteo.
  - **CA3 — Conciliación**
    - Given un conteo físico cerrado
    - When se calculan las diferencias
    - Then el sistema debe conciliar el conteo con los movimientos ocurridos durante el periodo y mostrar la diferencia resultante.
  - **CA4 — Generación del ajuste**
    - Given diferencias conciliadas
    - When se confirma el resultado del conteo
    - Then el sistema debe generar los movimientos de inventario con origen conteo.
- **Dependencias:** US-044, US-045, US-043.
- **Datos o entidades involucradas:** ConteoFisico, DetalleConteo, snapshot, MovimientoInventario.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — la frecuencia concreta de los conteos físicos (`07`). Ver RF-007.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Inventario y Kardex; `06-modelo-de-datos.md` § Inventario; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (12).
- **Estimación inicial:** XL

---

### [US-048] Producir subrecetas y descontar receta al vender

- **Estado:** ✅ Implementada (Sprint 9 — Existencias reales)
- **Épica/Módulo:** EP-08 — Inventario y compras
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Cocina / Administrador de local
- **Historia de usuario:**
  - Como encargado de cocina,
  - quiero registrar la producción de subrecetas y su rendimiento,
  - para que el inventario descuente los insumos una sola vez y el costo del plato sea correcto.
- **Descripción funcional:** Registro de producción: al producir una subreceta se descuentan sus insumos y se ingresa el rendimiento producido; al vender un plato final se descuenta su receta, incluidas las cantidades de subrecetas, sin duplicar consumos. Se registran porciones planificadas, preparadas y vendidas por plato. El sistema no calcula automáticamente la lista de insumos requeridos desde la planificación.
- **Reglas de negocio asociadas:** RN-047, RN-048, RN-057, RN-043.
- **Criterios de aceptación:**
  - **CA1 — Producción de subreceta**
    - Given una subreceta con receta definida
    - When se registra su producción
    - Then el sistema debe descontar sus insumos e ingresar el rendimiento producido al inventario.
  - **CA2 — Venta del plato final**
    - Given un plato cuya receta incluye una subreceta ya producida
    - When se vende
    - Then el sistema debe descontar su receta incluidas las cantidades de subreceta
    - And no debe volver a descontar los insumos ya consumidos en la producción de la subreceta.
  - **CA3 — Porciones**
    - Given un plato del día
    - When se consulta su seguimiento
    - Then el sistema debe mostrar porciones planificadas, preparadas y vendidas.
  - **CA4 — Sin cálculo automático de requerimientos**
    - Given la planificación semanal del menú
    - When se revisa el módulo de producción
    - Then el sistema no debe generar automáticamente una lista de insumos requeridos.
- **Dependencias:** US-010, US-043, US-013, US-033.
- **Datos o entidades involucradas:** Produccion (subreceta o plato, local, rendimiento, porciones), MovimientoInventario, Receta, IngredienteReceta.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Producción; `06-modelo-de-datos.md` § Menú y producción; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (11).
- **Estimación inicial:** XL

---

### [US-049] Registrar el destino de los sobrantes de producción

- **Épica/Módulo:** EP-08 — Inventario y compras
- **Prioridad:** Should
- **Release sugerido:** MVP
- **Actor:** Encargado de cocina / Administrador de local
- **Historia de usuario:**
  - Como encargado,
  - quiero decidir y registrar qué se hace con el sobrante,
  - para que el inventario refleje si se conservó, se consumió, se perdió o se reutilizó.
- **Descripción funcional:** Registro del destino del sobrante de producción entre conservar, personal, pérdida, reutilización u otro. El destino elegido genera el ajuste de inventario correspondiente.
- **Reglas de negocio asociadas:** RN-049, RN-052, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Selección del destino**
    - Given una producción con sobrante
    - When el encargado registra su destino
    - Then el sistema debe permitir elegir entre conservar, personal, pérdida, reutilización u otro.
  - **CA2 — Ajuste generado**
    - Given un destino registrado
    - When se confirma
    - Then el sistema debe generar el ajuste de inventario correspondiente a ese destino.
  - **CA3 — Trazabilidad**
    - Given un ajuste por sobrante
    - When se consulta el Kardex
    - Then debe identificar el destino registrado.
- **Dependencias:** US-048, US-045.
- **Datos o entidades involucradas:** Produccion, MovimientoInventario, Ajuste.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Producción.
- **Estimación inicial:** S

---

### [US-050] Registrar compras y actualizar el costo de última compra

- **Épica/Módulo:** EP-08 — Inventario y compras
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador de local
- **Historia de usuario:**
  - Como administrador de local,
  - quiero registrar la compra con su comprobante y confirmarla,
  - para aumentar el stock y actualizar el costo con el que se valorizan las recetas.
- **Descripción funcional:** Registro básico del ingreso de compra asociado a un proveedor y su comprobante (tipo, serie, número, fecha, productos, presentaciones, cantidades, precios, total y archivo adjunto). Al confirmar, aumenta el stock del local y actualiza el costo de última compra. No hay orden de compra ni cuenta por pagar.
- **Reglas de negocio asociadas:** RN-054, RN-055, RN-044, RN-057, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Datos del comprobante**
    - Given el registro de una compra
    - When se guarda
    - Then el sistema debe exigir tipo, serie, número, proveedor, fecha, productos, presentaciones, cantidades, precios y total, y permitir adjuntar el archivo del comprobante.
  - **CA2 — Confirmación de la compra**
    - Given una compra registrada
    - When se confirma
    - Then el sistema debe aumentar el stock del local según las presentaciones y conversiones
    - And debe actualizar el costo de última compra de cada producto.
  - **CA3 — Recálculo de recetas**
    - Given un costo de última compra actualizado
    - When se consultan las recetas que usan ese producto
    - Then el sistema debe mostrar los costos y márgenes futuros recalculados
    - And no debe modificar las ventas anteriores.
  - **CA4 — Sin OC ni CxP**
    - Given el módulo de compras
    - When se revisan sus funciones
    - Then el sistema no debe ofrecer órdenes de compra ni cuentas por pagar.
  - **CA5 — Protección del adjunto**
    - Given un comprobante adjunto
    - When lo consulta un usuario sin permiso
    - Then el sistema debe impedir el acceso.
- **Dependencias:** US-011, US-009, US-043, US-010.
- **Datos o entidades involucradas:** Compra, DetalleCompra, Proveedor, PresentaciónCompra, MovimientoInventario, costo de última compra, adjunto.
- **Riesgos, supuestos o pendientes:** la política de retención de adjuntos está pendiente — ver RF-011.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` §§ Compras, Recetas y costos; `06-modelo-de-datos.md` § Catálogo y costos; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (10); `README.md` § Exclusiones confirmadas.
- **Estimación inicial:** L

---

### [US-051] Registrar el consumo interno del personal

- **Épica/Módulo:** EP-08 — Inventario y compras
- **Prioridad:** Should
- **Release sugerido:** MVP
- **Actor:** Administrador de local / Cajero
- **Historia de usuario:**
  - Como administrador de local,
  - quiero registrar la comida del personal y su costo,
  - para descontar el inventario y reportarla separada de las ventas.
- **Descripción funcional:** Registro del consumo interno: una comida sin cobro por trabajador y turno; una segunda requiere autorización según permiso. Registra trabajador, platos, turno, costo y descuento de inventario, y se reporta separado de las ventas.
- **Reglas de negocio asociadas:** RN-056, RN-012, RN-057.
- **Criterios de aceptación:**
  - **CA1 — Primera comida sin cobro**
    - Given un trabajador que aún no registró consumo interno en su turno
    - When se registra su comida
    - Then el sistema debe permitirla sin cobro y descontar el inventario.
  - **CA2 — Segunda comida con autorización**
    - Given un trabajador que ya registró una comida en el turno
    - When se intenta registrar una segunda
    - Then el sistema debe exigir la autorización de un usuario con el permiso correspondiente.
  - **CA3 — Datos registrados**
    - Given un consumo interno confirmado
    - When se consulta
    - Then debe mostrar trabajador, platos, turno y costo.
  - **CA4 — Reporte separado**
    - Given consumos internos registrados
    - When se consultan los reportes de ventas
    - Then los consumos internos deben reportarse por separado y no como ventas.
- **Dependencias:** US-043, US-010, US-004, US-032.
- **Datos o entidades involucradas:** consumo interno (trabajador, platos, turno, costo), MovimientoInventario, Usuario.
- **Riesgos, supuestos o pendientes:** no se documenta qué platos puede incluir el consumo interno ni si aplica a motorizados o personal externo — ver RF-023.
- **Trazabilidad:** `04-menu-recetas-inventario-compras.md` § Consumo interno; `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos.
- **Estimación inicial:** M

---

