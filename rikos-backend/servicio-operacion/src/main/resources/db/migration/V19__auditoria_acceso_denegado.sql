-- Caso critico 19 (13.1): "el intento denegado tambien se registra -- sin
-- eso, la auditoria solo ve lo que si ocurrio". Un usuario sin permiso que
-- intenta anular, borrar o entrar a algo que no le corresponde no dejaba
-- ningun rastro hasta ahora.
--
-- Postgres no deja alterar un CHECK in-place: se borra y se recrea con la
-- lista ampliada (mismo patron que V18).

ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;

ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado'));
