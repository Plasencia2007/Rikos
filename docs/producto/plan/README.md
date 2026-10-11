# Plan

> **Corte del 11/10/2026:** consultar la [auditoría de producto y sprints](../../auditorias/2026-10-11/01-producto-sprints-y-equipo.md)
> y el [registro de pendientes](../../pendientes/02-auditoria-integral-sprints-0-7-2026-10-11.md).
> Las filas de estado siguientes conservan el resumen anterior: sprint 6/6b tiene
> cierre histórico del 05/10 con pendientes diferidos; Pedido rápido está construido;
> sprint 7 entrega 1 está hecha. Entrega 2/V41 se revisó como trabajo local y fue
> declarada backend hecho por otra sesión durante la auditoría; UI/aceptación y
> los hallazgos continúan abiertos.

Fases y entregas chicas y verificables. Per `docs/METODO-spec-driven.md` §6.

| Archivo | Qué contiene |
|---|---|
| `01-plan-de-sprints.md` | Los 15 sprints (0-14) con su objetivo, contenido y **qué se demuestra al final de cada uno**, el porqué del orden (permisos y auditoría al sprint 3, sprint 0 de herramientas, offline antes de facturación), lo que sigue bloqueado por terceros, y la tabla completa de las 72 historias ordenadas por sprint |
| `02-grafo-de-dependencias.md` | Nodos críticos: las historias de las que dependen muchas otras |
| `03-historial-de-cambios-del-backlog.md` | Qué cambió de la versión 1.0 a la 2.0 del backlog |
| `sprint-04-costos-y-menu.md` | Especificación del sprint 4: auditoría contra el modelo, decisiones, preguntas abiertas, entregas verificables |

Origen: secciones 4, 5.1, 8 y 9 del Product Backlog v2.0.

## Estado real por sprint

| Sprint | Objetivo | Estado | Historia(s) en `rikos-backend/doc/historias/` |
|---|---|---|---|
| 0 | Que las herramientas dejen de estorbar (HT-01/02/03) | Completo | -- |
| 1 | Entrar al sistema (US-001, US-005, US-002, US-003) | Completo | `US-001-US-003-bloqueo-pin-y-local-activo.md` |
| 2 | El primer módulo del negocio (US-008/009/011, HT-05, HT-07 = US-01 del v3) | Completo, salvo US-01 CA1 y CA3 (ver `04-comparacion-backlog-v3.md`) | `US-008-US-009-US-011-catalogo.md` |
| 3 | Control y trazabilidad (US-004, US-006, US-012, US-007, HT-06) | Completo -- HT-08 movido al 10 | `US-004-US-006-permisos-roles.md`, `2026-09-27-sprint3-control-y-trazabilidad.md` |
| 4 | Costos y menú (US-010, US-014, US-013, US-015) | Completo | `sprint-04-costos-y-menu.md`, `US-010-recetas-y-costeo.md`, `US-014-menu-normal-y-extra.md`, `US-013-programacion-semanal.md`, `US-015-agotados-por-local.md` |
| 5 | La cuenta del pensionista (US-017, US-016, US-018) | ✅ Cerrado (2026-09-29) -- 4 entregas: planes, parámetros, motor de precios, ficha y cuenta corriente | `sprint-05-cuenta-del-pensionista.md`, `US-017-planes-tarifarios-y-parametros-de-cobro.md`, `sprint5-motor-de-precios.md`, `US-016-ficha-del-pensionista.md`, `US-018-cuenta-corriente.md` |
| 6 | Vender en mesa (US-027 a US-031, HT-10) | En curso -- entregas 1 (mesas y mapa), 2A (pedido, backend) y 2B (pantalla del pedido) 3 (comanda y anulaciones) y 4 (reorganizar) hechas: el sprint 6 está completo salvo HT-10, que espera la impresora. Decisiones registradas como DT-055 a DT-061; comparación con el backlog v3 en `04-comparacion-backlog-v3.md` | `sprint-06-vender-en-mesa.md`, `US-027-mapa-de-mesas.md`, `US-028-tomar-el-pedido-backend.md`, `US-028-tomar-el-pedido-pantalla.md`, `US-029-reorganizar-cuentas.md`, `US-030-US-031-comanda-y-anulaciones.md` |
| 6b | Rediseño del POS (shell, Inicio, Órdenes activas, Pedido rápido, Historial) | En curso -- diseño aprobado 2026-09-30; entregas 1, 3 y 5 hechas; falta la 4 (Pedido rápido) | `sprint-06b-pos-rediseno.md`, `sprint-06b-lista-de-pedidos.md` (backend, sesión de pedidos), `salones-y-mesas.md` |
| 7 | Cobrar y cerrar caja (US-032 a US-036) | Especificado y aprobado el 2026-10-05; las 5 entregas hechas en backend: 1 (cajas y turnos), 2 (cobro), 3 (descuentos y anulación), 4 (cierre de turno) y 5 (reapertura); falta la pantalla de caja (5 entregas: cajas y turnos, cobro, descuentos y anulación, cierre de turno, reapertura) | `sprint-07-cobrar-y-cerrar.md`, `US-032-cajas-y-turnos.md` y `US-033-cobro.md` y `US-034-descuentos-y-anulacion-en-caja.md` y `US-035-US-036-cierre-de-turno-y-reapertura.md` (en historias) |
| 8 | El circuito del pensionista completo (US-019, US-020, US-021, US-024) | Pendiente | -- |
| 9 | Existencias reales (US-043, US-050, US-044, US-048, US-045, US-049, US-051) | Pendiente | -- |
| 10 | Que el local no dependa de Internet (HT-04, HT-08, HT-09, US-058 a US-061) | Pendiente | -- |
| 11 | Comprobantes (US-037, US-062) | Pendiente -- **bloqueado por terceros** (P-01 a P-05) | -- |
| 12 | Canales no presenciales (US-038 a US-042) | Pendiente | -- |
| 13 | Autoservicio del pensionista y cierre mensual (US-022, US-023, US-025, US-026) | Pendiente | -- |
| 14 | Visibilidad (US-052 a US-057, US-046, US-047) | Pendiente | -- |

## Cambios al plan original

### HT-08 (observabilidad mínima): del sprint 3 al 10 -- 2026-09-29

Se movió junto con HT-09 (imágenes y perfiles de despliegue). La
observabilidad sirve cuando hay un nodo en el local que no se puede mirar
directamente -- logs centralizados, métricas de sincronización. Mientras
todo corre en la máquina del desarrollador, sería instrumentar algo que
nadie mira. Es prioridad Should: no afecta el MVP. Se descartó hacerlo en
el 3 porque llegaba tarde (el sprint ya estaba cerrado) y sin nada que
observar todavía.

### Lo que el backlog pide mover ya

El sprint 11 depende de datos fiscales de la empresa, pruebas en la
cuenta demo del proveedor de comprobantes y una consulta al contador. El
backlog recomienda **iniciar esas consultas ahora**, no en el sprint 10,
porque son de terceros y tardan en volver.

## Cómo probar lo ya construido, en vivo

Logueado como `admin` / `cambiar123`:

- `/admin/catalogo/productos`, `/admin/catalogo/categorias`,
  `/admin/catalogo/unidades` -- catálogo (sprint 2).
- `/admin/compras/proveedores` -- proveedores (sprint 2).
- `/admin/configuracion/motivos` -- motivos configurables (sprint 3).
- `/admin/auditoria` -- bitácora, con filtros (sprint 3).
- `/admin/usuarios`, `/admin/usuarios/roles` -- usuarios, roles y
  permisos granulares (sprints 1 y 3).
