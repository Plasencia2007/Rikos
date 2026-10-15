# US-051 — Registrar el consumo interno del personal (sprint 9, entrega 6)

**Especificado en:** `docs/superpowers/plans/2026-10-15-us-049-us-051-sobrantes-y-consumo-interno.md`. En el backlog v3 es US-51. Épica EP-08 (Inventario, producción y compras). Hecho el 2026-10-15, backend y frontend.

## Qué se pidió
Permitir el registro de las comidas asignadas a los trabajadores por turno, garantizando que la primera comida de la jornada sea libre de cobro para el personal, descontando las raciones de las existencias físicas de almacén con valuación a costo contable (sin pasar por comanda de salón, pedido ni cobro en caja). Las segundas raciones en el mismo turno deben requerir autorización obligatoria de un supervisor (RN-052, RN-057, política de bienestar de personal).

## Qué se construyó
- **Migración V54:** Tabla `consumo_interno` con claves foráneas hacia `local`, `usuario` (personal beneficiario), `producto` (plato consumido) y `movimiento_inventario`. Permisos granulares `consumo_interno.ver`, `consumo_interno.crear` y `consumo_interno.autorizar`, y evento de auditoría `consumo_interno`.
- **Dominio e Infraestructura:**
  - Entidad JPA `ConsumoInterno.java` y repositorio `ConsumoInternoRepositorio.java`.
  - Soporte para contar raciones previas en la misma fecha y turno para detectar segundas comidas (`contarPorUsuarioFechaYTurno`).
- **Casos de uso:**
  - `RegistrarConsumoInterno`:
    - Valida que el producto corresponda a alimentos o preparaciones aptas para consumo humano (no insumos crudos que no sean comida).
    - Evalúa automáticamente si es la 1ra comida o una ración posterior en el mismo turno.
    - Si es 2da comida: exige autorización de supervisor (mediante operador con rol supervisor/admin o ingreso de PIN de supervisor válido).
    - Descuenta las existencias físicas en el almacén del local.
    - Genera movimiento en `MovimientoInventario` con `origen = 'consumo_interno'`, `signo = 'salida'` y valuación contable a costo unitario promedio del producto.
    - Asienta el consumo en la tabla `consumo_interno` y registra auditoría.
  - `ConsultarConsumosInternos`:
    - `listarPorLocal`: lista detallada de consumos por fecha y personal beneficiario.
    - `obtenerResumen`: cálculo de KPIs acumulados (total comidas, costo contable total, segundas comidas autorizadas).
- **API REST (`ConsumoInternoController`):**
  - `GET /locales/{localId}/consumo-interno` (requiere `consumo_interno.ver`).
  - `GET /locales/{localId}/consumo-interno/resumen` (requiere `consumo_interno.ver`).
  - `POST /locales/{localId}/consumo-interno` (requiere `consumo_interno.crear`).
- **Frontend (`ModalRegistrarConsumoInterno.tsx` y `PantallaConsumoInterno.tsx`):**
  - Pantalla dedicada ADM-25 en `/admin/inventario/consumo-interno` y alias en `/admin/produccion/consumo-interno`.
  - 3 KPIs ejecutivos: Comidas del Día / Periodo, Costo Contable Total (S/.) y Raciones Autorizadas / Segundas raciones.
  - Filtro por fecha, buscador en tiempo real de trabajadores y platos.
  - Modal reactivo con selección de personal activo, plato de menú con stock disponible, servicio (desayuno/almuerzo/cena), turno y validación de supervisor para segundas comidas.
  - Enlaces de navegación incorporados en la barra lateral del panel administrativo.

## Decisiones tomadas al ejecutar
- **Aislamiento Total del Circuito de Venta:** El consumo interno no crea comandas de salón, pedidos ni pasa por la caja registradora. Esto evita distorsionar las métricas de ventas brutas, ticket promedio ni generar obligaciones tributarias de comprobantes electrónicos para raciones que constituyen gastos operativos de alimentación laboral.
- **Detección Automática de 2da Comida:** El backend verifica el conteo en base de datos para la combinación `(localId, usuarioId, fecha, turno)`. Si ya existe una comida previa en el mismo turno y no se provee autorización de supervisor o PIN válido, la solicitud es rechazada inmediatamente (`SEGUNDA_COMIDA_REQUIERE_AUTORIZACION`).
- **Valuación Contable:** Cada ración consumida registra el costo unitario promedio ponderado de la preparación en el momento del consumo, permitiendo generar reportes contables exactos del costo de beneficios al personal.

## Cómo se verificó
- Tests unitarios en `RegistrarConsumoInternoTest.java`:
  - `registrarConsumo_primeraComida_sinCobroYDescuentaInventario`: ración normal libre de cobro con salida en Kardex a costo unitario.
  - `registrarConsumo_segundaComidaSinSupervisor_falla`: rechazo automático si se intenta registrar una 2da comida en el mismo turno sin supervisor.
  - `registrarConsumo_segundaComidaConPinSupervisor_exito`: validación y aceptación con PIN supervisor.
  - `registrarConsumo_insumoCrudoRechazado`: verificación de rechazo si se selecciona un insumo no preparado.
- Pruebas de integración de endpoints en `ConsumoInternoController`.
- Compilación y build exitoso de `rikos-web` (`npm run build`).
