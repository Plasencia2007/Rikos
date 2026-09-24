-- modelo-de-datos.md secciones 4.1-4.6 (US-008, US-009, US-011), FKs de
-- 22.3, indices de 23.2. Primer modulo de negocio real (sprint 2).

CREATE TABLE categoria_producto (
    id                  CHAR(26) PRIMARY KEY,
    categoria_padre_id  CHAR(26) REFERENCES categoria_producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    nombre              VARCHAR(80) NOT NULL,
    naturaleza          VARCHAR(20) NOT NULL CHECK (naturaleza IN
                            ('componente_menu', 'producto_fijo', 'extra', 'insumo', 'subreceta')),
    componente_menu     VARCHAR(20) CHECK (componente_menu IN
                            ('entrada', 'segundo', 'guarnicion', 'refresco', 'postre')),
    nivel               INTEGER NOT NULL CHECK (nivel IN (1, 2)),
    orden               INTEGER NOT NULL DEFAULT 0,
    activo              BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por          CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    actualizado_en      TIMESTAMPTZ,
    actualizado_por     CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

-- 4.1: nombre unico dentro de su padre. Las raices (categoria_padre_id NULL)
-- se cubren aparte porque NULL no colisiona consigo mismo en un UNIQUE normal.
CREATE UNIQUE INDEX ux_categoria_nombre_por_padre ON categoria_producto(categoria_padre_id, nombre)
    WHERE categoria_padre_id IS NOT NULL;
CREATE UNIQUE INDEX ux_categoria_nombre_raiz ON categoria_producto(nombre)
    WHERE categoria_padre_id IS NULL;

CREATE INDEX ix_categoria_padre_id ON categoria_producto(categoria_padre_id);

CREATE TABLE unidad_medida (
    id       CHAR(26) PRIMARY KEY,
    codigo   VARCHAR(10) NOT NULL,
    nombre   VARCHAR(60) NOT NULL,
    tipo     VARCHAR(10) NOT NULL CHECK (tipo IN ('masa', 'volumen', 'unidad')),
    activo   BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE unidad_medida ADD CONSTRAINT uq_unidad_codigo UNIQUE (codigo);

CREATE TABLE producto (
    id                    CHAR(26) PRIMARY KEY,
    codigo                VARCHAR(30) NOT NULL,
    nombre                VARCHAR(150) NOT NULL,
    categoria_id          CHAR(26) NOT NULL REFERENCES categoria_producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    unidad_base_id        CHAR(26) NOT NULL REFERENCES unidad_medida(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    es_vendible           BOOLEAN NOT NULL DEFAULT FALSE,
    es_insumo             BOOLEAN NOT NULL DEFAULT FALSE,
    es_subreceta          BOOLEAN NOT NULL DEFAULT FALSE,
    precio_venta          BIGINT,
    costo_ultima_compra   NUMERIC(14, 6),
    origen_costo          VARCHAR(10) CHECK (origen_costo IN ('manual', 'compra')),
    fecha_ultima_compra   DATE,
    controla_stock        BOOLEAN NOT NULL DEFAULT TRUE,
    activo                BOOLEAN NOT NULL DEFAULT TRUE,
    version               INTEGER NOT NULL DEFAULT 0,
    creado_en             TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por            CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    actualizado_en        TIMESTAMPTZ,
    actualizado_por       CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

ALTER TABLE producto ADD CONSTRAINT uq_producto_codigo UNIQUE (codigo);
CREATE INDEX ix_producto_categoria_id ON producto(categoria_id);
CREATE INDEX ix_producto_unidad_base_id ON producto(unidad_base_id);

CREATE TABLE producto_servicio (
    producto_id  CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE CASCADE ON UPDATE CASCADE,
    servicio     VARCHAR(10) NOT NULL CHECK (servicio IN ('desayuno', 'almuerzo', 'cena')),
    activo       BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (producto_id, servicio)
);

-- 23.2: "son las 7, ¿que puedo vender?" -- se consulta por servicio primero.
CREATE INDEX ix_producto_servicio_servicio ON producto_servicio(servicio, producto_id);

CREATE TABLE presentacion_compra (
    id                     CHAR(26) PRIMARY KEY,
    producto_id            CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    nombre                 VARCHAR(80) NOT NULL,
    unidad_id              CHAR(26) NOT NULL REFERENCES unidad_medida(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    factor_a_unidad_base   NUMERIC(14, 6) NOT NULL CHECK (factor_a_unidad_base > 0),
    es_predeterminada      BOOLEAN NOT NULL DEFAULT FALSE,
    activo                 BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX ix_presentacion_producto_id ON presentacion_compra(producto_id);

-- 4.4: una sola predeterminada activa por producto. Constraint de base de
-- datos, no solo de aplicacion -- cierra el hueco de concurrencia.
CREATE UNIQUE INDEX ux_presentacion_predeterminada ON presentacion_compra(producto_id)
    WHERE es_predeterminada AND activo;

CREATE TABLE conversion_unidad (
    id                  CHAR(26) PRIMARY KEY,
    producto_id         CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE CASCADE ON UPDATE CASCADE,
    unidad_origen_id    CHAR(26) NOT NULL REFERENCES unidad_medida(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    unidad_destino_id   CHAR(26) NOT NULL REFERENCES unidad_medida(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    factor              NUMERIC(14, 6) NOT NULL CHECK (factor > 0)
);

CREATE INDEX ix_conversion_producto_id ON conversion_unidad(producto_id);
ALTER TABLE conversion_unidad ADD CONSTRAINT uq_conversion_par_unidades
    UNIQUE (producto_id, unidad_origen_id, unidad_destino_id);

CREATE TABLE proveedor (
    id              CHAR(26) PRIMARY KEY,
    ruc             VARCHAR(11),
    razon_social    VARCHAR(200) NOT NULL,
    nombre_comercial VARCHAR(120),
    telefono        VARCHAR(20),
    correo          VARCHAR(160),
    direccion       VARCHAR(250),
    contacto        VARCHAR(120),
    activo          BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por      CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    actualizado_en  TIMESTAMPTZ,
    actualizado_por CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE UNIQUE INDEX ux_proveedor_ruc ON proveedor(ruc) WHERE ruc IS NOT NULL;
