-- Sprint 7, entrega 5 (US-035): reabrir un cobro mientras su turno de origen siga abierto (RN-073). El cobro no se borra: queda
-- revertido (quien, cuando y por que) y su dinero deja de contar en el turno. Las lineas de un cobro revertido dejan de ser
-- "vigentes", asi que la misma linea se puede cobrar de nuevo (el unico por linea pasa a ser parcial).

ALTER TABLE cobro
    ADD COLUMN revertido_en            TIMESTAMPTZ,
    ADD COLUMN revertido_por           CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD COLUMN motivo_reapertura_id    CHAR(26) REFERENCES motivo(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD COLUMN observacion_reapertura  VARCHAR(250),
    ADD CONSTRAINT cobro_revertido_con_datos CHECK (
        revertido_en IS NULL OR (revertido_por IS NOT NULL AND motivo_reapertura_id IS NOT NULL));
CREATE INDEX ix_cobro_revertido_por ON cobro(revertido_por) WHERE revertido_por IS NOT NULL;
CREATE INDEX ix_cobro_motivo_reapertura ON cobro(motivo_reapertura_id) WHERE motivo_reapertura_id IS NOT NULL;

ALTER TABLE cobro_linea ADD COLUMN vigente BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE cobro_linea DROP CONSTRAINT cobro_linea_una_vez;
CREATE UNIQUE INDEX cobro_linea_una_vez ON cobro_linea(detalle_pedido_id) WHERE vigente;
