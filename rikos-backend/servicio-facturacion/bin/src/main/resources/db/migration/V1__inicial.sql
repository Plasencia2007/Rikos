-- Migracion inicial de servicio-facturacion.
--
-- Este servicio tiene su propia base de datos: servicio-operacion
-- no consulta estas tablas, se comunican por eventos (DT-017).
--
-- Sobre la ausencia de clave de idempotencia en el proveedor (H-02):
-- el envio se serializa por serie y, ante una respuesta perdida,
-- se reconcilia contra el listado del dia antes de reintentar (DT-009).

CREATE TABLE IF NOT EXISTS comprobante (
    id                  VARCHAR(26) PRIMARY KEY,
    venta_id            VARCHAR(26)  NOT NULL,
    local_id            VARCHAR(26)  NOT NULL,
    tipo                VARCHAR(20)  NOT NULL,
    estado_fiscal       VARCHAR(20)  NOT NULL,
    estado_entrega      VARCHAR(20)  NOT NULL DEFAULT 'NO_APLICA',
    serie               VARCHAR(10),
    numero              VARCHAR(20),
    id_externo          VARCHAR(120),
    correo_enviado_en   TIMESTAMPTZ,
    creado_en           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Una venta no puede tener dos comprobantes: es la primera defensa
-- contra el duplicado descrito en H-02.
CREATE UNIQUE INDEX IF NOT EXISTS ux_comprobante_venta ON comprobante (venta_id);
