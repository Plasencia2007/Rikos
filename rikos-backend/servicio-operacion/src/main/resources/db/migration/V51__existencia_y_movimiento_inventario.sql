-- Sprint 9: US-043 (Mantener existencias por local con stock minimo y alertas)
-- Modelo de datos bloque 9 (EP-08): tablas movimiento_inventario y existencia.
-- RN-002: inventario general por local con clave compuesta (local_id, producto_id).
-- RN-050 / RN-056: existencia.stock_actual permite valores negativos (sin restriccion de >= 0).
-- RN-051: stock_minimo configurable por producto y local en unidad base.
-- RN-057: el Kardex es movimiento_inventario (append-only).

CREATE TABLE movimiento_inventario (
    id                  CHAR(26) PRIMARY KEY,
    local_id            CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    producto_id         CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    origen              VARCHAR(30) NOT NULL CHECK (origen IN
                            ('compra', 'produccion', 'venta', 'ajuste', 'transferencia',
                             'conteo', 'consumo_interno', 'anulacion_post_comanda')),
    signo               VARCHAR(10) NOT NULL CHECK (signo IN ('entrada', 'salida')),
    cantidad            NUMERIC(14, 6) NOT NULL CHECK (cantidad > 0),
    unidad_id           CHAR(26) NOT NULL REFERENCES unidad_medida(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    cantidad_base       NUMERIC(14, 6) NOT NULL CHECK (cantidad_base > 0),
    costo_unitario      NUMERIC(14, 6),
    stock_anterior      NUMERIC(14, 6) NOT NULL,
    stock_nuevo         NUMERIC(14, 6) NOT NULL,
    referencia_tipo     VARCHAR(40),
    referencia_id       CHAR(26),
    motivo_id           CHAR(26) REFERENCES motivo(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    observacion         VARCHAR(250),
    ocurrido_en         TIMESTAMPTZ NOT NULL,
    registrado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    registrado_por      CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE INDEX ix_movimiento_inventario_local_prod ON movimiento_inventario(local_id, producto_id, ocurrido_en DESC);
CREATE INDEX ix_movimiento_inventario_origen ON movimiento_inventario(origen);
CREATE INDEX ix_movimiento_inventario_ref ON movimiento_inventario(referencia_tipo, referencia_id);
CREATE INDEX ix_movimiento_inventario_motivo ON movimiento_inventario(motivo_id);
CREATE INDEX ix_movimiento_inventario_registrado_por ON movimiento_inventario(registrado_por);

CREATE TABLE existencia (
    local_id             CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    producto_id          CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    stock_actual         NUMERIC(14, 6) NOT NULL DEFAULT 0,
    stock_minimo         NUMERIC(14, 6) CHECK (stock_minimo IS NULL OR stock_minimo >= 0),
    ultimo_movimiento_id CHAR(26) REFERENCES movimiento_inventario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    recalculado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (local_id, producto_id)
);

CREATE INDEX ix_existencia_producto_id ON existencia(producto_id);

-- Semilla de permiso inventario.administrar
INSERT INTO permiso (id, modulo, accion, codigo, descripcion)
VALUES ('01J9INVADMIN00000000000001', 'inventario', 'administrar', 'inventario.administrar', 'Administrar existencias y stock mínimo')
ON CONFLICT (codigo) DO NOTHING;

-- Asignar permisos a administrador central y administrador de local
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r, permiso p
WHERE r.nombre IN ('administrador central', 'administrador de local')
  AND p.codigo IN ('inventario.ver', 'inventario.administrar')
ON CONFLICT DO NOTHING;

-- Evento de auditoria para modificacion de stock minimo
ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro', 'cambio_credencial', 'movimiento_cuenta', 'cambio_limite',
                       'cambio_mesa', 'bloqueo_mesa', 'precio_manual', 'comanda_emitida',
                       'reimpresion_comanda', 'pedido_terminado', 'turno_abierto', 'caja_creada',
                       'cobro_registrado', 'caja_modificada', 'cambio_stock_minimo'));

