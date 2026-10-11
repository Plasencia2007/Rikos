<!-- Extraído de rikos-backend/doc/RIKOS-modelo-de-datos.md (fuente completa, con la numeración de sección original intacta -- ver esa numeración si algún comentario en el código cita p. ej. "13.1"). Partido por bloque el 2026-09-29 per docs/METODO-spec-driven.md §5. -->

# 19. Frontera entre microservicios

El backend son dos servicios desplegables por separado (DT-011), cada uno con **su propia base de datos**. Ninguno lee las tablas del otro.

| | `servicio-operacion` | `servicio-facturacion` |
|---|---|---|
| Base de datos | `rikos_operacion` | `rikos_facturacion` |
| Tablas | 69 | 8 |
| Corre en el nodo del local | **Sí** | **No** |
| Corre en la nube | Sí | Sí |
| Qué resuelve | Toda la operación del restaurante | Emisión, seguimiento y entrega de comprobantes |

## 19.1 La única relación que la base de datos no puede garantizar

`comprobante_electronico.pedido_id` apunta a un pedido que vive en **otra base de datos**. No hay clave foránea, y no puede haberla: PostgreSQL no valida integridad referencial entre bases distintas.

**Cómo se compensa, en tres medidas concretas:**

1. **El identificador viaja completo.** Como los identificadores se generan en el origen (2.1), el ULID del pedido es definitivo desde antes del envío. No existe un identificador provisional que después haya que traducir.
2. **Se denormaliza lo mínimo necesario.** `comprobante_electronico` guarda `local_codigo`, `receptor_*` y `total` como copias, para que la bandeja de comprobantes se pueda mostrar **sin consultar al otro servicio**. Si `servicio-operacion` está caído, la bandeja sigue funcionando.
3. **La consistencia es eventual y auditada.** Una comprobación programada contrasta las ventas cerradas con comprobante encolado contra los comprobantes existentes, y reporta las que no aparecen. Es la misma idea de la reconciliación de DT-009, aplicada hacia adentro en vez de hacia el proveedor.

## 19.2 Por qué el pedido no viaja entero

Tentación frecuente: copiar el pedido completo con su detalle al servicio de facturación. **No conviene.** El comprobante necesita importes y datos del receptor, no recetas, costos ni asignación por persona. Copiar de más significa mantener sincronizadas dos versiones de la misma información y filtrar costos y márgenes a un servicio que no tiene por qué verlos.

Lo que viaja en el evento `facturacion.solicitudes` es exactamente lo que el comprobante necesita: identificador del pedido, local, tipo de documento, datos del receptor, líneas con descripción, cantidad y precio, y total.

## 19.3 Las tres formas de comunicación, y por qué ninguna es una consulta

Pregunta que aparece siempre al ver dos servicios con bases separadas: *¿y cuando facturación necesite el nombre del cliente, o el usuario, no le pregunta a operación?* **No.** Y el motivo no es purismo arquitectónico, es RN-086.

Si cerrar una venta dependiera de una llamada a `servicio-facturacion` —o al revés—, el local no podría vender durante un corte de Internet. DT-004 lo resuelve de raíz: la emisión es **siempre** asíncrona, y el comprobante se encola con todo lo que necesita adentro.

Existen exactamente tres canales:

| # | Canal | Sentido | Qué transporta | Síncrono |
|---|---|---|---|---|
| 1 | Cola `facturacion.solicitudes` | operación → facturación | El encargo completo: pedido, local, tipo de documento, datos del receptor, líneas e importes | No |
| 2 | Cola `facturacion.resultados` | facturación → operación | Serie, número, estado fiscal y rutas de los archivos | No |
| 3 | API de la bandeja, detrás del gateway | **frontend** → facturación | Consultar estado, reintentar, reenviar correo | Sí |

**El canal 3 no es comunicación entre servicios.** Es el navegador del administrador llamando a `servicio-facturacion` a través del gateway, igual que llama a `servicio-operacion`. Los dos servicios siguen sin conocerse. Y solo funciona desde la nube, que es donde está el administrador: en el nodo del local ese servicio ni siquiera corre (DT-015).

## 19.4 Qué necesita cada servicio del otro, caso por caso

Enumerado exhaustivamente, porque cada caso se resuelve distinto y es donde se cuela el acoplamiento.

| Qué necesita facturación | De dónde sale | Por qué no se consulta |
|---|---|---|
| **Datos del receptor** (DNI o RUC, nombre, dirección, correo) | Viajan en el evento, capturados por caja al cerrar (`pedido.receptor_*`) | El cajero los pide en el mostrador, sin Internet. Preguntarlos después sería preguntarle a quien ya cerró la caja |
| **Líneas del comprobante** (descripción, cantidad, precio) | Viajan en el evento | Es la foto de lo vendido. Consultarla después devolvería el pedido de *hoy*, no el de cuando se emitió |
| **Local y su serie** | Tabla replicada `emisor_local` | Es dato maestro, cambia una vez al año. Replicarlo cuesta menos que consultarlo mil veces al día |
| **Datos fiscales del emisor** (RUC, razón social, domicilio por local) | Tabla replicada `emisor_local` | Igual. Y sin ellos no se puede emitir, así que no pueden depender de que el otro servicio responda |
| **Quién reintentó desde la bandeja** | El identificador del usuario viene en el token que valida el gateway | La autenticación ya la resolvió el gateway; volver a preguntar quién es sería hacerlo dos veces |
| **Costos, márgenes, recetas, asignación por persona** | **Nada. No los necesita** | El comprobante no los lleva. Enviarlos filtraría información a un servicio que no tiene por qué verla |

| Qué necesita operación de facturación | De dónde sale | Por qué no se consulta |
|---|---|---|
| **Número y estado del comprobante** de una venta | Llega por la cola `facturacion.resultados` y se guarda en `comprobante_resumen` | El portal del pensionista (RN-028) y la reimpresión desde caja (`03` § Facturación) tienen que funcionar aunque facturación esté caído |
| **El PDF y el XML** para mostrarlos o reenviarlos | La ruta llega en el mismo evento; el archivo se sirve desde el almacenamiento propio (DT-010) | Los enlaces del proveedor no requieren token: la URL misma es la credencial (H-06). Exponerlos sería publicar el comprobante |

## 19.5 `emisor_local` — datos maestros replicados en facturación

Tabla de solo lectura desde el punto de vista del negocio: la mantiene la cola, nadie la edita a mano.

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `local_id` | identificador | No | PK. Mismo identificador que en operación |
| `local_codigo` | texto(20) | No | UQ |
| `local_nombre` | texto(120) | No | |
| `ruc_emisor` | texto(11) | No | De la empresa. P-04 |
| `razon_social_emisor` | texto(200) | No | P-04 |
| `domicilio_fiscal` | texto(250) | No | Por local. P-04 |
| `serie_boleta` / `serie_factura` | texto(4) | Sí | |
| `actualizado_en` | momento | No | Cuándo llegó la última réplica |

**Reglas**

- Se actualiza por el evento `local.actualizado` que publica `servicio-operacion` al dar de alta o modificar un local. Es el mismo mecanismo de outbox de todo lo demás.
- **Es una réplica, no una fuente de verdad.** Si discrepa con operación, manda operación. Una comprobación programada las contrasta.
- **Por qué replicar y no consultar:** son datos que cambian una vez al año y se necesitan en cada emisión. Y sobre todo: sin ellos no se puede emitir, así que hacerlos depender de que el otro servicio responda convertiría una caída de operación en una parada de la facturación.

## 19.6 `comprobante_resumen` — el reflejo en operación

La contraparte: lo mínimo que `servicio-operacion` necesita saber de un comprobante para no tener que preguntar.

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `comprobante_id` | identificador | No | PK. El mismo identificador que en facturación |
| `pedido_id` | identificador | No | FK → `pedido` |
| `cuenta_persona_id` | identificador | Sí | FK → `cuenta_persona` |
| `tipo` | enum('01', '03', '07', 'nota_venta', 'ticket') | No | |
| `serie` | texto(4) | Sí | |
| `numero` | entero | Sí | Nulo mientras el proveedor no responda |
| `estado_fiscal` | enum('pendiente', 'en_vuelo', 'enviado', 'registrado', 'aceptado', 'rechazado') | No | |
| `ruta_pdf` / `ruta_xml` | texto(500) | Sí | |
| `actualizado_en` | momento | No | |

**Reglas**

- Se escribe y actualiza **solo** desde el consumidor de la cola `facturacion.resultados`. Ninguna otra parte del código la toca.
- **Es lo que hace posible el portal del pensionista.** RN-028 le permite consultar sus comprobantes; si esa pantalla llamara a `servicio-facturacion`, dejaría de funcionar cada vez que ese servicio se reinicie.
- **Y la reimpresión desde caja.** `03` § Facturación electrónica pide consulta y reimpresión de la representación del comprobante: con esta tabla, caja sabe qué número tiene la boleta sin salir del nodo local.
- No lleva los datos del receptor: esos ya están en `pedido`.
---

# 20. Esquema de `servicio-operacion`

69 tablas en `rikos_operacion`, contando la `event_publication` que genera Spring Modulith. Es el monolito modular: todo lo que debe funcionar durante un corte de Internet vive aquí, porque además comparte transacción.

## 20.1 Inventario de tablas por módulo de Spring Modulith

Los módulos coinciden con los definidos en DT-013.

| Módulo | Tablas | Sprint |
|---|---|---|
| `identidad` | `empresa`, `local`, `horario_servicio`, `dia_especial`, `horario_dia_especial`, `rol`, `permiso`, `rol_permiso`, `usuario`, `usuario_rol`, `usuario_local`, `usuario_permiso_excepcion`, `sesion`, `bloqueo_pin` | 1 y 3 |
| `catalogo` | `categoria_producto`, `unidad_medida`, `producto`, `producto_servicio`, `presentacion_compra`, `conversion_unidad`, `proveedor`, `receta`, `ingrediente_receta` | 2 y 4 |
| `menu` | `semana_menu`, `dia_menu`, `opcion_menu`, `disponibilidad_producto` | 4 |
| `pensionistas` | `plan_tarifario`, `precio_plan`, `pensionista`, `responsable`, `cuenta_pensionista`, `movimiento_cuenta`, `saldo_cuenta_cache`, `periodo_cuenta`, `estado_cuenta_mensual`, `pre_ticket`, `abono` | 5, 8 y 13 |
| `venta` | `mesa`, `pedido`, `cuenta_persona`, `detalle_pedido`, `detalle_pedido_componente`, `comanda`, `detalle_comanda`, `comprobante_resumen` | 6 y 11 |
| `caja` | `caja`, `turno_caja`, `medio_pago`, `pago`, `movimiento_caja`, `cierre_caja` | 7 |
| `inventario` | `existencia`, `movimiento_inventario`, `transferencia`, `conteo_fisico`, `detalle_conteo`, `compra`, `detalle_compra`, `produccion`, `consumo_interno` | 9 |
| `delivery` | `pedido_delivery`, `historial_delivery`, `pedido_recojo` | 12 |
| `comun` | `motivo`, `auditoria`, `outbox`, `conflicto_sincronizacion`, `event_publication` | 0, 3 y 10 |

**Regla de Spring Modulith que esto impone:** un módulo **no** consulta las tablas de otro. `venta` no hace un `join` contra `pensionista`: pide el dato al módulo `pensionistas` por su interfaz pública. Las claves foráneas entre módulos existen en la base —son la misma base— pero el código no las recorre libremente. Es la disciplina que el build verifica automáticamente y la razón principal por la que se eligió Spring Boot (DT-012).

## 20.2 Diagrama de `servicio-operacion`

Las 69 tablas agrupadas por módulo, con las dependencias entre módulos.

**Una advertencia sobre por qué no es un diagrama plano de 47 cajas.** Se dibujó primero así, con las 66 tablas y el centenar de relaciones sueltas: el resultado era una maraña de cinco metros de ancho en la que no se distinguía nada. Un diagrama que no se puede leer no documenta, decora. La representación **tabla por tabla con todas sus conexiones** está en la sección 22, que es donde un administrador de base de datos va a mirar; este diagrama responde la otra pregunta, la de arquitectura: qué módulo depende de cuál.

![](dg/d6.png)

**Cómo se lee.** La flecha va de quien depende hacia aquello de lo que depende. Lo importante es que **no hay ciclos entre módulos**: `venta` depende de `catalogo`, `catalogo` no depende de `venta`. Es la condición que Spring Modulith verifica en el build, y la que permite que un módulo se pueda extraer como servicio independiente si algún día hiciera falta.

Las relaciones **dentro** de cada módulo están en los cuatro diagramas de la parte I (3.12, 6.5, 7.12, 10.2).

---

# 21. Esquema de `servicio-facturacion`

8 tablas en `rikos_facturacion`, contando `emisor_local` (19.5). La parte I describía solo `comprobante_electronico`, porque desde el punto de vista del negocio es la única entidad. Desde el punto de vista físico hacen falta seis tablas más, todas derivadas de decisiones ya tomadas: la emisión serializada con reconciliación de DT-009, el envío único de correo de DT-006, y la clasificación de errores de la sección 3.2 del documento de decisiones.

## 21.1 `serie_comprobante`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `serie` | texto(4) | No | UQ junto con `tipo` |
| `tipo` | enum('01','03','07') | No | Boleta, factura o nota de crédito |
| `local_codigo` | texto(20) | Sí | Referencia lógica. Nulo si la serie es común |
| `ultimo_numero_conocido` | entero | Sí | Lo último que devolvió el proveedor |
| `emision_en_curso` | booleano | No | **El cerrojo de DT-009** |
| `activo` | booleano | No | |

**Reglas**

- **DT-009.** `emision_en_curso` es lo que garantiza que **nunca haya dos comprobantes en vuelo de la misma serie**. Sin ese cerrojo, un timeout deja dos documentos indistinguibles en el listado del proveedor y la reconciliación no puede decidir cuál es cuál.
- `ultimo_numero_conocido` **no** se usa para numerar: el número lo asigna el proveedor (H-01). Sirve para detectar saltos de correlativo, que son la señal de que algo se emitió sin que nos enteráramos.
- **P-03** sigue abierto: falta confirmar si el proveedor permite varias series bajo la misma cuenta. Si no las permitiera, `local_codigo` queda nulo y todos los locales comparten serie, lo que **no** rompe nada porque el número lo asigna el proveedor.

## 21.2 `comprobante_electronico`

Definida en 10.1. Se le agregan aquí las columnas que corresponden al nivel físico.

| Atributo adicional | Tipo | Nulo | Descripción |
|---|---|---|---|
| `serie_id` | identificador | Sí | FK → `serie_comprobante` |
| `intentos` | entero | No | Contador, para la bandeja |
| `proximo_intento_en` | momento | Sí | Espera exponencial entre reintentos |
| `requiere_revision` | booleano | No | Verdadero lo saca de la cola y lo manda a la bandeja |
| `creado_en` / `actualizado_en` | momento | No | Traza |

## 21.3 `intento_emision`

Un registro por llamada al proveedor. **Solo se agregan filas.**

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `comprobante_id` | identificador | No | FK → `comprobante_electronico` |
| `numero_intento` | entero | No | UQ junto con `comprobante_id` |
| `enviado_en` | momento | No | |
| `respondido_en` | momento | Sí | Nulo si hubo timeout |
| `codigo_http` | entero | Sí | |
| `codigo_respuesta` | texto(10) | Sí | Código del proveedor o de SUNAT |
| `mensaje` | texto(1000) | Sí | |
| `categoria` | enum('exito','reintentable','no_reintentable','requiere_reconciliacion','alerta_token') | No | Ver sección 3.2 del documento de decisiones |
| `carga_enviada` | json | Sí | Payload, para poder reproducir el caso |

**Regla, sección 3.2 de decisiones.** `categoria` es lo que distingue un 422 —payload inválido, reintentar no arregla nada— de un timeout o un 500, que **pueden haber creado el documento** y por tanto exigen reconciliar antes de reintentar. La integración de referencia trataba todo 4xx y 5xx por igual; aquí no.

## 21.4 `reconciliacion`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `fecha_consultada` | fecha | No | El rango es un solo día (H-03) |
| `ejecutada_en` | momento | No | |
| `motivo` | enum('programada','timeout','codigo_2800','manual') | No | |
| `documentos_proveedor` | entero | No | Cuántos devolvió el listado |
| `documentos_locales` | entero | No | Cuántos teníamos registrados |
| `adoptados` | entero | No | Cuántos huérfanos se reconocieron como nuestros |
| `discrepancias` | json | Sí | Detalle de lo que no cuadró |

**Regla, DT-009.** Ante un timeout **no se reintenta a ciegas**: se consulta el listado del día, se compara con lo registrado, y como solo había un documento en vuelo para esa serie, el que aparezca de más es el nuestro. Esta tabla es el registro de esa operación, y es lo que permite demostrar después por qué un comprobante quedó con el número que tiene.

## 21.5 `envio_correo`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `comprobante_id` | identificador | No | FK → `comprobante_electronico` |
| `destinatario` | texto(160) | No | |
| `estado` | enum('pendiente','enviado','rebotado','descartado') | No | |
| `intentos` | entero | No | |
| `proveedor_mensaje_id` | texto(120) | Sí | Devuelto por el proveedor de correo |
| `error` | texto(500) | Sí | |
| `enviado_en` | momento | Sí | |

**Reglas**

- **DT-006.** El disparador es el estado **Aceptado**, nunca Registrado: recién ahí hay CDR y el documento es válido.
- El envío debe ocurrir **exactamente una vez**. Lo garantizan dos cosas juntas: `comprobante_electronico.correo_enviado_en` verificado en la misma transacción que crea esta fila, y una restricción de unicidad sobre `comprobante_id` entre los envíos que no son reintento manual.
- `rebotado` alimenta la bandeja: el reenvío manual desde caja crea una fila nueva, no modifica la anterior.

## 21.6 `outbox` y `event_publication`

El servicio de facturación tiene **su propio** `outbox`, con la misma estructura que el de operación (13.2), para publicar en la cola `facturacion.resultados` sin acoplar la transacción a RabbitMQ. Y su propia `event_publication` de Spring Modulith (DT-034).

## 21.7 Diagrama de `servicio-facturacion`

![](dg/d7.png)

## 21.8 Qué **no** está en este servicio

Registrado porque es donde se cuela el acoplamiento:

- **No hay tabla de clientes.** Los datos del receptor llegan dentro del evento y se guardan denormalizados en el comprobante. `emisor_local` es la única réplica, y es del **emisor**, no del cliente.
- **No hay costos ni márgenes.** El servicio de facturación no tiene por qué conocerlos.
- **No hay detalle de receta ni asignación por persona.** El comprobante lleva líneas con descripción, cantidad y precio, nada más.
- **No hay usuarios.** Quién cerró la venta es dato de `servicio-operacion`; aquí solo se registra quién reintentó desde la bandeja, y eso se guarda como identificador suelto.

---

