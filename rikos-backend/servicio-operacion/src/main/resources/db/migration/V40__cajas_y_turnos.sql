-- Sprint 7, entrega 1 (US-032): cajas y turnos de caja. Un local puede tener varias cajas (el dinero de cada una no se
-- cruza con el de las otras); el codigo crea "Caja 1" la primera vez que un local la necesita, igual que el salon
-- "Principal". Un turno lo abre un cajero en una caja con su fondo inicial y lo cierra cuando quiera (entrega 4).

CREATE TABLE caja (
    id         CHAR(26) PRIMARY KEY,
    local_id   CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    nombre     VARCHAR(60) NOT NULL,
    activo     BOOLEAN NOT NULL DEFAULT TRUE,
    version    INTEGER NOT NULL DEFAULT 0,
    creado_en  TIMESTAMPTZ NOT NULL DEFAULT now(),
    creado_por CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT caja_nombre_no_vacio CHECK (length(btrim(nombre)) > 0)
);

-- Dos cajas del mismo local no se llaman igual (sin distinguir mayusculas).
CREATE UNIQUE INDEX caja_nombre_unico_por_local ON caja(local_id, lower(nombre));

CREATE TABLE turno_caja (
    id             CHAR(26) PRIMARY KEY,
    caja_id        CHAR(26) NOT NULL REFERENCES caja(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    local_id       CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    cajero_id      CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    fondo_inicial  BIGINT NOT NULL CHECK (fondo_inicial >= 0),
    abierto_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    cerrado_en     TIMESTAMPTZ,
    estado         VARCHAR(10) NOT NULL DEFAULT 'abierto',
    version        INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT turno_caja_estado_check CHECK (estado IN ('abierto', 'cerrado')),
    CONSTRAINT turno_caja_cerrado_con_fecha CHECK ((estado = 'cerrado') = (cerrado_en IS NOT NULL))
);

-- Lo que sostiene RN-073: una caja tiene a lo sumo UN turno abierto, y un cajero tambien (su dinero no se cruza).
-- Son indices unicos parciales, asi que dos aperturas simultaneas no pueden pasar ni por una carrera.
CREATE UNIQUE INDEX turno_caja_un_abierto_por_caja ON turno_caja(caja_id) WHERE estado = 'abierto';
CREATE UNIQUE INDEX turno_caja_un_abierto_por_cajero ON turno_caja(cajero_id) WHERE estado = 'abierto';
CREATE INDEX ix_turno_caja_local ON turno_caja(local_id, abierto_en DESC);

ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro', 'cambio_credencial', 'movimiento_cuenta', 'cambio_limite',
                       'cambio_mesa', 'bloqueo_mesa', 'precio_manual', 'comanda_emitida',
                       'reimpresion_comanda', 'pedido_terminado', 'turno_abierto', 'caja_creada'));
