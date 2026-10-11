-- US-015 (modelo de datos §6.4): productos agotados por local. Es la unica
-- parte del menu que no es central. Decisiones en
-- docs/producto/plan/sprint-04-costos-y-menu.md (entrega 4).

-- Una fila por (local, producto), que guarda el ultimo cambio. La historia
-- completa (quien marco, quien reactivo y por que) queda en auditoria.
CREATE TABLE disponibilidad_producto (
    id            CHAR(26) PRIMARY KEY,
    local_id      CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    producto_id   CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    agotado       BOOLEAN NOT NULL,
    -- Solo en la reactivacion (RN-066): marcar es un toque, sin motivo.
    motivo_id     CHAR(26) REFERENCES motivo(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    observacion   VARCHAR(250),
    marcado_por   CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    marcado_en    TIMESTAMPTZ NOT NULL,
    CHECK (agotado OR motivo_id IS NOT NULL)
);

ALTER TABLE disponibilidad_producto ADD CONSTRAINT uq_disponibilidad_local_producto
    UNIQUE (local_id, producto_id);
-- "Que esta agotado en este local" es la consulta del POS.
CREATE INDEX ix_disponibilidad_local_agotado ON disponibilidad_producto(local_id) WHERE agotado;

-- Septimo tipo de motivo (auditoria del sprint 4, punto 4). Postgres no
-- deja alterar un CHECK en su lugar: se borra y se recrea.
ALTER TABLE motivo DROP CONSTRAINT motivo_tipo_check;
ALTER TABLE motivo ADD CONSTRAINT motivo_tipo_check
    CHECK (tipo IN ('ajuste_inventario', 'anulacion', 'descuento', 'rechazo_abono',
                    'sobrante_produccion', 'reapertura', 'disponibilidad'));

-- Un solo motivo sembrado, a pesar de RF-005: sin ninguno, cocina no podria
-- reactivar un agotado hasta que el administrador central cargue la lista.
-- El dueno lo puede editar o desactivar desde Configuracion > Motivos.
INSERT INTO motivo (id, tipo, descripcion, requiere_observacion) VALUES
    ('01M39A0MT0000000000000D001', 'disponibilidad', 'Se volvió a preparar', FALSE);

-- Evento nuevo: reactivar no es "marcar agotado con agotado = falso".
ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion'));
