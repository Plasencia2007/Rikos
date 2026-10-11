-- US-014: menu normal y menu extra como productos del sistema.
-- Decisiones en docs/producto/plan/sprint-04-costos-y-menu.md (entrega 2).

-- Sexta naturaleza de categoria. Postgres no deja alterar un CHECK en su
-- lugar: se borra y se recrea con la lista ampliada (mismo patron que V18).
ALTER TABLE categoria_producto DROP CONSTRAINT categoria_producto_naturaleza_check;
ALTER TABLE categoria_producto ADD CONSTRAINT categoria_producto_naturaleza_check
    CHECK (naturaleza IN ('componente_menu', 'producto_fijo', 'extra', 'insumo', 'subreceta', 'menu'));

-- Marca de los dos productos del menu. Nulo en todos los demas. El indice
-- unico parcial garantiza exactamente uno de cada tipo -- el POS (sprint 6)
-- y los planes (sprint 5) los buscan por aqui, no por nombre ni por id.
ALTER TABLE producto ADD COLUMN tipo_menu VARCHAR(10) CHECK (tipo_menu IN ('normal', 'extra'));
CREATE UNIQUE INDEX ux_producto_tipo_menu ON producto(tipo_menu) WHERE tipo_menu IS NOT NULL;

-- Una sola categoria raiz, sin hijas (es su propia hoja). Id de 26
-- caracteres: V16 existe porque V15 sembro ids de 25.
INSERT INTO categoria_producto (id, categoria_padre_id, nombre, naturaleza, componente_menu, nivel, orden) VALUES
    ('01M39A0RZ0000000000000R010', NULL, 'Menú del día', 'menu', NULL, 1, 0);

-- Sin precio a proposito (RF-002): el dueno no dio los precios definitivos.
-- Se venden por unidad, no controlan stock (lo que se descuenta son las
-- recetas de lo que el cliente eligio, sprint 6).
INSERT INTO producto (id, codigo, nombre, categoria_id, unidad_base_id, es_vendible, es_insumo, es_subreceta,
                      precio_venta, controla_stock, tipo_menu) VALUES
    ('01M39A0PM0000000000000N001', 'MENU-NORMAL', 'Menú normal', '01M39A0RZ0000000000000R010',
        (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE, NULL, FALSE, 'normal'),
    ('01M39A0PM0000000000000N002', 'MENU-EXTRA', 'Menú extra', '01M39A0RZ0000000000000R010',
        (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE, NULL, FALSE, 'extra');
