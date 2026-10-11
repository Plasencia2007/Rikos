-- Sprint 6, entrega 2 (US-028): el pedido y sus lineas. Decisiones en
-- docs/producto/plan/sprint-06-vender-en-mesa.md (3.3, 3.4, 3.11 y 4.1).
--
-- Difiere del modelo original en tres cosas, todas decididas en la especificacion:
--  * subtotal, total y cargo de taper NO se guardan: se derivan al leer las lineas
--    (mismo criterio que el saldo de la cuenta del pensionista);
--  * detalle_pedido.tipo_linea separa el menu, la comida del plan del pensionista
--    (desayuno y cena, que no tienen un producto de la carta) y el extra libre;
--    por eso producto_id puede ser nulo en esos dos ultimos;
--  * el costo, la receta y su composicion se congelan AL CERRAR (sprint 7), no aqui.
CREATE TABLE pedido (
    id                CHAR(26) PRIMARY KEY,
    local_id          CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    numero            VARCHAR(20) NOT NULL,
    fecha_operativa   DATE NOT NULL,
    canal             VARCHAR(12) NOT NULL CHECK (canal IN ('mesa', 'para_llevar')),
    servicio          VARCHAR(10) NOT NULL CHECK (servicio IN ('desayuno', 'almuerzo', 'cena')),
    mesa_id           CHAR(26) REFERENCES mesa(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    mozo_id           CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    estado            VARCHAR(20) NOT NULL DEFAULT 'abierto'
                      CHECK (estado IN ('abierto', 'comandado', 'pendiente_cierre')),
    abierto_en        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version           INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT pedido_mesa_segun_canal CHECK ((canal = 'mesa') = (mesa_id IS NOT NULL)),
    CONSTRAINT pedido_numero_unico UNIQUE (local_id, fecha_operativa, numero)
);

-- Una mesa tiene un solo pedido en curso.
CREATE UNIQUE INDEX ux_pedido_mesa_en_curso ON pedido(mesa_id)
    WHERE mesa_id IS NOT NULL AND estado IN ('abierto', 'comandado', 'pendiente_cierre');
CREATE INDEX ix_pedido_local_estado ON pedido(local_id, estado);
CREATE INDEX ix_pedido_mozo ON pedido(mozo_id);

CREATE TABLE cuenta_persona (
    id              CHAR(26) PRIMARY KEY,
    pedido_id       CHAR(26) NOT NULL REFERENCES pedido(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    etiqueta        VARCHAR(120) NOT NULL,
    orden           INTEGER NOT NULL,
    tipo_cliente    VARCHAR(12) NOT NULL DEFAULT 'regular' CHECK (tipo_cliente IN ('regular', 'pensionista')),
    pensionista_id  CHAR(26) REFERENCES pensionista(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    estado          VARCHAR(10) NOT NULL DEFAULT 'abierta' CHECK (estado IN ('abierta', 'cerrada')),
    CONSTRAINT cuenta_persona_pensionista_segun_tipo
        CHECK ((tipo_cliente = 'pensionista') = (pensionista_id IS NOT NULL))
);

-- Un pensionista aparece una sola vez en un pedido.
CREATE UNIQUE INDEX ux_cuenta_persona_pensionista ON cuenta_persona(pedido_id, pensionista_id)
    WHERE pensionista_id IS NOT NULL;
CREATE INDEX ix_cuenta_persona_pedido ON cuenta_persona(pedido_id, orden);
CREATE INDEX ix_cuenta_persona_pensionista ON cuenta_persona(pensionista_id) WHERE pensionista_id IS NOT NULL;

CREATE TABLE detalle_pedido (
    id                  CHAR(26) PRIMARY KEY,
    cuenta_persona_id   CHAR(26) NOT NULL REFERENCES cuenta_persona(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    tipo_linea          VARCHAR(14) NOT NULL
                        CHECK (tipo_linea IN ('menu_normal', 'menu_extra', 'plan_desayuno', 'plan_cena',
                                              'producto', 'extra_libre')),
    producto_id         CHAR(26) REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    descripcion         VARCHAR(120),
    cantidad            INTEGER NOT NULL CHECK (cantidad > 0),
    observacion         VARCHAR(250),
    estado              VARCHAR(10) NOT NULL DEFAULT 'pendiente' CHECK (estado IN ('pendiente', 'comandado', 'anulado')),
    -- Snapshot del precio al agregar la linea. Nunca cero: sin precio no se agrega.
    precio_unitario     BIGINT NOT NULL CHECK (precio_unitario > 0),
    taper_unitario      BIGINT NOT NULL DEFAULT 0 CHECK (taper_unitario >= 0),
    tarifa_origen       VARCHAR(10) NOT NULL CHECK (tarifa_origen IN ('publico', 'plan', 'extra', 'manual')),
    plan_tarifario_id   CHAR(26) REFERENCES plan_tarifario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    segundo_del_dia     BOOLEAN NOT NULL DEFAULT FALSE,
    creado_en           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    creado_por          CHAR(26) REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT detalle_producto_segun_tipo
        CHECK (producto_id IS NOT NULL OR tipo_linea IN ('plan_desayuno', 'plan_cena', 'extra_libre')),
    CONSTRAINT detalle_extra_libre_con_descripcion
        CHECK (tipo_linea <> 'extra_libre' OR (descripcion IS NOT NULL AND btrim(descripcion) <> ''))
);

CREATE INDEX ix_detalle_pedido_persona ON detalle_pedido(cuenta_persona_id, creado_en);
CREATE INDEX ix_detalle_pedido_producto ON detalle_pedido(producto_id) WHERE producto_id IS NOT NULL;
CREATE INDEX ix_detalle_pedido_plan ON detalle_pedido(plan_tarifario_id) WHERE plan_tarifario_id IS NOT NULL;
CREATE INDEX ix_detalle_pedido_creado_por ON detalle_pedido(creado_por);

CREATE TABLE detalle_pedido_componente (
    id                 CHAR(26) PRIMARY KEY,
    detalle_pedido_id  CHAR(26) NOT NULL REFERENCES detalle_pedido(id) ON DELETE CASCADE ON UPDATE RESTRICT,
    componente         VARCHAR(20) NOT NULL CHECK (componente IN ('entrada', 'segundo', 'guarnicion', 'refresco', 'postre')),
    producto_id        CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    opcion_menu_id     CHAR(26) REFERENCES opcion_menu(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT detalle_componente_unico UNIQUE (detalle_pedido_id, componente)
);

CREATE INDEX ix_detalle_componente_producto ON detalle_pedido_componente(producto_id);
CREATE INDEX ix_detalle_componente_opcion ON detalle_pedido_componente(opcion_menu_id) WHERE opcion_menu_id IS NOT NULL;

ALTER TABLE auditoria DROP CONSTRAINT auditoria_evento_check;
ALTER TABLE auditoria ADD CONSTRAINT auditoria_evento_check
    CHECK (evento IN ('descuento', 'anulacion', 'reapertura', 'ajuste_inventario', 'transferencia',
                       'cambio_receta', 'cambio_precio', 'cambio_permiso', 'aprobacion_abono',
                       'rechazo_abono', 'cierre_caja', 'marcado_agotado', 'cambio_plan',
                       'cambio_estado', 'eliminacion', 'acceso_denegado', 'reactivacion',
                       'cambio_parametro', 'cambio_credencial', 'movimiento_cuenta', 'cambio_limite',
                       'cambio_mesa', 'bloqueo_mesa', 'precio_manual'));
