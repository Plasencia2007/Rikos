-- US-013 (bloque 4 del modelo de datos, §6.1-6.3): programacion semanal del
-- almuerzo. Central, sin local_id. Decisiones en
-- docs/producto/plan/sprint-04-costos-y-menu.md (entrega 3).

-- "En servicio" y "pasada" no se guardan: se calculan por fecha sobre una
-- semana confirmada (decidido con el usuario en el sprint 4).
CREATE TABLE semana_menu (
    id               CHAR(26) PRIMARY KEY,
    fecha_inicio     DATE NOT NULL CHECK (EXTRACT(ISODOW FROM fecha_inicio) = 1),
    fecha_fin        DATE NOT NULL,
    estado           VARCHAR(12) NOT NULL DEFAULT 'borrador' CHECK (estado IN ('borrador', 'confirmado')),
    confirmada_en    TIMESTAMPTZ,
    confirmada_por   CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    version          INTEGER NOT NULL DEFAULT 0,
    creado_en        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por       CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CHECK (fecha_fin = fecha_inicio + 6)
);

ALTER TABLE semana_menu ADD CONSTRAINT uq_semana_menu_fecha_inicio UNIQUE (fecha_inicio);

CREATE TABLE dia_menu (
    id              CHAR(26) PRIMARY KEY,
    semana_menu_id  CHAR(26) NOT NULL REFERENCES semana_menu(id) ON DELETE CASCADE ON UPDATE CASCADE,
    fecha           DATE NOT NULL,
    observacion     VARCHAR(250)
);

-- Una fecha pertenece a una sola semana, asi que unica en toda la tabla
-- (mas fuerte que "unica dentro de la semana" del modelo, y lo implica).
ALTER TABLE dia_menu ADD CONSTRAINT uq_dia_menu_fecha UNIQUE (fecha);
CREATE INDEX ix_dia_menu_semana ON dia_menu(semana_menu_id);

CREATE TABLE opcion_menu (
    id                   CHAR(26) PRIMARY KEY,
    dia_menu_id          CHAR(26) NOT NULL REFERENCES dia_menu(id) ON DELETE CASCADE ON UPDATE CASCADE,
    producto_id          CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    -- Derivado de categoria_producto.componente_menu y guardado: la semana
    -- de marzo sigue diciendo lo que dijo aunque despues se reclasifique
    -- el plato (§6.3).
    componente           VARCHAR(20) NOT NULL CHECK (componente IN
                             ('entrada', 'segundo', 'guarnicion', 'refresco', 'postre')),
    tipo_menu            VARCHAR(10) NOT NULL DEFAULT 'normal' CHECK (tipo_menu IN ('normal', 'extra')),
    porciones_estimadas  INTEGER CHECK (porciones_estimadas > 0),
    activo               BOOLEAN NOT NULL DEFAULT TRUE,
    CHECK (tipo_menu = 'normal' OR componente = 'segundo')
);

ALTER TABLE opcion_menu ADD CONSTRAINT uq_opcion_menu_dia_producto UNIQUE (dia_menu_id, producto_id);
CREATE INDEX ix_opcion_menu_dia ON opcion_menu(dia_menu_id);
-- La advertencia por repeticion busca un producto en fechas cercanas.
CREATE INDEX ix_opcion_menu_producto ON opcion_menu(producto_id);

