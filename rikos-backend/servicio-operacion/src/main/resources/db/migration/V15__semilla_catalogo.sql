-- Unidades base y el arbol de categorias tal como esta en 4.1.1 del modelo.
-- Sin productos ni proveedores: el catalogo arranca vacio a proposito.

INSERT INTO unidad_medida (id, codigo, nombre, tipo) VALUES
    ('01M39A0KG0000000000000001', 'kg',  'Kilogramo', 'masa'),
    ('01M39A0KG0000000000000002', 'g',   'Gramo',     'masa'),
    ('01M39A0KG0000000000000003', 'l',   'Litro',     'volumen'),
    ('01M39A0KG0000000000000004', 'ml',  'Mililitro', 'volumen'),
    ('01M39A0KG0000000000000005', 'und', 'Unidad',    'unidad');

-- Raices (nivel 1). naturaleza y componente_menu se fijan aqui; las hijas
-- los heredan y no deben contradecirlos (4.1, regla de herencia).
INSERT INTO categoria_producto (id, categoria_padre_id, nombre, naturaleza, componente_menu, nivel, orden) VALUES
    ('01M39A0RZ000000000000R001', NULL, 'Entradas',                  'componente_menu', 'entrada',    1, 1),
    ('01M39A0RZ000000000000R002', NULL, 'Segundos y guisos',         'componente_menu', 'segundo',    1, 2),
    ('01M39A0RZ000000000000R003', NULL, 'Guarniciones y menestras',  'componente_menu', 'guarnicion', 1, 3),
    ('01M39A0RZ000000000000R004', NULL, 'Refrescos',                 'componente_menu', 'refresco',   1, 4),
    ('01M39A0RZ000000000000R005', NULL, 'Postres',                   'componente_menu', 'postre',     1, 5),
    ('01M39A0RZ000000000000R006', NULL, 'Extras',                    'extra',           NULL,         1, 6),
    ('01M39A0RZ000000000000R007', NULL, 'Platos fijos',              'producto_fijo',   NULL,         1, 7),
    ('01M39A0RZ000000000000R008', NULL, 'Insumos',                   'insumo',          NULL,         1, 8),
    ('01M39A0RZ000000000000R009', NULL, 'Subrecetas',                'subreceta',       NULL,         1, 9);

-- Subcategorias (nivel 2), heredan naturaleza/componente_menu del padre.
INSERT INTO categoria_producto (id, categoria_padre_id, nombre, naturaleza, componente_menu, nivel, orden) VALUES
    -- Entradas
    ('01M39A0RZ000000000000S001', '01M39A0RZ000000000000R001', 'Sopas y caldos',  'componente_menu', 'entrada', 2, 1),
    ('01M39A0RZ000000000000S002', '01M39A0RZ000000000000R001', 'Ensaladas',       'componente_menu', 'entrada', 2, 2),
    ('01M39A0RZ000000000000S003', '01M39A0RZ000000000000R001', 'Causas y fríos',  'componente_menu', 'entrada', 2, 3),
    -- Segundos y guisos
    ('01M39A0RZ000000000000S004', '01M39A0RZ000000000000R002', 'Pollo',                  'componente_menu', 'segundo', 2, 1),
    ('01M39A0RZ000000000000S005', '01M39A0RZ000000000000R002', 'Res',                    'componente_menu', 'segundo', 2, 2),
    ('01M39A0RZ000000000000S006', '01M39A0RZ000000000000R002', 'Cerdo',                  'componente_menu', 'segundo', 2, 3),
    ('01M39A0RZ000000000000S007', '01M39A0RZ000000000000R002', 'Pescados y mariscos',    'componente_menu', 'segundo', 2, 4),
    ('01M39A0RZ000000000000S008', '01M39A0RZ000000000000R002', 'Sin carne',              'componente_menu', 'segundo', 2, 5),
    -- Guarniciones y menestras
    ('01M39A0RZ000000000000S009', '01M39A0RZ000000000000R003', 'Arroces',            'componente_menu', 'guarnicion', 2, 1),
    ('01M39A0RZ000000000000S010', '01M39A0RZ000000000000R003', 'Menestras',          'componente_menu', 'guarnicion', 2, 2),
    ('01M39A0RZ000000000000S011', '01M39A0RZ000000000000R003', 'Papas y tubérculos', 'componente_menu', 'guarnicion', 2, 3),
    -- Extras
    ('01M39A0RZ000000000000S012', '01M39A0RZ000000000000R006', 'Segundos especiales',     'extra', NULL, 2, 1),
    ('01M39A0RZ000000000000S013', '01M39A0RZ000000000000R006', 'Porciones adicionales',   'extra', NULL, 2, 2),
    -- Platos fijos
    ('01M39A0RZ000000000000S014', '01M39A0RZ000000000000R007', 'Caldos y sopas',        'producto_fijo', NULL, 2, 1),
    ('01M39A0RZ000000000000S015', '01M39A0RZ000000000000R007', 'Platos de fondo',       'producto_fijo', NULL, 2, 2),
    ('01M39A0RZ000000000000S016', '01M39A0RZ000000000000R007', 'Sándwiches y panes',    'producto_fijo', NULL, 2, 3),
    ('01M39A0RZ000000000000S017', '01M39A0RZ000000000000R007', 'Bebidas calientes',     'producto_fijo', NULL, 2, 4),
    ('01M39A0RZ000000000000S018', '01M39A0RZ000000000000R007', 'Jugos y refrescos',     'producto_fijo', NULL, 2, 5),
    -- Insumos
    ('01M39A0RZ000000000000S019', '01M39A0RZ000000000000R008', 'Carnes y aves',              'insumo', NULL, 2, 1),
    ('01M39A0RZ000000000000S020', '01M39A0RZ000000000000R008', 'Pescados',                   'insumo', NULL, 2, 2),
    ('01M39A0RZ000000000000S021', '01M39A0RZ000000000000R008', 'Verduras y frutas',          'insumo', NULL, 2, 3),
    ('01M39A0RZ000000000000S022', '01M39A0RZ000000000000R008', 'Abarrotes',                  'insumo', NULL, 2, 4),
    ('01M39A0RZ000000000000S023', '01M39A0RZ000000000000R008', 'Lácteos y huevos',           'insumo', NULL, 2, 5),
    ('01M39A0RZ000000000000S024', '01M39A0RZ000000000000R008', 'Condimentos y especias',     'insumo', NULL, 2, 6),
    ('01M39A0RZ000000000000S025', '01M39A0RZ000000000000R008', 'Descartables y táperes',     'insumo', NULL, 2, 7),
    -- Subrecetas
    ('01M39A0RZ000000000000S026', '01M39A0RZ000000000000R009', 'Fondos y bases',      'subreceta', NULL, 2, 1),
    ('01M39A0RZ000000000000S027', '01M39A0RZ000000000000R009', 'Salsas y aderezos',   'subreceta', NULL, 2, 2),
    ('01M39A0RZ000000000000S028', '01M39A0RZ000000000000R009', 'Guisos base',         'subreceta', NULL, 2, 3);
