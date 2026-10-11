-- modelo-de-datos.md, seccion 3.7 ter. DT-039: corrige el bloqueo del PIN
-- del sprint 1 (estaba en memoria, por local) para que sea persistido y por
-- dispositivo -- un mozo que olvida su PIN no debe dejar inservible la
-- terminal de al lado, y un reinicio del nodo (cada corte de luz) no debe
-- borrar el contador.

CREATE TABLE bloqueo_pin (
    id                  CHAR(26) PRIMARY KEY,
    local_id            CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    dispositivo         VARCHAR(120) NOT NULL,
    intentos_fallidos   INTEGER NOT NULL DEFAULT 0,
    bloqueado_hasta     TIMESTAMPTZ,
    actualizado_en      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (local_id, dispositivo)
);
