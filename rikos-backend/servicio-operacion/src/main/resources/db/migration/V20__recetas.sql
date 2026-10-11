-- US-010 (bloque 3 del modelo de datos, §5): recetas y subrecetas
-- valorizadas. Central, sin local_id.
--
-- Diferencias con el modelo original, decididas en el sprint 4 (ver
-- docs/producto/plan/sprint-04-costos-y-menu.md):
--   - sin `nombre`: la receta se llama como su producto.
--   - sin `unidad_rendimiento_id`: el rendimiento va siempre en la unidad
--     base del producto, para que una subreceta se costee por unidad base
--     igual que un insumo.

CREATE TABLE receta (
    id                  CHAR(26) PRIMARY KEY,
    producto_id         CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    rendimiento         NUMERIC(14, 6) NOT NULL CHECK (rendimiento > 0),
    porciones           INTEGER NOT NULL CHECK (porciones > 0),
    -- Derivados: se recalculan en cascada cuando cambia el costo de un
    -- insumo del arbol. Nulos si algun ingrediente no tiene costo todavia.
    costo_total         NUMERIC(14, 6),
    costo_por_porcion   NUMERIC(14, 6),
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    version             INTEGER NOT NULL DEFAULT 0,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por          CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    actualizado_en      TIMESTAMPTZ,
    actualizado_por     CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- §5.1: una receta activa por producto. En la base, no solo en la
-- aplicacion -- cierra el hueco de dos altas simultaneas.
CREATE UNIQUE INDEX ux_receta_producto_activa ON receta(producto_id) WHERE activo;

CREATE TABLE ingrediente_receta (
    id            CHAR(26) PRIMARY KEY,
    receta_id     CHAR(26) NOT NULL REFERENCES receta(id) ON DELETE CASCADE ON UPDATE CASCADE,
    producto_id   CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    cantidad      NUMERIC(14, 6) NOT NULL CHECK (cantidad > 0),
    unidad_id     CHAR(26) NOT NULL REFERENCES unidad_medida(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    orden         INTEGER NOT NULL
);

CREATE INDEX ix_ingrediente_receta_receta_id ON ingrediente_receta(receta_id);
-- El recalculo en cascada pregunta "que recetas usan este producto".
CREATE INDEX ix_ingrediente_receta_producto_id ON ingrediente_receta(producto_id);
ALTER TABLE ingrediente_receta ADD CONSTRAINT uq_ingrediente_receta_producto
    UNIQUE (receta_id, producto_id);
