# US-045 — Registrar ajustes de inventario con motivo (sprint 9, entrega 3)

**Especificado en:** `docs/superpowers/plans/2026-10-13-us-045-ajustes-inventario.md`. En el backlog v3 es US-45. Épica EP-08 (Inventario y compras). Hecho el 2026-10-13, backend y frontend.

## Qué se pidió
Permitir al administrador registrar ajustes positivos (entradas / sobrantes) o negativos (salidas / mermas) con motivo obligatorio tomado de una lista configurable y observación (obligatoria si el motivo lo exige), actualizando el stock físico y dejando trazabilidad en el Kardex y en la bitácora transversal de auditoría (RN-052, RN-004, RN-012, RN-057).

## Qué se construyó
- **Semilla de Motivos en BD:** 5 motivos de tipo `ajuste_inventario` configurados en PostgreSQL:
  1. Merma / Vencimiento (requiere observación)
  2. Rotura / Daño (requiere observación)
  3. Sobrante de conteo
  4. Corrección de inventario (requiere observación)
  5. Consumo no registrado
- **Casos de uso:**
  - `RegistrarAjusteInventario`: valida existencia de local, producto y usuario; valida motivo obligatorio (`tipo = 'ajuste_inventario'`, activo); valida observación si `motivo.isRequiereObservacion()`; soporta modo variación (`INCREMENTO`, `DISMINUCION`, delta) y conteo real (`REEMPLAZO_STOCK`); actualiza `Existencia` con `saveAndFlush`; inserta movimiento en `MovimientoInventario` con `origen = 'ajuste'`; registra evento `ajuste_inventario` en bitácora de auditoría.
  - `ConsultarAjustesInventario`: lista el historial de ajustes filtrado por local, producto y rango de fechas.
- **API REST (`AjusteInventarioController`):**
  - `POST /api/locales/{localId}/inventario/ajustes` (permiso `inventario.descontar` o admin).
  - `GET /api/locales/{localId}/inventario/ajustes` (permiso `inventario.ver` o admin).
- **Frontend (`ModalRegistrarAjuste.tsx` y `PantallaAjustes.tsx`):**
  - Pantalla dedicada en `/admin/inventario/ajustes`.
  - 4 KPIs: Total Ajustes, Entradas / Sobrantes (+), Salidas / Mermas (-) e Impacto Neto.
  - Filtros por producto, texto y fechas.
  - Modal interactivo con modo dual: "Por Variación (+ / -)" vs "Por Conteo Físico Real".
  - Proyección en tiempo real del stock resultante y advertencia de saldo negativo (RN-057).
  - Selector de motivo con bloqueo reactivo de observación obligatoria.
  - Botón de ajuste rápido integrado en cada fila de `/admin/inventario/existencias`.

## Decisiones tomadas al ejecutar
- **Modo Dual de Captura:** El almacenero puede ajustar directamente una merma puntual (ej. -2 latas vencidas) o ingresar el stock contado físico (ej. hay 18 kg), donde el sistema calcula automáticamente la diferencia `conteo - stock_actual`.
- **Validación Estricta de Observación:** Si el motivo tiene `requiereObservacion = true`, el sistema bloquea tanto en frontend como en dominio (`OBSERVACION_REQUERIDA`).
- **Valuación de Costo:** Se toma el costo de última compra registrado en el catálogo (`producto.costoUltimaCompra`) y se calcula el costo total del movimiento (`cantidad * costoUnitario`).

## Cómo se verificó
- Tests unitarios en `RegistrarAjusteInventarioTest.java`: 6/6 tests pasaron (disminución, incremento, conteo real, error sin motivo, error sin observación requerida, error sin variación).
- Tests de seguridad RBAC en `AjusteInventarioControllerAccesoTest.java`: 4/4 tests pasaron (200 con permiso, 403 sin permiso).
- Pruebas E2E en Playwright con capturas de evidencia visual en artifacts:
  - `ajustes_pantalla_inicial.png`: pantalla vacía y KPIs en cero.
  - `ajustes_modal_abierto.png`: modal con selector y proyección.
  - `ajustes_validacion_motivo_obs.png`: alerta bloqueante de observación requerida.
  - `ajustes_tabla_dos_ajustes.png`: tabla con ajuste de salida y ajuste de entrada.
  - `ajustes_existencias_actualizadas.png`: stock actualizado en tiempo real.
  - `ajustes_kardex_reflejado.png`: movimientos reflejados con origen `'ajuste'`.
