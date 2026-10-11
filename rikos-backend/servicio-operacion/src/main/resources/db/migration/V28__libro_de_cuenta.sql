-- Sprint 5, entrega 4 (US-018): el libro de la cuenta corriente del
-- pensionista. Decisiones en docs/producto/plan/sprint-05-cuenta-del-pensionista.md
-- (secciones 3.7, 3.8 y 3.10).

-- 7.6: la tabla mas importante del sistema. SOLO SE AGREGAN FILAS: nunca se
-- modifican ni se borran. El saldo NO es una columna: es la suma de los
-- creditos menos la de los cargos (asi dos locales sin internet no se pisan
-- al sincronizar, y el estado de cuenta se puede reconstruir siempre).
--
-- Difiere del modelo original en dos cosas:
--  * tipo 'saldo_inicial': la carga de lo que cada pensionista debe o tiene a
--    favor al empezar. Distinto de 'arrastre' (que es del cierre mensual).
--  * sin periodo_id: los periodos son del cierre mensual (sprint 13), que lo
--    agrega con una migracion que asigna por mes de ocurrido_en.
CREATE TABLE movimiento_cuenta (
    id              CHAR(26) PRIMARY KEY,
    cuenta_id       CHAR(26) NOT NULL REFERENCES cuenta_pensionista(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- Obligatorio: el caso critico 20 exige mostrar en que local ocurrio.
    local_id        CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    tipo            VARCHAR(14) NOT NULL CHECK (tipo IN ('consumo', 'abono', 'ajuste', 'arrastre', 'saldo_inicial')),
    signo           VARCHAR(7) NOT NULL CHECK (signo IN ('cargo', 'credito')),
    importe         BIGINT NOT NULL CHECK (importe > 0),
    -- Sin FK todavia: pedidos y abonos son de los sprints 6 y 8.
    pedido_id       CHAR(26),
    abono_id        CHAR(26),
    motivo_id       CHAR(26) REFERENCES motivo(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    descripcion     VARCHAR(250),
    -- Cuando ocurrio el hecho economico (el estado de cuenta se arma por
    -- esta) y cuando llego a la base (la sincronizacion razona con esta).
    ocurrido_en     TIMESTAMPTZ NOT NULL,
    registrado_en   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    registrado_por  CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- Un ajuste siempre lleva motivo (US-018).
    CHECK (tipo <> 'ajuste' OR motivo_id IS NOT NULL),
    -- Consumo y abono tienen un sentido fijo; el saldo inicial y el ajuste eligen.
    CHECK (tipo <> 'consumo' OR signo = 'cargo'),
    CHECK (tipo <> 'abono' OR signo = 'credito')
);

CREATE INDEX ix_movimiento_cuenta_cuenta ON movimiento_cuenta(cuenta_id, ocurrido_en DESC);
CREATE INDEX ix_movimiento_cuenta_local ON movimiento_cuenta(local_id);
-- Un solo saldo inicial por cuenta: cargarlo dos veces duplicaria la deuda.
-- Un error se corrige con un ajuste con motivo.
CREATE UNIQUE INDEX ux_movimiento_saldo_inicial ON movimiento_cuenta(cuenta_id) WHERE tipo = 'saldo_inicial';

-- La inmutabilidad se hace cumplir en la base, no solo en el codigo: aunque
-- alguien agregue un metodo de borrado a un repositorio o escriba SQL a mano,
-- una fila del libro no se puede tocar. (Un error se corrige con otro
-- movimiento, nunca editando el anterior.)
CREATE FUNCTION movimiento_cuenta_solo_agrega() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'movimiento_cuenta es solo de agregado: no se modifica ni se borra (corrige con otro movimiento)';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_movimiento_cuenta_solo_agrega
    BEFORE UPDATE OR DELETE ON movimiento_cuenta
    FOR EACH ROW EXECUTE FUNCTION movimiento_cuenta_solo_agrega();

-- Tipo de motivo nuevo para los ajustes de cuenta. Mismo patron que V13/V23.
ALTER TABLE motivo DROP CONSTRAINT motivo_tipo_check;
ALTER TABLE motivo ADD CONSTRAINT motivo_tipo_check
    CHECK (tipo IN ('ajuste_inventario', 'anulacion', 'descuento', 'rechazo_abono', 'sobrante_produccion',
                    'reapertura', 'disponibilidad', 'ajuste_cuenta'));

-- Uno sembrado para poder operar desde el primer dia (RF-005 deja los motivos
-- en manos del duenio, pero sin ninguno no se podria corregir una cuenta).
INSERT INTO motivo (id, tipo, descripcion, requiere_observacion) VALUES
    ('01M39A0MT0000000000000D002', 'ajuste_cuenta', 'Corrección de cobro', FALSE);

ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro', 'cambio_credencial', 'movimiento_cuenta', 'cambio_limite'));
