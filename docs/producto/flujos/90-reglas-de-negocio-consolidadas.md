<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 3157-3268. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

# 11. Reglas de negocio consolidadas

Catálogo único, sin duplicados. Cuando una regla aparecía en varios documentos, se consolidó en un solo ID y se listaron todas sus fuentes.

| ID regla | Regla de negocio | Módulo | Fuente(s) | Historia(s) relacionada(s) |
|---|---|---|---|---|
| RN-001 | Catálogo, recetas, precios, planes y programación del menú se administran centralmente y son comunes a todos los locales | Transversal / Maestros | `01` Multilocal | US-008, US-009, US-010, US-013, US-014, US-017 |
| RN-002 | Cada local mantiene inventario, compras, producción, caja, mesas, disponibilidad y delivery propios, con un único inventario general | Transversal / Multilocal | `01` Multilocal; `06` Organización | US-001, US-015, US-027, US-032, US-043, US-052 |
| RN-003 | La cuenta del pensionista es única y utilizable en cualquier local, y cada movimiento identifica su local | Pensionistas | `01` Multilocal; `07` Caso 20 | US-018, US-055 |
| RN-004 | Descuentos, anulaciones, reaperturas, ajustes y transferencias de inventario, cambios de recetas, precios y permisos, aprobación o rechazo de abonos y cierres de caja registran usuario, fecha, hora, local, valores anterior y nuevo y motivo cuando aplique | Transversal / Auditoría | `01` Auditoría transversal | US-006, US-010, US-015, US-024, US-031, US-034, US-035, US-036, US-045, US-046 |
| RN-005 | Las ventas cerradas conservan precio, costo y composición históricos | Venta | `01` Reglas de historial; `06` Reglas de integridad | US-029, US-033, US-053 |
| RN-006 | Los cierres mensuales de pensionistas y los cierres de caja son inalterables después de cerrados y no aceptan movimientos retroactivos | Pensionistas / Caja | `01` Reglas de historial; `06` Reglas de integridad; `07` Caso 6 | US-025, US-035, US-036 |
| RN-007 | Desactivar registros no elimina su historial | Transversal | `01` Reglas de historial; `05` Usuarios y permisos | US-001, US-005, US-008, US-011, US-026 |
| RN-008 | Los pensionistas pueden utilizar su cuenta en todos los canales | Pensionistas / Canales | `01` Canales | US-018, US-028, US-038, US-040 |
| RN-009 | Para llevar, recojo y delivery añaden un cargo de táper por cada menú | Venta / Canales | `01` Canales; `03` Recojo programado | US-028, US-038, US-040 |
| RN-010 | Una transferencia entre locales genera ajustes enlazados de salida y entrada al confirmarse | Inventario | `01` Multilocal; `04` Inventario y Kardex; `06` Inventario | US-046 |
| RN-011 | El acceso general es por usuario y contraseña; los mozos usan PIN individual para cambio rápido en dispositivos compartidos | Usuarios | `05` Usuarios y permisos | US-002, US-003 |
| RN-012 | Los permisos son granulares por módulo y acción (ver, crear, modificar, desactivar, aprobar, anular, descontar, reabrir, ver costos, exportar, administrar permisos) y no dependen del nombre del cargo | Usuarios | `05` Usuarios y permisos; `06` Organización | US-004, US-005, US-010, US-031, US-034, US-045, US-051, US-052, US-053 |
| RN-013 | Un usuario puede asignarse a uno o varios locales | Usuarios | `05` Usuarios y permisos; `06` Organización | US-002, US-005 |
| RN-014 | El sistema no incluye asistencia, horarios laborales, planilla ni remuneraciones | Fuera de alcance | `05` Usuarios y permisos; `README` Exclusiones | US-004 |
| RN-015 | El consumo del pensionista se registra únicamente al cerrar en caja | Pensionistas | `02` Cuenta corriente; `07` Caso 1 | US-018, US-033, US-038 |
| RN-016 | Crédito disponible = límite individual − deuda pendiente | Pensionistas | `02` Cuenta corriente | US-018, US-055 |
| RN-017 | El límite de crédito es preventivo: alerta al superarlo pero permite el consumo | Pensionistas | `02` Cuenta corriente; `07` Caso 4 | US-018, US-033 |
| RN-018 | Se aplica primero el saldo a favor y el remanente aumenta la deuda | Pensionistas | `02` Cuenta corriente | US-018, US-033 |
| RN-019 | Se permiten múltiples consumos del mismo servicio en un día y cada uno se cobra | Pensionistas | `02` Cuenta corriente; `07` Caso 3 | US-018 |
| RN-020 | El cierre mensual arrastra deuda o saldo a favor al mes siguiente sin marcar atraso automáticamente | Pensionistas | `02` Cuenta corriente; `07` Caso 6 | US-025 |
| RN-021 | Los planes tarifarios definen precio de almuerzo normal y precios especiales por producto fijo de desayuno y cena, con vigencia, estado e historial | Pensionistas | `02` Planes tarifarios; `06` Pensionistas | US-016, US-017 |
| RN-022 | Los consumos anteriores conservan la tarifa aplicada | Pensionistas | `02` Planes tarifarios | US-017 |
| RN-023 | El menú extra se cobra al precio completo del extra, sin tarifa reducida | Pensionistas / Menú | `02` Planes tarifarios; `04` Almuerzo | US-014, US-017 |
| RN-024 | El servicio se determina automáticamente por horario configurable | Pensionistas | `02` Autoservicio presencial | US-007, US-019 |
| RN-025 | El pre-ticket no selecciona platos, no constituye consumo cargado y muestra código único, pensionista, servicio y fecha/hora con la leyenda correspondiente | Pensionistas | `02` Autoservicio presencial | US-019, US-020 |
| RN-026 | Los estados del pre-ticket son pendiente, vinculado, cerrado, vencido y anulado; el pendiente vence al terminar el horario del servicio | Pensionistas | `02` Autoservicio presencial; `07` Caso 2 | US-007, US-019, US-020 |
| RN-027 | Un pre-ticket vencido o anulado no genera cargo, y no se permite la doble vinculación | Pensionistas | `02` Autoservicio presencial | US-019, US-020 |
| RN-028 | El portal es un acceso único compartido por pensionista y responsable mediante DNI y contraseña, y no permite modificar consumos ni aprobar abonos | Pensionistas / Portal | `02` Portal compartido | US-022, US-023 |
| RN-029 | Las notificaciones internas se emiten solo por abono aprobado o rechazado y por estado mensual generado, nunca por consumo ni por superar el límite | Pensionistas / Portal | `02` Portal compartido | US-022, US-024, US-025 |
| RN-030 | El abono en efectivo lo registra caja, queda confirmado inmediatamente y entra en conciliación | Pensionistas / Caja | `02` Abonos | US-021, US-036 |
| RN-031 | Los abonos digitales quedan pendientes y no modifican el saldo hasta la aprobación exclusiva del administrador | Pensionistas | `02` Abonos; `07` Caso 5 | US-021, US-023, US-024, US-055 |
| RN-032 | El reporte de abono desde el portal exige imagen de comprobante, número de operación, monto, fecha, medio y cuenta destino, y valida la posible duplicidad del número de operación | Pensionistas / Portal | `02` Abonos; `06` Reglas de integridad | US-023 |
| RN-033 | Se permiten abonos parciales y frecuencias habituales semanal, quincenal o mensual | Pensionistas | `02` Abonos, Ficha | US-016, US-021 |
| RN-034 | Un pensionista desactivado no genera nuevos tickets ni consumos, conserva cuenta, historial y portal, admite abonos posteriores, y su reactivación reutiliza la misma cuenta | Pensionistas | `02` Desactivación | US-019, US-026 |
| RN-035 | Se emite un comprobante electrónico por cada consumo al cerrar en caja, a nombre y DNI del pensionista; el abono no genera venta y puede generar constancia interna | Pensionistas / Facturación | `02` Facturación | US-021, US-033, US-037, US-039 |
| RN-036 | El DNI del pensionista es único en la empresa y el responsable es obligatorio | Pensionistas | `02` Ficha; `06` Reglas de integridad | US-016 |
| RN-037 | El menú normal se compone de entrada, segundo con guarnición, refresco y postre, con precio único independiente de las opciones elegidas | Menú | `04` Almuerzo | US-014, US-028 |
| RN-038 | Si hay varias opciones el mozo informa y registra la elegida; si hay una sola se asigna directamente | Menú / POS | `04` Almuerzo | US-020, US-028 |
| RN-039 | La programación semanal se hace por día y servicio, con estados borrador, confirmado y en servicio, registrando opciones y porciones estimadas | Menú | `04` Programación semanal; `06` Menú y producción | US-013 |
| RN-040 | El menú confirmado es editable sin modificar el catálogo maestro | Menú | `04` Programación semanal | US-013 |
| RN-041 | El sistema advierte si un segundo o guiso aparece en los tres días anteriores, pero permite confirmar | Menú | `04` Programación semanal | US-013 |
| RN-042 | El menú solo se publica dentro del POS; desayuno y cena usan productos fijos elegidos individualmente y no llevan programación semanal | Menú | `04` Programación semanal; `README` Exclusiones | US-013, US-028 |
| RN-043 | Cada componente tiene receta valorizada y costo por porción, y se permiten subrecetas dentro de otras recetas | Recetas y costos | `04` Recetas y costos; `06` Catálogo y costos | US-010, US-048 |
| RN-044 | Las recetas se valorizan con el costo de última compra y una nueva compra recalcula costos actuales y márgenes futuros | Recetas y costos | `04` Recetas y costos, Compras; `07` Caso 10 | US-010, US-050 |
| RN-045 | La actualización de la receta maestra aplica únicamente a nuevas ventas | Recetas y costos | `04` Recetas y costos | US-010 |
| RN-046 | Cada venta guarda copia histórica de componentes, receta aplicada, costos, precio, utilidad y margen | Venta / Costos | `04` Recetas y costos; `06` Venta | US-033, US-053 |
| RN-047 | Producir una subreceta descuenta sus insumos e ingresa el rendimiento; vender el plato final descuenta su receta incluidas las subrecetas, sin duplicar consumos | Producción / Inventario | `04` Producción; `07` Caso 11 | US-033, US-048 |
| RN-048 | Se registran porciones planificadas, preparadas y vendidas por plato, y no se calcula automáticamente la lista de insumos requeridos desde la planificación | Producción | `04` Producción | US-048, US-056 |
| RN-049 | El encargado decide el destino del sobrante entre conservar, personal, pérdida, reutilización u otro, y ese destino produce el ajuste de inventario correspondiente | Producción / Inventario | `04` Producción | US-049 |
| RN-050 | El stock negativo se permite con alerta y no bloquea movimientos | Inventario | `04` Inventario y Kardex | US-043, US-056 |
| RN-051 | El stock mínimo es una cantidad configurable por producto y local | Inventario | `04` Inventario y Kardex | US-043, US-056 |
| RN-052 | Los ajustes usan una lista configurable de motivos y observación, y guardan stock anterior, stock nuevo, diferencia, costo, usuario, fecha y local | Inventario | `04` Inventario y Kardex; `06` Inventario | US-012, US-045, US-047, US-049, US-056 |
| RN-053 | Los conteos físicos son totales y periódicos, no detienen la operación, toman snapshot inicial y concilian compras, ventas, producción y ajustes del periodo | Inventario | `04` Inventario y Kardex; `06` Inventario; `07` Caso 12 | US-047 |
| RN-054 | Confirmar una compra aumenta el stock y actualiza el costo de última compra, asociando el comprobante con tipo, serie, número, proveedor, fecha, productos, presentaciones, cantidades, precios, total y archivo adjunto | Compras | `04` Compras; `06` Catálogo y costos; `07` Caso 10 | US-050 |
| RN-055 | El registro de compras es básico: no hay orden de compra ni cuenta por pagar | Compras | `04` Compras; `README` Exclusiones | US-011, US-050 |
| RN-056 | El consumo interno permite una comida sin cobro por trabajador y turno; la segunda requiere autorización según permiso, registra trabajador, platos, turno y costo, descuenta inventario y se reporta separado de las ventas | Consumo interno | `04` Consumo interno; `05` Reportes mínimos | US-051, US-057 |
| RN-057 | Todo movimiento de inventario debe tener origen rastreable entre compra, producción, venta, ajuste, transferencia, conteo o consumo interno | Inventario | `06` Inventario, Reglas de integridad | US-031, US-039, US-043, US-044, US-045, US-046, US-047, US-048, US-049, US-050, US-051 |
| RN-058 | El POS es una PWA responsive para celular, tablet y terminal fija | POS | `03` POS y mesas; `README` Alcance; `05` Restricción tecnológica | US-027, US-028 |
| RN-059 | Los estados de mesa son libre, ocupada, unida, pendiente de cierre y bloqueada | POS | `03` POS y mesas | US-027, US-029 |
| RN-060 | Se permiten mesas mixtas con clientes regulares y varios pensionistas, asignando productos a cada persona al tomar el pedido | POS | `03` POS y mesas; `07` Caso 7 | US-020, US-028, US-029 |
| RN-061 | Se permiten cierres parciales sin liberar la mesa mientras haya consumos pendientes | POS / Caja | `03` POS y mesas | US-033 |
| RN-062 | Existe una sola impresora central de cocina y la comanda muestra pedido/mesa/canal, mozo, hora, productos por persona, componentes, observaciones y tipo de entrega | Cocina | `03` Comandas | US-030 |
| RN-063 | Toda reimpresión de comanda se marca como copia | Cocina | `03` Comandas | US-030 |
| RN-064 | Cocina informa el pedido listo verbalmente o mediante campana y no requiere cambio digital de estado | Cocina | `03` Comandas | US-030 |
| RN-065 | Un producto anulado después de comandar no se cobra pero descuenta receta como consumo o pérdida; anulado antes de comandar no descuenta inventario | Cocina / Inventario | `03` Comandas; `07` Caso 9 | US-031, US-057 |
| RN-066 | Un producto marcado como agotado se oculta en pedidos nuevos sin modificar comandas existentes, y su reactivación registra usuario, hora y motivo | Disponibilidad | `03` Disponibilidad; `07` Caso 8 | US-015, US-028 |
| RN-067 | La caja se abre y se cierra por turno y el cajero puede ver el efectivo esperado antes de contar | Caja | `03` Caja | US-032, US-036 |
| RN-068 | El cierre concilia fondo inicial, cobros, abonos en efectivo y medios digitales o tarjeta | Caja | `03` Caja | US-036, US-054 |
| RN-069 | No se permiten gastos, retiros ni ingresos extraordinarios en caja; la única excepción es el abono en efectivo de pensionista | Caja | `03` Caja; `06` Caja | US-021, US-032, US-033 |
| RN-070 | Cualquier diferencia de caja desde S/0.01 genera alerta sin bloquear el cierre | Caja | `03` Caja; `07` Caso 18 | US-036, US-054 |
| RN-071 | Una cuenta puede dividirse libremente entre efectivo, Yape, Plin y tarjeta | Caja | `03` Caja | US-033 |
| RN-072 | Los descuentos y anulaciones requieren permisos específicos y motivo | Caja | `03` Caja; `07` Caso 19 | US-031, US-034 |
| RN-073 | Solo un administrador autorizado puede reabrir una venta y únicamente mientras el turno de origen siga abierto | Caja | `03` Caja; `06` Reglas de integridad; `07` Caso 17 | US-035 |
| RN-074 | Después del cierre de turno las correcciones se hacen mediante operación compensatoria o documento relacionado, sin alterar el cierre original | Caja | `03` Caja | US-035 |
| RN-075 | La facturación electrónica se integra por API externa, guarda respuesta, estado y errores, y usa cola offline, reintentos idempotentes y prevención de comprobantes duplicados | Facturación | `03` Facturación electrónica; `07` Caso 16 | US-037, US-059 |
| RN-076 | El recojo programado se recibe por WhatsApp, se registra manualmente y guarda fecha y hora futura, nombre y celular de quien recoge | Recojo | `03` Recojo programado | US-038 |
| RN-077 | En recojo, el cliente regular paga por anticipado antes de confirmar y al pensionista se le carga al cerrar el pedido | Recojo | `03` Recojo programado; `07` Caso 13 | US-038 |
| RN-078 | La cancelación de un recojo es sin cobro si ocurre antes de enviar la comanda | Recojo | `03` Recojo programado | US-038 |
| RN-079 | Un recojo preparado y no retirado descuenta inventario y se registra como no recogido: al cliente regular no se le devuelve el pago y al pensionista se le carga el pedido completo con comprobante | Recojo | `03` Recojo programado; `07` Casos 13 y 14 | US-039 |
| RN-080 | El delivery es propio, inicialmente gratuito y sin zonas, y caja decide si acepta la dirección | Delivery | `03` Delivery propio; `README` Alcance | US-040 |
| RN-081 | Los estados del delivery son pendiente, asignado, aceptado, recogido, en ruta, entregado y fallido, y la entrega se confirma solo marcando entregado | Delivery | `03` Delivery propio; `07` Caso 15 | US-040, US-041, US-042 |
| RN-082 | El motorizado usa vista web móvil, consulta dirección y teléfono, cambia estados y comparte su última ubicación mientras está en ruta | Delivery | `03` Delivery propio | US-041 |
| RN-083 | Los motorizados tienen sueldo fijo y no se calculan comisiones | Delivery | `03` Delivery propio; `README` Exclusiones | US-041, US-042 |
| RN-084 | Caja cierra la venta de delivery después de revisar y liquidar el pedido, controlando el dinero cobrado y las incidencias por motorizado | Delivery / Caja | `03` Delivery propio; `07` Caso 15 | US-042, US-057 |
| RN-085 | Los reportes y estados de cuenta se exportan a Excel y PDF, y cada estado mensual cerrado conserva un PDF fijo | Reportes | `05` Reportes mínimos; `README` Alcance | US-025, US-052, US-053, US-054, US-055, US-056, US-057 |
| RN-086 | El local opera completo sin Internet (autenticación local, POS, pensionistas, impresora, caja, inventario, producción y delivery local) y conserva una copia local del saldo necesario para operar pensionistas | Offline | `05` Offline-first; `07` Caso 16 | US-002, US-019, US-030, US-058, US-059 |
| RN-087 | La sincronización previene duplicados mediante identificadores únicos e idempotencia, resuelve conflictos sin perder transacciones y deja registro de la resolución | Offline | `05` Offline-first; `07` Caso 16 | US-037, US-059 |
| RN-088 | El portal externo, la API fiscal y la ubicación remota pueden quedar temporalmente indisponibles sin afectar la operación local | Offline | `05` Offline-first | US-022, US-037, US-041, US-058 |
| RN-089 | Existe respaldo central en tiempo real más copias recuperables | Respaldo | `05` Respaldo y seguridad; `README` Alcance | US-060 |
| RN-090 | Contraseñas y PIN nunca se guardan ni se muestran en texto legible, y el PIN presencial es distinto de la contraseña del portal | Seguridad | `05` Respaldo y seguridad; `02` Ficha | US-002, US-003, US-005, US-016, US-061 |
| RN-091 | Se aplica bloqueo temporal tras intentos fallidos de PIN | Seguridad | `05` Respaldo y seguridad | US-003, US-019, US-061 |
| RN-092 | Los adjuntos de pagos y compras se protegen según permisos y se registran las sesiones y acciones sensibles | Seguridad | `05` Respaldo y seguridad | US-002, US-006, US-023, US-024, US-050, US-061 |
| RN-093 | La documentación no prescribe lenguaje, framework, base de datos, nube ni arquitectura; la solución debe cumplir PWA, multilocal, operación local completa, sincronización y respaldo | Restricción técnica | `05` Restricción tecnológica; `README` Enfoque técnico | US-058, US-060, y todo el backlog |
| RN-094 | El código de pre-ticket es único y de un solo uso | Pensionistas / Integridad | `06` Reglas de integridad | US-019, US-020 |

---



---

## Cierre

Este backlog contiene **11 épicas, 72 elementos priorizados —62 historias de negocio y 10 habilitantes técnicas—, 70 pantallas inventariadas, 94 reglas de negocio consolidadas y 28 preguntas de refinamiento**, todas trazadas a los 8 archivos `.md` de la especificación funcional v0.1 de RIKO'S.

Siete de las preguntas de refinamiento están clasificadas como **bloqueantes para desarrollo** (RF-001, RF-003, RF-004, RF-012, RF-013, RF-014, RF-015) y afectan al sprint 11. Como dependen de terceros —el propietario, el contador y el proveedor de comprobantes— conviene moverlas ahora y no cuando el sprint esté encima.
