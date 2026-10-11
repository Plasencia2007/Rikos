-- Sprint 6, entrega 1 (US-027): las mesas de cada local.
-- Decisiones en docs/producto/plan/sprint-06-vender-en-mesa.md (3.2 y 4.1).
--
-- Difiere del modelo original: NO hay columna "estado". Libre, ocupada, unida y
-- pendiente de cierre se derivan al consultar (de los pedidos y de
-- mesa_principal_id); solo "bloqueada" es un dato propio de la mesa, con su
-- motivo, quien y cuando.
CREATE TABLE mesa (
    id                 CHAR(26) PRIMARY KEY,
    local_id           CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    numero             VARCHAR(10) NOT NULL,
    nombre             VARCHAR(60),
    capacidad          INTEGER,
    bloqueada          BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_bloqueo     VARCHAR(250),
    bloqueada_por      CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    bloqueada_en       TIMESTAMPTZ,
    -- Si esta unida, la mesa que encabeza la union (se usa desde la entrega 4).
    mesa_principal_id  CHAR(26) REFERENCES mesa(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    activo             BOOLEAN NOT NULL DEFAULT TRUE,
    version            INTEGER NOT NULL DEFAULT 0,
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por         CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    actualizado_en     TIMESTAMPTZ,
    actualizado_por    CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT mesa_numero_unico_por_local UNIQUE (local_id, numero),
    CONSTRAINT mesa_capacidad_positiva CHECK (capacidad IS NULL OR capacidad > 0),
    CONSTRAINT mesa_bloqueada_con_motivo CHECK (NOT bloqueada OR (motivo_bloqueo IS NOT NULL AND btrim(motivo_bloqueo) <> '')),
    CONSTRAINT mesa_no_se_une_a_si_misma CHECK (mesa_principal_id IS NULL OR mesa_principal_id <> id)
);

CREATE INDEX ix_mesa_local ON mesa(local_id, numero);

ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro', 'cambio_credencial', 'movimiento_cuenta', 'cambio_limite',
                       'cambio_mesa', 'bloqueo_mesa'));
