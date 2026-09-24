-- Modulo identidad (modelo-de-datos.md, seccion 3.1). Empresa es la entidad
-- propietaria: una sola fila en la practica, pero modelada como tabla porque
-- los datos fiscales cuelgan de ella y local tiene FK contra ella.

CREATE TABLE empresa (
    id                  CHAR(26) PRIMARY KEY,
    ruc                 VARCHAR(11) NOT NULL,
    razon_social        VARCHAR(200) NOT NULL,
    nombre_comercial    VARCHAR(120),
    domicilio_fiscal    VARCHAR(250) NOT NULL,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMPTZ
);

ALTER TABLE empresa ADD CONSTRAINT uq_empresa_ruc UNIQUE (ruc);
