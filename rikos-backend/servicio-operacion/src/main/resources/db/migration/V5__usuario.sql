-- modelo-de-datos.md, seccion 3.7. Dato maestro (24.2): traza completa +
-- version para bloqueo optimista. creado_por/actualizado_por son auto-
-- referencia (nulo en la fila creada por el proceso de semilla).

CREATE TABLE usuario (
    id                  CHAR(26) PRIMARY KEY,
    nombre_usuario      VARCHAR(60) NOT NULL,
    nombres             VARCHAR(120) NOT NULL,
    apellidos           VARCHAR(120) NOT NULL,
    dni                 VARCHAR(8),
    correo              VARCHAR(160),
    telefono            VARCHAR(20),
    hash_contrasena     VARCHAR(255) NOT NULL,
    hash_pin            VARCHAR(255),
    intentos_fallidos   INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta      TIMESTAMPTZ,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    version             INTEGER NOT NULL DEFAULT 0,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por          CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    actualizado_en      TIMESTAMPTZ,
    actualizado_por     CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

ALTER TABLE usuario ADD CONSTRAINT uq_usuario_nombre_usuario UNIQUE (nombre_usuario);

CREATE INDEX ix_usuario_creado_por ON usuario(creado_por);
CREATE INDEX ix_usuario_actualizado_por ON usuario(actualizado_por);
