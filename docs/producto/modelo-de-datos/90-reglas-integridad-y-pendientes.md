<!-- Extraído de rikos-backend/doc/RIKOS-modelo-de-datos.md (fuente completa, con la numeración de sección original intacta -- ver esa numeración si algún comentario en el código cita p. ej. "13.1"). Partido por bloque el 2026-09-29 per docs/METODO-spec-driven.md §5. -->

# 14. Reglas de integridad transversales

Las cuatro que `06` § Reglas de integridad esenciales declara, más las que se desprenden del resto.

| # | Regla | Dónde se implementa |
|---|---|---|
| 1 | DNI de pensionista único en la empresa | Restricción `unique` en `pensionista.dni` |
| 2 | Código de pre-ticket único y de un solo uso | `unique` en `pre_ticket.codigo` más `unique` en `cuenta_persona.pre_ticket_id` |
| 3 | Número de operación digital detecta posibles duplicados | **Alerta** en la aplicación, no restricción: dos bancos distintos pueden repetir número |
| 4 | Una venta cerrada conserva snapshot económico inmutable | Columnas de snapshot en `detalle_pedido`, escritas al cerrar y nunca actualizadas |
| 5 | Los cierres mensuales y de caja son inalterables | Sin operación de actualización en la capa de aplicación; comprobación por estado |
| 6 | Desactivar no elimina historial | Ninguna tabla admite borrado físico |
| 7 | Una receta no puede contenerse a sí misma | Recorrido del árbol al guardar; la base no puede validarlo |
| 8 | La suma de los pagos iguala el total cerrado | Comprobación al cerrar, con la regla de redondeo de 2.6 |
| 9 | Un turno abierto por caja a la vez | Índice único parcial sobre `turno_caja` donde `estado = 'abierto'` |
| 10 | Los movimientos de los dos libros no se actualizan ni se borran | Permisos de base de datos y ausencia de operaciones de actualización en el código |

---

# 15. Lo que este modelo deja fuera a propósito

Para que nadie lo agregue creyendo que falta:

- **Reservas de mesas** — excluido en `README`.
- **Fidelización de clientes regulares** — excluido.
- **Asistencia, horarios, planilla y remuneraciones** — excluido, RN-014.
- **Órdenes de compra y cuentas por pagar** — excluido.
- **Importación masiva desde Excel** — excluida.
- **Comisiones de motorizados** — sueldo fijo, RN-084.
- **Zonas y costo de delivery** — gratuito y sin zonas, RN-081.
- **Estado digital de «pedido listo» en cocina** — cocina avisa verbalmente, RN-065.
- **Ficha extensa del cliente regular** — solo los datos que exige el comprobante.
- **Historial de recorrido del motorizado** — solo la última ubicación.

---

# 16. Orden de construcción de las tablas

Qué migración crea qué, siguiendo el plan de sprints. Es el puente entre este modelo lógico y el físico.

| Sprint | Tablas que se crean |
|---|---|
| 0 | `event_publication`, `outbox` |
| 1 | `empresa`, `local`, `rol`, `permiso`, `rol_permiso`, `usuario`, `usuario_rol`, `usuario_local`, `sesion`, `bloqueo_pin` |
| 2 | `categoria_producto`, `unidad_medida`, `producto`, `producto_servicio`, `presentacion_compra`, `conversion_unidad`, `proveedor` |
| 3 | `usuario_permiso_excepcion`, `auditoria`, `motivo`, `horario_servicio`, `dia_especial`, `horario_dia_especial` |
| 4 | `receta`, `ingrediente_receta`, `semana_menu`, `dia_menu`, `opcion_menu`, `disponibilidad_producto` |
| 5 | `plan_tarifario`, `precio_plan`, `pensionista`, `responsable`, `cuenta_pensionista`, `movimiento_cuenta`, `saldo_cuenta_cache`, `periodo_cuenta` |
| 6 | `mesa`, `pedido`, `cuenta_persona`, `detalle_pedido`, `detalle_pedido_componente`, `comanda`, `detalle_comanda` |
| 7 | `caja`, `turno_caja`, `medio_pago`, `pago`, `movimiento_caja`, `cierre_caja` |
| 8 | `pre_ticket`, `abono` |
| 9 | `existencia`, `movimiento_inventario`, `transferencia`, `conteo_fisico`, `detalle_conteo`, `compra`, `detalle_compra`, `produccion`, `consumo_interno` |
| 10 | `conflicto_sincronizacion` |
| 11 | `serie_comprobante`, `comprobante_electronico`, `intento_emision`, `envio_correo`, `reconciliacion`, `emisor_local`, `outbox` (todas en la base de `servicio-facturacion`) · `comprobante_resumen` (en la de operación) |
| 12 | `pedido_delivery`, `historial_delivery`, `pedido_recojo` |
| 13 | `estado_cuenta_mensual` |

**Nota sobre el sprint 11.** `comprobante_electronico` vive en la base de datos de `servicio-facturacion`, que es un servicio separado (DT-011). No tiene clave foránea contra `pedido`: guarda el identificador del pedido como valor, sin restricción, porque cruzan la frontera entre dos servicios. Es la única relación del modelo que la base de datos no puede garantizar, y es deliberado.

---

# 17. Pendientes que este modelo deja abiertos

| ID | Qué falta | Afecta a |
|---|---|---|
| RF-006 | Umbral de intentos fallidos, duración del bloqueo y método de recuperación de acceso | `usuario` |
| RF-009 | Campos opcionales adicionales de clientes y proveedores | `proveedor` |
| RF-010 | Cantidad de locales, mesas, usuarios concurrentes y volumen pico | Dimensionamiento e índices |
| P-05 | Tratamiento del IGV y de las ventas a crédito, con el contador | `comprobante_electronico`, regla de redondeo de 2.6 |
| P-15 | Confirmación de la política de redondeo propuesta en 2.6 | Todo el modelo económico |
| `07` | Horarios exactos de servicio, precios, precio del táper, motivos iniciales, frecuencia de conteos, reglas de cortesía y productos compartidos, política de retención de respaldos | Datos, no estructura |
| `07` | Reglas exactas de resolución de conflictos entre operaciones offline de varios locales | `conflicto_sincronizacion` |

Ninguno de estos pendientes impide empezar: todos son datos o afinamientos, no cambios de estructura.

---
---

# PARTE II — Especificación física por microservicio

La parte I describe **qué** se representa. La parte II describe **cómo** se implementa: en qué base de datos vive cada tabla, qué claves foráneas existen y con qué comportamiento, qué índices hacen falta y cómo se rastrea cada tabla hasta el requisito que la justifica.

---

# 18. Notación empleada

Los diagramas usan **notación pie de cuervo** (crow's foot), que es la que entienden sin ambigüedad desarrolladores, arquitectos y administradores de base de datos.

| Símbolo en el diagrama | Lectura | Ejemplo del modelo |
|---|---|---|
| `||--||` | Uno a uno obligatorio en ambos lados | `pensionista` — `cuenta_pensionista` |
| `||--o|` | Uno a cero-o-uno | `turno_caja` — `cierre_caja` |
| `||--o{` | Uno a cero-o-muchos | `local` — `pedido` |
| `||--|{` | Uno a uno-o-muchos | `receta` — `ingrediente_receta` |
| `}o--o{` | Muchos a muchos, resuelto con tabla intermedia | `usuario` — `local` vía `usuario_local` |
| Línea punteada | **Referencia lógica sin clave foránea**, porque cruza la frontera entre microservicios | `comprobante_electronico` — `pedido` |

En las tablas de atributos:

| Marca | Significado |
|---|---|
| **PK** | Clave primaria |
| **PK compuesta** | Forma parte de una clave primaria de varias columnas |
| **FK →** | Clave foránea con integridad garantizada por la base de datos |
| **Referencia lógica** | Guarda el identificador de otra entidad **sin** clave foránea. La integridad la garantiza la aplicación |
| **UQ** | Restricción de unicidad |
| **CK** | Restricción de comprobación (`check`) |
| **IX** | Índice no único |

---

