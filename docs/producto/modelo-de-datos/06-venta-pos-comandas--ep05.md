<!-- Extraído de rikos-backend/doc/RIKOS-modelo-de-datos.md (fuente completa, con la numeración de sección original intacta -- ver esa numeración si algún comentario en el código cita p. ej. "13.1"). Partido por bloque el 2026-09-29 per docs/METODO-spec-driven.md §5. -->

# 8. Bloque 6 — Venta, POS y comandas

> **Cambios decididos en el sprint 6 (entrega 2, 2026-09-30)** — ver `plan/sprint-06-vender-en-mesa.md` (3.11 y 4.1):
> - **`pedido`, `cuenta_persona`:** `subtotal`, `total` y `cargo_taper` **no son columnas**: se derivan al leer las líneas (igual que el saldo de la cuenta del pensionista). `pedido` gana `fecha_operativa` (para el número por local y día) y solo usa los estados `abierto`, `comandado` y `pendiente_cierre` por ahora. Una mesa tiene **un solo pedido en curso** (índice único parcial) y un pensionista aparece **una vez por pedido**.
> - **`detalle_pedido`:** gana `tipo_linea` (`menu_normal`, `menu_extra`, `plan_desayuno`, `plan_cena`, `producto`, `extra_libre`), `descripcion` y `taper_unitario`, `segundo_del_dia`; `producto_id` es **nulo** en la comida del plan y en el extra libre; `tarifa_origen` gana `manual` (el único caso en que el mozo pone un precio, con bitácora `precio_manual`). El precio se guarda **al agregar la línea**; el costo, la receta y su composición se congelan **al cerrar** (sprint 7). `tipo_menu`, `costo_unitario`, `receta_id`, `composicion_json`, `utilidad`, `margen` y los campos de anulación llegan con sus entregas.
> - **`detalle_pedido_componente`:** una fila por componente del menú (única por línea), aunque el componente tenga una sola opción (RN-038).

Corresponde a EP-05 y a las historias US-027 a US-031. Todo este bloque lleva `local_id`.

## 8.1 `mesa`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `local_id` | identificador | No | FK → `local` |
| `numero` | texto(10) | No | Único dentro del local |
| `nombre` | texto(60) | Sí | «Terraza 3» |
| `capacidad` | entero | Sí | |
| `bloqueada` | booleano | No | Por defecto falso. Solo una mesa libre se bloquea |
| `motivo_bloqueo` | texto(250) | Sí | Obligatorio al bloquear |
| `bloqueada_por` | identificador | Sí | FK → `usuario` |
| `bloqueada_en` | momento | Sí | |
| `mesa_principal_id` | identificador | Sí | FK → `mesa`. Presente si está unida a otra |
| `activo` | booleano | No | |

**Reglas**

- **RN-059.** Los cinco estados (libre, ocupada, unida, pendiente de cierre, bloqueada) son los que fija la especificación; no se agregan ni se quitan. (Antes citada como RN-058, que en la lista consolidada es la PWA responsive.)
- **Unión de mesas.** Se modela con `mesa_principal_id` apuntando a la mesa que encabeza la unión, en lugar de una tabla de uniones. Es más simple y cubre el caso descrito: varias mesas se juntan bajo una, y al separarlas se limpia la referencia.
- **El estado NO es una columna (decidido en el sprint 6, 2026-09-29).** Se deriva al consultar, con esta prioridad: bloqueada, unida (tiene `mesa_principal_id`), pendiente de cierre (su pedido lo está), ocupada (pedido abierto o comandado) y libre. Guardarlo duplicaba lo que ya dicen los pedidos y podía desincronizarse, sobre todo con el trabajo sin internet del sprint 10; a escala de un local calcularlo no es caro. Solo una mesa **libre** se bloquea, con motivo (RF-028, resuelto en `plan/sprint-06-vender-en-mesa.md` 3.2).

## 8.2 `pedido`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `local_id` | identificador | No | FK → `local` |
| `numero` | texto(20) | No | Correlativo interno por local y día |
| `canal` | enum('mesa','para_llevar','delivery','recojo') | No | Los cuatro canales de `01` |
| `servicio` | enum('desayuno','almuerzo','cena') | No | Determinado por el horario del local |
| `mesa_id` | identificador | Sí | FK → `mesa`. Solo si `canal = mesa` |
| `turno_caja_id` | identificador | Sí | FK → `turno_caja`. Se asigna al cerrar |
| `mozo_id` | identificador | Sí | FK → `usuario` |
| `estado` | enum('abierto','comandado','pendiente_cierre','cerrado','anulado') | No | |
| `subtotal` | dinero | No | |
| `descuento` | dinero | No | |
| `cargo_taper` | dinero | No | Por menú, en para llevar, recojo y delivery |
| `total` | dinero | No | |
| `abierto_en` | momento | No | |
| `cerrado_en` | momento | Sí | |
| `cerrado_por` | identificador | Sí | FK → `usuario` |
| `reabierto_en` | momento | Sí | |
| `reabierto_por` | identificador | Sí | FK → `usuario` |
| `motivo_descuento_id` | identificador | Sí | FK → `motivo` |
| `comprobante_tipo` | enum('01','03','07','nota_venta','ticket') | Sí | Documento elegido al cerrar |
| `receptor_tipo_doc` | enum('dni','ruc','sin_documento') | Sí | |
| `receptor_numero_doc` | texto(11) | Sí | |
| `receptor_nombre` | texto(200) | Sí | |
| `receptor_direccion` | texto(250) | Sí | Obligatoria en factura |
| `receptor_correo` | texto(160) | Sí | Opcional y a pedido (DT-006) |

**Reglas**

- **`05` § Reportes mínimos** exige ventas por local, canal, **servicio**, producto y usuario. Sin la columna `servicio`, ese reporte solo podría deducirse de la hora del pedido, lo que falla en cuanto una franja horaria cambie.
- **Los datos del receptor se capturan aquí, no en el servicio de facturación.** La venta cierra en el local, posiblemente sin Internet, y es en ese momento cuando el cajero pide el DNI o el RUC (DT-005). El comprobante se encola con esos datos dentro; el servicio de facturación los recibe, no los pregunta. Sin estas columnas, un cierre sin conexión no tendría dónde guardar el documento que el cliente pidió.
- **RN-009, `01` § Canales.** Para llevar, recojo y delivery añaden un cargo de táper **por cada menú**, no por pedido. El precio del táper está pendiente (`07`).
- **RN-073, caso crítico 17.** Una venta **no puede reabrirse después de cerrar su turno**. Es una restricción de aplicación que compara el turno del pedido con el turno abierto; conviene además una comprobación en base de datos.
- **RN-074.** Después del cierre del turno, corregir se hace con operación compensatoria o documento relacionado, **sin alterar el cierre original**.
- Un pedido puede asociarse a un cliente regular, a uno o **varios** pensionistas, o a una mezcla de ambos: por eso el pensionista no es columna del pedido, sino de `cuenta_persona`.

## 8.3 `cuenta_persona`

La pieza que hace posible la mesa mixta. Divide un pedido en las personas que lo comparten.

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `pedido_id` | identificador | No | FK → `pedido` |
| `etiqueta` | texto(40) | No | «Persona 1», o el nombre del pensionista |
| `tipo_cliente` | enum('regular','pensionista') | No | |
| `pensionista_id` | identificador | Sí | FK → `pensionista`. Obligatorio si es pensionista |
| `pre_ticket_id` | identificador | Sí | FK → `pre_ticket` |
| `subtotal` | dinero | No | |
| `total` | dinero | No | |
| `estado` | enum('abierta','cerrada') | No | |
| `cerrada_en` | momento | Sí | |

**Reglas**

- **RN-060, caso crítico 7.** Una mesa mixta separa correctamente particulares y pensionistas **desde la toma del pedido**, no al cobrar. Por eso la división por persona es parte del pedido y no del pago.
- **RN-061.** Se permiten cierres parciales sin liberar la mesa mientras haya consumos pendientes: una `cuenta_persona` puede estar cerrada mientras el pedido sigue abierto.
- Único por `pre_ticket_id` cuando no es nulo: **impide la doble vinculación** del pre-ticket.
- **Supuesto de modelado:** un pre-ticket se vincula a una sola cuenta de persona. La especificación dice «impedir doble vinculación» sin más detalle; esta es la lectura estricta.

## 8.4 `detalle_pedido`

Aquí vive el snapshot económico. Es la tabla de la que salen todos los reportes de rentabilidad.

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `cuenta_persona_id` | identificador | No | FK → `cuenta_persona` |
| `producto_id` | identificador | No | FK → `producto` |
| `cantidad` | entero | No | |
| `tipo_menu` | enum('normal','extra','producto') | Sí | |
| `observacion` | texto(250) | Sí | «Sin cebolla» |
| `estado` | enum('pendiente','comandado','anulado','servido') | No | |
| `precio_unitario` | dinero | No | **Snapshot**: precio aplicado en ese momento |
| `tarifa_origen` | enum('publico','plan','extra') | No | De dónde salió el precio |
| `plan_tarifario_id` | identificador | Sí | FK → `plan_tarifario`. Plan aplicado |
| `costo_unitario` | costo | No | **Snapshot**: costo en ese momento |
| `receta_id` | identificador | Sí | FK → `receta`. **Snapshot**: receta aplicada |
| `composicion_json` | json | Sí | **Snapshot**: árbol de receta y componentes elegidos |
| `utilidad` | costo | No | Derivado, guardado |
| `margen` | numeric(7,4) | No | Derivado, guardado |
| `anulado_en` | momento | Sí | |
| `anulado_por` | identificador | Sí | FK → `usuario` |
| `motivo_anulacion_id` | identificador | Sí | FK → `motivo` |
| `anulado_post_comanda` | booleano | No | Decide si descuenta inventario |

**Reglas**

- **RN-005, RN-046, 2.4.** Los seis campos de snapshot se escriben al cerrar la venta y **no vuelven a tocarse nunca**. Un cambio de precio o de receta posterior no los afecta.
- **RN-064, caso crítico 9.** Un producto anulado **después** de comandar no se cobra, pero **descuenta la receta igual**, como pérdida. Un producto anulado **antes** de comandar no descuenta inventario. `anulado_post_comanda` es la columna que decide, y por eso no basta con `anulado_en`.
- **RN-069.** Anular requiere permiso específico y motivo.
- `composicion_json` es la única columna `json` del modelo, y es deliberado: guarda una estructura de árbol que nunca se consulta ni se filtra, solo se muestra al reimprimir o auditar una venta. Normalizarla obligaría a una tabla de snapshot por nivel de receta, que nadie consultaría.

## 8.5 `detalle_pedido_componente`

Qué opción eligió el comensal para cada componente del menú.

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `detalle_pedido_id` | identificador | No | FK → `detalle_pedido` |
| `componente` | enum('entrada','segundo','guarnicion','refresco','postre') | No | |
| `producto_id` | identificador | No | FK → `producto` |
| `opcion_menu_id` | identificador | Sí | FK → `opcion_menu` |

**Regla, RN-042.** Si el día ofrece varias opciones de un componente, el mozo registra la elegida y queda una fila aquí. Si hay una sola, se asigna directamente, pero **la fila se crea igual**: el registro histórico de qué comió cada quien no depende de que hubiera alternativa.

## 8.6 `comanda`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `pedido_id` | identificador | No | FK → `pedido` |
| `local_id` | identificador | No | FK → `local` |
| `numero` | entero | No | Correlativo por pedido |
| `emitida_en` | momento | No | |
| `emitida_por` | identificador | No | FK → `usuario` |
| `impresiones` | entero | No | Por defecto 1 |
| `ultima_impresion_en` | momento | Sí | |
| `estado_impresion` | enum('pendiente','impresa','fallida') | No | |

**Reglas**

- **RN-062.** Una **sola** impresora central de cocina por local. `estado_impresion` existe porque la impresora puede estar sin papel o apagada, y eso no debe detener la venta.
- **RN-063.** Toda reimpresión se marca como **copia**. `impresiones > 1` implica que todo lo que salga después lleva la leyenda. Es lo que evita que cocina prepare dos veces el mismo plato.
- **RN-065.** Cocina avisa verbalmente o con campana; **no** cambia un estado digital. Por eso no hay estado «preparado» ni «listo» en la comanda: modelarlo sería inventar un requisito.

## 8.7 `detalle_comanda`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `comanda_id` | identificador | No | FK → `comanda` |
| `detalle_pedido_id` | identificador | No | FK → `detalle_pedido` |
| `cantidad` | entero | No | |

**Regla.** La comanda es una **foto de lo que se mandó a cocina**, no un espejo del pedido. Si después se agregan platos, se emite otra comanda. Por eso el detalle se guarda aparte y no se consulta contra `detalle_pedido` en el momento de imprimir.

---



---

## Cambios del sprint 6 tras la especificación original (2026-09-30)

El código manda; estas columnas y reglas se agregaron después del diseño de arriba:

- **`pedido.nombre_cliente`** (V35, texto de 120, opcional): a nombre de quien es el pedido, solo para reconocer la mesa. No es una ficha de cliente (RF-017 sigue sin definir).
- **`detalle_comanda.etiqueta_persona`** (V36, texto de 120, obligatoria): la etiqueta de la persona tal como era al comandar; la comanda es una foto y no cambia si la línea pasa a otra persona (US-029).
- **`detalle_pedido`** gana `anulado_en`, `anulado_por`, `motivo_anulacion_id`, `observacion_anulacion` y `anulado_post_comanda` (V33, con un CHECK que exige motivo, quién y cuándo a toda línea anulada) y **`version`** (V37, control de concurrencia: dos escrituras sobre la misma línea no se pisan).
- **`comanda.impresiones`** nace en **0** (no en 1 como decía 8.6): comandar no es imprimir; la primera impresión la deja en 1 y desde la segunda sale como COPIA.
- **`pedido.estado`** solo admite `abierto`, `comandado` y `pendiente_cierre` hasta el sprint 7, que agrega `cerrado` y `anulado`.
- **`mesa`** gana `salon_id`, forma, medidas y posición en el plano (V34, rediseño de salones, otra sesión); el estado de la mesa sigue derivándose.
