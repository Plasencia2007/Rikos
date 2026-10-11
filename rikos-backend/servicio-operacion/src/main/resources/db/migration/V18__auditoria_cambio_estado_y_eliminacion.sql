-- Sprint 3, US-006: la auditoria pasa de ser codigo a medida para
-- cambios de permiso a un servicio transversal (RegistradorAuditoria).
-- Dos eventos nuevos que 13.1 no contemplaba, agregados porque el equipo
-- decidio permitir el borrado real (DT-043/DT-051): sin una fila de
-- auditoria, un registro borrado desaparece sin dejar rastro.
--
-- Postgres no deja alterar un CHECK in-place: se borra y se recrea con
-- la lista ampliada.

ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;

ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion'));
