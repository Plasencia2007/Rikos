# US-044 — Consultar el Kardex con origen rastreable (sprint 9, entrega 2)

**Especificado en:** `docs/producto/plan/sprint-09-existencias-reales.md`. En el backlog v3 es US-44. Épica EP-08 (Inventario y compras). Hecho el 2026-10-12, backend y frontend.

## Qué se pidió
Registro y consulta permanente de todos los movimientos de inventario de cada local, con origen rastreable (`compra`, `produccion`, `venta`, `ajuste`, `transferencia`, `conteo` o `consumo_interno`), guardando stock anterior, stock nuevo, diferencia, costo unitario, costo total, usuario, fecha y local (RN-052, RN-057, RN-002).

## Qué se construyó
- **Entidad `MovimientoInventario`:** Tabla `movimiento_inventario` en PostgreSQL con check constraint `movimiento_inventario_origen_check` (`compra`, `produccion`, `venta`, `ajuste`, `transferencia`, `conteo`, `consumo_interno`, `anulacion_post_comanda`), signo (`entrada`, `salida`), cantidades base, costos, stock anterior y stock resultante.
- **Caso de uso `ConsultarKardex`:** Consulta paginada y filtrable por producto, origen, signo y rango de fechas con JPA Specification.
- **API REST (`KardexController`):**
  - `GET /api/locales/{localId}/kardex` con parámetros `productoId`, `origen`, `desde`, `hasta`, `pagina`, `tamano` y `orden`. Protegido con `PERM_inventario.ver` o admin.
- **Frontend (`PantallaKardex.tsx`):**
  - Pantalla dedicada en `/admin/inventario/kardex` (ADM-17).
  - Selector de producto, selector de origen del movimiento, rango de fechas y orden cronológico.
  - Tabla completa de movimientos con badges de origen, variación de stock, stock previo -> nuevo, costo unitario, costo total, referencia / motivo y colaborador que registró el movimiento.

## Decisiones tomadas al ejecutar
- **El Kardex es un libro de solo lectura e inmutable:** No se edita ni se borra ningún movimiento; cualquier rectificación se realiza mediante un ajuste de inventario (US-045).
- **Valuación con costo histórico del movimiento:** Cada movimiento almacena el costo unitario vigente al momento de ocurrir, permitiendo auditoría contable y financiera precisa sin verse alterado por compras posteriores.
- **Filtro directo desde Existencias:** La tabla de existencias incluye un botón "Ver Kardex" por fila que pre-filtra automáticamente el producto en el Kardex.

## Cómo se verificó
- Tests unitarios en backend comprobando inserción y paginación de movimientos.
- Tests RBAC: solo personal autorizado accede al Kardex.
- Verificación en navegador y Playwright: filtrado por origen (`compra`, `venta`, `produccion`, `ajuste`), cambio de páginas y consistencia de saldos.
