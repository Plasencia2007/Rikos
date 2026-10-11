-- Sprint 5, entrega 1 (US-017): planes tarifarios y parametros globales.
-- Decisiones en docs/producto/plan/sprint-05-cuenta-del-pensionista.md
-- (secciones 3.4 y 3.6).

-- El plan es la identidad ("Plan pensionista"); sus precios viven en
-- versiones con vigencia. Cambiar un precio NUNCA reescribe una fila que ya
-- rigio: cierra la version anterior y abre una nueva (RN-021). Distinto del
-- modelo original (una fila con `version`), que obligaba a repetir nombre y
-- descripcion en cada version y dejaba a pensionista.plan_tarifario_id
-- apuntando a una version en vez de al plan.
CREATE TABLE plan_tarifario (
    id           CHAR(26) PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    descripcion  VARCHAR(250),
    activo       BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por   CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

ALTER TABLE plan_tarifario ADD CONSTRAINT uq_plan_tarifario_nombre UNIQUE (nombre);

-- Precio por COMIDA (no por producto): el pensionista no elige, recoge lo
-- del dia a un precio unico (decision del usuario, sprint 5). En centimos,
-- como producto.precio_venta. Nulo = "sin precio todavia": el motor de
-- precios no inventa uno.
CREATE TABLE plan_tarifario_version (
    id               CHAR(26) PRIMARY KEY,
    plan_id          CHAR(26) NOT NULL REFERENCES plan_tarifario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    precio_desayuno  BIGINT CHECK (precio_desayuno > 0),
    precio_almuerzo  BIGINT CHECK (precio_almuerzo > 0),
    precio_cena      BIGINT CHECK (precio_cena > 0),
    vigente_desde    DATE NOT NULL,
    vigente_hasta    DATE,
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por       CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde)
);

ALTER TABLE plan_tarifario_version ADD CONSTRAINT uq_plan_version_desde UNIQUE (plan_id, vigente_desde);
-- Una sola version abierta por plan: la ultima. Las anteriores estan cerradas.
CREATE UNIQUE INDEX ux_plan_version_abierta ON plan_tarifario_version(plan_id) WHERE vigente_hasta IS NULL;

-- Parametros globales editables (precio del taper, regla del segundo
-- consumo). Con id ULID porque la bitacora (auditoria.entidad_id CHAR(26))
-- lo necesita; `clave` es la que usa el codigo.
CREATE TABLE parametro_global (
    id            CHAR(26) PRIMARY KEY,
    clave         VARCHAR(60) NOT NULL UNIQUE,
    valor         VARCHAR(200),
    cambiado_en   TIMESTAMPTZ,
    cambiado_por  CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- El taper nace SIN monto (`07` lo deja pendiente): sin monto no se cobra
-- taper. El segundo consumo del dia va por defecto con el precio del plan.
INSERT INTO parametro_global (id, clave, valor) VALUES
    ('01M39A0PA0000000000000P001', 'taper_precio', NULL),
    ('01M39A0PA0000000000000P002', 'segundo_consumo_regla', 'precio_del_plan');

-- Accion nueva: administrar planes y parametros (solo el administrador
-- central). pensionistas.modificar no sirve: el administrador de local lo
-- tiene para editar fichas, y no debe tocar precios. Postgres no altera un
-- CHECK en su lugar (mismo patron que V18, V19, V24).
ALTER TABLE permiso DROP CONSTRAINT permiso_accion_check;
ALTER TABLE permiso ADD CONSTRAINT permiso_accion_check
    CHECK (accion IN ('ver', 'crear', 'modificar', 'desactivar', 'aprobar', 'anular', 'descontar', 'reabrir',
                      'ver_costos', 'exportar', 'administrar_permisos', 'marcar_agotado', 'administrar'));

ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro'));
