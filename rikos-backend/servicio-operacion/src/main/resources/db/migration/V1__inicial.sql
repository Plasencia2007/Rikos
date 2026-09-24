-- Migracion inicial de servicio-operacion.
--
-- Recordatorio de las decisiones que condicionan este esquema:
--   * Los identificadores se generan en el origen (ULID), nunca autoincrementales.
--   * Todo registro transaccional lleva su local_id.
--   * La cuenta del pensionista y el inventario son libros de movimientos,
--     no campos de saldo que se actualizan.
--   * La venta guarda snapshot inmutable de precio, costo y receta al cerrarse.

CREATE TABLE IF NOT EXISTS local (
    id          VARCHAR(26) PRIMARY KEY,
    nombre      VARCHAR(120) NOT NULL,
    activo      BOOLEAN      NOT NULL DEFAULT TRUE,
    creado_en   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
