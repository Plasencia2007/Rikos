-- modelo-de-datos.md, secciones 3.6, 3.7bis y 3.8, con los ON DELETE de la
-- tabla 22.2. Tablas de relacion pura: la fila desaparece cuando se quita
-- la asignacion, nunca cuando se borra el usuario (el usuario no se borra
-- nunca). La cascada CASCADE esta del lado que nunca se dispara en la
-- practica -- ver la nota de 22.2 -- y sirve para que quitar la asignacion
-- sea una sola operacion.

CREATE TABLE rol_permiso (
    rol_id      CHAR(26) NOT NULL REFERENCES rol(id) ON DELETE CASCADE ON UPDATE RESTRICT,
    permiso_id  CHAR(26) NOT NULL REFERENCES permiso(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por  CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    PRIMARY KEY (rol_id, permiso_id)
);

CREATE INDEX ix_rol_permiso_permiso_id ON rol_permiso(permiso_id);
CREATE INDEX ix_rol_permiso_creado_por ON rol_permiso(creado_por);

CREATE TABLE usuario_rol (
    usuario_id      CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE CASCADE ON UPDATE RESTRICT,
    rol_id          CHAR(26) NOT NULL REFERENCES rol(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    asignado_en     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    asignado_por    CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    PRIMARY KEY (usuario_id, rol_id)
);

CREATE INDEX ix_usuario_rol_rol_id ON usuario_rol(rol_id);
CREATE INDEX ix_usuario_rol_asignado_por ON usuario_rol(asignado_por);

CREATE TABLE usuario_local (
    usuario_id      CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE CASCADE ON UPDATE RESTRICT,
    local_id        CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    es_principal    BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por      CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    PRIMARY KEY (usuario_id, local_id)
);

CREATE INDEX ix_usuario_local_local_id ON usuario_local(local_id);
CREATE INDEX ix_usuario_local_creado_por ON usuario_local(creado_por);
