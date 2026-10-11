<!-- Extraído de rikos-backend/doc/RIKOS-modelo-de-datos.md (fuente completa, con la numeración de sección original intacta -- ver esa numeración si algún comentario en el código cita p. ej. "13.1"). Partido por bloque el 2026-09-29 per docs/METODO-spec-driven.md §5. -->

# 22. Representación tabla por tabla: todas las conexiones

Cada clave foránea del sistema, con su cardinalidad, su obligatoriedad y su comportamiento ante borrado y actualización. Es la vista que necesita un administrador de base de datos para escribir el DDL sin tener que interpretar nada.

## 22.1 Política general de integridad referencial

Tres reglas, y las excepciones se marcan una por una en la tabla siguiente.

| Situación | `ON DELETE` | `ON UPDATE` | Motivo |
|---|---|---|---|
| **Por defecto, toda FK** | `RESTRICT` | `RESTRICT` | Ninguna entidad del sistema se borra (2.5): se desactiva. Una FK en cascada sería una puerta a perder historial |
| **Detalle que no existe sin su cabecera** | `CASCADE` | `RESTRICT` | `detalle_compra` sin `compra` no significa nada. Aplica solo a detalles en estado borrador |
| **Referencia opcional a un catálogo** | `SET NULL` | `RESTRICT` | Desactivar un motivo no debe impedir leer el movimiento que lo usó |

**`ON UPDATE RESTRICT` en todas.** Las claves primarias son ULID generados en el origen y **nunca cambian**. Una cascada de actualización sería código muerto que oculta un error si alguna vez alguien intenta cambiar un identificador.

**La regla que más importa:** ninguna tabla de los dos libros —`movimiento_cuenta` y `movimiento_inventario`— ni `auditoria`, `sesion`, `cierre_caja` o `estado_cuenta_mensual` admite borrado por cascada desde ningún lado. Son el historial del negocio.

## 22.2 Módulo `identidad`

| Tabla origen | Columna | Tabla destino | Cardinalidad | Nulo | ON DELETE |
|---|---|---|---|---|---|
| `local` | `empresa_id` | `empresa` | N—1 | No | RESTRICT |
| `horario_servicio` | `local_id` | `local` | N—1 | No | RESTRICT |
| `dia_especial` | `local_id` | `local` | N—1 | No | RESTRICT |
| `horario_dia_especial` | `dia_especial_id` | `dia_especial` | N—1 | No | CASCADE |
| `usuario_rol` | `usuario_id` | `usuario` | N—1 | No | CASCADE |
| `usuario_rol` | `rol_id` | `rol` | N—1 | No | RESTRICT |
| `usuario_rol` | `asignado_por` | `usuario` | N—1 | No | RESTRICT |
| `rol_permiso` | `rol_id` | `rol` | N—1 | No | CASCADE |
| `rol_permiso` | `permiso_id` | `permiso` | N—1 | No | RESTRICT |
| `usuario_local` | `usuario_id` | `usuario` | N—1 | No | CASCADE |
| `usuario_local` | `local_id` | `local` | N—1 | No | RESTRICT |
| `usuario_permiso_excepcion` | `usuario_id` | `usuario` | N—1 | No | CASCADE |
| `usuario_permiso_excepcion` | `permiso_id` | `permiso` | N—1 | No | RESTRICT |
| `sesion` | `usuario_id` | `usuario` | N—1 | **Sí** | RESTRICT |
| `sesion` | `local_id` | `local` | N—1 | Sí | RESTRICT |
| `bloqueo_pin` | `local_id` | `local` | N—1 | No | RESTRICT |

**Nota sobre las cascadas.** `usuario_rol`, `rol_permiso`, `usuario_local` y `usuario_permiso_excepcion` son tablas de relación puras: no llevan historial propio, y quitar un rol a un usuario es una operación normal. El usuario mismo **nunca** se borra, así que la cascada no se dispara nunca en la práctica; está para que quitar la asignación sea una sola operación.

`sesion.usuario_id` admite nulo porque un intento fallido con un nombre de usuario inexistente **también se registra** (3.10). Si la columna fuera obligatoria, justo ese caso —el que más interesa vigilar— no se podría guardar.

## 22.3 Módulo `catalogo`

| Tabla origen | Columna | Tabla destino | Cardinalidad | Nulo | ON DELETE |
|---|---|---|---|---|---|
| `categoria_producto` | `categoria_padre_id` | `categoria_producto` | N—1 | Sí | RESTRICT |
| `producto` | `categoria_id` | `categoria_producto` | N—1 | No | RESTRICT |
| `producto` | `unidad_base_id` | `unidad_medida` | N—1 | No | RESTRICT |
| `producto_servicio` | `producto_id` | `producto` | N—1 | No | CASCADE |
| `presentacion_compra` | `producto_id` | `producto` | N—1 | No | RESTRICT |
| `presentacion_compra` | `unidad_id` | `unidad_medida` | N—1 | No | RESTRICT |
| `conversion_unidad` | `producto_id` | `producto` | N—1 | No | CASCADE |
| `conversion_unidad` | `unidad_origen_id` | `unidad_medida` | N—1 | No | RESTRICT |
| `conversion_unidad` | `unidad_destino_id` | `unidad_medida` | N—1 | No | RESTRICT |
| `receta` | `producto_id` | `producto` | 1—1 | No | RESTRICT |
| `receta` | `unidad_rendimiento_id` | `unidad_medida` | N—1 | No | RESTRICT |
| `ingrediente_receta` | `receta_id` | `receta` | N—1 | No | CASCADE |
| `ingrediente_receta` | `producto_id` | `producto` | N—1 | No | RESTRICT |
| `ingrediente_receta` | `unidad_id` | `unidad_medida` | N—1 | No | RESTRICT |

**`presentacion_compra` es RESTRICT y `conversion_unidad` es CASCADE**, aunque ambas cuelgan del producto. La diferencia: una presentación aparece en `detalle_compra` de compras ya registradas, así que borrarla rompería historial. Una conversión de unidades es configuración pura que nadie referencia.

## 22.4 Módulo `menu`

| Tabla origen | Columna | Tabla destino | Cardinalidad | Nulo | ON DELETE |
|---|---|---|---|---|---|
| `dia_menu` | `semana_menu_id` | `semana_menu` | N—1 | No | CASCADE |
| `opcion_menu` | `dia_menu_id` | `dia_menu` | N—1 | No | CASCADE |
| `opcion_menu` | `producto_id` | `producto` | N—1 | No | RESTRICT |
| `disponibilidad_producto` | `local_id` | `local` | N—1 | No | RESTRICT |
| `disponibilidad_producto` | `producto_id` | `producto` | N—1 | No | CASCADE |
| `disponibilidad_producto` | `motivo_id` | `motivo` | N—1 | Sí | SET NULL |
| `disponibilidad_producto` | `marcado_por` | `usuario` | N—1 | No | RESTRICT |
| `semana_menu` | `confirmada_por` | `usuario` | N—1 | Sí | RESTRICT |

**La cascada de `semana_menu` solo aplica en borrador.** Una semana en estado `confirmado` o `en_servicio` ya tiene ventas asociadas a sus opciones y no se borra: se desactiva. La comprobación es de aplicación.

## 22.5 Módulo `pensionistas`

| Tabla origen | Columna | Tabla destino | Cardinalidad | Nulo | ON DELETE |
|---|---|---|---|---|---|
| `precio_plan` | `plan_tarifario_id` | `plan_tarifario` | N—1 | No | CASCADE |
| `precio_plan` | `producto_id` | `producto` | N—1 | No | RESTRICT |
| `pensionista` | `plan_tarifario_id` | `plan_tarifario` | N—1 | No | RESTRICT |
| `responsable` | `pensionista_id` | `pensionista` | 1—1 | No | RESTRICT |
| `cuenta_pensionista` | `pensionista_id` | `pensionista` | 1—1 | No | RESTRICT |
| `saldo_cuenta_cache` | `cuenta_id` | `cuenta_pensionista` | 1—1 | No | CASCADE |
| `periodo_cuenta` | `cuenta_id` | `cuenta_pensionista` | N—1 | No | RESTRICT |
| `estado_cuenta_mensual` | `periodo_id` | `periodo_cuenta` | 1—1 | No | RESTRICT |
| `movimiento_cuenta` | `cuenta_id` | `cuenta_pensionista` | N—1 | No | RESTRICT |
| `movimiento_cuenta` | `local_id` | `local` | N—1 | **No** | RESTRICT |
| `movimiento_cuenta` | `periodo_id` | `periodo_cuenta` | N—1 | No | RESTRICT |
| `movimiento_cuenta` | `pedido_id` | `pedido` | N—1 | Sí | RESTRICT |
| `movimiento_cuenta` | `abono_id` | `abono` | 1—1 | Sí | RESTRICT |
| `movimiento_cuenta` | `motivo_id` | `motivo` | N—1 | Sí | RESTRICT |
| `movimiento_cuenta` | `registrado_por` | `usuario` | N—1 | No | RESTRICT |
| `pre_ticket` | `pensionista_id` | `pensionista` | N—1 | No | RESTRICT |
| `pre_ticket` | `local_id` | `local` | N—1 | No | RESTRICT |
| `pre_ticket` | `vinculado_por` | `usuario` | N—1 | Sí | RESTRICT |
| `abono` | `pensionista_id` | `pensionista` | N—1 | No | RESTRICT |
| `abono` | `local_id` | `local` | N—1 | Sí | RESTRICT |
| `abono` | `turno_caja_id` | `turno_caja` | N—1 | Sí | RESTRICT |
| `abono` | `registrado_por` / `aprobado_por` | `usuario` | N—1 | Sí | RESTRICT |
| `abono` | `motivo_rechazo_id` | `motivo` | N—1 | Sí | RESTRICT |

**`saldo_cuenta_cache` es la única CASCADE del módulo**, y es coherente: es caché. Si desapareciera la cuenta, el caché no tiene sentido; y si el caché se pierde, se reconstruye sumando el libro.

**`movimiento_cuenta.motivo_id` es RESTRICT, no SET NULL**, a diferencia de `disponibilidad_producto`. La razón: en un ajuste de cuenta el motivo es parte del hecho económico y el estado de cuenta debe poder mostrarlo años después. Un motivo referenciado por un movimiento se desactiva, no se borra.

## 22.6 Módulo `venta`

| Tabla origen | Columna | Tabla destino | Cardinalidad | Nulo | ON DELETE |
|---|---|---|---|---|---|
| `mesa` | `local_id` | `local` | N—1 | No | RESTRICT |
| `mesa` | `mesa_principal_id` | `mesa` | N—1 | Sí | SET NULL |
| `pedido` | `local_id` | `local` | N—1 | No | RESTRICT |
| `pedido` | `mesa_id` | `mesa` | N—1 | Sí | RESTRICT |
| `pedido` | `turno_caja_id` | `turno_caja` | N—1 | Sí | RESTRICT |
| `pedido` | `mozo_id` / `cerrado_por` / `reabierto_por` | `usuario` | N—1 | Sí | RESTRICT |
| `pedido` | `motivo_descuento_id` | `motivo` | N—1 | Sí | RESTRICT |
| `cuenta_persona` | `pedido_id` | `pedido` | N—1 | No | CASCADE |
| `cuenta_persona` | `pensionista_id` | `pensionista` | N—1 | Sí | RESTRICT |
| `cuenta_persona` | `pre_ticket_id` | `pre_ticket` | 1—1 **UQ** | Sí | RESTRICT |
| `detalle_pedido` | `cuenta_persona_id` | `cuenta_persona` | N—1 | No | CASCADE |
| `detalle_pedido` | `producto_id` | `producto` | N—1 | No | RESTRICT |
| `detalle_pedido` | `receta_id` | `receta` | N—1 | Sí | RESTRICT |
| `detalle_pedido` | `plan_tarifario_id` | `plan_tarifario` | N—1 | Sí | RESTRICT |
| `detalle_pedido` | `motivo_anulacion_id` | `motivo` | N—1 | Sí | RESTRICT |
| `detalle_pedido` | `anulado_por` | `usuario` | N—1 | Sí | RESTRICT |
| `detalle_pedido_componente` | `detalle_pedido_id` | `detalle_pedido` | N—1 | No | CASCADE |
| `detalle_pedido_componente` | `producto_id` | `producto` | N—1 | No | RESTRICT |
| `detalle_pedido_componente` | `opcion_menu_id` | `opcion_menu` | N—1 | Sí | RESTRICT |
| `comanda` | `pedido_id` | `pedido` | N—1 | No | RESTRICT |
| `comanda` | `local_id` | `local` | N—1 | No | RESTRICT |
| `comanda` | `emitida_por` | `usuario` | N—1 | No | RESTRICT |
| `detalle_comanda` | `comanda_id` | `comanda` | N—1 | No | CASCADE |
| `detalle_comanda` | `detalle_pedido_id` | `detalle_pedido` | N—1 | No | RESTRICT |
| `comprobante_resumen` | `pedido_id` | `pedido` | N—1 | No | RESTRICT |
| `comprobante_resumen` | `cuenta_persona_id` | `cuenta_persona` | N—1 | Sí | RESTRICT |

**Las cascadas del pedido son solo para pedidos abiertos.** Un pedido `cerrado` no se borra nunca: RN-074 exige corregirlo con documento relacionado. La cascada existe porque un pedido abierto que se anula sin haber comandado sí puede desaparecer entero. La regla que lo controla es de aplicación, y conviene además un `trigger` de base de datos que rechace el borrado cuando `estado = 'cerrado'`: es la última línea de defensa contra un `DELETE` mal escrito en una consulta manual.

**`comanda` es RESTRICT, no CASCADE**, aunque cuelgue del pedido. Una comanda impresa ya salió por la impresora de cocina: es un hecho ocurrido en el mundo físico y su registro no se borra.

## 22.7 Módulo `caja`

| Tabla origen | Columna | Tabla destino | Cardinalidad | Nulo | ON DELETE |
|---|---|---|---|---|---|
| `caja` | `local_id` | `local` | N—1 | No | RESTRICT |
| `turno_caja` | `caja_id` | `caja` | N—1 | No | RESTRICT |
| `turno_caja` | `local_id` | `local` | N—1 | No | RESTRICT |
| `turno_caja` | `cajero_id` | `usuario` | N—1 | No | RESTRICT |
| `cierre_caja` | `turno_caja_id` | `turno_caja` | 1—1 **UQ** | No | RESTRICT |
| `cierre_caja` | `cerrado_por` | `usuario` | N—1 | No | RESTRICT |
| `movimiento_caja` | `turno_caja_id` | `turno_caja` | N—1 | No | RESTRICT |
| `movimiento_caja` | `local_id` | `local` | N—1 | No | RESTRICT |
| `movimiento_caja` | `medio_pago_id` | `medio_pago` | N—1 | No | RESTRICT |
| `movimiento_caja` | `pago_id` | `pago` | 1—1 | Sí | RESTRICT |
| `movimiento_caja` | `abono_id` | `abono` | 1—1 | Sí | RESTRICT |
| `movimiento_caja` | `registrado_por` | `usuario` | N—1 | No | RESTRICT |
| `pago` | `pedido_id` | `pedido` | N—1 | No | RESTRICT |
| `pago` | `cuenta_persona_id` | `cuenta_persona` | N—1 | Sí | RESTRICT |
| `pago` | `medio_pago_id` | `medio_pago` | N—1 | No | RESTRICT |
| `pago` | `turno_caja_id` | `turno_caja` | N—1 | No | RESTRICT |
| `pago` | `registrado_por` | `usuario` | N—1 | No | RESTRICT |

**Todo el módulo es RESTRICT, sin una sola excepción.** Es dinero. Nada de lo que entra aquí se borra en cascada por ningún camino.

**`pago.pedido_id` es RESTRICT mientras `cuenta_persona.pedido_id` es CASCADE**, y eso crea una asimetría deliberada: un pedido con pagos registrados **no se puede borrar**, porque la FK lo impide desde la base de datos, sin depender de que la aplicación se acuerde.

## 22.8 Módulo `inventario`

| Tabla origen | Columna | Tabla destino | Cardinalidad | Nulo | ON DELETE |
|---|---|---|---|---|---|
| `existencia` | `local_id` + `producto_id` | `local`, `producto` | PK compuesta | No | RESTRICT |
| `movimiento_inventario` | `local_id` | `local` | N—1 | No | RESTRICT |
| `movimiento_inventario` | `producto_id` | `producto` | N—1 | No | RESTRICT |
| `movimiento_inventario` | `unidad_id` | `unidad_medida` | N—1 | No | RESTRICT |
| `movimiento_inventario` | `motivo_id` | `motivo` | N—1 | Sí | RESTRICT |
| `movimiento_inventario` | `registrado_por` | `usuario` | N—1 | No | RESTRICT |
| `movimiento_inventario` | `referencia_id` | — | **Polimórfica** | Sí | — |
| `transferencia` | `local_origen_id` / `local_destino_id` | `local` | N—1 | No | RESTRICT |
| `transferencia` | `movimiento_salida_id` / `movimiento_entrada_id` | `movimiento_inventario` | 1—1 | Sí | RESTRICT |
| `conteo_fisico` | `local_id` | `local` | N—1 | No | RESTRICT |
| `conteo_fisico` | `responsable_id` | `usuario` | N—1 | No | RESTRICT |
| `detalle_conteo` | `conteo_id` | `conteo_fisico` | N—1 | No | CASCADE |
| `detalle_conteo` | `producto_id` | `producto` | N—1 | No | RESTRICT |
| `detalle_conteo` | `movimiento_ajuste_id` | `movimiento_inventario` | 1—1 | Sí | RESTRICT |
| `compra` | `local_id` | `local` | N—1 | No | RESTRICT |
| `compra` | `proveedor_id` | `proveedor` | N—1 | No | RESTRICT |
| `compra` | `confirmada_por` | `usuario` | N—1 | Sí | RESTRICT |
| `detalle_compra` | `compra_id` | `compra` | N—1 | No | CASCADE |
| `detalle_compra` | `producto_id` | `producto` | N—1 | No | RESTRICT |
| `detalle_compra` | `presentacion_id` | `presentacion_compra` | N—1 | Sí | RESTRICT |
| `detalle_compra` | `movimiento_id` | `movimiento_inventario` | 1—1 | Sí | RESTRICT |
| `produccion` | `local_id` / `producto_id` / `receta_id` | `local`, `producto`, `receta` | N—1 | No | RESTRICT |
| `produccion` | `motivo_id` | `motivo` | N—1 | Sí | RESTRICT |
| `produccion` | `registrado_por` | `usuario` | N—1 | No | RESTRICT |
| `consumo_interno` | `local_id` / `producto_id` | `local`, `producto` | N—1 | No | RESTRICT |
| `consumo_interno` | `usuario_id` / `autorizado_por` | `usuario` | N—1 | No / Sí | RESTRICT |
| `consumo_interno` | `movimiento_id` | `movimiento_inventario` | 1—1 | Sí | RESTRICT |

**`movimiento_inventario.referencia_id` es la única columna del modelo sin clave foránea dentro del mismo servicio.** Es una relación polimórfica: `referencia_tipo` dice de qué tabla viene y `referencia_id` cuál fila. La base de datos **no puede validarla**. Se acepta el costo porque la alternativa —ocho columnas de clave foránea, siete de ellas nulas, en la tabla más voluminosa del sistema— es peor en espacio, en índices y en legibilidad.

**La compensación:** una comprobación programada que recorra los movimientos del día y verifique que cada `referencia_id` existe en la tabla que dice `referencia_tipo`. Cuesta poco y detecta un error de código que de otro modo aparecería meses después como un Kardex que no cuadra.

**La cascada de `detalle_compra` aplica solo en estado `borrador`.** Una compra confirmada ya movió stock y actualizó costos: no se borra, se anula.

## 22.9 Módulo `delivery`

| Tabla origen | Columna | Tabla destino | Cardinalidad | Nulo | ON DELETE |
|---|---|---|---|---|---|
| `pedido_delivery` | `pedido_id` | `pedido` | 1—1 **UQ** | No | RESTRICT |
| `pedido_delivery` | `local_id` | `local` | N—1 | No | RESTRICT |
| `pedido_delivery` | `motorizado_id` | `usuario` | N—1 | Sí | RESTRICT |
| `pedido_delivery` | `liquidado_por` | `usuario` | N—1 | Sí | RESTRICT |
| `historial_delivery` | `pedido_delivery_id` | `pedido_delivery` | N—1 | No | CASCADE |
| `historial_delivery` | `registrado_por` | `usuario` | N—1 | No | RESTRICT |
| `pedido_recojo` | `pedido_id` | `pedido` | 1—1 **UQ** | No | RESTRICT |
| `pedido_recojo` | `local_id` | `local` | N—1 | No | RESTRICT |

**Restricción que la base no puede expresar y hay que validar en la aplicación:** un pedido tiene `pedido_delivery` **o** `pedido_recojo`, nunca ambos, y solo si su `canal` es el correspondiente. Es una restricción entre tres tablas; se comprueba al crear.

## 22.10 Módulo `comun`

| Tabla origen | Columna | Tabla destino | Cardinalidad | Nulo | ON DELETE |
|---|---|---|---|---|---|
| `auditoria` | `usuario_id` | `usuario` | N—1 | No | RESTRICT |
| `auditoria` | `local_id` | `local` | N—1 | Sí | RESTRICT |
| `auditoria` | `motivo_id` | `motivo` | N—1 | Sí | RESTRICT |
| `auditoria` | `entidad_id` | — | **Polimórfica** | No | — |
| `outbox` | `local_id` | `local` | N—1 | Sí | RESTRICT |
| `conflicto_sincronizacion` | `local_id` | `local` | N—1 | No | RESTRICT |
| `conflicto_sincronizacion` | `resuelto_por` | `usuario` | N—1 | Sí | RESTRICT |

## 22.11 `servicio-facturacion`

| Tabla origen | Columna | Tabla destino | Cardinalidad | Nulo | ON DELETE |
|---|---|---|---|---|---|
| `comprobante_electronico` | `serie_id` | `serie_comprobante` | N—1 | Sí | RESTRICT |
| `comprobante_electronico` | `documento_afectado_id` | `comprobante_electronico` | N—1 | Sí | RESTRICT |
| `intento_emision` | `comprobante_id` | `comprobante_electronico` | N—1 | No | RESTRICT |
| `envio_correo` | `comprobante_id` | `comprobante_electronico` | N—1 | No | RESTRICT |
| `comprobante_electronico` | `pedido_id` | `pedido` **(otra BD)** | — | Sí | **Sin FK** |
| `comprobante_electronico` | `cuenta_persona_id` | `cuenta_persona` **(otra BD)** | — | Sí | **Sin FK** |
| `serie_comprobante` | `local_codigo` | `emisor_local` | N—1 | Sí | RESTRICT |
| `comprobante_electronico` | `local_id` | `emisor_local` | N—1 | No | RESTRICT |
| `emisor_local` | `local_id` | `local` **(otra BD)** | — | No | **Sin FK** |

**Ninguna cascada en todo el servicio.** Un comprobante emitido es un documento fiscal: no se borra por ningún camino, ni siquiera el rechazado. Y sus intentos de emisión son la prueba de qué se envió y qué respondió el proveedor.

---

# 23. Índices sugeridos

PostgreSQL crea automáticamente un índice por cada clave primaria y por cada restricción de unicidad. **No los crea para las claves foráneas**, y esa es la primera fuente de lentitud en un esquema como este.

## 23.1 Regla general

**Toda columna de clave foránea lleva índice.** No se listan una por una en las tablas siguientes: se dan por creadas. Lo que sí se lista son los índices **compuestos y parciales**, que son los que resuelven las consultas reales del sistema y que nadie crea por costumbre.

## 23.2 Índices que sostienen la operación diaria

| Tabla | Índice | Tipo | Qué consulta resuelve |
|---|---|---|---|
| `pedido` | (`local_id`, `estado`) donde `estado <> 'cerrado'` | Parcial | El mapa de mesas y la lista de pedidos abiertos. Es la consulta más frecuente del POS y el índice parcial la mantiene pequeña aunque haya años de ventas |
| `pedido` | (`local_id`, `abierto_en`) | Compuesto | Reportes por rango de fechas y local |
| `pedido` | (`turno_caja_id`) donde `turno_caja_id` no es nulo | Parcial | Cierre de turno: qué ventas pertenecen a este turno |
| `mesa` | (`local_id`, `estado`) | Compuesto | Mapa de mesas |
| `cuenta_persona` | (`pre_ticket_id`) **UQ** donde no es nulo | Único parcial | **Impide la doble vinculación del pre-ticket bajo concurrencia.** No es optimización, es integridad |
| `detalle_pedido` | (`cuenta_persona_id`, `estado`) | Compuesto | Qué falta comandar de esta cuenta |
| `comanda` | (`local_id`, `emitida_en`) | Compuesto | Reimpresión y auditoría de comandas del día |
| `turno_caja` | (`caja_id`) **UQ** donde `estado = 'abierto'` | Único parcial | **Garantiza un turno abierto por caja.** Sostiene RN-073 desde la base de datos, no desde la aplicación |
| `pre_ticket` | (`codigo`) **UQ** | Único | Requisito de `06` § Reglas de integridad |
| `pre_ticket` | (`local_id`, `estado`, `vence_en`) donde `estado = 'pendiente'` | Parcial | El proceso que vence los pre-tickets al terminar el servicio |
| `disponibilidad_producto` | (`local_id`, `producto_id`) **UQ** | Único | Un estado de disponibilidad por producto y local |
| `producto_servicio` | (`servicio`, `producto_id`) | Compuesto | «Son las 7, ¿qué puedo vender?» |
| `horario_servicio` | (`local_id`, `dia_semana`, `servicio`) | Compuesto | Qué servicio rige ahora mismo. El kiosco lo consulta en cada ingreso |
| `dia_especial` | (`local_id`, `fecha`) **UQ** | Único | Un día especial por fecha y local; se consulta antes que el horario semanal |

## 23.3 Índices de los dos libros

Son las tablas que más crecen. Un índice mal elegido aquí se nota al año.

| Tabla | Índice | Tipo | Qué consulta resuelve |
|---|---|---|---|
| `movimiento_cuenta` | (`cuenta_id`, `ocurrido_en`) | Compuesto | Estado de cuenta y saldo. **El más importante del sistema** |
| `movimiento_cuenta` | (`periodo_id`) | Simple | Cierre mensual |
| `movimiento_cuenta` | (`cuenta_id`, `id`) | Compuesto | Recálculo incremental del caché desde `ultimo_movimiento_id`. Funciona porque el ULID ordena cronológicamente (P-14) |
| `movimiento_cuenta` | (`local_id`, `ocurrido_en`) | Compuesto | Caso crítico 20: movimientos por local |
| `movimiento_inventario` | (`local_id`, `producto_id`, `ocurrido_en`) | Compuesto | **El Kardex.** Sin él, cada consulta de stock recorre la tabla entera |
| `movimiento_inventario` | (`referencia_tipo`, `referencia_id`) | Compuesto | Trazar un movimiento hasta su origen, y la comprobación de integridad de 22.8 |
| `movimiento_inventario` | (`local_id`, `ocurrido_en`) | Compuesto | Reporte de Kardex por periodo |
| `auditoria` | (`local_id`, `ocurrido_en`) | Compuesto | Bitácora filtrada, ADM-31 |
| `auditoria` | (`entidad`, `entidad_id`) | Compuesto | «¿Qué le pasó a este registro?» |
| `auditoria` | (`usuario_id`, `ocurrido_en`) | Compuesto | «¿Qué hizo este usuario?» |

**Nota sobre el volumen.** Con tres locales y unos 200 pedidos diarios por local, `movimiento_inventario` crece del orden de 3.000 filas al día: cerca de un millón al año. Es perfectamente manejable para PostgreSQL **con estos índices** y penoso sin ellos. Si algún día creciera más, el particionado por rango de fecha es el siguiente paso, y el índice `(local_id, producto_id, ocurrido_en)` ya está preparado para eso.

## 23.4 Índices de sincronización y colas

| Tabla | Índice | Tipo | Qué consulta resuelve |
|---|---|---|---|
| `outbox` | (`estado`, `creado_en`) donde `estado = 'pendiente'` | Parcial | El sincronizador lee solo lo pendiente. Sin el índice parcial, la consulta empeora a medida que crece el histórico de enviados |
| `sesion` | (`usuario_id`, `iniciada_en`) | Compuesto | Auditoría de accesos |
| `sesion` | (`resultado`, `iniciada_en`) donde `resultado <> 'exitoso'` | Parcial | Detección de intentos de acceso fallidos |
| `bloqueo_pin` | (`local_id`, `dispositivo`) **UQ** | Único | Un contador por dispositivo |

## 23.5 Índices de `servicio-facturacion`

| Tabla | Índice | Tipo | Qué consulta resuelve |
|---|---|---|---|
| `comprobante_electronico` | (`pedido_id`) **UQ** donde no es nulo y el tipo no es nota de crédito | Único parcial | **Impide emitir dos comprobantes para la misma venta.** Es la defensa contra el duplicado, y vive en la base de datos porque el proveedor no ofrece idempotencia (H-02) |
| `comprobante_electronico` | (`estado_fiscal`, `proximo_intento_en`) donde `estado_fiscal` en (`pendiente`,`enviado`) | Parcial | La cola de emisión |
| `comprobante_electronico` | (`requiere_revision`) donde es verdadero | Parcial | La bandeja de comprobantes |
| `comprobante_electronico` | (`serie`, `numero`) **UQ** donde `numero` no es nulo | Único parcial | Detecta un correlativo asignado dos veces |
| `comprobante_electronico` | (`emitido_en`) | Simple | Reconciliación por rango de fechas (H-03) |
| `comprobante_electronico` | (`huella`, `emitido_en`) | Compuesto | Reconciliación de DT-009: encontrar nuestro documento en el listado del proveedor |
| `intento_emision` | (`comprobante_id`, `numero_intento`) **UQ** | Único | |
| `envio_correo` | (`estado`) donde `estado` en (`pendiente`,`rebotado`) | Parcial | Cola de correos y bandeja de rebotes |
| `emisor_local` | (`local_codigo`) **UQ** | Único | Resolución del emisor al armar el payload |
| `comprobante_resumen` | (`pedido_id`) | Simple | Reimpresión desde caja |
| `comprobante_resumen` | (`cuenta_persona_id`) | Simple | Comprobantes del pensionista en el portal (RN-028) |

**El índice único parcial sobre `pedido_id` merece atención.** Es el que garantiza que una venta no genere dos boletas, incluso si el evento llega duplicado por la cola. Tiene que excluir las notas de crédito, porque una venta puede tener su boleta **y** su nota de crédito, ambas apuntando al mismo pedido.

---

# 24. Campos de auditoría estándar

Dos mecanismos distintos que conviene no confundir (2.8). Aquí queda definido exactamente qué lleva cada tabla.

## 24.1 El juego estándar de columnas de traza

| Columna | Tipo | Nulo | Se escribe |
|---|---|---|---|
| `creado_en` | momento | No | Al insertar. Valor por defecto: el momento actual |
| `creado_por` | identificador | Sí | Al insertar. Nulo solo en filas creadas por procesos del sistema |
| `actualizado_en` | momento | Sí | En cada actualización |
| `actualizado_por` | identificador | Sí | En cada actualización |
| `version` | entero | No | Bloqueo optimista. **Solo en datos maestros** (2.9) |

`creado_por` y `actualizado_por` **no llevan clave foránea a `usuario`** en las tablas de alto volumen. En las demás sí. El criterio: en `movimiento_inventario`, que crece un millón de filas al año, la FK añade un índice que nunca se consulta por ese lado; en `producto`, que tiene cientos de filas, no cuesta nada y ayuda.

## 24.2 Qué lleva cada grupo de tablas

| Grupo de tablas | Traza completa | `version` | Fila en `auditoria` |
|---|---|---|---|
| Datos maestros: `local`, `usuario`, `producto`, `receta`, `plan_tarifario`, `proveedor`, `categoria_producto` | Sí | **Sí** | Solo en los eventos que `01` enumera: cambio de receta, de precio, de permiso, de plan |
| Catálogos casi estáticos: `rol`, `permiso`, `medio_pago`, `unidad_medida` | **No llevan traza** | No | No por sí mismos |
| Tablas de relación: `usuario_rol`, `rol_permiso`, `usuario_local`, `producto_servicio` | Solo `creado_en` y `creado_por` | No | **Sí en `rol_permiso`, `usuario_rol` y `usuario_permiso_excepcion`**: los tres son cambio de permiso, que `01` § Auditoría transversal exige registrar |
| Transaccionales: `pedido`, `compra`, `produccion`, `conteo_fisico`, `abono` | Sí | No | Sí en descuentos, anulaciones, reaperturas y aprobación de abonos |
| **Libros**: `movimiento_cuenta`, `movimiento_inventario` | Columnas propias: `registrado_en`, `registrado_por`, `ocurrido_en` | No | Sí en los ajustes y transferencias |
| **Inalterables**: `auditoria`, `sesion`, `cierre_caja`, `estado_cuenta_mensual`, `intento_emision` | Solo de creación | No | No: son ellas mismas el registro |

## 24.3 Por qué los libros no usan `creado_en`

Usan `ocurrido_en` y `registrado_en`, y la diferencia importa (7.6). `ocurrido_en` es cuándo pasó el hecho económico; `registrado_en` es cuándo la fila llegó a la base. Durante un corte de Internet pueden separarse horas. El estado de cuenta se arma por `ocurrido_en`; la sincronización razona con `registrado_en`. Una sola columna `creado_en` obligaría a elegir, y cualquiera de las dos elecciones rompe uno de los dos usos.

## 24.4 Cómo se escribe la traza

**No con `triggers` de base de datos.** Con la capa de aplicación: Spring Data JPA con `@CreatedDate`, `@CreatedBy`, `@LastModifiedDate` y `@LastModifiedBy`, y un `AuditorAware` que devuelve el usuario de la sesión.

**El motivo de no usar `triggers`:** el nodo del local y la nube corren la misma aplicación pero pueden tener versiones de esquema distintas durante un despliegue. Un `trigger` es lógica que vive solo en la base y que nadie recuerda al depurar; además no sabe qué usuario está operando sin pasarle una variable de sesión, que es exactamente el tipo de acoplamiento que después falla en silencio.

**La tabla `auditoria` sí se escribe explícitamente**, desde la capa de aplicación, en la misma transacción que el hecho auditado. Si la venta se anula, la fila de auditoría existe; si la anulación falla, no queda una auditoría de algo que no pasó.

---

# 25. Matriz de trazabilidad

Cada tabla hasta el requisito que la justifica. Es lo que permite defender el modelo: ninguna tabla existe «porque suele haberla».

| Tabla | Documento fuente | Historias | Reglas de negocio |
|---|---|---|---|
| `empresa`, `local` | `01` § Multilocal; `06` § Organización | US-001 | RN-001, RN-002, RN-007 |
| `horario_servicio`, `dia_especial`, `horario_dia_especial` | `02` § Autoservicio; `07` § Pendientes | US-007, US-019 | RN-030 |
| `rol`, `permiso`, `rol_permiso`, `usuario_rol` | `05` § Usuarios y permisos; `06` § Organización | US-004 | RN-013 |
| `usuario`, `usuario_local`, `usuario_permiso_excepcion` | `05` § Usuarios y permisos | US-002, US-003, US-005 | RN-011, RN-012, RN-013, RN-090, RN-091 |
| `sesion` | `05` § Respaldo y seguridad | US-002 | RN-092 |
| `bloqueo_pin` | `05` § Respaldo y seguridad | US-003 | RN-091 |
| `motivo` | `03` § Caja; `04` § Inventario; `07` § Pendientes | US-012 | RN-069 |
| `categoria_producto`, `producto`, `producto_servicio` | `04` § Catálogo, § Almuerzo | US-008, US-014 | RN-047 |
| `unidad_medida`, `conversion_unidad`, `presentacion_compra` | `04` § Inventario | US-009 | — |
| `proveedor` | `04` § Compras | US-011 | — |
| `receta`, `ingrediente_receta` | `04` § Recetas y costos | US-010 | RN-044, RN-045, RN-046 |
| `semana_menu`, `dia_menu`, `opcion_menu` | `04` § Programación semanal | US-013, US-014 | RN-039, RN-041, RN-042, RN-043 |
| `disponibilidad_producto` | `03` § Disponibilidad | US-015 | RN-055 |
| `plan_tarifario`, `precio_plan` | `02` § Planes tarifarios | US-017 | RN-033, RN-034, RN-041 |
| `pensionista`, `responsable` | `02` § Ficha | US-016, US-026 | RN-035, RN-036, RN-090 |
| `cuenta_pensionista`, `movimiento_cuenta`, `saldo_cuenta_cache` | `02` § Cuenta corriente | US-018 | RN-015, RN-017, RN-018, RN-019, RN-020 |
| `periodo_cuenta`, `estado_cuenta_mensual` | `02` § Cuenta corriente; `05` § Reportes | US-025 | RN-024, RN-025 |
| `pre_ticket` | `02` § Autoservicio presencial | US-019, US-020 | RN-030 |
| `abono` | `02` § Abonos | US-021, US-023, US-024 | RN-021, RN-022, RN-023, RN-029 |
| `mesa` | `03` § POS y mesas | US-027, US-029 | RN-058 |
| `pedido`, `cuenta_persona`, `detalle_pedido` | `03` § POS y mesas; `04` § Recetas | US-028, US-031, US-033 | RN-005, RN-009, RN-046, RN-060, RN-061, RN-064, RN-073, RN-074 |
| `detalle_pedido_componente` | `04` § Almuerzo | US-028 | RN-042 |
| `comanda`, `detalle_comanda` | `03` § Comandas | US-030 | RN-062, RN-063, RN-065 |
| `caja`, `turno_caja`, `cierre_caja` | `03` § Caja | US-032, US-036 | RN-070, RN-071, RN-072 |
| `medio_pago`, `pago`, `movimiento_caja` | `03` § Caja | US-033, US-034, US-035 | RN-067, RN-070 |
| `comprobante_electronico` y tablas de facturación | `03` § Facturación electrónica; DT-003 a DT-010 | US-037 | RN-075, RN-088 |
| `emisor_local` | DT-011, DT-017; `07` § Pendientes (P-04) | US-037 | RN-088 |
| `comprobante_resumen` | `02` § Portal; `03` § Facturación electrónica | US-022, US-037 | RN-028 |
| `existencia`, `movimiento_inventario` | `04` § Inventario y Kardex | US-043, US-044, US-045 | RN-056, RN-057 |
| `transferencia` | `01` § Multilocal; `04` § Inventario | US-046 | RN-006 |
| `conteo_fisico`, `detalle_conteo` | `04` § Inventario | US-048 | Caso crítico 12 |
| `compra`, `detalle_compra` | `04` § Compras | US-050 | RN-047 |
| `produccion` | `04` § Producción | US-049 | Caso crítico 11 |
| `consumo_interno` | `04` § Consumo interno | US-051 | — |
| `pedido_delivery`, `historial_delivery` | `03` § Delivery propio | US-040, US-041, US-042 | RN-080, RN-081, RN-082, RN-084 |
| `pedido_recojo` | `03` § Recojo programado | US-038, US-039 | RN-076, RN-077, RN-078 |
| `auditoria` | `01` § Auditoría transversal | US-006 | RN-003, RN-004 |
| `outbox`, `conflicto_sincronizacion` | `05` § Offline-first; DT-016, DT-017 | US-058 a US-061 | RN-086, RN-087, RN-088 |
| `event_publication` | DT-034 | HT-04 | — |

**Las tres tablas sin regla de negocio propia** —`unidad_medida` y su familia, `proveedor`, `consumo_interno`— no son una omisión: la especificación las describe funcionalmente sin enunciar una regla que restrinja su comportamiento.

---

# 26. Hallazgos de la revisión y correcciones aplicadas

Revisión completa del modelo lógico realizada el 16 de septiembre de 2026, con criterio de diseño relacional y de frontera entre microservicios. Se encontraron **nueve defectos**, todos corregidos en esta versión. Los defectos 7 y 8 salieron de revisar qué necesita cada servicio del otro, caso por caso (19.4); el noveno apareció al generar el código DBML, que obliga a escribir cada tabla explícitamente.

| # | Hallazgo | Gravedad | Corrección |
|---|---|---|---|
| 1 | `comprobante_electronico` declaraba `pedido_id`, `cuenta_persona_id` y `local_id` como **claves foráneas**, pero esa tabla vive en la base de datos del otro microservicio. Una FK entre bases distintas no existe | **Alta** | Reclasificadas como referencias lógicas sin FK. Se agregó `local_codigo` denormalizado para que la bandeja funcione con el otro servicio caído, y la sección 19 explica cómo se compensa la falta de integridad |
| 2 | La vinculación del pre-ticket estaba guardada **dos veces**: `pre_ticket.pedido_id` y `cuenta_persona.pre_ticket_id`. Dos verdades sobre el mismo hecho, que pueden desalinearse | **Alta** | Se eliminó `pre_ticket.pedido_id`. La relación queda solo en `cuenta_persona`, con índice único parcial que impide la doble vinculación bajo concurrencia |
| 3 | **No había dónde guardar los datos del receptor del comprobante** en `servicio-operacion`. La venta cierra en el local, posiblemente sin Internet, y es ahí donde el cajero pide el DNI o el RUC; el comprobante se encola después | **Alta** | Se agregaron `comprobante_tipo` y `receptor_*` a `pedido`. Sin esto, un cierre sin conexión perdía el documento que el cliente pidió |
| 4 | `pedido` no tenía columna `servicio`, pero `05` § Reportes exige ventas por local, canal, **servicio**, producto y usuario | Media | Se agregó `pedido.servicio`. Deducirlo de la hora habría fallado en cuanto cambiara una franja horaria |
| 5 | `usuario` tenía **un solo** `rol_id`, mientras `06` § Organización dice «Usuario puede recibir roles y excepciones», en plural. El modelo contradecía la fuente | Media | Se reemplazó por la tabla `usuario_rol` (N—N). El permiso efectivo pasa a ser la unión de los permisos de todos sus roles, más las excepciones |
| 6 | `existencia.costo_promedio` introducía un **segundo criterio de valorización** junto al costo de última compra de RN-047. Dos números que responden lo mismo y discrepan | Baja | Se eliminó la columna. La valorización es una sola: costo de última compra |

| 7 | **`servicio-operacion` no tenía dónde guardar el número ni el estado del comprobante.** El portal del pensionista (RN-028) y la reimpresión desde caja lo necesitan, y habrían tenido que consultar al otro servicio — rompiendo la operación durante un corte | **Alta** | Se agregó `comprobante_resumen`, alimentada por la cola `facturacion.resultados` |
| 8 | **`servicio-facturacion` no tenía los datos fiscales del emisor.** Sin RUC, razón social y domicilio por local no se puede armar el payload, y consultarlos al otro servicio haría que una caída de operación detuviera la facturación | **Alta** | Se agregó `emisor_local`, réplica de datos maestros mantenida por evento |

| 9 | **El recuento de tablas del documento estaba equivocado.** Decía 47 tablas en `servicio-operacion` y 54 en total; contándolas una por una al generar el código DBML resultan **66 y 74**. El error venía de la primera versión y se arrastró por todas las secciones | Baja | Corregido en las seis secciones donde aparecía. No afecta el diseño —ninguna tabla faltaba ni sobraba—, pero un recuento erróneo en un documento de referencia es exactamente el tipo de dato que alguien cita después sin verificar |

## 26.1 Lo que se revisó y quedó conforme

Para que la revisión sea verificable, también lo que **no** cambió:

- **Consistencia nomenclatural.** Las 77 tablas siguen la convención de 1.3 sin excepciones: `snake_case`, singular, sin tildes ni ñ, FK con el nombre de la entidad más `_id`, tablas de relación con los dos nombres unidos.
- **Cobertura de las reglas de integridad de `06`.** Las cuatro reglas esenciales tienen implementación identificada (sección 14), y tres de las cuatro viven en la base de datos, no solo en el código.
- **Alcance.** No se agregó ninguna entidad fuera de la especificación. Las ocho tablas de `servicio-facturacion` derivan de decisiones ya tomadas (DT-006, DT-009 y la sección 3.2 del documento de decisiones), y la sección 15 lista explícitamente lo que se deja fuera a propósito.
- **Las dos relaciones polimórficas** —`movimiento_inventario.referencia_id` y `auditoria.entidad_id`— están documentadas como tales, con su comprobación programada como compensación. No son un descuido.

## 26.2 Lo que sigue siendo riesgo, y no es corregible desde el modelo

- **P-14 quedó resuelto en el sprint 1.** Se verificó generando cinco identificadores con un segundo de separación: el prefijo crece de forma monótona y el orden alfabético coincide con el cronológico. Los tres índices de recálculo incremental de la sección 23 son válidos.
- **P-15, el redondeo.** La sección 2.6 propone una política; falta la validación del contador en la parte del IGV.
- **RF-010, el dimensionamiento.** La cantidad de locales, mesas, usuarios concurrentes y volumen pico sigue sin definirse. Los índices de la sección 23 se dimensionaron contra un supuesto de tres locales y 200 pedidos diarios por local. Si el volumen real fuera un orden de magnitud mayor, hay que revisar el particionado de los dos libros.
