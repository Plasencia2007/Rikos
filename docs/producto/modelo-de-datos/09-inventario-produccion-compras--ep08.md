<!-- Extraído de rikos-backend/doc/RIKOS-modelo-de-datos.md (fuente completa, con la numeración de sección original intacta -- ver esa numeración si algún comentario en el código cita p. ej. "13.1"). Partido por bloque el 2026-09-29 per docs/METODO-spec-driven.md §5. -->

# 11. Bloque 9 — Inventario, producción y compras

Corresponde a EP-08 y EP-09, historias US-043 a US-051. Todo lleva `local_id`: cada local tiene su propio inventario general.

## 11.1 `existencia`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `local_id` | identificador | No | PK compuesta. FK → `local` |
| `producto_id` | identificador | No | PK compuesta. FK → `producto` |
| `stock_actual` | cantidad | No | **Derivado** del libro de movimientos |
| `stock_minimo` | cantidad | Sí | Configurable **por producto y local** |
| `ultimo_movimiento_id` | identificador | Sí | Hasta dónde está calculado |
| `recalculado_en` | momento | No | |

**Reglas**

- **US-001 CA2.** La clave primaria compuesta local + producto es lo que garantiza **un único inventario general por local**, sin necesidad de una tabla `inventario` intermedia.
- **Igual que el saldo del pensionista (2.3 y 7.7), `stock_actual` es una proyección**, no la fuente de verdad. Si discrepa del libro, manda el libro.
- **RN-056.** Se permite **stock negativo con alerta**, sin bloquear el movimiento. Ninguna restricción de la base puede impedir que baje de cero: hacerlo rompería la operación real, donde el insumo se usa antes de que alguien registre la compra.
- **`04` § Inventario.** `stock_minimo` va aquí y no en `producto` porque es configurable por local.
- **No hay columna de costo promedio.** RN-047 valoriza con el **costo de última compra**, que vive en `producto`. Guardar además un promedio móvil por local introduciría un segundo criterio de valorización que la especificación no pide y que discreparía del primero en los reportes.

## 11.2 `movimiento_inventario` — el segundo libro

**Solo se agregan filas.**

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `local_id` | identificador | No | FK → `local` |
| `producto_id` | identificador | No | FK → `producto` |
| `origen` | enum('compra','produccion','venta','ajuste','transferencia','conteo','consumo_interno','anulacion_post_comanda') | No | |
| `signo` | enum('entrada','salida') | No | |
| `cantidad` | cantidad | No | Siempre positiva |
| `unidad_id` | identificador | No | FK → `unidad_medida` |
| `cantidad_base` | cantidad | No | Convertida a la unidad base del producto |
| `costo_unitario` | costo | Sí | Valorización del movimiento |
| `stock_anterior` | cantidad | No | **Requisito explícito** de `04` § Inventario |
| `stock_nuevo` | cantidad | No | Ídem |
| `referencia_tipo` | texto(40) | Sí | Tabla de origen |
| `referencia_id` | identificador | Sí | Fila de origen |
| `motivo_id` | identificador | Sí | FK → `motivo`. Obligatorio si `origen = ajuste` |
| `observacion` | texto(250) | Sí | |
| `ocurrido_en` | momento | No | |
| `registrado_en` | momento | No | |
| `registrado_por` | identificador | No | FK → `usuario` |

**Reglas**

- **RN-057.** El Kardex **es** esta tabla. El stock es su suma.
- **`04` § Inventario** exige guardar stock anterior, nuevo, diferencia, costo, usuario, fecha y local en cada ajuste. Esas columnas están todas, y se llenan en **todos** los orígenes, no solo en los ajustes: es lo que permite reconstruir el Kardex leyendo una sola tabla.
- **Caso crítico 11.** Producir una subreceta descuenta sus insumos. Vender el plato descuenta la subreceta **como producto**, no sus insumos otra vez. El enumerado de `origen` distingue los dos casos y es la única defensa contra el doble descuento.
- **Caso crítico 9.** Una anulación posterior a comanda genera un movimiento de `origen = anulacion_post_comanda`, que es una salida real aunque no haya venta. Se separa de `venta` para que el reporte de pérdidas exista.
- **Supuesto de modelado:** se usa `referencia_tipo` más `referencia_id` en lugar de ocho columnas de clave foránea. Es una relación polimórfica: la base no puede validarla, y hay que hacerlo en la aplicación. Se acepta el costo porque ocho columnas nulas en la tabla más voluminosa del sistema es peor.

## 11.3 `transferencia`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `local_origen_id` | identificador | No | FK → `local` |
| `local_destino_id` | identificador | No | FK → `local` |
| `estado` | enum('borrador','confirmada') | No | |
| `movimiento_salida_id` | identificador | Sí | FK → `movimiento_inventario` |
| `movimiento_entrada_id` | identificador | Sí | FK → `movimiento_inventario` |
| `observacion` | texto(250) | Sí | |
| `confirmada_en` | momento | Sí | |
| `confirmada_por` | identificador | Sí | FK → `usuario` |

**Regla, RN-006, `01` § Multilocal.** La transferencia genera **dos ajustes enlazados** de salida y entrada, aplicados **simultáneamente** al confirmarse. Los dos movimientos se escriben en la misma transacción; si uno falla, ninguno existe. Un inventario con la salida registrada y la entrada perdida es el peor resultado posible.

## 11.4 `conteo_fisico` y `detalle_conteo`

| `conteo_fisico` | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `local_id` | identificador | No | FK → `local` |
| `estado` | enum('abierto','conciliado','cerrado') | No | |
| `iniciado_en` | momento | No | Momento del snapshot |
| `cerrado_en` | momento | Sí | |
| `responsable_id` | identificador | No | FK → `usuario` |

| `detalle_conteo` | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `conteo_id` | identificador | No | FK → `conteo_fisico` |
| `producto_id` | identificador | No | FK → `producto` |
| `stock_snapshot` | cantidad | No | Stock al iniciar el conteo |
| `cantidad_contada` | cantidad | Sí | |
| `movimientos_periodo` | cantidad | No | Lo ocurrido **durante** el conteo |
| `diferencia` | cantidad | Sí | |
| `movimiento_ajuste_id` | identificador | Sí | FK → `movimiento_inventario` |

**Regla, caso crítico 12.** El conteo **no detiene la operación**. Por eso se guarda el snapshot inicial y, aparte, los movimientos ocurridos durante el conteo: la diferencia real es `cantidad_contada − (stock_snapshot + movimientos_periodo)`. Sin la columna `movimientos_periodo`, toda venta hecha durante el conteo aparecería como faltante.

## 11.5 `compra` y `detalle_compra`

| `compra` | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `local_id` | identificador | No | FK → `local` |
| `proveedor_id` | identificador | No | FK → `proveedor` |
| `tipo_documento` | enum('factura','boleta','ticket','sin_documento') | No | |
| `serie` | texto(10) | Sí | |
| `numero` | texto(20) | Sí | |
| `fecha_documento` | fecha | No | |
| `total` | dinero | No | |
| `ruta_adjunto` | texto(500) | Sí | Imagen o PDF del documento |
| `estado` | enum('borrador','confirmada','anulada') | No | |
| `confirmada_en` | momento | Sí | |
| `confirmada_por` | identificador | Sí | FK → `usuario` |

| `detalle_compra` | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `compra_id` | identificador | No | FK → `compra` |
| `producto_id` | identificador | No | FK → `producto` |
| `presentacion_id` | identificador | Sí | FK → `presentacion_compra` |
| `cantidad` | cantidad | No | En la presentación indicada |
| `cantidad_base` | cantidad | No | Convertida |
| `precio_unitario` | costo | No | |
| `subtotal` | dinero | No | |
| `movimiento_id` | identificador | Sí | FK → `movimiento_inventario` |

**Reglas**

- **RN-047, caso crítico 10.** Confirmar la compra aumenta el stock, actualiza `producto.costo_ultima_compra` y **recalcula las recetas futuras sin cambiar las ventas anteriores**. La segunda mitad de esa frase la garantiza el snapshot de `detalle_pedido`.
- Único por (`proveedor_id`, `tipo_documento`, `serie`, `numero`) cuando hay documento: evita registrar dos veces la misma factura, que es el error de digitación más común.
- **Fuera de alcance:** orden de compra y cuenta por pagar (`04` § Compras, exclusiones de `README`). No se modelan.
- Los adjuntos se protegen por permisos, igual que los de abonos.

## 11.6 `produccion`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `local_id` | identificador | No | FK → `local` |
| `producto_id` | identificador | No | FK → `producto`. Subreceta o plato |
| `receta_id` | identificador | No | FK → `receta` |
| `fecha` | fecha | No | |
| `servicio` | enum('desayuno','almuerzo','cena') | Sí | |
| `porciones_planificadas` | entero | Sí | |
| `porciones_preparadas` | entero | No | |
| `porciones_vendidas` | entero | Sí | Se completa al cierre del servicio |
| `rendimiento_obtenido` | cantidad | No | |
| `sobrante` | cantidad | Sí | |
| `destino_sobrante` | enum('conservar','personal','perdida','reutilizacion','otro') | Sí | |
| `motivo_id` | identificador | Sí | FK → `motivo` |
| `estado` | enum('borrador','confirmada') | No | |
| `registrado_por` | identificador | No | FK → `usuario` |

**Reglas**

- **`04` § Producción.** Producir descuenta los insumos e **ingresa el rendimiento producido**: son dos movimientos de inventario, salida de insumos y entrada de la subreceta.
- Se registran las tres cifras —planificado, preparado y vendido— porque el reporte de producción las pide juntas (`05` § Reportes mínimos).
- **El sistema no calcula automáticamente la lista de insumos requeridos** a partir de la planificación. Está dicho explícitamente y conviene no «mejorarlo».
- El destino del sobrante lo decide el encargado y **produce el ajuste de inventario correspondiente**: de ahí que el enumerado tenga cinco valores y cada uno genere un movimiento distinto.

## 11.7 `consumo_interno`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `local_id` | identificador | No | FK → `local` |
| `usuario_id` | identificador | No | FK → `usuario`. El trabajador |
| `fecha` | fecha | No | |
| `servicio` | enum('desayuno','almuerzo','cena') | No | |
| `turno` | texto(20) | Sí | |
| `producto_id` | identificador | No | FK → `producto` |
| `costo` | costo | No | Valorizado, no cobrado |
| `requirio_autorizacion` | booleano | No | |
| `autorizado_por` | identificador | Sí | FK → `usuario` |
| `movimiento_id` | identificador | Sí | FK → `movimiento_inventario` |

**Reglas, `04` § Consumo interno**

- Una comida sin cobro por trabajador y turno. Una **segunda** requiere autorización según permiso: de ahí las dos últimas columnas.
- Se reporta **separado de las ventas**. No es un pedido, no genera comprobante y no pasa por caja: por eso es una tabla propia y no un pedido con descuento del 100 %.

---

