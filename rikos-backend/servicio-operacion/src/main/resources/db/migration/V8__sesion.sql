-- modelo-de-datos.md, seccion 3.10. Tabla inalterable (24.2): solo columnas
-- de creacion, nada se actualiza salvo finalizada_en al cerrar la sesion.
-- usuario_id admite nulo a proposito: un intento con un usuario inexistente
-- tambien se registra (22.2), y es justo el caso que mas interesa vigilar.

CREATE TABLE sesion (
    id              CHAR(26) PRIMARY KEY,
    usuario_id      CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    local_id        CHAR(26) REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    tipo            VARCHAR(20) NOT NULL CHECK (tipo IN ('contrasena', 'pin')),
    resultado       VARCHAR(20) NOT NULL CHECK (resultado IN ('exitoso', 'fallido', 'bloqueado')),
    dispositivo     VARCHAR(120),
    direccion_ip    VARCHAR(45),
    iniciada_en     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finalizada_en   TIMESTAMPTZ
);

CREATE INDEX ix_sesion_local_id ON sesion(local_id);

-- 23.4: auditoria de accesos por usuario.
CREATE INDEX ix_sesion_usuario_iniciada ON sesion(usuario_id, iniciada_en);

-- 23.4: deteccion de intentos fallidos, indice parcial.
CREATE INDEX ix_sesion_resultado_fallido ON sesion(resultado, iniciada_en)
    WHERE resultado <> 'exitoso';
