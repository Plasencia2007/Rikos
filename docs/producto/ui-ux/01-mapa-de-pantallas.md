<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 2866-3012. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

# 6. Mapa de pantallas

Inventario completo de las vistas del sistema, agrupadas por superficie. Cada una indica las historias que la sustentan y las reglas que condicionan su diseño.

Las cinco superficies y dónde se sirve cada una están definidas en DT-021 y DT-029.

## 7.1 POS — equipo del local

Pantalla completa. Se usa de pie, con una mano, con prisa. *(Actualizado 2026-09-30: el POS lleva una barra lateral colapsable; ver `03-pos-rediseno.md` § 1. Antes decía "sin barra lateral".)*

| Código | Pantalla | Qué resuelve | Historias |
|---|---|---|---|
| POS-01 | Bloqueo y PIN de mozo | Identificación rápida en dispositivo compartido. Es pantalla de bloqueo, no login: aparece cada vez que cambia de manos | US-003 |
| POS-02 | Mapa de mesas | Estado de cada mesa: libre, ocupada, unida, pendiente de cierre, bloqueada | US-027 |
| POS-03 | Toma de pedido | Asignación de cada producto a una persona de la mesa. **La pantalla más difícil del sistema** | US-028, US-020 |
| POS-04 | Selección de opciones del día | Cuando el componente del menú tiene varias opciones programadas; si hay una sola, se asigna directo | US-013, US-028 |
| POS-05 | Gestión de cuentas | Unir mesas, mover pedidos, dividir por productos, trasladar entre personas | US-029 |
| POS-06 | Anulación de producto | Con motivo obligatorio. El efecto en inventario depende de si ya se comandó | US-031 |
| POS-07 | Envío y reimpresión de comanda | Toda reimpresión se marca como copia | US-030 |
| POS-08 | Cobro | División libre entre efectivo, Yape, Plin y tarjeta. Cierres parciales sin liberar la mesa | US-033 |
| POS-09 | Descuento con autorización | Permiso específico y motivo | US-034 |
| POS-10 | Apertura de turno | Fondo inicial | US-032 |
| POS-11 | Cierre de turno | Efectivo esperado visible antes de contar. Alerta desde S/0.01 sin bloquear | US-036 |
| POS-12 | Registro de abono | Efectivo confirmado al instante; digital queda pendiente | US-021 |
| POS-13 | Consulta de pensionista | Saldo, límite y crédito disponible antes de cerrar | US-018 |
| POS-14 | Vinculación de pre-ticket | El mozo teclea el código del papel. Impide doble vinculación | US-020 |
| POS-15 | Registro de recojo programado | Fecha y hora futura, quién recoge, su celular | US-038 |
| POS-16 | Registro de delivery | Dirección, teléfono y asignación de motorizado | US-040 |
| POS-17 | Marcado de agotado | Oculta el producto en pedidos nuevos sin tocar comandas emitidas | US-015 |
| POS-18 | Reapertura de venta | Solo con turno abierto y permiso de reabrir | US-035 |

## 7.2 Kiosco — equipo del local

Pantalla fija con impresora. La usa el pensionista sin ayuda.

| Código | Pantalla | Qué resuelve | Historias |
|---|---|---|---|
| KIO-01 | Ingreso de DNI | Punto de entrada. Teclado grande, sin distracciones | US-019 |
| KIO-02 | Ingreso de PIN | Con bloqueo temporal tras intentos fallidos | US-019 |
| KIO-03 | Confirmación e impresión | El servicio se determina solo por horario. No hay selección de platos | US-019, US-007 |
| KIO-04 | Estados de error | Pensionista desactivado, PIN bloqueado, fuera de horario de servicio | US-019, US-026 |

## 7.3 Portal — nube, dominio propio

Lo usan el pensionista y su responsable con un mismo acceso. Solo consulta.

| Código | Pantalla | Qué resuelve | Historias |
|---|---|---|---|
| POR-01 | Ingreso | DNI y contraseña del portal. Credenciales distintas de las internas | US-022 |
| POR-02 | Resumen de cuenta | Saldo, deuda, límite y crédito disponible | US-022, US-018 |
| POR-03 | Movimientos | Consumos, abonos y ajustes, con el local de cada uno | US-022 |
| POR-04 | Comprobantes | Boletas de cada consumo, servidas desde el almacenamiento propio | US-022, US-037 |
| POR-05 | Estados mensuales | El PDF fijo conservado en cada cierre | US-025 |
| POR-06 | Reportar abono | Imagen del comprobante, número de operación, monto, fecha, medio y cuenta | US-023 |
| POR-07 | Notificaciones | Solo abono aprobado o rechazado y estado mensual generado. Nunca por consumo | US-022 |

## 7.4 Motorizado — nube, celular

Se usa en la calle, con datos móviles, a veces con una mano y el casco puesto.

| Código | Pantalla | Qué resuelve | Historias |
|---|---|---|---|
| MOT-01 | Ingreso | Usuario y contraseña. Es personal interno | US-002 |
| MOT-02 | Mis entregas | Lista de pedidos asignados, ordenada | US-041 |
| MOT-03 | Detalle y cambio de estado | Dirección, teléfono, y los estados aceptado, recogido, en ruta, entregado | US-041 |
| MOT-04 | Incidencia o entrega fallida | Registro del problema para la liquidación | US-041, US-042 |

## 7.5 Panel del administrador — nube

Barra lateral con las secciones de DT-026. Se usa sentado, en una PC.

### Operación

| Código | Pantalla | Qué resuelve | Historias |
|---|---|---|---|
| ADM-01 | Inicio | Ventas y rentabilidad, caja y diferencias, pensionistas y saldos, inventario y costos. Con filtros por día, semana, mes, turno, local, canal, producto, plan y tipo de cliente | US-052 |
| ADM-02 | Caja y turnos | Cierres, diferencias y detalle por turno | US-054, US-036 |
| ADM-03 | Delivery y recojo | Seguimiento de estados, incidencias y liquidación por motorizado | US-042, US-057 |

### Pensionistas

| Código | Pantalla | Qué resuelve | Historias |
|---|---|---|---|
| ADM-04 | Listado de pensionistas | Con deuda, saldo a favor y crédito disponible | US-016, US-055 |
| ADM-05 | Ficha del pensionista | Datos, responsable obligatorio, plan, límite, credenciales | US-016 |
| ADM-06 | Cuenta corriente | Movimientos del pensionista y su saldo consolidado multilocal | US-018 |
| ADM-07 | Abonos pendientes | Bandeja de aprobación con el comprobante adjunto | US-024 |
| ADM-08 | Cierre mensual | Generación del periodo y su PDF fijo | US-025 |
| ADM-09 | Planes tarifarios | Precios por servicio y producto, con vigencia e historial | US-017 |

### Menú y catálogo

| Código | Pantalla | Qué resuelve | Historias |
|---|---|---|---|
| ADM-10 | Programación semanal | Semana por día y servicio, con estados y porciones estimadas. Advierte repetición de segundos | US-013 |
| ADM-11 | Menú normal y extra | Composición y precios | US-014 |
| ADM-12 | Productos | Catálogo central con su clasificación | US-008 |
| ADM-13 | Unidades y presentaciones | Unidad base y factores de conversión | US-009 |
| ADM-14 | Recetas y subrecetas | Composición, rendimiento, porciones y costo por porción | US-010 |
| ADM-15 | Disponibilidad | Productos agotados por local y su reactivación | US-015 |

### Inventario y compras

| Código | Pantalla | Qué resuelve | Historias |
|---|---|---|---|
| ADM-16 | Existencias | Stock por local, mínimos y alertas de negativo | US-043 |
| ADM-17 | Kardex | Movimientos con su origen rastreable | US-044 |
| ADM-18 | Ajustes | Con motivo de lista configurable y observación | US-045 |
| ADM-19 | Transferencias entre locales | Dos ajustes enlazados | US-046 |
| ADM-20 | Conteo físico | Snapshot, captura y conciliación sin detener la operación | US-047 |
| ADM-21 | Producción | Subrecetas producidas, porciones planificadas, preparadas y vendidas | US-048 |
| ADM-22 | Sobrantes | Destino: conservar, personal, pérdida, reutilización u otro | US-049 |
| ADM-23 | Compras | Documento, proveedor, presentaciones y adjunto | US-050 |
| ADM-24 | Proveedores | Catálogo | US-011 |
| ADM-25 | Consumo interno | Comida del personal, separada de las ventas | US-051 |

### Facturación

| Código | Pantalla | Qué resuelve | Historias |
|---|---|---|---|
| ADM-26 | Bandeja de comprobantes | Pendientes, rechazados y correos rebotados, con reintento | US-037 |
| ADM-27 | Detalle del comprobante | Estado fiscal, estado de entrega, XML, PDF y CDR | US-037 |

### Administración

| Código | Pantalla | Qué resuelve | Historias |
|---|---|---|---|
| ADM-28 | Locales | Alta y desactivación | US-001 |
| ADM-29 | Usuarios | Datos, rol, excepciones y asignación a locales | US-005 |
| ADM-30 | Roles y permisos | Matriz por módulo y acción | US-004 |
| ADM-31 | Bitácora de auditoría | Filtrable por local, usuario, fecha y tipo de evento | US-006 |
| ADM-32 | Horarios de servicio | Rangos de desayuno, almuerzo y cena | US-007 |
| ADM-33 | Motivos configurables | Ajuste, anulación y rechazo de pagos | US-012 |

### Reportes

| Código | Pantalla | Qué resuelve | Historias |
|---|---|---|---|
| ADM-34 | Ventas y rentabilidad | Por local, canal, servicio, producto y usuario, con costo histórico | US-053 |
| ADM-35 | Pensionistas y abonos | Cartera, estados mensuales y abonos por estado | US-055 |
| ADM-36 | Inventario y producción | Existencias, Kardex, alertas y planificado contra vendido | US-056 |
| ADM-37 | Operativos | Delivery, productos anulados tras comanda y consumo interno | US-057 |

**Total: 70 pantallas** — 18 en POS, 4 en kiosco, 7 en portal, 4 en motorizado y 37 en el panel.

---

