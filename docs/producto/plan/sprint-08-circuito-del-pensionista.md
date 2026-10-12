# Sprint 8 — El circuito del pensionista, completo

**Historias (del backlog):** US-019 (pre-ticket autoservicio), US-020 (vincular pre-ticket al pedido),
US-021 (abonos en caja), US-024 (aprobar/rechazar abonos digitales).
Flujos en `flujos/04-ep04-pensionistas.md`; modelo en `modelo-de-datos/05-pensionistas-planes-cuenta-corriente--ep04.md`.

**Estado:** En curso — especificando e implementando **Entrega 1: US-019 (Pre-ticket de autoservicio)**.

**Cómo se llegó a esto:** Brainstorming y entrevista /grill-me con el usuario el 2026-10-11.
Cada decisión lleva su razón explícita y alternativas descartadas.

---

## 1. Qué queremos lograr

Conectar el circuito presencial del pensionista desde el momento en que entra al local:
el estudiante llega al kiosco de autoservicio, ingresa su DNI y PIN, el sistema reconoce su servicio
según el horario activo y le emite un pre-ticket impreso (con código de 6 dígitos) **sin generar todavía ningún cargo contable**.
Luego el mozo en el salón vinculará dicho código al pedido de la mesa (US-020), y la caja cerrará con cargo a su cuenta (sprint 7).

**Éxito de la Entrega 1 (US-019):**
Un pensionista activo puede acercarse al totem del Kiosco (`/kiosco`), digitar su DNI y PIN, y obtener su pre-ticket
con código numérico de 6 dígitos, servicio determinado automáticamente y leyenda obligatoria de que no constituye consumo cargado.
Si falla 5 veces consecutivas, se bloquea temporalmente su cuenta (15 min) sin inhabilitar el kiosco para los demás.
Si ya tenía un pre-ticket pendiente del mismo turno, se le recuerda y permite reimprimirlo en vez de duplicar.

---

## 2. Contexto y Decisiones de Diseño

### 2.1 Reutilización de pre-ticket pendiente (RF-018)
- **Decisión:** Si el estudiante ya tiene un pre-ticket en estado `pendiente` para el mismo servicio en ese local, el kiosco no genera una nueva fila ni código; le muestra el ticket pendiente actual y permite reimprimirlo.
- **Razón:** Evita desperdicio de papel y códigos huérfanos si un alumno presiona dos veces o vuelve a consultar mientras espera mesa.

### 2.2 Código numérico de 6 dígitos
- **Decisión:** Formato `\d{6}` (ej. `381920`).
- **Razón:** En el POS del mozo, el teclado es predominantemente numérico. Un código de 6 dígitos es rápido de teclear y memorizar, y ofrece suficiente espacio para un turno de servicio con restricción de unicidad activa.

### 2.3 Experiencia en el Kiosco (`/kiosco`) e Impresión
- **Decisión:** Pantalla táctil minimalista para DNI y PIN numérico. Tras validar, se renderiza la tarjeta del ticket térmico con su código grande, botón de imprimir (`window.print()`) y temporizador de autocierre a los 15 segundos que devuelve la pantalla al estado inicial para el siguiente estudiante.
- **Razón:** Agiliza la fila en horas punta y no requiere intervención de personal del restaurante.

### 2.4 Bloqueo por intentos fallidos por Pensionista (CA6)
- **Decisión:** El contador de fallos (`intentos_fallidos` y `bloqueado_hasta`) se gestiona a nivel de la entidad `Pensionista` (asociado a su DNI), no por terminal/dispositivo.
- **Razón:** A diferencia del mozo (que no ingresa usuario previo en el POS), el estudiante sí ingresa su DNI. Bloquear el terminal completo inhabilitaría el kiosco para 20 estudiantes esperando en la fila si uno solo olvidó su PIN.

### 2.5 Determinación del horario y modo desarrollo
- **Decisión:** Se utiliza el servicio de dominio existente `ResolverServicioVigente` (que resuelve días especiales y turnos semanales). Si la hora actual no coincide con ningún servicio, se muestra error informativo ("Fuera del horario de atención"), pero en modo desarrollo/pruebas el API admite un parámetro opcional de `servicio` para no bloquear el testing local.

---

## 3. Modelo de Datos

### 3.1 Tabla `pre_ticket` (Migración Flyway V42)

```sql
CREATE TABLE pre_ticket (
    id             CHAR(26) PRIMARY KEY,
    codigo         VARCHAR(12) NOT NULL,
    pensionista_id CHAR(26) NOT NULL REFERENCES pensionista(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    local_id       CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    servicio       VARCHAR(20) NOT NULL,
    estado         VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    emitido_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    vence_en       TIMESTAMPTZ NOT NULL,
    vinculado_en   TIMESTAMPTZ,
    vinculado_por  CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT pre_ticket_codigo_uq UNIQUE (codigo),
    CONSTRAINT pre_ticket_servicio_check CHECK (servicio IN ('desayuno', 'almuerzo', 'cena')),
    CONSTRAINT pre_ticket_estado_check CHECK (estado IN ('pendiente', 'vinculado', 'cerrado', 'vencido', 'anulado'))
);

CREATE INDEX ix_pre_ticket_local_estado ON pre_ticket(local_id, estado, vence_en);
CREATE INDEX ix_pre_ticket_pensionista_servicio ON pre_ticket(pensionista_id, servicio, estado);
```

### 3.2 Ampliación a `pensionista`
Se agregan columnas para el bloqueo individual por intentos fallidos:
```sql
ALTER TABLE pensionista ADD COLUMN intentos_fallidos INTEGER NOT NULL DEFAULT 0;
ALTER TABLE pensionista ADD COLUMN bloqueado_hasta TIMESTAMPTZ;
```

---

## 4. API Backend

- **Ruta:** `POST /api/kiosco/pre-ticket` (pública en `ConfiguracionSeguridad`, no requiere JWT de empleado).
- **Request:**
  ```json
  {
    "localCodigo": "sanisidro",
    "dni": "72345678",
    "pin": "1234",
    "servicioManual": "almuerzo" // Opcional, solo desarrollo/fallback
  }
  ```
- **Response Exitoso (200 OK):**
  ```json
  {
    "id": "01M...",
    "codigo": "381920",
    "pensionistaId": "01M...",
    "nombrePensionista": "Luis Alberto Quispe Rojas",
    "dni": "72345678",
    "servicio": "almuerzo",
    "estado": "pendiente",
    "emitidoEn": "2026-10-11T13:15:00Z",
    "venceEn": "2026-10-11T15:30:00Z",
    "reutilizado": false,
    "leyenda": "Este pre-ticket no constituye un consumo cargado ni comprobante de pago."
  }
  ```
- **Errores:**
  - `401 Unauthorized` (`CREDENCIALES_INVALIDAS`): DNI no encontrado o PIN erróneo.
  - `423 Locked` (`BLOQUEADO`): Cuenta bloqueada por intentos fallidos (con `segundosRestantes`).
  - `400 Bad Request` (`PENSIONISTA_INACTIVO`, `FUERA_DE_HORARIO`, `LOCAL_INACTIVO`).

---

## 5. Frontend: Superficie `/kiosco` (`rikos-web`)

- Ubicación: `rikos-web/src/apps/kiosco/` y `rikos-web/src/features/kiosco/`.
- Pantalla táctil con:
  1. Ingreso de DNI (8 dígitos) con teclado táctil numérico en pantalla.
  2. Ingreso de PIN (4 dígitos) oculto.
  3. Visualización del ticket emitido:
     - Logo RIKO'S y nombre del restaurante.
     - Código en tipografía monoespaciada grande (`381 920`).
     - Nombre del pensionista y servicio.
     - Fecha, hora y texto legal obligatorio de no-cobro.
     - Botón "Imprimir Ticket" y barra de progreso de 15 segundos para resetear la pantalla.

---

## 6. Plan de Implementación TDD (Entrega 1: US-019) — Completado

1. **Migración Flyway V47:** `pre_ticket` y columnas de bloqueo en `pensionista`, más `cuenta_persona.pre_ticket_id`.
2. **Entidad y Repositorio:** `PreTicket`, `PreTicketRepositorio` en módulo `pensionistas`.
3. **Caso de Uso Backend:** `EmitirPreTicket` con pruebas unitarias y de integración (`EmitirPreTicketTest`, 10 pruebas verdes).
4. **Controlador:** `KioscoController` en `pensionistas.api` expuesto bajo `POST /api/kiosco/pre-ticket` (`KioscoControllerTest`, 4 pruebas verdes).
5. **Frontend API y Pantalla:** Cliente HTTP en `api/cliente.ts` y componente interactivo `/kiosco`.
6. **Verificación End-to-End:** Verificado con Playwright emitiendo pre-tickets de almuerzo.

---

## 7. Entrega 2: US-020 (Vincular pre-ticket al pedido del pensionista)

**Objetivo:** El mozo en el salón recibe el pre-ticket del estudiante y digita el código de 6 dígitos en la mesa/persona correspondiente en el POS (`POS-03`, `POS-14`). El pre-ticket pasa de `pendiente` a `vinculado`, y la persona del pedido queda identificada como pensionista.

### 7.1 Decisiones de Diseño y Reglas de Negocio

1. **UX con doble pestaña en `DialogoPensionista`:**
   - **Pestaña principal (defecto):** "Código del ticket" con campo numérico y teclado táctil rápido en pantalla para tablet/pantalla táctil del POS.
   - **Pestaña secundaria (contingencia):** "Por DNI o nombre" para buscar directamente si la impresora del kiosco falló o el ticket no se puede leer.
2. **Validaciones estrictas al vincular (RN-025, RN-026, RN-027, RN-094):**
   - El código debe existir en la base de datos (`PRE_TICKET_NO_ENCONTRADO`, 404).
   - Debe pertenecer al mismo local activo (`PRE_TICKET_DE_OTRO_LOCAL`, 422).
   - El servicio del ticket (desayuno/almuerzo/cena) debe coincidir con el servicio del pedido (`SERVICIO_NO_COINCIDE`, 422).
   - No puede estar vencido por horario (`PRE_TICKET_VENCIDO`, 422).
   - No puede estar ya vinculado a otro pedido/persona (`PRE_TICKET_YA_VINCULADO`, 409).
   - No puede estar anulado ni cerrado (`PRE_TICKET_ANULADO` / `PRE_TICKET_CERRADO`, 409).
   - El pensionista debe estar activo (`PENSIONISTA_INACTIVO`, 409).
   - El pensionista no puede estar ya asignado a otra persona en el mismo pedido (`PENSIONISTA_YA_EN_EL_PEDIDO`, 409).
   - La persona no debe tener líneas creadas todavía (`PERSONA_CON_LINEAS`, 409).
3. **Manejo de errores o desvinculación:**
   - Si el mozo elimina la persona (`QuitarPersona`) o cancela el pedido sin cobro (`TerminarPedido`), el pre-ticket vinculado se desvincula y vuelve a estado `pendiente`, permitiendo al estudiante reutilizar su ticket en la mesa correcta.
4. **Cierre en caja (CA5):**
   - Al cerrar el pedido tras cobrar en caja (`CobrarPedido`), los pre-tickets vinculados a las personas del pedido pasan a estado `cerrado`.

### 7.2 API Backend

- **Ruta:** `PUT /api/pedidos/{id}/personas/{personaId}/pre-ticket`
- **Seguridad:** Requiere JWT autenticado (`@AuthenticationPrincipal actorId`).
- **Request Body:**
  ```json
  {
    "codigo": "381920"
  }
  ```
- **Response Exitoso (200 OK):** Devuelve el `PedidoRespuesta` actualizado con la persona ya configurada con `tipoCliente = "pensionista"`, `pensionistaId`, y `etiqueta = nombreCompleto`.
- **Errores:** Códigos 404, 409 y 422 según la regla violada.

### 7.3 Plan de Implementación TDD (US-020)

1. **Mapeo JPA `CuentaPersona`:** Mapear `preTicketId` en `CuentaPersona.java` y actualizar método `asignarPensionista`.
2. **Método `desvincular()` en `PreTicket`:** Añadir `desvincular()` a la entidad `PreTicket`.
3. **Caso de Uso Backend:** `VincularPreTicket` en `pe.rikos.operacion.venta.aplicacion` con pruebas unitarias exhaustivas en `VincularPreTicketTest` (pruebas de éxito, ticket no encontrado, otro local, servicio distinto, vencido, ya vinculado, persona con líneas, pensionista repetido).
4. **Integración con Ciclo de Vida:**
   - Actualizar `QuitarPersona` y `TerminarPedido` para devolver pre-tickets a `pendiente`.
   - Actualizar `CobrarPedido` para marcar pre-tickets como `cerrado` al cerrar el pedido.
5. **Controlador:** Exponer endpoint en `PedidoController`.
6. **Frontend:**
   - Función `vincularPreTicketAPersona(token, pedidoId, personaId, codigo)` en `api/cliente.ts`.
   - Pestaña principal de 6 dígitos con teclado numérico en `DialogoPensionista.tsx`.
   - Conexión en `PantallaPedido.tsx`.
7. **Verificación:** Pruebas unitarias backend (`mvn test`), compilación frontend y verificación funcional.

---

## 8. Entrega 3: US-021 (Registrar abonos del pensionista en caja)

**Objetivo:** El cajero registra los abonos que realiza el pensionista o su responsable en el restaurante (en efectivo o digitales como Yape, Plin y transferencia bancaria). El efectivo queda confirmado de inmediato, entra en la conciliación del turno del cajero y actualiza el saldo de la cuenta corriente. Los digitales quedan en estado `pendiente` a la espera de aprobación del administrador (US-024) y no modifican el saldo. Se permiten abonos parciales y no se genera una venta contable/tributaria (RN-035), emitiendo una constancia interna imprimible.

### 8.1 Decisiones de Diseño y Reglas de Negocio

1. **Punto de acceso en Caja (`PantallaCaja.tsx` / POS-12):**
   - Botón visible y táctil "Abono pensionista" en la cabecera/acciones de la sesión activa de caja (requiere permiso `caja.crear` y un turno de caja abierto en el local de la sesión).
   - Abre el modal `DialogoRegistrarAbono` con:
     - Buscador por DNI o nombre del pensionista.
     - Indicador claro de deuda actual y saldo a favor del pensionista seleccionado.
     - Selector de medio de pago: Efectivo, Yape, Plin, Transferencia bancaria.
     - Campo / teclado de monto en soles.
     - Para medios digitales: campo obligatorio para `numeroOperacion` y selector de `cuentaDestino` (BCP RIKO'S, BBVA RIKO'S, Yape RIKO'S, Plin RIKO'S, con opción de texto libre).
2. **Ciclo de vida y Efecto Contable según Medio:**
   - **Efectivo (RN-030, CA1):**
     - Se registra con `estado = 'aprobado'`.
     - Genera `movimiento_caja` (`tipo = 'abono_pensionista'`, `medio = 'efectivo'`, `importe = monto`, `turno_caja_id = turno.id`, `pago_id = null`).
     - Genera `movimiento_cuenta` en el libro de la cuenta corriente del pensionista (`tipo = 'abono'`, `signo = 'credito'`, `importe = monto`, `abono_id = abono.id`).
     - Entra inmediatamente en la conciliación del turno: `resumenDelTurno` suma el abono al efectivo cobrado y al efectivo esperado (`fondoInicial + efectivo`), garantizando que `CerrarTurno` cuadre el arqueo.
     - Registra auditoría con evento `aprobacion_abono`.
   - **Digitales: Yape, Plin, Transferencia (RN-031, CA2):**
     - Se registra con `estado = 'pendiente'`.
     - No genera `movimiento_caja` ni `movimiento_cuenta`.
     - No altera el saldo del pensionista ni el efectivo de la caja hasta que el administrador lo apruebe (US-024).
     - Validación de duplicidad (RN-032): si ya existe otro abono con el mismo número de operación para ese medio, emite alerta/advertencia sin bloquear el registro.
3. **Abono Parcial y Estados de Cuenta (RN-033, RN-034, CA3):**
   - Se acepta cualquier monto mayor a cero (`monto > 0`). Si el monto es menor a la deuda, la amortiza parcialmente. Si excede la deuda, el excedente queda como saldo a favor en el libro.
   - Si el pensionista está en estado `inactivo`, el abono se permite para saldar deudas pendientes (RN-034).
4. **Constancia Interna e Impresión (RN-035, CA4):**
   - No genera comprobante electrónico ni boleta/factura en SUNAT ni movimiento de venta.
   - Tras registrar con éxito, se despliega una tarjeta de constancia interna imprimible (`window.print()` / formato ticket 80mm) con:
     - Título: "CONSTANCIA DE ABONO - RIKO'S".
     - Datos del pensionista: DNI y nombre completo.
     - Datos de la transacción: Fecha/hora, medio de pago, número de operación (si aplica), cuenta destino (si aplica).
     - Monto abonado: S/ XX.XX.
     - Estado: "CONFIRMADO EN CAJA" (Efectivo) o "PENDIENTE DE APROBACIÓN" (Digital).
     - Resumen de cuenta: Saldo anterior, monto abonado, nuevo saldo / deuda resultante.
     - Leyenda legal obligatoria: "Constancia interna de abono a cuenta corriente. No constituye comprobante de pago."

### 8.2 Modelo de Datos (Migración Flyway V48)

```sql
CREATE TABLE abono (
    id                 CHAR(26) PRIMARY KEY,
    pensionista_id     CHAR(26) NOT NULL REFERENCES pensionista(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    local_id           CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    medio              VARCHAR(20) NOT NULL CHECK (medio IN ('efectivo', 'yape', 'plin', 'transferencia')),
    monto              BIGINT NOT NULL CHECK (monto > 0),
    fecha_operacion    DATE NOT NULL,
    numero_operacion   VARCHAR(50),
    cuenta_destino     VARCHAR(80),
    ruta_adjunto       VARCHAR(500),
    origen             VARCHAR(20) NOT NULL CHECK (origen IN ('caja', 'portal')),
    estado             VARCHAR(20) NOT NULL CHECK (estado IN ('pendiente', 'aprobado', 'rechazado')),
    registrado_por     CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    aprobado_por       CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    aprobado_en        TIMESTAMPTZ,
    motivo_rechazo_id  CHAR(26) REFERENCES motivo(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    turno_caja_id      CHAR(26) REFERENCES turno_caja(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    observacion        VARCHAR(500),
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT abono_efectivo_caja CHECK (
        origen <> 'caja' OR medio <> 'efectivo' OR (turno_caja_id IS NOT NULL AND estado = 'aprobado')
    ),
    CONSTRAINT abono_digital_operacion CHECK (
        medio = 'efectivo' OR origen <> 'caja' OR numero_operacion IS NOT NULL
    )
);

CREATE INDEX ix_abono_pensionista ON abono(pensionista_id, creado_en DESC);
CREATE INDEX ix_abono_local_estado ON abono(local_id, estado);
CREATE INDEX ix_abono_turno ON abono(turno_caja_id);
CREATE INDEX ix_abono_numero_operacion ON abono(numero_operacion) WHERE numero_operacion IS NOT NULL;

-- Clave foránea de movimiento_cuenta a abono
ALTER TABLE movimiento_cuenta ADD CONSTRAINT fk_movimiento_cuenta_abono
    FOREIGN KEY (abono_id) REFERENCES abono(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
```

### 8.3 API Backend

- **Ruta:** `POST /api/turnos/actual/abonos`
- **Seguridad:** Requiere JWT autenticado con permiso `caja.crear` (`@AuthenticationPrincipal actorId`).
- **Request Body:**
  ```json
  {
    "pensionistaId": "01M...",
    "medio": "efectivo", // "efectivo" | "yape" | "plin" | "transferencia"
    "monto": 5000,      // céntimos (S/ 50.00)
    "numeroOperacion": "1829304", // obligatorio para medios digitales
    "cuentaDestino": "BCP RIKO'S", // opcional/recomendado para digitales
    "observacion": "Pago parcial de la semana"
  }
  ```
- **Response Exitoso (201 Created):**
  ```json
  {
    "id": "01M...",
    "pensionistaId": "01M...",
    "pensionistaNombre": "Joel López",
    "pensionistaDni": "60606060",
    "medio": "efectivo",
    "monto": 5000,
    "estado": "aprobado",
    "fechaOperacion": "2026-10-12",
    "numeroOperacion": null,
    "cuentaDestino": null,
    "turnoCajaId": "01M...",
    "saldoAnterior": 8000,
    "nuevoSaldo": 3000,
    "saldoAFavor": 0,
    "alertaDuplicado": false,
    "constancia": {
      "localNombre": "San Isidro",
      "cajeroNombre": "Admin General",
      "fecha": "2026-10-12T13:45:00Z",
      "leyenda": "Constancia interna de abono a cuenta corriente. No constituye comprobante de pago."
    }
  }
  ```
- **Ruta de Consulta:** `GET /api/turnos/actual/abonos`
  - Devuelve la lista de abonos registrados en el turno activo del cajero.

### 8.4 Plan de Implementación TDD (US-021)

1. **Migración Flyway V48:** `abono` y FK a `movimiento_cuenta`.
2. **Entidades JPA:**
   - Crear entidad `Abono` y `AbonoRepositorio`.
   - Mapear `abonoId` en `MovimientoCuenta`.
3. **Caso de Uso Backend `RegistrarAbonoEnCaja`:**
   - Pruebas unitarias exhaustivas en `RegistrarAbonoEnCajaTest`:
     - Abono en efectivo: aprobado de inmediato, genera movimiento de caja y de cuenta, reduce deuda en resumen.
     - Abono digital (Yape): queda pendiente, no genera movimiento de caja ni libro de cuenta, valida número de operación.
     - Validación de turno abierto y local activo.
     - Validación de pensionista inexistente y monto <= 0.
     - Abono a pensionista inactivo aceptado.
     - Alerta de duplicidad de número de operación.
4. **Controlador REST:**
   - Métodos en `CajaController` (`POST /turnos/actual/abonos`, `GET /turnos/actual/abonos`).
   - Permisos en `ConfiguracionSeguridad` (`POST /turnos/actual/abonos` con `caja.crear`).
   - Pruebas en `CajaAbonoControllerTest`.
5. **Frontend Web:**
   - Clientes API en `api/cliente.ts`: `registrarAbonoEnCaja`, `listarAbonosDelTurno`.
   - Componente `DialogoRegistrarAbono.tsx`: selector de pensionista, medio, monto, cuenta destino, número de operación y teclado táctil.
   - Componente `ConstanciaAbono.tsx`: ticket térmico imprimible.
   - Integración en `PantallaCaja.tsx`: botón "Abono pensionista" y visualización de abonos en transacciones del turno.
6. **Verificación:** Pruebas unitarias backend (`mvn test`), compilación frontend y prueba end-to-end.

---

## 9. Entrega 4: US-024 (Aprobar o rechazar abonos digitales)

**Objetivo:** El Administrador Central revisa en una bandeja dedicada (`ADM-07`) los abonos registrados por medios digitales (Yape, Plin, Transferencia bancaria) en caja o portal que se encuentren en estado `pendiente`. El administrador verifica el dinero en la cuenta bancaria de la empresa y decide aprobar o rechazar el abono. Al aprobar, se acredita el saldo a la cuenta corriente del pensionista y se audita el evento. Al rechazar, no se altera el saldo y se exige un motivo formal del catálogo de motivos, registrando la auditoría correspondiente.

### 9.1 Decisiones de Diseño y Reglas de Negocio

1. **Ubicación en UI y Acceso (`ADM-07`):**
   - Acceso desde el menú lateral de administración: **Pensionistas → Abonos pendientes** con un indicador numérico (badge) que muestra la cantidad de solicitudes en estado `pendiente`.
   - Requiere permiso `pensionistas.administrar` para aprobar o rechazar (exclusivo de Administrador Central, CA1).
2. **Bandeja y Filtros:**
   - Pestañas de estado:
     - **Pendientes** (activa por defecto).
     - **Aprobados**.
     - **Rechazados**.
     - **Todos**.
   - Buscador rápido por nombre/DNI del pensionista o número de operación.
   - Datos por fila: Fecha de operación, Pensionista (DNI y nombre), Medio de pago con insignia (Yape, Plin, Transferencia), N° de Operación (con botón para copiar al portapapeles), Cuenta destino, Monto en soles (S/ XX.XX), Origen (Caja / Portal), Botón "Ver comprobante" (si hay adjunto) o datos del registro, y botones de acción **"Aprobar"** y **"Rechazar"**.
3. **Flujo de Aprobación (CA2):**
   - Endpoint: `POST /api/pensionistas/abonos/{id}/aprobar`.
   - Valida que el abono exista y esté en estado `pendiente`. Si ya fue procesado, retorna error `ABONO_YA_PROCESADO`.
   - Ejecuta `AcreditarAbono`:
     - Genera `movimiento_cuenta` (`tipo = 'abono'`, `signo = 'credito'`, `importe = abono.monto`, `abono_id = abono.id`).
     - Reduce la deuda o incrementa el saldo a favor del pensionista en su cuenta corriente.
   - Actualiza `abono`: `estado = 'aprobado'`, `aprobado_por = actorId`, `aprobado_en = now()`.
   - Registra evento en `auditoria`: `evento = 'aprobacion_abono'`, `entidad = 'abono'`, `entidad_id = abono.id`, `valor_nuevo = { estado: "aprobado", monto: ... }`.
4. **Flujo de Rechazo (CA3, RF-005, RN-004):**
   - Endpoint: `POST /api/pensionistas/abonos/{id}/rechazar`.
   - Request Body: `{ "motivoId": "01...", "observacion": "..." }`.
   - Valida que el abono exista y esté en estado `pendiente`.
   - Valida que el `motivoId` pertenezca al catálogo `motivo` con `tipo = 'rechazo_abono'` y esté activo. Si el motivo tiene `requiere_observacion = true`, la observación no puede estar vacía.
   - Actualiza `abono`: `estado = 'rechazado'`, `aprobado_por = actorId`, `aprobado_en = now()`, `motivo_rechazo_id = motivoId`, `observacion = observacionLimpia`.
   - **No altera el saldo** de la cuenta del pensionista ni genera movimientos contables.
   - Registra evento en `auditoria`: `evento = 'rechazo_abono'`, `entidad = 'abono'`, `entidad_id = abono.id`, `motivo_id = motivoId`, `observacion = ...`.
5. **Comprobante Adjunto (RN-092):**
   - Modal de previsualización de comprobante (`ruta_adjunto`) para imágenes y PDFs. Si el abono no posee archivo adjunto (fue registrado en caja con solo el N° de operación), el modal presenta una ficha resumen con los datos bancarios para su rápida validación.

### 9.2 Modelo de Datos y Migración Flyway (V49)

Migración `V49__motivos_rechazo_abono.sql` para sembrar el catálogo de motivos iniciales (RF-005):
```sql
INSERT INTO motivo (id, tipo, descripcion, requiere_observacion, activo) VALUES
    ('01M49MOTIVO000000000000R01', 'rechazo_abono', 'Operación no encontrada en cuenta bancaria', FALSE, TRUE),
    ('01M49MOTIVO000000000000R02', 'rechazo_abono', 'Monto no coincide con el extracto bancario', TRUE, TRUE),
    ('01M49MOTIVO000000000000R03', 'rechazo_abono', 'Comprobante ilegible, incompleto o adulterado', FALSE, TRUE),
    ('01M49MOTIVO000000000000R04', 'rechazo_abono', 'Número de operación duplicado o ya utilizado', FALSE, TRUE),
    ('01M49MOTIVO000000000000R05', 'rechazo_abono', 'Otro motivo', TRUE, TRUE)
ON CONFLICT DO NOTHING;
```

### 9.3 Contratos API Backend

- `GET /api/pensionistas/abonos/pendientes`:
  - Devuelve la lista de abonos digitales con `estado = 'pendiente'` ordenados por `creado_en ASC`.
  - Seguridad: Requiere `pensionistas.ver` o `pensionistas.administrar`.
- `GET /api/pensionistas/abonos`:
  - Parámetros opcionales: `?estado=pendiente|aprobado|rechazado` y `?busqueda=...`.
  - Seguridad: Requiere `pensionistas.ver` o `pensionistas.administrar`.
- `POST /api/pensionistas/abonos/{id}/aprobar`:
  - Seguridad: Requiere `pensionistas.administrar` (Administrador central).
  - Retorna `200 OK` con datos del abono aprobado y saldo resultante.
- `POST /api/pensionistas/abonos/{id}/rechazar`:
  - Body: `{ "motivoId": "...", "observacion": "..." }`.
  - Seguridad: Requiere `pensionistas.administrar`.
  - Retorna `200 OK` con datos del abono rechazado.


