# Plan de Implementación: US-050 — Registrar compras y actualizar el costo de última compra

## 1. Objetivo
Permitir al administrador de local registrar compras asociadas a proveedores con su comprobante de sustento (factura, boleta, ticket o sin documento), detallando productos, presentaciones, cantidades y precios.
Al confirmar la compra:
1. Aumenta el stock del local en la unidad base de cada producto aplicando las conversiones de presentación (RN-054, RN-055, CA2).
2. Genera el registro inmutable en `movimiento_inventario` con origen `compra` y signo `entrada` (RN-057).
3. Actualiza en `producto` el `costo_ultima_compra` en su unidad base, `origen_costo = 'compra'` y `fecha_ultima_compra` (RN-047, CA2).
4. Recalcula automáticamente en cascada los costos de las recetas que utilizan los productos afectados (CA3).
5. Sube y protege el archivo adjunto del comprobante (CA1, CA5).
6. Respeta que el sistema no gestiona órdenes de compra ni cuentas por pagar (CA4).

---

## 2. Decisiones de Diseño Clave
- **Ciclo de vida con estado borrador:** Las compras pueden guardarse como `borrador` para digitación progresiva y luego `confirmada` de manera atómica con transacción `@Transactional`.
- **Moneda y dinero:** Todos los precios y totales monetarios se almacenan como `BIGINT` en céntimos de sol (consistente con el resto del backend).
- **Conversión a unidad base:**
  - Si el detalle usa `presentacion_id`, `cantidad_base = cantidad * presentacion.factorAUnidadBase`.
  - Si usa la unidad base directa, `cantidad_base = cantidad`.
  - El costo por unidad base para `producto.costo_ultima_compra` es `(subtotal / cantidad_base)` con escala 6 `NUMERIC(14,6)`.
- **Unicidad de comprobante:** Índice único condicional `(proveedor_id, tipo_documento, serie, numero)` para evitar duplicidad de facturas/boletas.
- **Adjunto:** Almacenamiento seguro en disco en `uploads/comprobantes-compra/` servido a través de endpoint REST protegido por el permiso `compras.ver`.

---

## 3. Plan Paso a Paso

### Paso 1: Migración Flyway V52 y Modelo de Datos
- Crear migración `V52__compra_y_detalle_compra.sql`:
  - Tablas `compra` y `detalle_compra`.
  - Semillas de permisos: `compras.ver` y `compras.registrar` asignados al rol `admin`.
- Crear entidades JPA y repositorios:
  - `Compra.java`, `DetalleCompra.java`, `TipoDocumentoCompra.java`, `EstadoCompra.java`.
  - `CompraRepositorio.java`, `DetalleCompraRepositorio.java`.
- Actualizar `SembradorCatalogoPermisos.java` con los nuevos códigos de permisos.

### Paso 2: Casos de Uso Backend (TDD)
- DTOs: `RegistroCompraDto.java`, `LineaDetalleCompraDto.java`, `ResumenCompraDto.java`, `DetalleCompraCompletoDto.java`.
- Casos de uso:
  - `RegistrarCompra.java`: Creación en borrador o confirmada directa.
  - `ConfirmarCompra.java`: Transición atómica: creación de movimientos de inventario, actualización de existencias por local, actualización de `costo_ultima_compra` en `producto` y llamada a `calculadoraCostoReceta.recalcularEnCascada(productoId)`.
  - `ConsultarCompras.java`: Búsqueda y consulta de compras por local.
  - `GestionarAdjuntoCompra.java`: Subida y recuperación segura de adjuntos.
- Pruebas unitarias en `ComprasTest.java`:
  - Cálculo de `cantidad_base` con presentaciones.
  - Asiento de movimientos de inventario y aumento de stock en `existencia`.
  - Actualización de `costo_ultima_compra` y recálculo en cascada de recetas.
  - Rechazo de compras duplicadas por proveedor y serie/número.
  - Validación de totales y montos no negativos.

### Paso 3: Controlador REST y Seguridad
- `CompraController.java`:
  - `GET /locales/{localId}/compras`: listar compras.
  - `GET /locales/{localId}/compras/{compraId}`: detalle de compra.
  - `POST /locales/{localId}/compras`: crear compra.
  - `POST /locales/{localId}/compras/{compraId}/confirmar`: confirmar compra borrador.
  - `POST /locales/{localId}/compras/{compraId}/adjunto`: subir archivo comprobante.
  - `GET /locales/{localId}/compras/{compraId}/adjunto`: descargar archivo comprobante.
- Configurar matchers en `ConfiguracionSeguridad.java` con `PERM_compras.ver` y `PERM_compras.registrar`.
- Pruebas de integración de seguridad `CompraControllerAccesoTest.java`.

### Paso 4: Frontend Web (`rikos-web`)
- Extender `src/api/cliente.ts` con tipos y funciones para compras, proveedores y adjuntos.
- Componentes UI:
  - `src/features/compras/PantallaCompras.tsx`: Listado de compras con KPIs, filtros por fecha/estado y botón "Nueva Compra".
  - `src/features/compras/DialogoFormularioCompra.tsx`: Modal / formulario reactivo para cargar proveedor, tipo, serie, número, fecha, selector de productos/presentaciones con cálculo automático de subtotales y total.
  - Botón de adjuntar archivo y confirmación rápida.
- Integración en navegación:
  - Enlace "Compras" en `BarraLateral.tsx`.
  - Ruta `/admin/compras` en `src/apps/admin/index.tsx`.
- Verificación de compilación: `tsc -b && vite build`.

### Paso 5: Verificación Integral End-to-End
- Ejecutar suite de pruebas de backend con Maven (`BUILD SUCCESS`).
- Probar flujo completo en navegador con Playwright:
  1. Registrar una nueva compra para San Isidro de un insumo (ej. pollo o insumo con presentación).
  2. Confirmar la compra y adjuntar comprobante de prueba.
  3. Verificar que el stock en la pantalla de Existencias (`/admin/inventario/existencias`) aumentó automáticamente.
  4. Verificar que el costo de última compra se actualizó en el producto y las recetas asociadas se recalcularon.
  5. Capturar screenshots de evidencia.
