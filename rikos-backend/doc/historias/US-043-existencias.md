# US-043 — Mantener existencias por local con stock mínimo y alertas (sprint 9, entrega 1)

**Especificado en:** `docs/producto/plan/sprint-09-existencias-reales.md`. En el backlog v3 es US-43. Épica EP-08 (Inventario y compras). Hecho el 2026-10-12, backend y frontend.

## Qué se pidió
Que el administrador de local pueda consultar en tiempo real las existencias físicas de su local por producto, con un umbral de stock mínimo configurable por combinación local-producto. Al bajar del mínimo o al llegar a saldo negativo, el sistema debe alertar visualmente sin bloquear nunca las ventas, comandas o despachos (RN-050, RN-051, RN-057).

## Qué se construyó
- **Modelo y Persistencia:** Entidad `Existencia` con clave compuesta `(local_id, producto_id)` en tabla `existencia`. Campos: `stock_actual` (`NUMERIC(14,4)`), `stock_minimo` (`NUMERIC(14,4)` nullable) y versión para bloqueo optimista.
- **Caso de uso `ConsultarExistencias`:** Recupera el stock físico de todos los productos activos para el local especificado, calculando el estado de alerta derivado (`normal`, `bajo_minimo`, `negativo`, `sin_stock`). Permite filtrar por pestaña: todos, bajo mínimo, o negativos.
- **Caso de uso `ConfigurarStockMinimo`:** Permite al administrador fijar, modificar o quitar (`null`) el stock mínimo de un producto en su local.
- **Caso de uso `ObtenerResumenAlertasInventario`:** Calcula métricas agregadas del local: total de productos catalogados, total bajo stock mínimo y total con stock negativo.
- **API REST (`ExistenciaController`):**
  - `GET /api/locales/{localId}/existencias?filtro={todos|bajo_minimo|negativo}` (permiso `inventario.ver` o admin).
  - `PUT /api/locales/{localId}/existencias/{productoId}/stock-minimo` (permiso `inventario.administrar` o admin).
  - `GET /api/locales/{localId}/existencias/resumen-alertas` (permiso `inventario.ver` o admin).
- **Frontend (`PantallaExistencias.tsx` y `DialogoStockMinimo.tsx`):**
  - Pantalla en `/admin/inventario/existencias` (ADM-16).
  - 3 tarjetas KPI superiores: Total Productos, Bajo Stock Mínimo y Stock Negativo (conmutables como filtros rápidos).
  - Tabla completa con código, nombre, categoría, unidad, stock actual, stock mínimo, insignias de estado y acciones por fila.
  - Modal para configurar stock mínimo con validación numérica y botón de limpieza ("Sin mínimo").

## Decisiones tomadas al ejecutar
- **El stock negativo se tolera siempre (RN-057):** Si un producto no tiene compras registradas pero se vende en el POS, el stock pasa a saldo negativo emitiendo insignia roja `● Stock Negativo`, sin bloquear la comanda ni el cobro.
- **Stock mínimo por local:** El stock mínimo es independiente por local (un local céntrico puede requerir umbral mayor que uno periférico).
- **Sin ceros falsos:** Si un producto nunca ha tenido movimientos en ese local, su existencia se inicializa en 0 al primer movimiento o consulta.

## Cómo se verificó
- Tests unitarios en `servicio-operacion` para consulta de existencias y configuración de stock mínimo.
- Tests de autorización RBAC: roles autorizados acceden; roles no autorizados (mozo, cocina) reciben 403.
- Verificación en navegador y Playwright: visualización de KPIs, filtrado por pestaña y edición de umbral mínimo en tiempo real.
