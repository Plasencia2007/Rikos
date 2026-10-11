-- Sprint 7, entrega 3 (US-034): el descuento por linea al cobrar lleva su motivo y su observacion (RN-072). V41 ya tenia la
-- columna `descuento`; aqui se agrega el porque. Un descuento sin motivo no puede existir, ni en la base.
ALTER TABLE cobro_linea
    ADD COLUMN motivo_descuento_id    CHAR(26) REFERENCES motivo(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD COLUMN observacion_descuento  VARCHAR(250),
    ADD CONSTRAINT cobro_linea_descuento_con_motivo CHECK (descuento = 0 OR motivo_descuento_id IS NOT NULL);

CREATE INDEX ix_cobro_linea_motivo_descuento ON cobro_linea(motivo_descuento_id) WHERE motivo_descuento_id IS NOT NULL;
