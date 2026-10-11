-- Sprint 6, entrega 3 (US-030 comanda, US-031 anulaciones).
--
--  * comanda: la foto de lo que se mando a cocina, numerada por pedido. Cada impresion se cuenta (impresiones);
--    desde la segunda sale con la leyenda COPIA. estado_impresion: pendiente | impresa | fallida (esta ultima
--    queda para cuando exista un agente de impresion).
--  * detalle_comanda: una fila por linea comandada. UNIQUE(detalle_pedido_id): una linea sale en UNA sola comanda.
--  * detalle_pedido gana los datos de la anulacion. anulado_post_comanda decide, en el sprint 9, si la receta se
--    descuenta igual (RN-065).
--  * Eventos de bitacora: comanda_emitida y reimpresion_comanda.

CREATE TABLE comanda (
    id                   CHAR(26) PRIMARY KEY,
    pedido_id            CHAR(26) NOT NULL REFERENCES pedido(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    local_id             CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    numero               INTEGER NOT NULL CHECK (numero > 0),
    emitida_en           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    emitida_por          CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    impresiones          INTEGER NOT NULL DEFAULT 0 CHECK (impresiones >= 0),
    ultima_impresion_en  TIMESTAMPTZ,
    estado_impresion     VARCHAR(10) NOT NULL DEFAULT 'pendiente'
                         CHECK (estado_impresion IN ('pendiente', 'impresa', 'fallida')),
    CONSTRAINT comanda_numero_unico UNIQUE (pedido_id, numero)
);

CREATE INDEX ix_comanda_local ON comanda(local_id);
CREATE INDEX ix_comanda_emitida_por ON comanda(emitida_por);

CREATE TABLE detalle_comanda (
    id                 CHAR(26) PRIMARY KEY,
    comanda_id         CHAR(26) NOT NULL REFERENCES comanda(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    detalle_pedido_id  CHAR(26) NOT NULL REFERENCES detalle_pedido(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    cantidad           INTEGER NOT NULL CHECK (cantidad > 0),
    CONSTRAINT detalle_comanda_linea_unica UNIQUE (detalle_pedido_id)
);

CREATE INDEX ix_detalle_comanda_comanda ON detalle_comanda(comanda_id);

ALTER TABLE detalle_pedido
    ADD COLUMN anulado_en           TIMESTAMPTZ,
    ADD COLUMN anulado_por          CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD COLUMN motivo_anulacion_id  CHAR(26) REFERENCES motivo(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD COLUMN observacion_anulacion VARCHAR(250),
    ADD COLUMN anulado_post_comanda BOOLEAN NOT NULL DEFAULT FALSE,
    ADD CONSTRAINT detalle_anulado_con_datos
        CHECK (estado <> 'anulado' OR (anulado_en IS NOT NULL AND anulado_por IS NOT NULL
                                       AND motivo_anulacion_id IS NOT NULL)),
    ADD CONSTRAINT detalle_post_comanda_solo_si_anulado
        CHECK (NOT anulado_post_comanda OR estado = 'anulado');

CREATE INDEX ix_detalle_pedido_anulado_por ON detalle_pedido(anulado_por) WHERE anulado_por IS NOT NULL;
CREATE INDEX ix_detalle_pedido_motivo ON detalle_pedido(motivo_anulacion_id) WHERE motivo_anulacion_id IS NOT NULL;

ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro', 'cambio_credencial', 'movimiento_cuenta', 'cambio_limite',
                       'cambio_mesa', 'bloqueo_mesa', 'precio_manual', 'comanda_emitida',
                       'reimpresion_comanda'));
