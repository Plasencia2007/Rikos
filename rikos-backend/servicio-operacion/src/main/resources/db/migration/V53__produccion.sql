-- US-048 (bloque 9 del modelo de datos, §11.6): Producción de subrecetas y platos
-- Registro de porciones planificadas, preparadas y vendidas, y rendimiento obtenido.

CREATE TABLE produccion (
    id                     CHAR(26) PRIMARY KEY,
    local_id               CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    producto_id            CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    receta_id              CHAR(26) NOT NULL REFERENCES receta(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    fecha                  DATE NOT NULL,
    servicio               VARCHAR(20) CHECK (servicio IN ('desayuno', 'almuerzo', 'cena')),
    porciones_planificadas INTEGER,
    porciones_preparadas   INTEGER NOT NULL CHECK (porciones_preparadas > 0),
    porciones_vendidas     INTEGER NOT NULL DEFAULT 0,
    rendimiento_obtenido   NUMERIC(14, 6) NOT NULL CHECK (rendimiento_obtenido > 0),
    sobrante               NUMERIC(14, 6),
    destino_sobrante       VARCHAR(20) CHECK (destino_sobrante IN ('conservar', 'personal', 'perdida', 'reutilizacion', 'otro')),
    motivo_id              CHAR(26) REFERENCES motivo(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    estado                 VARCHAR(20) NOT NULL DEFAULT 'confirmada' CHECK (estado IN ('borrador', 'confirmada')),
    registrado_por         CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    creado_en              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_produccion_local_id ON produccion(local_id);
CREATE INDEX ix_produccion_fecha ON produccion(fecha);
CREATE INDEX ix_produccion_producto_id ON produccion(producto_id);
CREATE INDEX ix_produccion_receta_id ON produccion(receta_id);

-- Semilla de permisos para producción
INSERT INTO permiso (id, modulo, accion, codigo, descripcion)
VALUES
    ('01J9PRODVER000000000000001', 'produccion', 'ver', 'produccion.ver', 'Ver historial y seguimiento de producción'),
    ('01J9PRODREG000000000000002', 'produccion', 'crear', 'produccion.crear', 'Registrar producción de subrecetas y platos'),
    ('01J9PRODREG000000000000003', 'produccion', 'crear', 'produccion.registrar', 'Registrar producción de subrecetas y platos (alias)')
ON CONFLICT (codigo) DO NOTHING;

-- Asignar permisos a roles: administrador central, administrador de local y cocina
INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r, permiso p
WHERE r.nombre IN ('administrador central', 'administrador de local', 'cocina')
  AND p.codigo IN ('produccion.ver', 'produccion.crear', 'produccion.registrar')
ON CONFLICT DO NOTHING;

-- Eventos de auditoria para produccion
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
                       'registro_compra', 'confirmacion_compra', 'registro_produccion'));
