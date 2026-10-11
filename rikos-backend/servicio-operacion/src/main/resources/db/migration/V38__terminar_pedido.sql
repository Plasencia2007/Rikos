-- Sprint 6b, adenda "Terminar pedido": un cierre SIN cobro, provisional hasta el sprint 7. Agrega el estado 'cerrado' del
-- pedido (el mismo que el sprint 7 necesita para cobrar y cerrar) con quien y cuando lo cerro, y el evento de bitacora.
-- 'anulado' del pedido sigue fuera: llega con el sprint 7.

ALTER TABLE pedido DROP CONSTRAINT pedido_estado_check;
ALTER TABLE pedido ADD CONSTRAINT pedido_estado_check
    CHECK (estado IN ('abierto', 'comandado', 'pendiente_cierre', 'cerrado'));

ALTER TABLE pedido
    ADD COLUMN cerrado_en  TIMESTAMPTZ,
    ADD COLUMN cerrado_por CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD CONSTRAINT pedido_cerrado_con_datos
        CHECK (estado <> 'cerrado' OR (cerrado_en IS NOT NULL AND cerrado_por IS NOT NULL));

CREATE INDEX ix_pedido_cerrado_por ON pedido(cerrado_por) WHERE cerrado_por IS NOT NULL;

ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro', 'cambio_credencial', 'movimiento_cuenta', 'cambio_limite',
                       'cambio_mesa', 'bloqueo_mesa', 'precio_manual', 'comanda_emitida',
                       'reimpresion_comanda', 'pedido_terminado'));
