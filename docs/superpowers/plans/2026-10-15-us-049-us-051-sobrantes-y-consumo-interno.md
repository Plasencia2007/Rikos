# Plan de Implementación: US-049 y US-051 (Sobrantes de Producción y Consumo Interno)

- **Épica:** EP-08 — Inventario y compras
- **Sprint:** Sprint 9 — Existencias reales (Entregas 6 y 7)
- **Metodología:** `docs/METODO-spec-driven.md` con `/superpowers` y `/grill-me`

---

## 1. Alcance y Criterios de Aceptación

### US-049: Registrar el destino de los sobrantes de producción
1. **CA1 (Selección del destino):** Permite registrar el sobrante de una producción confirmada eligiendo entre los 5 destinos canónicos: `conservar`, `personal`, `perdida`, `reutilizacion` u `otro` (RN-049).
2. **CA2 (Ajuste de inventario generado):**
   - Si el destino es `conservar` o `reutilizacion`, el producto elaborado se mantiene disponible en el stock del local sin salida de inventario.
   - Si el destino es `perdida`, `personal` u `otro`, el sistema descuenta la cantidad sobrante de `Existencia` y asienta un movimiento de salida en `MovimientoInventario` con `origen = 'produccion'` o `ajuste` y motivo formal `sobrante_produccion` (RN-052).
3. **CA3 (Trazabilidad e inmutabilidad):** Actualiza `sobrante`, `destino_sobrante` y `motivo_id` en la tabla `produccion`. La producción queda liquidada impidiendo liquidaciones dobles.
4. **CA4 (Control de acceso):** Protegido por permiso granular `produccion.sobrante` (asignable a roles administrativos, cocina o cajero según requerimiento de la sede).

### US-051: Registrar el consumo interno del personal
1. **CA1 (Primera comida sin cobro):** Un trabajador activo puede registrar una comida en su turno sin cobro; el sistema valoriza el costo contable del plato/producto y descuenta el inventario de existencias (RN-056).
2. **CA2 (Segunda comida con autorización):** Si el trabajador ya registra un consumo en la misma fecha y servicio/turno en ese local, el sistema exige autorización de supervisor:
   - Si el operador actual cuenta con permiso `consumo_interno.autorizar`, ingresa observación obligatoria.
   - Si el operador no cuenta con el permiso, se solicita el PIN/credencial de un usuario autorizado.
3. **CA3 (Datos y trazabilidad):** Registra en la tabla `consumo_interno` (`local_id`, `usuario_id`, `fecha`, `servicio`, `turno`, `producto_id`, `cantidad`, `costo`, `requirio_autorizacion`, `autorizado_por`, `observacion`, `movimiento_id`). Asienta movimiento en Kardex con `origen = 'consumo_interno'`.
4. **CA4 (Reporte separado de ventas):** El consumo interno se reporta totalmente desacoplado de las ventas de caja; no crea comanda de salón, ni pedido, ni comprobante fiscal.
5. **CA5 (Permisos):** `consumo_interno.ver`, `consumo_interno.crear`, `consumo_interno.autorizar`.

---

## 2. Fases y Tareas de Implementación

### Fase 1: Base de Datos y Permisos (Migración V54)
- **Archivo:** `V54__consumo_interno_y_permisos_sobrantes.sql`
  - Crear tabla `consumo_interno` con claves foráneas a `local`, `usuario`, `producto` y `movimiento_inventario`.
  - Crear índices por `local_id`, `usuario_id`, `fecha` y `servicio`.
  - Sembrar permisos granulares:
    - `produccion.sobrante` ("Liquidar destino de sobrantes de producción")
    - `consumo_interno.ver` ("Ver consumos internos del personal")
    - `consumo_interno.crear` / `consumo_interno.registrar` ("Registrar comida del personal")
    - `consumo_interno.autorizar` ("Autorizar comidas extraordinarias del personal")
  - Asignar permisos a roles `administrador central`, `administrador de local` y `cajero`.
  - Registrar evento de auditoría `consumo_interno` y `liquidacion_sobrante`.

### Fase 2: Backend Operación (`servicio-operacion`)

#### A. Módulo Producción (US-049)
1. En `Produccion.java`:
   - Añadir método de dominio `liquidarSobrante(BigDecimal sobrante, String destino, Motivo motivo, Usuario usuario)`.
   - Validaciones: no liquidar si ya tiene `destinoSobrante != null`, `sobrante > 0` y `sobrante <= porcionesPreparadas` (o rendimiento).
2. DTOs:
   - `LiquidarSobranteComando`: `localId`, `produccionId`, `sobrante`, `destinoSobrante`, `motivoId`, `observacion`, `usuarioId`.
3. Caso de uso `RegistrarDestinoSobrante.java`:
   - Validar producción y estado.
   - Si destino es `perdida`, `personal` u `otro`:
     - Disminuir stock en `Existencia`.
     - Crear `MovimientoInventario` con `signo = 'salida'`, `origen = 'produccion'`, `referencia_tipo = 'produccion'`, `motivo = sobrante_produccion`.
   - Actualizar producción con `saveAndFlush`.
   - Auditar evento.
4. Exponer endpoint en `ProduccionController.java`:
   - `POST /api/locales/{localId}/producciones/{id}/sobrante` (permiso `produccion.sobrante`).
5. Tests unitarios: `RegistrarDestinoSobranteTest.java`.

#### B. Módulo Consumo Interno (US-051)
1. Entidad de Dominio `ConsumoInterno.java` y repositorio `ConsumoInternoRepositorio.java`.
2. DTOs:
   - `RegistrarConsumoInternoComando`: `localId`, `trabajadorId`, `fecha`, `servicio`, `turno`, `productoId`, `cantidad`, `observacion`, `autorizadoPorId`, `supervisorPin`.
   - `FilaConsumoInternoDTO`: id, trabajadorNombre, productoNombre, fecha, servicio, cantidad, costo, requirioAutorizacion, autorizadoPorNombre, ocurridoEn.
3. Caso de uso `RegistrarConsumoInterno.java`:
   - Validar trabajador activo en el local.
   - Validar producto activo y vendible (no insumo crudo).
   - Validar si ya cuenta con consumo en la misma fecha y servicio:
     - Si existe previo: verificar que operador tenga `consumo_interno.autorizar` o validar PIN de supervisor proporcionado.
   - Descontar `Existencia` del producto.
   - Crear `MovimientoInventario` con `signo = 'salida'`, `origen = 'consumo_interno'`.
   - Guardar `ConsumoInterno`.
   - Auditar evento.
4. Caso de uso `ConsultarConsumosInternos.java`:
   - Listar consumos por local, fecha o trabajador con métricas agregadas (total platos consumidos, costo total valorizado).
5. Controlador `ConsumoInternoController.java`:
   - `GET /api/locales/{localId}/consumo-interno` (`consumo_interno.ver`).
   - `POST /api/locales/{localId}/consumo-interno` (`consumo_interno.crear`).
6. Tests unitarios: `RegistrarConsumoInternoTest.java`.

### Fase 3: Frontend Web (`rikos-web`)
1. **Cliente API (`cliente.ts`):**
   - Endpoints para `registrarSobranteProduccion`, `listarConsumosInternos`, `registrarConsumoInterno`.
2. **US-049 en Producción:**
   - Crear `ModalLiquidarSobrante.tsx`:
     - Muestra datos de la producción (producto, fecha, porciones preparadas).
     - Campo numérico de porciones sobrantes.
     - Selector de destino (`conservar`, `personal`, `perdida`, `reutilizacion`, `otro`) con explicación clara del impacto en stock.
     - Selector de motivo si aplica y observación.
   - En `PantallaProduccion.tsx`:
     - Columna o botón de acción *"Liquidar sobrante"* en filas de producción pendientes de liquidación. Badge con el destino si ya fue liquidado.
3. **US-051 en Inventario / Personal:**
   - Crear `ModalRegistrarConsumoInterno.tsx`:
     - Selector de trabajador del local.
     - Selector de producto/plato.
     - Detección automática del servicio vigente (desayuno/almuerzo/cena).
     - Validación en vivo de 1ra vs 2da comida: si es 2da comida, muestra advertencia ámbar y pide autorización (observación si tiene permiso o PIN de supervisor).
   - Crear `PantallaConsumoInterno.tsx` (ADM-25):
     - Tarjetas KPI: Total comidas del personal del día, costo total valorizado, comidas con autorización especial.
     - Filtro por fecha, servicio y trabajador.
     - Tabla de consumos con badges y detalles.
     - Botón `+ Registrar Comida de Personal`.
   - Navegación:
     - Enlace en `BarraLateral.tsx` bajo Inventario o Personal: `/admin/inventario/consumo-interno`.
     - Ruta en `apps/admin/index.tsx`.

### Fase 4: Documentación y Verificación
1. Crear documentación técnica en `rikos-backend/doc/historias/`:
   - `US-049-sobrantes-de-produccion.md`
   - `US-051-consumo-interno.md`
2. Actualizar `ESTADO.md` y `docs/producto/plan/README.md`.
3. Verificación con tests unitarios del backend (`mvn test`) y visual en frontend.
