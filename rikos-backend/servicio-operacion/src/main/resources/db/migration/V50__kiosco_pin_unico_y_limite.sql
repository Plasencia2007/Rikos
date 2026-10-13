-- Los PIN antiguos no se pueden derivar de bcrypt. Se habilitan mediante regeneracion administrativa.
ALTER TABLE pensionista ADD COLUMN huella_pin VARCHAR(64);
CREATE UNIQUE INDEX uq_pensionista_huella_pin ON pensionista(huella_pin) WHERE huella_pin IS NOT NULL;

-- Limite de solicitudes persistente, sin conservar direcciones de red en claro.
CREATE TABLE kiosco_intentos (
    origen_hash VARCHAR(64) PRIMARY KEY,
    ventana TIMESTAMPTZ NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0)
);
