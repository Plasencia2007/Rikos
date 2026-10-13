-- Sprint 9: US-050 (Registrar compras y actualizar el costo de última compra)
-- Modelo de datos bloque 9 (EP-08): tablas compra y detalle_compra.
-- RN-047: confirmacion actualiza producto.costo_ultima_compra y recalcula recetas futuras.
-- RN-054 / RN-055: aumento de stock en unidad base segun presentacion_compra.
-- RN-057: confirmacion genera movimiento_inventario (origen='compra', signo='entrada').
-- Unicidad condicional de comprobante: (proveedor_id, tipo_documento, serie, numero) para evitar duplicados.

CREATE TABLE compra (
    id               CHAR(26) PRIMARY KEY,
    local_id         CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    proveedor_id     CHAR(26) NOT NULL REFERENCES proveedor(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    tipo_documento   VARCHAR(20) NOT NULL CHECK (tipo_documento IN ('factura', 'boleta', 'ticket', 'sin_documento')),
    serie            VARCHAR(10),
    numero           VARCHAR(20),
    fecha_documento  DATE NOT NULL,
    total            BIGINT NOT NULL CHECK (total >= 0),
    ruta_adjunto     VARCHAR(500),
    estado           VARCHAR(20) NOT NULL DEFAULT 'borrador' CHECK (estado IN ('borrador', 'confirmada', 'anulada')),
    confirmada_en    TIMESTAMPTZ,
    confirmada_por   CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    observacion      VARCHAR(250),
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por       CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- Indice unico para evitar duplicidad de comprobantes emitidos por el mismo proveedor
CREATE UNIQUE INDEX ux_compra_documento_proveedor ON compra (proveedor_id, tipo_documento, serie, numero)
    WHERE tipo_documento != 'sin_documento' AND estado != 'anulada';

CREATE INDEX ix_compra_local_fecha ON compra (local_id, fecha_documento DESC);
CREATE INDEX ix_compra_proveedor ON compra (proveedor_id);
CREATE INDEX ix_compra_estado ON compra (estado);

CREATE TABLE detalle_compra (
    id               CHAR(26) PRIMARY KEY,
    compra_id        CHAR(26) NOT NULL REFERENCES compra(id) ON DELETE CASCADE ON UPDATE CASCADE,
    producto_id      CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    presentacion_id  CHAR(26) REFERENCES presentacion_compra(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    cantidad         NUMERIC(14, 6) NOT NULL CHECK (cantidad > 0),
    cantidad_base    NUMERIC(14, 6) NOT NULL CHECK (cantidad_base > 0),
    precio_unitario  BIGINT NOT NULL CHECK (precio_unitario >= 0),
    subtotal         BIGINT NOT NULL CHECK (subtotal >= 0),
    movimiento_id    CHAR(26) REFERENCES movimiento_inventario(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE INDEX ix_detalle_compra_compra ON detalle_compra (compra_id);
CREATE INDEX ix_detalle_compra_producto ON detalle_compra (producto_id);

-- Semillas de permisos de compras si no estuvieran presentes
INSERT INTO permiso (id, modulo, accion, codigo, descripcion)
VALUES
    ('01J9COMPVER000000000000001', 'compras', 'ver', 'compras.ver', 'Ver compras y proveedores'),
    ('01J9COMPREG000000000000002', 'compras', 'crear', 'compras.crear', 'Registrar compras'),
    ('01J9COMPMOD000000000000003', 'compras', 'modificar', 'compras.modificar', 'Editar una compra registrada')
ON CONFLICT (codigo) DO NOTHING;

-- Asignar permisos a administrador central y administrador de local
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r, permiso p
WHERE r.nombre IN ('administrador central', 'administrador de local')
  AND p.codigo IN ('compras.ver', 'compras.crear', 'compras.modificar')
ON CONFLICT DO NOTHING;

-- Eventos de auditoria para compras
ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro', 'cambio_credencial', 'movimiento_cuenta', 'cambio_limite',
                       'cambio_mesa', 'bloqueo_mesa', 'precio_manual', 'comanda_emitida',
                       'reimpresion_comanda', 'pedido_terminado', 'turno_abierto', 'caja_creada',
                       'cobro_registrado', 'caja_modificada', 'cambio_stock_minimo',
                       'registro_compra', 'confirmacion_compra'));
