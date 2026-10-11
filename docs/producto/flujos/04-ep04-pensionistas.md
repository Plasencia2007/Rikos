<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 870-1331. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

### EP-04 — Pensionistas: cuenta corriente, autoservicio, portal y abonos

---

### [US-016] Registrar la ficha del pensionista y su responsable

- **Épica/Módulo:** EP-04 — Pensionistas
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central / Administrador de local
- **Historia de usuario:**
  - Como administrador,
  - quiero registrar la ficha completa del pensionista junto con su responsable obligatorio,
  - para poder abrirle una cuenta corriente identificada y contactable.
- **Descripción funcional:** Alta y mantenimiento del pensionista con sus datos personales, académicos, plan tarifario, límite de crédito, frecuencia habitual de abono, PIN presencial, contraseña del portal y estado. El responsable es obligatorio. El DNI del pensionista es único en la empresa.
- **Reglas de negocio asociadas:** RN-036, RN-090, RN-007, RN-021.
- **Criterios de aceptación:**
  - **CA1 — Campos del pensionista**
    - Given el alta de un pensionista
    - When se guarda la ficha
    - Then el sistema debe registrar DNI, nombres y apellidos, teléfono, correo, universidad o instituto, código de estudiante, carrera, ciclo o semestre opcional, plan tarifario, límite de crédito, frecuencia habitual de abono, PIN presencial, contraseña del portal y estado activo/inactivo.
  - **CA2 — Responsable obligatorio**
    - Given el alta de un pensionista
    - When se intenta guardar sin responsable
    - Then el sistema debe impedir el registro
    - And debe exigir nombre completo, DNI, teléfono, correo, parentesco o relación y dirección del responsable.
  - **CA3 — DNI único**
    - Given un DNI ya registrado para otro pensionista de la empresa
    - When se intenta registrar de nuevo
    - Then el sistema debe rechazar el alta.
  - **CA4 — Credenciales separadas y no legibles**
    - Given la ficha guardada
    - When se consulta
    - Then el sistema no debe mostrar el PIN ni la contraseña del portal en texto legible
    - And debe mantenerlos como credenciales distintas entre sí.
  - **CA5 — Apertura de cuenta**
    - Given un pensionista dado de alta
    - When se confirma el registro
    - Then el sistema debe crear su cuenta corriente asociada.
- **Dependencias:** US-017 (plan tarifario), US-004.
- **Datos o entidades involucradas:** Pensionista, Responsable, CuentaPensionista, PlanTarifario.
- **Riesgos, supuestos o pendientes:** los campos opcionales adicionales de clientes están pendientes (`07`) — ver RF-009. El proceso de asignación y recuperación de la contraseña del portal no está documentado — ver RF-027.
- **Trazabilidad:** `02-pensionistas.md` § Ficha; `06-modelo-de-datos.md` §§ Pensionistas, Reglas de integridad esenciales; `05-usuarios-reportes-multilocal-offline.md` § Respaldo y seguridad.
- **Estimación inicial:** M

---

### [US-017] Administrar planes tarifarios y precios especiales

- **Épica/Módulo:** EP-04 — Pensionistas
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador central,
  - quiero configurar planes tarifarios con vigencia e historial y asignarlos a los pensionistas,
  - para cobrar a cada estudiante el precio que le corresponde sin alterar lo ya consumido.
- **Descripción funcional:** Mantenimiento de planes tarifarios configurables y asignables. Cada plan define el precio del almuerzo normal y precios especiales por producto fijo de desayuno y cena. Tienen vigencia, estado e historial de modificaciones. Los consumos anteriores conservan la tarifa aplicada. El menú extra nunca usa tarifa reducida.
- **Reglas de negocio asociadas:** RN-021, RN-022, RN-023, RN-001, RN-004.
- **Criterios de aceptación:**
  - **CA1 — Definición del plan**
    - Given la creación de un plan tarifario
    - When se configura
    - Then el sistema debe permitir definir el precio del almuerzo normal y precios especiales por producto fijo de desayuno y cena.
  - **CA2 — Vigencia e historial**
    - Given un plan con precios definidos
    - When se modifica
    - Then el sistema debe conservar el historial de modificaciones y la vigencia de cada versión.
  - **CA3 — Consumos anteriores intactos**
    - Given consumos registrados con una tarifa previa
    - When el plan cambia de precio
    - Then el sistema debe conservar la tarifa aplicada en esos consumos.
  - **CA4 — Menú extra a precio completo**
    - Given un pensionista con plan tarifario asignado
    - When consume un menú extra
    - Then el sistema debe cobrar el precio completo del extra sin aplicar tarifa reducida.
  - **CA5 — Alcance central**
    - Given un plan configurado
    - When se usa en cualquier local
    - Then el sistema debe aplicar el mismo plan y precios.
- **Dependencias:** US-008, US-006.
- **Datos o entidades involucradas:** PlanTarifario, PrecioPlan (por servicio/producto), Pensionista, vigencia, estado.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — precios definitivos, planes y vigencias (`07`). No se documenta qué precio aplica al pensionista para productos que el plan no contempla (por ejemplo extras o postres sueltos) — ver RF-019.
- **Trazabilidad:** `02-pensionistas.md` § Planes tarifarios; `06-modelo-de-datos.md` § Pensionistas; `07-criterios-de-aceptacion-y-pendientes.md` § Pendientes funcionales.
- **Estimación inicial:** L

---

### [US-018] Mantener la cuenta corriente del pensionista

- **Épica/Módulo:** EP-04 — Pensionistas
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador / Cajero (consulta y registro)
- **Historia de usuario:**
  - Como administrador,
  - quiero que cada pensionista tenga una cuenta corriente única con cargos, abonos, ajustes y saldos,
  - para conocer en todo momento su deuda, su saldo a favor y su crédito disponible en cualquier local.
- **Descripción funcional:** Cuenta corriente única por pensionista, utilizable en todos los locales, que acumula cargos, abonos, ajustes, saldo a favor, deuda y crédito disponible. El cargo se genera únicamente al cerrar el consumo en caja. El saldo a favor se aplica primero y el remanente aumenta la deuda. El límite de crédito es preventivo. Cada movimiento identifica el local donde ocurrió.
- **Reglas de negocio asociadas:** RN-003, RN-015, RN-016, RN-017, RN-018, RN-019, RN-008.
- **Criterios de aceptación:**
  - **CA1 — Cargo solo al cerrar en caja**
    - Given un pedido de pensionista tomado y comandado
    - When aún no se cierra en caja
    - Then el sistema no debe registrar ningún cargo en su cuenta.
  - **CA2 — Cálculo del crédito disponible**
    - Given una cuenta con límite individual y deuda pendiente
    - When se consulta el crédito disponible
    - Then el sistema debe mostrar el resultado de límite individual menos deuda pendiente.
  - **CA3 — Aplicación del saldo a favor**
    - Given una cuenta con saldo a favor menor que el consumo
    - When se cierra el consumo
    - Then el sistema debe aplicar primero el saldo a favor
    - And debe aumentar la deuda solo por el remanente.
  - **CA4 — Límite preventivo**
    - Given un consumo que hace superar el límite individual
    - When el cajero cierra la cuenta
    - Then el sistema debe mostrar una alerta
    - And debe permitir completar el cierre.
  - **CA5 — Varios consumos del mismo servicio**
    - Given un pensionista que ya consumió el almuerzo del día
    - When registra un segundo consumo del mismo servicio ese mismo día
    - Then el sistema debe permitirlo y generar un cargo adicional.
  - **CA6 — Cuenta única multilocal**
    - Given un pensionista que consume en un local distinto al habitual
    - When se consulta su cuenta
    - Then el sistema debe mostrar una sola cuenta con los movimientos de todos los locales
    - And debe indicar el local de cada movimiento.
- **Dependencias:** US-016, US-017, US-033 (cierre en caja que genera el cargo).
- **Datos o entidades involucradas:** CuentaPensionista, MovimientoCuenta (consumo, abono, ajuste, arrastre), Local, límite de crédito, saldo a favor, deuda.
- **Riesgos, supuestos o pendientes:** el tratamiento tributario de las ventas a crédito debe validarse con el contador (`07` § Validaciones externas) — ver RF-014.
- **Trazabilidad:** `02-pensionistas.md` § Cuenta corriente; `01-alcance-y-reglas.md` §§ Canales, Multilocal; `06-modelo-de-datos.md` § Pensionistas; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (3, 4, 20).
- **Estimación inicial:** L

---

### [US-019] Emitir el pre-ticket de autoservicio del pensionista

- **Épica/Módulo:** EP-04 — Pensionistas
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Pensionista
- **Historia de usuario:**
  - Como pensionista,
  - quiero identificarme con mi DNI y PIN y obtener un pre-ticket impreso,
  - para que el mozo me atienda sin que yo tenga que elegir platos en el kiosco.
- **Descripción funcional:** Terminal de autoservicio donde el pensionista ingresa DNI y PIN corto; el sistema determina el servicio por horario configurable e imprime un pre-ticket sin selección de platos, con código único de un solo uso. Estados: pendiente, vinculado, cerrado, vencido y anulado.
- **Reglas de negocio asociadas:** RN-024, RN-025, RN-026, RN-027, RN-094, RN-034, RN-091.
- **Criterios de aceptación:**
  - **CA1 — Emisión del pre-ticket**
    - Given un pensionista activo que ingresa DNI y PIN correctos dentro del horario de un servicio
    - When confirma
    - Then el sistema debe determinar automáticamente el servicio e imprimir un pre-ticket en estado pendiente, sin selección de platos.
  - **CA2 — Contenido obligatorio**
    - Given un pre-ticket impreso
    - When se revisa
    - Then debe mostrar código único, pensionista, servicio, fecha y hora y la leyenda de que no es un consumo cargado.
  - **CA3 — Sin efecto en la cuenta**
    - Given un pre-ticket emitido
    - When se consulta la cuenta corriente del pensionista
    - Then el sistema no debe mostrar ningún cargo asociado a ese pre-ticket.
  - **CA4 — Vencimiento**
    - Given un pre-ticket en estado pendiente
    - When termina el horario del servicio
    - Then el sistema debe marcarlo como vencido
    - And no debe generar ningún cargo.
  - **CA5 — Pensionista desactivado**
    - Given un pensionista desactivado
    - When intenta emitir un pre-ticket
    - Then el sistema debe impedirlo.
  - **CA6 — Bloqueo por intentos fallidos**
    - Given intentos fallidos consecutivos de PIN
    - When se alcanza el umbral configurado
    - Then el sistema debe bloquear temporalmente el intento.
- **Dependencias:** US-016, US-007, US-058 (debe funcionar sin Internet).
- **Datos o entidades involucradas:** PreTicket (código, pensionista, servicio, horario, estado, pedido vinculado), Pensionista, configuración de horarios.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — dispositivo e impresora del kiosco (`07`), horarios exactos (`07`) y plazo de bloqueo por intentos fallidos (`07`). Ambigüedad: la especificación permite varios consumos del mismo servicio en un día pero no aclara si se pueden emitir varios pre-tickets pendientes del mismo servicio simultáneamente — ver RF-018.
- **Trazabilidad:** `02-pensionistas.md` § Autoservicio presencial; `06-modelo-de-datos.md` §§ Pensionistas, Reglas de integridad esenciales; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (1, 2).
- **Estimación inicial:** L

---

### [US-020] Vincular el pre-ticket al pedido del pensionista

- **Épica/Módulo:** EP-04 — Pensionistas
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Mozo
- **Historia de usuario:**
  - Como mozo,
  - quiero ingresar manualmente el código del pre-ticket que me entrega el pensionista y vincularlo a su pedido,
  - para asociar correctamente lo que consume con su cuenta y su mesa.
- **Descripción funcional:** El mozo recibe el papel, ingresa manualmente el código único del pre-ticket y vincula pensionista, mesa o persona y productos. El sistema impide la doble vinculación y rechaza pre-tickets vencidos o anulados.
- **Reglas de negocio asociadas:** RN-025, RN-026, RN-027, RN-094, RN-060.
- **Criterios de aceptación:**
  - **CA1 — Vinculación exitosa**
    - Given un pre-ticket en estado pendiente
    - When el mozo ingresa su código y lo asocia a la mesa o persona y a los productos elegidos
    - Then el sistema debe cambiar el estado del pre-ticket a vinculado
    - And debe asociar el consumo al pensionista correspondiente.
  - **CA2 — Doble vinculación impedida**
    - Given un pre-ticket ya vinculado
    - When se intenta usar el mismo código en otro pedido
    - Then el sistema debe rechazar la operación.
  - **CA3 — Pre-ticket vencido o anulado**
    - Given un pre-ticket vencido o anulado
    - When el mozo intenta vincularlo
    - Then el sistema debe rechazar la operación e informar el estado.
  - **CA4 — Registro de opciones**
    - Given un menú con varias opciones programadas
    - When el mozo registra la elegida por el pensionista
    - Then el sistema debe guardar esa opción en el detalle del pedido.
  - **CA5 — Estado tras el cierre**
    - Given un pre-ticket vinculado cuyo pedido se cierra en caja
    - When se confirma el cierre
    - Then el sistema debe cambiar su estado a cerrado.
- **Dependencias:** US-019, US-028, US-013.
- **Datos o entidades involucradas:** PreTicket, Pedido, CuentaPersona, DetallePedido, Pensionista, Mesa.
- **Riesgos, supuestos o pendientes:** ninguno adicional a RF-018.
- **Trazabilidad:** `02-pensionistas.md` § Autoservicio presencial; `03-pos-caja-cocina-delivery.md` § POS y mesas; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (1, 7).
- **Estimación inicial:** M

---

### [US-021] Registrar abonos del pensionista en caja

- **Épica/Módulo:** EP-04 — Pensionistas
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Cajero
- **Historia de usuario:**
  - Como cajero,
  - quiero registrar los abonos que hace el pensionista o su responsable,
  - para actualizar su saldo y que el dinero entre correctamente en la conciliación del turno.
- **Descripción funcional:** Registro de abonos en efectivo, Yape, Plin y transferencia bancaria. El efectivo queda confirmado de inmediato y entra en la conciliación del turno; los digitales quedan pendientes de aprobación. Se permiten abonos parciales. El abono no genera una venta.
- **Reglas de negocio asociadas:** RN-030, RN-031, RN-032, RN-033, RN-069, RN-035.
- **Criterios de aceptación:**
  - **CA1 — Abono en efectivo**
    - Given un pensionista con cuenta activa
    - When el cajero registra un abono en efectivo
    - Then el sistema debe confirmarlo inmediatamente, actualizar el saldo e incluirlo en la conciliación del turno.
  - **CA2 — Abono digital desde caja**
    - Given un abono por Yape, Plin o transferencia registrado por caja
    - When se guarda
    - Then el sistema debe dejarlo en estado pendiente
    - And no debe modificar el saldo hasta su aprobación.
  - **CA3 — Abono parcial**
    - Given una deuda pendiente
    - When se registra un abono por un monto menor a la deuda
    - Then el sistema debe aceptarlo y actualizar la deuda restante según su estado.
  - **CA4 — No genera venta**
    - Given un abono registrado
    - When se revisan las ventas del turno
    - Then el sistema no debe generar una venta por el abono
    - And puede generar una constancia interna.
  - **CA5 — Excepción de caja**
    - Given la restricción de no permitir ingresos extraordinarios en caja
    - When se registra un abono en efectivo de pensionista
    - Then el sistema debe permitirlo como única excepción documentada.
- **Dependencias:** US-018, US-032.
- **Datos o entidades involucradas:** Abono (medio, monto, estado, adjunto, aprobador), MovimientoCuenta, MovimientoCaja, TurnoCaja, MedioPago.
- **Riesgos, supuestos o pendientes:** el tratamiento tributario de abonos debe validarse con el contador — ver RF-014.
- **Trazabilidad:** `02-pensionistas.md` §§ Abonos, Facturación; `03-pos-caja-cocina-delivery.md` § Caja; `06-modelo-de-datos.md` §§ Pensionistas, Caja.
- **Estimación inicial:** M

---

### [US-022] Consultar el portal compartido del pensionista

- **Épica/Módulo:** EP-04 — Pensionistas
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Pensionista y responsable (acceso compartido)
- **Historia de usuario:**
  - Como pensionista o responsable,
  - quiero ingresar a un portal con DNI y contraseña para ver movimientos, saldos y comprobantes,
  - para hacer seguimiento de los consumos y de la deuda sin tener que preguntar en el local.
- **Descripción funcional:** Portal web de consulta con un único acceso compartido por pensionista y responsable. Permite ver movimientos, saldo, límite, crédito disponible, comprobantes y estados mensuales, y recibir notificaciones internas acotadas. No permite modificar consumos ni aprobar abonos.
- **Reglas de negocio asociadas:** RN-028, RN-029, RN-088, RN-090, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Acceso compartido**
    - Given un pensionista con contraseña de portal
    - When el pensionista o su responsable ingresa con DNI y esa contraseña
    - Then el sistema debe dar acceso a la misma información de la cuenta.
  - **CA2 — Contenido de consulta**
    - Given una sesión activa en el portal
    - When se consulta la cuenta
    - Then el sistema debe mostrar movimientos, saldo, límite, crédito disponible, comprobantes y estados mensuales.
  - **CA3 — Alcance restringido**
    - Given una sesión activa en el portal
    - When se revisan las acciones disponibles
    - Then el sistema no debe permitir modificar consumos ni aprobar abonos.
  - **CA4 — Notificaciones acotadas**
    - Given eventos de la cuenta
    - When se aprueba o rechaza un abono, o se genera un estado mensual
    - Then el sistema debe notificar internamente
    - And no debe notificar por cada consumo ni por superar el límite.
  - **CA5 — Indisponibilidad tolerada**
    - Given una caída de Internet en el local
    - When el portal no está disponible
    - Then la operación del local debe continuar sin verse afectada.
- **Dependencias:** US-018, US-025, US-016.
- **Datos o entidades involucradas:** Pensionista, Responsable, CuentaPensionista, MovimientoCuenta, EstadoCuentaMensual, ComprobanteElectronico.
- **Riesgos, supuestos o pendientes:** el alta y la recuperación de la contraseña del portal no están documentadas — ver RF-027.
- **Trazabilidad:** `02-pensionistas.md` § Portal compartido; `05-usuarios-reportes-multilocal-offline.md` §§ Offline-first, Respaldo y seguridad.
- **Estimación inicial:** L

---

### [US-023] Reportar un abono digital desde el portal

- **Épica/Módulo:** EP-04 — Pensionistas
- **Prioridad:** Should
- **Release sugerido:** Post-MVP
- **Actor:** Pensionista y responsable
- **Historia de usuario:**
  - Como pensionista o responsable,
  - quiero reportar desde el portal un abono digital adjuntando el comprobante,
  - para que el administrador lo apruebe y se refleje en mi saldo.
- **Descripción funcional:** Registro de abonos digitales desde el portal con datos y adjunto obligatorios. Queda en estado pendiente hasta la aprobación del administrador y no modifica el saldo. El sistema valida la posible duplicidad del número de operación.
- **Reglas de negocio asociadas:** RN-031, RN-032, RN-028, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Datos obligatorios**
    - Given el formulario de reporte de abono en el portal
    - When el usuario intenta enviarlo sin imagen de comprobante, número de operación, monto, fecha, medio o cuenta destino
    - Then el sistema debe impedir el envío.
  - **CA2 — Estado pendiente sin efecto en el saldo**
    - Given un abono digital reportado con todos los datos
    - When se envía
    - Then el sistema debe registrarlo en estado pendiente
    - And no debe modificar el saldo del pensionista.
  - **CA3 — Validación de duplicidad**
    - Given un número de operación ya registrado
    - When se reporta otro abono con el mismo número
    - Then el sistema debe advertir la posible duplicidad.
  - **CA4 — Protección del adjunto**
    - Given un comprobante adjunto cargado
    - When un usuario sin permiso intenta consultarlo
    - Then el sistema debe impedir el acceso.
- **Dependencias:** US-022, US-024.
- **Datos o entidades involucradas:** Abono (estado, adjunto, número de operación, medio, cuenta destino, aprobador), CuentaPensionista.
- **Riesgos, supuestos o pendientes:** ninguno documentado más allá de la política de retención de adjuntos (RF-011).
- **Trazabilidad:** `02-pensionistas.md` §§ Abonos, Portal compartido; `05-usuarios-reportes-multilocal-offline.md` § Respaldo y seguridad; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (5).
- **Estimación inicial:** M

---

### [US-024] Aprobar o rechazar abonos digitales

- **Épica/Módulo:** EP-04 — Pensionistas
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador,
  - quiero revisar los abonos digitales pendientes y aprobarlos o rechazarlos,
  - para que solo el dinero verificado modifique el saldo del pensionista.
- **Descripción funcional:** Bandeja de abonos pendientes con su comprobante adjunto y datos. Solo el administrador aprueba o rechaza. La aprobación actualiza el saldo; el rechazo no. Ambas acciones se auditan y notifican internamente al portal.
- **Reglas de negocio asociadas:** RN-031, RN-029, RN-004, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Aprobación exclusiva del administrador**
    - Given un abono pendiente
    - When un usuario sin permiso de aprobar intenta procesarlo
    - Then el sistema debe impedirlo.
  - **CA2 — Aprobación**
    - Given un abono pendiente
    - When el administrador lo aprueba
    - Then el sistema debe actualizar el saldo del pensionista
    - And debe registrar usuario, fecha, hora y valores en la auditoría
    - And debe notificar internamente al portal.
  - **CA3 — Rechazo**
    - Given un abono pendiente
    - When el administrador lo rechaza
    - Then el sistema no debe modificar el saldo
    - And debe registrar el rechazo y notificarlo internamente.
  - **CA4 — Sin efecto mientras esté pendiente**
    - Given un abono en estado pendiente
    - When se consulta el saldo del pensionista
    - Then el saldo no debe incluir ese abono.
- **Dependencias:** US-021, US-023, US-004, US-006.
- **Datos o entidades involucradas:** Abono (estado, aprobador), CuentaPensionista, MovimientoCuenta, registro de auditoría.
- **Riesgos, supuestos o pendientes:** los motivos de rechazo de pagos están pendientes de definición — ver RF-005.
- **Trazabilidad:** `02-pensionistas.md` § Abonos; `01-alcance-y-reglas.md` § Auditoría transversal; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (5).
- **Estimación inicial:** M

---

### [US-025] Generar el cierre mensual y el estado de cuenta

- **Épica/Módulo:** EP-04 — Pensionistas
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador,
  - quiero cerrar el mes de cada pensionista y generar su estado de cuenta,
  - para dejar un corte formal e inalterable y arrastrar el saldo al mes siguiente.
- **Descripción funcional:** Cierre mensual de la cuenta con saldo inicial, consumos, abonos, ajustes y saldo final. Arrastra deuda o saldo a favor al mes siguiente sin marcar atraso automáticamente. El estado mensual cerrado es inalterable, conserva un PDF fijo y es consultable desde el portal.
- **Reglas de negocio asociadas:** RN-006, RN-020, RN-085, RN-029.
- **Criterios de aceptación:**
  - **CA1 — Contenido del cierre**
    - Given un periodo mensual con movimientos
    - When se genera el cierre
    - Then el estado debe mostrar saldo inicial, consumos, abonos, ajustes y saldo final.
  - **CA2 — Arrastre**
    - Given un cierre con deuda o saldo a favor
    - When inicia el mes siguiente
    - Then el sistema debe arrastrar ese saldo como saldo inicial
    - And no debe marcar atraso automáticamente.
  - **CA3 — Inalterabilidad**
    - Given un estado mensual cerrado
    - When se intenta registrar un movimiento retroactivo en ese periodo
    - Then el sistema debe impedirlo.
  - **CA4 — PDF fijo conservado**
    - Given un estado mensual cerrado
    - When se consulta más adelante desde el portal o desde reportes
    - Then el sistema debe entregar el mismo PDF conservado en el cierre.
  - **CA5 — Notificación**
    - Given la generación del estado mensual
    - When se completa
    - Then el sistema debe notificarlo internamente en el portal.
- **Dependencias:** US-018, US-021, US-024.
- **Datos o entidades involucradas:** PeriodoCuenta, EstadoCuentaMensual, MovimientoCuenta, CuentaPensionista.
- **Riesgos, supuestos o pendientes:** el tratamiento tributario de los cierres mensuales debe validarse con el contador — ver RF-014.
- **Trazabilidad:** `02-pensionistas.md` § Cuenta corriente; `05-usuarios-reportes-multilocal-offline.md` § Reportes mínimos; `06-modelo-de-datos.md` §§ Pensionistas, Reglas de integridad esenciales; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (6).
- **Estimación inicial:** L

---

### [US-026] Desactivar y reactivar a un pensionista

- **Épica/Módulo:** EP-04 — Pensionistas
- **Prioridad:** Should
- **Release sugerido:** MVP
- **Actor:** Administrador
- **Historia de usuario:**
  - Como administrador,
  - quiero desactivar a un pensionista y poder reactivarlo después,
  - para cortar el consumo a crédito sin perder su historial ni su deuda.
- **Descripción funcional:** La desactivación bloquea nuevos tickets y consumos, conserva la cuenta, el historial y el acceso de consulta al portal, y permite seguir registrando abonos. La reactivación reutiliza la misma cuenta.
- **Reglas de negocio asociadas:** RN-034, RN-007.
- **Criterios de aceptación:**
  - **CA1 — Bloqueo de consumo**
    - Given un pensionista desactivado
    - When intenta emitir un pre-ticket o se intenta registrar un consumo a su cuenta
    - Then el sistema debe impedirlo.
  - **CA2 — Consulta y abonos posteriores**
    - Given un pensionista desactivado con deuda
    - When ingresa al portal o el cajero registra un abono
    - Then el sistema debe permitir la consulta y el registro del abono.
  - **CA3 — Reactivación sobre la misma cuenta**
    - Given un pensionista desactivado
    - When se reactiva
    - Then el sistema debe reutilizar la misma cuenta con su saldo e historial.
- **Dependencias:** US-016, US-018, US-021.
- **Datos o entidades involucradas:** Pensionista (estado), CuentaPensionista, MovimientoCuenta, PreTicket.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `02-pensionistas.md` § Desactivación; `01-alcance-y-reglas.md` § Reglas de historial.
- **Estimación inicial:** S

---

