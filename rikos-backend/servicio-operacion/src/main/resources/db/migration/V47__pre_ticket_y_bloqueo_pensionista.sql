-- Sprint 8, Entrega 1 (US-019): Pre-ticket de autoservicio y bloqueo de pensionista
-- Terminal de autoservicio del pensionista (kiosco). Emite pre-ticket con codigo unico de 6 digitos.
-- Incluye bloqueo de PIN individual por pensionista tras 5 fallos consecutivos.

CREATE TABLE pre_ticket (
    id             CHAR(26) PRIMARY KEY,
    codigo         VARCHAR(12) NOT NULL,
    pensionista_id CHAR(26) NOT NULL REFERENCES pensionista(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    local_id       CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    servicio       VARCHAR(20) NOT NULL,
    estado         VARCHAR(20) NOT NULL DEFAULT 'pendiente',
    emitido_en     TIMESTAMPTZ NOT NULL DEFAULT now(),
    vence_en       TIMESTAMPTZ NOT NULL,
    vinculado_en   TIMESTAMPTZ,
    vinculado_por  CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT pre_ticket_codigo_uq UNIQUE (codigo),
    CONSTRAINT pre_ticket_servicio_check CHECK (servicio IN ('desayuno', 'almuerzo', 'cena')),
    CONSTRAINT pre_ticket_estado_check CHECK (estado IN ('pendiente', 'vinculado', 'cerrado', 'vencido', 'anulado'))
);

CREATE INDEX ix_pre_ticket_local_estado ON pre_ticket(local_id, estado, vence_en);
CREATE INDEX ix_pre_ticket_pensionista_servicio ON pre_ticket(pensionista_id, servicio, estado);

-- Bloqueo individual por intentos fallidos de PIN en el pensionista (US-019 CA6)
ALTER TABLE pensionista ADD COLUMN intentos_fallidos INTEGER NOT NULL DEFAULT 0;
ALTER TABLE pensionista ADD COLUMN bloqueado_hasta TIMESTAMPTZ;

-- Relacion 1-1 unica con cuenta_persona para cuando se vincule el pre-ticket (US-020)
ALTER TABLE cuenta_persona ADD COLUMN pre_ticket_id CHAR(26) REFERENCES pre_ticket(id) ON DELETE RESTRICT ON UPDATE RESTRICT;
CREATE UNIQUE INDEX ux_cuenta_persona_pre_ticket ON cuenta_persona(pre_ticket_id) WHERE pre_ticket_id IS NOT NULL;
