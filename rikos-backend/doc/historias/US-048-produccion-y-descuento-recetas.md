# US-048 — Producir subrecetas y descontar receta al vender (sprint 9, entrega 4)

**Especificado en:** `docs/superpowers/plans/2026-10-13-us-048-produccion-subrecetas.md`. En el backlog v3 es US-48. Épica EP-08 (Inventario y compras). Hecho el 2026-10-13, backend y frontend.

## Qué se pidió
Permitir registrar la producción de subrecetas (bases de cocina como caldos, salsas o aderezos) escalando proporcionalmente los insumos base según el rendimiento obtenido, ingresando la subreceta al stock y recalculando su costo en cascada (RN-047). Al vender un plato que contiene una subreceta ya producida (Caso Crítico 11), el sistema debe descontar la subreceta directamente **sin volver a descontar los insumos base ya consumidos**.

## Qué se construyó
- **Migración V53:** Tabla `produccion`, constraints de integridad, permisos `produccion.ver`, `produccion.crear`, `produccion.registrar` y evento de auditoría `registro_produccion`.
- **Casos de uso:**
  - `RegistrarProduccion`: calcula factor de escala (`rendimientoObtenido / receta.rendimiento`), valida insumos, descuenta insumos con `origen = 'produccion'` y `signo = 'salida'`, ingresa subreceta con `origen = 'produccion'` y `signo = 'entrada'`, actualiza costo de producción en producto y dispara recálculo de costos en cascada en recetas dependientes (`CalculadoraCostoReceta.recalcularEnCascada`).
  - `DescontarInventarioPorComanda`: implementa la regla central de **Caso Crítico 11**. Al emitirse una comanda a cocina, si el ingrediente de la receta es una subreceta producida (`prod.isEsSubreceta()`), descuenta la subreceta terminada de las existencias sin duplicar el consumo de sus ingredientes originales.
  - `ConsultarProducciones`: consulta filtrada por fecha y tipo de tanda.
- **API REST (`ProduccionController`):**
  - `GET /api/locales/{localId}/produccion` (permiso `produccion.ver` o admin).
  - `GET /api/locales/{localId}/produccion/precarga` (permiso `produccion.ver` o admin).
  - `POST /api/locales/{localId}/produccion` (permiso `produccion.crear` o admin).
- **Frontend (`PantallaProduccion.tsx` y `ModalRegistrarProduccion.tsx`):**
  - Pantalla dedicada en `/admin/produccion` (ADM-19).
  - 4 KPIs: Tandas Producidas, Subrecetas Elaboradas, Porciones Preparadas, Porciones Vendidas.
  - Modal reactivo con selección de producto, precarga automática de porciones estimadas desde la programación semanal del menú, cálculo en vivo de factor de escala, consumo proyectado y costo unitario.

## Aclaración Arquitectónica: Descuento de Inventario vs Flujo de Cobro en Caja
- **Momento del Descuento:** En RIKO'S, el inventario se descuenta en el momento de la **emisión de la comanda a cocina** (`ComandarPedido` invocando `DescontarInventarioPorComanda`), per **RN-065** y **Caso Crítico 9**. Es en la cocina donde los insumos físicos se transforman o consumen.
- **Flujo de Cobro (`CobrarPedido` en `caja/`):** El cobro en caja es una operación puramente financiera (liquidación con efectivo, medios digitales o cargo a cuenta corriente de pensionistas). **No descuenta inventario** para evitar duplicar el consumo ya aplicado en comanda.
- **Anulaciones (`AnularLinea`):** Si una línea se anula antes de comanda, nunca descontó existencias. Si se anula después de comandar (`anulado_post_comanda`), el plato ya se cocinó/sirvió, por lo que **no se repone inventario** (se asume como merma/consumo per RN-065).
- **Cierre sin cobro (`TerminarPedido`):** Cierra mesas sin líneas comandadas pendientes de cobro; no altera inventario.
- Esta separación garantiza que el módulo de `caja/` y el ciclo de cobro operen con total independencia e integridad, sin efectos colaterales de inventario al cobrar.

## Cómo se verificó
- Tests unitarios de dominio y escalado proporcional en `RegistrarProduccionTest.java`.
- Tests de Caso Crítico 11 en `DescontarInventarioPorComandaTest.java`: verificación de descuento directo de subrecetas sin re-descontar insumos base.
- Tests RBAC en `ProduccionControllerAccesoTest.java`.
- Pruebas E2E en Playwright con capturas de evidencia visual en artifacts:
  - Producción de 5 kg de Salsa Huancaína escalando Ají Amarillo, Leche y Queso.
  - Kardex reflejando salidas de insumos e ingreso de salsa con origen `'produccion'`.
  - Comanda de Papa a la Huancaína descontando Salsa Huancaína sin duplicar insumos.
