# US-024: Aprobar o rechazar abonos digitales Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar la bandeja de abonos digitales pendientes (`ADM-07`) para el Administrador Central, permitiendo revisar comprobantes, aprobar abonos (acreditando el saldo a la cuenta corriente del pensionista) o rechazarlos (con motivo obligatorio del catálogo y sin alterar saldo), con auditoría transversal completa.

**Architecture:** Módulo `pensionistas` y `catalogo` en Spring Boot con PostgreSQL. Migración Flyway V49 para motivos de rechazo, casos de uso `AprobarAbonoDigital` y `RechazarAbonoDigital`, endpoints REST en `AbonoController` o `CuentaController`, y pantalla `PantallaAbonosPendientes` en `rikos-web`.

**Tech Stack:** Java 21, Spring Boot 3.3, JPA / Hibernate, Flyway, PostgreSQL, React 19, TypeScript, Vite.

**Spec:** `docs/producto/plan/sprint-08-circuito-del-pensionista.md` §9

## Global Constraints

- **RN-031:** Los abonos digitales quedan pendientes (`estado = 'pendiente'`) y no modifican el saldo hasta la aprobación exclusiva del administrador.
- **RN-004:** La aprobación y rechazo de abonos registran usuario, fecha, hora, valores anterior y nuevo y motivo cuando aplique en la tabla `auditoria`.
- **RN-029:** Las notificaciones internas se emiten por abono aprobado o rechazado (auditadas internamente).
- **RN-092:** Los datos de comprobantes y transferencias se protegen según permisos.
- **CA1:** Solo un usuario con permiso `pensionistas.administrar` (Administrador central) puede aprobar o rechazar abonos digitales.
- **CA2:** Al aprobar: actualiza `estado = 'aprobado'`, acredita a la cuenta corriente del pensionista vía `AcreditarAbono`, y audita con `aprobacion_abono`.
- **CA3:** Al rechazar: exige un motivo del catálogo `motivo` (`tipo = 'rechazo_abono'`), no altera el saldo del pensionista, y audita con `rechazo_abono`.
- **CA4:** Un abono en estado pendiente no tiene efecto en el saldo del pensionista hasta que sea aprobado.
- **Idempotencia:** Si un abono ya fue aprobado o rechazado previamente, la operación se rechaza con código 422 (`ABONO_YA_PROCESADO`).

---

### Task 1: Migración Flyway V49 (Catálogo de motivos de rechazo) y Repositorio de Abonos

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/resources/db/migration/V49__motivos_rechazo_abono.sql`
- Modify: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/pensionistas/infraestructura/AbonoRepositorio.java`
- Modify: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/pensionistas/infraestructura/Abono.java`

- [ ] **Step 1: Crear script de migración SQL `V49__motivos_rechazo_abono.sql`**
  Sembrar motivos de tipo `rechazo_abono` en la tabla `motivo`:
  - `01M49MOTIVO000000000000R01`: "Operación no encontrada en cuenta bancaria" (requiere_observacion = FALSE)
  - `01M49MOTIVO000000000000R02`: "Monto no coincide con el extracto bancario" (requiere_observacion = TRUE)
  - `01M49MOTIVO000000000000R03`: "Comprobante ilegible, incompleto o adulterado" (requiere_observacion = FALSE)
  - `01M49MOTIVO000000000000R04`: "Número de operación duplicado o ya utilizado" (requiere_observacion = FALSE)
  - `01M49MOTIVO000000000000R05`: "Otro motivo" (requiere_observacion = TRUE)

- [ ] **Step 2: Actualizar `Abono.java` con métodos de dominio**
  - `rechazar(actorId, motivoId, observacion, ahora)`
  - Getters y setters necesarios para aprobación y rechazo.

- [ ] **Step 3: Agregar consultas a `AbonoRepositorio.java`**
  - `List<Abono> findByEstadoOrderByCreadoEnAsc(String estado);`
  - `List<Abono> findByOrderByCreadoEnDesc();`
  - Métodos con especificación o filtrado por estado.

---

### Task 2: Casos de Uso Backend (`AprobarAbonoDigital` y `RechazarAbonoDigital`)

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/pensionistas/aplicacion/AprobarAbonoDigital.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/pensionistas/aplicacion/RechazarAbonoDigital.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/pensionistas/aplicacion/ConsultaAbonos.java`
- Create: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/pensionistas/aplicacion/AprobarRechazarAbonoDigitalTest.java`

- [ ] **Step 1: Escribir pruebas unitarias en `AprobarRechazarAbonoDigitalTest.java`**
  - Aprobar abono pendiente: cambia estado a `aprobado`, invoca `AcreditarAbono`, reduce deuda, genera `MovimientoCuenta` y registra auditoría.
  - Rechazar abono pendiente con motivo válido: cambia estado a `rechazado`, no altera saldo ni crea `MovimientoCuenta`, registra auditoría con motivo.
  - Rechazar con motivo que requiere observación pero viene vacía: falla con `OBSERVACION_REQUERIDA`.
  - Aprobar abono ya aprobado o rechazado: falla con `ABONO_YA_PROCESADO`.
  - Rechazar abono ya procesado: falla con `ABONO_YA_PROCESADO`.
  - Intentar aprobar o rechazar abono inexistente: falla con `ABONO_NO_ENCONTRADO`.

- [ ] **Step 2: Implementar `AprobarAbonoDigital.java`**
  - Validar estado `pendiente`.
  - Llamar a `AcreditarAbono.ejecutar(...)`.
  - Actualizar `abono` (`estado = 'aprobado'`, `aprobado_por = actorId`, `aprobado_en = ahora`).
  - Registrar evento de auditoría `aprobacion_abono`.

- [ ] **Step 3: Implementar `RechazarAbonoDigital.java`**
  - Validar estado `pendiente`.
  - Validar `motivoId` contra `ConsultaMotivos.resolver(motivoId, "rechazo_abono")`.
  - Validar si requiere observación.
  - Actualizar `abono` (`estado = 'rechazado'`, `aprobado_por = actorId`, `aprobado_en = ahora`, `motivo_rechazo_id`, `observacion`).
  - Registrar evento de auditoría `rechazo_abono`.

- [ ] **Step 4: Implementar `ConsultaAbonos.java`**
  - Listar abonos filtrando por estado opcional y buscando por DNI/nombre de pensionista o N° operación.
  - Mapear DTO con nombres de pensionista, cajero/aprobador y descripción del motivo de rechazo.

---

### Task 3: Controladores REST y Seguridad

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/pensionistas/api/AbonoController.java`
- Modify: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/comun/seguridad/ConfiguracionSeguridad.java`
- Create: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/pensionistas/api/AbonoControllerAccesoTest.java`

- [ ] **Step 1: Implementar `AbonoController.java`**
  - `GET /api/pensionistas/abonos/pendientes`
  - `GET /api/pensionistas/abonos` (con filtros `estado` y `busqueda`)
  - `POST /api/pensionistas/abonos/{id}/aprobar`
  - `POST /api/pensionistas/abonos/{id}/rechazar`

- [ ] **Step 2: Configurar permisos en `ConfiguracionSeguridad.java`**
  - `POST /api/pensionistas/abonos/*/aprobar` -> `hasAuthority('PERM_pensionistas.administrar')`
  - `POST /api/pensionistas/abonos/*/rechazar` -> `hasAuthority('PERM_pensionistas.administrar')`
  - `GET /api/pensionistas/abonos/**` -> `hasAnyAuthority('PERM_pensionistas.ver', 'PERM_pensionistas.administrar')`

- [ ] **Step 3: Test de acceso por rol `AbonoControllerAccesoTest.java`**
  - Admin central (`pensionistas.administrar`): puede listar, aprobar y rechazar.
  - Cajero o Mozo: recibe `403 Forbidden` al intentar aprobar o rechazar.

---

### Task 4: Frontend Web (`rikos-web`)

**Files:**
- Modify: `rikos-web/src/api/cliente.ts`
- Create: `rikos-web/src/features/pensionistas/DialogoRechazarAbono.tsx`
- Create: `rikos-web/src/features/pensionistas/ModalComprobanteAbono.tsx`
- Create: `rikos-web/src/features/pensionistas/PantallaAbonosPendientes.tsx`
- Modify: `rikos-web/src/apps/admin/index.tsx` (navegación y ruteo bajo Pensionistas)

- [ ] **Step 1: Extender `api/cliente.ts`**
  - `listarAbonos(token, params)`
  - `aprobarAbono(token, id)`
  - `rechazarAbono(token, id, motivoId, observacion)`
  - `listarMotivosRechazoAbono(token)` (usando `listarMotivos(token, 'rechazo_abono')`)

- [ ] **Step 2: Componentes UI de apoyo**
  - `DialogoRechazarAbono.tsx`: selector de motivo con advertencia si requiere observación.
  - `ModalComprobanteAbono.tsx`: zoom del voucher o ficha bancaria si no hay adjunto.

- [ ] **Step 3: Pantalla `PantallaAbonosPendientes.tsx`**
  - Pestañas: Pendientes (badge), Aprobados, Rechazados, Todos.
  - Buscador reactivo.
  - Tabla con botón de copiado del N° de operación.
  - Botones de acción "Aprobar" y "Rechazar" con feedback inmediato y actualización de la lista.

- [ ] **Step 4: Integración en navegación de administración**
  - Agregar ruta y enlace en el sidebar: **Pensionistas → Abonos pendientes** con badge numérico.

---

### Task 5: Verificación End-to-End

- [ ] **Step 1: Compilación backend y ejecución de suite de tests**
- [ ] **Step 2: Compilación frontend `npm run build`**
- [ ] **Step 3: Verificación en navegador Playwright**
  - Iniciar sesión como cajero y registrar abono digital (Yape).
  - Iniciar sesión como administrador central y verificar que aparece en "Abonos pendientes".
  - Probar visualización de datos bancarios.
  - Aprobar un abono: verificar acreditación en cuenta y cambio de estado.
  - Registrar otro abono digital y rechazarlo con motivo: verificar que no alteró deuda.
