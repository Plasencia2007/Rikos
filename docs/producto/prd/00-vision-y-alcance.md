<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 1-95. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

# Product Backlog Inicial — Sistema de Gestión Integral RIKO'S

Versión del backlog: 2.0
Fecha de elaboración: 11 de septiembre de 2026
Fuente de verdad: los 8 archivos `.md` de la especificación funcional v0.1 del proyecto.
Rol de elaboración: Product Owner senior / Business Analyst.

> **Qué cambió respecto de la versión 1.0.** Se corrigió el orden de ejecución, se añadieron las diez historias técnicas habilitantes que un backlog derivado de requisitos no puede contener, se incorporó el mapa completo de las 70 pantallas del sistema, y la tabla resumen pasó a estar ordenada por sprint en lugar de por prioridad. Las 61 historias de negocio no cambiaron de contenido. El detalle está en el apartado 9.

> **Nota metodológica.** Todo el contenido está derivado exclusivamente de los archivos `.md` listados en la Sección 2. Donde fue necesario inferir algo para poder secuenciar el trabajo, se marca explícitamente como **Supuesto de análisis**. Ninguna funcionalidad, campo, integración o regla ajena a los documentos fue añadida.

# 1. Resumen del entendimiento del sistema

## 1.1 Propósito del sistema

RIKO'S es un restaurante criollo peruano que atiende público general y una proporción importante de estudiantes pensionistas. El sistema busca documentar e implementar un **sistema de gestión integral, multilocal y offline-first** que cubra la operación completa del negocio: venta, atención, pensionistas, menú, recetas, costos, inventario, compras, caja, facturación electrónica, delivery propio y reportes.

El objetivo declarado en `README.md` es que un equipo de desarrollo pueda diseñar la arquitectura e implementar el sistema **sin depender de explicaciones verbales**. La documentación es deliberadamente **agnóstica de tecnología**: no prescribe lenguaje, framework, base de datos, nube ni arquitectura de despliegue.

## 1.2 Problema de negocio que busca resolver

De los documentos se desprenden cinco problemas centrales:

1. **Control de la cuenta corriente de pensionistas.** Consumo diario a crédito, con responsable obligatorio, planes tarifarios, límites, abonos por múltiples medios y cierre mensual con arrastre. Hoy no hay un mecanismo trazable que registre el consumo únicamente al cerrar en caja ni que impida doble cobro o doble vinculación.
2. **Operación de venta multicanal.** Mesa, para llevar, delivery propio y recojo programado, con mesas mixtas donde conviven clientes regulares y varios pensionistas y donde cada producto debe asignarse a una persona.
3. **Costeo real y control de existencias.** Menú que cambia diariamente, recetas con subrecetas, costeo por última compra, producción, Kardex y conteos físicos que no detienen la operación.
4. **Continuidad operativa.** El local debe poder vender, comandar, cobrar y cargar consumos de pensionistas **sin Internet**, y sincronizar después sin duplicar pedidos, cargos ni comprobantes.
5. **Control administrativo y trazabilidad.** Auditoría transversal de descuentos, anulaciones, reaperturas, ajustes, transferencias, cambios de recetas/precios/permisos, aprobación de abonos y cierres de caja, más reportes exportables a Excel y PDF.

## 1.3 Principales actores identificados

Fuente: `01-alcance-y-reglas.md` § Actores.

| Actor | Rol en el sistema |
|---|---|
| Administrador central | Administra catálogo, recetas, precios, planes y programación de menú comunes a todos los locales; aprueba abonos; administra permisos. |
| Administrador de local | Opera y controla un local: inventario, compras, producción, caja, delivery, disponibilidad. |
| Cajero | Apertura/cierre de turno, cobro, abonos en efectivo, conciliación, emisión de comprobantes, aceptación de direcciones de delivery. |
| Mozo | Toma de pedido, asignación de productos por persona, vinculación de pre-tickets, comandas. Accede por PIN individual. |
| Cocina | Recibe comandas por impresora central, marca productos agotados, avisa pedido listo de forma verbal o por campana. |
| Motorizado | Vista web móvil de delivery: consulta dirección/teléfono, cambia estados, comparte última ubicación en ruta. |
| Pensionista y responsable | Acceso compartido al portal (consulta de movimientos, saldos, comprobantes, estados mensuales) y registro de abonos digitales. El pensionista además usa el autoservicio presencial con DNI y PIN. |
| Cliente regular | Cliente de público general, atendido en cualquiera de los cuatro canales. |

## 1.4 Módulos detectados

1. Alcance, reglas transversales, multilocal y auditoría (`01`).
2. Pensionistas: ficha, cuenta corriente, planes tarifarios, autoservicio, portal, abonos, facturación (`02`).
3. POS, mesas, comandas, disponibilidad, caja, facturación electrónica, recojo programado y delivery propio (`03`).
4. Catálogo, almuerzo y programación semanal, recetas y costos, producción, inventario y Kardex, compras, consumo interno (`04`).
5. Usuarios y permisos, panel del administrador, reportes, offline-first, respaldo y seguridad (`05`).
6. Modelo conceptual de datos y reglas de integridad esenciales (`06`).
7. Criterios de aceptación críticos y pendientes funcionales (`07`).

## 1.5 Procesos principales

- **Configuración maestra:** empresa/locales → usuarios, roles y permisos → catálogo, unidades y presentaciones → proveedores → recetas y subrecetas → planes tarifarios → horarios de servicio → motivos configurables.
- **Planificación:** programación semanal del almuerzo por día y servicio (borrador → confirmado → en servicio); productos fijos para desayuno y cena.
- **Registro operativo:** autoservicio del pensionista → pre-ticket → toma de pedido y asignación por persona → comanda a cocina → cierre en caja → cargo a cuenta corriente y comprobante electrónico.
- **Canales alternativos:** para llevar, recojo programado (con táper y pago anticipado del cliente regular) y delivery propio con estados y liquidación.
- **Control:** compras que actualizan costo de última compra, producción de subrecetas, movimientos de inventario con origen rastreable, ajustes, transferencias entre locales, conteos físicos, consumo interno.
- **Cierre y control financiero:** cierre de turno de caja con conciliación y alerta desde S/0.01; cierre mensual de pensionistas con arrastre y PDF fijo.
- **Reportes y continuidad:** panel del administrador, reportes exportables, operación offline y sincronización idempotente, respaldo en tiempo real.

## 1.6 Fuera de alcance (exclusiones confirmadas)

Fuente: `README.md` § Exclusiones confirmadas y `05-usuarios-reportes-multilocal-offline.md` § Usuarios y permisos.

- Reservas de mesas.
- Fidelización para clientes regulares.
- Asistencia, horarios laborales, planilla y remuneraciones.
- Órdenes de compra y cuentas por pagar.
- Importación inicial masiva desde Excel o sistemas anteriores.
- Integración automática con WhatsApp.
- Publicación pública del menú del día (el menú solo se publica dentro del POS).
- Optimización automática de rutas.
- Cálculo de comisiones de motorizados (sueldo fijo).

---

# 2. Inventario de fuentes revisadas

| Archivo Markdown | Tema principal | Requisitos o reglas relevantes identificadas |
|---|---|---|
| `README.md` | Portada, objetivo, alcance de la primera versión, convenciones y exclusiones | Objetivo del sistema; 11 puntos de alcance confirmado (POS responsive, 4 canales, pensionistas con cuenta corriente y portal, programación semanal, recetas/costos/inventario/Kardex, proveedores y compras, caja por turnos y facturación electrónica por API, delivery propio, usuarios/roles/permisos, reportes Excel/PDF, multilocal y respaldo en tiempo real); convenciones Obligatorio/Pendiente/Excluido; 8 exclusiones confirmadas; los importes de ejemplo no son precios definitivos |
| `01-alcance-y-reglas.md` | Contexto del negocio, actores, canales, multilocal, auditoría e historial | 8 actores; 4 canales; táper por menú en para llevar/recojo/delivery; centralización de catálogo, recetas, precios, planes y programación; inventario, compras, producción, caja, mesas, disponibilidad y delivery por local; inventario general único por local; cuenta de pensionista única y multilocal; transferencias con ajustes enlazados; 6 eventos con auditoría obligatoria; historial inmutable de ventas cerradas, cierres mensuales y cierres de caja; desactivar no elimina historial |
| `02-pensionistas.md` | Ficha, cuenta corriente, planes tarifarios, autoservicio, portal, abonos, desactivación y facturación | Campos de ficha y responsable obligatorio; registro del consumo solo al cerrar en caja; fórmula de crédito disponible; límite preventivo; aplicación previa del saldo a favor; múltiples consumos por servicio y día; cierre mensual con arrastre; planes con vigencia e historial; menú extra a precio completo; flujo de autoservicio en 6 pasos; 5 estados del pre-ticket y su vencimiento; contenido obligatorio del pre-ticket y prohibición de doble vinculación; portal compartido con alcance limitado; medios y estados de abono; requisitos del reporte de abono digital; validación de duplicidad de número de operación; aprobación exclusiva del administrador; abonos parciales y frecuencias; reglas de desactivación/reactivación; comprobante por consumo a nombre y DNI del pensionista |
| `03-pos-caja-cocina-delivery.md` | POS y mesas, comandas, disponibilidad, caja, facturación electrónica, recojo programado y delivery propio | PWA responsive; 5 estados de mesa; mover/unir/dividir/trasladar; mesas mixtas y asignación por persona; cierres parciales sin liberar mesa; impresora central única y contenido de la comanda; reimpresión marcada como copia; aviso verbal/campana sin estado digital; efecto en inventario de anulaciones antes y después de comandar; marcado manual de agotado y reactivación auditada; apertura/cierre por turno; efectivo esperado visible; conciliación; prohibición de gastos/retiros/ingresos extraordinarios salvo abono en efectivo; alerta desde S/0.01 sin bloquear; división de pago entre efectivo/Yape/Plin/tarjeta; descuentos y anulaciones con permiso y motivo; reapertura solo con turno abierto; corrección posterior por operación compensatoria; API fiscal con cola offline e idempotencia; reglas completas de recojo programado y de delivery propio con 7 estados |
| `04-menu-recetas-inventario-compras.md` | Catálogo, almuerzo y programación semanal, recetas y costos, producción, inventario y Kardex, compras, consumo interno | Clasificación de productos; composición y precio único del menú normal; menú extra con precio propio; manejo de opciones por el mozo; programación semanal por día y servicio con 3 estados y porciones estimadas; edición sin tocar el catálogo maestro; advertencia por repetición en 3 días previos; publicación solo en POS; desayuno y cena sin programación semanal; recetas valorizadas, subrecetas, campos de receta; costeo por última compra y recálculo futuro; actualización de receta maestra aplicable a nuevas ventas; snapshot histórico por venta; permisos sobre costos y recetas; producción de subrecetas y descuento al vender; porciones planificadas/preparadas/vendidas; ausencia de cálculo automático de insumos requeridos; destinos de sobrantes y su ajuste; inventario general por local; stock negativo con alerta; stock mínimo por producto y local; unidades base y conversiones; ajustes con motivos configurables y datos obligatorios; transferencias como dos ajustes enlazados; conteos físicos con snapshot y conciliación; compras sin OC ni CxP con documento adjunto; confirmación que aumenta stock y actualiza costo; consumo interno con una comida libre por trabajador y turno |
| `05-usuarios-reportes-multilocal-offline.md` | Usuarios y permisos, panel, reportes, offline-first, respaldo, seguridad y restricción tecnológica | Usuario/contraseña general y PIN de mozo; roles y permisos granulares por módulo y acción, independientes del cargo; asignación multilocal; desactivación sin borrar historial; exclusión de RRHH; lista de 11 permisos mínimos; 4 prioridades del panel y sus filtros; 12 reportes mínimos; exportación a Excel y PDF y PDF fijo por estado mensual; operación local completa sin Internet incluida autenticación y copia local del saldo; sincronización con identificadores únicos e idempotencia; resolución de conflictos con registro; indisponibilidad tolerada de portal, API fiscal y ubicación; respaldo central en tiempo real; contraseñas y PIN nunca legibles; bloqueo temporal por intentos fallidos; separación PIN/contraseña de portal; protección de adjuntos por permisos; registro de sesiones y acciones sensibles; neutralidad tecnológica |
| `06-modelo-de-datos.md` | Modelo conceptual de entidades, relaciones y reglas de integridad | Empresa/Local y sus dependencias; Usuario N—N Local; Rol N—N Permiso con excepciones por usuario; Producto, presentaciones y conversiones; Receta e IngredienteReceta; Compra/DetalleCompra; SemanaMenu/DiaMenu/OpcionMenu; Produccion; Existencia por Local+Producto; MovimientoInventario con origen; Ajuste con MotivoAjuste; Transferencia enlazada; ConteoFisico/DetalleConteo con snapshot; Pedido/CuentaPersona/DetallePedido con snapshot de costo/receta; Comanda/DetalleComanda con impresiones; Pago/MedioPago; ComprobanteElectronico; Pensionista/CuentaPensionista/Responsable/PlanTarifario/PrecioPlan/MovimientoCuenta/PeriodoCuenta/EstadoCuentaMensual/PreTicket/Abono; PedidoDelivery/HistorialDelivery/PedidoRecojo; TurnoCaja/MovimientoCaja/CierreCaja; 7 reglas de integridad esenciales |
| `07-criterios-de-aceptacion-y-pendientes.md` | Casos críticos de aceptación, pendientes funcionales y validaciones externas | 20 casos de aceptación end-to-end que atraviesan pensionistas, POS, caja, inventario, recojo, delivery, offline y permisos; 13 pendientes funcionales; 3 validaciones externas pendientes (contador, proveedor de comprobantes, hardware real) |

---

