# Sprint 2 — Catálogo, unidades y proveedores — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Ship the first business-domain module (categorías, unidades, productos, presentaciones, conversiones, proveedores) with real backend enforcement, plus the three infra pieces it needs: OpenAPI-generated frontend types (HT-05), a shared numeric-entry component (HT-07), and a generalized collapsible sidebar with an icon-rail mode (9 bis).

**Architecture:** Backend follows the existing `identidad`/`locales` module shape exactly (`infraestructura`/`aplicacion`/`api` packages, `Resultado<T,E>` for expected failures, route-matcher permission gating in `ConfiguracionSeguridad`, ULID ids via `Identificador.nuevo().valor()`). Frontend follows the existing `features/*` + `comun/componentes` shape, with two new cross-cutting pieces (`EntradaNumerica`, the rebuilt `BarraLateral`) that other screens will depend on later.

**Tech Stack:** Spring Boot 4 / Java 21 / PostgreSQL 17 / Flyway / Spring Data JPA — React 19 / TypeScript / Vite / React Router 7. New: `springdoc-openapi-starter-webmvc-ui` (backend), `openapi-typescript` (frontend, dev dependency).

**Spec:** `docs/superpowers/specs/2026-09-24-sprint2-catalogo-design.md`

## Global Constraints

- Flyway is authoritative: never edit an applied migration, no `DROP TABLE`. Next migration numbers are `V14` and `V15`.
- Composite-key entities map with plain-`String` `@Id` fields plus a read-only `@ManyToOne` association, per DT-041/2.8 bis — never rely on `@JdbcTypeCode(CHAR)` propagating through `@IdClass`.
- Every mutating use case takes `actorId` (from `@AuthenticationPrincipal String`) and stamps `creadoPor`/`actualizadoPor` — no anonymous writes.
- The backend is the real gate: every `catalogo.*` permission is enforced in `ConfiguracionSeguridad`, never only hidden in the UI.
- IDs are ULIDs from `Identificador.nuevo().valor()` — never DB autoincrement.
- Money is céntimos-as-integer (`dinero`); costs are 6-decimal (`costo`, `numeric(14,6)` in Postgres, `BigDecimal` in Java). Rounding (half-up, to the céntimo) happens only when *presenting*, never in intermediate calculations.
- The four screen states (vacío, cargando, con datos, error) apply to every new screen, using the existing `EstadoVacio`/`EstadoCargando`/`EstadoError` components.
- "Local" is the only word used in the UI for a physical location — never "sucursal" (carried over from Sprint 1, still applies to any new copy).
- Nothing in `menu`, `recetas`, `venta`, `caja`, `pensionistas` is touched — those are later sprints.
- Generated OpenAPI types live in `src/api/generado/` and are never hand-edited; that path goes in `.gitignore`.

## Review Focus

- **A product code with leading/trailing whitespace or a stray character** — the spec never says to normalize `codigo`, but every other unique code in the system (`local.codigo`) is validated with a strict pattern; an unvalidated free-text unique column would let `"ARROZ"` and `" ARROZ "` both exist and collide invisibly in reports. Task 6 validates the format and its test proves a code with a leading space is rejected before it ever reaches the uniqueness check.
- **A category that used to have children but they were all deactivated being treated as a leaf again, and vice versa** — the spec says "categoría sin hijas", and the plan defines that as "no *active* children" (`existsByCategoriaPadreIdAndActivoTrue`). Task 3's repository test proves both directions on the category itself: a root starts as a leaf, stops being one once an active child exists, and becomes one again once that child is deactivated. What the plan does **not** add a test for: a product created while its category was still a leaf, followed by a *new* sibling category being added under the same parent afterward — the product's own row is never re-validated (nothing in this module re-walks existing products when a category changes), so it would keep pointing at a category that is no longer technically a leaf. This matches the model's own framing ("es el error más fácil de cometer al **cargar** el catálogo" — a load-time rule, not a standing invariant enforced retroactively), so it's left as expected behavior rather than a bug, and is called out explicitly in Task 33's historia doc as a case worth a human decision if it ever surprises someone in practice.
- **A `factor_a_unidad_base` or conversión `factor` of zero or negative** — the model only says "cantidad", not "positive cantidad", but a zero or negative factor makes every downstream stock calculation silently wrong instead of loudly wrong. Task 8 and Task 9 both validate `factor > 0` and both have a test proving the rejection.
- **Two presentations both marked `es_predeterminada` for the same product** — Task 8's use-case test proves the application-level check rejects a second one. The database's own unique partial index (Task 1) is the layer that actually closes the concurrent-request gap a naive check-then-insert would have; this plan does not add a second test that deliberately bypasses the use case to hit the constraint directly (e.g. via two raw concurrent inserts), since the existing project has no precedent for that style of concurrency test — if this gap matters enough to prove independently of the use case, add it as a follow-up, not silently assumed covered here.
- **A `proveedor.ruc` of exactly 11 characters but non-numeric, or 10/12 characters** — the model says `texto(11)`, which bounds length but not content; Task 10 validates the RUC is either null or exactly 11 digits, matching how `empresa.ruc` is already validated elsewhere in the seed data's comment (`20123456789`, 11 digits), and its test proves a 5-digit RUC is rejected.

---

## Phase A — Backend: `catalogo` module

### Task 1: Migration `V14__catalogo.sql` — schema

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/resources/db/migration/V14__catalogo.sql`
- Test: manual — applied via the existing Flyway-on-startup mechanism, verified in Task 2

**Interfaces:**
- Produces: the 7 tables (`categoria_producto`, `unidad_medida`, `producto`, `producto_servicio`, `presentacion_compra`, `conversion_unidad`, `proveedor`) that every later task's entities map to. Column names below are exactly what later `@Column(name=...)` annotations must match.

- [ ] **Step 1: Write the migration**

```sql
-- rikos-backend/servicio-operacion/src/main/resources/db/migration/V14__catalogo.sql
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
```

- [ ] **Step 2: Commit**

```bash
cd rikos-backend
git add servicio-operacion/src/main/resources/db/migration/V14__catalogo.sql
git commit -m "feat(catalogo): migracion V14 con las 7 tablas del modulo catalogo"
```

(If the repo has no `.git` yet, skip this and every later commit step — confirmed in this project's case: `rikos-backend` and `rikos-web` currently have no git repository at all. Note this to the human partner once, then proceed without commit steps for the rest of the plan, saving work by re-running `mvn test`/`tsc` instead as the checkpoint.)

---

### Task 2: Migration `V15__semilla_catalogo.sql` — seed data, and verify both migrations apply

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/resources/db/migration/V15__semilla_catalogo.sql`

**Interfaces:**
- Consumes: `usuario` table (FK target for `creado_por`, left NULL here — system seed, same pattern as `SembradorCatalogoPermisos` using `null` for `creado_por` rather than a placeholder string, per the FK-violation lesson from Sprint 1).
- Produces: 5 rows in `unidad_medida`, 9 root rows + their subcategory rows in `categoria_producto`, fully seeded per model section 4.1.1.

- [ ] **Step 1: Write the seed migration**

```sql
-- rikos-backend/servicio-operacion/src/main/resources/db/migration/V15__semilla_catalogo.sql
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
```

- [ ] **Step 2: Apply and verify both migrations against the dev database**

Run:
```bash
cd rikos-backend/servicio-operacion
../mvnw.cmd flyway:info "-Dspring-boot.run.profiles=local"
```
Expected: `V14` and `V15` listed as `Pending` (or `Success` after the next app start, which runs Flyway automatically). Start the app once (`../mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"`) and confirm no Flyway errors in the startup log, then verify the seed counts directly:

```bash
docker exec rikos-postgres psql -U rikos -d rikos_operacion -c "select count(*) from unidad_medida; select count(*) from categoria_producto where nivel=1; select count(*) from categoria_producto where nivel=2;"
```
Expected: `5`, `9`, `28`.

- [ ] **Step 3: Commit**

```bash
git add servicio-operacion/src/main/resources/db/migration/V15__semilla_catalogo.sql
git commit -m "feat(catalogo): semilla de unidades y arbol de categorias (4.1.1)"
```

---

### Task 3: Entities `CategoriaProducto` and `UnidadMedida` + repositories

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/CategoriaProducto.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/CategoriaProductoRepositorio.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/UnidadMedida.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/UnidadMedidaRepositorio.java`
- Test: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/infraestructura/CategoriaProductoRepositorioTest.java`

**Interfaces:**
- Produces: `CategoriaProducto` (getters: `getId`, `getCategoriaPadre`, `getNombre`, `getNaturaleza`, `getComponenteMenu`, `getNivel`, `isActivo`; domain methods `actualizar(String nombre, int orden)`, `desactivar(String)`), `CategoriaProductoRepositorio` (`existsByCategoriaPadreIdAndActivoTrue(String)`, `findByCategoriaPadreIdIsNull()`, `findByCategoriaPadreId(String)`), `UnidadMedida` (getters `getId`, `getCodigo`, `getNombre`, `getTipo`, `isActivo`), `UnidadMedidaRepositorio` (`existsByCodigo(String)`).

- [ ] **Step 1: Write the entities**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/CategoriaProducto.java
package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 4.1: arbol de dos niveles con autorreferencia. naturaleza y
 * componente_menu solo se fijan en la raiz (nivel 1); una subcategoria los
 * hereda sin poder contradecir a su padre -- el caso de uso valida eso,
 * no esta entidad.
 */
@Entity
@Table(name = "categoria_producto")
public class CategoriaProducto {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "categoria_padre_id")
    private CategoriaProducto categoriaPadre;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(nullable = false, length = 20)
    private String naturaleza;

    @Column(name = "componente_menu", length = 20)
    private String componenteMenu;

    @Column(nullable = false)
    private int nivel;

    @Column(nullable = false)
    private int orden;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creado_por", length = 26)
    private String creadoPor;

    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "actualizado_por", length = 26)
    private String actualizadoPor;

    protected CategoriaProducto() {
    }

    /** Raiz: categoriaPadre null, naturaleza y componenteMenu propios. */
    public CategoriaProducto(String id, String nombre, String naturaleza, String componenteMenu, int orden,
                              String creadoPor) {
        this.id = id;
        this.categoriaPadre = null;
        this.nombre = nombre;
        this.naturaleza = naturaleza;
        this.componenteMenu = componenteMenu;
        this.nivel = 1;
        this.orden = orden;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    /** Subcategoria: hereda naturaleza y componenteMenu del padre. */
    public CategoriaProducto(String id, CategoriaProducto padre, String nombre, int orden, String creadoPor) {
        this.id = id;
        this.categoriaPadre = padre;
        this.nombre = nombre;
        this.naturaleza = padre.naturaleza;
        this.componenteMenu = padre.componenteMenu;
        this.nivel = 2;
        this.orden = orden;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public void actualizar(String nombre, int orden, String actualizadoPor) {
        this.nombre = nombre;
        this.orden = orden;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public void desactivar(String actualizadoPor) {
        this.activo = false;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public String getId() {
        return id;
    }

    public CategoriaProducto getCategoriaPadre() {
        return categoriaPadre;
    }

    public String getNombre() {
        return nombre;
    }

    public String getNaturaleza() {
        return naturaleza;
    }

    public String getComponenteMenu() {
        return componenteMenu;
    }

    public int getNivel() {
        return nivel;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isActivo() {
        return activo;
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/CategoriaProductoRepositorio.java
package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaProductoRepositorio extends JpaRepository<CategoriaProducto, String> {

    /** Es hoja si esto es false: nadie cuelga de ella (4.1, regla de hoja). */
    boolean existsByCategoriaPadreIdAndActivoTrue(String categoriaPadreId);

    List<CategoriaProducto> findByCategoriaPadreIdIsNullOrderByOrden();

    List<CategoriaProducto> findByCategoriaPadreIdOrderByOrden(String categoriaPadreId);
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/UnidadMedida.java
package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "unidad_medida")
public class UnidadMedida {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(nullable = false, length = 10)
    private String codigo;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false)
    private boolean activo = true;

    protected UnidadMedida() {
    }

    public UnidadMedida(String id, String codigo, String nombre, String tipo) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public void desactivar() {
        this.activo = false;
    }

    public String getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getTipo() {
        return tipo;
    }

    public boolean isActivo() {
        return activo;
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/UnidadMedidaRepositorio.java
package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadMedidaRepositorio extends JpaRepository<UnidadMedida, String> {
    boolean existsByCodigo(String codigo);
}
```

- [ ] **Step 2: Write the failing repository test**

```java
// rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/infraestructura/CategoriaProductoRepositorioTest.java
package pe.rikos.operacion.catalogo.infraestructura;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CategoriaProductoRepositorioTest {

    @Autowired
    private CategoriaProductoRepositorio categorias;

    @Test
    void una_categoria_sin_hijas_activas_es_hoja() {
        CategoriaProducto raiz = categorias.save(
                new CategoriaProducto(Identificador.nuevo().valor(), "Raiz de prueba", "insumo", null, 1, null));

        assertThat(categorias.existsByCategoriaPadreIdAndActivoTrue(raiz.getId())).isFalse();

        CategoriaProducto hija = categorias.save(
                new CategoriaProducto(Identificador.nuevo().valor(), raiz, "Hija de prueba", 1, null));

        assertThat(categorias.existsByCategoriaPadreIdAndActivoTrue(raiz.getId())).isTrue();

        hija.desactivar(null);
        categorias.save(hija);
        categorias.flush();

        assertThat(categorias.existsByCategoriaPadreIdAndActivoTrue(raiz.getId())).isFalse();
    }

    @Test
    void una_subcategoria_hereda_naturaleza_y_componente_menu_del_padre() {
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), "Entradas de prueba", "componente_menu", "entrada", 1, null));
        CategoriaProducto hija = categorias.save(
                new CategoriaProducto(Identificador.nuevo().valor(), raiz, "Sopas de prueba", 1, null));

        assertThat(hija.getNaturaleza()).isEqualTo("componente_menu");
        assertThat(hija.getComponenteMenu()).isEqualTo("entrada");
    }
}
```

- [ ] **Step 3: Run test to verify it fails (entities don't exist yet before Step 1's files are saved — confirm Step 1 is actually saved first, then this step is really a green-from-the-start sanity check)**

Run: `../mvnw.cmd test -Dtest=CategoriaProductoRepositorioTest`
Expected: PASS (this is new infrastructure with no prior broken state — the important check here is that it actually exercises the DB, not that it starts red; the standard red-green cycle is preserved in the *use case* tests in Task 5, where real domain rules are new)

- [ ] **Step 4: Commit**

```bash
git add servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/CategoriaProducto.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/CategoriaProductoRepositorio.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/UnidadMedida.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/UnidadMedidaRepositorio.java servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/infraestructura/CategoriaProductoRepositorioTest.java
git commit -m "feat(catalogo): entidades CategoriaProducto y UnidadMedida"
```

---

### Task 4: Entity `Producto` + repository

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/Producto.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProductoRepositorio.java`

**Interfaces:**
- Consumes: `CategoriaProducto` (Task 3), `UnidadMedida` (Task 3).
- Produces: `Producto` (getters for every column; `actualizar(DatosActualizacion, String)`; `desactivar(String)`; `registrarCostoManual(BigDecimal, String)`; `registrarCostoDeCompra(BigDecimal, String)` — the two paths for `origen_costo` described in the spec), `ProductoRepositorio` (`existsByCodigo(String)`).

- [ ] **Step 1: Write the entity**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/Producto.java
package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 4.3: tres banderas de naturaleza, no un tipo unico -- el arroz es insumo,
 * entra en recetas, y se vende como guarnicion, las tres a la vez.
 *
 * origen_costo (decision del sprint 2, no esta en el modelo original):
 * 'manual' mientras el costo lo cargo a mano el usuario ("estimado, se
 * reemplaza con la primera compra"); 'compra' desde que una compra real
 * (sprint 9) lo confirma, momento en el que deja de ser editable a mano.
 */
@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaProducto categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "unidad_base_id", nullable = false)
    private UnidadMedida unidadBase;

    @Column(name = "es_vendible", nullable = false)
    private boolean esVendible;

    @Column(name = "es_insumo", nullable = false)
    private boolean esInsumo;

    @Column(name = "es_subreceta", nullable = false)
    private boolean esSubreceta;

    @Column(name = "precio_venta")
    private Long precioVenta;

    @Column(name = "costo_ultima_compra", precision = 14, scale = 6)
    private BigDecimal costoUltimaCompra;

    @Column(name = "origen_costo", length = 10)
    private String origenCosto;

    @Column(name = "fecha_ultima_compra")
    private LocalDate fechaUltimaCompra;

    @Column(name = "controla_stock", nullable = false)
    private boolean controlaStock = true;

    @Column(nullable = false)
    private boolean activo = true;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creado_por", length = 26)
    private String creadoPor;

    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "actualizado_por", length = 26)
    private String actualizadoPor;

    protected Producto() {
    }

    public Producto(String id, String codigo, String nombre, CategoriaProducto categoria, UnidadMedida unidadBase,
                     boolean esVendible, boolean esInsumo, boolean esSubreceta, Long precioVenta,
                     boolean controlaStock, String creadoPor) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.categoria = categoria;
        this.unidadBase = unidadBase;
        this.esVendible = esVendible;
        this.esInsumo = esInsumo;
        this.esSubreceta = esSubreceta;
        this.precioVenta = precioVenta;
        this.controlaStock = controlaStock;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public record DatosActualizacion(String nombre, CategoriaProducto categoria, UnidadMedida unidadBase,
                                      boolean esVendible, boolean esInsumo, boolean esSubreceta, Long precioVenta,
                                      boolean controlaStock) {
    }

    public void actualizar(DatosActualizacion datos, String actualizadoPor) {
        this.nombre = datos.nombre();
        this.categoria = datos.categoria();
        this.unidadBase = datos.unidadBase();
        this.esVendible = datos.esVendible();
        this.esInsumo = datos.esInsumo();
        this.esSubreceta = datos.esSubreceta();
        this.precioVenta = datos.precioVenta();
        this.controlaStock = datos.controlaStock();
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public void desactivar(String actualizadoPor) {
        this.activo = false;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    /** Costo a mano, mientras no exista una compra real (RN-047, sprint 9). */
    public void registrarCostoManual(BigDecimal costo, String actualizadoPor) {
        this.costoUltimaCompra = costo;
        this.origenCosto = "manual";
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    /** Lo llamara el caso de uso de confirmar-compra del sprint 9. Desde
     * aqui el costo deja de ser editable a mano (lo hace cumplir el caso
     * de uso de actualizar producto, no esta entidad). */
    public void registrarCostoDeCompra(BigDecimal costo, LocalDate fecha, String actualizadoPor) {
        this.costoUltimaCompra = costo;
        this.origenCosto = "compra";
        this.fechaUltimaCompra = fecha;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public boolean costoEsEditableAMano() {
        return !"compra".equals(origenCosto);
    }

    public String getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public CategoriaProducto getCategoria() {
        return categoria;
    }

    public UnidadMedida getUnidadBase() {
        return unidadBase;
    }

    public boolean isEsVendible() {
        return esVendible;
    }

    public boolean isEsInsumo() {
        return esInsumo;
    }

    public boolean isEsSubreceta() {
        return esSubreceta;
    }

    public Long getPrecioVenta() {
        return precioVenta;
    }

    public BigDecimal getCostoUltimaCompra() {
        return costoUltimaCompra;
    }

    public String getOrigenCosto() {
        return origenCosto;
    }

    public LocalDate getFechaUltimaCompra() {
        return fechaUltimaCompra;
    }

    public boolean isControlaStock() {
        return controlaStock;
    }

    public boolean isActivo() {
        return activo;
    }

    public int getVersion() {
        return version;
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProductoRepositorio.java
package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepositorio extends JpaRepository<Producto, String> {
    boolean existsByCodigo(String codigo);
}
```

- [ ] **Step 2: Commit**

```bash
git add servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/Producto.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProductoRepositorio.java
git commit -m "feat(catalogo): entidad Producto con origen_costo manual/compra"
```

---

### Task 5: Use cases `CrearCategoria` / `ActualizarCategoria` + `ErrorCatalogo`

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/ErrorCatalogo.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearCategoria.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/ActualizarCategoria.java`
- Test: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearCategoriaTest.java`

**Interfaces:**
- Consumes: `CategoriaProductoRepositorio` (Task 3).
- Produces: `ErrorCatalogo` enum (`NOMBRE_DUPLICADO`, `NO_ENCONTRADO`, `CATEGORIA_PADRE_NO_ENCONTRADA`, `CATEGORIA_NO_ES_HOJA`, `NATURALEZA_INVALIDA`, `PRECIO_VENTA_OBLIGATORIO`, `UNIDAD_OBLIGATORIA`, `FACTOR_INVALIDO`, `PAR_UNIDADES_DUPLICADO`, `PREDETERMINADA_DUPLICADA`, `RUC_INVALIDO`, `RUC_DUPLICADO`, `CODIGO_DUPLICADO`) — the full set every later use case in this module needs, defined once here since it's a single shared error type for the whole `catalogo` module (mirrors `ErrorLocal`/`ErrorRol` being one enum per module, not per use case). `CrearCategoria.ejecutar(String nombre, String naturaleza, String componenteMenu, String categoriaPadreId, int orden, String actorId)`, `ActualizarCategoria.ejecutar(String id, String nombre, int orden, String actorId)`.

- [ ] **Step 1: Write the error enum (shared by every use case in this module)**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/ErrorCatalogo.java
package pe.rikos.operacion.catalogo.aplicacion;

public enum ErrorCatalogo {
    NOMBRE_DUPLICADO,
    NO_ENCONTRADO,
    CATEGORIA_PADRE_NO_ENCONTRADA,
    CATEGORIA_NO_ES_HOJA,
    NATURALEZA_INVALIDA,
    CODIGO_DUPLICADO,
    CODIGO_FORMATO_INVALIDO,
    UNIDAD_OBLIGATORIA,
    PRECIO_VENTA_OBLIGATORIO,
    FACTOR_INVALIDO,
    PAR_UNIDADES_DUPLICADO,
    PREDETERMINADA_DUPLICADA,
    RUC_INVALIDO,
    RUC_DUPLICADO,
    TIENE_HISTORIAL,
    COSTO_YA_CONFIRMADO_POR_COMPRA
}
```

- [ ] **Step 2: Write the failing test for `CrearCategoria`**

```java
// rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearCategoriaTest.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CrearCategoriaTest {

    @Autowired
    private CrearCategoria crearCategoria;

    @Autowired
    private ActualizarCategoria actualizarCategoria;

    @Autowired
    private CategoriaProductoRepositorio categorias;

    @Test
    void crea_una_raiz_con_su_naturaleza_y_componente_menu() {
        Resultado<CategoriaProducto, ErrorCatalogo> resultado =
                crearCategoria.ejecutar("Raiz test A", "componente_menu", "postre", null, 1, "actor");

        assertThat(resultado.esExito()).isTrue();
        CategoriaProducto raiz = resultado.valorOLanza();
        assertThat(raiz.getNivel()).isEqualTo(1);
        assertThat(raiz.getComponenteMenu()).isEqualTo("postre");
    }

    @Test
    void una_subcategoria_hereda_naturaleza_del_padre_e_ignora_la_que_se_le_mande() {
        CategoriaProducto raiz =
                crearCategoria.ejecutar("Raiz test B", "insumo", null, null, 1, "actor").valorOLanza();

        Resultado<CategoriaProducto, ErrorCatalogo> resultado =
                crearCategoria.ejecutar("Hija test B", "producto_fijo", null, raiz.getId(), 1, "actor");

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().getNaturaleza()).isEqualTo("insumo");
    }

    @Test
    void rechaza_una_subcategoria_de_padre_inexistente() {
        Resultado<CategoriaProducto, ErrorCatalogo> resultado =
                crearCategoria.ejecutar("Hija huerfana", "insumo", null, "no-existe", 1, "actor");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.CATEGORIA_PADRE_NO_ENCONTRADA);
    }

    @Test
    void rechaza_naturaleza_invalida() {
        Resultado<CategoriaProducto, ErrorCatalogo> resultado =
                crearCategoria.ejecutar("Raiz mala", "no-existe-esta-naturaleza", null, null, 1, "actor");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.NATURALEZA_INVALIDA);
    }

    @Test
    void rechaza_nombre_duplicado_dentro_del_mismo_padre() {
        CategoriaProducto raiz =
                crearCategoria.ejecutar("Raiz test C", "insumo", null, null, 1, "actor").valorOLanza();
        crearCategoria.ejecutar("Repetida", "insumo", null, raiz.getId(), 1, "actor");

        Resultado<CategoriaProducto, ErrorCatalogo> resultado =
                crearCategoria.ejecutar("Repetida", "insumo", null, raiz.getId(), 2, "actor");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.NOMBRE_DUPLICADO);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `../mvnw.cmd test -Dtest=CrearCategoriaTest`
Expected: FAIL — `CrearCategoria`/`ActualizarCategoria` beans don't exist yet.

- [ ] **Step 4: Write the use cases**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearCategoria.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;

import java.util.Optional;
import java.util.Set;

@Service
public class CrearCategoria {

    private static final Set<String> NATURALEZAS_VALIDAS =
            Set.of("componente_menu", "producto_fijo", "extra", "insumo", "subreceta");

    private final CategoriaProductoRepositorio categorias;

    public CrearCategoria(CategoriaProductoRepositorio categorias) {
        this.categorias = categorias;
    }

    @Transactional
    public Resultado<CategoriaProducto, ErrorCatalogo> ejecutar(String nombre, String naturaleza,
            String componenteMenu, String categoriaPadreId, int orden, String actorId) {

        if (!NATURALEZAS_VALIDAS.contains(naturaleza)) {
            return Resultado.fracaso(ErrorCatalogo.NATURALEZA_INVALIDA);
        }

        if (categoriaPadreId == null) {
            if (categorias.findByCategoriaPadreIdIsNullOrderByOrden().stream()
                    .anyMatch(c -> c.getNombre().equals(nombre))) {
                return Resultado.fracaso(ErrorCatalogo.NOMBRE_DUPLICADO);
            }
            CategoriaProducto raiz = new CategoriaProducto(
                    Identificador.nuevo().valor(), nombre, naturaleza, componenteMenu, orden, actorId);
            return Resultado.exito(categorias.save(raiz));
        }

        Optional<CategoriaProducto> padre = categorias.findById(categoriaPadreId);
        if (padre.isEmpty()) {
            return Resultado.fracaso(ErrorCatalogo.CATEGORIA_PADRE_NO_ENCONTRADA);
        }
        if (categorias.findByCategoriaPadreIdOrderByOrden(categoriaPadreId).stream()
                .anyMatch(c -> c.getNombre().equals(nombre))) {
            return Resultado.fracaso(ErrorCatalogo.NOMBRE_DUPLICADO);
        }

        CategoriaProducto hija =
                new CategoriaProducto(Identificador.nuevo().valor(), padre.get(), nombre, orden, actorId);
        return Resultado.exito(categorias.save(hija));
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/ActualizarCategoria.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;

@Service
public class ActualizarCategoria {

    private final CategoriaProductoRepositorio categorias;

    public ActualizarCategoria(CategoriaProductoRepositorio categorias) {
        this.categorias = categorias;
    }

    @Transactional
    public Resultado<CategoriaProducto, ErrorCatalogo> ejecutar(String id, String nombre, int orden,
            String actorId) {
        return categorias.findById(id)
                .map(categoria -> {
                    categoria.actualizar(nombre, orden, actorId);
                    return Resultado.<CategoriaProducto, ErrorCatalogo>exito(categorias.save(categoria));
                })
                .orElse(Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO));
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `../mvnw.cmd test -Dtest=CrearCategoriaTest`
Expected: PASS (all 5 tests).

- [ ] **Step 6: Commit**

```bash
git add servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/ErrorCatalogo.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearCategoria.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/ActualizarCategoria.java servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearCategoriaTest.java
git commit -m "feat(catalogo): casos de uso CrearCategoria/ActualizarCategoria"
```

---

### Task 6: Use cases `CrearProducto` / `ActualizarProducto` / `DesactivarProducto` / `RegistrarCostoManual`

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearProducto.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/ActualizarProducto.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/DesactivarProducto.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/RegistrarCostoManual.java`
- Test: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearProductoTest.java`

**Interfaces:**
- Consumes: `ProductoRepositorio`, `CategoriaProductoRepositorio`, `UnidadMedidaRepositorio` (Tasks 3–4), `ErrorCatalogo` (Task 5).
- Produces: `CrearProducto.ejecutar(String codigo, String nombre, String categoriaId, String unidadBaseId, boolean esVendible, boolean esInsumo, boolean esSubreceta, Long precioVenta, boolean controlaStock, String actorId)`, `ActualizarProducto.ejecutar(String id, Producto.DatosActualizacion, String actorId)`, `DesactivarProducto.ejecutar(String id, String actorId)`, `RegistrarCostoManual.ejecutar(String productoId, BigDecimal costo, String actorId)`.

- [ ] **Step 1: Write the failing test**

```java
// rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearProductoTest.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CrearProductoTest {

    @Autowired
    private CrearProducto crearProducto;
    @Autowired
    private RegistrarCostoManual registrarCostoManual;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private ProductoRepositorio productos;

    private CategoriaProducto hoja(String nombre) {
        CategoriaProducto raiz = categorias.save(
                new pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto(
                        pe.rikos.contratos.Identificador.nuevo().valor(), "Raiz " + nombre, "insumo", null, 1, null));
        return categorias.save(new CategoriaProducto(
                pe.rikos.contratos.Identificador.nuevo().valor(), raiz, nombre, 1, null));
    }

    private CategoriaProducto raizConHijas(String nombre) {
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                pe.rikos.contratos.Identificador.nuevo().valor(), nombre, "insumo", null, 1, null));
        categorias.save(new CategoriaProducto(
                pe.rikos.contratos.Identificador.nuevo().valor(), raiz, nombre + " hija", 1, null));
        return raiz;
    }

    private UnidadMedida unidad() {
        return unidades.save(new UnidadMedida(pe.rikos.contratos.Identificador.nuevo().valor(), "kgtest",
                "Kilogramo test", "masa"));
    }

    @Test
    void crea_un_producto_insumo_sobre_una_categoria_hoja() {
        CategoriaProducto hoja = hoja("Verduras test");
        UnidadMedida u = unidad();

        Resultado<Producto, ErrorCatalogo> resultado = crearProducto.ejecutar(
                "PROD-001", "Zanahoria", hoja.getId(), u.getId(), false, true, false, null, true, "actor");

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().isEsInsumo()).isTrue();
    }

    @Test
    void rechaza_un_producto_colgado_de_una_categoria_con_hijas() {
        CategoriaProducto raizConHijas = raizConHijas("Raiz con hijas test");
        UnidadMedida u = unidad();

        Resultado<Producto, ErrorCatalogo> resultado = crearProducto.ejecutar(
                "PROD-002", "Producto mal colgado", raizConHijas.getId(), u.getId(), false, true, false, null, true,
                "actor");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.CATEGORIA_NO_ES_HOJA);
    }

    @Test
    void exige_precio_de_venta_si_es_vendible() {
        CategoriaProducto hoja = hoja("Guarnicion test");
        UnidadMedida u = unidad();

        Resultado<Producto, ErrorCatalogo> resultado = crearProducto.ejecutar(
                "PROD-003", "Arroz porcion", hoja.getId(), u.getId(), true, false, false, null, true, "actor");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.PRECIO_VENTA_OBLIGATORIO);
    }

    @Test
    void el_costo_manual_queda_marcado_como_editable_y_el_de_compra_no() {
        CategoriaProducto hoja = hoja("Abarrotes test");
        UnidadMedida u = unidad();
        Producto producto = crearProducto.ejecutar(
                "PROD-004", "Aceite", hoja.getId(), u.getId(), false, true, false, null, true, "actor")
                .valorOLanza();

        Resultado<Producto, ErrorCatalogo> conCostoManual =
                registrarCostoManual.ejecutar(producto.getId(), new BigDecimal("12.500000"), "actor");

        assertThat(conCostoManual.esExito()).isTrue();
        Producto actualizado = conCostoManual.valorOLanza();
        assertThat(actualizado.getOrigenCosto()).isEqualTo("manual");
        assertThat(actualizado.costoEsEditableAMano()).isTrue();

        actualizado.registrarCostoDeCompra(new BigDecimal("13.000000"), java.time.LocalDate.now(), "actor");
        assertThat(actualizado.getOrigenCosto()).isEqualTo("compra");
        assertThat(actualizado.costoEsEditableAMano()).isFalse();
    }

    @Test
    void rechaza_un_codigo_con_espacios_o_formato_invalido() {
        CategoriaProducto hoja = hoja("Bebidas test formato");
        UnidadMedida u = unidad();

        Resultado<Producto, ErrorCatalogo> resultado = crearProducto.ejecutar(
                " ARROZ ", "Arroz con espacios en el codigo", hoja.getId(), u.getId(), false, true, false, null,
                true, "actor");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.CODIGO_FORMATO_INVALIDO);
    }

    @Test
    void rechaza_una_categoria_inexistente_la_categoria_es_obligatoria() {
        UnidadMedida u = unidad();

        Resultado<Producto, ErrorCatalogo> resultado = crearProducto.ejecutar(
                "PROD-005", "Producto sin categoria valida", "no-existe-esta-categoria", u.getId(), false, true,
                false, null, true, "actor");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.NO_ENCONTRADO);
    }

    @Test
    void desactivar_no_borra_el_producto_sigue_consultable_con_su_historial() {
        CategoriaProducto hoja = hoja("Lacteos test desactivar");
        UnidadMedida u = unidad();
        Producto producto = crearProducto.ejecutar(
                "PROD-006", "Leche evaporada", hoja.getId(), u.getId(), false, true, false, null, true, "actor")
                .valorOLanza();

        producto.desactivar("actor");

        Producto encontrado = productos.findById(producto.getId()).orElseThrow();
        assertThat(encontrado.isActivo()).isFalse();
        assertThat(encontrado.getCodigo()).isEqualTo("PROD-006");
        assertThat(encontrado.getNombre()).isEqualTo("Leche evaporada");
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `../mvnw.cmd test -Dtest=CrearProductoTest`
Expected: FAIL — use cases don't exist yet.

- [ ] **Step 3: Write the use cases**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearProducto.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;

import java.util.regex.Pattern;

@Service
public class CrearProducto {

    // Sin espacios ni minusculas sueltas: "ARROZ" y "arroz " no deben poder
    // coexistir como codigos distintos que en realidad colisionan en
    // cualquier reporte que los muestre juntos. Mismo criterio que el
    // codigo de local (CrearLocal, sprint 1), adaptado: aqui se permiten
    // mayusculas porque el codigo de producto se imprime en etiquetas y
    // comandas, no es parte de una URL.
    private static final Pattern FORMATO_CODIGO = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9-]*$");

    private final ProductoRepositorio productos;
    private final CategoriaProductoRepositorio categorias;
    private final UnidadMedidaRepositorio unidades;

    public CrearProducto(ProductoRepositorio productos, CategoriaProductoRepositorio categorias,
                          UnidadMedidaRepositorio unidades) {
        this.productos = productos;
        this.categorias = categorias;
        this.unidades = unidades;
    }

    @Transactional
    public Resultado<Producto, ErrorCatalogo> ejecutar(String codigo, String nombre, String categoriaId,
            String unidadBaseId, boolean esVendible, boolean esInsumo, boolean esSubreceta, Long precioVenta,
            boolean controlaStock, String actorId) {

        if (codigo == null || !FORMATO_CODIGO.matcher(codigo).matches()) {
            return Resultado.fracaso(ErrorCatalogo.CODIGO_FORMATO_INVALIDO);
        }

        if (productos.existsByCodigo(codigo)) {
            return Resultado.fracaso(ErrorCatalogo.CODIGO_DUPLICADO);
        }

        CategoriaProducto categoria = categorias.findById(categoriaId).orElse(null);
        if (categoria == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }
        // 4.1: los productos cuelgan de la hoja, nunca de una categoria con
        // subcategorias activas -- es el error mas facil de cometer al cargar
        // el catalogo, y aqui se rechaza en vez de permitirse.
        if (categorias.existsByCategoriaPadreIdAndActivoTrue(categoriaId)) {
            return Resultado.fracaso(ErrorCatalogo.CATEGORIA_NO_ES_HOJA);
        }

        UnidadMedida unidad = unidades.findById(unidadBaseId).orElse(null);
        if (unidad == null) {
            return Resultado.fracaso(ErrorCatalogo.UNIDAD_OBLIGATORIA);
        }

        if (esVendible && precioVenta == null) {
            return Resultado.fracaso(ErrorCatalogo.PRECIO_VENTA_OBLIGATORIO);
        }

        Producto producto = new Producto(Identificador.nuevo().valor(), codigo, nombre, categoria, unidad,
                esVendible, esInsumo, esSubreceta, precioVenta, controlaStock, actorId);
        return Resultado.exito(productos.save(producto));
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/ActualizarProducto.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;

@Service
public class ActualizarProducto {

    private final ProductoRepositorio productos;
    private final CategoriaProductoRepositorio categorias;

    public ActualizarProducto(ProductoRepositorio productos, CategoriaProductoRepositorio categorias) {
        this.productos = productos;
        this.categorias = categorias;
    }

    @Transactional
    public Resultado<Producto, ErrorCatalogo> ejecutar(String id, Producto.DatosActualizacion datos,
            String actorId) {
        Producto producto = productos.findById(id).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }
        if (categorias.existsByCategoriaPadreIdAndActivoTrue(datos.categoria().getId())) {
            return Resultado.fracaso(ErrorCatalogo.CATEGORIA_NO_ES_HOJA);
        }
        if (datos.esVendible() && datos.precioVenta() == null) {
            return Resultado.fracaso(ErrorCatalogo.PRECIO_VENTA_OBLIGATORIO);
        }
        producto.actualizar(datos, actorId);
        return Resultado.exito(productos.save(producto));
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/DesactivarProducto.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;

/** Desactivar impide usarlo en registros nuevos y conserva su historial
 * -- nunca se borra (US-008). */
@Service
public class DesactivarProducto {

    private final ProductoRepositorio productos;

    public DesactivarProducto(ProductoRepositorio productos) {
        this.productos = productos;
    }

    @Transactional
    public Resultado<Producto, ErrorCatalogo> ejecutar(String id, String actorId) {
        return productos.findById(id)
                .map(producto -> {
                    producto.desactivar(actorId);
                    return Resultado.<Producto, ErrorCatalogo>exito(productos.save(producto));
                })
                .orElse(Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO));
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/RegistrarCostoManual.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;

import java.math.BigDecimal;

/** El costo inicial a mano (sprint 2). Deja de ser posible en cuanto el
 * producto ya tiene origen_costo='compra' (RegistrarCostoDeCompra, sprint 9). */
@Service
public class RegistrarCostoManual {

    private final ProductoRepositorio productos;

    public RegistrarCostoManual(ProductoRepositorio productos) {
        this.productos = productos;
    }

    @Transactional
    public Resultado<Producto, ErrorCatalogo> ejecutar(String productoId, BigDecimal costo, String actorId) {
        Producto producto = productos.findById(productoId).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }
        if (!producto.costoEsEditableAMano()) {
            return Resultado.fracaso(ErrorCatalogo.COSTO_YA_CONFIRMADO_POR_COMPRA);
        }
        producto.registrarCostoManual(costo, actorId);
        return Resultado.exito(productos.save(producto));
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `../mvnw.cmd test -Dtest=CrearProductoTest`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearProducto.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/ActualizarProducto.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/DesactivarProducto.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/RegistrarCostoManual.java servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearProductoTest.java
git commit -m "feat(catalogo): casos de uso de Producto, incluido el costo inicial manual"
```

---

### Task 7: `ProductoServicio` (clave compuesta) + `AsignarServiciosProducto`

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProductoServicioId.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProductoServicio.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProductoServicioRepositorio.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/AsignarServiciosProducto.java`
- Test: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/AsignarServiciosProductoTest.java`

**Interfaces:**
- Consumes: `Producto`/`ProductoRepositorio` (Task 4), `ErrorCatalogo` (Task 5).
- Produces: `AsignarServiciosProducto.ejecutar(String productoId, Set<String> servicios, String actorId)` — replaces the full set of services for a product, only allowed when the product's category has `naturaleza = producto_fijo`.

- [ ] **Step 1: Write the composite-key id class and entity (DT-041/2.8 bis pattern — plain `String` id fields, read-only `@ManyToOne` for navigation)**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProductoServicioId.java
package pe.rikos.operacion.catalogo.infraestructura;

import java.io.Serializable;
import java.util.Objects;

public class ProductoServicioId implements Serializable {
    private String productoId;
    private String servicio;

    public ProductoServicioId() {
    }

    public ProductoServicioId(String productoId, String servicio) {
        this.productoId = productoId;
        this.servicio = servicio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductoServicioId that)) return false;
        return Objects.equals(productoId, that.productoId) && Objects.equals(servicio, that.servicio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productoId, servicio);
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProductoServicio.java
package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 4.1 bis: en que servicios se ofrece un producto_fijo. Ver la nota en
 * UsuarioRol (identidad) sobre por que productoId es un @Id String plano
 * y no una asociacion @ManyToOne dentro de la clave compuesta -- Hibernate
 * no propaga @JdbcTypeCode(CHAR) ahi (DT-041/2.8 bis). "servicio" no
 * necesita el mismo tratamiento: es un enum-texto, no un ULID con FK.
 */
@Entity
@Table(name = "producto_servicio")
@IdClass(ProductoServicioId.class)
public class ProductoServicio {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "producto_id", length = 26)
    private String productoId;

    @Id
    @Column(length = 10)
    private String servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", insertable = false, updatable = false)
    private Producto producto;

    @Column(nullable = false)
    private boolean activo = true;

    protected ProductoServicio() {
    }

    public ProductoServicio(Producto producto, String servicio) {
        this.producto = producto;
        this.productoId = producto.getId();
        this.servicio = servicio;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getServicio() {
        return servicio;
    }

    public boolean isActivo() {
        return activo;
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProductoServicioRepositorio.java
package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoServicioRepositorio extends JpaRepository<ProductoServicio, ProductoServicioId> {
    List<ProductoServicio> findByProductoId(String productoId);

    void deleteByProductoId(String productoId);
}
```

- [ ] **Step 2: Write the failing test**

```java
// rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/AsignarServiciosProductoTest.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AsignarServiciosProductoTest {

    @Autowired
    private AsignarServiciosProducto asignarServicios;
    @Autowired
    private ProductoServicioRepositorio productoServicios;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private ProductoRepositorio productos;

    private Producto productoFijo() {
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), "Platos fijos test", "producto_fijo", null, 1, null));
        CategoriaProducto hoja = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), raiz, "Bebidas calientes test", 1, null));
        UnidadMedida u = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "undtest", "Unidad test",
                "unidad"));
        return productos.save(new Producto(Identificador.nuevo().valor(), "TE-001", "Te", hoja, u,
                true, false, false, 500L, false, null));
    }

    private Producto productoComponenteMenu() {
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), "Segundos test", "componente_menu", "segundo", 1, null));
        CategoriaProducto hoja = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), raiz, "Pollo test", 1, null));
        UnidadMedida u = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "undtest2", "Unidad test 2",
                "unidad"));
        return productos.save(new Producto(Identificador.nuevo().valor(), "PO-001", "Pollo a la plancha", hoja, u,
                true, false, false, 1500L, false, null));
    }

    @Test
    void asigna_desayuno_y_cena_a_un_te() {
        Producto te = productoFijo();

        Resultado<Void, ErrorCatalogo> resultado =
                asignarServicios.ejecutar(te.getId(), Set.of("desayuno", "cena"), "actor");

        assertThat(resultado.esExito()).isTrue();
        assertThat(productoServicios.findByProductoId(te.getId())).hasSize(2);
    }

    @Test
    void rechaza_asignar_servicios_a_un_componente_de_menu() {
        Producto pollo = productoComponenteMenu();

        Resultado<Void, ErrorCatalogo> resultado =
                asignarServicios.ejecutar(pollo.getId(), Set.of("almuerzo"), "actor");

        assertThat(resultado.esExito()).isFalse();
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `../mvnw.cmd test -Dtest=AsignarServiciosProductoTest`
Expected: FAIL — `AsignarServiciosProducto` doesn't exist yet.

- [ ] **Step 4: Write the use case**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/AsignarServiciosProducto.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.ProductoServicio;
import pe.rikos.operacion.catalogo.infraestructura.ProductoServicioRepositorio;

import java.util.Set;

/** 4.1 bis: solo aplica a productos cuya categoria tiene
 * naturaleza = producto_fijo -- los componentes de menu no la necesitan,
 * su servicio es siempre el almuerzo y lo fija la programacion semanal. */
@Service
public class AsignarServiciosProducto {

    private final ProductoRepositorio productos;
    private final ProductoServicioRepositorio productoServicios;

    public AsignarServiciosProducto(ProductoRepositorio productos,
                                     ProductoServicioRepositorio productoServicios) {
        this.productos = productos;
        this.productoServicios = productoServicios;
    }

    @Transactional
    public Resultado<Void, ErrorCatalogo> ejecutar(String productoId, Set<String> servicios, String actorId) {
        Producto producto = productos.findById(productoId).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }
        if (!"producto_fijo".equals(producto.getCategoria().getNaturaleza())) {
            return Resultado.fracaso(ErrorCatalogo.NATURALEZA_INVALIDA);
        }

        // Reemplaza el conjunto completo. El delete debe hacer flush antes
        // del insert -- Hibernate reordena DELETE despues de INSERT en la
        // misma transaccion (bug conocido, ver GuardarHorarioSemanal /
        // AsignarPermisosARol de sprints anteriores).
        productoServicios.deleteByProductoId(productoId);
        productoServicios.flush();

        for (String servicio : servicios) {
            productoServicios.save(new ProductoServicio(producto, servicio));
        }

        return Resultado.exito(null);
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `../mvnw.cmd test -Dtest=AsignarServiciosProductoTest`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProductoServicioId.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProductoServicio.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProductoServicioRepositorio.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/AsignarServiciosProducto.java servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/AsignarServiciosProductoTest.java
git commit -m "feat(catalogo): producto_servicio, con la regla de flush conocida"
```

---

### Task 8: `PresentacionCompra` — entity, repository, use cases

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/PresentacionCompra.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/PresentacionCompraRepositorio.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearPresentacion.java`
- Test: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearPresentacionTest.java`

**Interfaces:**
- Consumes: `Producto`/`UnidadMedida` (Tasks 3–4), `ErrorCatalogo` (Task 5).
- Produces: `PresentacionCompra` (getters, `desactivar()`), `CrearPresentacion.ejecutar(String productoId, String nombre, String unidadId, BigDecimal factor, boolean esPredeterminada, String actorId)`.

- [ ] **Step 1: Write the entity and repository**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/PresentacionCompra.java
package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** 4.4: el saco de 50 kg, la caja de 24 unidades. Una sola predeterminada
 * activa por producto -- lo garantiza el indice unico parcial de la BD
 * (V14), esta clase solo expone el estado. */
@Entity
@Table(name = "presentacion_compra")
public class PresentacionCompra {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, length = 80)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "unidad_id", nullable = false)
    private UnidadMedida unidad;

    @Column(name = "factor_a_unidad_base", nullable = false, precision = 14, scale = 6)
    private BigDecimal factorAUnidadBase;

    @Column(name = "es_predeterminada", nullable = false)
    private boolean esPredeterminada;

    @Column(nullable = false)
    private boolean activo = true;

    protected PresentacionCompra() {
    }

    public PresentacionCompra(String id, Producto producto, String nombre, UnidadMedida unidad,
                               BigDecimal factorAUnidadBase, boolean esPredeterminada) {
        this.id = id;
        this.producto = producto;
        this.nombre = nombre;
        this.unidad = unidad;
        this.factorAUnidadBase = factorAUnidadBase;
        this.esPredeterminada = esPredeterminada;
    }

    public void desactivar() {
        this.activo = false;
        this.esPredeterminada = false;
    }

    public String getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getNombre() {
        return nombre;
    }

    public UnidadMedida getUnidad() {
        return unidad;
    }

    public BigDecimal getFactorAUnidadBase() {
        return factorAUnidadBase;
    }

    public boolean isEsPredeterminada() {
        return esPredeterminada;
    }

    public boolean isActivo() {
        return activo;
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/PresentacionCompraRepositorio.java
package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PresentacionCompraRepositorio extends JpaRepository<PresentacionCompra, String> {
    List<PresentacionCompra> findByProductoId(String productoId);

    boolean existsByProductoIdAndEsPredeterminadaTrueAndActivoTrue(String productoId);
}
```

- [ ] **Step 2: Write the failing test**

```java
// rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearPresentacionTest.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CrearPresentacionTest {

    @Autowired
    private CrearPresentacion crearPresentacion;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private ProductoRepositorio productos;

    private Producto productoInsumo() {
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), "Insumos test presentacion", "insumo", null, 1, null));
        CategoriaProducto hoja = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), raiz, "Abarrotes test presentacion", 1, null));
        UnidadMedida kg = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "kgpres", "Kilogramo pres",
                "masa"));
        return productos.save(new Producto(Identificador.nuevo().valor(), "ARR-001", "Arroz", hoja, kg,
                false, true, false, null, true, null));
    }

    @Test
    void el_factor_se_calcula_correctamente_1_saco_50_kg() {
        Producto arroz = productoInsumo();
        UnidadMedida saco = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "sacopres", "Saco pres",
                "masa"));

        Resultado<PresentacionCompra, ErrorCatalogo> resultado = crearPresentacion.ejecutar(
                arroz.getId(), "Saco 50 kg", saco.getId(), new BigDecimal("50"), true, "actor");

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().getFactorAUnidadBase()).isEqualByComparingTo("50");
    }

    @Test
    void rechaza_un_factor_cero_o_negativo() {
        Producto arroz = productoInsumo();
        UnidadMedida saco = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "sacopres2",
                "Saco pres 2", "masa"));

        Resultado<PresentacionCompra, ErrorCatalogo> resultado = crearPresentacion.ejecutar(
                arroz.getId(), "Saco invalido", saco.getId(), new BigDecimal("0"), true, "actor");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.FACTOR_INVALIDO);
    }

    @Test
    void rechaza_una_segunda_predeterminada_activa_para_el_mismo_producto() {
        Producto arroz = productoInsumo();
        UnidadMedida saco = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "sacopres3",
                "Saco pres 3", "masa"));
        UnidadMedida bolsa = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "bolsapres",
                "Bolsa pres", "masa"));
        crearPresentacion.ejecutar(arroz.getId(), "Saco 50 kg", saco.getId(), new BigDecimal("50"), true, "actor");

        Resultado<PresentacionCompra, ErrorCatalogo> resultado = crearPresentacion.ejecutar(
                arroz.getId(), "Bolsa 5 kg", bolsa.getId(), new BigDecimal("5"), true, "actor");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.PREDETERMINADA_DUPLICADA);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `../mvnw.cmd test -Dtest=CrearPresentacionTest`
Expected: FAIL — `CrearPresentacion` doesn't exist yet.

- [ ] **Step 4: Write the use case**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearPresentacion.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;

import java.math.BigDecimal;

@Service
public class CrearPresentacion {

    private final PresentacionCompraRepositorio presentaciones;
    private final ProductoRepositorio productos;
    private final UnidadMedidaRepositorio unidades;

    public CrearPresentacion(PresentacionCompraRepositorio presentaciones, ProductoRepositorio productos,
                              UnidadMedidaRepositorio unidades) {
        this.presentaciones = presentaciones;
        this.productos = productos;
        this.unidades = unidades;
    }

    @Transactional
    public Resultado<PresentacionCompra, ErrorCatalogo> ejecutar(String productoId, String nombre,
            String unidadId, BigDecimal factor, boolean esPredeterminada, String actorId) {

        if (factor == null || factor.compareTo(BigDecimal.ZERO) <= 0) {
            return Resultado.fracaso(ErrorCatalogo.FACTOR_INVALIDO);
        }

        Producto producto = productos.findById(productoId).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }

        UnidadMedida unidad = unidades.findById(unidadId).orElse(null);
        if (unidad == null) {
            return Resultado.fracaso(ErrorCatalogo.UNIDAD_OBLIGATORIA);
        }

        if (esPredeterminada
                && presentaciones.existsByProductoIdAndEsPredeterminadaTrueAndActivoTrue(productoId)) {
            return Resultado.fracaso(ErrorCatalogo.PREDETERMINADA_DUPLICADA);
        }

        PresentacionCompra presentacion = new PresentacionCompra(Identificador.nuevo().valor(), producto, nombre,
                unidad, factor, esPredeterminada);
        return Resultado.exito(presentaciones.save(presentacion));
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `../mvnw.cmd test -Dtest=CrearPresentacionTest`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/PresentacionCompra.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/PresentacionCompraRepositorio.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearPresentacion.java servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearPresentacionTest.java
git commit -m "feat(catalogo): presentaciones de compra con predeterminada unica"
```

---

### Task 9: `ConversionUnidad` — entity, repository, use case

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ConversionUnidad.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ConversionUnidadRepositorio.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearConversion.java`
- Test: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearConversionTest.java`

**Interfaces:**
- Consumes: `Producto`/`UnidadMedida` (Tasks 3–4), `ErrorCatalogo` (Task 5).
- Produces: `ConversionUnidad` (getters), `CrearConversion.ejecutar(String productoId, String unidadOrigenId, String unidadDestinoId, BigDecimal factor, String actorId)`.

- [ ] **Step 1: Write the entity and repository**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ConversionUnidad.java
package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** 4.5: por producto, nunca global -- un litro de aceite y un litro de
 * chicha no pesan lo mismo. */
@Entity
@Table(name = "conversion_unidad")
public class ConversionUnidad {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "unidad_origen_id", nullable = false)
    private UnidadMedida unidadOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "unidad_destino_id", nullable = false)
    private UnidadMedida unidadDestino;

    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal factor;

    protected ConversionUnidad() {
    }

    public ConversionUnidad(String id, Producto producto, UnidadMedida unidadOrigen, UnidadMedida unidadDestino,
                             BigDecimal factor) {
        this.id = id;
        this.producto = producto;
        this.unidadOrigen = unidadOrigen;
        this.unidadDestino = unidadDestino;
        this.factor = factor;
    }

    public String getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public UnidadMedida getUnidadOrigen() {
        return unidadOrigen;
    }

    public UnidadMedida getUnidadDestino() {
        return unidadDestino;
    }

    public BigDecimal getFactor() {
        return factor;
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ConversionUnidadRepositorio.java
package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConversionUnidadRepositorio extends JpaRepository<ConversionUnidad, String> {
    List<ConversionUnidad> findByProductoId(String productoId);

    boolean existsByProductoIdAndUnidadOrigenIdAndUnidadDestinoId(String productoId, String unidadOrigenId,
            String unidadDestinoId);
}
```

- [ ] **Step 2: Write the failing test**

```java
// rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearConversionTest.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CrearConversionTest {

    @Autowired
    private CrearConversion crearConversion;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private ProductoRepositorio productos;

    private Producto productoInsumo(String codigo) {
        CategoriaProducto raiz = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), "Insumos test conversion " + codigo, "insumo", null, 1, null));
        CategoriaProducto hoja = categorias.save(new CategoriaProducto(
                Identificador.nuevo().valor(), raiz, "Abarrotes test conversion " + codigo, 1, null));
        UnidadMedida l = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "l" + codigo, "Litro " + codigo,
                "volumen"));
        return productos.save(new Producto(Identificador.nuevo().valor(), codigo, "Producto " + codigo, hoja, l,
                false, true, false, null, true, null));
    }

    @Test
    void aplica_el_factor_correctamente() {
        Producto aceite = productoInsumo("ACE-001");
        UnidadMedida litro = aceite.getUnidadBase();
        UnidadMedida kg = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "kgconv", "Kilogramo conv",
                "masa"));

        Resultado<ConversionUnidad, ErrorCatalogo> resultado =
                crearConversion.ejecutar(aceite.getId(), litro.getId(), kg.getId(), new BigDecimal("0.92"), "actor");

        assertThat(resultado.esExito()).isTrue();
        BigDecimal resultadoConversion = new BigDecimal("2").multiply(resultado.valorOLanza().getFactor());
        assertThat(resultadoConversion).isEqualByComparingTo("1.84");
    }

    @Test
    void rechaza_un_par_de_unidades_repetido_para_el_mismo_producto() {
        Producto aceite = productoInsumo("ACE-002");
        UnidadMedida litro = aceite.getUnidadBase();
        UnidadMedida kg = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "kgconv2",
                "Kilogramo conv 2", "masa"));
        crearConversion.ejecutar(aceite.getId(), litro.getId(), kg.getId(), new BigDecimal("0.92"), "actor");

        Resultado<ConversionUnidad, ErrorCatalogo> resultado =
                crearConversion.ejecutar(aceite.getId(), litro.getId(), kg.getId(), new BigDecimal("0.95"), "actor");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.PAR_UNIDADES_DUPLICADO);
    }

    @Test
    void rechaza_factor_cero_o_negativo() {
        Producto aceite = productoInsumo("ACE-003");
        UnidadMedida litro = aceite.getUnidadBase();
        UnidadMedida kg = unidades.save(new UnidadMedida(Identificador.nuevo().valor(), "kgconv3",
                "Kilogramo conv 3", "masa"));

        Resultado<ConversionUnidad, ErrorCatalogo> resultado =
                crearConversion.ejecutar(aceite.getId(), litro.getId(), kg.getId(), new BigDecimal("-1"), "actor");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.FACTOR_INVALIDO);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `../mvnw.cmd test -Dtest=CrearConversionTest`
Expected: FAIL — `CrearConversion` doesn't exist yet.

- [ ] **Step 4: Write the use case**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearConversion.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;

import java.math.BigDecimal;

@Service
public class CrearConversion {

    private final ConversionUnidadRepositorio conversiones;
    private final ProductoRepositorio productos;
    private final UnidadMedidaRepositorio unidades;

    public CrearConversion(ConversionUnidadRepositorio conversiones, ProductoRepositorio productos,
                            UnidadMedidaRepositorio unidades) {
        this.conversiones = conversiones;
        this.productos = productos;
        this.unidades = unidades;
    }

    @Transactional
    public Resultado<ConversionUnidad, ErrorCatalogo> ejecutar(String productoId, String unidadOrigenId,
            String unidadDestinoId, BigDecimal factor, String actorId) {

        if (factor == null || factor.compareTo(BigDecimal.ZERO) <= 0) {
            return Resultado.fracaso(ErrorCatalogo.FACTOR_INVALIDO);
        }

        Producto producto = productos.findById(productoId).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }

        UnidadMedida origen = unidades.findById(unidadOrigenId).orElse(null);
        UnidadMedida destino = unidades.findById(unidadDestinoId).orElse(null);
        if (origen == null || destino == null) {
            return Resultado.fracaso(ErrorCatalogo.UNIDAD_OBLIGATORIA);
        }

        if (conversiones.existsByProductoIdAndUnidadOrigenIdAndUnidadDestinoId(productoId, unidadOrigenId,
                unidadDestinoId)) {
            return Resultado.fracaso(ErrorCatalogo.PAR_UNIDADES_DUPLICADO);
        }

        ConversionUnidad conversion =
                new ConversionUnidad(Identificador.nuevo().valor(), producto, origen, destino, factor);
        return Resultado.exito(conversiones.save(conversion));
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `../mvnw.cmd test -Dtest=CrearConversionTest`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ConversionUnidad.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ConversionUnidadRepositorio.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearConversion.java servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearConversionTest.java
git commit -m "feat(catalogo): conversiones de unidad por producto"
```

---

### Task 10: `Proveedor` — entity, repository, use cases

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/Proveedor.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProveedorRepositorio.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearProveedor.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/ActualizarProveedor.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/DesactivarProveedor.java`
- Test: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearProveedorTest.java`

**Interfaces:**
- Consumes: `ErrorCatalogo` (Task 5).
- Produces: `Proveedor` (getters, `actualizar(DatosActualizacion, String)`, `desactivar(String)`), `CrearProveedor.ejecutar(String ruc, String razonSocial, String nombreComercial, String telefono, String correo, String direccion, String contacto, String actorId)`.

- [ ] **Step 1: Write the entity and repository**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/Proveedor.java
package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** 4.6: RUC opcional -- el mercado de abastos existe, una compra a un
 * proveedor informal es real. No incluye ordenes de compra ni cuentas por
 * pagar (fuera de alcance, US-011). */
@Entity
@Table(name = "proveedor")
public class Proveedor {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(length = 11)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 120)
    private String nombreComercial;

    @Column(length = 20)
    private String telefono;

    @Column(length = 160)
    private String correo;

    @Column(length = 250)
    private String direccion;

    @Column(length = 120)
    private String contacto;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creado_por", length = 26)
    private String creadoPor;

    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "actualizado_por", length = 26)
    private String actualizadoPor;

    protected Proveedor() {
    }

    public Proveedor(String id, String ruc, String razonSocial, String nombreComercial, String telefono,
                      String correo, String direccion, String contacto, String creadoPor) {
        this.id = id;
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.nombreComercial = nombreComercial;
        this.telefono = telefono;
        this.correo = correo;
        this.direccion = direccion;
        this.contacto = contacto;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public record DatosActualizacion(String ruc, String razonSocial, String nombreComercial, String telefono,
                                      String correo, String direccion, String contacto) {
    }

    public void actualizar(DatosActualizacion datos, String actualizadoPor) {
        this.ruc = datos.ruc();
        this.razonSocial = datos.razonSocial();
        this.nombreComercial = datos.nombreComercial();
        this.telefono = datos.telefono();
        this.correo = datos.correo();
        this.direccion = datos.direccion();
        this.contacto = datos.contacto();
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public void desactivar(String actualizadoPor) {
        this.activo = false;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public String getId() {
        return id;
    }

    public String getRuc() {
        return ruc;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getContacto() {
        return contacto;
    }

    public boolean isActivo() {
        return activo;
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProveedorRepositorio.java
package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepositorio extends JpaRepository<Proveedor, String> {
    boolean existsByRuc(String ruc);
}
```

- [ ] **Step 2: Write the failing test**

```java
// rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearProveedorTest.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CrearProveedorTest {

    @Autowired
    private CrearProveedor crearProveedor;

    @Test
    void crea_un_proveedor_sin_ruc_mercado_de_abastos() {
        Resultado<Proveedor, ErrorCatalogo> resultado = crearProveedor.ejecutar(
                null, "Don Pepe Abarrotes", null, "987654321", null, "Mercado Central puesto 12", "Pepe", "actor");

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().getRuc()).isNull();
    }

    @Test
    void rechaza_un_ruc_que_no_tiene_exactamente_11_digitos() {
        Resultado<Proveedor, ErrorCatalogo> resultado = crearProveedor.ejecutar(
                "12345", "Proveedor Mal Ruc SAC", null, null, null, null, null, "actor");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.RUC_INVALIDO);
    }

    @Test
    void rechaza_ruc_duplicado_cuando_no_es_nulo() {
        crearProveedor.ejecutar("20123456780", "Proveedor Uno SAC", null, null, null, null, null, "actor");

        Resultado<Proveedor, ErrorCatalogo> resultado =
                crearProveedor.ejecutar("20123456780", "Proveedor Dos SAC", null, null, null, null, null, "actor");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error())
                .isEqualTo(ErrorCatalogo.RUC_DUPLICADO);
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `../mvnw.cmd test -Dtest=CrearProveedorTest`
Expected: FAIL — `CrearProveedor` doesn't exist yet.

- [ ] **Step 4: Write the use cases**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearProveedor.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;

import java.util.regex.Pattern;

@Service
public class CrearProveedor {

    private static final Pattern FORMATO_RUC = Pattern.compile("^\\d{11}$");

    private final ProveedorRepositorio proveedores;

    public CrearProveedor(ProveedorRepositorio proveedores) {
        this.proveedores = proveedores;
    }

    @Transactional
    public Resultado<Proveedor, ErrorCatalogo> ejecutar(String ruc, String razonSocial, String nombreComercial,
            String telefono, String correo, String direccion, String contacto, String actorId) {

        if (ruc != null && !FORMATO_RUC.matcher(ruc).matches()) {
            return Resultado.fracaso(ErrorCatalogo.RUC_INVALIDO);
        }
        if (ruc != null && proveedores.existsByRuc(ruc)) {
            return Resultado.fracaso(ErrorCatalogo.RUC_DUPLICADO);
        }

        Proveedor proveedor = new Proveedor(Identificador.nuevo().valor(), ruc, razonSocial, nombreComercial,
                telefono, correo, direccion, contacto, actorId);
        return Resultado.exito(proveedores.save(proveedor));
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/ActualizarProveedor.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;

import java.util.regex.Pattern;

@Service
public class ActualizarProveedor {

    private static final Pattern FORMATO_RUC = Pattern.compile("^\\d{11}$");

    private final ProveedorRepositorio proveedores;

    public ActualizarProveedor(ProveedorRepositorio proveedores) {
        this.proveedores = proveedores;
    }

    @Transactional
    public Resultado<Proveedor, ErrorCatalogo> ejecutar(String id, Proveedor.DatosActualizacion datos,
            String actorId) {
        if (datos.ruc() != null && !FORMATO_RUC.matcher(datos.ruc()).matches()) {
            return Resultado.fracaso(ErrorCatalogo.RUC_INVALIDO);
        }
        return proveedores.findById(id)
                .map(proveedor -> {
                    proveedor.actualizar(datos, actorId);
                    return Resultado.<Proveedor, ErrorCatalogo>exito(proveedores.save(proveedor));
                })
                .orElse(Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO));
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/DesactivarProveedor.java
package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;

/** Desactivar impide usarlo en compras nuevas y conserva las anteriores
 * (US-011) -- nunca se borra. */
@Service
public class DesactivarProveedor {

    private final ProveedorRepositorio proveedores;

    public DesactivarProveedor(ProveedorRepositorio proveedores) {
        this.proveedores = proveedores;
    }

    @Transactional
    public Resultado<Proveedor, ErrorCatalogo> ejecutar(String id, String actorId) {
        return proveedores.findById(id)
                .map(proveedor -> {
                    proveedor.desactivar(actorId);
                    return Resultado.<Proveedor, ErrorCatalogo>exito(proveedores.save(proveedor));
                })
                .orElse(Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO));
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `../mvnw.cmd test -Dtest=CrearProveedorTest`
Expected: PASS (3 tests).

- [ ] **Step 6: Commit**

```bash
git add servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/Proveedor.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/infraestructura/ProveedorRepositorio.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/CrearProveedor.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/ActualizarProveedor.java servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/aplicacion/DesactivarProveedor.java servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/aplicacion/CrearProveedorTest.java
git commit -m "feat(catalogo): proveedores con RUC opcional"
```

---

### Task 11: REST controllers for the whole module

**Files:**
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/api/CategoriaController.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/api/UnidadController.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/api/ProductoController.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/api/PresentacionController.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/api/ConversionController.java`
- Create: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/api/ProveedorController.java`

**Interfaces:**
- Consumes: every use case from Tasks 5–10.
- Produces: the actual routes every later frontend task calls: `GET/POST /categorias`, `PUT /categorias/{id}`, `GET /unidades`, `POST /unidades`, `GET /productos`, `POST /productos`, `PUT /productos/{id}`, `POST /productos/{id}/desactivar`, `POST /productos/{id}/activar`, `PUT /productos/{id}/costo-manual`, `PUT /productos/{id}/servicios`, `GET /productos/{id}/presentaciones`, `POST /presentaciones`, `GET /productos/{id}/conversiones`, `POST /conversiones`, `GET /proveedores`, `POST /proveedores`, `PUT /proveedores/{id}`, `POST /proveedores/{id}/desactivar`, `POST /proveedores/{id}/activar` — all under the service's `/api` context-path (so `/api/productos`, etc.), reachable through the gateway's existing `/api/**` route with no gateway changes needed.

- [ ] **Step 1: Write `CategoriaController`**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/api/CategoriaController.java
package pe.rikos.operacion.catalogo.api;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.ActualizarCategoria;
import pe.rikos.operacion.catalogo.aplicacion.CrearCategoria;
import pe.rikos.operacion.catalogo.aplicacion.ErrorCatalogo;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CrearCategoria crearCategoria;
    private final ActualizarCategoria actualizarCategoria;
    private final CategoriaProductoRepositorio categorias;

    public CategoriaController(CrearCategoria crearCategoria, ActualizarCategoria actualizarCategoria,
                                CategoriaProductoRepositorio categorias) {
        this.crearCategoria = crearCategoria;
        this.actualizarCategoria = actualizarCategoria;
        this.categorias = categorias;
    }

    public record CategoriaRequest(@NotBlank String nombre, String naturaleza, String componenteMenu,
                                    String categoriaPadreId, int orden) {
    }

    public record CategoriaRespuesta(String id, String nombre, String naturaleza, String componenteMenu, int nivel,
                                      int orden, boolean esHoja, boolean activo, String categoriaPadreId) {
        static CategoriaRespuesta de(CategoriaProducto c, boolean esHoja) {
            return new CategoriaRespuesta(c.getId(), c.getNombre(), c.getNaturaleza(), c.getComponenteMenu(),
                    c.getNivel(), c.getOrden(), esHoja, c.isActivo(),
                    c.getCategoriaPadre() != null ? c.getCategoriaPadre().getId() : null);
        }
    }

    @GetMapping
    public List<CategoriaRespuesta> listar() {
        return categorias.findAll().stream()
                .map(c -> CategoriaRespuesta.de(c, !categorias.existsByCategoriaPadreIdAndActivoTrue(c.getId())))
                .toList();
    }

    @PostMapping
    public ResponseEntity<?> crear(@org.springframework.validation.annotation.Validated @RequestBody CategoriaRequest cuerpo,
                                    @AuthenticationPrincipal String actorId) {
        Resultado<CategoriaProducto, ErrorCatalogo> resultado = crearCategoria.ejecutar(cuerpo.nombre(),
                cuerpo.naturaleza(), cuerpo.componenteMenu(), cuerpo.categoriaPadreId(), cuerpo.orden(), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(CategoriaRespuesta.de(resultado.valorOLanza(), true))
                : errorDe(resultado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody CategoriaRequest cuerpo,
                                         @AuthenticationPrincipal String actorId) {
        Resultado<CategoriaProducto, ErrorCatalogo> resultado =
                actualizarCategoria.ejecutar(id, cuerpo.nombre(), cuerpo.orden(), actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(CategoriaRespuesta.de(resultado.valorOLanza(),
                        !categorias.existsByCategoriaPadreIdAndActivoTrue(id)))
                : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorCatalogo> resultado) {
        ErrorCatalogo error = ((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error();
        HttpStatus estado = switch (error) {
            case NO_ENCONTRADO, CATEGORIA_PADRE_NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
            case NATURALEZA_INVALIDA -> HttpStatus.UNPROCESSABLE_ENTITY;
            case NOMBRE_DUPLICADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}
```

- [ ] **Step 2: Write `UnidadController`**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/api/UnidadController.java
package pe.rikos.operacion.catalogo.api;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/unidades")
public class UnidadController {

    private final UnidadMedidaRepositorio unidades;

    public UnidadController(UnidadMedidaRepositorio unidades) {
        this.unidades = unidades;
    }

    public record UnidadRequest(@NotBlank String codigo, @NotBlank String nombre, @NotBlank String tipo) {
    }

    public record UnidadRespuesta(String id, String codigo, String nombre, String tipo, boolean activo) {
        static UnidadRespuesta de(UnidadMedida u) {
            return new UnidadRespuesta(u.getId(), u.getCodigo(), u.getNombre(), u.getTipo(), u.isActivo());
        }
    }

    @GetMapping
    public List<UnidadRespuesta> listar() {
        return unidades.findAll().stream().map(UnidadRespuesta::de).toList();
    }

    @PostMapping
    public ResponseEntity<?> crear(@org.springframework.validation.annotation.Validated @RequestBody UnidadRequest cuerpo) {
        if (unidades.existsByCodigo(cuerpo.codigo())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "CODIGO_DUPLICADO"));
        }
        UnidadMedida creada = unidades.save(
                new UnidadMedida(Identificador.nuevo().valor(), cuerpo.codigo(), cuerpo.nombre(), cuerpo.tipo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UnidadRespuesta.de(creada));
    }
}
```

- [ ] **Step 3: Write `ProductoController`**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/api/ProductoController.java
package pe.rikos.operacion.catalogo.api;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.*;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.ProductoServicioRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class ProductoController {

    private final CrearProducto crearProducto;
    private final ActualizarProducto actualizarProducto;
    private final DesactivarProducto desactivarProducto;
    private final RegistrarCostoManual registrarCostoManual;
    private final AsignarServiciosProducto asignarServiciosProducto;
    private final ProductoRepositorio productos;
    private final CategoriaProductoRepositorio categorias;
    private final UnidadMedidaRepositorio unidades;
    private final ProductoServicioRepositorio productoServicios;

    public ProductoController(CrearProducto crearProducto, ActualizarProducto actualizarProducto,
            DesactivarProducto desactivarProducto, RegistrarCostoManual registrarCostoManual,
            AsignarServiciosProducto asignarServiciosProducto, ProductoRepositorio productos,
            CategoriaProductoRepositorio categorias, UnidadMedidaRepositorio unidades,
            ProductoServicioRepositorio productoServicios) {
        this.crearProducto = crearProducto;
        this.actualizarProducto = actualizarProducto;
        this.desactivarProducto = desactivarProducto;
        this.registrarCostoManual = registrarCostoManual;
        this.asignarServiciosProducto = asignarServiciosProducto;
        this.productos = productos;
        this.categorias = categorias;
        this.unidades = unidades;
        this.productoServicios = productoServicios;
    }

    public record ProductoRequest(@NotBlank String codigo, @NotBlank String nombre, @NotBlank String categoriaId,
                                   @NotBlank String unidadBaseId, boolean esVendible, boolean esInsumo,
                                   boolean esSubreceta, Long precioVenta, boolean controlaStock) {
    }

    public record CostoManualRequest(BigDecimal costo) {
    }

    public record ServiciosRequest(Set<String> servicios) {
    }

    public record ProductoRespuesta(String id, String codigo, String nombre, String categoriaId,
                                     String categoriaNombre, String unidadBaseId, boolean esVendible,
                                     boolean esInsumo, boolean esSubreceta, Long precioVenta, BigDecimal costo,
                                     String origenCosto, boolean controlaStock, boolean activo,
                                     List<String> servicios) {
        static ProductoRespuesta de(Producto p, List<String> servicios) {
            return new ProductoRespuesta(p.getId(), p.getCodigo(), p.getNombre(), p.getCategoria().getId(),
                    p.getCategoria().getNombre(), p.getUnidadBase().getId(), p.isEsVendible(), p.isEsInsumo(),
                    p.isEsSubreceta(), p.getPrecioVenta(), p.getCostoUltimaCompra(), p.getOrigenCosto(),
                    p.isControlaStock(), p.isActivo(), servicios);
        }
    }

    @GetMapping("/productos")
    public List<ProductoRespuesta> listar() {
        return productos.findAll().stream()
                .map(p -> ProductoRespuesta.de(p, productoServicios.findByProductoId(p.getId()).stream()
                        .map(ps -> ps.getServicio()).toList()))
                .toList();
    }

    @PostMapping("/productos")
    public ResponseEntity<?> crear(@org.springframework.validation.annotation.Validated @RequestBody ProductoRequest cuerpo,
                                    @AuthenticationPrincipal String actorId) {
        Resultado<Producto, ErrorCatalogo> resultado = crearProducto.ejecutar(cuerpo.codigo(), cuerpo.nombre(),
                cuerpo.categoriaId(), cuerpo.unidadBaseId(), cuerpo.esVendible(), cuerpo.esInsumo(),
                cuerpo.esSubreceta(), cuerpo.precioVenta(), cuerpo.controlaStock(), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(ProductoRespuesta.de(resultado.valorOLanza(), List.of()))
                : errorDe(resultado);
    }

    @PutMapping("/productos/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody ProductoRequest cuerpo,
                                         @AuthenticationPrincipal String actorId) {
        CategoriaProducto categoria = categorias.findById(cuerpo.categoriaId()).orElse(null);
        UnidadMedida unidad = unidades.findById(cuerpo.unidadBaseId()).orElse(null);
        if (categoria == null || unidad == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "NO_ENCONTRADO"));
        }
        Producto.DatosActualizacion datos = new Producto.DatosActualizacion(cuerpo.nombre(), categoria, unidad,
                cuerpo.esVendible(), cuerpo.esInsumo(), cuerpo.esSubreceta(), cuerpo.precioVenta(),
                cuerpo.controlaStock());
        Resultado<Producto, ErrorCatalogo> resultado = actualizarProducto.ejecutar(id, datos, actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(ProductoRespuesta.de(resultado.valorOLanza(),
                        productoServicios.findByProductoId(id).stream().map(ps -> ps.getServicio()).toList()))
                : errorDe(resultado);
    }

    @PostMapping("/productos/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        Resultado<Producto, ErrorCatalogo> resultado = desactivarProducto.ejecutar(id, actorId);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    @PutMapping("/productos/{id}/costo-manual")
    public ResponseEntity<?> registrarCosto(@PathVariable String id, @RequestBody CostoManualRequest cuerpo,
                                             @AuthenticationPrincipal String actorId) {
        Resultado<Producto, ErrorCatalogo> resultado = registrarCostoManual.ejecutar(id, cuerpo.costo(), actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(ProductoRespuesta.de(resultado.valorOLanza(),
                        productoServicios.findByProductoId(id).stream().map(ps -> ps.getServicio()).toList()))
                : errorDe(resultado);
    }

    @PutMapping("/productos/{id}/servicios")
    public ResponseEntity<?> asignarServicios(@PathVariable String id, @RequestBody ServiciosRequest cuerpo,
                                               @AuthenticationPrincipal String actorId) {
        Resultado<Void, ErrorCatalogo> resultado =
                asignarServiciosProducto.ejecutar(id, cuerpo.servicios(), actorId);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorCatalogo> resultado) {
        ErrorCatalogo error = ((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error();
        HttpStatus estado = switch (error) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case CODIGO_DUPLICADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}
```

- [ ] **Step 4: Write `PresentacionController` and `ConversionController`**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/api/PresentacionController.java
package pe.rikos.operacion.catalogo.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.CrearPresentacion;
import pe.rikos.operacion.catalogo.aplicacion.ErrorCatalogo;
import pe.rikos.operacion.catalogo.infraestructura.PresentacionCompra;
import pe.rikos.operacion.catalogo.infraestructura.PresentacionCompraRepositorio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
public class PresentacionController {

    private final CrearPresentacion crearPresentacion;
    private final PresentacionCompraRepositorio presentaciones;

    public PresentacionController(CrearPresentacion crearPresentacion,
                                   PresentacionCompraRepositorio presentaciones) {
        this.crearPresentacion = crearPresentacion;
        this.presentaciones = presentaciones;
    }

    public record PresentacionRequest(String productoId, String nombre, String unidadId, BigDecimal factor,
                                       boolean esPredeterminada) {
    }

    public record PresentacionRespuesta(String id, String nombre, String unidadId, BigDecimal factor,
                                         boolean esPredeterminada, boolean activo) {
        static PresentacionRespuesta de(PresentacionCompra p) {
            return new PresentacionRespuesta(p.getId(), p.getNombre(), p.getUnidad().getId(),
                    p.getFactorAUnidadBase(), p.isEsPredeterminada(), p.isActivo());
        }
    }

    @GetMapping("/productos/{productoId}/presentaciones")
    public List<PresentacionRespuesta> listar(@PathVariable String productoId) {
        return presentaciones.findByProductoId(productoId).stream().map(PresentacionRespuesta::de).toList();
    }

    @PostMapping("/presentaciones")
    public ResponseEntity<?> crear(@RequestBody PresentacionRequest cuerpo,
                                    @AuthenticationPrincipal String actorId) {
        Resultado<PresentacionCompra, ErrorCatalogo> resultado = crearPresentacion.ejecutar(cuerpo.productoId(),
                cuerpo.nombre(), cuerpo.unidadId(), cuerpo.factor(), cuerpo.esPredeterminada(), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(PresentacionRespuesta.de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorCatalogo> resultado) {
        ErrorCatalogo error = ((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error();
        HttpStatus estado = switch (error) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case PREDETERMINADA_DUPLICADA -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}
```

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/api/ConversionController.java
package pe.rikos.operacion.catalogo.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.CrearConversion;
import pe.rikos.operacion.catalogo.aplicacion.ErrorCatalogo;
import pe.rikos.operacion.catalogo.infraestructura.ConversionUnidad;
import pe.rikos.operacion.catalogo.infraestructura.ConversionUnidadRepositorio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
public class ConversionController {

    private final CrearConversion crearConversion;
    private final ConversionUnidadRepositorio conversiones;

    public ConversionController(CrearConversion crearConversion, ConversionUnidadRepositorio conversiones) {
        this.crearConversion = crearConversion;
        this.conversiones = conversiones;
    }

    public record ConversionRequest(String productoId, String unidadOrigenId, String unidadDestinoId,
                                     BigDecimal factor) {
    }

    public record ConversionRespuesta(String id, String unidadOrigenId, String unidadDestinoId, BigDecimal factor) {
        static ConversionRespuesta de(ConversionUnidad c) {
            return new ConversionRespuesta(c.getId(), c.getUnidadOrigen().getId(), c.getUnidadDestino().getId(),
                    c.getFactor());
        }
    }

    @GetMapping("/productos/{productoId}/conversiones")
    public List<ConversionRespuesta> listar(@PathVariable String productoId) {
        return conversiones.findByProductoId(productoId).stream().map(ConversionRespuesta::de).toList();
    }

    @PostMapping("/conversiones")
    public ResponseEntity<?> crear(@RequestBody ConversionRequest cuerpo,
                                    @AuthenticationPrincipal String actorId) {
        Resultado<ConversionUnidad, ErrorCatalogo> resultado = crearConversion.ejecutar(cuerpo.productoId(),
                cuerpo.unidadOrigenId(), cuerpo.unidadDestinoId(), cuerpo.factor(), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(ConversionRespuesta.de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorCatalogo> resultado) {
        ErrorCatalogo error = ((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error();
        HttpStatus estado = switch (error) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case PAR_UNIDADES_DUPLICADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}
```

- [ ] **Step 5: Write `ProveedorController`**

```java
// rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/api/ProveedorController.java
package pe.rikos.operacion.catalogo.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.ActualizarProveedor;
import pe.rikos.operacion.catalogo.aplicacion.CrearProveedor;
import pe.rikos.operacion.catalogo.aplicacion.DesactivarProveedor;
import pe.rikos.operacion.catalogo.aplicacion.ErrorCatalogo;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/proveedores")
public class ProveedorController {

    private final CrearProveedor crearProveedor;
    private final ActualizarProveedor actualizarProveedor;
    private final DesactivarProveedor desactivarProveedor;
    private final ProveedorRepositorio proveedores;

    public ProveedorController(CrearProveedor crearProveedor, ActualizarProveedor actualizarProveedor,
                                DesactivarProveedor desactivarProveedor, ProveedorRepositorio proveedores) {
        this.crearProveedor = crearProveedor;
        this.actualizarProveedor = actualizarProveedor;
        this.desactivarProveedor = desactivarProveedor;
        this.proveedores = proveedores;
    }

    public record ProveedorRequest(String ruc, String razonSocial, String nombreComercial, String telefono,
                                    String correo, String direccion, String contacto) {
        Proveedor.DatosActualizacion aDatos() {
            return new Proveedor.DatosActualizacion(ruc, razonSocial, nombreComercial, telefono, correo, direccion,
                    contacto);
        }
    }

    public record ProveedorRespuesta(String id, String ruc, String razonSocial, String nombreComercial,
                                      String telefono, String correo, String direccion, String contacto,
                                      boolean activo) {
        static ProveedorRespuesta de(Proveedor p) {
            return new ProveedorRespuesta(p.getId(), p.getRuc(), p.getRazonSocial(), p.getNombreComercial(),
                    p.getTelefono(), p.getCorreo(), p.getDireccion(), p.getContacto(), p.isActivo());
        }
    }

    @GetMapping
    public List<ProveedorRespuesta> listar() {
        return proveedores.findAll().stream().map(ProveedorRespuesta::de).toList();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ProveedorRequest cuerpo, @AuthenticationPrincipal String actorId) {
        Resultado<Proveedor, ErrorCatalogo> resultado = crearProveedor.ejecutar(cuerpo.ruc(), cuerpo.razonSocial(),
                cuerpo.nombreComercial(), cuerpo.telefono(), cuerpo.correo(), cuerpo.direccion(), cuerpo.contacto(),
                actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(ProveedorRespuesta.de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody ProveedorRequest cuerpo,
                                         @AuthenticationPrincipal String actorId) {
        Resultado<Proveedor, ErrorCatalogo> resultado =
                actualizarProveedor.ejecutar(id, cuerpo.aDatos(), actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(ProveedorRespuesta.de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    @PostMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        Resultado<Proveedor, ErrorCatalogo> resultado = desactivarProveedor.ejecutar(id, actorId);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorCatalogo> resultado) {
        ErrorCatalogo error = ((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error();
        HttpStatus estado = switch (error) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case RUC_DUPLICADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}
```

- [ ] **Step 6: Compile and manually smoke-test one endpoint per controller**

Run: `../mvnw.cmd compile`
Expected: `BUILD SUCCESS`. Then start the backend and curl `GET /api/categorias` and `GET /api/unidades` with a valid admin token (same login pattern used throughout this project's prior sprints) — expect `200` with the seeded 9 root categories and 5 units.

- [ ] **Step 7: Commit**

```bash
git add servicio-operacion/src/main/java/pe/rikos/operacion/catalogo/api/
git commit -m "feat(catalogo): controladores REST del modulo completo"
```

---

### Task 12: Permission gating in `ConfiguracionSeguridad` + the 6 role-access tests

**Files:**
- Modify: `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/comun/seguridad/ConfiguracionSeguridad.java`
- Test: `rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/api/CatalogoAccesoPorRolTest.java`

**Interfaces:**
- Consumes: the permission codes already seeded in Sprint 1 (`catalogo.ver`, `catalogo.crear`, `catalogo.modificar`, `catalogo.desactivar` — confirmed present in `SembradorCatalogoPermisos`), the JWT `PERM_*` authorities already emitted by `FiltroAutenticacionJwt`.
- Produces: real 403s enforced at the route level for every method under `/categorias/**`, `/unidades/**`, `/productos/**`, `/presentaciones/**`, `/conversiones/**`, `/proveedores/**`.

- [ ] **Step 1: Write the failing test — this is the "administrador de local never worked" lesson made structural: one test per role, not just per administrador central**

```java
// rikos-backend/servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/api/CatalogoAccesoPorRolTest.java
package pe.rikos.operacion.catalogo.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.identidad.infraestructura.*;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Una prueba por cada uno de los 6 roles del sistema, contra un endpoint
 * real de catalogo. La leccion del sprint anterior: "administrador de
 * local" recibio 403 en todo el panel durante meses porque todas las
 * pruebas anteriores entraban como administrador central. No vuelve a
 * pasar si cada rol tiene su propia prueba.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogoAccesoPorRolTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepositorio usuarios;
    @Autowired
    private RolRepositorio roles;
    @Autowired
    private UsuarioRolRepositorio usuarioRoles;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ObjectMapper json;

    private String tokenPara(String nombreRol) throws Exception {
        String login = "test-" + nombreRol.replace(" ", "") + "-" + Identificador.nuevo().valor().substring(0, 6);
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), login, "Test", nombreRol,
                null, null, null, passwordEncoder.encode("clave12345"), null));
        Rol rol = roles.findAll().stream().filter(r -> r.getNombre().equals(nombreRol)).findFirst().orElseThrow();
        usuarioRoles.save(new UsuarioRol(usuario, rol, null));

        String cuerpo = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "nombreUsuario", usuario.getNombreUsuario(), "contrasena", "clave12345"))))
                .andReturn().getResponse().getContentAsString();
        return (String) json.readValue(cuerpo, Map.class).get("token");
    }

    @Test
    void administrador_central_puede_crear_productos() throws Exception {
        String token = tokenPara("administrador central");
        mvc.perform(get("/categorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void administrador_de_local_puede_crear_productos() throws Exception {
        String token = tokenPara("administrador de local");
        mvc.perform(get("/categorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void cajero_solo_puede_leer_no_crear() throws Exception {
        String token = tokenPara("cajero");
        mvc.perform(get("/categorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mvc.perform(post("/categorias").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"x\",\"naturaleza\":\"insumo\",\"orden\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void mozo_no_tiene_acceso_ni_de_lectura() throws Exception {
        String token = tokenPara("mozo");
        mvc.perform(get("/categorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void cocina_no_tiene_acceso_ni_de_lectura() throws Exception {
        String token = tokenPara("cocina");
        mvc.perform(get("/categorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void motorizado_no_tiene_acceso_ni_de_lectura() throws Exception {
        String token = tokenPara("motorizado");
        mvc.perform(get("/categorias").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `../mvnw.cmd test -Dtest=CatalogoAccesoPorRolTest`
Expected: FAIL — no route matcher exists yet for `/categorias/**` etc., so every request currently 403s (including administrador central) because it falls through to `anyRequest().authenticated()` without the right authority, OR passes anonymously if not yet covered — either way, at least the `cajero` create-vs-read split test fails since nothing distinguishes them yet.

- [ ] **Step 3: Extend `ConfiguracionSeguridad`**

Modify `rikos-backend/servicio-operacion/src/main/java/pe/rikos/operacion/comun/seguridad/ConfiguracionSeguridad.java`, inserting new matchers **before** the existing generic `.anyRequest().authenticated()` line and **before** the broad `/locales/**` etc. block is irrelevant here since these are entirely different paths — insert this new block anywhere before `.anyRequest().authenticated()`:

```java
                // Catalogo (sprint 2): el permiso manda, no el rol fijo --
                // cajero lee pero no escribe, mozo/cocina/motorizado no
                // entran ni a leer. GET primero (mas especifico que nada
                // mas lo cubre), luego el resto por metodo.
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/categorias/**", "/unidades/**", "/productos/**", "/presentaciones/**",
                        "/conversiones/**", "/proveedores/**")
                    .hasAuthority("PERM_catalogo.ver")
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/categorias/*/desactivar", "/categorias/*/activar", "/productos/*/desactivar",
                        "/productos/*/activar", "/proveedores/*/desactivar", "/proveedores/*/activar")
                    .hasAuthority("PERM_catalogo.desactivar")
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/categorias/**", "/unidades/**", "/productos/**", "/presentaciones/**",
                        "/conversiones/**", "/proveedores/**")
                    .hasAuthority("PERM_catalogo.crear")
                .requestMatchers(org.springframework.http.HttpMethod.PUT,
                        "/categorias/**", "/unidades/**", "/productos/**", "/presentaciones/**",
                        "/conversiones/**", "/proveedores/**")
                    .hasAuthority("PERM_catalogo.modificar")
```

Order matters (the first matcher that matches a request wins, per the file's existing convention): the `desactivar`/`activar` POST matcher must be declared before the general POST matcher, exactly like the existing `/roles/*/permisos` matcher is declared before the general `/roles/**` one.

- [ ] **Step 4: Run test to verify it passes**

Run: `../mvnw.cmd test -Dtest=CatalogoAccesoPorRolTest`
Expected: PASS (6 tests).

- [ ] **Step 5: Run the full backend suite to confirm no regression**

Run: `../mvnw.cmd test`
Expected: PASS, total test count = 23 (prior) + new tests from Tasks 3, 5, 6, 7, 8, 9, 10, 12 of this plan (count and report the exact new total when running this step for real — do not guess it here).

- [ ] **Step 6: Commit**

```bash
git add servicio-operacion/src/main/java/pe/rikos/operacion/comun/seguridad/ConfiguracionSeguridad.java servicio-operacion/src/test/java/pe/rikos/operacion/catalogo/api/CatalogoAccesoPorRolTest.java
git commit -m "feat(catalogo): permisos por ruta y las 6 pruebas de acceso por rol"
```

---

## Phase B — HT-05: tipos generados desde OpenAPI

### Task 13: `springdoc-openapi` in the backend

**Files:**
- Modify: `rikos-backend/servicio-operacion/pom.xml`

**Interfaces:**
- Produces: `GET /api/v3/api-docs` (JSON OpenAPI spec), which Task 14's generator consumes.

- [ ] **Step 1: Add the dependency**

In `rikos-backend/servicio-operacion/pom.xml`, inside `<dependencies>`, add (matching the version convention already used for `spring-boot-starter-*` — no explicit `<version>` tag, since the project's Spring Boot BOM does not manage `springdoc-openapi`, so a version IS required here; use the latest 2.x release compatible with Spring Boot 4 / Spring Framework 7, confirmed at implementation time via `mvn versions:display-dependency-updates` or the springdoc release notes, since this plan cannot pin a version number that might already be stale by execution time):

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version><!-- resolver la ultima version 2.x compatible con Spring Boot 4 / Spring 7 al momento de implementar --></version>
</dependency>
```

**Note for the implementer:** this is the one place in this plan where a concrete value is deliberately left for execution time rather than guessed — pinning a library version from a design document risks shipping a stale or non-existent version. Resolve it by checking `https://springdoc.org` or Maven Central for the current 2.x release compatible with Spring Boot 4, add it, and confirm with Step 2 below.

- [ ] **Step 2: Verify the dependency resolves and the endpoint responds**

Run:
```bash
cd rikos-backend/servicio-operacion
../mvnw.cmd compile
```
Expected: `BUILD SUCCESS` (confirms the version resolved). Then start the backend and:
```bash
curl -s http://localhost:8081/api/v3/api-docs | head -c 200
```
Expected: a JSON document starting with `{"openapi":"3....`

- [ ] **Step 3: Commit**

```bash
git add servicio-operacion/pom.xml
git commit -m "feat(ht-05): springdoc-openapi expone la especificacion en /api/v3/api-docs"
```

---

### Task 14: `openapi-typescript` in the frontend, generation script, `.gitignore`

**Files:**
- Modify: `rikos-web/package.json`
- Create: `rikos-web/.gitignore` (or modify if it already exists — check first)
- Create: `rikos-web/src/api/generado/.gitkeep` (placeholder so the empty folder exists before the first generation)

**Interfaces:**
- Produces: `npm run generar-tipos` script; `src/api/generado/esquema.d.ts` (git-ignored, regenerated on demand) exporting a TypeScript `components` namespace that Task 15 consumes.

- [ ] **Step 1: Check for an existing `.gitignore` in `rikos-web`**

Run: `ls rikos-web/.gitignore 2>&1 || echo "no existe"`

- [ ] **Step 2: Add the dev dependency and the script**

In `rikos-web/package.json`, add to `devDependencies`:
```json
"openapi-typescript": "^7.4.0"
```
(pin the actual latest 7.x at implementation time the same way as Task 13 — 7.x is the current major as of this plan's writing, but confirm via `npm view openapi-typescript version` before adding).

Add to `scripts`:
```json
"generar-tipos": "openapi-typescript http://localhost:8081/api/v3/api-docs -o src/api/generado/esquema.d.ts"
```

Run: `npm install` in `rikos-web/` to fetch the new dev dependency.

- [ ] **Step 3: Create (or append to) `.gitignore`**

```
# rikos-web/.gitignore
node_modules/
dist/

# HT-05: generado desde /api/v3/api-docs, nunca se edita a mano.
# Correr "npm run generar-tipos" con el backend arriba para regenerarlo.
src/api/generado/
```

(If a `.gitignore` already exists with `node_modules/`/`dist/` entries, only append the `src/api/generado/` block with its comment — don't duplicate the existing lines.)

- [ ] **Step 4: Generate once against the running backend and inspect the output**

Run (with the backend from Task 13 still running):
```bash
cd rikos-web
npm run generar-tipos
```
Expected: `src/api/generado/esquema.d.ts` created, containing a `components` interface with a `schemas` key listing every `record` currently exposed by the controllers (`LocalRespuesta`, `RolRespuesta`, `UsuarioRespuesta`, the new `CategoriaRespuesta`/`ProductoRespuesta`/`ProveedorRespuesta` from Phase A's controllers, etc.).

- [ ] **Step 5: Commit (the generated file itself is NOT committed — only the script, package.json changes, and .gitignore)**

```bash
git add rikos-web/package.json rikos-web/package-lock.json rikos-web/.gitignore
git commit -m "feat(ht-05): openapi-typescript y el comando generar-tipos"
```

---

### Task 15: Migrate `cliente.ts` to the generated types, demonstrate drift detection

**Files:**
- Modify: `rikos-web/src/api/cliente.ts`

**Interfaces:**
- Consumes: `src/api/generado/esquema.d.ts` (Task 14).
- Produces: the same exported interfaces `cliente.ts` already has (`Local`, `Rol`, `Usuario`, `UsuarioDetalle`, `Permiso`, `PermisoUsuario`, etc.), now defined as aliases instead of hand-written redefinitions — every existing importer of these types (every `Pantalla*.tsx` file) keeps working with no changes required at the call site, since the shape stays the same, only its source changes.

- [ ] **Step 1: Add the import and alias the first interface as a proof of concept**

In `rikos-web/src/api/cliente.ts`, near the top (after the `ErrorApi` class), add:

```typescript
import type { components } from './generado/esquema'

type Esquemas = components['schemas']
```

Then find the existing hand-written `export interface Local { ... }` block and replace it with:

```typescript
export type Local = Esquemas['LocalRespuesta']
```

- [ ] **Step 2: Run `tsc --noEmit` to find every place the hand-written and generated shapes disagree**

Run: `npx tsc --noEmit`
Expected: likely several errors — the hand-written `Local` interface and the generated `LocalRespuesta` schema may differ in small ways (e.g. optional vs. required fields, since Java `record` nullability doesn't always map 1:1 to what springdoc infers). Read each error and fix `cliente.ts`'s usage (not the generated file) until clean. This is expected, not a failure of the task — document in the phase's completion note (Task 33) exactly what mismatches were found, since that's the demonstration the checklist asks for.

- [ ] **Step 3: Repeat Step 1's pattern for the remaining hand-written interfaces**

Replace each of `Rol`, `Usuario`, `UsuarioDetalle`, `Permiso`, `PermisoUsuario`, `ExcepcionPermiso`, `RolResumen`, `LocalPrincipalResumen`, `UltimoAcceso` with `type X = Esquemas['XRespuesta']` (or the actual generated schema name — inspect `esquema.d.ts`'s `schemas` keys directly, since springdoc's auto-generated names may not exactly match the record's Java name if there are naming collisions across controllers; resolve any collision by checking the generated file, not by guessing). Run `npx tsc --noEmit` after each replacement, fixing usage-site errors as they appear, rather than replacing all of them at once — smaller batches make the source of each new error obvious.

- [ ] **Step 4: Run the full frontend type-check to confirm the migration is clean**

Run: `npx tsc --noEmit`
Expected: no errors.

- [ ] **Step 5: Demonstrate drift detection for the checklist — change a field, regenerate, show the break**

This step is the literal deliverable requested in the sprint checklist ("muéstrame el comando y qué pasa si el backend cambia un campo"), performed once, manually, and its output captured in `doc/historias/US-008-US-009-US-011-catalogo.md` (Task 33):

1. Temporarily rename a field in one backend response record (e.g. `ProveedorRespuesta.razonSocial` → `ProveedorRespuesta.nombreLegal` in `ProveedorController.java`).
2. Restart the backend, run `npm run generar-tipos` again.
3. Run `npx tsc --noEmit` — it now fails at every place `proveedor.razonSocial` is read in the frontend (there are none yet in this sprint's new screens if Task 15 runs before Phase E, so this may need to be re-run once Phase E's `PantallaProveedores.tsx` exists and actually reads `razonSocial` — if so, perform this demonstration step again at the end of Phase E instead of here, and note that in the historia doc).
4. Revert the backend field rename.
5. Copy the exact `tsc` error output into the historia doc as the evidence.

- [ ] **Step 6: Commit**

```bash
git add rikos-web/src/api/cliente.ts
git commit -m "feat(ht-05): cliente.ts usa los tipos generados en vez de interfaces a mano"
```

---

## Phase C — HT-07: entrada numérica compartida y formato de moneda

### Task 16: `EntradaNumerica` component

**Files:**
- Create: `rikos-web/src/comun/componentes/EntradaNumerica.tsx`
- Modify: `rikos-web/src/comun/componentes/index.ts`
- Test: manual, verified via Task 17/18's integration (this component has no business logic worth a unit test in isolation beyond what TypeScript's prop types already enforce — its correctness is proven by its two real consumers)

**Interfaces:**
- Consumes: `TecladoNumerico` (existing, unchanged — `{ onDigito, onBorrar, deshabilitado }`).
- Produces: `EntradaNumerica` component with props `{ valor: string, onCambiar: (v: string) => void, largoFijo?: number, oculto?: boolean, alCompletar?: (v: string) => void, deshabilitado?: boolean }` — this exact signature is what Tasks 17 and 18 both import and use.

- [ ] **Step 1: Write the component**

```tsx
// rikos-web/src/comun/componentes/EntradaNumerica.tsx
import { TecladoNumerico } from './TecladoNumerico'

/**
 * HT-07: un solo componente para dos patrones de entrada numerica tactil.
 *
 * Modo PIN (largoFijo + oculto): puntos en vez de digitos, autoconfirma
 * al llegar al largo fijo. Lo usan el login del mozo/kiosco y el PIN del
 * formulario de usuario.
 *
 * Modo cantidad (sin oculto): se ve lo tecleado, sin autoconfirmar --
 * quien lo use decide cuando confirmar (boton aparte). Para cuando
 * existan las pantallas de cantidad de pedido y cierre de caja.
 */
interface Props {
  valor: string
  onCambiar: (valor: string) => void
  largoFijo?: number
  oculto?: boolean
  alCompletar?: (valor: string) => void
  deshabilitado?: boolean
}

export function EntradaNumerica({ valor, onCambiar, largoFijo, oculto, alCompletar, deshabilitado }: Props) {
  function agregarDigito(digito: string) {
    if (deshabilitado) return
    if (largoFijo && valor.length >= largoFijo) return
    const nuevo = valor + digito
    onCambiar(nuevo)
    if (largoFijo && nuevo.length === largoFijo) alCompletar?.(nuevo)
  }

  function borrar() {
    onCambiar('')
  }

  return (
    <div className="entrada-numerica">
      {oculto && largoFijo ? (
        <div className="entrada-numerica-puntos" aria-label={`${valor.length} de ${largoFijo} dígitos`}>
          {Array.from({ length: largoFijo }).map((_, i) => (
            <span key={i} className={i < valor.length ? 'punto lleno' : 'punto'} />
          ))}
        </div>
      ) : (
        <div className="entrada-numerica-valor" aria-live="polite">
          {valor || '0'}
        </div>
      )}

      <TecladoNumerico onDigito={agregarDigito} onBorrar={borrar} deshabilitado={deshabilitado} />
    </div>
  )
}
```

- [ ] **Step 2: Export it**

In `rikos-web/src/comun/componentes/index.ts`, add:
```typescript
export { EntradaNumerica } from './EntradaNumerica'
```

- [ ] **Step 3: Add the CSS for the visible-value mode (the dots mode reuses `PantallaPin.tsx`'s existing `.pin-puntos`/`.punto`/`.punto.lleno` classes — rename those to the generic names below so both modes share one stylesheet block instead of two near-duplicates)**

In `rikos-web/src/estilos/global.css`, find the existing `.pin-puntos`, `.punto`, `.punto.lleno` rules (from `PantallaPin.tsx`'s styling) and rename their selectors to `.entrada-numerica-puntos`, `.punto`, `.punto.lleno` (the inner `.punto`/`.punto.lleno` class names can stay as-is since they're already generic; only the wrapper class needs renaming from `.pin-puntos` to `.entrada-numerica-puntos`). Add a new rule for the visible-value mode:

```css
.entrada-numerica-valor {
  font-size: 28px;
  font-weight: 700;
  text-align: center;
  min-height: 40px;
  letter-spacing: .02em;
}
```

- [ ] **Step 4: Commit**

```bash
git add rikos-web/src/comun/componentes/EntradaNumerica.tsx rikos-web/src/comun/componentes/index.ts rikos-web/src/estilos/global.css
git commit -m "feat(ht-07): componente EntradaNumerica compartido"
```

---

### Task 17: Refactor `PantallaPin.tsx` to use `EntradaNumerica`

**Files:**
- Modify: `rikos-web/src/features/autenticacion/PantallaPin.tsx`

**Interfaces:**
- Consumes: `EntradaNumerica` (Task 16).

- [ ] **Step 1: Replace the inline PIN state/dots/keypad wiring with `EntradaNumerica`**

In `rikos-web/src/features/autenticacion/PantallaPin.tsx`, replace this block:

```tsx
          <div className="pin-puntos" aria-label={`PIN de ${pin.length} de ${LARGO_PIN} dígitos`}>
            {Array.from({ length: LARGO_PIN }).map((_, i) => (
              <span key={i} className={i < pin.length ? 'punto lleno' : 'punto'} />
            ))}
          </div>

          <TecladoNumerico onDigito={agregar} onBorrar={() => setPin('')} deshabilitado={verificando || bloqueo.activa} />
```

with:

```tsx
          <EntradaNumerica
            valor={pin}
            onCambiar={setPin}
            largoFijo={LARGO_PIN}
            oculto
            alCompletar={intentar}
            deshabilitado={verificando || bloqueo.activa}
          />
```

Remove the now-unused `agregar` function (its logic — the length check and calling `intentar` at completion — is now `EntradaNumerica`'s job via `alCompletar`) and the `TecladoNumerico` import, replacing it with an `EntradaNumerica` import from `'../../comun/componentes/EntradaNumerica'`.

- [ ] **Step 2: Verify in the browser**

Run: start `rikos-web`'s dev server (`npm run dev`), navigate to the PIN screen (`/` for the kiosco/POS lock screen per the existing routing), type a 4-digit PIN, confirm it still submits on the 4th digit exactly as before, confirm the dots still fill in the same visual style, confirm "Borrar" still clears it. This is a pure refactor — zero behavior change is the pass criterion.

- [ ] **Step 3: Commit**

```bash
git add rikos-web/src/features/autenticacion/PantallaPin.tsx
git commit -m "refactor(ht-07): PantallaPin usa EntradaNumerica en vez de logica propia"
```

---

### Task 18: Migrate the PIN field in `PantallaUsuarioFormulario.tsx` to `EntradaNumerica`

**Files:**
- Modify: `rikos-web/src/features/usuarios/PantallaUsuarioFormulario.tsx`

**Interfaces:**
- Consumes: `EntradaNumerica` (Task 16).

- [ ] **Step 1: Replace the plain `<input maxLength={4}>` PIN field**

In `rikos-web/src/features/usuarios/PantallaUsuarioFormulario.tsx`, find the `.pin-code-inputs` block (a single `<input>` with `maxLength={4}`, client-validated against `/^\d{4}$/` on submit) and replace it with `EntradaNumerica` in oculto mode, without `alCompletar` (this form has an explicit "Guardar" button — it must not auto-submit the whole user form just because 4 digits were typed):

```tsx
          <EntradaNumerica
            valor={formulario.pin}
            onCambiar={(v) => setFormulario((f) => ({ ...f, pin: v }))}
            largoFijo={4}
            oculto
          />
```

Keep the existing `/^\d{4}$/` validation on submit exactly as-is — `EntradaNumerica`'s `largoFijo` only stops the keypad from accepting a 5th digit, it doesn't replace form-level validation (a user can still submit with 0-3 digits typed, which the existing check already catches).

- [ ] **Step 2: Verify in the browser**

Navigate to `/admin/usuarios/nuevo`, confirm the PIN block now renders as dots + keypad instead of a plain text field, type 4 digits, confirm the dots fill in, confirm the rest of the form (credenciales, locales, roles) is untouched, save a user and confirm the PIN still persists correctly (same manual check as the prior sprint's verification: login with the new PIN succeeds).

- [ ] **Step 3: Commit**

```bash
git add rikos-web/src/features/usuarios/PantallaUsuarioFormulario.tsx
git commit -m "refactor(ht-07): el PIN del formulario de usuario usa EntradaNumerica"
```

---

### Task 19: `formatearMoneda` utility

**Files:**
- Create: `rikos-web/src/comun/formato/moneda.ts`
- Test: `rikos-web/src/comun/formato/moneda.test.ts` (if the project has a test runner configured for the frontend — check first; if not, this task's "test" step becomes a manual `tsx`/node one-off check, noted explicitly rather than silently skipped)

**Interfaces:**
- Produces: `formatearMoneda(centimos: number): string` — e.g. `formatearMoneda(1250)` → `"S/ 12.50"`. Used wherever a `dinero` (céntimos) value is displayed; not used for `costo` (6-decimal) values, which are a separate future formatting need not in this sprint's scope.

- [ ] **Step 1: Check whether the frontend has a test runner**

Run: `grep -E "vitest|jest" rikos-web/package.json`
Expected: likely nothing — this project's frontend has had no test runner configured through Sprint 1. If that's confirmed, skip Steps 2–4's automated form and instead do a one-off manual verification in Step 3 via `node` directly against the compiled output, or via a temporary `console.log` in a component during dev — document in the historia doc (Task 33) that frontend has no test runner yet, which is itself worth surfacing as a gap (see Task 33's "qué decidiste donde el modelo se quedó corto" — this is a process gap, not a model gap, but the sprint's closing report is the right place to flag it either way).

- [ ] **Step 2: Write the function**

```typescript
// rikos-web/src/comun/formato/moneda.ts
/**
 * 2.6: los importes en centimos nunca tienen fraccion (nadie paga
 * S/ 12.4567) -- esta funcion solo formatea, nunca redondea, porque no
 * hay nada que redondear en un entero. El redondeo mitad-arriba aplica a
 * los costos (6 decimales), que se formatean aparte y no en este sprint.
 */
export function formatearMoneda(centimos: number): string {
  const soles = centimos / 100
  return `S/ ${soles.toFixed(2)}`
}
```

- [ ] **Step 3: Verify manually (no test runner available per Step 1)**

Run:
```bash
cd rikos-web
node -e "
function formatearMoneda(centimos) { return 'S/ ' + (centimos/100).toFixed(2); }
console.log(formatearMoneda(1250));  // S/ 12.50
console.log(formatearMoneda(100));   // S/ 1.00
console.log(formatearMoneda(5));     // S/ 0.05
console.log(formatearMoneda(0));     // S/ 0.00
"
```
Expected: exactly those four lines, in that order.

- [ ] **Step 4: Commit**

```bash
git add rikos-web/src/comun/formato/moneda.ts
git commit -m "feat(ht-07): utilidad formatearMoneda"
```

---

## Phase D — Navegación: acordeón generalizado + modo riel

### Task 20: Replace `NAVEGACION` with the full tree from the spec

**Files:**
- Modify: `rikos-web/src/apps/admin/BarraLateral.tsx`

**Interfaces:**
- Produces: the new `NAVEGACION` data shape every later task in this phase renders. `ItemNav.subItems` entries without a `ruta` are "pendiente" (existing convention, unchanged); entries with a `ruta` are active.

- [ ] **Step 1: Replace the `NAVEGACION` constant**

In `rikos-web/src/apps/admin/BarraLateral.tsx`, replace the entire `NAVEGACION` array with:

```typescript
const NAVEGACION: SeccionNav[] = [
  {
    titulo: 'Operación',
    items: [
      { etiqueta: 'Inicio', Icono: IconoInicio },
      {
        etiqueta: 'Caja y turnos',
        Icono: IconoCajaRegistradora,
        subItems: [
          { etiqueta: 'Turnos' },
          { etiqueta: 'Cierres de caja' },
        ],
      },
      {
        etiqueta: 'Delivery y recojo',
        Icono: IconoMoto,
        subItems: [
          { etiqueta: 'Delivery' },
          { etiqueta: 'Recojo programado' },
        ],
      },
    ],
  },
  {
    titulo: 'Negocio',
    items: [
      {
        etiqueta: 'Pensionistas',
        Icono: IconoPersonas,
        subItems: [
          { etiqueta: 'Pensionistas' },
          { etiqueta: 'Cuentas corrientes' },
          { etiqueta: 'Abonos' },
          { etiqueta: 'Planes tarifarios' },
          { etiqueta: 'Estados de cuenta' },
        ],
      },
      {
        etiqueta: 'Menú semanal',
        Icono: IconoCalendarioSemana,
        subItems: [
          { etiqueta: 'Programación semanal' },
          { etiqueta: 'Disponibilidad' },
        ],
      },
      {
        etiqueta: 'Catálogo y recetas',
        Icono: IconoLibro,
        subItems: [
          { etiqueta: 'Productos', ruta: '/admin/catalogo/productos' },
          { etiqueta: 'Categorías', ruta: '/admin/catalogo/categorias' },
          { etiqueta: 'Unidades y presentaciones', ruta: '/admin/catalogo/unidades' },
          { etiqueta: 'Recetas' },
        ],
      },
      {
        etiqueta: 'Inventario',
        Icono: IconoCaja,
        subItems: [
          { etiqueta: 'Existencias' },
          { etiqueta: 'Kardex' },
          { etiqueta: 'Ajustes' },
          { etiqueta: 'Transferencias' },
          { etiqueta: 'Conteos físicos' },
        ],
      },
      {
        etiqueta: 'Producción',
        Icono: IconoProduccion,
        subItems: [
          { etiqueta: 'Producción' },
          { etiqueta: 'Consumo interno' },
        ],
      },
      {
        etiqueta: 'Compras y proveedores',
        Icono: IconoCarrito,
        subItems: [
          { etiqueta: 'Compras' },
          { etiqueta: 'Proveedores', ruta: '/admin/compras/proveedores' },
        ],
      },
      {
        etiqueta: 'Facturación',
        Icono: IconoRecibo,
        subItems: [
          { etiqueta: 'Bandeja de comprobantes' },
          { etiqueta: 'Series' },
        ],
      },
    ],
  },
  {
    titulo: 'Administración',
    items: [
      {
        etiqueta: 'Reportes',
        Icono: IconoGrafico,
        subItems: [
          { etiqueta: 'Ventas y rentabilidad' },
          { etiqueta: 'Caja y diferencias' },
          { etiqueta: 'Pensionistas y saldos' },
          { etiqueta: 'Inventario y Kardex' },
          { etiqueta: 'Producción' },
          { etiqueta: 'Delivery' },
          { etiqueta: 'Consumos internos' },
        ],
      },
      {
        etiqueta: 'Usuarios y permisos',
        Icono: IconoUsuario,
        subItems: [
          { etiqueta: 'Administrar', ruta: '/admin/usuarios' },
          { etiqueta: 'Roles', ruta: '/admin/usuarios/roles' },
        ],
      },
      { etiqueta: 'Locales', ruta: '/admin/locales', Icono: IconoUbicacion },
      {
        etiqueta: 'Configuración',
        Icono: IconoEngranaje,
        subItems: [
          { etiqueta: 'Motivos' },
          { etiqueta: 'Medios de pago' },
          { etiqueta: 'Datos de la empresa' },
        ],
      },
    ],
  },
]
```

Note `SubItemNav.ruta` must become optional (`ruta?: string`) in the `SubItemNav` interface just above — it's currently required (`ruta: string`), which is what makes today's "Usuarios y permisos" submenu items always active; the new tree needs pending sub-items too. Update:

```typescript
interface SubItemNav {
  etiqueta: string
  ruta?: string
}
```

- [ ] **Step 2: Confirm it still compiles (rendering logic changes come in Task 21 — this step only proves the data shape is accepted)**

Run: `npx tsc --noEmit` in `rikos-web/`.
Expected: errors, because Task 21 hasn't yet updated the render logic to handle `subItems` entries without a `ruta` (today's code assumes every `subItems` entry has one, since `NavLink to={sub.ruta}` requires a string). This is expected — Task 21 fixes it. Do not attempt to make this step pass in isolation; note the specific compile error here as the starting point for Task 21's Step 1.

- [ ] **Step 3: Commit**

```bash
git add rikos-web/src/apps/admin/BarraLateral.tsx
git commit -m "feat(navegacion): arbol completo de la barra lateral, marcado pendiente lo que no existe"
```

---

### Task 21: Generalize the accordion — collapsible per module, `localStorage`-persisted, auto-open on active child, mixed pending/active children

**Files:**
- Modify: `rikos-web/src/apps/admin/BarraLateral.tsx`

**Interfaces:**
- Consumes: the `NAVEGACION` tree (Task 20).
- Produces: the rendering behavior every requirement in the spec's "Comportamiento" section describes. No new exported interface — this is a leaf UI component, its only consumer is the app shell that already renders it (`apps/admin/index.tsx`, unchanged).

- [ ] **Step 1: Fix the `subItems` rendering to allow pending sub-items and support click-to-toggle instead of always-expanded**

Replace the entire body of the `BarraLateral` function in `rikos-web/src/apps/admin/BarraLateral.tsx` with:

```tsx
const CLAVE_ABIERTOS = 'rikos-panel-lateral-abiertos'

function leerAbiertos(): Record<string, boolean> {
  try {
    return JSON.parse(localStorage.getItem(CLAVE_ABIERTOS) ?? '{}')
  } catch {
    return {}
  }
}

function guardarAbiertos(abiertos: Record<string, boolean>) {
  try {
    localStorage.setItem(CLAVE_ABIERTOS, JSON.stringify(abiertos))
  } catch {
    // localStorage puede fallar en modo privado -- no es critico, se pierde
    // el estado recordado entre recargas y ya, no rompe la navegacion.
  }
}

export function BarraLateral({ abierta, onCerrar }: { abierta: boolean; onCerrar: () => void }) {
  const ubicacion = useLocation()
  const [abiertos, setAbiertos] = useState<Record<string, boolean>>(leerAbiertos)

  // Si la ruta activa es un hijo de un modulo cerrado, ese modulo se abre
  // solo al entrar -- no se sobreescribe un cierre manual del usuario en
  // otros modulos, solo se agrega el que corresponde a la ruta actual.
  useEffect(() => {
    for (const seccion of NAVEGACION) {
      for (const item of seccion.items) {
        if (!item.subItems) continue
        const esActivo = item.subItems.some((sub) => sub.ruta && ubicacion.pathname.startsWith(sub.ruta))
        if (esActivo && !abiertos[item.etiqueta]) {
          setAbiertos((actual) => {
            const nuevo = { ...actual, [item.etiqueta]: true }
            guardarAbiertos(nuevo)
            return nuevo
          })
        }
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ubicacion.pathname])

  function alternar(etiqueta: string) {
    setAbiertos((actual) => {
      const nuevo = { ...actual, [etiqueta]: !actual[etiqueta] }
      guardarAbiertos(nuevo)
      return nuevo
    })
  }

  return (
    <nav className={`panel-lateral ${abierta ? 'abierta' : ''}`} aria-label="Navegación del panel">
      <FormaDecorativaLateral />

      <div className="panel-lateral-marca">
        <IconoGorroChef className="panel-lateral-marca-icono" aria-hidden="true" />
        <div className="panel-lateral-marca-texto">
          <strong>RIKO'S</strong>
          <span>RESTAURANTE</span>
        </div>
      </div>

      <div className="panel-lateral-scroll">
        {NAVEGACION.map((seccion) => (
          <div className="panel-lateral-seccion" key={seccion.titulo}>
            <div className="panel-lateral-titulo">{seccion.titulo}</div>
            {seccion.items.map((item) => {
              if (item.subItems) {
                const todosPendientes = item.subItems.every((sub) => !sub.ruta)
                const grupoActivo = item.subItems.some((sub) => sub.ruta && ubicacion.pathname.startsWith(sub.ruta))
                const expandido = Boolean(abiertos[item.etiqueta])

                if (todosPendientes) {
                  return (
                    <span key={item.etiqueta} className="panel-lateral-item panel-lateral-item-deshabilitado">
                      <item.Icono className="panel-lateral-item-icono" aria-hidden="true" />
                      <span className="panel-lateral-item-texto">{item.etiqueta}</span>
                      <span className="panel-lateral-pendiente">Pendiente</span>
                    </span>
                  )
                }

                return (
                  <div key={item.etiqueta} className="panel-lateral-grupo">
                    <button
                      type="button"
                      className={`panel-lateral-item panel-lateral-item-grupo ${grupoActivo ? 'activo' : ''}`}
                      aria-expanded={expandido}
                      onClick={() => alternar(item.etiqueta)}
                    >
                      <item.Icono className="panel-lateral-item-icono" aria-hidden="true" />
                      <span className="panel-lateral-item-texto">{item.etiqueta}</span>
                      <IconoFlechaAbajo
                        className={`panel-lateral-item-chevron ${expandido ? 'girado' : ''}`}
                        aria-hidden="true"
                        width={14}
                        height={14}
                      />
                    </button>
                    <div className={`panel-lateral-subitems ${expandido ? 'expandido' : ''}`}>
                      <div className="panel-lateral-subitems-inner">
                        {item.subItems.map((sub) =>
                          sub.ruta ? (
                            <NavLink
                              key={sub.etiqueta}
                              to={sub.ruta}
                              className={({ isActive }) => `panel-lateral-subitem ${isActive ? 'activo' : ''}`}
                              onClick={onCerrar}
                            >
                              <span className="panel-lateral-subitem-punto" aria-hidden="true" />
                              {sub.etiqueta}
                            </NavLink>
                          ) : (
                            <span key={sub.etiqueta} className="panel-lateral-subitem panel-lateral-subitem-pendiente">
                              <span className="panel-lateral-subitem-punto" aria-hidden="true" />
                              {sub.etiqueta}
                              <span className="panel-lateral-pendiente">Pendiente</span>
                            </span>
                          ),
                        )}
                      </div>
                    </div>
                  </div>
                )
              }

              return item.ruta ? (
                <NavLink
                  key={item.etiqueta}
                  to={item.ruta}
                  className={({ isActive }) => `panel-lateral-item ${isActive ? 'activo' : ''}`}
                  onClick={onCerrar}
                >
                  <item.Icono className="panel-lateral-item-icono" aria-hidden="true" />
                  {item.etiqueta}
                </NavLink>
              ) : (
                <span key={item.etiqueta} className="panel-lateral-item panel-lateral-item-deshabilitado">
                  <item.Icono className="panel-lateral-item-icono" aria-hidden="true" />
                  <span className="panel-lateral-item-texto">{item.etiqueta}</span>
                  <span className="panel-lateral-pendiente">Pendiente</span>
                </span>
              )
            })}
          </div>
        ))}
      </div>
    </nav>
  )
}
```

Add `useEffect` and `useState` to the existing `import { useState } from 'react'`-less file's React import (currently `BarraLateral.tsx` imports only types from React implicitly via JSX — add `import { useEffect, useState } from 'react'` at the top), and add `IconoFlechaAbajo` to the existing `comun/componentes` import list (it's already exported per the earlier `index.ts` check in this plan's research phase).

- [ ] **Step 2: Add the accordion CSS (animated expand/collapse, respecting `prefers-reduced-motion`, plus the pending-subitem and chevron styles)**

In `rikos-web/src/estilos/global.css`, find the existing `.panel-lateral-subitems` rule (from Sprint 1's "Usuarios y permisos" submenu) and replace it — that rule currently has no collapse animation since every submenu was always expanded before this task:

```css
.panel-lateral-subitems {
  display: grid;
  grid-template-rows: 0fr;
  overflow: hidden;
  transition: grid-template-rows .2s ease;
}
.panel-lateral-subitems > * { min-height: 0; }
.panel-lateral-subitems.expandido { grid-template-rows: 1fr; }
.panel-lateral-subitems > .panel-lateral-subitems-inner { overflow: hidden; }

@media (prefers-reduced-motion: reduce) {
  .panel-lateral-subitems { transition: none; }
}

.panel-lateral-item-grupo {
  width: 100%;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  font: inherit;
  color: inherit;
}
.panel-lateral-item-chevron { transition: transform .15s ease; margin-left: auto; }
.panel-lateral-item-chevron.girado { transform: rotate(180deg); }

.panel-lateral-subitem-pendiente {
  opacity: .55;
  cursor: default;
}
```

(Step 1's JSX already wraps the mapped children in `.panel-lateral-subitems-inner`, which is what lets the `grid-template-rows: 0fr → 1fr` transition animate smoothly — the outer `.panel-lateral-subitems` measures a single child instead of the raw list.)

- [ ] **Step 3: Verify in the browser**

Run the frontend dev server, log in as admin, navigate to `/admin`:
- Click "Catálogo y recetas" — it expands, shows Productos/Categorías/Unidades y presentaciones (clickable) and Recetas (pending, dimmed). It does NOT navigate anywhere on click.
- Click it again — it collapses.
- Reload the page — its open/closed state is preserved (test both open and closed).
- Navigate directly to `/admin/usuarios/roles` — "Usuarios y permisos" auto-opens even if it was closed before navigating.
- Confirm "Caja y turnos" (all children pending) renders disabled exactly like before, not as a clickable accordion.
- Confirm "Compras y proveedores" (one active child, one pending) expands and shows both, with "Proveedores" clickable and "Compras" dimmed.

- [ ] **Step 4: Commit**

```bash
git add rikos-web/src/apps/admin/BarraLateral.tsx rikos-web/src/estilos/global.css
git commit -m "feat(navegacion): acordeon generalizado con estado recordado y auto-apertura"
```

---

### Task 22: Icon-rail collapse mode with hover/click flyout submenu

**Files:**
- Modify: `rikos-web/src/apps/admin/BarraLateral.tsx`

**Interfaces:**
- Produces: a `localStorage`-persisted collapse toggle, independent of the accordion's open/closed state from Task 21. Desktop-only (the existing mobile drawer behavior via `abierta`/`onCerrar` is untouched — the rail mode only applies when the drawer is in its "always visible" desktop state, not when it's the mobile off-canvas panel).

- [ ] **Step 1: Add the collapse state and toggle button**

In `rikos-web/src/apps/admin/BarraLateral.tsx`, add alongside the existing `abiertos` state:

```tsx
const CLAVE_COLAPSADA = 'rikos-panel-lateral-colapsada'

function leerColapsada(): boolean {
  try {
    return localStorage.getItem(CLAVE_COLAPSADA) === '1'
  } catch {
    return false
  }
}
```

Inside `BarraLateral`, add:

```tsx
const [colapsada, setColapsada] = useState<boolean>(leerColapsada)

function alternarColapso() {
  setColapsada((actual) => {
    const nuevo = !actual
    try {
      localStorage.setItem(CLAVE_COLAPSADA, nuevo ? '1' : '0')
    } catch {
      // ver nota de guardarAbiertos -- no critico si falla.
    }
    return nuevo
  })
}
```

Add the toggle button right after the `<FormaDecorativaLateral />` line, before `.panel-lateral-marca`:

```tsx
<button
  type="button"
  className="panel-lateral-colapsar"
  onClick={alternarColapso}
  aria-label={colapsada ? 'Expandir menú' : 'Colapsar menú'}
>
  <IconoFlechaIzquierda
    className={`panel-lateral-colapsar-icono ${colapsada ? 'girado' : ''}`}
    aria-hidden="true"
    width={16}
    height={16}
  />
</button>
```

Add `colapsada ? 'colapsada' : ''` to the `nav`'s `className` template string (alongside the existing `abierta ? 'abierta' : ''`):

```tsx
<nav className={`panel-lateral ${abierta ? 'abierta' : ''} ${colapsada ? 'colapsada' : ''}`} aria-label="Navegación del panel">
```

- [ ] **Step 2: Add the flyout — a small self-contained subcomponent, since it needs its own hover-delay state per menu item**

Add this new component in the same file, below `BarraLateral` (or as a separate file `rikos-web/src/apps/admin/ItemConFlyout.tsx` if `BarraLateral.tsx` is getting long — check its line count first with `wc -l rikos-web/src/apps/admin/BarraLateral.tsx`; if it's over ~250 lines after Task 21's changes, extract this into its own file and import it, following the plan's file-structure principle of one clear responsibility per file):

```tsx
function ItemConFlyout({
  item,
  ubicacion,
  onNavegar,
}: {
  item: ItemNav
  ubicacion: ReturnType<typeof useLocation>
  onNavegar: () => void
}) {
  const [flyoutAbierto, setFlyoutAbierto] = useState(false)
  const cerrarConDelay = useRef<ReturnType<typeof setTimeout>>()

  function abrir() {
    clearTimeout(cerrarConDelay.current)
    setFlyoutAbierto(true)
  }

  function cerrarConRetraso() {
    cerrarConDelay.current = setTimeout(() => setFlyoutAbierto(false), 200)
  }

  if (!item.subItems) return null
  const grupoActivo = item.subItems.some((sub) => sub.ruta && ubicacion.pathname.startsWith(sub.ruta))
  const todosPendientes = item.subItems.every((sub) => !sub.ruta)

  return (
    <div
      className="panel-lateral-riel-item"
      onMouseEnter={todosPendientes ? undefined : abrir}
      onMouseLeave={todosPendientes ? undefined : cerrarConRetraso}
    >
      <button
        type="button"
        className={`panel-lateral-item panel-lateral-item-riel ${grupoActivo ? 'activo' : ''} ${todosPendientes ? 'panel-lateral-item-deshabilitado' : ''}`}
        aria-expanded={flyoutAbierto}
        aria-label={item.etiqueta}
        disabled={todosPendientes}
        onClick={() => (todosPendientes ? undefined : setFlyoutAbierto((v) => !v))}
        onKeyDown={(e) => {
          if (e.key === 'Escape') setFlyoutAbierto(false)
        }}
      >
        <item.Icono className="panel-lateral-item-icono" aria-hidden="true" />
      </button>

      {flyoutAbierto && (
        <div className="panel-lateral-flyout" onMouseEnter={abrir} onMouseLeave={cerrarConRetraso}>
          <div className="panel-lateral-flyout-titulo">{item.etiqueta}</div>
          {item.subItems.map((sub) =>
            sub.ruta ? (
              <NavLink
                key={sub.etiqueta}
                to={sub.ruta}
                className={({ isActive }) => `panel-lateral-subitem ${isActive ? 'activo' : ''}`}
                onClick={() => {
                  setFlyoutAbierto(false)
                  onNavegar()
                }}
              >
                {sub.etiqueta}
              </NavLink>
            ) : (
              <span key={sub.etiqueta} className="panel-lateral-subitem panel-lateral-subitem-pendiente">
                {sub.etiqueta}
                <span className="panel-lateral-pendiente">Pendiente</span>
              </span>
            ),
          )}
        </div>
      )}
    </div>
  )
}
```

Add `useRef` to the file's React import.

- [ ] **Step 3: Render `ItemConFlyout` instead of the accordion markup when `colapsada` is true**

In the `seccion.items.map(...)` body from Task 21, wrap the existing `if (item.subItems) { ... }` branch's return with a check on `colapsada`:

```tsx
              if (item.subItems) {
                if (colapsada) {
                  return (
                    <ItemConFlyout key={item.etiqueta} item={item} ubicacion={ubicacion} onNavegar={onCerrar} />
                  )
                }
                // ... el resto del bloque de Task 21 sin cambios, para el modo expandido
```

Also wrap the plain `item.ruta` and pending-leaf branches (the `return item.ruta ? (...) : (...)` at the end) so that in `colapsada` mode they render icon-only (no `<item.Icono .../>` text label, since `.panel-lateral-item-texto` is what carries the label and CSS will hide it — see Step 4, this can stay as unconditional JSX and let CSS handle the visual collapse for these leaf items specifically, since they have no flyout to build).

- [ ] **Step 4: Add the rail-mode CSS**

```css
.panel-lateral-colapsar {
  width: 28px;
  height: 28px;
  border-radius: 999px;
  border: none;
  background: rgba(255,255,255,.12);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  align-self: flex-end;
  margin: var(--espacio-2) var(--espacio-2) 0 0;
}
.panel-lateral-colapsar-icono { transition: transform .15s ease; }
.panel-lateral-colapsar-icono.girado { transform: rotate(180deg); }

.panel-lateral.colapsada { width: 64px; }
.panel-lateral.colapsada .panel-lateral-marca-texto,
.panel-lateral.colapsada .panel-lateral-titulo,
.panel-lateral.colapsada .panel-lateral-item-texto,
.panel-lateral.colapsada .panel-lateral-pendiente {
  display: none;
}
.panel-lateral.colapsada .panel-lateral-item { justify-content: center; }

.panel-lateral-riel-item { position: relative; }
.panel-lateral-item-riel {
  width: 100%;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  justify-content: center;
  padding: var(--espacio-2) 0;
  color: inherit;
}
.panel-lateral-item-riel.activo { color: var(--color-marca-claro, #fff); }
.panel-lateral-item-riel:disabled { cursor: default; opacity: .5; }

.panel-lateral-flyout {
  position: absolute;
  left: 100%;
  top: 0;
  margin-left: var(--espacio-2);
  min-width: 220px;
  background: var(--color-superficie, #fff);
  color: var(--color-texto, #1a1a1a);
  border-radius: var(--radio-2, 8px);
  box-shadow: 0 8px 24px rgba(0,0,0,.18);
  padding: var(--espacio-2);
  z-index: 40;
}
.panel-lateral-flyout-titulo {
  font-weight: 700;
  font-size: 12px;
  opacity: .6;
  padding: var(--espacio-1) var(--espacio-2);
}
.panel-lateral-flyout .panel-lateral-subitem {
  display: flex;
  align-items: center;
  gap: var(--espacio-2);
  padding: var(--espacio-2);
  border-radius: var(--radio-1, 6px);
  color: inherit;
  text-decoration: none;
}
.panel-lateral-flyout .panel-lateral-subitem:hover { background: var(--color-superficie-hover, #f2f2f2); }
.panel-lateral-flyout .panel-lateral-subitem.activo { background: var(--color-marca); color: #fff; }

/* El modo riel es solo de escritorio -- en movil la barra sigue siendo
   el cajon abierto/cerrado de siempre, sin el toggle de colapso. */
@media (max-width: 900px) {
  .panel-lateral-colapsar { display: none; }
  .panel-lateral.colapsada { width: auto; }
  .panel-lateral.colapsada .panel-lateral-marca-texto,
  .panel-lateral.colapsada .panel-lateral-titulo,
  .panel-lateral.colapsada .panel-lateral-item-texto,
  .panel-lateral.colapsada .panel-lateral-pendiente {
    display: revert;
  }
}
```

Check `rikos-web/src/estilos/tokens.css` for the actual token names (`--color-superficie`, `--color-superficie-hover`, `--radio-1`, `--radio-2`, `--color-marca-claro`) before finalizing this block — the names above are best guesses based on the naming convention seen elsewhere (`--color-marca`, `--espacio-2`) and must be corrected to match whatever the tokens file actually defines; do not invent new tokens if equivalents already exist.

- [ ] **Step 5: Verify in the browser (desktop width)**

- Click the collapse toggle: the sidebar shrinks to icon-only width, labels disappear.
- Hover over "Catálogo y recetas": after a brief moment, a flyout panel appears to the right with Productos/Categorías/Unidades y presentaciones (clickable) and Recetas (pending).
- Move the cursor into the flyout itself: it stays open (doesn't close just because the cursor left the icon).
- Move the cursor away from both the icon and the flyout: it closes after the delay.
- Click a flyout item: navigates and the flyout closes.
- Tab to the collapsed "Catálogo y recetas" button with the keyboard, press Enter: flyout opens. Press Escape: flyout closes.
- Reload the page with the sidebar collapsed: it stays collapsed (localStorage persisted).
- Resize to mobile width: the collapse toggle disappears, the sidebar behaves exactly as it did before this task (full-width drawer, no rail mode).

- [ ] **Step 6: Commit**

```bash
git add rikos-web/src/apps/admin/BarraLateral.tsx rikos-web/src/estilos/global.css
git commit -m "feat(navegacion): modo riel colapsado a iconos con submenu flotante"
```

---

## Phase E — Pantallas

### Task 23: `cliente.ts` — API functions for the whole `catalogo` module

**Files:**
- Modify: `rikos-web/src/api/cliente.ts`

**Interfaces:**
- Consumes: `pedirAutenticado` (existing), the generated types from Task 15.
- Produces: `listarCategorias`, `crearCategoria`, `actualizarCategoria`, `listarUnidades`, `crearUnidad`, `listarProductos`, `crearProducto`, `actualizarProducto`, `desactivarProducto`, `activarProducto`, `registrarCostoManual`, `asignarServiciosProducto`, `listarPresentaciones`, `crearPresentacion`, `listarConversiones`, `crearConversion`, `listarProveedores`, `crearProveedor`, `actualizarProveedor`, `desactivarProveedor`, `activarProveedor` — every one of these is what Tasks 25–30's screens import.

- [ ] **Step 1: Add the functions**

Append to `rikos-web/src/api/cliente.ts` (following the file's existing section-comment convention, e.g. `// ---- Autenticacion (US-002, US-003) ----`):

```typescript
// ---- Catalogo (US-008, US-009, US-011) ---------------------------------

export type Categoria = Esquemas['CategoriaRespuesta']
export type Unidad = Esquemas['UnidadRespuesta']
export type Producto = Esquemas['ProductoRespuesta']
export type Presentacion = Esquemas['PresentacionRespuesta']
export type ConversionUnidad = Esquemas['ConversionRespuesta']
export type Proveedor = Esquemas['ProveedorRespuesta']

export const listarCategorias = (token: string) => pedirAutenticado<Categoria[]>('/api/categorias', token)

export const crearCategoria = (
  token: string,
  datos: { nombre: string; naturaleza?: string; componenteMenu?: string; categoriaPadreId?: string; orden: number },
) => pedirAutenticado<Categoria>('/api/categorias', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarCategoria = (token: string, id: string, datos: { nombre: string; orden: number }) =>
  pedirAutenticado<Categoria>(`/api/categorias/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const listarUnidades = (token: string) => pedirAutenticado<Unidad[]>('/api/unidades', token)

export const crearUnidad = (token: string, datos: { codigo: string; nombre: string; tipo: string }) =>
  pedirAutenticado<Unidad>('/api/unidades', token, { method: 'POST', body: JSON.stringify(datos) })

export const listarProductos = (token: string) => pedirAutenticado<Producto[]>('/api/productos', token)

export interface DatosProducto {
  codigo: string
  nombre: string
  categoriaId: string
  unidadBaseId: string
  esVendible: boolean
  esInsumo: boolean
  esSubreceta: boolean
  precioVenta: number | null
  controlaStock: boolean
}

export const crearProducto = (token: string, datos: DatosProducto) =>
  pedirAutenticado<Producto>('/api/productos', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarProducto = (token: string, id: string, datos: DatosProducto) =>
  pedirAutenticado<Producto>(`/api/productos/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const desactivarProducto = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/productos/${id}/desactivar`, token, { method: 'POST' })

export const activarProducto = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/productos/${id}/activar`, token, { method: 'POST' })

export const registrarCostoManual = (token: string, productoId: string, costo: number) =>
  pedirAutenticado<Producto>(`/api/productos/${productoId}/costo-manual`, token, {
    method: 'PUT',
    body: JSON.stringify({ costo }),
  })

export const asignarServiciosProducto = (token: string, productoId: string, servicios: string[]) =>
  pedirAutenticado<void>(`/api/productos/${productoId}/servicios`, token, {
    method: 'PUT',
    body: JSON.stringify({ servicios }),
  })

export const listarPresentaciones = (token: string, productoId: string) =>
  pedirAutenticado<Presentacion[]>(`/api/productos/${productoId}/presentaciones`, token)

export const crearPresentacion = (
  token: string,
  datos: { productoId: string; nombre: string; unidadId: string; factor: number; esPredeterminada: boolean },
) => pedirAutenticado<Presentacion>('/api/presentaciones', token, { method: 'POST', body: JSON.stringify(datos) })

export const listarConversiones = (token: string, productoId: string) =>
  pedirAutenticado<ConversionUnidad[]>(`/api/productos/${productoId}/conversiones`, token)

export const crearConversion = (
  token: string,
  datos: { productoId: string; unidadOrigenId: string; unidadDestinoId: string; factor: number },
) => pedirAutenticado<ConversionUnidad>('/api/conversiones', token, { method: 'POST', body: JSON.stringify(datos) })

export const listarProveedores = (token: string) => pedirAutenticado<Proveedor[]>('/api/proveedores', token)

export interface DatosProveedor {
  ruc: string | null
  razonSocial: string
  nombreComercial: string | null
  telefono: string | null
  correo: string | null
  direccion: string | null
  contacto: string | null
}

export const crearProveedor = (token: string, datos: DatosProveedor) =>
  pedirAutenticado<Proveedor>('/api/proveedores', token, { method: 'POST', body: JSON.stringify(datos) })

export const actualizarProveedor = (token: string, id: string, datos: DatosProveedor) =>
  pedirAutenticado<Proveedor>(`/api/proveedores/${id}`, token, { method: 'PUT', body: JSON.stringify(datos) })

export const desactivarProveedor = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/proveedores/${id}/desactivar`, token, { method: 'POST' })

export const activarProveedor = (token: string, id: string) =>
  pedirAutenticado<void>(`/api/proveedores/${id}/activar`, token, { method: 'POST' })
```

- [ ] **Step 2: Type-check**

Run: `npx tsc --noEmit` in `rikos-web/`.
Expected: clean (assuming Task 15's generated schema names match — if `CategoriaRespuesta`/`UnidadRespuesta`/etc. don't exist under that exact key in `Esquemas`, this is exactly the kind of drift Task 15 exists to catch; check `src/api/generado/esquema.d.ts` directly for the real key names and correct the `type X = Esquemas['...']` lines above to match).

- [ ] **Step 3: Commit**

```bash
git add rikos-web/src/api/cliente.ts
git commit -m "feat(catalogo): funciones de API para categorias, productos, unidades, proveedores"
```

---

### Task 24: Register routes in `apps/admin/index.tsx`

**Files:**
- Modify: `rikos-web/src/apps/admin/index.tsx`

**Interfaces:**
- Consumes: `PantallaCategorias`, `PantallaProductos`, `PantallaProductoFormulario`, `PantallaUnidadesYPresentaciones`, `PantallaProveedores`, `PantallaProveedorFormulario` (Tasks 25–30 — this task's routes reference components that don't exist until those tasks run; write this task last within Phase E in execution order, or accept temporary compile errors between this task and Task 30 if executing strictly in file order. Recommended: execute Task 24 immediately after Task 30, not before, even though it's numbered here for narrative flow with Task 20's route strings).

- [ ] **Step 1: Add the imports and routes**

In `rikos-web/src/apps/admin/index.tsx`, add to the import block:

```tsx
import { PantallaProductos, PantallaProductoFormulario, PantallaCategorias, PantallaUnidadesYPresentaciones } from '../../features/catalogo'
import { PantallaProveedores, PantallaProveedorFormulario } from '../../features/proveedores'
```

Add inside `<Routes>`, before the existing `<Route path="*" ...>`:

```tsx
          <Route path="catalogo/productos" element={<PantallaProductos />} />
          <Route path="catalogo/productos/nuevo" element={<PantallaProductoFormulario />} />
          <Route path="catalogo/productos/:id/editar" element={<PantallaProductoFormulario />} />
          <Route path="catalogo/categorias" element={<PantallaCategorias />} />
          <Route path="catalogo/unidades" element={<PantallaUnidadesYPresentaciones />} />
          <Route path="compras/proveedores" element={<PantallaProveedores />} />
          <Route path="compras/proveedores/nuevo" element={<PantallaProveedorFormulario />} />
          <Route path="compras/proveedores/:id/editar" element={<PantallaProveedorFormulario />} />
```

(Specific routes like `catalogo/productos/nuevo` are declared before the generic `catalogo/productos/:id/editar` would otherwise shadow them if reordered — matching this project's established route-ordering convention from Sprint 1.)

- [ ] **Step 2: Commit**

```bash
git add rikos-web/src/apps/admin/index.tsx
git commit -m "feat(catalogo): registra las rutas de productos, categorias, unidades y proveedores"
```

---

### Task 25: `PantallaCategorias.tsx`

**Files:**
- Create: `rikos-web/src/features/catalogo/PantallaCategorias.tsx`
- Create: `rikos-web/src/features/catalogo/index.ts` (barrel — Task 26–28 add to this same file)

**Interfaces:**
- Consumes: `listarCategorias`, `crearCategoria`, `actualizarCategoria` (Task 23), `Tabla`/`Boton`/`Campo`/`Select`/`Modal` (existing shared components), `useSesion` (existing session hook, same pattern as every other `Pantalla*.tsx`).
- Produces: `PantallaCategorias` component, exported via the barrel for Task 24's route registration.

- [ ] **Step 1: Write the screen**

```tsx
// rikos-web/src/features/catalogo/PantallaCategorias.tsx
import { useEffect, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, Campo, Modal, Select, Tabla, type ColumnaTabla } from '../../comun/componentes'
import { listarCategorias, crearCategoria, actualizarCategoria, type Categoria } from '../../api/cliente'

const NATURALEZAS = [
  { valor: 'componente_menu', etiqueta: 'Componente de menú' },
  { valor: 'producto_fijo', etiqueta: 'Producto fijo' },
  { valor: 'extra', etiqueta: 'Extra' },
  { valor: 'insumo', etiqueta: 'Insumo' },
  { valor: 'subreceta', etiqueta: 'Subreceta' },
]

const COMPONENTES_MENU = [
  { valor: 'entrada', etiqueta: 'Entrada' },
  { valor: 'segundo', etiqueta: 'Segundo' },
  { valor: 'guarnicion', etiqueta: 'Guarnición' },
  { valor: 'refresco', etiqueta: 'Refresco' },
  { valor: 'postre', etiqueta: 'Postre' },
]

export function PantallaCategorias() {
  const { sesion } = useSesion()
  const token = sesion!.token

  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [modalAbierto, setModalAbierto] = useState<'raiz' | 'hija' | null>(null)
  const [padreSeleccionado, setPadreSeleccionado] = useState<Categoria | null>(null)
  const [editando, setEditando] = useState<Categoria | null>(null)
  const [nombre, setNombre] = useState('')
  const [naturaleza, setNaturaleza] = useState(NATURALEZAS[0].valor)
  const [componenteMenu, setComponenteMenu] = useState('')
  const [guardando, setGuardando] = useState(false)
  const [errorGuardado, setErrorGuardado] = useState('')

  function cargar() {
    setCargando(true)
    setError('')
    listarCategorias(token)
      .then(setCategorias)
      .catch(() => setError('No se pudieron cargar las categorías.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  const raices = categorias.filter((c) => c.categoriaPadreId === null)
  const hijasDe = (padreId: string) => categorias.filter((c) => c.categoriaPadreId === padreId)

  function abrirRaiz() {
    setModalAbierto('raiz')
    setEditando(null)
    setNombre('')
    setNaturaleza(NATURALEZAS[0].valor)
    setComponenteMenu('')
    setErrorGuardado('')
  }

  function abrirHija(padre: Categoria) {
    setModalAbierto('hija')
    setPadreSeleccionado(padre)
    setEditando(null)
    setNombre('')
    setErrorGuardado('')
  }

  function abrirEdicion(categoria: Categoria) {
    setModalAbierto(categoria.categoriaPadreId ? 'hija' : 'raiz')
    setEditando(categoria)
    setNombre(categoria.nombre)
    setNaturaleza(categoria.naturaleza)
    setComponenteMenu(categoria.componenteMenu ?? '')
    setErrorGuardado('')
  }

  async function guardar() {
    setGuardando(true)
    setErrorGuardado('')
    try {
      if (editando) {
        await actualizarCategoria(token, editando.id, { nombre, orden: editando.orden })
      } else if (modalAbierto === 'raiz') {
        await crearCategoria(token, {
          nombre,
          naturaleza,
          componenteMenu: naturaleza === 'componente_menu' ? componenteMenu : undefined,
          orden: raices.length + 1,
        })
      } else if (padreSeleccionado) {
        await crearCategoria(token, {
          nombre,
          categoriaPadreId: padreSeleccionado.id,
          orden: hijasDe(padreSeleccionado.id).length + 1,
        })
      }
      setModalAbierto(null)
      cargar()
    } catch {
      setErrorGuardado('No se pudo guardar la categoría. Revisa que el nombre no esté repetido.')
    } finally {
      setGuardando(false)
    }
  }

  const columnas: ColumnaTabla<Categoria>[] = [
    { encabezado: 'Nombre', render: (c) => (c.categoriaPadreId ? `— ${c.nombre}` : c.nombre) },
    { encabezado: 'Naturaleza', render: (c) => c.naturaleza },
    { encabezado: 'Componente de menú', render: (c) => c.componenteMenu ?? '—' },
    {
      encabezado: 'Acciones',
      acciones: true,
      render: (c) => (
        <div style={{ display: 'flex', gap: 8 }}>
          {!c.categoriaPadreId && (
            <Boton variante="secundario" onClick={() => abrirHija(c)}>
              Agregar subcategoría
            </Boton>
          )}
          <Boton variante="secundario" onClick={() => abrirEdicion(c)}>
            Editar
          </Boton>
        </div>
      ),
    },
  ]

  const filasOrdenadas = raices.flatMap((raiz) => [raiz, ...hijasDe(raiz.id)])

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <h1>Categorías</h1>
        <Boton onClick={abrirRaiz}>Nueva categoría raíz</Boton>
      </div>

      <Tabla
        columnas={columnas}
        filas={filasOrdenadas}
        obtenerClave={(c) => c.id}
        cargando={cargando}
        error={error}
        vacioTitulo="Todavía no hay categorías"
        vacioDescripcion="Crea la primera categoría raíz para empezar a organizar el catálogo."
        vacioAccion={<Boton onClick={abrirRaiz}>Nueva categoría raíz</Boton>}
      />

      {modalAbierto && (
        <Modal
          abierto
          titulo={editando ? 'Editar categoría' : modalAbierto === 'raiz' ? 'Nueva categoría raíz' : `Nueva subcategoría de ${padreSeleccionado?.nombre}`}
          onCerrar={() => setModalAbierto(null)}
        >
          <Campo etiqueta="Nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} required />
          {modalAbierto === 'raiz' && !editando && (
            <>
              <Select etiqueta="Naturaleza" value={naturaleza} onChange={(e) => setNaturaleza(e.target.value)}>
                {NATURALEZAS.map((n) => (
                  <option key={n.valor} value={n.valor}>
                    {n.etiqueta}
                  </option>
                ))}
              </Select>
              {naturaleza === 'componente_menu' && (
                <Select etiqueta="Componente de menú" value={componenteMenu} onChange={(e) => setComponenteMenu(e.target.value)}>
                  <option value="">— Elegir —</option>
                  {COMPONENTES_MENU.map((c) => (
                    <option key={c.valor} value={c.valor}>
                      {c.etiqueta}
                    </option>
                  ))}
                </Select>
              )}
            </>
          )}
          {errorGuardado && <p className="campo-error">{errorGuardado}</p>}
          <Boton onClick={guardar} cargando={guardando}>
            Guardar
          </Boton>
        </Modal>
      )}
    </section>
  )
}
```

- [ ] **Step 2: Create the barrel file**

```typescript
// rikos-web/src/features/catalogo/index.ts
export { PantallaCategorias } from './PantallaCategorias'
```

(Tasks 26–28 append their own exports to this same file.)

- [ ] **Step 3: Verify in the browser**

Navigate to `/admin/catalogo/categorias` (after Task 24's routes are registered — if executing in strict task order, temporarily add this one route early to verify, or defer this verification step until Task 24 actually runs). Confirm: 9 root categories render each with their subcategories indented below with an em-dash prefix, "Nueva categoría raíz" opens a modal with naturaleza + componente_menu fields, adding a subcategory to an existing root works and does NOT show naturaleza/componente_menu fields (inherited, not chosen).

- [ ] **Step 4: Commit**

```bash
git add rikos-web/src/features/catalogo/PantallaCategorias.tsx rikos-web/src/features/catalogo/index.ts
git commit -m "feat(catalogo): pantalla de categorias"
```

---

### Task 26: `PantallaProductos.tsx` — list, search, filters, explanatory empty state

**Files:**
- Create: `rikos-web/src/features/catalogo/PantallaProductos.tsx`
- Modify: `rikos-web/src/features/catalogo/index.ts`

**Interfaces:**
- Consumes: `listarProductos`, `listarCategorias`, `desactivarProducto`, `activarProducto` (Task 23), `Tabla`, `Campo`, `Select`, `ModalDeConfirmacion` (existing).
- Produces: `PantallaProductos`, exported via the barrel.

- [ ] **Step 1: Write the screen**

```tsx
// rikos-web/src/features/catalogo/PantallaProductos.tsx
import { useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, Campo, ModalDeConfirmacion, Select, Tabla, type ColumnaTabla } from '../../comun/componentes'
import {
  listarProductos,
  listarCategorias,
  desactivarProducto,
  activarProducto,
  type Producto,
  type Categoria,
} from '../../api/cliente'

export function PantallaProductos() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [productos, setProductos] = useState<Producto[]>([])
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [busqueda, setBusqueda] = useState('')
  const [filtroCategoria, setFiltroCategoria] = useState('')
  const [filtroNaturaleza, setFiltroNaturaleza] = useState('')
  const [filtroEstado, setFiltroEstado] = useState<'todos' | 'activos' | 'inactivos'>('activos')
  const [paraCambiarEstado, setParaCambiarEstado] = useState<Producto | null>(null)

  function cargar() {
    setCargando(true)
    setError('')
    Promise.all([listarProductos(token), listarCategorias(token)])
      .then(([p, c]) => {
        setProductos(p)
        setCategorias(c)
      })
      .catch(() => setError('No se pudieron cargar los productos.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  const filtrados = useMemo(() => {
    return productos.filter((p) => {
      if (busqueda && !p.nombre.toLowerCase().includes(busqueda.toLowerCase()) && !p.codigo.toLowerCase().includes(busqueda.toLowerCase())) {
        return false
      }
      if (filtroCategoria && p.categoriaId !== filtroCategoria) return false
      if (filtroNaturaleza === 'vendible' && !p.esVendible) return false
      if (filtroNaturaleza === 'insumo' && !p.esInsumo) return false
      if (filtroNaturaleza === 'subreceta' && !p.esSubreceta) return false
      if (filtroEstado === 'activos' && !p.activo) return false
      if (filtroEstado === 'inactivos' && p.activo) return false
      return true
    })
  }, [productos, busqueda, filtroCategoria, filtroNaturaleza, filtroEstado])

  async function confirmarCambioEstado() {
    if (!paraCambiarEstado) return
    if (paraCambiarEstado.activo) {
      await desactivarProducto(token, paraCambiarEstado.id)
    } else {
      await activarProducto(token, paraCambiarEstado.id)
    }
    setParaCambiarEstado(null)
    cargar()
  }

  const columnas: ColumnaTabla<Producto>[] = [
    { encabezado: 'Código', render: (p) => p.codigo },
    { encabezado: 'Nombre', render: (p) => p.nombre },
    { encabezado: 'Categoría', render: (p) => p.categoriaNombre },
    {
      encabezado: 'Naturaleza',
      render: (p) =>
        [p.esVendible && 'Vendible', p.esInsumo && 'Insumo', p.esSubreceta && 'Subreceta'].filter(Boolean).join(' · ') || '—',
    },
    { encabezado: 'Estado', render: (p) => (p.activo ? 'Activo' : 'Inactivo') },
    {
      encabezado: 'Acciones',
      acciones: true,
      render: (p) => (
        <div style={{ display: 'flex', gap: 8 }}>
          <Boton variante="secundario" onClick={() => navigate(`/admin/catalogo/productos/${p.id}/editar`)}>
            Editar
          </Boton>
          <Boton variante={p.activo ? 'peligro' : 'secundario'} onClick={() => setParaCambiarEstado(p)}>
            {p.activo ? 'Desactivar' : 'Activar'}
          </Boton>
        </div>
      ),
    },
  ]

  const hayCategoriasHoja = categorias.some((c) => c.esHoja)

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <h1>Productos</h1>
        <Boton onClick={() => navigate('/admin/catalogo/productos/nuevo')} disabled={!hayCategoriasHoja}>
          Nuevo producto
        </Boton>
      </div>

      <div className="panel-admin-filtros">
        <Campo etiqueta="Buscar" placeholder="Código o nombre" value={busqueda} onChange={(e) => setBusqueda(e.target.value)} />
        <Select etiqueta="Categoría" value={filtroCategoria} onChange={(e) => setFiltroCategoria(e.target.value)}>
          <option value="">Todas</option>
          {categorias.filter((c) => c.esHoja).map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombre}
            </option>
          ))}
        </Select>
        <Select etiqueta="Naturaleza" value={filtroNaturaleza} onChange={(e) => setFiltroNaturaleza(e.target.value)}>
          <option value="">Todas</option>
          <option value="vendible">Vendible</option>
          <option value="insumo">Insumo</option>
          <option value="subreceta">Subreceta</option>
        </Select>
        <Select etiqueta="Estado" value={filtroEstado} onChange={(e) => setFiltroEstado(e.target.value as typeof filtroEstado)}>
          <option value="activos">Activos</option>
          <option value="inactivos">Inactivos</option>
          <option value="todos">Todos</option>
        </Select>
      </div>

      <Tabla
        columnas={columnas}
        filas={filtrados}
        obtenerClave={(p) => p.id}
        cargando={cargando}
        error={error}
        vacioTitulo={
          !hayCategoriasHoja && productos.length === 0
            ? 'Primero crea una categoría'
            : productos.length === 0
              ? 'Todavía no hay productos'
              : 'Nada coincide con el filtro'
        }
        vacioDescripcion={
          !hayCategoriasHoja && productos.length === 0
            ? 'Un producto necesita una categoría hoja para colgar de ella. Ve a Categorías y crea al menos una subcategoría.'
            : productos.length === 0
              ? 'Crea el primer producto del catálogo: elige su categoría, su unidad base y si es vendible, insumo o subreceta.'
              : 'Prueba con otro texto de búsqueda o quita algún filtro.'
        }
        vacioAccion={
          !hayCategoriasHoja && productos.length === 0 ? (
            <Boton onClick={() => navigate('/admin/catalogo/categorias')}>Ir a Categorías</Boton>
          ) : productos.length === 0 ? (
            <Boton onClick={() => navigate('/admin/catalogo/productos/nuevo')}>Nuevo producto</Boton>
          ) : undefined
        }
      />

      {paraCambiarEstado && (
        <ModalDeConfirmacion
          abierto
          titulo={paraCambiarEstado.activo ? 'Desactivar producto' : 'Activar producto'}
          mensaje={
            paraCambiarEstado.activo
              ? `"${paraCambiarEstado.nombre}" dejará de poder usarse en registros nuevos. Su historial se conserva.`
              : `"${paraCambiarEstado.nombre}" volverá a estar disponible para registros nuevos.`
          }
          onConfirmar={confirmarCambioEstado}
          onCancelar={() => setParaCambiarEstado(null)}
        />
      )}
    </section>
  )
}
```

- [ ] **Step 2: Add to the barrel**

```typescript
// rikos-web/src/features/catalogo/index.ts (append)
export { PantallaProductos } from './PantallaProductos'
```

- [ ] **Step 3: Verify in the browser**

With zero products seeded (confirmed in Task 2 — the catalog starts empty), navigate to `/admin/catalogo/productos`: confirm the empty state explains creating a category first (since none exist beyond the seeded tree, which does have leaves — so actually confirm the *other* empty message: "Todavía no hay productos" with the "Nuevo producto" action, since Task 2's seed already includes leaf categories). Create a couple of test products, confirm search and each filter narrows the list correctly, confirm desactivar/activar round-trips with the confirmation modal.

- [ ] **Step 4: Commit**

```bash
git add rikos-web/src/features/catalogo/PantallaProductos.tsx rikos-web/src/features/catalogo/index.ts
git commit -m "feat(catalogo): pantalla de productos con busqueda, filtros y vacio explicativo"
```

---

### Task 27: `PantallaProductoFormulario.tsx` — create/edit with the three flags, leaf-only category picker, conditional cost block

**Files:**
- Create: `rikos-web/src/features/catalogo/PantallaProductoFormulario.tsx`
- Modify: `rikos-web/src/features/catalogo/index.ts`

**Interfaces:**
- Consumes: `listarCategorias`, `listarUnidades`, `listarProductos` (to preload when editing — no single-product GET endpoint was built in Task 11; find the product in the already-fetched list, same pattern `PantallaRolFormulario.tsx` used before its Task-3-of-the-prior-sprint fix, this time done right from the start), `crearProducto`, `actualizarProducto`, `registrarCostoManual` (Task 23).
- Produces: `PantallaProductoFormulario`, exported via the barrel.

- [ ] **Step 1: Write the screen**

```tsx
// rikos-web/src/features/catalogo/PantallaProductoFormulario.tsx
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, Campo, Checkbox, EstadoCargando, EstadoError, Select } from '../../comun/componentes'
import {
  listarCategorias,
  listarUnidades,
  listarProductos,
  crearProducto,
  actualizarProducto,
  registrarCostoManual,
  type Categoria,
  type Unidad,
  type Producto,
  type DatosProducto,
} from '../../api/cliente'

export function PantallaProductoFormulario() {
  const { id } = useParams<{ id: string }>()
  const esNuevo = id === undefined
  const navigate = useNavigate()
  const { sesion } = useSesion()
  const token = sesion!.token

  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [unidades, setUnidades] = useState<Unidad[]>([])
  const [productoExistente, setProductoExistente] = useState<Producto | null>(null)

  const [codigo, setCodigo] = useState('')
  const [nombre, setNombre] = useState('')
  const [categoriaId, setCategoriaId] = useState('')
  const [unidadBaseId, setUnidadBaseId] = useState('')
  const [esVendible, setEsVendible] = useState(false)
  const [esInsumo, setEsInsumo] = useState(false)
  const [esSubreceta, setEsSubreceta] = useState(false)
  const [precioVentaSoles, setPrecioVentaSoles] = useState('')
  const [controlaStock, setControlaStock] = useState(true)
  const [costoInicial, setCostoInicial] = useState('')

  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setCargando(true)
    setErrorCarga('')
    Promise.all([listarCategorias(token), listarUnidades(token), esNuevo ? Promise.resolve([]) : listarProductos(token)])
      .then(([cats, unids, productos]) => {
        setCategorias(cats)
        setUnidades(unids)
        if (!esNuevo) {
          const p = productos.find((x) => x.id === id)
          if (!p) throw new Error('producto no encontrado')
          setProductoExistente(p)
          setCodigo(p.codigo)
          setNombre(p.nombre)
          setCategoriaId(p.categoriaId)
          setUnidadBaseId(p.unidadBaseId)
          setEsVendible(p.esVendible)
          setEsInsumo(p.esInsumo)
          setEsSubreceta(p.esSubreceta)
          setPrecioVentaSoles(p.precioVenta != null ? (p.precioVenta / 100).toFixed(2) : '')
          setControlaStock(p.controlaStock)
        }
      })
      .catch(() => setErrorCarga('No se pudo cargar el producto.'))
      .finally(() => setCargando(false))
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, esNuevo, token])

  async function guardar() {
    setError('')
    if (esVendible && !precioVentaSoles) {
      setError('El precio de venta es obligatorio para un producto vendible.')
      return
    }
    setGuardando(true)
    try {
      const datos: DatosProducto = {
        codigo,
        nombre,
        categoriaId,
        unidadBaseId,
        esVendible,
        esInsumo,
        esSubreceta,
        precioVenta: esVendible ? Math.round(parseFloat(precioVentaSoles) * 100) : null,
        controlaStock,
      }
      let idFinal = id
      if (esNuevo) {
        const creado = await crearProducto(token, datos)
        idFinal = creado.id
      } else {
        await actualizarProducto(token, id!, datos)
      }
      if (esInsumo && costoInicial && idFinal) {
        await registrarCostoManual(token, idFinal, parseFloat(costoInicial))
      }
      navigate('/admin/catalogo/productos')
    } catch {
      setError('No se pudo guardar el producto. Revisa que el código no esté repetido.')
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga) return <EstadoError mensaje={errorCarga} />

  const categoriasHoja = categorias.filter((c) => c.esHoja)
  const costoEsManualEditable = !productoExistente || productoExistente.origenCosto !== 'compra'

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <h1>{esNuevo ? 'Nuevo producto' : 'Editar producto'}</h1>
        <Boton onClick={guardar} cargando={guardando}>
          {esNuevo ? 'Crear producto' : 'Guardar cambios'}
        </Boton>
      </div>

      <div className="panel-admin-tarjeta">
        <Campo etiqueta="Código" value={codigo} onChange={(e) => setCodigo(e.target.value)} required />
        <Campo etiqueta="Nombre" value={nombre} onChange={(e) => setNombre(e.target.value)} required />

        <Select etiqueta="Categoría (hoja)" value={categoriaId} onChange={(e) => setCategoriaId(e.target.value)} required>
          <option value="">— Elegir —</option>
          {categoriasHoja.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombre}
            </option>
          ))}
        </Select>

        <Select etiqueta="Unidad base" value={unidadBaseId} onChange={(e) => setUnidadBaseId(e.target.value)} required>
          <option value="">— Elegir —</option>
          {unidades.map((u) => (
            <option key={u.id} value={u.id}>
              {u.nombre} ({u.codigo})
            </option>
          ))}
        </Select>

        <div className="panel-admin-checkboxes">
          <Checkbox etiqueta="Es vendible" checked={esVendible} onChange={(e) => setEsVendible(e.target.checked)} />
          <Checkbox etiqueta="Es insumo" checked={esInsumo} onChange={(e) => setEsInsumo(e.target.checked)} />
          <Checkbox etiqueta="Es subreceta" checked={esSubreceta} onChange={(e) => setEsSubreceta(e.target.checked)} />
        </div>

        {esVendible && (
          <Campo
            etiqueta="Precio de venta (S/)"
            type="number"
            step="0.01"
            value={precioVentaSoles}
            onChange={(e) => setPrecioVentaSoles(e.target.value)}
            required
          />
        )}

        <Checkbox etiqueta="Controla stock" checked={controlaStock} onChange={(e) => setControlaStock(e.target.checked)} />

        {esInsumo && (
          <div className="panel-admin-tarjeta-anidada">
            <Campo
              etiqueta="Costo inicial (S/, por unidad base)"
              type="number"
              step="0.000001"
              value={costoInicial}
              onChange={(e) => setCostoInicial(e.target.value)}
              disabled={!costoEsManualEditable}
              ayuda={
                costoEsManualEditable
                  ? 'Costo estimado: se reemplaza automáticamente con la primera compra registrada de este producto.'
                  : 'Este costo ya viene de una compra confirmada y no se puede editar a mano.'
              }
            />
          </div>
        )}

        {error && <p className="campo-error">{error}</p>}
      </div>
    </section>
  )
}
```

- [ ] **Step 2: Add to the barrel**

```typescript
// rikos-web/src/features/catalogo/index.ts (append)
export { PantallaProductoFormulario } from './PantallaProductoFormulario'
```

- [ ] **Step 3: Verify in the browser**

Create a new product: confirm the category dropdown only lists leaf categories (never a root that has subcategories), confirm the precio-venta field only appears when "Es vendible" is checked and blocks save without it, confirm the costo-inicial block only appears when "Es insumo" is checked and shows the "estimado" help text. Save it, edit it back, confirm every field round-trips correctly including the costo inicial.

- [ ] **Step 4: Commit**

```bash
git add rikos-web/src/features/catalogo/PantallaProductoFormulario.tsx rikos-web/src/features/catalogo/index.ts
git commit -m "feat(catalogo): formulario de producto con costo inicial condicional"
```

---

### Task 28: `PantallaUnidadesYPresentaciones.tsx` — units tab + per-product presentaciones/conversiones with live factor result

**Files:**
- Create: `rikos-web/src/features/catalogo/PantallaUnidadesYPresentaciones.tsx`
- Modify: `rikos-web/src/features/catalogo/index.ts`

**Interfaces:**
- Consumes: `listarUnidades`, `crearUnidad`, `listarProductos`, `listarPresentaciones`, `crearPresentacion`, `listarConversiones`, `crearConversion` (Task 23), `Tabs` (existing).
- Produces: `PantallaUnidadesYPresentaciones`, exported via the barrel.

- [ ] **Step 1: Write the screen**

```tsx
// rikos-web/src/features/catalogo/PantallaUnidadesYPresentaciones.tsx
import { useEffect, useState } from 'react'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, Campo, Select, Tabla, Tabs, type ColumnaTabla, type Pestana } from '../../comun/componentes'
import {
  listarUnidades,
  crearUnidad,
  listarProductos,
  listarPresentaciones,
  crearPresentacion,
  listarConversiones,
  crearConversion,
  type Unidad,
  type Producto,
  type Presentacion,
  type ConversionUnidad,
} from '../../api/cliente'

const PESTANAS: Pestana[] = [
  { id: 'unidades', etiqueta: 'Unidades' },
  { id: 'presentaciones', etiqueta: 'Presentaciones de compra' },
  { id: 'conversiones', etiqueta: 'Conversiones' },
]

export function PantallaUnidadesYPresentaciones() {
  const { sesion } = useSesion()
  const token = sesion!.token

  const [pestana, setPestana] = useState('unidades')
  const [unidades, setUnidades] = useState<Unidad[]>([])
  const [productos, setProductos] = useState<Producto[]>([])
  const [productoId, setProductoId] = useState('')
  const [presentaciones, setPresentaciones] = useState<Presentacion[]>([])
  const [conversiones, setConversiones] = useState<ConversionUnidad[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')

  function cargarBase() {
    setCargando(true)
    setError('')
    Promise.all([listarUnidades(token), listarProductos(token)])
      .then(([u, p]) => {
        setUnidades(u)
        setProductos(p.filter((x) => x.esInsumo))
      })
      .catch(() => setError('No se pudieron cargar las unidades.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargarBase, [token])

  useEffect(() => {
    if (!productoId) return
    listarPresentaciones(token, productoId).then(setPresentaciones)
    listarConversiones(token, productoId).then(setConversiones)
  }, [productoId, token])

  // ---- Unidades -----------------------------------------------------
  const [codigoUnidad, setCodigoUnidad] = useState('')
  const [nombreUnidad, setNombreUnidad] = useState('')
  const [tipoUnidad, setTipoUnidad] = useState('masa')

  async function guardarUnidad() {
    await crearUnidad(token, { codigo: codigoUnidad, nombre: nombreUnidad, tipo: tipoUnidad })
    setCodigoUnidad('')
    setNombreUnidad('')
    cargarBase()
  }

  const columnasUnidades: ColumnaTabla<Unidad>[] = [
    { encabezado: 'Código', render: (u) => u.codigo },
    { encabezado: 'Nombre', render: (u) => u.nombre },
    { encabezado: 'Tipo', render: (u) => u.tipo },
  ]

  // ---- Presentaciones -------------------------------------------------
  const [nombrePresentacion, setNombrePresentacion] = useState('')
  const [unidadPresentacionId, setUnidadPresentacionId] = useState('')
  const [factorPresentacion, setFactorPresentacion] = useState('')
  const [esPredeterminada, setEsPredeterminada] = useState(false)

  async function guardarPresentacion() {
    await crearPresentacion(token, {
      productoId,
      nombre: nombrePresentacion,
      unidadId: unidadPresentacionId,
      factor: parseFloat(factorPresentacion),
      esPredeterminada,
    })
    setNombrePresentacion('')
    setFactorPresentacion('')
    listarPresentaciones(token, productoId).then(setPresentaciones)
  }

  const productoSeleccionado = productos.find((p) => p.id === productoId)
  const unidadBaseSeleccionada = unidades.find((u) => u.id === productoSeleccionado?.unidadBaseId)

  // ---- Conversiones ----------------------------------------------------
  const [unidadOrigenId, setUnidadOrigenId] = useState('')
  const [unidadDestinoId, setUnidadDestinoId] = useState('')
  const [factorConversion, setFactorConversion] = useState('')

  async function guardarConversion() {
    await crearConversion(token, {
      productoId,
      unidadOrigenId,
      unidadDestinoId,
      factor: parseFloat(factorConversion),
    })
    setFactorConversion('')
    listarConversiones(token, productoId).then(setConversiones)
  }

  return (
    <section className="panel-admin">
      <h1>Unidades y presentaciones</h1>
      <Tabs pestanas={PESTANAS} activa={pestana} onCambiar={setPestana} />

      {pestana === 'unidades' && (
        <div className="panel-admin-tarjeta">
          <div className="panel-admin-fila-formulario">
            <Campo etiqueta="Código" value={codigoUnidad} onChange={(e) => setCodigoUnidad(e.target.value)} />
            <Campo etiqueta="Nombre" value={nombreUnidad} onChange={(e) => setNombreUnidad(e.target.value)} />
            <Select etiqueta="Tipo" value={tipoUnidad} onChange={(e) => setTipoUnidad(e.target.value)}>
              <option value="masa">Masa</option>
              <option value="volumen">Volumen</option>
              <option value="unidad">Unidad</option>
            </Select>
            <Boton onClick={guardarUnidad}>Agregar unidad</Boton>
          </div>
          <Tabla columnas={columnasUnidades} filas={unidades} obtenerClave={(u) => u.id} cargando={cargando} error={error} />
        </div>
      )}

      {(pestana === 'presentaciones' || pestana === 'conversiones') && (
        <div className="panel-admin-tarjeta">
          <Select etiqueta="Producto (insumo)" value={productoId} onChange={(e) => setProductoId(e.target.value)}>
            <option value="">— Elegir —</option>
            {productos.map((p) => (
              <option key={p.id} value={p.id}>
                {p.nombre}
              </option>
            ))}
          </Select>

          {productoId && pestana === 'presentaciones' && (
            <>
              <div className="panel-admin-fila-formulario">
                <Campo etiqueta="Nombre" placeholder="Saco 50 kg" value={nombrePresentacion} onChange={(e) => setNombrePresentacion(e.target.value)} />
                <Select etiqueta="Unidad" value={unidadPresentacionId} onChange={(e) => setUnidadPresentacionId(e.target.value)}>
                  <option value="">— Elegir —</option>
                  {unidades.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.nombre}
                    </option>
                  ))}
                </Select>
                <Campo etiqueta="Factor" type="number" step="0.000001" value={factorPresentacion} onChange={(e) => setFactorPresentacion(e.target.value)} />
                <label className="opcion">
                  <input type="checkbox" className="opcion-control" checked={esPredeterminada} onChange={(e) => setEsPredeterminada(e.target.checked)} />
                  <span className="opcion-etiqueta">Predeterminada</span>
                </label>
                <Boton onClick={guardarPresentacion}>Agregar</Boton>
              </div>
              {unidadPresentacionId && factorPresentacion && unidadBaseSeleccionada && (
                <p className="campo-ayuda">
                  1 {nombrePresentacion || 'presentación'} = {factorPresentacion} {unidadBaseSeleccionada.codigo}
                </p>
              )}
              <ul className="lista-simple">
                {presentaciones.map((p) => (
                  <li key={p.id}>
                    {p.nombre} — factor {p.factorAUnidadBase} {p.esPredeterminada && '(predeterminada)'}
                  </li>
                ))}
              </ul>
            </>
          )}

          {productoId && pestana === 'conversiones' && (
            <>
              <div className="panel-admin-fila-formulario">
                <Select etiqueta="De" value={unidadOrigenId} onChange={(e) => setUnidadOrigenId(e.target.value)}>
                  <option value="">— Elegir —</option>
                  {unidades.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.nombre}
                    </option>
                  ))}
                </Select>
                <Select etiqueta="A" value={unidadDestinoId} onChange={(e) => setUnidadDestinoId(e.target.value)}>
                  <option value="">— Elegir —</option>
                  {unidades.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.nombre}
                    </option>
                  ))}
                </Select>
                <Campo etiqueta="Factor" type="number" step="0.000001" value={factorConversion} onChange={(e) => setFactorConversion(e.target.value)} />
                <Boton onClick={guardarConversion}>Agregar</Boton>
              </div>
              {unidadOrigenId && unidadDestinoId && factorConversion && (
                <p className="campo-ayuda">
                  1 {unidades.find((u) => u.id === unidadOrigenId)?.codigo} = {factorConversion}{' '}
                  {unidades.find((u) => u.id === unidadDestinoId)?.codigo}
                </p>
              )}
              <ul className="lista-simple">
                {conversiones.map((c) => (
                  <li key={c.id}>
                    {unidades.find((u) => u.id === c.unidadOrigenId)?.codigo} → {unidades.find((u) => u.id === c.unidadDestinoId)?.codigo}: {c.factor}
                  </li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}
    </section>
  )
}
```

- [ ] **Step 2: Add the `.lista-simple`/`.panel-admin-fila-formulario` CSS if not already present**

Check `rikos-web/src/estilos/global.css` for an existing `.panel-admin-fila-formulario` or equivalent inline-form-row pattern (Sprint 1's `Pantalla*Formulario.tsx` files likely already have something similar for grouped inline fields) — reuse it if it exists under any name, or add a minimal one:

```css
.panel-admin-fila-formulario {
  display: flex;
  gap: var(--espacio-3);
  align-items: flex-end;
  flex-wrap: wrap;
}
.lista-simple { list-style: none; padding: 0; margin: var(--espacio-3) 0 0; }
.lista-simple li { padding: var(--espacio-2) 0; border-bottom: 1px solid var(--color-borde, #eee); }
```

- [ ] **Step 3: Add to the barrel**

```typescript
// rikos-web/src/features/catalogo/index.ts (append)
export { PantallaUnidadesYPresentaciones } from './PantallaUnidadesYPresentaciones'
```

- [ ] **Step 4: Verify in the browser**

Navigate to `/admin/catalogo/unidades`: confirm the 5 seeded units list on the "Unidades" tab, add a new one, confirm it appears. Switch to "Presentaciones de compra", pick a product, type a factor of 50 for a unit named "Saco 50 kg" — confirm the live text "1 Saco 50 kg = 50 kg" (or whatever unit) appears **while typing**, before clicking "Agregar". Same check on "Conversiones" with a "1 l = 0.92 kg"-style live preview.

- [ ] **Step 5: Commit**

```bash
git add rikos-web/src/features/catalogo/PantallaUnidadesYPresentaciones.tsx rikos-web/src/features/catalogo/index.ts rikos-web/src/estilos/global.css
git commit -m "feat(catalogo): pantalla de unidades, presentaciones y conversiones con resultado en vivo"
```

---

### Task 29: `PantallaProveedores.tsx`

**Files:**
- Create: `rikos-web/src/features/proveedores/PantallaProveedores.tsx`
- Create: `rikos-web/src/features/proveedores/index.ts`

**Interfaces:**
- Consumes: `listarProveedores`, `desactivarProveedor`, `activarProveedor` (Task 23).
- Produces: `PantallaProveedores`, exported via `features/proveedores`'s barrel (a separate feature folder from `catalogo`, matching the navigation decision that proveedores lives under "Compras y proveedores" — Task 24's import already reflects this as a separate module path).

- [ ] **Step 1: Write the screen**

```tsx
// rikos-web/src/features/proveedores/PantallaProveedores.tsx
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, ModalDeConfirmacion, Tabla, type ColumnaTabla } from '../../comun/componentes'
import { listarProveedores, desactivarProveedor, activarProveedor, type Proveedor } from '../../api/cliente'

export function PantallaProveedores() {
  const { sesion } = useSesion()
  const token = sesion!.token
  const navigate = useNavigate()

  const [proveedores, setProveedores] = useState<Proveedor[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState('')
  const [paraCambiarEstado, setParaCambiarEstado] = useState<Proveedor | null>(null)

  function cargar() {
    setCargando(true)
    setError('')
    listarProveedores(token)
      .then(setProveedores)
      .catch(() => setError('No se pudieron cargar los proveedores.'))
      .finally(() => setCargando(false))
  }

  useEffect(cargar, [token])

  async function confirmarCambioEstado() {
    if (!paraCambiarEstado) return
    if (paraCambiarEstado.activo) {
      await desactivarProveedor(token, paraCambiarEstado.id)
    } else {
      await activarProveedor(token, paraCambiarEstado.id)
    }
    setParaCambiarEstado(null)
    cargar()
  }

  const columnas: ColumnaTabla<Proveedor>[] = [
    { encabezado: 'RUC', render: (p) => p.ruc ?? '— (informal)' },
    { encabezado: 'Razón social', render: (p) => p.razonSocial },
    { encabezado: 'Nombre comercial', render: (p) => p.nombreComercial ?? '—' },
    { encabezado: 'Teléfono', render: (p) => p.telefono ?? '—' },
    { encabezado: 'Estado', render: (p) => (p.activo ? 'Activo' : 'Inactivo') },
    {
      encabezado: 'Acciones',
      acciones: true,
      render: (p) => (
        <div style={{ display: 'flex', gap: 8 }}>
          <Boton variante="secundario" onClick={() => navigate(`/admin/compras/proveedores/${p.id}/editar`)}>
            Editar
          </Boton>
          <Boton variante={p.activo ? 'peligro' : 'secundario'} onClick={() => setParaCambiarEstado(p)}>
            {p.activo ? 'Desactivar' : 'Activar'}
          </Boton>
        </div>
      ),
    },
  ]

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <h1>Proveedores</h1>
        <Boton onClick={() => navigate('/admin/compras/proveedores/nuevo')}>Nuevo proveedor</Boton>
      </div>

      <Tabla
        columnas={columnas}
        filas={proveedores}
        obtenerClave={(p) => p.id}
        cargando={cargando}
        error={error}
        vacioTitulo="Todavía no hay proveedores"
        vacioDescripcion="Registra tu primer proveedor. El RUC es opcional: también puedes registrar compras al mercado de abastos."
        vacioAccion={<Boton onClick={() => navigate('/admin/compras/proveedores/nuevo')}>Nuevo proveedor</Boton>}
      />

      {paraCambiarEstado && (
        <ModalDeConfirmacion
          abierto
          titulo={paraCambiarEstado.activo ? 'Desactivar proveedor' : 'Activar proveedor'}
          mensaje={
            paraCambiarEstado.activo
              ? `"${paraCambiarEstado.razonSocial}" dejará de poder usarse en compras nuevas. Las compras anteriores se conservan.`
              : `"${paraCambiarEstado.razonSocial}" volverá a estar disponible para compras nuevas.`
          }
          onConfirmar={confirmarCambioEstado}
          onCancelar={() => setParaCambiarEstado(null)}
        />
      )}
    </section>
  )
}
```

- [ ] **Step 2: Create the barrel**

```typescript
// rikos-web/src/features/proveedores/index.ts
export { PantallaProveedores } from './PantallaProveedores'
```

(Task 30 appends its own export here.)

- [ ] **Step 3: Verify in the browser**

Navigate to `/admin/compras/proveedores`: confirm the empty state mentions the RUC-optional/mercado-de-abastos case, create a proveedor without a RUC, confirm it shows "— (informal)" in the list, confirm desactivar/activar round-trips with the confirmation modal explaining the consequence.

- [ ] **Step 4: Commit**

```bash
git add rikos-web/src/features/proveedores/PantallaProveedores.tsx rikos-web/src/features/proveedores/index.ts
git commit -m "feat(proveedores): pantalla de lista de proveedores"
```

---

### Task 30: `PantallaProveedorFormulario.tsx`

**Files:**
- Create: `rikos-web/src/features/proveedores/PantallaProveedorFormulario.tsx`
- Modify: `rikos-web/src/features/proveedores/index.ts`

**Interfaces:**
- Consumes: `listarProveedores` (to preload when editing, same no-single-GET pattern as Task 27), `crearProveedor`, `actualizarProveedor` (Task 23).
- Produces: `PantallaProveedorFormulario`, exported via the barrel.

- [ ] **Step 1: Write the screen**

```tsx
// rikos-web/src/features/proveedores/PantallaProveedorFormulario.tsx
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { useSesion } from '../../comun/sesion/SesionContexto'
import { Boton, Campo, EstadoCargando, EstadoError } from '../../comun/componentes'
import { listarProveedores, crearProveedor, actualizarProveedor, type DatosProveedor } from '../../api/cliente'

export function PantallaProveedorFormulario() {
  const { id } = useParams<{ id: string }>()
  const esNuevo = id === undefined
  const navigate = useNavigate()
  const { sesion } = useSesion()
  const token = sesion!.token

  const [cargando, setCargando] = useState(true)
  const [errorCarga, setErrorCarga] = useState('')

  const [ruc, setRuc] = useState('')
  const [razonSocial, setRazonSocial] = useState('')
  const [nombreComercial, setNombreComercial] = useState('')
  const [telefono, setTelefono] = useState('')
  const [correo, setCorreo] = useState('')
  const [direccion, setDireccion] = useState('')
  const [contacto, setContacto] = useState('')

  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    setCargando(true)
    setErrorCarga('')
    if (esNuevo) {
      setCargando(false)
      return
    }
    listarProveedores(token)
      .then((todos) => {
        const p = todos.find((x) => x.id === id)
        if (!p) throw new Error('proveedor no encontrado')
        setRuc(p.ruc ?? '')
        setRazonSocial(p.razonSocial)
        setNombreComercial(p.nombreComercial ?? '')
        setTelefono(p.telefono ?? '')
        setCorreo(p.correo ?? '')
        setDireccion(p.direccion ?? '')
        setContacto(p.contacto ?? '')
      })
      .catch(() => setErrorCarga('No se pudo cargar el proveedor.'))
      .finally(() => setCargando(false))
  }, [id, esNuevo, token])

  async function guardar() {
    setError('')
    setGuardando(true)
    try {
      const datos: DatosProveedor = {
        ruc: ruc || null,
        razonSocial,
        nombreComercial: nombreComercial || null,
        telefono: telefono || null,
        correo: correo || null,
        direccion: direccion || null,
        contacto: contacto || null,
      }
      if (esNuevo) {
        await crearProveedor(token, datos)
      } else {
        await actualizarProveedor(token, id!, datos)
      }
      navigate('/admin/compras/proveedores')
    } catch {
      setError('No se pudo guardar el proveedor. Revisa que el RUC tenga 11 dígitos y no esté repetido.')
    } finally {
      setGuardando(false)
    }
  }

  if (cargando) return <EstadoCargando />
  if (errorCarga) return <EstadoError mensaje={errorCarga} />

  return (
    <section className="panel-admin">
      <div className="panel-admin-encabezado">
        <h1>{esNuevo ? 'Nuevo proveedor' : 'Editar proveedor'}</h1>
        <Boton onClick={guardar} cargando={guardando}>
          {esNuevo ? 'Crear proveedor' : 'Guardar cambios'}
        </Boton>
      </div>

      <div className="panel-admin-tarjeta">
        <Campo
          etiqueta="RUC (opcional)"
          value={ruc}
          onChange={(e) => setRuc(e.target.value)}
          ayuda="Déjalo vacío para un proveedor informal, como el mercado de abastos."
        />
        <Campo etiqueta="Razón social" value={razonSocial} onChange={(e) => setRazonSocial(e.target.value)} required />
        <Campo etiqueta="Nombre comercial" value={nombreComercial} onChange={(e) => setNombreComercial(e.target.value)} />
        <Campo etiqueta="Teléfono" value={telefono} onChange={(e) => setTelefono(e.target.value)} />
        <Campo etiqueta="Correo" type="email" value={correo} onChange={(e) => setCorreo(e.target.value)} />
        <Campo etiqueta="Dirección" value={direccion} onChange={(e) => setDireccion(e.target.value)} />
        <Campo etiqueta="Persona de contacto" value={contacto} onChange={(e) => setContacto(e.target.value)} />

        {error && <p className="campo-error">{error}</p>}
      </div>
    </section>
  )
}
```

- [ ] **Step 2: Add to the barrel**

```typescript
// rikos-web/src/features/proveedores/index.ts (append)
export { PantallaProveedorFormulario } from './PantallaProveedorFormulario'
```

- [ ] **Step 3: Verify in the browser, then go back and execute Task 24 (route registration) now that every screen this phase needed exists**

Create a proveedor with a RUC, confirm validation rejects a malformed one (not 11 digits) with the backend's `RUC_INVALIDO` surfacing as the generic save-error message (a nicer field-level error is a reasonable future improvement, not required by this sprint's spec). Create one without a RUC, confirm it saves. Edit an existing one, confirm every field round-trips.

- [ ] **Step 4: Commit**

```bash
git add rikos-web/src/features/proveedores/PantallaProveedorFormulario.tsx rikos-web/src/features/proveedores/index.ts
git commit -m "feat(proveedores): formulario de alta y edicion"
```

---

## Phase F — Verificación final y cierre

### Task 31: Full backend and frontend verification pass

**Files:** none created — this task only runs commands and confirms output.

**Interfaces:** none — this is the checkpoint that everything from Tasks 1–30 is internally consistent.

- [ ] **Step 1: Run the full backend test suite**

Run:
```bash
cd rikos-backend/servicio-operacion
../mvnw.cmd test
```
Expected: `BUILD SUCCESS`, `Tests run: N, Failures: 0, Errors: 0`. Record the exact `N` — it must be at least `23 + 5 (CrearCategoriaTest) + 7 (CrearProductoTest) + 2 (AsignarServiciosProductoTest) + 3 (CrearPresentacionTest) + 3 (CrearConversionTest) + 3 (CrearProveedorTest) + 6 (CatalogoAccesoPorRolTest) = 52` (the exact final count from actually running the suite is what goes in Task 33's historia doc, not this arithmetic — recompute from the real test run output).

- [ ] **Step 2: Run the full frontend type-check**

Run:
```bash
cd rikos-web
npx tsc --noEmit
```
Expected: no errors. If Task 15's generated-type migration left any mismatch unresolved, fix it here before proceeding — do not carry a red `tsc` into the historia doc.

- [ ] **Step 3: Confirm seed counts directly against the database**

Run:
```bash
docker exec rikos-postgres psql -U rikos -d rikos_operacion -c "select count(*) from unidad_medida; select count(*) from categoria_producto where nivel=1; select count(*) from categoria_producto where nivel=2; select count(*) from producto; select count(*) from proveedor;"
```
Expected: `5`, `9`, `28`, and whatever count of test products/proveedores accumulated from manual verification during Tasks 26/29 (report the real number, don't assume zero — the manual browser checks in earlier tasks created real rows in the dev database, which is fine and expected, just report it accurately).

- [ ] **Step 4: No commit — this task only verifies. If any check fails, fix the underlying task's code, re-run that task's own test, then re-run this task's three checks from the top.**

---

### Task 32: Screenshots for the sprint checklist

**Files:** none created directly by this task — screenshots are captured via the browser tool available at execution time (this plan does not prescribe which tool, since that depends on what's available in the executing session; the prior sprint's screenshots were captured via Playwright when a browser MCP tool with disk-save capability was available, and via manual description when it wasn't — follow whichever is available, and if neither can save to disk, say so explicitly in the historia doc rather than silently omitting the images, matching how the previous sprint's session handled the same limitation).

**Interfaces:** none.

- [ ] **Step 1: Capture the four required screens**

With the dev server running and logged in as `admin`:
1. `/admin/catalogo/productos` — with at least one product created (from Task 26's verification), showing the list with search/filter controls visible.
2. `/admin/catalogo/productos` — with all filters cleared and, if possible, a second browser profile or a temporary `DELETE`-free way to view the empty state (e.g. filter to a nonsense search term to trigger "Nada coincide con el filtro", OR capture the *actual* first-run empty state before Task 26's verification step created any products — whichever is still reachable at this point in execution; if all products created during testing can't be cleanly removed without violating "never hard-delete", capture the filtered-to-empty variant and note in the historia doc that the true zero-product state was captured earlier, during Task 26's own verification, if a screenshot was taken then).
3. `/admin/catalogo/categorias` — showing the full seeded tree.
4. `/admin/catalogo/unidades` — the presentaciones tab mid-entry, showing the live "1 X = Y Z" text.
5. `/admin/compras/proveedores` — the list with at least one proveedor.

- [ ] **Step 2: Save to the project's screenshot convention**

Save under `rikos-backend/doc/capturas/catalogo/`, numbered following the existing convention from `doc/capturas/README.md` (e.g. `01-productos-lista.png`, `02-productos-vacio-o-filtrado.png`, `03-categorias-arbol.png`, `04-unidades-presentaciones-factor-vivo.png`, `05-proveedores-lista.png`).

- [ ] **Step 3: No commit for the images themselves if this repo situation matches the earlier sprint's (no `.git` present) — otherwise commit normally**

---

### Task 33: Write `doc/historias/US-008-US-009-US-011-catalogo.md`

**Files:**
- Create: `rikos-backend/doc/historias/US-008-US-009-US-011-catalogo.md`
- Modify: `rikos-backend/doc/historias/README.md` (add a row to its "Historias documentadas" table, per the convention already established there)

**Interfaces:**
- Consumes: the real output of Tasks 31 and 32 — every number and finding in this document must come from something that was actually run, not estimated while writing this plan.

- [ ] **Step 1: Write the historia doc, following the exact structure `doc/historias/README.md` already prescribes (Qué se pidió / Qué se construyó / Qué se dejó fuera y por qué / Cómo se verificó / Bugs encontrados en el camino)**

The content to include, sourced from this plan's execution:

- **Qué se pidió:** one paragraph summarizing the sprint 2 request (catálogo, unidades, proveedores, HT-05, HT-07, navegación 9bis) with a link back to this plan and to the spec.
- **Qué se construyó:** the 7 tables, the module structure, HT-05's generation pipeline, HT-07's `EntradaNumerica`, the full navigation tree with accordion + rail mode.
- **Qué se dejó fuera y por qué:** órdenes de compra/cuentas por pagar, importación masiva, the three deferred design decisions from the spec's "Decisiones de diseño" section, restated here with the reasoning.
- **Cómo se verificó:** the real test count from Task 31 Step 1, the real seed counts from Task 31 Step 3, the `tsc` clean result, and the manual browser verification notes accumulated across Tasks 25–30.
- **El costo inicial:** explain the `origen_costo` column decision (reused `costo_ultima_compra` + new `origen_costo` enum, not a wholly separate column) exactly as decided in the spec, with a note on where `registrarCostoDeCompra` will be called from once sprint 9 exists.
- **HT-05 demonstration:** the exact `tsc` error output captured in Task 15 Step 5, showing a renamed backend field breaking the frontend build.
- **Las 6 pruebas de rol:** paste the test names and confirm all green, referencing the real Task 31 Step 1 run.
- **Bugs encontrados en el camino:** anything found during Tasks 1–30's own verification steps that wasn't anticipated by this plan (if none, say so explicitly rather than omitting the section).
- **Rutas para volver a mirar en vivo:** the five URLs from Task 32.

- [ ] **Step 2: Add the row to `doc/historias/README.md`'s table**

```markdown
| `US-008-US-009-US-011-catalogo.md` | Catálogo, unidades, proveedores + HT-05/HT-07/navegación | Hecho, verificado |
```

- [ ] **Step 3: Commit (or note if no git repo, per this plan's earlier finding)**

```bash
git add doc/historias/US-008-US-009-US-011-catalogo.md doc/historias/README.md
git commit -m "docs: historia de US-008/US-009/US-011 (catalogo, unidades, proveedores)"
```

---
