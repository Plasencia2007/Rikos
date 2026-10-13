# Plan de Implementación: US-045 — Registrar ajustes de inventario con motivo

- **Épica:** EP-08 — Inventario y compras
- **Sprint:** Sprint 9 — Existencias reales
- **Metodología:** `docs/METODO-spec-driven.md` con `/superpowers` y `/grill-me`

---

## 1. Alcance y Criterios de Aceptación (US-045)

1. **CA1 (Ajuste positivo y negativo):** Permite registrar incrementos (`signo = 'entrada'`) o mermas/disminuciones (`signo = 'salida'`), actualizando el `stock_actual` de la entidad `Existencia`. Permite saldos negativos per RN-057.
2. **CA2 (Motivo obligatorio):** El motivo debe pertenecer a la lista de motivos configurables (`tipo = 'ajuste_inventario'` y `activo = true`). Si `motivo.isRequiereObservacion()` es verdadero, la observación es obligatoria.
3. **CA3 (Trazabilidad y datos guardados):** Registra en `MovimientoInventario` con `origen = 'ajuste'`, guardando stock anterior, stock nuevo, cantidad (diferencia), costo unitario del producto, usuario responsable, fecha/hora, local, motivo y observación. Registra auditoría transversal con `evento = 'ajuste_inventario'` (RN-004).
4. **CA4 (Control de acceso):** Protegido por permiso `inventario.descontar` (o roles administrativos `ROLE_ADMINISTRADOR_CENTRAL`, `ROLE_ADMINISTRADOR_DE_LOCAL`).
5. **Decisiones de Diseño (/grill-me):**
   - **UI:** Pantalla dedicada en `/admin/inventario/ajustes` (activando link en la barra lateral) con historial de ajustes y modal interactivo `+ Nuevo Ajuste`. Acceso rápido mediante botón "Ajustar" en la tabla de existencias (`/admin/inventario/existencias`).
   - **Modo Dual:** El modal permite digitar la variación directa (+/- delta) o digitar el conteo real físico (calculando automáticamente la diferencia `conteo - stock_actual`).

---

## 2. Tareas de Implementación

### Tarea 1: Dominio y Casos de Uso Backend (`servicio-operacion`)
- Crear DTOs:
  - `RegistroAjusteComando` (`localId`, `productoId`, `motivoId`, `tipoAjuste` [INCREMENTO | DISMINUCION | REEMPLAZO_STOCK], `cantidad`, `stockFinal`, `observacion`, `usuarioId`).
  - `DetalleAjusteDTO` (`id`, `productoId`, `productoNombre`, `productoCodigo`, `unidadMedida`, `signo`, `cantidad`, `stockAnterior`, `stockNuevo`, `costoUnitario`, `costoTotal`, `motivoId`, `motivoDescripcion`, `observacion`, `ocurridoEn`, `usuarioNombre`).
- Implementar caso de uso `RegistrarAjusteInventario`:
  - Validar existencia de local, producto y existencia. Si no existe existencia previa, inicializar con stock 0.
  - Validar motivo (`tipo == 'ajuste_inventario'` y `activo == true`).
  - Validar requerimiento de observación.
  - Calcular stock nuevo y diferencia.
  - Crear y persistir `MovimientoInventario` con `saveAndFlush`.
  - Actualizar `Existencia` con `saveAndFlush`.
  - Registrar evento de auditoría `ajuste_inventario` (RN-004).
- Implementar caso de uso `ConsultarAjustesInventario`:
  - Listar movimientos con `origen = 'ajuste'` para el local, ordenados por `ocurridoEn DESC`, con filtros opcionales por producto o fecha.
- Tests unitarios:
  - `RegistrarAjusteInventarioTest`: ajuste positivo, ajuste negativo con stock negativo permitido (RN-057), fallo por falta de motivo, fallo por falta de observación obligatoria.

### Tarea 2: Controlador REST y Seguridad
- Crear `AjusteInventarioController` en `/locales/{localId}/inventario/ajustes`:
  - `POST`: `registrarAjuste` -> requiere `PERM_inventario.descontar` o roles admin.
  - `GET`: `consultarAjustes` -> requiere `PERM_inventario.ver` o roles admin.
- Semilla de motivos de ajuste de prueba en PostgreSQL (ej. "Merma / Vencimiento", "Rotura / Daño", "Sobrante de conteo", "Corrección de inventario").
- Actualizar `ConfiguracionSeguridad.java` para proteger las rutas.
- Test de integración y seguridad: `AjusteInventarioControllerAccesoTest`.

### Tarea 3: Frontend Web (`rikos-web`)
- En `cliente.ts`: agregar funciones `listarAjustesInventario` y `registrarAjusteInventario`.
- Componente `ModalRegistrarAjuste.tsx`:
  - Selector de producto con búsqueda y visualización de stock actual y unidad.
  - Selector de pestañas de modo: "Por Variación (+/-)" vs "Por Stock Contado Real".
  - Desplegable de motivos activos (`tipo = 'ajuste_inventario'`) con indicación visual si la observación es obligatoria.
  - Resumen en tiempo real: Stock Actual $\to$ Variación $\to$ Saldo Proyectado, con alerta de stock negativo si aplica (RN-057).
- Componente `PantallaAjustes.tsx`:
  - KPIs de ajustes del mes/semana (Total Ajustes, Incrementos netos, Mermas netas, Impacto valorizado).
  - Filtros por fecha y búsqueda por producto.
  - Tabla de historial con badges de tipo (+/-), motivo, usuario y observación.
  - Botón `+ Nuevo Ajuste`.
- Integración en navegación:
  - Activar enlace en `BarraLateral.tsx` hacia `/admin/inventario/ajustes`.
  - Registrar ruta en `apps/admin/index.tsx`.
  - Agregar botón de acción "Ajustar" en `PantallaExistencias.tsx` para abrir el modal preseleccionando el producto.

### Tarea 4: Validación y Pruebas E2E
- Ejecutar suite de pruebas unitarias (`mvn test`).
- Navegar con Playwright a `/admin/inventario/ajustes`.
- Registrar un ajuste positivo y uno negativo (merma).
- Verificar que el Kardex y las Existencias reflejen los cambios inmediatamente.
- Generar reporte de evidencia en `docs/superpowers/reports/`.
