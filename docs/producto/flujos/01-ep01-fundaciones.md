<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 291-553. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

### EP-01 — Fundaciones: organización, usuarios, permisos y auditoría

---

### [US-001] Administrar empresa y locales

- **Épica/Módulo:** EP-01 — Fundaciones
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador central,
  - quiero registrar la empresa y sus locales con su configuración propia,
  - para que toda la operación, el inventario y la caja queden separados por local desde la primera versión.
- **Descripción funcional:** Alta, edición y desactivación de locales pertenecientes a la empresa. Cada local queda habilitado como contenedor de caja, mesas, inventario, compras, producción, pedidos y delivery propios. Los elementos centrales (catálogo, recetas, precios, planes y programación del menú) no se configuran por local.
- **Reglas de negocio asociadas:** RN-001, RN-002, RN-007.
- **Criterios de aceptación:**
  - **CA1 — Alta de local**
    - Given que el administrador central tiene permiso de creación
    - When registra un nuevo local de la empresa
    - Then el sistema debe crear el local con su propio inventario general, caja, mesas, compras, producción y delivery
    - And no debe crear copias separadas del catálogo, recetas, precios, planes ni programación de menú.
  - **CA2 — Un único inventario general por local**
    - Given un local registrado
    - When se consulta su configuración de existencias
    - Then el sistema debe mostrar un único inventario general asociado a ese local.
  - **CA3 — Desactivación conservando historial**
    - Given un local con operaciones registradas
    - When el administrador lo desactiva
    - Then el sistema debe impedir nuevas operaciones en ese local
    - And debe conservar íntegramente su historial.
- **Dependencias:** ninguna. Es la historia raíz del backlog.
- **Datos o entidades involucradas:** Empresa, Local; relaciones Local 1—N Caja, Mesa, Inventario, Compra, Pedido y Producción.
- **Riesgos, supuestos o pendientes:** la cantidad de locales iniciales no está definida (`07` § Pendientes funcionales) — ver RF-010.
- **Trazabilidad:** `01-alcance-y-reglas.md` § Multilocal; `06-modelo-de-datos.md` § Organización; `README.md` § Alcance confirmado.
- **Estimación inicial:** S

---

### [US-002] Autenticar usuarios con usuario y contraseña

- **Épica/Módulo:** EP-01 — Fundaciones
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Todos los roles internos (administrador central, administrador de local, cajero, mozo, cocina, motorizado)
- **Historia de usuario:**
  - Como usuario del sistema,
  - quiero iniciar sesión con mi usuario y contraseña,
  - para acceder solo a los módulos y locales que me corresponden.
- **Descripción funcional:** Acceso general al sistema mediante usuario y contraseña, con resolución de los permisos efectivos del usuario (rol más excepciones) y del o los locales asignados. La autenticación debe estar disponible localmente cuando no hay Internet.
- **Reglas de negocio asociadas:** RN-011, RN-012, RN-013, RN-086, RN-090, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Ingreso válido**
    - Given un usuario activo con credenciales correctas y al menos un local asignado
    - When inicia sesión
    - Then el sistema debe otorgar acceso limitado a sus permisos efectivos y a sus locales asignados.
  - **CA2 — Usuario desactivado**
    - Given un usuario desactivado
    - When intenta iniciar sesión
    - Then el sistema debe negar el acceso
    - And debe conservar su historial de acciones previas.
  - **CA3 — Credenciales nunca legibles**
    - Given cualquier operación de consulta o mantenimiento de usuarios
    - When se visualiza la ficha del usuario
    - Then el sistema no debe mostrar la contraseña en texto legible en ninguna pantalla ni reporte.
  - **CA4 — Autenticación sin Internet**
    - Given un local sin conexión a Internet
    - When un usuario habilitado inicia sesión en ese local
    - Then el sistema debe autenticarlo localmente y permitir la operación del local.
  - **CA5 — Registro de sesión**
    - Given un inicio de sesión exitoso o fallido
    - When ocurre el evento
    - Then el sistema debe registrar la sesión como acción sensible auditable.
- **Dependencias:** US-004 (permisos), US-005 (usuarios), US-058 (operación offline para CA4).
- **Datos o entidades involucradas:** Usuario, Rol, Permiso, Local, sesión.
- **Riesgos, supuestos o pendientes:** el método de recuperación de acceso no está definido (`07` § Pendientes funcionales) — ver RF-006. La habilitación de la autenticación local para usuarios nuevos o con contraseña recién cambiada durante una caída no está documentada — ver RF-022.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` §§ Usuarios y permisos, Offline-first, Respaldo y seguridad; `06-modelo-de-datos.md` § Organización.
- **Estimación inicial:** M

---

### [US-003] Acceder como mozo mediante PIN individual

- **Épica/Módulo:** EP-01 — Fundaciones
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Mozo
- **Historia de usuario:**
  - Como mozo,
  - quiero identificarme con un PIN individual en un dispositivo compartido,
  - para cambiar de usuario rápidamente sin interrumpir la atención.
- **Descripción funcional:** Acceso rápido por PIN individual para mozos en dispositivos compartidos del POS, con bloqueo temporal tras intentos fallidos. El PIN del mozo es distinto de la contraseña general y del PIN presencial del pensionista.
- **Reglas de negocio asociadas:** RN-011, RN-090, RN-091, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Cambio rápido de mozo**
    - Given un dispositivo compartido con el POS abierto
    - When un mozo ingresa su PIN individual válido
    - Then el sistema debe identificarlo como el usuario activo del dispositivo y atribuirle las acciones que realice.
  - **CA2 — PIN no legible**
    - Given la ficha de un mozo
    - When un administrador la consulta
    - Then el sistema no debe mostrar el PIN en texto legible.
  - **CA3 — Bloqueo por intentos fallidos**
    - Given un número de intentos fallidos consecutivos de PIN
    - When se alcanza el umbral configurado
    - Then el sistema debe bloquear temporalmente el acceso por PIN.
- **Dependencias:** US-005 (usuarios y PIN), US-004 (permisos del rol).
- **Datos o entidades involucradas:** Usuario, PIN de mozo, Local.
- **Riesgos, supuestos o pendientes:** el plazo de bloqueo por intentos fallidos está pendiente de definición (`07` § Pendientes funcionales) — ver RF-006. **Pendiente de definición** para el valor del umbral y la duración del bloqueo.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` §§ Usuarios y permisos, Respaldo y seguridad.
- **Estimación inicial:** S

---

### [US-004] Definir roles y permisos granulares

- **Épica/Módulo:** EP-01 — Fundaciones
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador central,
  - quiero definir roles con permisos granulares por módulo y acción,
  - para que cada usuario pueda hacer exactamente lo que su función requiere sin depender del nombre de su cargo.
- **Descripción funcional:** Configuración de roles y del conjunto de permisos por módulo y acción, con posibilidad de asignar excepciones a un usuario concreto. Los permisos mínimos configurables son: ver, crear, modificar, desactivar, aprobar, anular, descontar, reabrir, ver costos, exportar y administrar permisos. Todo cambio de permisos se audita.
- **Reglas de negocio asociadas:** RN-004, RN-012, RN-014.
- **Criterios de aceptación:**
  - **CA1 — Creación de rol con permisos**
    - Given un administrador con permiso de administrar permisos
    - When crea un rol y le asigna permisos por módulo y acción
    - Then el sistema debe guardar el rol con la combinación exacta de permisos seleccionada.
  - **CA2 — Los once permisos mínimos están disponibles**
    - Given la pantalla de configuración de un rol
    - When se listan las acciones configurables
    - Then el sistema debe ofrecer al menos ver, crear, modificar, desactivar, aprobar, anular, descontar, reabrir, ver costos, exportar y administrar permisos.
  - **CA3 — Independencia del cargo**
    - Given dos usuarios con el mismo cargo
    - When se les asignan roles o excepciones distintas
    - Then el sistema debe aplicar a cada uno sus permisos efectivos y no los del cargo.
  - **CA4 — Restricción efectiva**
    - Given un usuario sin el permiso "ver costos"
    - When accede a una receta o a un reporte que contiene costos
    - Then el sistema no debe mostrarle los costos.
  - **CA5 — Auditoría del cambio**
    - Given un cambio de permisos
    - When se guarda
    - Then el sistema debe registrar usuario, fecha, hora, local, valor anterior y valor nuevo.
  - **CA6 — Exclusión de RRHH**
    - Given la configuración de módulos disponibles
    - When se revisan las opciones
    - Then el sistema no debe ofrecer asistencia, horarios laborales, planilla ni remuneraciones.
- **Dependencias:** US-001 (locales para el ámbito del permiso).
- **Datos o entidades involucradas:** Rol, Permiso, relación Rol N—N Permiso, excepciones por Usuario.
- **Riesgos, supuestos o pendientes:** ninguno documentado.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Usuarios y permisos; `01-alcance-y-reglas.md` § Auditoría transversal; `06-modelo-de-datos.md` § Organización.
- **Estimación inicial:** M

---

### [US-005] Gestionar usuarios y su asignación multilocal

- **Épica/Módulo:** EP-01 — Fundaciones
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador central,
  - quiero crear usuarios, asignarles roles y uno o varios locales, y desactivarlos cuando corresponda,
  - para controlar quién opera en cada local sin perder el historial de lo que hizo.
- **Descripción funcional:** Mantenimiento de usuarios internos: datos de acceso, rol, excepciones de permisos, PIN cuando corresponde y asignación a uno o varios locales. La desactivación no elimina historial.
- **Reglas de negocio asociadas:** RN-007, RN-012, RN-013, RN-090.
- **Criterios de aceptación:**
  - **CA1 — Asignación a varios locales**
    - Given un usuario creado
    - When el administrador le asigna dos o más locales
    - Then el sistema debe permitirle operar en todos los locales asignados y en ninguno más.
  - **CA2 — Desactivación sin borrado**
    - Given un usuario con operaciones registradas
    - When se desactiva
    - Then el sistema debe impedir su acceso
    - And debe conservar su historial y su atribución en operaciones pasadas.
  - **CA3 — Excepciones de permisos**
    - Given un usuario con rol asignado
    - When se le añade una excepción de permiso
    - Then el sistema debe aplicar el permiso efectivo resultante del rol más la excepción.
- **Dependencias:** US-001, US-004.
- **Datos o entidades involucradas:** Usuario, Rol, Permiso, Local (Usuario N—N Local).
- **Riesgos, supuestos o pendientes:** la cantidad de usuarios concurrentes esperada no está definida (`07`) — ver RF-010.
- **Trazabilidad:** `05-usuarios-reportes-multilocal-offline.md` § Usuarios y permisos; `06-modelo-de-datos.md` § Organización.
- **Estimación inicial:** M

---

### [US-006] Registrar auditoría transversal de acciones sensibles

- **Épica/Módulo:** EP-01 — Fundaciones
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central / Administrador de local
- **Historia de usuario:**
  - Como administrador,
  - quiero que el sistema registre automáticamente quién, cuándo, dónde y qué cambió en cada acción sensible,
  - para poder auditar la operación y sustentar cualquier revisión posterior.
- **Descripción funcional:** Servicio transversal de auditoría que captura usuario, fecha, hora, local, valores anteriores y nuevos y el motivo cuando aplique, para el conjunto de eventos definido en la especificación. Es una capacidad compartida consumida por historias de otras épicas.
- **Reglas de negocio asociadas:** RN-004, RN-092.
- **Criterios de aceptación:**
  - **CA1 — Eventos cubiertos**
    - Given cualquiera de los eventos: descuento, anulación, reapertura, ajuste de inventario, transferencia de inventario, cambio de receta, cambio de precio, cambio de permisos, aprobación o rechazo de abono, cierre de caja
    - When el evento se confirma
    - Then el sistema debe registrar usuario, fecha, hora, local, valores anteriores y nuevos.
  - **CA2 — Motivo obligatorio cuando aplica**
    - Given un evento que la especificación exige justificar (descuento, anulación, reactivación de producto agotado, ajuste)
    - When el usuario confirma el evento sin indicar motivo
    - Then el sistema debe impedir la confirmación hasta que se registre el motivo.
  - **CA3 — Consulta de la bitácora**
    - Given un usuario con permiso de ver auditoría
    - When consulta la bitácora filtrando por local, usuario, fecha o tipo de evento
    - Then el sistema debe mostrar los registros correspondientes.
  - **CA4 — Inmutabilidad**
    - Given un registro de auditoría existente
    - When cualquier usuario intenta modificarlo o eliminarlo
    - Then el sistema debe impedirlo.
- **Dependencias:** US-002 (identidad del usuario), US-001 (local).
- **Datos o entidades involucradas:** Registro de auditoría (usuario, fecha, hora, local, valor anterior, valor nuevo, motivo, tipo de evento).
- **Riesgos, supuestos o pendientes:** **Supuesto de análisis:** la especificación no define el tiempo de retención de la bitácora; se relaciona con la política de retención pendiente — ver RF-011.
- **Trazabilidad:** `01-alcance-y-reglas.md` § Auditoría transversal; `05-usuarios-reportes-multilocal-offline.md` § Respaldo y seguridad.
- **Estimación inicial:** M

---

### [US-007] Configurar horarios de servicio por local

- **Épica/Módulo:** EP-01 — Fundaciones
- **Prioridad:** Must
- **Release sugerido:** MVP
- **Actor:** Administrador central
- **Historia de usuario:**
  - Como administrador central,
  - quiero configurar los horarios de desayuno, almuerzo y cena,
  - para que el sistema determine automáticamente el servicio del pensionista y controle el vencimiento de los pre-tickets.
- **Descripción funcional:** Parámetro de configuración de los tres servicios con su rango horario. Este parámetro alimenta la determinación automática del servicio en el autoservicio del pensionista y el vencimiento del pre-ticket pendiente.
- **Reglas de negocio asociadas:** RN-024, RN-026.
- **Criterios de aceptación:**
  - **CA1 — Configuración de rangos**
    - Given un administrador con permiso de modificar configuración
    - When define el rango horario de desayuno, almuerzo y cena
    - Then el sistema debe guardar la configuración y usarla para determinar el servicio.
  - **CA2 — Determinación automática del servicio**
    - Given un pensionista que se identifica en el autoservicio dentro del rango del almuerzo
    - When el sistema determina el servicio
    - Then debe asignar automáticamente "almuerzo" sin intervención del usuario.
  - **CA3 — Vencimiento por fin de horario**
    - Given un pre-ticket en estado pendiente
    - When termina el horario del servicio al que pertenece
    - Then el sistema debe cambiar su estado a vencido.
- **Dependencias:** US-001.
- **Datos o entidades involucradas:** configuración de horarios de servicio, Local, PreTicket, Servicio.
- **Riesgos, supuestos o pendientes:** **Pendiente de definición** — los horarios exactos de desayuno, almuerzo y cena no han sido proporcionados (`07` § Pendientes funcionales). La historia es implementable como parámetro configurable; los valores iniciales deben cargarse cuando negocio los defina. Ver RF-001. Tampoco se documenta si los horarios pueden diferir entre locales — **Supuesto de análisis:** se modela como configuración central con posibilidad de valor por local, sujeto a confirmación.
- **Trazabilidad:** `02-pensionistas.md` § Autoservicio presencial; `07-criterios-de-aceptacion-y-pendientes.md` §§ Casos críticos (2), Pendientes funcionales.
- **Estimación inicial:** S

---

