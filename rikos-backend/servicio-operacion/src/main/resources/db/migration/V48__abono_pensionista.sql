-- Sprint 8, Entrega 3 (US-021): Registro de abonos del pensionista en caja (y base para US-024).
-- Modelo especificado en docs/producto/modelo-de-datos/05-pensionistas-planes-cuenta-corriente--ep04.md § 7.11
-- y docs/producto/plan/sprint-08-circuito-del-pensionista.md § 8.

CREATE TABLE abono (
    id                 CHAR(26) PRIMARY KEY,
    pensionista_id     CHAR(26) NOT NULL REFERENCES pensionista(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    local_id           CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    medio              VARCHAR(20) NOT NULL CHECK (medio IN ('efectivo', 'yape', 'plin', 'transferencia')),
    monto              BIGINT NOT NULL CHECK (monto > 0),
    fecha_operacion    DATE NOT NULL,
    numero_operacion   VARCHAR(50),
    cuenta_destino     VARCHAR(80),
    ruta_adjunto       VARCHAR(500),
    origen             VARCHAR(20) NOT NULL CHECK (origen IN ('caja', 'portal')),
    estado             VARCHAR(20) NOT NULL CHECK (estado IN ('pendiente', 'aprobado', 'rechazado')),
    registrado_por     CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    aprobado_por       CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    aprobado_en        TIMESTAMPTZ,
    motivo_rechazo_id  CHAR(26) REFERENCES motivo(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    turno_caja_id      CHAR(26) REFERENCES turno_caja(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    observacion        VARCHAR(500),
    creado_en          TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- RN-030: En caja, el efectivo queda confirmado de inmediato y pertenece al turno activo.
    CONSTRAINT abono_efectivo_caja CHECK (
        origen <> 'caja' OR medio <> 'efectivo' OR (turno_caja_id IS NOT NULL AND estado = 'aprobado')
    ),
    -- RN-031: En caja, los abonos digitales requieren obligatoriamente el número de operación.
    CONSTRAINT abono_digital_operacion CHECK (
        medio = 'efectivo' OR origen <> 'caja' OR numero_operacion IS NOT NULL
    )
);

CREATE INDEX ix_abono_pensionista ON abono(pensionista_id, creado_en DESC);
CREATE INDEX ix_abono_local_estado ON abono(local_id, estado);
CREATE INDEX ix_abono_turno ON abono(turno_caja_id);
CREATE INDEX ix_abono_numero_operacion ON abono(numero_operacion) WHERE numero_operacion IS NOT NULL;

-- Clave foranea de movimiento_cuenta a abono (preparada en V28 para sprint 8)
ALTER TABLE movimiento_cuenta ADD CONSTRAINT fk_movimiento_cuenta_abono
    FOREIGN KEY (abono_id) REFERENCES abono(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
