-- modelo-de-datos.md, secciones 3.9 (usuario_permiso_excepcion), 3.11
-- (motivo) y 13.1 (auditoria). Adelanta US-004 y US-006 del sprint 3.
--
-- rol.color: distintivo visual de la pantalla de Roles -- no estaba en el
-- modelo original porque es puramente de presentacion, igual que
-- serie_boleta/serie_factura en local (3.2) no tienen relacion con datos
-- de negocio.

ALTER TABLE rol ADD COLUMN color VARCHAR(7);

-- 3.9: relacion pura usuario<->permiso, igual criterio de ON DELETE que
-- usuario_rol (22.2) -- la fila desaparece cuando se quita la excepcion,
-- nunca porque se borre el usuario (nunca se borra, 2.5) o el permiso.
CREATE TABLE usuario_permiso_excepcion (
    usuario_id  CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE CASCADE ON UPDATE RESTRICT,
    permiso_id  CHAR(26) NOT NULL REFERENCES permiso(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    otorgado    BOOLEAN NOT NULL,
    motivo      VARCHAR(200),
    creado_en   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por  CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    PRIMARY KEY (usuario_id, permiso_id)
);

CREATE INDEX ix_usuario_permiso_excepcion_permiso_id ON usuario_permiso_excepcion(permiso_id);
CREATE INDEX ix_usuario_permiso_excepcion_creado_por ON usuario_permiso_excepcion(creado_por);

-- 3.11: una sola tabla con discriminador para las seis listas de motivos
-- (evita seis pantallas de administracion identicas). Los valores los
-- carga el propietario (07 § Pendientes); esta migracion solo crea la
-- tabla, sin datos semilla.
CREATE TABLE motivo (
    id                     CHAR(26) PRIMARY KEY,
    tipo                   VARCHAR(30) NOT NULL
        CHECK (tipo IN ('ajuste_inventario', 'anulacion', 'descuento', 'rechazo_abono',
                         'sobrante_produccion', 'reapertura')),
    descripcion            VARCHAR(120) NOT NULL,
    requiere_observacion   BOOLEAN NOT NULL,
    activo                 BOOLEAN NOT NULL DEFAULT TRUE
);

-- Unico por (tipo, descripcion) entre los activos -- indice parcial, no
-- una UNIQUE plana, por el mismo motivo que horario_servicio (V11): un
-- motivo desactivado no debe bloquear reusar la misma descripcion despues.
CREATE UNIQUE INDEX ux_motivo_tipo_descripcion_activo ON motivo(tipo, descripcion) WHERE activo;

-- 13.1: tabla de auditoria, nunca se modifica ni se borra. entidad_id
-- identifica la fila afectada; para las relaciones N-N sin id propio
-- (rol_permiso, usuario_rol, usuario_permiso_excepcion) se usa el id de
-- la parte "duena" del cambio (el rol o el usuario), documentado en el
-- codigo que la escribe.
CREATE TABLE auditoria (
    id              CHAR(26) PRIMARY KEY,
    local_id        CHAR(26) REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    usuario_id      CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    evento          VARCHAR(30) NOT NULL
        CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                           'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                           'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan')),
    entidad         VARCHAR(60) NOT NULL,
    entidad_id      CHAR(26) NOT NULL,
    valor_anterior  JSONB,
    valor_nuevo     JSONB,
    motivo_id       CHAR(26) REFERENCES motivo(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    observacion     VARCHAR(500),
    ocurrido_en     TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX ix_auditoria_usuario_id ON auditoria(usuario_id);
CREATE INDEX ix_auditoria_entidad ON auditoria(entidad, entidad_id);
CREATE INDEX ix_auditoria_local_id ON auditoria(local_id);
