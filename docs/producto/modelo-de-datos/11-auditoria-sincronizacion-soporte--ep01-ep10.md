<!-- Extraído de rikos-backend/doc/RIKOS-modelo-de-datos.md (fuente completa, con la numeración de sección original intacta -- ver esa numeración si algún comentario en el código cita p. ej. "13.1"). Partido por bloque el 2026-09-29 per docs/METODO-spec-driven.md §5. -->

# 13. Bloque 11 — Auditoría, sincronización y soporte

Corresponde a EP-01 (US-006) y EP-10 (US-058 a US-061), más las habilitantes técnicas.

## 13.1 `auditoria`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `local_id` | identificador | Sí | FK → `local` |
| `usuario_id` | identificador | No | FK → `usuario` |
| `evento` | enum('descuento','anulacion','reapertura','ajuste_inventario','transferencia','cambio_receta','cambio_precio','cambio_permiso','aprobacion_abono','rechazo_abono','cierre_caja','marcado_agotado','cambio_plan') | No | |
| `entidad` | texto(60) | No | Tabla afectada |
| `entidad_id` | identificador | No | Fila afectada |
| `valor_anterior` | json | Sí | |
| `valor_nuevo` | json | Sí | |
| `motivo_id` | identificador | Sí | FK → `motivo` |
| `observacion` | texto(500) | Sí | |
| `ocurrido_en` | momento | No | |

**Reglas**

- **`01` § Auditoría transversal.** Registra usuario, fecha, hora, local, valores anteriores y nuevos, y motivo cuando aplique, en los seis grupos de eventos que la especificación enumera. El enumerado los cubre todos.
- **Esta tabla no se modifica ni se borra jamás.** Es el único registro de por qué el sistema está como está.
- `valor_anterior` y `valor_nuevo` son `json` porque cada evento afecta a una entidad distinta y no hay una forma común. Nunca se consultan por su contenido, solo se muestran.
- **Caso crítico 19.** Un usuario sin permiso no puede ver costos, anular, descontar ni ajustar inventario. El intento denegado **también** se registra: sin eso, la auditoría solo ve lo que sí ocurrió.

## 13.2 `outbox`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `tipo_evento` | texto(80) | No | |
| `agregado_tipo` | texto(60) | No | |
| `agregado_id` | identificador | No | |
| `carga` | json | No | El evento serializado |
| `local_id` | identificador | Sí | FK → `local` |
| `estado` | enum('pendiente','enviado','fallido') | No | |
| `intentos` | entero | No | |
| `ultimo_error` | texto(500) | Sí | |
| `creado_en` | momento | No | |
| `enviado_en` | momento | Sí | |

**Reglas**

- **DT-016, DT-017.** Se escribe **en la misma transacción** que el hecho de negocio. Si la venta se guarda, el evento se guarda; si la venta falla, no queda un evento huérfano. Es lo que hace que la sincronización no pierda ni duplique.
- En el nodo del local esta tabla **es** la cola: no hay RabbitMQ allí. El sincronizador la vacía cuando vuelve Internet.
- **RN-087.** Prevenir duplicados mediante identificadores únicos e idempotencia: el consumidor descarta un evento cuyo `id` ya procesó.

## 13.3 `conflicto_sincronizacion`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `entidad` | texto(60) | No | |
| `entidad_id` | identificador | No | |
| `local_id` | identificador | No | FK → `local` |
| `descripcion` | texto(500) | No | |
| `resolucion` | enum('local_gana','central_gana','ambos_conservados','manual') | No | |
| `resuelto_en` | momento | No | |
| `resuelto_por` | identificador | Sí | FK → `usuario` |

**Regla, RN-088, `05` § Offline-first.** Resolver conflictos **sin perder transacciones** y **dejar registro de la resolución**. Esta tabla es ese registro. Las reglas exactas de resolución están pendientes (`07`).

## 13.4 `event_publication`

Tabla que exige Spring Modulith para su registro de eventos publicados. Su estructura la define el framework; se versiona con Flyway según DT-034 y **no se diseña a mano**.

---

