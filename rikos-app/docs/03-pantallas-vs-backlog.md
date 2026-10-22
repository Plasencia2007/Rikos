# 03 · Pantallas del canvas vs. backlog v3

> **Documento nuevo. No modifica `03-backlog-v3.md` ni `05-enmiendas-al-backlog-v3.md`.** Solo cruza lo que ya existe: las 80 láminas del canvas de Rikos-App contra las 64 historias del backlog v3 (IDs de dos dígitos; el ID del repo, de tres dígitos, va en la segunda columna según `04-traduccion-de-ids-v3.md`).
>
> Alcance de la lectura: se cruzó por **título y propósito de la historia**. No se verificó criterio de aceptación por criterio de aceptación; eso queda para cuando se entregue cada historia (método spec-driven, paso de especificación).

Leyenda de cobertura: **Completa** = hay pantalla(s) para el flujo principal y los casos borde conocidos · **Parcial** = hay pantalla pero falta una parte de la historia · **Sin pantalla** = el canvas no la cubre.

## 1. Historias con diseño

### E1 · Registrar los pedidos de venta

| Historia | ID repo | Pantallas | Cobertura | Nota |
|---|---|---|---|---|
| US-01 Operar el POS con una sola mano | HT-07 | Transversal: Sistema visual, DS1, DS2, M07x/M08x (letra grande) | Completa | Zona del pulgar, objetivos ≥ 48 dp, contraste auditado (27 pares) |
| US-02 Programar el menú semanal | US-013 | A02c Menú del día | Parcial | Solo ver/activar; programar la semana se queda en la web |
| US-03 Menú normal y menú extra | US-014 | M09 Menú del día y extras | Completa (mozo) | La configuración del menú es de la web |
| US-04 Marcar agotados | US-015 | M06 Agotados | Completa | |
| US-05 Mapa de mesas | US-027 | M03, M03b (noche), M03c (vistazo), M03d (avisos), T01 (iPad) | Completa | M03d depende de WebSocket (ver `02-brechas-de-api.md`) |
| US-06 Pedido por persona | US-028 | M07, M08, M08b, M08c, M10, M07n/M08n, M07x/M08x, T02 | Completa | |
| US-07 Unir, mover, dividir y trasladar | US-029 | M13, M13b, M13c, M13d, M14, M29 | Completa | M29 resuelve 409 `PEDIDO_MODIFICADO` |
| US-08 Emitir la comanda | US-030 | M12, M12a, M12b, M12c, M12d, M12n | Completa | Reimpresión en M12d |
| US-09 Anular productos | US-031 | M11, M11b, M11c | Completa | M11c exige PIN de supervisor (hueco de API, ver `02-brechas-de-api.md`) |
| US-10 Recojo programado | US-038 | M24, M24a | Completa | Campos de recojo aún no están en `POST /api/pedidos` |
| US-12 Delivery y asignar motorizado | US-040 | M24a, M25, M25b | Parcial | Registrar y seguir: sí. Asignar motorizado: falta pantalla |

### E2 · Controlar la cobranza

| Historia | ID repo | Pantallas | Cobertura | Nota |
|---|---|---|---|---|
| US-15 Ficha del pensionista | US-016 | A05, M10 | Parcial | Consulta; el alta/edición completa queda en web |
| US-17 Cuenta corriente del pensionista | US-018 | A06 | Completa (consulta) | |
| US-18 Abrir turno de caja | US-032 | M21 | Completa | |
| US-19 Cobrar con medios divididos | US-033 | M15, M15b, M15c, M16, M16b, M16n, M17, M18, T03 | Completa | Precuenta (M15c) sin endpoint, `02-brechas-de-api.md` |
| US-20 Descuentos y anulaciones | US-034 | M19, M11c | Completa | |
| US-21 Reabrir una venta | US-035 | M20 | Completa | |
| US-22 Cerrar el turno | US-036 | M22, M22b, A07 | Completa | |
| US-24 Vincular pre-ticket al pedido | US-020 | M10 | Parcial | Falta el escaneo/ingreso del pre-ticket como paso propio |
| US-25 Abonos en caja | US-021 | M23 | Completa | |
| US-26 Aprobar/rechazar abonos | US-024 | A03, A04 | Completa | |
| US-27 Comprobantes electrónicos | US-037 | M16b, M18, M18b | Completa | M18b = SUNAT pendiente |
| US-28 Estado de cuenta | US-025 | A06 | Parcial | Consulta; el cierre mensual es de la web |
| US-29 Comprobante por correo | US-062 | M18 (compartir) | Parcial | Compartir por WhatsApp/otro; correo depende del dato del cliente |

### E4 · Continuidad del servicio

| Historia | ID repo | Pantallas | Cobertura | Nota |
|---|---|---|---|---|
| US-48 Operar sin Internet | US-058 | M27 | Parcial | Cubre el estado y la cola; sin Internet pero con red local depende de DT-018/019/024 |
| US-49 Sincronizar sin duplicados | US-059 | M27, M29 | Parcial | Sin ids de idempotencia en cobros/abonos (ver `02-brechas-de-api.md`) |

### E5 · Supervisar la operación

| Historia | ID repo | Pantallas | Cobertura | Nota |
|---|---|---|---|---|
| US-53 Usuario y contraseña | US-002 | A01 | Completa | |
| US-54 PIN de mozo | US-003 | M02, M28 | Completa | M28 = bloqueo 423 (DT-039) |
| US-55 Usuarios y multilocal | US-005 | A09b, M26 | Parcial | Cambio de local sí; alta de usuarios en web |
| US-56 Roles y permisos | US-004 | A09b, M15b, M30 (sin permiso) | Parcial | La app solo muestra/respeta permisos |
| US-57 Auditoría | US-006 | A08 Bitácora | Completa (consulta) | |
| US-59 Panel del administrador | US-052 | A02, A02b | Parcial | Resumen y salón en vivo; filtros avanzados en web |

## 2. Historias sin pantalla (fuera del alcance móvil o pendientes)

**Decisión de producto abierta (la toma el equipo, no este doc):** ¿estas historias son «de la web» por diseño, o falta diseñarlas para móvil?

| Grupo | Historias | Lectura |
|---|---|---|
| Recojo y delivery, parte operativa | US-11 (recojo no retirado), US-13 (vista motorizado), US-14 (liquidar motorizado) | Es otro rol (motorizado) y otro flujo; candidato a una segunda fase |
| Pensionistas, parte administrativa | US-16 (planes tarifarios), US-23 (pre-ticket de autoservicio), US-30/31 (portal), US-32 (desactivar) | El portal y el kiosco son otras superficies; US-32 podría ser una acción de A06 |
| Inventario y catálogo | US-33 a US-46 (catálogo, recetas, existencias, Kardex, compras, conteos…) | Fuera del alcance mozo-primero; viven en la web |
| Continuidad (infraestructura) | US-47 (reinicio autónomo), US-50 (respaldo), US-51 (seguridad) | No tienen UI; la app solo debe respetarlas |
| Administración | US-52 (empresa/locales), US-58 (horarios de servicio) | Web |
| Reportes | US-60 a US-64 | Solo hay un resumen en A02; reportes completos en web |

## 3. Pantallas sin historia propia

Son pantallas **habilitantes o transversales**: sirven a varias historias y no tienen una propia en el backlog.

| Pantalla | Para qué sirve | Historias a las que apoya |
|---|---|---|
| M00 Bienvenida | Primer arranque | US-54 |
| M01 Configurar local | Vincular el equipo con `localCodigo` (DT-040) | US-54, US-48 |
| M26 / M26b Perfil y ajustes del equipo | Cambio de local, modo noche, tamaño de letra | US-01, US-55 |
| M30 / M30b Cargando, vacío, error, sin permiso | Estados comunes | Todas |
| M03d Avisos en tiempo real | «Plato listo», «piden la cuenta» | US-05, US-08 (requiere WebSocket) |
| A09 Más: local y cuenta | Sesión, versión | US-53 |

Sugerencia (sin tocar el backlog): si el equipo quiere que estas queden trazables, lo natural sería registrarlas como **tareas técnicas habilitantes** (sección 4 del backlog) o como una enmienda en `05-enmiendas`. Esa decisión es del equipo.

## 4. Resumen

| | Cantidad |
|---|---|
| Historias totales del backlog v3 | 64 |
| Con pantalla completa | 21 |
| Con pantalla parcial | 11 |
| Sin pantalla | 32 |
| Láminas del canvas | 80 (incluye mapa, sistema visual, flujo navegable, modo noche/letra grande y DS) |

Lectura rápida: **el flujo del mozo (E1 salón y E2 caja) está cubierto de punta a punta**; lo que falta es lo que el backlog ya ubica en otras superficies (web, portal, motorizado, kiosco).
