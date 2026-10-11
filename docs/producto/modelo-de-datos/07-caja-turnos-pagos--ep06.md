<!-- Extraído de rikos-backend/doc/RIKOS-modelo-de-datos.md (fuente completa, con la numeración de sección original intacta -- ver esa numeración si algún comentario en el código cita p. ej. "13.1"). Partido por bloque el 2026-09-29 per docs/METODO-spec-driven.md §5. -->

# 9. Bloque 7 — Caja, turnos y pagos

Corresponde a EP-06 y a las historias US-032 a US-036.

> **Cambios del sprint 7 (2026-10-05 y 2026-10-11):** `caja` y `turno_caja` se crearon en V40 (un turno abierto por caja **y por cajero**, con índices únicos parciales; "Caja 1" se crea sola la primera vez). V41 agrega `cobro`, `cobro_linea` (snapshot por línea), `pago` (con `monto_recibido` para el vuelto), `movimiento_caja` y `cobro_cargo`, y **no** crea `medio_pago`: el medio es un texto con `CHECK`. `pago` cuelga de `cobro`, no de `cuenta_persona`, porque el cobro es por líneas. Ver `plan/sprint-07-cobrar-y-cerrar.md` §4.2 y §4.2 bis.

## 9.1 `caja`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `local_id` | identificador | No | FK → `local` |
| `nombre` | texto(60) | No | |
| `activo` | booleano | No | |

## 9.2 `turno_caja`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `caja_id` | identificador | No | FK → `caja` |
| `local_id` | identificador | No | FK → `local` |
| `cajero_id` | identificador | No | FK → `usuario` |
| `fondo_inicial` | dinero | No | |
| `abierto_en` | momento | No | |
| `cerrado_en` | momento | Sí | |
| `estado` | enum('abierto','cerrado') | No | |

**Regla.** Una sola caja con turno abierto a la vez por caja. Es la restricción que sostiene RN-073: sin ella no se puede decidir si una venta pertenece al turno vigente.

## 9.3 `movimiento_caja`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `turno_caja_id` | identificador | No | FK → `turno_caja` |
| `local_id` | identificador | No | FK → `local` |
| `tipo` | enum('venta','abono_pensionista') | No | **Solo estos dos** |
| `medio_pago_id` | identificador | No | FK → `medio_pago` |
| `importe` | dinero | No | |
| `pago_id` | identificador | Sí | FK → `pago` |
| `abono_id` | identificador | Sí | FK → `abono` |
| `ocurrido_en` | momento | No | |
| `registrado_por` | identificador | No | FK → `usuario` |

**Regla, RN-070.** **No** se permiten gastos, retiros ni ingresos extraordinarios. La única excepción es el abono en efectivo del pensionista. El enumerado de `tipo` tiene exactamente dos valores por esa razón: agregar un tercero sería violar la regla desde el modelo.

## 9.4 `cierre_caja`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `turno_caja_id` | identificador | No | FK → `turno_caja`. Único |
| `efectivo_esperado` | dinero | No | |
| `efectivo_contado` | dinero | No | |
| `diferencia` | dinero | No | Puede ser negativa |
| `total_yape` | dinero | No | |
| `total_plin` | dinero | No | |
| `total_tarjeta` | dinero | No | |
| `total_transferencia` | dinero | No | |
| `observacion` | texto | Sí | |
| `cerrado_en` | momento | No | |
| `cerrado_por` | identificador | No | FK → `usuario` |

**Reglas**

- **RN-071.** El cajero **puede ver el efectivo esperado antes de contar**. Es una decisión del negocio, no un descuido; el modelo solo la registra.
- **RN-072, caso crítico 18.** Cualquier diferencia **desde S/ 0.01** genera alerta, y la alerta **no bloquea el cierre**.
- **`01` § Reglas de historial.** El cierre es **inalterable** una vez cerrado. Ninguna corrección posterior modifica esta fila.

## 9.5 `medio_pago`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `codigo` | enum('efectivo','yape','plin','tarjeta','transferencia','cuenta_pensionista') | No | Único |
| `nombre` | texto(40) | No | |
| `es_efectivo` | booleano | No | Entra en el arqueo |
| `activo` | booleano | No | |

**Nota.** `cuenta_pensionista` es un medio de pago porque desde el punto de vista del cobro lo es: la cuenta se salda contra el consumo. Pero **no** entra en la conciliación de caja, porque no mueve dinero en el turno. De ahí `es_efectivo` y la distinción explícita.

## 9.6 `pago`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `pedido_id` | identificador | No | FK → `pedido` |
| `cuenta_persona_id` | identificador | Sí | FK → `cuenta_persona`. Presente en cierre parcial |
| `medio_pago_id` | identificador | No | FK → `medio_pago` |
| `importe` | dinero | No | |
| `referencia` | texto(50) | Sí | Número de operación digital |
| `turno_caja_id` | identificador | No | FK → `turno_caja` |
| `registrado_en` | momento | No | |
| `registrado_por` | identificador | No | FK → `usuario` |

**Regla, RN-067.** Una cuenta se puede dividir **libremente** entre efectivo, Yape, Plin y tarjeta: por eso hay N pagos por pedido y no una columna de medio en el pedido. La suma de los pagos debe igualar el total de lo cerrado, y esa comprobación se hace en la aplicación al cerrar.

---

