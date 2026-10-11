-- Sprint 7, entrega 4 (US-036): el cierre del turno de caja. Guarda lo esperado, lo contado y la diferencia (RN-067, RN-070) y
-- es INALTERABLE (RN-006): ningun UPDATE ni DELETE, ni siquiera desde SQL. Una diferencia desde S/ 0,01 genera alerta en la
-- aplicacion, pero nunca impide el cierre.

ALTER TABLE turno_caja ADD COLUMN cerrado_por CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
CREATE INDEX ix_turno_caja_cerrado_por ON turno_caja(cerrado_por) WHERE cerrado_por IS NOT NULL;

CREATE TABLE cierre_caja (
    id                 CHAR(26) PRIMARY KEY,
    turno_caja_id      CHAR(26) NOT NULL UNIQUE REFERENCES turno_caja(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    local_id           CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- fondo inicial + efectivo cobrado en el turno (el vuelto no cuenta, los cobros reabiertos tampoco).
    efectivo_esperado  BIGINT NOT NULL CHECK (efectivo_esperado >= 0),
    efectivo_contado   BIGINT NOT NULL CHECK (efectivo_contado >= 0),
    -- contado - esperado: negativo = falta dinero, positivo = sobra.
    diferencia         BIGINT NOT NULL,
    total_yape         BIGINT NOT NULL DEFAULT 0 CHECK (total_yape >= 0),
    total_plin         BIGINT NOT NULL DEFAULT 0 CHECK (total_plin >= 0),
    total_tarjeta      BIGINT NOT NULL DEFAULT 0 CHECK (total_tarjeta >= 0),
    observacion        VARCHAR(500),
    cerrado_en         TIMESTAMPTZ NOT NULL DEFAULT now(),
    cerrado_por        CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT cierre_caja_diferencia_cuadra CHECK (diferencia = efectivo_contado - efectivo_esperado)
);
CREATE INDEX ix_cierre_caja_local ON cierre_caja(local_id, cerrado_en DESC);
CREATE INDEX ix_cierre_caja_cerrado_por ON cierre_caja(cerrado_por);

CREATE FUNCTION cierre_caja_inalterable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'cierre_caja es inalterable: un cierre no se modifica ni se borra';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER cierre_caja_sin_cambios BEFORE UPDATE OR DELETE ON cierre_caja
    FOR EACH ROW EXECUTE FUNCTION cierre_caja_inalterable();
