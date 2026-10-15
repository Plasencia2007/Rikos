# US-050 — Registrar compras y actualizar costo de última compra (sprint 9, entrega 2)

**Especificado en:** `docs/producto/plan/sprint-09-existencias-reales.md` y `docs/superpowers/plans/2026-10-13-us-050-registro-de-compras.md`. En el backlog v3 es US-50. Épica EP-08 (Inventario y compras). Hecho el 2026-10-13, backend y frontend.

## Qué se pidió
Registrar comprobantes de compra a proveedores (factura, boleta, nota de venta, recibo interno) con detalle de ítems (insumos/productos), cantidades y costos unitarios. Al confirmar la compra:
1. Incrementar el stock actual en `existencia` del local (RN-050).
2. Asentar los movimientos de entrada en `kardex` vinculados al comprobante (RN-053).
3. Actualizar `ultimo_costo_compra` en la tabla `producto` para alimentar el costeo en cascada de recetas (RN-052).
4. El comprobante confirmado se vuelve inalterable; si hay error, se compensa mediante ajuste de inventario o nota de crédito.

## Qué se construyó
- **Migración Flyway V52:** Tablas `compra` y `detalle_compra` con claves foráneas a `proveedor`, `local` y `producto`, montos de subtotal, IGV y total, tipo de comprobante, serie y número.
- **Modelo de Dominio:** Entidades `Compra` y `DetalleCompra` con estados `BORRADOR`, `CONFIRMADA`, `ANULADA` y tipos de documento `FACTURA`, `BOLETA`, `NOTA_VENTA`, `GUIA_REMISION`.
- **Casos de Uso de Aplicación:**
  - `RegistrarCompra`: Crea la compra en estado borrador o la registra directamente con validación de proveedor activo y productos catalogados.
  - `ConfirmarCompra`: Valida estado previo, incrementa existencias en `existencia`, asienta entradas en `kardex` (`tipo_movimiento = 'compra'`), actualiza `producto.costo_unitario` / `ultimo_costo_compra` y marca la compra como `CONFIRMADA`.
  - `ConsultarCompras`: Lista compras con filtros por fecha, proveedor, estado y tipo de documento.
  - `GestionarAdjuntoCompra`: Permite adjuntar y visualizar foto/PDF del comprobante físico para cotejo contable.
- **API REST (`CompraController`):**
  - `GET /api/locales/{localId}/compras` (permiso `compras.ver` o admin).
  - `POST /api/locales/{localId}/compras` (permiso `compras.registrar` o admin).
  - `POST /api/locales/{localId}/compras/{id}/confirmar` (permiso `compras.confirmar` o admin).
  - `POST /api/locales/{localId}/compras/{id}/adjunto` (permiso `compras.registrar` o admin).
- **Frontend (`PantallaCompras.tsx` y `DialogoFormularioCompra.tsx`):**
  - Pantalla en `/admin/inventario/compras` (ADM-17).
  - Buscador y selector de proveedores con autocompletado de RUC/Razón Social.
  - Grilla dinámica para agregar ítems de compra con cálculo automático de subtotal, IGV y total.
  - Modal de confirmación con vista previa del comprobante y badge de estado.

## Decisiones tomadas al ejecutar
- **Transaccionalidad atómica:** La confirmación de compra ejecuta en una sola transacción la actualización de `existencia`, inserción en `kardex`, actualización de costos y cambio de estado de la compra. Si falla algún ítem, todo hace rollback.
- **Inmutabilidad post-confirmación:** Una compra confirmada no puede editarse en precios ni cantidades; las correcciones posteriores se efectúan por US-045 (Ajustes de inventario con motivo).
- **Precios e IGV:** Admite compras con IGV discriminado o incluido según el tipo de comprobante.

## Cómo se verificó
- Tests unitarios en `ComprasTest` (registro, confirmación, cálculo de totales e impacto en kardex).
- Tests de control de acceso en `CompraControllerAccesoTest` (verificación de permisos RBAC).
- Pruebas E2E en navegador con Playwright registrando una compra de prueba y verificando el incremento de existencias en tiempo real.
