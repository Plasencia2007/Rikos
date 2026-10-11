<!-- Extraído de rikos-backend/doc/RIKOS-modelo-de-datos.md (fuente completa, con la numeración de sección original intacta -- ver esa numeración si algún comentario en el código cita p. ej. "13.1"). Partido por bloque el 2026-09-29 per docs/METODO-spec-driven.md §5. -->

# 12. Bloque 10 — Delivery y recojo programado

Corresponde a EP-07, historias US-038 a US-042.

## 12.1 `pedido_delivery`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `pedido_id` | identificador | No | FK → `pedido`. Único |
| `local_id` | identificador | No | FK → `local` |
| `cliente_nombre` | texto(120) | No | |
| `cliente_telefono` | texto(20) | No | |
| `direccion` | texto(250) | No | |
| `referencia` | texto(250) | Sí | |
| `direccion_aceptada` | booleano | No | Caja decide (RN-081) |
| `motorizado_id` | identificador | Sí | FK → `usuario` |
| `estado` | enum('pendiente','asignado','aceptado','recogido','en_ruta','entregado','fallido') | No | |
| `monto_cobrado` | dinero | Sí | Lo que el motorizado recibió |
| `liquidado_en` | momento | Sí | |
| `liquidado_por` | identificador | Sí | FK → `usuario` |
| `ultima_latitud` / `ultima_longitud` | numeric(10,7) | Sí | Última ubicación en ruta |
| `ubicacion_actualizada_en` | momento | Sí | |

**Reglas**

- **RN-080, RN-081.** Delivery propio, **gratuito y sin zonas**. No hay columna de costo de envío ni de zona: agregarlas sería modelar algo que la especificación excluye.
- **RN-082.** Los siete estados son los que fija la especificación. **La entrega se confirma solo marcando `entregado`**.
- **Caso crítico 15.** El motorizado marca entregado, pero **la venta no se cierra hasta la liquidación en caja**. Por eso `liquidado_en` está aquí y el cierre del pedido depende de ella.
- **RN-084.** Sueldo fijo, **no se calculan comisiones**. No hay columna de comisión.
- La ubicación se guarda como **última posición**, no como recorrido: `03` pide compartir la última ubicación mientras está en ruta, no un historial de rastreo. Modelar el recorrido completo sería recolectar datos de ubicación que nadie pidió, con el problema de protección de datos que eso implica.

## 12.2 `historial_delivery`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `pedido_delivery_id` | identificador | No | FK → `pedido_delivery` |
| `estado_anterior` | texto(20) | Sí | |
| `estado_nuevo` | texto(20) | No | |
| `latitud` / `longitud` | numeric(10,7) | Sí | |
| `incidencia` | texto(250) | Sí | |
| `ocurrido_en` | momento | No | |
| `registrado_por` | identificador | No | FK → `usuario` |

**Regla.** Registra cambios de estado, ubicación e incidencias. Es lo que alimenta el reporte de entregas e incidencias por motorizado.

## 12.3 `pedido_recojo`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `pedido_id` | identificador | No | FK → `pedido`. Único |
| `local_id` | identificador | No | FK → `local` |
| `fecha_hora_recojo` | momento | No | Fecha y hora **futuras** |
| `persona_recoge` | texto(120) | No | |
| `celular` | texto(20) | No | |
| `pago_anticipado` | booleano | No | Obligatorio para cliente regular |
| `estado_preparacion` | enum('pendiente','en_preparacion','listo') | No | |
| `estado_retiro` | enum('pendiente','retirado','no_recogido','cancelado') | No | |

**Reglas**

- **RN-076, RN-077.** Cliente regular: **pago anticipado antes de confirmar**. Pensionista: cargo al cerrar el pedido.
- **RN-078.** Cancelación **sin cobro** solo antes de enviar la comanda.
- **Casos críticos 13 y 14.** Si ya se preparó y no se recoge: se descuenta inventario y se registra `no_recogido`. Al cliente regular **no se le devuelve el pago**; al pensionista se le **carga el pedido completo** y se emite su comprobante. Dos estados separados —preparación y retiro— son necesarios porque esa combinación (preparado + no recogido) es justamente la que dispara la regla.

---

