-- Salones y mesas: un salon agrupa las mesas de un local (terraza, salon principal, barra...) y guarda el
-- tamano y el color de su plano; cada mesa lleva su forma, sus atributos (VIP, zona exterior, zona de
-- fumadores) y su lugar en el plano. Diseno en el canvas ("Salones y mesas") y en
-- docs/producto/plan/sprint-06-vender-en-mesa.md.
--
-- mesa.salon_id queda NULLABLE a proposito: el codigo que crea mesas asigna siempre un salon (el
-- "Principal" del local si no se elige otro), pero una fila insertada por fuera de ese camino no debe
-- romper. La migracion deja todas las mesas de hoy en un salon "Principal" por local.
CREATE TABLE salon (
    id               CHAR(26) PRIMARY KEY,
    local_id         CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    nombre           VARCHAR(60) NOT NULL,
    descripcion      VARCHAR(250),
    icono            VARCHAR(20) NOT NULL DEFAULT 'cubiertos',
    color            VARCHAR(20) NOT NULL DEFAULT 'verde',
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    ancho_plano      INTEGER NOT NULL DEFAULT 1200,
    alto_plano       INTEGER NOT NULL DEFAULT 800,
    color_piso       VARCHAR(7) NOT NULL DEFAULT '#F3EEDF',
    version          INTEGER NOT NULL DEFAULT 0,
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por       CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    actualizado_en   TIMESTAMPTZ,
    actualizado_por  CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT salon_nombre_unico_por_local UNIQUE (local_id, nombre),
    CONSTRAINT salon_plano_positivo CHECK (ancho_plano > 0 AND alto_plano > 0),
    CONSTRAINT salon_color_piso_hex CHECK (color_piso ~ '^#[0-9A-Fa-f]{6}$')
);

CREATE INDEX ix_salon_local ON salon(local_id, nombre);

ALTER TABLE mesa
    ADD COLUMN salon_id        CHAR(26) REFERENCES salon(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    ADD COLUMN forma           VARCHAR(12) NOT NULL DEFAULT 'cuadrada',
    ADD COLUMN vip             BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN zona_exterior   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN zona_fumadores  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN pos_x           INTEGER,
    ADD COLUMN pos_y           INTEGER,
    ADD COLUMN ancho           INTEGER NOT NULL DEFAULT 84,
    ADD COLUMN alto            INTEGER NOT NULL DEFAULT 84,
    ADD CONSTRAINT mesa_forma_valida CHECK (forma IN ('cuadrada', 'redonda', 'rectangular')),
    ADD CONSTRAINT mesa_medidas_positivas CHECK (ancho > 0 AND alto > 0);

CREATE INDEX ix_mesa_salon ON mesa(salon_id);

-- Un salon "Principal" por cada local que ya tiene mesas, y sus mesas dentro de el, en una cuadricula
-- (8 por fila) para que el plano nazca ordenado. El id sale del md5: 26 caracteres hexadecimales.
INSERT INTO salon (id, local_id, nombre, icono, color)
SELECT UPPER(SUBSTRING(MD5(local_id || 'principal') FROM 1 FOR 26)), local_id, 'Principal', 'cubiertos', 'verde'
FROM mesa
GROUP BY local_id;

UPDATE mesa m
SET salon_id = s.id
FROM salon s
WHERE s.local_id = m.local_id AND s.nombre = 'Principal' AND m.salon_id IS NULL;

UPDATE mesa m
SET pos_x = 60 + ((n.orden - 1) % 8) * 130,
    pos_y = 60 + ((n.orden - 1) / 8) * 130
FROM (SELECT id, ROW_NUMBER() OVER (PARTITION BY local_id ORDER BY numero) AS orden FROM mesa) n
WHERE n.id = m.id;
