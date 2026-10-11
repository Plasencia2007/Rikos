-- Sprint 5, entrega 3 (US-016): la ficha del pensionista, su responsable y su
-- cuenta corriente vacia. Decisiones en
-- docs/producto/plan/sprint-05-cuenta-del-pensionista.md (seccion 3.2).

-- Difiere del modelo original (7.3) en tres cosas, decididas con el usuario:
--  * comidas habituales (tres banderas): con eso se calcula el tope de credito;
--    NO limitan lo que puede consumir. Se descarto el "local habitual".
--  * limite_credito nulo = "calculado" (lo usa la entrega 4); con valor = fijado
--    a mano por pedido del responsable.
--  * hash_contrasena_portal nulo por ahora: como se asigna, entrega y recupera
--    esta sin definir (RF-027) y el portal es de otro sprint.
CREATE TABLE pensionista (
    id                       CHAR(26) PRIMARY KEY,
    dni                      CHAR(8) NOT NULL CHECK (dni ~ '^[0-9]{8}$'),
    nombres                  VARCHAR(120) NOT NULL,
    apellidos                VARCHAR(120) NOT NULL,
    telefono                 VARCHAR(20),
    correo                   VARCHAR(160),
    institucion              VARCHAR(150),
    codigo_estudiante        VARCHAR(30),
    carrera                  VARCHAR(120),
    ciclo                    VARCHAR(20),
    plan_tarifario_id        CHAR(26) NOT NULL REFERENCES plan_tarifario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    come_desayuno            BOOLEAN NOT NULL DEFAULT FALSE,
    come_almuerzo            BOOLEAN NOT NULL DEFAULT TRUE,
    come_cena                BOOLEAN NOT NULL DEFAULT FALSE,
    frecuencia_abono         VARCHAR(10) CHECK (frecuencia_abono IN ('semanal', 'quincenal', 'mensual')),
    limite_credito           BIGINT CHECK (limite_credito > 0),
    hash_pin                 VARCHAR(255) NOT NULL,
    hash_contrasena_portal   VARCHAR(255),
    activo                   BOOLEAN NOT NULL DEFAULT TRUE,
    version                  INTEGER NOT NULL DEFAULT 0,
    creado_en                TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por               CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    actualizado_en           TIMESTAMPTZ,
    actualizado_por          CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CHECK (come_desayuno OR come_almuerzo OR come_cena)
);

-- US-016 CA3, RN-036: el DNI es unico en TODA la empresa, no por local. En la
-- base y no solo en la aplicacion: dos altas simultaneas no pueden duplicarlo.
ALTER TABLE pensionista ADD CONSTRAINT uq_pensionista_dni UNIQUE (dni);
CREATE INDEX ix_pensionista_plan ON pensionista(plan_tarifario_id);

-- 7.4: el responsable es obligatorio y los seis campos tambien. Tabla aparte
-- porque son seis campos con identidad propia; relacion 1-1.
CREATE TABLE responsable (
    id               CHAR(26) PRIMARY KEY,
    pensionista_id   CHAR(26) NOT NULL UNIQUE REFERENCES pensionista(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    nombre_completo  VARCHAR(200) NOT NULL,
    dni              CHAR(8) NOT NULL CHECK (dni ~ '^[0-9]{8}$'),
    telefono         VARCHAR(20) NOT NULL,
    correo           VARCHAR(160) NOT NULL,
    parentesco       VARCHAR(60) NOT NULL,
    direccion        VARCHAR(250) NOT NULL
);

-- 7.5: una cuenta por pensionista, valida en todos los locales. Su libro de
-- movimientos llega en la entrega 4. La cuenta no se cierra ni se recrea al
-- desactivar y reactivar al pensionista (RN-034).
CREATE TABLE cuenta_pensionista (
    id              CHAR(26) PRIMARY KEY,
    pensionista_id  CHAR(26) NOT NULL UNIQUE REFERENCES pensionista(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    abierta_en      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    estado          VARCHAR(12) NOT NULL DEFAULT 'abierta' CHECK (estado IN ('abierta', 'suspendida'))
);

-- Evento nuevo: regenerar el PIN es un cambio de credencial y queda en la
-- bitacora (sin guardar el PIN). Mismo patron que V18, V19, V24, V26.
ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro', 'cambio_credencial'));
