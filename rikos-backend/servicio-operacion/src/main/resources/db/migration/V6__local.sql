-- modelo-de-datos.md, seccion 3.2. Reemplaza el "local" placeholder de
-- V1__inicial.sql (4 columnas, creado solo para probar el esqueleto del
-- Sprint 0; nunca tuvo filas reales). V1 no se edita -- esta migracion
-- dropea esa tabla provisional y crea la version real definida por el
-- modelo, con FK a empresa y traza completa (dato maestro, 24.2).

DROP TABLE IF EXISTS local;

CREATE TABLE local (
    id                  CHAR(26) PRIMARY KEY,
    empresa_id          CHAR(26) NOT NULL REFERENCES empresa(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    codigo              VARCHAR(20) NOT NULL,
    nombre              VARCHAR(120) NOT NULL,
    direccion           VARCHAR(250) NOT NULL,
    telefono            VARCHAR(20),
    serie_boleta        VARCHAR(4),
    serie_factura       VARCHAR(4),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    version             INTEGER NOT NULL DEFAULT 0,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por          CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    actualizado_en      TIMESTAMPTZ,
    actualizado_por     CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

ALTER TABLE local ADD CONSTRAINT uq_local_codigo UNIQUE (codigo);

CREATE INDEX ix_local_empresa_id ON local(empresa_id);
CREATE INDEX ix_local_creado_por ON local(creado_por);
CREATE INDEX ix_local_actualizado_por ON local(actualizado_por);
