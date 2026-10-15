# US-049 — Registrar el destino de los sobrantes de producción (sprint 9, entrega 5)

**Especificado en:** `docs/superpowers/plans/2026-10-15-us-049-us-051-sobrantes-y-consumo-interno.md`. En el backlog v3 es US-49. Épica EP-08 (Inventario, producción y compras). Hecho el 2026-10-15, backend y frontend.

## Qué se pidió
Permitir al personal autorizado registrar el destino del sobrante al finalizar un turno o tanda de producción confirmada (conservar para el siguiente turno, consumo de personal, pérdida / merma, reutilización en otra preparación u otro), descontando las existencias físicas y asentando el movimiento de salida en el Kardex solo cuando el destino no permanezca en el stock del local (RN-052, RN-057, Caso Crítico 11).

## Qué se construyó
- **Migración V54:** Columnas `sobrante`, `destino_sobrante`, `motivo_id`, `motivo_descripcion`, `observacion_sobrante` y `liquidado_en` añadidas a la tabla `produccion`. Permiso granular `produccion.sobrante` sembrado y asignado a los roles autorizados (`administrador central`, `administrador de local`, `cajero`, `cocina`), y tipo de evento `sobrante_produccion` en la bitácora transversal de auditoría.
- **Dominio (`Produccion.java`):**
  - Método `liquidarSobrante(BigDecimal sobrante, String destino, String motivoId, String motivoDescripcion, String observacion)`.
  - Validación de estado: solo producciones confirmadas y no liquidadas previamente pueden registrar destino.
  - Validación de consistencia: el sobrante no puede exceder el rendimiento real obtenido en la tanda.
- **Caso de uso (`RegistrarDestinoSobrante.java`):**
  - Valida la existencia de la tanda de producción y los permisos.
  - Si el destino es `conservar` o `reutilizacion`: el producto continúa en almacén, por lo que **no se genera salida contable** de inventario.
  - Si el destino es `personal`, `perdida` u `otro`: descuenta las existencias físicas del almacén (`ExistenciaRepositorio`) y registra un movimiento de tipo `salida` en `MovimientoInventario` con `origen = 'produccion'` y motivo descriptivo.
  - Asienta auditoría con evento `sobrante_produccion`.
- **API REST (`ProduccionController`):**
  - `POST /locales/{localId}/produccion/{id}/sobrante` (autorizado con permiso `produccion.sobrante` o roles directivos).
- **Frontend (`ModalLiquidarSobrante.tsx` y `PantallaProduccion.tsx`):**
  - Modal interactivo con resumen de la tanda, selector de destino con badges contextuales e indicador dinámico de impacto en inventario (alerta verde para stock conservado vs alerta roja para salida de existencias).
  - Selector de motivo y campo de observaciones detalladas.
  - Columna "Sobrante / Destino (US-049)" en la tabla principal de tandas de producción con badges por estado y botón rápido `⚖️ Liquidar`.

## Decisiones tomadas al ejecutar
- **Permisos Granulares Independientes de Nombre de Cargo:** Se estableció el permiso `produccion.sobrante` en la base de datos para desacoplar la lógica de negocio de la nomenclatura de cargos y permitir que administradores de sede, cajeros o personal de cocina asignado puedan operar la liquidación según la política operativa de cada local.
- **Impacto Diferencial en Kardex:** Para evitar distorsiones en los costos del lote, el rendimiento inicial de la producción permanece intacto en la tanda; solo los destinos que abandonan el local físico (`perdida`, `personal`, `otro`) generan asientos de salida en el Kardex.

## Cómo se verificó
- Tests unitarios en `RegistrarDestinoSobranteTest.java`:
  - `liquidarSobrante_conservar_noDescuentaInventario`: verifica que destinos conservados no alteran existencias ni generan movimientos de salida.
  - `liquidarSobrante_perdida_descuentaInventarioYRegistraKardex`: verifica el descuento de stock físico y la creación del movimiento con origen `'produccion'`.
  - `liquidarSobrante_sobranteExcedeRendimiento_falla`: verificación de rechazo ante cantidades que superan el rendimiento total producido.
  - `liquidarSobrante_yaLiquidada_falla`: previene doble liquidación o conflicto de concurrencia.
- Compilación y build exitoso de `rikos-web` (`npm run build`).
