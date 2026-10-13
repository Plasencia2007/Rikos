# Sprint 9 — Existencias reales

**Historias (del backlog):**
- **US-043:** Mantener existencias por local con stock mínimo y alertas (Entrega 1)
- **US-050:** Registrar compras y actualizar el costo de última compra (Entrega 2)
- **US-044:** Consultar el Kardex con origen rastreable (Entrega 3)
- **US-048:** Producir subrecetas y descontar receta al vender (Entrega 4)
- **US-045:** Registrar ajustes de inventario con motivo (Entrega 5)
- **US-049:** Registrar el destino de los sobrantes de producción (Entrega 6)
- **US-051:** Registrar el consumo interno del personal (Entrega 7)

Flujos en `flujos/08-ep08-inventario-produccion-compras.md`; modelo en `modelo-de-datos/09-inventario-produccion-compras--ep08.md`.

**Estado:** En curso — **Entrega 1: US-043 completada**. **Entrega 2: US-050 (Registrar compras y actualizar costo de última compra) completada**. Listo para continuar con la siguiente historia de EP-08.

**Cómo se llegó a esto:** Brainstorming y entrevista /grill-me con el usuario el 2026-10-12.
Cada decisión lleva su razón explícita y alternativas descartadas.

---

## 1. Qué queremos lograr

Dotar a cada local del restaurante de control de existencias en tiempo real sin entorpecer ni bloquear la operación del restaurante:
el administrador o encargado del local puede ver el stock actual de cada insumo o producto terminado, configurar el umbral de **stock mínimo** para su local, y recibir alertas inmediatas cuando un producto esté bajo el mínimo o en **stock negativo** (sin bloquear las ventas ni la cocina, respetando RN-050/RN-056).

### Éxito de la Entrega 1 (US-043)
1. Cada local tiene su propio inventario general con clave compuesta `(local_id, producto_id)` (RN-002, CA1).
2. El administrador de local o administrador central puede definir y actualizar el `stock_minimo` por producto y local en su unidad base (CA2, RN-051).
3. La interfaz (pantalla **ADM-16: Existencias**) muestra la lista completa de productos del local con indicadores claros:
   - **Normal:** `stock_actual > stock_minimo` (o sin mínimo configurado y stock >= 0).
   - **Bajo stock mínimo:** `0 <= stock_actual <= stock_minimo` (alerta ámbar preventiva, CA3).
   - **Stock negativo:** `stock_actual < 0` (alerta roja destacada sin bloqueo operacional, CA4, RN-050).
4. Pestañas de filtrado rápido: `Todos`, `Bajo mínimo`, `Negativos`, y buscador en vivo por nombre o código de producto.

---

## 2. Contexto y Decisiones de Diseño

### 2.1 Inicialización perezosa (*on-demand*) de existencias
- **Decisión:** Al consultar las existencias de un local, el sistema proyecta todos los productos activos del catálogo. Si un producto aún no registra movimientos ni configuración en la tabla física `existencia`, se muestra con stock `0` y stock mínimo no fijado. La fila en `existencia` se crea al configurar su primer stock mínimo o al asentar su primer movimiento de inventario.
- **Razón:** Evita generar cientos o miles de filas vacías e inactivas en la base de datos por cada local, manteniendo la integridad referencial y facilitando que nuevos productos se visualicen de inmediato sin scripts manuales de migración.
- **Descartado:** Generar filas masivas mediante triggers o jobs cada vez que se crea un producto o local.

### 2.2 Unidades de medida y precisión decimal
- **Decisión:** El `stock_actual` y `stock_minimo` se almacenan y muestran siempre en la **unidad base** del producto (`kg`, `litros`, `unidad`), utilizando tipo `NUMERIC(14, 6)` en base de datos. En la interfaz se formatea según la unidad (hasta 3 decimales para peso/volumen y sin decimales para unidades enteras).
- **Razón:** Las presentaciones de compra (ej. saco de 50 kg, caja de 24 botellas) se convertirán a la unidad base al registrar compras (US-050); mantener las existencias siempre en unidad base previene inconsistencias de conversión y errores de cálculo.

### 2.3 Edición rápida de stock mínimo por fila
- **Decisión:** La tabla de existencias incluye un botón de acción rápida *"Fijar mínimo"* en cada fila que abre un modal ligero validado (`>= 0` o vacío para remover el umbral).
- **Razón:** Permite a los administradores ajustar rápidamente los umbrales de abastecimiento sin abandonar la vista consolidada ni tener que navegar a pantallas complejas de configuración de producto.

### 2.4 Stock negativo permitido con alerta no bloqueante (RN-050 / RN-056)
- **Decisión:** La columna `stock_actual` en la tabla `existencia` no tiene restricción `CHECK (stock_actual >= 0)`.
- **Razón:** En el día a día de un restaurante, la cocina consume insumos (ej. pollo, verduras, arroz) antes de que el personal administrativo registre la factura física de compra. Bloquear la operación por stock negativo detendría el servicio a clientes; por tanto, el sistema emite alertas visuales pero permite que los movimientos continúen.

### 2.5 Base para el libro de movimientos (Kardex - RN-057)
- **Decisión:** En la migración inicial del módulo (`V51`) se crea tanto la tabla `existencia` como la tabla `movimiento_inventario` (el libro contable). `stock_actual` es una proyección derivada del libro.
- **Razón:** Garantiza que `ultimo_movimiento_id` apunte a una clave foránea consistente y prepara la base arquitectónica para US-044 (Kardex), US-045 (Ajustes) y US-050 (Compras).

---

## 3. Modelo de Datos (Migración Flyway V51)

```sql
-- V51__existencia_y_movimiento_inventario.sql

-- 1. Tabla de movimientos de inventario (Kardex: solo se agregan filas)
CREATE TABLE movimiento_inventario (
    id                    CHAR(26) PRIMARY KEY,
    local_id              CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    producto_id           CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    origen                VARCHAR(30) NOT NULL
        CHECK (origen IN ('compra', 'produccion', 'venta', 'ajuste', 'transferencia',
                          'conteo', 'consumo_interno', 'anulacion_post_comanda')),
    signo                 VARCHAR(10) NOT NULL
        CHECK (signo IN ('entrada', 'salida')),
    cantidad              NUMERIC(14, 6) NOT NULL CHECK (cantidad > 0),
    unidad_id             CHAR(26) NOT NULL REFERENCES unidad_medida(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    cantidad_base         NUMERIC(14, 6) NOT NULL CHECK (cantidad_base > 0),
    costo_unitario        BIGINT, -- en centimos de sol
    stock_anterior        NUMERIC(14, 6) NOT NULL,
    stock_nuevo           NUMERIC(14, 6) NOT NULL,
    referencia_tipo       VARCHAR(40),
    referencia_id         CHAR(26),
    motivo_id             CHAR(26) REFERENCES motivo(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    observacion           VARCHAR(250),
    ocurrido_en           TIMESTAMPTZ NOT NULL,
    registrado_en         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    registrado_por        CHAR(26) NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);

CREATE INDEX ix_mov_inv_local_producto ON movimiento_inventario(local_id, producto_id, ocurrido_en DESC);
CREATE INDEX ix_mov_inv_origen ON movimiento_inventario(origen, ocurrido_en DESC);

-- 2. Tabla de existencias por local (proyección de balance y configuración de mínimos)
CREATE TABLE existencia (
    local_id              CHAR(26) NOT NULL REFERENCES local(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    producto_id           CHAR(26) NOT NULL REFERENCES producto(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    stock_actual          NUMERIC(14, 6) NOT NULL DEFAULT 0,
    stock_minimo          NUMERIC(14, 6) CHECK (stock_minimo IS NULL OR stock_minimo >= 0),
    ultimo_movimiento_id  CHAR(26) REFERENCES movimiento_inventario(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    recalculado_en        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (local_id, producto_id)
);

CREATE INDEX ix_existencia_local_stock ON existencia(local_id, stock_actual);
```

---

## 4. Contratos de API REST

### 4.1 `GET /api/locales/{localId}/existencias`
- **Permiso:** `inventario.ver`
- **Query params opcionales:** `estado` (`todos` | `bajo_minimo` | `negativo`), `busqueda` (texto).
- **Respuesta (200 OK):**
```json
[
  {
    "localId": "01M2QZ60D35B90CW00EJA15VFC",
    "productoId": "01M2QZ60P10000000000000001",
    "productoNombre": "Pechuga de Pollo Fresca",
    "productoCodigo": "INS-001",
    "categoriaNombre": "Carnes y Aves",
    "unidadMedidaId": "01M2QZ60U00000000000000001",
    "unidadMedidaNombre": "Kilogramo",
    "unidadMedidaSimbolo": "kg",
    "stockActual": 14.500000,
    "stockMinimo": 20.000000,
    "alerta": "bajo_minimo",
    "recalculadoEn": "2026-10-12T18:30:00Z"
  }
]
```

### 4.2 `GET /api/locales/{localId}/existencias/resumen-alertas`
- **Permiso:** `inventario.ver`
- **Respuesta (200 OK):**
```json
{
  "totalProductos": 45,
  "bajoMinimo": 3,
  "negativos": 1,
  "sinStock": 2
}
```

### 4.3 `PUT /api/locales/{localId}/existencias/{productoId}/stock-minimo`
- **Permiso:** `inventario.administrar`
- **Body:**
```json
{
  "stockMinimo": 25.0
}
```
*(o `null` para eliminar el umbral de stock mínimo)*
- **Respuesta (200 OK):** Objeto de existencia actualizado.
- **Errores:** `400` si `stockMinimo < 0`, `404` si local o producto no existen, `403` si no tiene permisos.

---

## 5. UI/UX: Pantalla ADM-16 (`/admin/inventario/existencias`)

- **Ruta:** `/admin/inventario/existencias` (y redirección desde `/admin/inventario`).
- **Navegación:** Barra Lateral → Sección Negocio → **Inventario** → Subítem **Existencias** (con badge si hay alertas de stock bajo mínimo o negativo).
- **Tarjetas KPI en la cabecera:**
  - Total productos registrados en inventario
  - Productos bajo stock mínimo (ámbar con conteo)
  - Productos con stock negativo (rojo crítico con conteo)
- **Barra de filtros:**
  - Buscador en vivo por nombre o código de producto.
  - Pestañas / Pills: `Todos`, `Bajo mínimo` (con badge), `Stock negativo` (con badge).
- **Tabla interactiva:**
  - Código y Nombre del producto
  - Categoría
  - Stock actual (destacado con color y símbolo de unidad)
  - Stock mínimo (con botón de edición rápida)
  - Estado (`Insignia`: `Normal` [verde], `Bajo mínimo` [ámbar], `Negativo` [roja])
  - Acciones: botón *"Fijar mínimo"* / *"Editar mínimo"*.
- **Modal de edición de stock mínimo:**
  - Muestra nombre del producto y unidad base.
  - Input numérico con controles de incremento o borrado de umbral.
  - Mensaje claro: *"Se alertará cuando las existencias en este local bajen de este valor."*
