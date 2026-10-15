-- US-049 y US-051 (bloque 9 del modelo de datos, §§ 11.6 y 11.7)
-- 1. Tabla de consumo interno del personal
-- 2. Permisos para sobrantes de produccion y consumo interno
-- 3. Eventos de auditoria correspondientes

CREATE TABLE consumo_interno (
    id                    CHAR(26) PRIMARY KEY,
    local_id              CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    usuario_id            CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    fecha                 DATE NOT NULL,
    servicio              VARCHAR(20) NOT NULL CHECK (servicio IN ('desayuno', 'almuerzo', 'cena')),
    turno                 VARCHAR(20),
    producto_id           CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    cantidad              NUMERIC(14, 6) NOT NULL CHECK (cantidad > 0),
    costo                 NUMERIC(14, 4) NOT NULL CHECK (costo >= 0),
    requirio_autorizacion BOOLEAN NOT NULL DEFAULT FALSE,
    autorizado_por        CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    observacion           VARCHAR(250),
    movimiento_id         CHAR(26) REFERENCES movimiento_inventario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    creado_en             TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_consumo_interno_local ON consumo_interno(local_id);
CREATE INDEX ix_consumo_interno_usuario ON consumo_interno(usuario_id);
CREATE INDEX ix_consumo_interno_fecha_servicio ON consumo_interno(local_id, fecha, servicio);

-- Semilla de permisos
INSERT INTO permiso (id, modulo, accion, codigo, descripcion)
VALUES
    ('01J9PRODSOB000000000000001', 'produccion', 'modificar', 'produccion.sobrante', 'Registrar destino de sobrantes de producción'),
    ('01J9CONSINV000000000000001', 'consumo_interno', 'ver', 'consumo_interno.ver', 'Ver registros de consumo interno del personal'),
    ('01J9CONSINR000000000000002', 'consumo_interno', 'crear', 'consumo_interno.crear', 'Registrar consumo interno del personal'),
    ('01J9CONSINR000000000000003', 'consumo_interno', 'crear', 'consumo_interno.registrar', 'Registrar consumo interno del personal (alias)'),
    ('01J9CONSINA000000000000004', 'consumo_interno', 'aprobar', 'consumo_interno.autorizar', 'Autorizar segunda ración de consumo interno del personal')
ON CONFLICT (codigo) DO NOTHING;

-- Asignar permisos a roles principales (administrador central, administrador de local, cajero, cocina)
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r, permiso p
WHERE r.nombre IN ('administrador central', 'administrador de local', 'cajero', 'cocina')
  AND p.codigo IN ('produccion.sobrante', 'consumo_interno.ver', 'consumo_interno.crear', 'consumo_interno.registrar', 'consumo_interno.autorizar')
ON CONFLICT DO NOTHING;

-- Eventos de auditoria
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
                       'registro_compra', 'confirmacion_compra', 'registro_produccion',
                       'sobrante_produccion', 'consumo_interno'));
