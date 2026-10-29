-- DATOS DE PRUEBA. Solo los carga SembradorDatosDePrueba con el perfil "demo";
-- nunca Flyway, nunca la nube. Todo es inventado: nombres, DNI, RUC, precios y costos.
--
-- Los datos del SISTEMA (empresa, local San Isidro, admin, roles, permisos,
-- categorias, unidades, motivos) los crean las migraciones; aqui solo se usan.
--
-- Idempotente: IDs fijos y ON CONFLICT DO NOTHING, se puede correr en cada arranque.
-- Los {{...}} los reemplaza el sembrador con hashes calculados al arrancar.
-- IDs: '01DEM0' + tipo (2 letras Crockford) + 18 digitos = 26 caracteres.

-- ---------------------------------------------------------------- personal
-- Contrasena de todos: demo123. PIN de mozo: mozo1 1111, mozo2 2222.
INSERT INTO usuario (id, nombre_usuario, nombres, apellidos, hash_contrasena, hash_pin, activo, version, creado_en, creado_por) VALUES
    ('01DEM0EM000000000000000001', 'mozo1',      'Carlos', 'Huaman Soto',    '{{HASH_CLAVE}}', '{{HASH_PIN_MOZO_1}}', TRUE, 0, NOW(), '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0EM000000000000000002', 'mozo2',      'Rosa',   'Quispe Llanos',  '{{HASH_CLAVE}}', '{{HASH_PIN_MOZO_2}}', TRUE, 0, NOW(), '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0EM000000000000000003', 'cajero1',    'Pedro',  'Ramos Vega',     '{{HASH_CLAVE}}', NULL,                  TRUE, 0, NOW(), '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0EM000000000000000004', 'cocina1',    'Lucia',  'Flores Paucar',  '{{HASH_CLAVE}}', NULL,                  TRUE, 0, NOW(), '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0EM000000000000000005', 'adminlocal', 'Maria',  'Torres Benites', '{{HASH_CLAVE}}', NULL,                  TRUE, 0, NOW(), '01M2QZ606763Q7XC00M8VD7WM1')
ON CONFLICT DO NOTHING;

INSERT INTO usuario_rol (usuario_id, rol_id, asignado_en, asignado_por) VALUES
    ('01DEM0EM000000000000000001', '01M2QZ60ER3521K700BX7J7KP3', NOW(), '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0EM000000000000000002', '01M2QZ60ER3521K700BX7J7KP3', NOW(), '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0EM000000000000000003', '01M2QZ60EPNAZHF200W9WQHKSH', NOW(), '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0EM000000000000000004', '01M2QZ60T1V1NJ8J00BAXJ3Y8S', NOW(), '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0EM000000000000000005', '01M2QZ60PEWYF1V1003FRDBCGG', NOW(), '01M2QZ606763Q7XC00M8VD7WM1'),
    -- mozo1 tambien es cajero: cobra y saca la pre-cuenta desde el pedido (adenda 2026-10-29).
    ('01DEM0EM000000000000000001', '01M2QZ60EPNAZHF200W9WQHKSH', NOW(), '01M2QZ606763Q7XC00M8VD7WM1')
ON CONFLICT DO NOTHING;

INSERT INTO usuario_local (usuario_id, local_id, es_principal, creado_en, creado_por)
SELECT u.id, '01M2QZ60D35B90CW00EJA15VFC', TRUE, NOW(), '01M2QZ606763Q7XC00M8VD7WM1'
FROM usuario u WHERE u.id LIKE '01DEM0EM%'
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- salones y mesas
INSERT INTO salon (id, local_id, nombre, descripcion, icono, color, creado_por) VALUES
    ('01DEM0SA000000000000000001', '01M2QZ60D35B90CW00EJA15VFC', 'Salón principal', 'Datos de prueba', 'cubiertos', 'verde', '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0SA000000000000000002', '01M2QZ60D35B90CW00EJA15VFC', 'Terraza',         'Datos de prueba', 'cubiertos', 'verde', '01M2QZ606763Q7XC00M8VD7WM1')
ON CONFLICT DO NOTHING;

INSERT INTO mesa (id, local_id, salon_id, numero, capacidad, forma, zona_exterior, pos_x, pos_y, ancho, alto, creado_por) VALUES
    ('01DEM0ME000000000000000001', '01M2QZ60D35B90CW00EJA15VFC', '01DEM0SA000000000000000001', '1',  4, 'cuadrada',    FALSE, 150, 120,  84, 84, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0ME000000000000000002', '01M2QZ60D35B90CW00EJA15VFC', '01DEM0SA000000000000000001', '2',  4, 'cuadrada',    FALSE, 390, 120,  84, 84, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0ME000000000000000003', '01M2QZ60D35B90CW00EJA15VFC', '01DEM0SA000000000000000001', '3',  4, 'cuadrada',    FALSE, 630, 120,  84, 84, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0ME000000000000000004', '01M2QZ60D35B90CW00EJA15VFC', '01DEM0SA000000000000000001', '4',  4, 'cuadrada',    FALSE, 870, 120,  84, 84, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0ME000000000000000005', '01M2QZ60D35B90CW00EJA15VFC', '01DEM0SA000000000000000001', '5',  4, 'cuadrada',    FALSE, 150, 360,  84, 84, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0ME000000000000000006', '01M2QZ60D35B90CW00EJA15VFC', '01DEM0SA000000000000000001', '6',  4, 'cuadrada',    FALSE, 390, 360,  84, 84, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0ME000000000000000007', '01M2QZ60D35B90CW00EJA15VFC', '01DEM0SA000000000000000001', '7',  6, 'rectangular', FALSE, 590, 360, 160, 84, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0ME000000000000000008', '01M2QZ60D35B90CW00EJA15VFC', '01DEM0SA000000000000000001', '8',  6, 'rectangular', FALSE, 850, 360, 160, 84, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0ME000000000000000009', '01M2QZ60D35B90CW00EJA15VFC', '01DEM0SA000000000000000002', 'T1', 2, 'redonda',     TRUE,   80,  80,  84, 84, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0ME000000000000000010', '01M2QZ60D35B90CW00EJA15VFC', '01DEM0SA000000000000000002', 'T2', 2, 'redonda',     TRUE,  280,  80,  84, 84, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0ME000000000000000011', '01M2QZ60D35B90CW00EJA15VFC', '01DEM0SA000000000000000002', 'T3', 2, 'redonda',     TRUE,  480,  80,  84, 84, '01M2QZ606763Q7XC00M8VD7WM1')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- horarios de servicio (los 7 dias)
INSERT INTO horario_servicio (id, local_id, servicio, dia_semana, hora_inicio, hora_fin, activo)
SELECT '01DEM0HS' || LPAD((d.n * 10 + s.n)::text, 18, '0'), '01M2QZ60D35B90CW00EJA15VFC', s.servicio, d.dia, s.inicio, s.fin, TRUE
FROM (VALUES (1, 'lunes'), (2, 'martes'), (3, 'miercoles'), (4, 'jueves'), (5, 'viernes'), (6, 'sabado'), (7, 'domingo')) AS d(n, dia)
CROSS JOIN (VALUES (1, 'desayuno', TIME '07:00', TIME '10:00'),
                   (2, 'almuerzo', TIME '12:00', TIME '16:00'),
                   (3, 'cena',     TIME '18:00', TIME '21:00')) AS s(n, servicio, inicio, fin)
WHERE NOT EXISTS (SELECT 1 FROM horario_servicio h
                  WHERE h.local_id = '01M2QZ60D35B90CW00EJA15VFC' AND h.servicio = s.servicio AND h.dia_semana = d.dia)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- precios del menu (solo si nadie los fijo)
UPDATE producto SET precio_venta = 1200 WHERE id = '01M39A0PM0000000000000N001' AND precio_venta IS NULL;
UPDATE producto SET precio_venta = 1600 WHERE id = '01M39A0PM0000000000000N002' AND precio_venta IS NULL;
UPDATE parametro_global SET valor = '100' WHERE clave = 'taper_precio' AND valor IS NULL;

-- ---------------------------------------------------------------- insumos (costo en soles por unidad base)
INSERT INTO producto (id, codigo, nombre, categoria_id, unidad_base_id, es_vendible, es_insumo, es_subreceta,
                      costo_ultima_compra, origen_costo, controla_stock, creado_por) VALUES
    ('01DEM0PR000000000000000001', 'DEMO-POLLO',    'Pollo entero',      '01M39A0RZ0000000000000S019', (SELECT id FROM unidad_medida WHERE codigo = 'kg'),  FALSE, TRUE, FALSE, 11.50, 'manual', TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000002', 'DEMO-RES',      'Lomo de res',       '01M39A0RZ0000000000000S019', (SELECT id FROM unidad_medida WHERE codigo = 'kg'),  FALSE, TRUE, FALSE, 32.00, 'manual', TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000003', 'DEMO-ARROZ',    'Arroz extra',       '01M39A0RZ0000000000000S022', (SELECT id FROM unidad_medida WHERE codigo = 'kg'),  FALSE, TRUE, FALSE,  4.20, 'manual', TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000004', 'DEMO-PAPA',     'Papa amarilla',     '01M39A0RZ0000000000000S021', (SELECT id FROM unidad_medida WHERE codigo = 'kg'),  FALSE, TRUE, FALSE,  3.50, 'manual', TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000005', 'DEMO-CEBOLLA',  'Cebolla roja',      '01M39A0RZ0000000000000S021', (SELECT id FROM unidad_medida WHERE codigo = 'kg'),  FALSE, TRUE, FALSE,  2.80, 'manual', TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000006', 'DEMO-AJI',      'Ají amarillo',      '01M39A0RZ0000000000000S024', (SELECT id FROM unidad_medida WHERE codigo = 'kg'),  FALSE, TRUE, FALSE,  8.00, 'manual', TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000007', 'DEMO-ACEITE',   'Aceite vegetal',    '01M39A0RZ0000000000000S022', (SELECT id FROM unidad_medida WHERE codigo = 'l'),   FALSE, TRUE, FALSE,  9.00, 'manual', TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000008', 'DEMO-LECHE',    'Leche evaporada',   '01M39A0RZ0000000000000S023', (SELECT id FROM unidad_medida WHERE codigo = 'l'),   FALSE, TRUE, FALSE,  7.50, 'manual', TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000009', 'DEMO-MAIZ',     'Maíz morado',       '01M39A0RZ0000000000000S021', (SELECT id FROM unidad_medida WHERE codigo = 'kg'),  FALSE, TRUE, FALSE,  6.00, 'manual', TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000010', 'DEMO-AZUCAR',   'Azúcar rubia',      '01M39A0RZ0000000000000S022', (SELECT id FROM unidad_medida WHERE codigo = 'kg'),  FALSE, TRUE, FALSE,  4.00, 'manual', TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000011', 'DEMO-FIDEO',    'Tallarín',          '01M39A0RZ0000000000000S022', (SELECT id FROM unidad_medida WHERE codigo = 'kg'),  FALSE, TRUE, FALSE,  5.50, 'manual', TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000012', 'DEMO-ESPINACA', 'Espinaca',          '01M39A0RZ0000000000000S021', (SELECT id FROM unidad_medida WHERE codigo = 'kg'),  FALSE, TRUE, FALSE,  4.50, 'manual', TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000013', 'DEMO-TAPER',    'Táper descartable', '01M39A0RZ0000000000000S025', (SELECT id FROM unidad_medida WHERE codigo = 'und'), FALSE, TRUE, FALSE,  0.50, 'manual', TRUE, '01M2QZ606763Q7XC00M8VD7WM1')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- componentes del menu (sin precio: se cobran con el menu)
INSERT INTO producto (id, codigo, nombre, categoria_id, unidad_base_id, es_vendible, es_insumo, es_subreceta, controla_stock, creado_por) VALUES
    ('01DEM0PR000000000000000021', 'DEMO-SOPA',        'Sopa de casa',     '01M39A0RZ0000000000000S001', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE, FALSE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000022', 'DEMO-CAUSA',       'Causa limeña',     '01M39A0RZ0000000000000S003', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE, FALSE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000023', 'DEMO-ENSALADA',    'Ensalada fresca',  '01M39A0RZ0000000000000S002', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE, FALSE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000024', 'DEMO-AJIGALLINA',  'Ají de gallina',   '01M39A0RZ0000000000000S004', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE, FALSE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000025', 'DEMO-ARROZPOLLO',  'Arroz con pollo',  '01M39A0RZ0000000000000S004', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE, FALSE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000026', 'DEMO-TALLARIN',    'Tallarín verde',   '01M39A0RZ0000000000000S008', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE, FALSE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000027', 'DEMO-LOMO',        'Lomo saltado',     '01M39A0RZ0000000000000S005', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE, FALSE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000028', 'DEMO-CHICHA',      'Chicha morada',    '01M39A0RZ0000000000000R004', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE, FALSE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000029', 'DEMO-MAZAMORRA',   'Mazamorra morada', '01M39A0RZ0000000000000R005', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE, FALSE, '01M2QZ606763Q7XC00M8VD7WM1')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- extras y platos a la carta (precio en centimos)
INSERT INTO producto (id, codigo, nombre, categoria_id, unidad_base_id, es_vendible, es_insumo, es_subreceta, precio_venta, controla_stock, creado_por) VALUES
    ('01DEM0PR000000000000000031', 'DEMO-PAPAS',     'Porción de papas fritas', '01M39A0RZ0000000000000S013', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE,  600, FALSE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000032', 'DEMO-HUEVO',     'Huevo frito',             '01M39A0RZ0000000000000S013', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE,  200, FALSE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000041', 'DEMO-CALDO',     'Caldo de gallina',        '01M39A0RZ0000000000000S014', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE, 1500, FALSE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000042', 'DEMO-LOMOCARTA', 'Lomo saltado a la carta', '01M39A0RZ0000000000000S015', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE, 2800, FALSE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000043', 'DEMO-CAFE',      'Café pasado',             '01M39A0RZ0000000000000S017', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE,  400, FALSE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PR000000000000000044', 'DEMO-PAPAYA',    'Jugo de papaya',          '01M39A0RZ0000000000000S018', (SELECT id FROM unidad_medida WHERE codigo = 'und'), TRUE, FALSE, FALSE,  700, FALSE, '01M2QZ606763Q7XC00M8VD7WM1')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- recetas (cantidades en la unidad base del insumo)
INSERT INTO receta (id, producto_id, rendimiento, porciones, activo, creado_por) VALUES
    ('01DEM0RE000000000000000001', '01DEM0PR000000000000000024', 10, 10, TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0RE000000000000000002', '01DEM0PR000000000000000025', 10, 10, TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0RE000000000000000003', '01DEM0PR000000000000000027', 10, 10, TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0RE000000000000000004', '01DEM0PR000000000000000026', 10, 10, TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0RE000000000000000005', '01DEM0PR000000000000000028', 20, 20, TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0RE000000000000000006', '01DEM0PR000000000000000042',  1,  1, TRUE, '01M2QZ606763Q7XC00M8VD7WM1')
ON CONFLICT DO NOTHING;

INSERT INTO ingrediente_receta (id, receta_id, producto_id, cantidad, unidad_id, orden)
SELECT g.id, g.receta_id, g.producto_id, g.cantidad, p.unidad_base_id, g.orden
FROM (VALUES
    -- aji de gallina
    ('01DEM0GR000000000000000101', '01DEM0RE000000000000000001', '01DEM0PR000000000000000001', 1.50, 1),
    ('01DEM0GR000000000000000102', '01DEM0RE000000000000000001', '01DEM0PR000000000000000006', 0.30, 2),
    ('01DEM0GR000000000000000103', '01DEM0RE000000000000000001', '01DEM0PR000000000000000008', 0.80, 3),
    ('01DEM0GR000000000000000104', '01DEM0RE000000000000000001', '01DEM0PR000000000000000004', 1.00, 4),
    -- arroz con pollo
    ('01DEM0GR000000000000000201', '01DEM0RE000000000000000002', '01DEM0PR000000000000000001', 1.50, 1),
    ('01DEM0GR000000000000000202', '01DEM0RE000000000000000002', '01DEM0PR000000000000000003', 1.20, 2),
    ('01DEM0GR000000000000000203', '01DEM0RE000000000000000002', '01DEM0PR000000000000000005', 0.30, 3),
    -- lomo saltado (menu extra)
    ('01DEM0GR000000000000000301', '01DEM0RE000000000000000003', '01DEM0PR000000000000000002', 1.80, 1),
    ('01DEM0GR000000000000000302', '01DEM0RE000000000000000003', '01DEM0PR000000000000000005', 1.00, 2),
    ('01DEM0GR000000000000000303', '01DEM0RE000000000000000003', '01DEM0PR000000000000000004', 1.50, 3),
    ('01DEM0GR000000000000000304', '01DEM0RE000000000000000003', '01DEM0PR000000000000000007', 0.30, 4),
    -- tallarin verde
    ('01DEM0GR000000000000000401', '01DEM0RE000000000000000004', '01DEM0PR000000000000000011', 1.20, 1),
    ('01DEM0GR000000000000000402', '01DEM0RE000000000000000004', '01DEM0PR000000000000000012', 0.80, 2),
    ('01DEM0GR000000000000000403', '01DEM0RE000000000000000004', '01DEM0PR000000000000000008', 0.40, 3),
    -- chicha morada
    ('01DEM0GR000000000000000501', '01DEM0RE000000000000000005', '01DEM0PR000000000000000009', 1.00, 1),
    ('01DEM0GR000000000000000502', '01DEM0RE000000000000000005', '01DEM0PR000000000000000010', 0.80, 2),
    -- lomo saltado a la carta (una porcion)
    ('01DEM0GR000000000000000601', '01DEM0RE000000000000000006', '01DEM0PR000000000000000002', 0.20, 1),
    ('01DEM0GR000000000000000602', '01DEM0RE000000000000000006', '01DEM0PR000000000000000005', 0.10, 2),
    ('01DEM0GR000000000000000603', '01DEM0RE000000000000000006', '01DEM0PR000000000000000004', 0.20, 3),
    ('01DEM0GR000000000000000604', '01DEM0RE000000000000000006', '01DEM0PR000000000000000007', 0.03, 4)
) AS g(id, receta_id, producto_id, cantidad, orden)
JOIN producto p ON p.id = g.producto_id
ON CONFLICT DO NOTHING;

-- Costo de las recetas de prueba: cantidad (ya en unidad base) x costo_ultima_compra,
-- la misma regla que CalculadoraCostoReceta. Solo si todavia no tienen costo.
UPDATE receta r
SET costo_total = c.total, costo_por_porcion = ROUND(c.total / r.porciones, 6)
FROM (SELECT i.receta_id, ROUND(SUM(i.cantidad * p.costo_ultima_compra), 6) AS total
      FROM ingrediente_receta i JOIN producto p ON p.id = i.producto_id
      WHERE i.receta_id LIKE '01DEM0RE%'
      GROUP BY i.receta_id) c
WHERE r.id = c.receta_id AND r.costo_total IS NULL;

-- ---------------------------------------------------------------- stock inicial (una entrada de ajuste por insumo)
-- Solo si el insumo todavia no tiene existencia en San Isidro: asi el Kardex
-- siempre cuadra con el stock. La espinaca queda bajo el minimo a proposito.
INSERT INTO movimiento_inventario (id, local_id, producto_id, origen, signo, cantidad, unidad_id, cantidad_base,
                                   costo_unitario, stock_anterior, stock_nuevo, observacion, ocurrido_en, registrado_por)
SELECT s.mov_id, '01M2QZ60D35B90CW00EJA15VFC', s.producto_id, 'ajuste', 'entrada', s.stock, p.unidad_base_id, s.stock,
       p.costo_ultima_compra, 0, s.stock, 'Stock inicial de datos de prueba', NOW(), '01M2QZ606763Q7XC00M8VD7WM1'
FROM (VALUES
    ('01DEM0MV000000000000000001', '01DEM0PR000000000000000001',  20.0,  5.0),
    ('01DEM0MV000000000000000002', '01DEM0PR000000000000000002',  10.0,  3.0),
    ('01DEM0MV000000000000000003', '01DEM0PR000000000000000003',  50.0, 10.0),
    ('01DEM0MV000000000000000004', '01DEM0PR000000000000000004',  30.0,  8.0),
    ('01DEM0MV000000000000000005', '01DEM0PR000000000000000005',  15.0,  4.0),
    ('01DEM0MV000000000000000006', '01DEM0PR000000000000000006',   4.0,  1.0),
    ('01DEM0MV000000000000000007', '01DEM0PR000000000000000007',  10.0,  2.0),
    ('01DEM0MV000000000000000008', '01DEM0PR000000000000000008',  12.0,  3.0),
    ('01DEM0MV000000000000000009', '01DEM0PR000000000000000009',   6.0,  2.0),
    ('01DEM0MV000000000000000010', '01DEM0PR000000000000000010',  10.0,  3.0),
    ('01DEM0MV000000000000000011', '01DEM0PR000000000000000011',   8.0,  2.0),
    ('01DEM0MV000000000000000012', '01DEM0PR000000000000000012',   0.5,  1.0),
    ('01DEM0MV000000000000000013', '01DEM0PR000000000000000013', 200.0, 50.0)
) AS s(mov_id, producto_id, stock, minimo)
JOIN producto p ON p.id = s.producto_id
WHERE NOT EXISTS (SELECT 1 FROM existencia e
                  WHERE e.local_id = '01M2QZ60D35B90CW00EJA15VFC' AND e.producto_id = s.producto_id)
ON CONFLICT DO NOTHING;

INSERT INTO existencia (local_id, producto_id, stock_actual, stock_minimo, ultimo_movimiento_id, recalculado_en)
SELECT m.local_id, m.producto_id, m.stock_nuevo, s.minimo, m.id, NOW()
FROM (VALUES
    ('01DEM0MV000000000000000001',  5.0), ('01DEM0MV000000000000000002',  3.0), ('01DEM0MV000000000000000003', 10.0),
    ('01DEM0MV000000000000000004',  8.0), ('01DEM0MV000000000000000005',  4.0), ('01DEM0MV000000000000000006',  1.0),
    ('01DEM0MV000000000000000007',  2.0), ('01DEM0MV000000000000000008',  3.0), ('01DEM0MV000000000000000009',  2.0),
    ('01DEM0MV000000000000000010',  3.0), ('01DEM0MV000000000000000011',  2.0), ('01DEM0MV000000000000000012',  1.0),
    ('01DEM0MV000000000000000013', 50.0)
) AS s(mov_id, minimo)
JOIN movimiento_inventario m ON m.id = s.mov_id
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- proveedores
INSERT INTO proveedor (id, ruc, razon_social, nombre_comercial, telefono, contacto, activo, creado_por) VALUES
    ('01DEM0PV000000000000000001', '20600000001', 'Avícola Los Andes S.A.C.',               'Avícola Los Andes', '987654321', 'Jorge Paz',  TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PV000000000000000002', '20600000002', 'Distribuidora Mercado Central E.I.R.L.', 'Mercado Central',   '912345678', 'Ana Ccori',  TRUE, '01M2QZ606763Q7XC00M8VD7WM1')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- planes tarifarios (centimos)
INSERT INTO plan_tarifario (id, nombre, descripcion, activo, creado_por) VALUES
    ('01DEM0PL000000000000000001', 'Plan estudiante (prueba)',  'Datos de prueba', TRUE, '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PL000000000000000002', 'Plan trabajador (prueba)',  'Datos de prueba', TRUE, '01M2QZ606763Q7XC00M8VD7WM1')
ON CONFLICT DO NOTHING;

INSERT INTO plan_tarifario_version (id, plan_id, precio_desayuno, precio_almuerzo, precio_cena, vigente_desde, creado_por) VALUES
    ('01DEM0PT000000000000000001', '01DEM0PL000000000000000001', 600,  1000,  900, DATE '2026-10-01', '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PT000000000000000002', '01DEM0PL000000000000000002', 700,  1200, 1000, DATE '2026-10-01', '01M2QZ606763Q7XC00M8VD7WM1')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------- pensionistas (PIN del kiosco: 111111, 222222, 333333)
INSERT INTO pensionista (id, dni, nombres, apellidos, telefono, institucion, carrera, ciclo, plan_tarifario_id,
                         come_desayuno, come_almuerzo, come_cena, frecuencia_abono, limite_credito,
                         hash_pin, huella_pin, activo, version, creado_en, creado_por) VALUES
    ('01DEM0PE000000000000000001', '70000001', 'Ana Lucia', 'Mendoza Rios',  '900000001', 'Universidad de prueba', 'Ingeniería', 'V',   '01DEM0PL000000000000000001',
        FALSE, TRUE, TRUE,  'mensual',   30000, '{{HASH_PIN_PENS_1}}', '{{HUELLA_PIN_PENS_1}}', TRUE, 0, NOW(), '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PE000000000000000002', '70000002', 'Jorge',     'Salazar Cueva', '900000002', 'Universidad de prueba', 'Derecho',    'III', '01DEM0PL000000000000000001',
        TRUE,  TRUE, FALSE, 'quincenal', 30000, '{{HASH_PIN_PENS_2}}', '{{HUELLA_PIN_PENS_2}}', TRUE, 0, NOW(), '01M2QZ606763Q7XC00M8VD7WM1'),
    ('01DEM0PE000000000000000003', '70000003', 'Valeria',   'Paredes Lima',  '900000003', NULL,                    NULL,         NULL,  '01DEM0PL000000000000000002',
        FALSE, TRUE, FALSE, 'semanal',   20000, '{{HASH_PIN_PENS_3}}', '{{HUELLA_PIN_PENS_3}}', TRUE, 0, NOW(), '01M2QZ606763Q7XC00M8VD7WM1')
ON CONFLICT DO NOTHING;

INSERT INTO responsable (id, pensionista_id, nombre_completo, dni, telefono, correo, parentesco, direccion)
SELECT r.id, r.pensionista_id, r.nombre, r.dni, r.telefono, r.correo, r.parentesco, r.direccion
FROM (VALUES
    ('01DEM0RP000000000000000001', '01DEM0PE000000000000000001', 'Rosa Rios Campos',     '40000001', '980000001', 'responsable1@ejemplo.com', 'Madre',  'Calle de prueba 101, Lima'),
    ('01DEM0RP000000000000000002', '01DEM0PE000000000000000002', 'Luis Salazar Peña',    '40000002', '980000002', 'responsable2@ejemplo.com', 'Padre',  'Calle de prueba 202, Lima'),
    ('01DEM0RP000000000000000003', '01DEM0PE000000000000000003', 'Valeria Paredes Lima', '70000003', '900000003', 'responsable3@ejemplo.com', 'Ella misma', 'Calle de prueba 303, Lima')
) AS r(id, pensionista_id, nombre, dni, telefono, correo, parentesco, direccion)
WHERE EXISTS (SELECT 1 FROM pensionista p WHERE p.id = r.pensionista_id)
ON CONFLICT DO NOTHING;

INSERT INTO cuenta_pensionista (id, pensionista_id, abierta_en, estado)
SELECT c.id, c.pensionista_id, NOW(), 'abierta'
FROM (VALUES
    ('01DEM0CT000000000000000001', '01DEM0PE000000000000000001'),
    ('01DEM0CT000000000000000002', '01DEM0PE000000000000000002'),
    ('01DEM0CT000000000000000003', '01DEM0PE000000000000000003')
) AS c(id, pensionista_id)
WHERE EXISTS (SELECT 1 FROM pensionista p WHERE p.id = c.pensionista_id)
ON CONFLICT DO NOTHING;
