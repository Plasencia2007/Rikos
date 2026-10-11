<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 2537-2737. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

### EP-10 — Continuidad: offline, sincronización, respaldo y seguridad

---

### [US-058] Operar el local completo sin conexión a Internet

- **Épica/Módulo:** EP-10 — Continuidad
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Todos los roles operativos del local
- **Historia de usuario:**
  - Como personal del local,
  - quiero seguir operando cuando se cae Internet,
  - para no detener la atención, el cobro ni la carga de consumos de los pensionistas.
- **Descripción funcional:** Operación local completa sin Internet: autenticación disponible localmente, POS, pensionistas, impresora, caja, inventario, producción y delivery local. Se conserva una copia local del saldo necesario para operar pensionistas. El portal externo, la API fiscal y la ubicación remota pueden quedar temporalmente indisponibles.
- **Reglas de negocio asociadas:** RN-086, RN-088, RN-093.
- **Criterios de aceptación:**
  - **CA1 — Módulos operables sin Internet**
    - Given un local sin conexión a Internet
    - When el personal opera
    - Then deben funcionar la autenticación local, el POS, la gestión de pensionistas, la impresora, la caja, el inventario, la producción y el delivery local.
  - **CA2 — Saldo local del pensionista**
    - Given un local sin conexión
    - When un pensionista consume
    - Then el sistema debe usar la copia local del saldo necesaria para operar y registrar el cargo al cerrar en caja.
  - **CA3 — Servicios externos degradados**
    - Given una caída de Internet
    - When el portal, la API fiscal o la ubicación remota no responden
    - Then el sistema debe continuar la operación local y encolar lo que corresponda.
  - **CA4 — Continuidad tras la caída**
    - Given una caída de Internet durante el servicio
    - When se restablece la conexión
    - Then el local debe haber operado sin interrupciones y quedar listo para sincronizar.
- **Dependencias:** US-002, US-018, US-028, US-030, US-033, US-037, US-043.
- **Datos o entidades involucradas:** copia local de saldo, cola de comprobantes, Pedido, Comanda, TurnoCaja, MovimientoInventario.
- **Riesgos, supuestos o pendientes:** la operación offline debe probarse con el hardware real (`07` § Validaciones externas) — ver RF-016. La habilitación de la autenticación local para credenciales nuevas durante una caída no está documentada — ver RF-022.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` §§ Offline-first, Restricción tecnológica; `07-criterios-de-aceptacion-y-pendientes.md` § Casos críticos (16); `README.md` § Alcance confirmado.
- **Estimación inicial:** XL

---

### [US-059] Sincronizar sin duplicados y resolver conflictos

- **Épica/Módulo:** EP-10 — Continuidad
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Sistema / Administrador central (supervisión)
- **Historia de usuario:**
  - Como administrador central,
  - quiero que al volver Internet todo se sincronice sin duplicar ni perder transacciones,
  - para que la información central sea confiable después de cualquier caída.
- **Descripción funcional:** Sincronización al restablecerse la conexión, con identificadores únicos e idempotencia para prevenir duplicados y una resolución de conflictos que no pierde transacciones y deja registro de la resolución aplicada.
- **Reglas de negocio asociadas:** RN-087, RN-075, RN-086.
- **Criterios de aceptación:**
  - **CA1 — Sin duplicados**
    - Given pedidos, cargos y comprobantes generados sin conexión
    - When se restablece Internet y se sincroniza
    - Then el sistema no debe duplicar pedidos, cargos ni comprobantes.
  - **CA2 — Idempotencia**
    - Given una sincronización interrumpida y reintentada
    - When se ejecuta de nuevo
    - Then el resultado debe ser el mismo que con un único envío exitoso.
  - **CA3 — Resolución de conflictos**
    - Given operaciones en conflicto entre el local y el central
    - When se sincronizan
    - Then el sistema debe resolverlas sin perder ninguna transacción
    - And debe dejar registro de la resolución aplicada.
  - **CA4 — Cuenta de pensionista consistente**
    - Given consumos offline en más de un local sobre la misma cuenta
    - When se sincronizan
    - Then el saldo consolidado debe reflejar todos los movimientos con su local de origen.
- **Dependencias:** US-058, US-037, US-018.
- **Datos o entidades involucradas:** identificadores únicos de transacción, cola de sincronización, registro de resolución de conflictos, MovimientoCuenta, Pedido, ComprobanteElectronico.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — las reglas exactas de resolución de conflictos entre operaciones offline de varios locales (`07` § Pendientes funcionales). Ver RF-013; sin esa definición, CA3 no puede especificarse por completo.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Offline-first; `03-pos-caja-cocina-delivery.md` § Facturación electrónica; `07-criterios-de-aceptacion-y-pendientes.md` §§ Casos críticos (16), Pendientes funcionales.
- **Estimación inicial:** XL

---

### [US-060] Respaldar la información en tiempo real

- **Épica/Módulo:** EP-10 — Continuidad
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador central,
  - quiero un respaldo central en tiempo real con copias recuperables,
  - para no perder información ante una falla y poder restaurarla.
- **Descripción funcional:** Respaldo central en tiempo real más copias recuperables de la información del sistema.
- **Reglas de negocio asociadas:** RN-089, RN-093.
- **Criterios de aceptación:**
  - **CA1 — Respaldo en tiempo real**
    - Given operaciones registradas en cualquier local con conexión
    - When se confirman
    - Then deben quedar respaldadas centralmente en tiempo real.
  - **CA2 — Copias recuperables**
    - Given un respaldo existente
    - When se necesita restaurar información
    - Then el sistema debe permitir recuperarla desde las copias.
- **Dependencias:** US-059.
- **Datos o entidades involucradas:** todas las entidades transaccionales y sus adjuntos.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — la política de retención de respaldos y adjuntos (`07`). Ver RF-011.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Respaldo y seguridad; `README.md` § Alcance confirmado.
- **Estimación inicial:** L

---

### [US-061] Proteger credenciales, sesiones y adjuntos

- **Épica/Módulo:** EP-10 — Continuidad
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador central,
  - quiero que las credenciales nunca sean legibles y que los adjuntos estén protegidos por permisos,
  - para resguardar los datos sensibles de usuarios, pensionistas y proveedores.
- **Descripción funcional:** Medidas transversales de seguridad: contraseñas y PIN nunca almacenados ni mostrados en texto legible, separación del PIN presencial y la contraseña del portal, bloqueo temporal tras intentos fallidos de PIN, protección de adjuntos de pagos y compras según permisos, y registro de sesiones y acciones sensibles.
- **Reglas de negocio asociadas:** RN-090, RN-091, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Credenciales no legibles**
    - Given cualquier pantalla, exportación o reporte
    - When se muestran datos de usuarios o pensionistas
    - Then las contraseñas y los PIN nunca deben aparecer en texto legible.
  - **CA2 — Credenciales separadas**
    - Given un pensionista
    - When se gestionan sus accesos
    - Then el PIN presencial y la contraseña del portal deben mantenerse como credenciales distintas.
  - **CA3 — Bloqueo temporal**
    - Given intentos fallidos consecutivos de PIN
    - When se alcanza el umbral configurado
    - Then el sistema debe bloquear temporalmente el acceso.
  - **CA4 — Adjuntos protegidos**
    - Given adjuntos de pagos o compras
    - When accede un usuario sin el permiso correspondiente
    - Then el sistema debe impedir el acceso.
  - **CA5 — Registro de sesiones y acciones sensibles**
    - Given una sesión o una acción sensible
    - When ocurre
    - Then el sistema debe registrarla.
- **Dependencias:** US-002, US-003, US-016, US-022, US-023, US-050, US-006.
- **Datos o entidades involucradas:** Usuario, Pensionista, credenciales, adjuntos de Abono y Compra, registro de sesiones y auditoría.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — plazo de bloqueo por intentos fallidos y método de recuperación de acceso (`07`). Ver RF-006.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Respaldo y seguridad; `02-pensionistas.md` § Ficha.
- **Estimación inicial:** M

---


---

### [US-062] Entregar el comprobante al cliente por correo

- **Épica/Módulo:** EP-06 — Caja y facturación
- **Prioridad:** Should
- **Release sugerido:** Post-MVP · **Sprint:** 11
- **Actor:** Sistema / Cajero (reenvío manual)
- **Historia de usuario:**
  - Como cliente que pidió su comprobante,
  - quiero recibirlo en mi correo apenas esté aceptado,
  - para no depender de un papel ni de volver al local.
- **Descripción funcional:** al pasar el comprobante a aceptado, el sistema descarga su PDF y XML del proveedor, los guarda en el almacenamiento propio y los envía por correo al cliente desde el dominio del restaurante. El envío es opcional y a pedido para el cliente regular: solo se pide el correo cuando el cliente dice que lo quiere.
- **Reglas de negocio asociadas:** RN-035, RN-029, RN-085, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Disparo al aceptar**
    - Given un comprobante que pasa al estado aceptado
    - When se confirma ese cambio de estado
    - Then el sistema debe enviar el correo con el PDF y el XML adjuntos
    - And no debe enviarlo mientras el comprobante esté solo registrado.
  - **CA2 — Exactamente una vez**
    - Given un comprobante ya notificado
    - When el proceso de reconsulta vuelve a encontrarlo aceptado
    - Then el sistema no debe enviar un segundo correo.
  - **CA3 — Comprobante rechazado**
    - Given un comprobante rechazado por SUNAT
    - When se procesa su estado
    - Then el sistema no debe enviar ningún correo al cliente.
  - **CA4 — Rebote**
    - Given un correo que rebota
    - When se detecta
    - Then el comprobante debe quedar marcado en la bandeja para revisión
    - And debe permitir corregir la dirección y reenviar.
  - **CA5 — Reenvío manual**
    - Given un cliente que reclama no haber recibido su comprobante
    - When el cajero lo busca y solicita el reenvío
    - Then el sistema debe volver a enviarlo sin alterar el estado fiscal.
  - **CA6 — Pensionistas excluidos**
    - Given un consumo de pensionista
    - When se emite su comprobante
    - Then el sistema no debe enviar correo por ese consumo
    - And el comprobante debe quedar disponible en el portal.
- **Dependencias:** US-037, US-022.
- **Datos o entidades involucradas:** ComprobanteElectronico (estado de entrega, marca de correo enviado), Pensionista, Cliente regular, almacenamiento de archivos.
- **Riesgos, supuestos o pendientes:** CA6 se apoya en RN-029, que prohíbe notificar por cada consumo. Si el propietario quisiera lo contrario, debe ser una preferencia en la ficha del pensionista, apagada por defecto, y hay que modificar esa regla. **Supuesto de análisis:** el envío por correo no figura en ninguno de los ocho documentos de especificación; es una decisión funcional posterior del equipo.
- **Trazabilidad:** decisión técnica del equipo (DT-006 y DT-007); se apoya en `02-pensionistas.md` §§ Portal compartido, Facturación y en `05-usuarios-reportes-multilocal-offline.md` § Respaldo y seguridad.
- **Estimación inicial:** M


---

