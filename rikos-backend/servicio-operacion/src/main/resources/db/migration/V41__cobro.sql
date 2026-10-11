-- Sprint 7, entrega 2 (US-033): el cobro. Un cobro es un acto de cobrar LINEAS de un pedido (no cuentas completas) en el
-- turno de un cajero. Decisiones en docs/producto/plan/sprint-07-cobrar-y-cerrar.md (4.2 a 4.6).
--
-- Difiere del modelo original (9.5, 9.6) en dos cosas, ambas por el cobro por lineas:
--  * no hay tabla medio_pago: el medio es un texto con CHECK (efectivo, yape, plin, tarjeta); "cuenta del pensionista" no es
--    un medio de pago de caja sino un cargo (cobro_cargo), porque no mueve dinero en el turno;
--  * `cobro_linea` guarda el snapshot economico de cada linea cobrada (precio, descuento, costo, utilidad, composicion).

-- Una linea cobrada ya no se modifica, ni se quita, ni se anula, ni se traslada (la reapertura es la entrega 5).
ALTER TABLE detalle_pedido DROP CONSTRAINT detalle_pedido_estado_check;
ALTER TABLE detalle_pedido ADD CONSTRAINT detalle_pedido_estado_check
    CHECK (estado IN ('pendiente', 'comandado', 'anulado', 'cobrado'));

CREATE TABLE cobro (
    id              CHAR(26) PRIMARY KEY,
    pedido_id       CHAR(26) NOT NULL REFERENCES pedido(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    local_id        CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    turno_caja_id   CHAR(26) NOT NULL REFERENCES turno_caja(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    cobrado_por     CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    cobrado_en      TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- Lo que sumaron las lineas = lo cobrado en medios + lo cargado a cuentas de pensionistas.
    total           BIGINT NOT NULL CHECK (total > 0),
    total_medios    BIGINT NOT NULL CHECK (total_medios >= 0),
    total_cuentas   BIGINT NOT NULL CHECK (total_cuentas >= 0),
    vuelto          BIGINT NOT NULL DEFAULT 0 CHECK (vuelto >= 0),
    CONSTRAINT cobro_total_cuadra CHECK (total = total_medios + total_cuentas)
);
CREATE INDEX ix_cobro_pedido ON cobro(pedido_id);
CREATE INDEX ix_cobro_turno ON cobro(turno_caja_id);
CREATE INDEX ix_cobro_cobrado_por ON cobro(cobrado_por);
CREATE INDEX ix_cobro_local ON cobro(local_id, cobrado_en DESC);

CREATE TABLE cobro_linea (
    id                  CHAR(26) PRIMARY KEY,
    cobro_id            CHAR(26) NOT NULL REFERENCES cobro(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    detalle_pedido_id   CHAR(26) NOT NULL REFERENCES detalle_pedido(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    cuenta_persona_id   CHAR(26) NOT NULL REFERENCES cuenta_persona(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    pensionista_id      CHAR(26) REFERENCES pensionista(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    destino             VARCHAR(10) NOT NULL CHECK (destino IN ('medios', 'cuenta')),
    producto_id         CHAR(26) REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    tipo_linea          VARCHAR(14) NOT NULL,
    nombre              VARCHAR(160) NOT NULL,
    cantidad            INTEGER NOT NULL CHECK (cantidad > 0),
    -- El snapshot (RN-005, RN-046): precio, taper y descuento tal como se cobraron.
    precio_unitario     BIGINT NOT NULL CHECK (precio_unitario >= 0),
    taper_unitario      BIGINT NOT NULL DEFAULT 0 CHECK (taper_unitario >= 0),
    descuento           BIGINT NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    importe             BIGINT NOT NULL CHECK (importe >= 0),
    -- Costo por unidad de la receta aplicada (DT-054); NULO si algun insumo no tiene costo todavia: nunca se guarda como cero.
    costo_unitario      NUMERIC(14, 6),
    utilidad            NUMERIC(14, 6),
    composicion         JSONB,
    CONSTRAINT cobro_linea_pensionista_segun_destino CHECK ((destino = 'cuenta') = (pensionista_id IS NOT NULL)),
    CONSTRAINT cobro_linea_una_vez UNIQUE (detalle_pedido_id)
);
CREATE INDEX ix_cobro_linea_cobro ON cobro_linea(cobro_id);
CREATE INDEX ix_cobro_linea_persona ON cobro_linea(cuenta_persona_id);
CREATE INDEX ix_cobro_linea_pensionista ON cobro_linea(pensionista_id) WHERE pensionista_id IS NOT NULL;
CREATE INDEX ix_cobro_linea_producto ON cobro_linea(producto_id) WHERE producto_id IS NOT NULL;

-- Un pago es una parte del cobro en un medio. importe = lo que entra a la caja; en efectivo, monto_recibido puede ser mayor
-- (la diferencia es el vuelto, que no es ingreso).
CREATE TABLE pago (
    id              CHAR(26) PRIMARY KEY,
    cobro_id        CHAR(26) NOT NULL REFERENCES cobro(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    medio           VARCHAR(10) NOT NULL CHECK (medio IN ('efectivo', 'yape', 'plin', 'tarjeta')),
    importe         BIGINT NOT NULL CHECK (importe > 0),
    monto_recibido  BIGINT,
    referencia      VARCHAR(50),
    CONSTRAINT pago_recibido_solo_efectivo CHECK (
        (medio = 'efectivo' AND monto_recibido IS NOT NULL AND monto_recibido >= importe)
        OR (medio <> 'efectivo' AND monto_recibido IS NULL))
);
CREATE INDEX ix_pago_cobro ON pago(cobro_id);

-- RN-069: el unico movimiento de caja que existe es una venta (o, en el sprint 8, el abono en efectivo del pensionista).
-- Un tercer valor seria permitir gastos, retiros o ingresos extraordinarios.
CREATE TABLE movimiento_caja (
    id              CHAR(26) PRIMARY KEY,
    turno_caja_id   CHAR(26) NOT NULL REFERENCES turno_caja(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    local_id        CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    tipo            VARCHAR(20) NOT NULL CHECK (tipo IN ('venta', 'abono_pensionista')),
    medio           VARCHAR(10) NOT NULL CHECK (medio IN ('efectivo', 'yape', 'plin', 'tarjeta')),
    importe         BIGINT NOT NULL CHECK (importe > 0),
    pago_id         CHAR(26) REFERENCES pago(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ocurrido_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    registrado_por  CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT movimiento_venta_con_pago CHECK (tipo <> 'venta' OR pago_id IS NOT NULL)
);
CREATE INDEX ix_movimiento_caja_turno ON movimiento_caja(turno_caja_id, medio);
CREATE INDEX ix_movimiento_caja_pago ON movimiento_caja(pago_id) WHERE pago_id IS NOT NULL;
CREATE INDEX ix_movimiento_caja_registrado_por ON movimiento_caja(registrado_por);
CREATE INDEX ix_movimiento_caja_local ON movimiento_caja(local_id);

-- Lo cargado a la cuenta corriente de un pensionista en este cobro (RN-015): enlaza el cobro con el movimiento del libro.
CREATE TABLE cobro_cargo (
    id                    CHAR(26) PRIMARY KEY,
    cobro_id              CHAR(26) NOT NULL REFERENCES cobro(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    pensionista_id        CHAR(26) NOT NULL REFERENCES pensionista(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    movimiento_cuenta_id  CHAR(26) NOT NULL REFERENCES movimiento_cuenta(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    importe               BIGINT NOT NULL CHECK (importe > 0),
    -- Despues del cargo, la deuda paso del tope: solo avisa (RN-017), nunca bloquea.
    supera_tope           BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT cobro_cargo_uno_por_pensionista UNIQUE (cobro_id, pensionista_id)
);
CREATE INDEX ix_cobro_cargo_pensionista ON cobro_cargo(pensionista_id);
CREATE INDEX ix_cobro_cargo_movimiento ON cobro_cargo(movimiento_cuenta_id);

ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro', 'cambio_credencial', 'movimiento_cuenta', 'cambio_limite',
                       'cambio_mesa', 'bloqueo_mesa', 'precio_manual', 'comanda_emitida',
                       'reimpresion_comanda', 'pedido_terminado', 'turno_abierto', 'caja_creada',
                       'cobro_registrado'));
