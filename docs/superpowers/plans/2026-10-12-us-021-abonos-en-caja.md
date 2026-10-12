# US-021: Registrar abonos del pensionista en caja Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar el registro de abonos del pensionista en caja (efectivo y digital) con conciliación automática del turno, actualización del libro de cuenta y emisión de constancia interna imprimible.

**Architecture:** Módulo `caja` y `pensionistas` en Spring Boot con PostgreSQL. Migración Flyway V48 para tabla `abono`, caso de uso `RegistrarAbonoEnCaja`, endpoints REST en `CajaController`, y modal táctil `DialogoRegistrarAbono` con constancia térmica imprimible en `rikos-web`.

**Tech Stack:** Java 21, Spring Boot 3.3, JPA / Hibernate, Flyway, PostgreSQL, React 19, TypeScript, Vite.

**Spec:** `docs/producto/plan/sprint-08-circuito-del-pensionista.md` §8

## Global Constraints

- **RN-030:** El abono en efectivo lo registra caja, queda confirmado de inmediato (`estado = 'aprobado'`) y entra en la conciliación del turno (`efectivoEsperado = fondoInicial + efectivo`).
- **RN-031:** Los abonos digitales (Yape, Plin, Transferencia) quedan pendientes (`estado = 'pendiente'`) y no modifican el saldo hasta la aprobación del administrador (US-024).
- **RN-032:** Validar posible duplicidad de `numero_operacion` como advertencia/alerta sin bloquear el registro.
- **RN-033:** Se permiten abonos parciales por cualquier monto positivo mayor a cero (`monto > 0`).
- **RN-034:** Un pensionista inactivo no consume, pero sus abonos y ajustes sí se aceptan para amortizar su deuda.
- **RN-035:** El abono no genera una venta tributaria (ni boleta ni factura); genera una constancia interna con leyenda obligatoria: "Constancia interna de abono a cuenta corriente. No constituye comprobante de pago."
- **RN-069:** El abono en efectivo de pensionista es la única excepción de ingreso permitida en caja fuera de ventas.
- **Permisos:** Registro de abono requiere permiso `caja.crear` y un turno de caja abierto en el local de la sesión.

## Review Focus

1. **Abono en efectivo vs Arqueo de caja:** Al cerrar turno (`CerrarTurno`), el efectivo esperado debe incluir con exactitud matemática el importe del abono registrado en el turno.
2. **Abono digital no altera caja ni saldo:** El registro de un abono por Yape o Plin no debe generar `MovimientoCaja` ni fila en `MovimientoCuenta`, y el saldo deudor del pensionista debe permanecer intacto.
3. **Validación de número de operación en digitales:** Si el medio es Yape, Plin o Transferencia y el cajero no ingresa `numeroOperacion`, la solicitud debe ser rechazada con código 422.
4. **Idempotencia y alerta de duplicado:** Si se registra un número de operación ya existente, se debe guardar normalmente pero marcando `alertaDuplicado = true`.
5. **Pensionista inactivo:** Debe permitir abonar a un pensionista con `activo = false` si tiene cuenta registrada.

---

### Task 1: Migración Flyway V48 y Entidades JPA `Abono`

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/resources/db/migration/V48__abono_pensionista.sql`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/pensionistas/infraestructura/Abono.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/pensionistas/infraestructura/AbonoRepositorio.java`
- Modify: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/pensionistas/infraestructura/MovimientoCuenta.java`

**Interfaces:**
- Consumes: PostgreSQL schema, `pensionista`, `turno_caja`, `local`, `usuario`.
- Produces: `Abono` entity, `AbonoRepositorio` with `findByTurnoCajaId(String turnoId)` and `existsByNumeroOperacionAndMedio(String numeroOperacion, String medio)`.

- [ ] **Step 1: Crear script de migración SQL `V48__abono_pensionista.sql`**
  Definir tabla `abono`, índices y llave foránea en `movimiento_cuenta.abono_id`.

- [ ] **Step 2: Crear entidad JPA `Abono.java`**
  Mapear atributos: `id`, `pensionistaId`, `localId`, `medio`, `monto`, `fechaOperacion`, `numeroOperacion`, `cuentaDestino`, `rutaAdjunto`, `origen`, `estado`, `registradoPor`, `aprobadoPor`, `aprobadoEn`, `motivoRechazoId`, `turnoCajaId`, `observacion`, `creadoEn`.
  Métodos de dominio: `aprobar(actorId, ahora)`, `esAprobado()`, `esPendiente()`.

- [ ] **Step 3: Mapear `abonoId` en `MovimientoCuenta.java`**
  Agregar `@Column(name = "abono_id", length = 26) private String abonoId;` y su getter `getAbonoId()`.

- [ ] **Step 4: Crear interfaz Spring Data `AbonoRepositorio.java`**
  Métodos:
  - `List<Abono> findByTurnoCajaIdOrderByCreadoEnDesc(String turnoCajaId);`
  - `boolean existsByNumeroOperacionAndMedio(String numeroOperacion, String medio);`

---

### Task 2: Caso de Uso Backend `RegistrarAbonoEnCaja` (TDD)

**Files:**
- Create: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/caja/aplicacion/RegistrarAbonoEnCajaTest.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/caja/aplicacion/RegistrarAbonoEnCaja.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/caja/aplicacion/AbonoRegistrado.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/caja/aplicacion/ErrorAbono.java`

**Interfaces:**
- Consumes: `TurnoCajaRepositorio`, `MovimientoCajaRepositorio`, `AbonoRepositorio`, `PensionistaRepositorio`, `ConsultaCuenta`, `LibroDeCuenta`, `RegistradorAuditoria`.
- Produces: `RegistrarAbonoEnCaja.ejecutar(cajeroId, SolicitudAbono) -> Resultado<AbonoRegistrado, ErrorAbono>`.

- [ ] **Step 1: Escribir tests unitarios en `RegistrarAbonoEnCajaTest.java`**
  Tests para:
  1. `abonoEfectivoExitoso`: aprobado, crea movimiento de caja y cuenta, audita `aprobacion_abono`, actualiza saldo.
  2. `abonoDigitalExitoso`: queda pendiente, no crea movimiento de caja ni libro de cuenta, valida número de operación.
  3. `abonoDigitalConNumeroOperacionDuplicado`: acepta el abono pero retorna `alertaDuplicado = true`.
  4. `fallaSiNoHayTurnoAbierto`: retorna `SIN_TURNO_ABIERTO`.
  5. `fallaSiMontoEsInvalido`: monto null o <= 0 retorna `MONTO_INVALIDO`.
  6. `fallaSiMedioDigitalNoTieneNumeroOperacion`: retorna `NUMERO_OPERACION_REQUERIDO`.
  7. `fallaSiPensionistaNoExiste`: retorna `PENSIONISTA_NO_ENCONTRADO`.
  8. `permiteAbonarAPensionistaInactivo`: acepta abono y amortiza deuda aunque `activo == false`.

- [ ] **Step 2: Ejecutar test para verificar fallos iniciales**
  Run: `mvn test -Dtest=RegistrarAbonoEnCajaTest`
  Expected: Fallo de compilación / clases no encontradas.

- [ ] **Step 3: Implementar `ErrorAbono.java`, `AbonoRegistrado.java` y `RegistrarAbonoEnCaja.java`**
  Implementar lógica de negocio, validaciones y orquestación con `LibroDeCuenta` y `MovimientoCajaRepositorio`.

- [ ] **Step 4: Ejecutar tests para verificar que todos pasan en verde**
  Run: `mvn test -Dtest=RegistrarAbonoEnCajaTest`
  Expected: BUILD SUCCESS con todas las pruebas pasando.

---

### Task 3: Controlador REST y Seguridad en `CajaController`

**Files:**
- Modify: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/caja/api/CajaController.java`
- Modify: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/comun/seguridad/ConfiguracionSeguridad.java`
- Create: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/caja/api/CajaAbonoControllerTest.java`

**Interfaces:**
- Consumes: `RegistrarAbonoEnCaja`, `AbonoRepositorio`.
- Produces:
  - `POST /turnos/actual/abonos` (201 Created con DTO de respuesta y constancia).
  - `GET /turnos/actual/abonos` (200 OK con lista de abonos del turno).

- [ ] **Step 1: Escribir tests de integración en `CajaAbonoControllerTest.java`**
  1. `registrarAbonoEfectivoRetorna201ConConstancia`
  2. `registrarAbonoDigitalRetorna201ConEstadoPendiente`
  3. `registrarAbonoSinNumeroOperacionRetorna422`
  4. `registrarAbonoSinTurnoAbiertoRetorna409`
  5. `listarAbonosDelTurnoRetorna200`
  6. `rechazaPeticionSinPermisoCajaCrearCon403`

- [ ] **Step 2: Agregar DTOs y endpoints en `CajaController.java`**
  Definir `AbonoRequest`, `AbonoRespuesta`, `ConstanciaRespuesta`, mapear `POST /turnos/actual/abonos` y `GET /turnos/actual/abonos`.

- [ ] **Step 3: Configurar permisos en `ConfiguracionSeguridad.java`**
  Permitir `POST /turnos/actual/abonos` a `PERM_caja.crear`, y `GET /turnos/actual/abonos` a `PERM_caja.ver`.

- [ ] **Step 4: Ejecutar suite de pruebas de controladores**
  Run: `mvn test -Dtest=CajaAbonoControllerTest,CajaControllerTest`
  Expected: BUILD SUCCESS.

---

### Task 4: Frontend Web: API y Componentes en `rikos-web`

**Files:**
- Modify: `rikos-web/src/api/cliente.ts`
- Create: `rikos-web/src/features/caja/DialogoRegistrarAbono.tsx`
- Create: `rikos-web/src/features/caja/ConstanciaAbono.tsx`
- Modify: `rikos-web/src/features/caja/PantallaCaja.tsx`
- Modify: `rikos-web/src/features/caja/caja.css` (o estilos compartidos)

**Interfaces:**
- Consumes: `/api/turnos/actual/abonos`, `/api/pensionistas`, `/api/pensionistas/{id}/cuenta`.
- Produces: `DialogoRegistrarAbono`, `ConstanciaAbono`, botón y lista de abonos en `PantallaCaja`.

- [ ] **Step 1: Agregar tipos y funciones en `api/cliente.ts`**
  Definir `AbonoDelTurno`, `SolicitudAbono`, `RespuestaAbono`, `registrarAbonoEnCaja(token, datos)`, `listarAbonosDelTurno(token)`.

- [ ] **Step 2: Crear componente `ConstanciaAbono.tsx`**
  Diseño térmico con datos del pensionista, monto, saldo anterior, saldo restante, leyenda obligatoria ("Constancia interna de abono a cuenta corriente. No constituye comprobante de pago.") y botón `window.print()`.

- [ ] **Step 3: Crear componente `DialogoRegistrarAbono.tsx`**
  Buscador táctil de pensionista por DNI/nombre, indicador de deuda actual, selector de medios (Efectivo, Yape, Plin, Transferencia), input numérico / teclado táctil para monto, campos adicionales para digitales (`numeroOperacion` y selector de `cuentaDestino` con BCP, BBVA, Yape, Plin).

- [ ] **Step 4: Integrar en `PantallaCaja.tsx`**
  - Añadir botón "Abono pensionista" en cabecera/acciones.
  - Mostrar sección de abonos del turno con acceso para ver/reimprimir constancia.
  - Al completar un abono, refrescar automáticamente el turno (actualizando `efectivoEsperado` y `efectivoCobrado`).

- [ ] **Step 5: Validar compilación frontend**
  Run: `npx tsc -p tsconfig.app.json --noEmit`
  Expected: 0 errores de TypeScript.

---

### Task 5: Verificación Integral End-to-End

- [ ] **Step 1: Ejecutar toda la suite de tests de backend**
  Run: `mvn test`
  Expected: BUILD SUCCESS con 0 fallos.

- [ ] **Step 2: Reiniciar el demonio de `servicio-operacion` con la nueva versión compilada**

- [ ] **Step 3: Prueba funcional con Playwright / Navegador**
  - Iniciar sesión como cajero en el POS con turno abierto.
  - Abrir diálogo de abono, seleccionar a Joel López (DNI `60606060`).
  - Registrar abono en efectivo por S/ 20.00.
  - Verificar emisión de constancia, actualización del saldo deudor y cuadre en el resumen de caja.
  - Registrar abono digital (Yape) por S/ 15.00 con número de operación `998877`.
  - Verificar que queda en estado `pendiente`, no mueve el arqueo de efectivo y genera constancia de abono digital.
