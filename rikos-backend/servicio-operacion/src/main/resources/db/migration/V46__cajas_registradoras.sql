-- Cajas registradoras (Configuracion, sprint 7): cada caja lleva un CODIGO unico en su local (CAJ-01...), una sola es la PRINCIPAL (la
-- que viene elegida al abrir turno) y el turno guarda una nota de apertura opcional. La caja sembrada deja de llamarse "Caja 1" y pasa a
-- "Caja Principal" (CAJ-01). El POS solo elige entre las cajas registradas; se administran desde Configuracion.

ALTER TABLE caja
    ADD COLUMN codigo     VARCHAR(20),
    ADD COLUMN principal  BOOLEAN NOT NULL DEFAULT FALSE;

-- Las cajas que ya existen: codigo CAJ-NN por orden de creacion dentro de su local, y la mas antigua es la principal.
WITH numeradas AS (
    SELECT id, row_number() OVER (PARTITION BY local_id ORDER BY creado_en, id) AS n FROM caja
)
UPDATE caja c
SET codigo = 'CAJ-' || lpad(numeradas.n::text, 2, '0'),
    principal = (numeradas.n = 1)
FROM numeradas
WHERE numeradas.id = c.id;

-- La sembrada "Caja 1" se llama ahora "Caja Principal" (si el local no tiene ya otra con ese nombre).
UPDATE caja c SET nombre = 'Caja Principal'
WHERE c.nombre = 'Caja 1' AND c.principal
  AND NOT EXISTS (SELECT 1 FROM caja o WHERE o.local_id = c.local_id AND lower(o.nombre) = 'caja principal');

ALTER TABLE caja ALTER COLUMN codigo SET NOT NULL;
ALTER TABLE caja ADD CONSTRAINT caja_codigo_no_vacio CHECK (length(btrim(codigo)) > 0);
CREATE UNIQUE INDEX caja_codigo_unico_por_local ON caja(local_id, lower(codigo));
-- A lo sumo una caja principal por local.
CREATE UNIQUE INDEX caja_una_principal_por_local ON caja(local_id) WHERE principal;

ALTER TABLE turno_caja ADD COLUMN nota_apertura VARCHAR(250);

ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro', 'cambio_credencial', 'movimiento_cuenta', 'cambio_limite',
                       'cambio_mesa', 'bloqueo_mesa', 'precio_manual', 'comanda_emitida',
                       'reimpresion_comanda', 'pedido_terminado', 'turno_abierto', 'caja_creada',
                       'cobro_registrado', 'caja_modificada'));
