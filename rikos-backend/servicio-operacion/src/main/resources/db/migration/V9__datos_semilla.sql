-- Datos semilla para poder entrar al sistema (Sprint 1, US-002).
-- IDs generados con Identificador.nuevo() (contratos), no escritos a mano.
--
-- Usuario admin: admin / cambiar123
-- MARCADO EXPLICITAMENTE COMO DE DESARROLLO. hash_contrasena es un BCrypt
-- real de "cambiar123"; se debe cambiar al primer uso. RUC y direccion son
-- placeholders (P-04 sigue pendiente de definicion en el modelo).

INSERT INTO empresa (id, ruc, razon_social, nombre_comercial, domicilio_fiscal, creado_en)
VALUES ('01M2QZ609M6G76Q60092ZVZHMH', '20123456789', 'RIKOS SOCIEDAD ANONIMA CERRADA',
        'Riko''s', 'Av. Placeholder 123, Lima (P-04 pendiente)', NOW());

INSERT INTO local (id, empresa_id, codigo, nombre, direccion, activo, version, creado_en)
VALUES ('01M2QZ60D35B90CW00EJA15VFC', '01M2QZ609M6G76Q60092ZVZHMH', 'sanisidro', 'San Isidro',
        'Direccion de prueba, San Isidro, Lima', TRUE, 0, NOW());

-- Los seis roles del negocio (01 S Actores). es_sistema = true: no se borran.
INSERT INTO rol (id, nombre, descripcion, es_sistema, activo) VALUES
    ('01M2QZ600G59KP9900YCPTA3QE', 'administrador central', 'Administra toda la organizacion, todos los locales', TRUE, TRUE),
    ('01M2QZ60PEWYF1V1003FRDBCGG', 'administrador de local', 'Administra un local especifico', TRUE, TRUE),
    ('01M2QZ60EPNAZHF200W9WQHKSH', 'cajero', 'Opera la caja del local', TRUE, TRUE),
    ('01M2QZ60ER3521K700BX7J7KP3', 'mozo', 'Toma pedidos en el POS', TRUE, TRUE),
    ('01M2QZ60T1V1NJ8J00BAXJ3Y8S', 'cocina', 'Ve y despacha comandas', TRUE, TRUE),
    ('01M2QZ60ZN8QVGKD00SDD9GQAB', 'motorizado', 'Entrega pedidos de delivery', TRUE, TRUE);

-- DESARROLLO: contrasena "cambiar123", debe cambiarse al primer uso.
INSERT INTO usuario (id, nombre_usuario, nombres, apellidos, hash_contrasena, activo, version, creado_en)
VALUES ('01M2QZ606763Q7XC00M8VD7WM1', 'admin', 'Administrador', 'Sistema',
        '$2a$10$LyBllcOe9iUPowgmI48G/.OSM/IUTGFAkPxbNW8JQogkUb0tImeJi', TRUE, 0, NOW());

INSERT INTO usuario_rol (usuario_id, rol_id, asignado_en, asignado_por)
VALUES ('01M2QZ606763Q7XC00M8VD7WM1', '01M2QZ600G59KP9900YCPTA3QE', NOW(), '01M2QZ606763Q7XC00M8VD7WM1');

INSERT INTO usuario_local (usuario_id, local_id, es_principal, creado_en, creado_por)
VALUES ('01M2QZ606763Q7XC00M8VD7WM1', '01M2QZ60D35B90CW00EJA15VFC', TRUE, NOW(), '01M2QZ606763Q7XC00M8VD7WM1');
