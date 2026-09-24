-- modelo-de-datos.md, secciones 3.4 y 3.5. Catalogos casi estaticos: sin
-- columnas de auditoria, ni en la seccion 3 ni en el grupo de 24.2.
-- rol_permiso (la relacion N-N) se crea despues, junto con usuario_rol y
-- usuario_local, cuando ya existen todas las tablas que referencia.

CREATE TABLE rol (
    id          CHAR(26) PRIMARY KEY,
    nombre      VARCHAR(60) NOT NULL,
    descripcion VARCHAR(200),
    es_sistema  BOOLEAN NOT NULL DEFAULT FALSE,
    activo      BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE rol ADD CONSTRAINT uq_rol_nombre UNIQUE (nombre);

CREATE TABLE permiso (
    id          CHAR(26) PRIMARY KEY,
    modulo      VARCHAR(40) NOT NULL,
    accion      VARCHAR(30) NOT NULL
        CHECK (accion IN ('ver', 'crear', 'modificar', 'desactivar', 'aprobar',
                           'anular', 'descontar', 'reabrir', 'ver_costos',
                           'exportar', 'administrar_permisos')),
    codigo      VARCHAR(80) NOT NULL,
    descripcion VARCHAR(200)
);

ALTER TABLE permiso ADD CONSTRAINT uq_permiso_codigo UNIQUE (codigo);
