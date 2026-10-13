# Plan de Implementación: US-043 — Mantener existencias por local con stock mínimo y alertas

Sprint 9 (Entrega 1: Existencias reales y alertas de inventario).
Basado en `docs/producto/plan/sprint-09-existencias-reales.md`, EP-08 y `docs/METODO-spec-driven.md`.

---

## Tareas

### Paso 1: Migración Flyway V51 y Entidades JPA
- [ ] Crear migración `V51__existencia_y_movimiento_inventario.sql` en `servicio-operacion`:
  - Tabla `movimiento_inventario` (Kardex: solo append, con índices y FKs).
  - Tabla `existencia` (PK compuesta `(local_id, producto_id)`, `stock_actual` sin restricción de negatividad [RN-050], `stock_minimo >= 0`).
  - Sembrar permisos `inventario.ver` e `inventario.administrar` en tabla `permiso` y asociarlos a roles autorizados (`administrador central` y `administrador de local`).
- [ ] Crear entidad `ExistenciaId.java` (`@Embeddable` con `localId` y `productoId`).
- [ ] Crear entidad `Existencia.java` (`@Entity` mapeando tabla `existencia`).
- [ ] Crear entidad `MovimientoInventario.java` (`@Entity` mapeando tabla `movimiento_inventario`).
- [ ] Crear `ExistenciaRepositorio.java` y `MovimientoInventarioRepositorio.java`.

### Paso 2: Casos de Uso Backend (TDD)
- [ ] Crear DTOs / Proyecciones: `FilaExistencia.java`, `ResumenAlertasInventario.java`, `SolicitudStockMinimo.java`.
- [ ] Implementar `ConsultarExistencias.java`:
  - Obtiene los productos activos del local/catálogo.
  - Cruza con las filas de `existencia` existentes o proyecta `0` si aún no hay movimientos.
  - Calcula la alerta (`normal`, `bajo_minimo`, `negativo`, `sin_stock`).
- [ ] Implementar `ConfigurarStockMinimo.java`:
  - Valida que el producto y local existan y estén activos.
  - Actualiza o crea la fila en `existencia` con el nuevo `stock_minimo` (o null).
- [ ] Escribir pruebas unitarias en `ExistenciasTest.java` verificando CA1, CA2, CA3, CA4.
- [ ] Ejecutar `..\mvnw test` y asegurar paso en verde.

### Paso 3: Controlador REST y Seguridad
- [ ] Crear `ExistenciaController.java` bajo `pe.rikos.operacion.inventario.api`:
  - `GET /locales/{localId}/existencias`
  - `GET /locales/{localId}/existencias/resumen-alertas`
  - `PUT /locales/{localId}/existencias/{productoId}/stock-minimo`
- [ ] Registrar reglas de autorización en `ConfiguracionSeguridad.java` (`hasAuthority("PERM_inventario.ver")` y `hasAuthority("PERM_inventario.administrar")`).
- [ ] Escribir pruebas de integración `ExistenciaControllerAccesoTest.java` verificando control de acceso por rol y endpoints REST.

### Paso 4: Frontend Web (`rikos-web`)
- [ ] Extender `src/api/cliente.ts` con tipos `FilaExistencia`, `ResumenAlertasInventario` y funciones de API.
- [ ] Crear componente modal `DialogoStockMinimo.tsx` para definir/actualizar stock mínimo con validación numérica.
- [ ] Crear pantalla `PantallaExistencias.tsx` (ADM-16):
  - KPIs en cabecera: Total productos, Bajo mínimo, Negativos.
  - Filtros por pills / tabs: `Todos`, `Bajo mínimo`, `Stock negativo`.
  - Buscador por nombre y código.
  - Tabla interactiva con badges de estado y acción rápida de fijar mínimo.
- [ ] Registrar ruta `/admin/inventario/existencias` en `src/apps/admin/index.tsx`.
- [ ] Añadir subítem en `src/apps/admin/BarraLateral.tsx` bajo menú **Inventario**.
- [ ] Validar compilación limpia con `npm run build`.

### Paso 5: Verificación Integral End-to-End
- [ ] Iniciar backend y frontend con la migración V51 aplicada.
- [ ] Ejecutar flujo con Playwright:
  - Navegar a `/admin/inventario/existencias`.
  - Configurar stock mínimo a un producto y verificar persistencia.
  - Verificar alerta de bajo stock mínimo en la tabla y en las tarjetas KPI.
  - Tomar captura de pantalla de evidencia.
