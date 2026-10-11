# Convencion: una historia de usuario, un archivo

A partir de ahora, cada historia de usuario (o sprint completo, si son
varias chicas relacionadas) se documenta aca, no solo en el mensaje de
chat. Un archivo por historia: `US-XXX-nombre-corto.md`.

## Que va en cada archivo

- **Que se pidio** (resumen corto, no el copy-paste completo del chat).
- **Que se construyo**: modelo/migraciones, backend, frontend -- lo
  suficiente para que alguien que no vio el chat entienda el cambio sin
  leer el diff completo.
- **Que se dejo fuera a proposito y por que** (si algo se excluyo por
  duda o por no estar pedido explicitamente).
- **Como se verifico**: pruebas automatizadas (cuantas, cuales) +
  evidencia en vivo (que se probo a mano, con que datos reales -- no
  "deberia funcionar", sino "se probo con el usuario X y paso esto").
- **Bugs encontrados en el camino** (aunque no sean del alcance de la
  historia) y como se resolvieron.

## Por que

Para que otra sesion de Claude (u otra persona) pueda entender el estado
real del sistema sin tener que releer la conversacion completa donde se
construyo. El chat se pierde o se resume; esto queda.

## Historias documentadas

| Archivo | Historia | Estado |
|---|---|---|
| `US-004-US-006-permisos-roles.md` | Permisos granulares + roles y auditoria | Hecho, verificado |
| `US-008-US-009-US-011-catalogo.md` | Catalogo de productos, unidades/presentaciones/conversiones, proveedores + HT-05/HT-07 + sidebar generalizado | Hecho, verificado |
| `US-001-US-003-bloqueo-pin-y-local-activo.md` | DT-039 (bloqueo de PIN persistido por dispositivo) + US-001 CA3 (`activo=falso` bloquea el PIN) | Hecho, verificado |
| `2026-09-25-endurecimiento-pre-sprint-3.md` | Manejador global de errores, guarda contra la semilla de desarrollo en `nube`, `esquema.d.ts` regenerado, matchers muertos, puerto Postgres, `CrearUnidad`, 43 endpoints tipados para OpenAPI | Hecho, verificado |
| `US-016-ficha-del-pensionista.md` | Sprint 5, entrega 3: ficha del pensionista y su responsable, DNI único, PIN generado y mostrado una vez, cuenta abierta, desactivar y reactivar | Hecho, verificado |
| `sprint5-motor-de-precios.md` | Sprint 5, entrega 2: motor de precios (cliente normal, pensionista con ticket, extra, segundo almuerzo, agregados, táper), tarjeta "Probar un precio" | Hecho, verificado |
| `US-017-planes-tarifarios-y-parametros-de-cobro.md` | Sprint 5, entrega 1: planes con versiones y vigencia, parámetros de cobro (táper, segundo consumo), módulo `pensionistas`, permiso `pensionistas.administrar` | Hecho, verificado |
| `sprint-06b-lista-de-pedidos.md` | Sprint 6b: `GET /pedidos` (en curso con líneas o cerrados, filtros y paginación), `GET /pedidos/resumen` y `mozoNombre`/`abiertoEn` en el pedido, para el rediseño del POS; 17 pruebas |
| `US-035-US-036-cierre-de-turno-y-reapertura.md` | Sprint 7, entregas 4 y 5: cierre de turno inalterable con conciliación (V43) y reapertura de un cobro dentro del turno (V44); 25 pruebas |
| `US-034-descuentos-y-anulacion-en-caja.md` | Sprint 7, entrega 3: descuentos por línea al cobrar (V42, permiso `caja.descontar`), anular lo último cierra el pedido, y los arreglos CAJ-01/03/05/06 de la auditoría |
| `US-033-cobro.md` | Sprint 7, entrega 2: cobro por líneas (V41), pago dividido con vuelto, cargo al pensionista, snapshot por línea, "Terminar pedido" acotado; 30 pruebas |
| `US-032-cajas-y-turnos.md` | Sprint 7, entrega 1: cajas y turnos (V40, módulo `caja`), un turno abierto por caja y por cajero; 16 pruebas |
| `sprint-06-cierre.md` | Cierre del sprint 6: qué se decidió distinto del backlog v3, cómo se verificó y qué queda abierto (HT-10, tipo de entrega, 56 px) |
| `US-029-reorganizar-cuentas.md` | Sprint 6, entrega 4: pasar productos entre personas y dividir la cuenta con el precio recalculado, mover el pedido de mesa, unir y separar mesas; comanda intacta (V36); 41 pruebas nuevas |
| `US-030-US-031-comanda-y-anulaciones.md` | Sprint 6, entrega 3: comandar (foto por comanda), vista imprimible con COPIA desde la segunda impresión, anular con permiso y motivo antes o después de comandar, bitácora; 35 pruebas nuevas |
| `US-028-tomar-el-pedido-pantalla.md` | Sprint 6, entrega 2B: pantalla del pedido en el POS (personas, menú, carta, extra, pensionista, para llevar con táper); verificada en el navegador en escritorio, tablet y móvil |
| `US-028-tomar-el-pedido-backend.md` | Sprint 6, entrega 2A: pedido, personas y líneas con precio del motor (menú, carta, comida del plan, extra), mesa ocupada de verdad, API con 409 en concurrencia; 449 pruebas |
| `US-027-mapa-de-mesas.md` | Sprint 6, entrega 1: módulo `venta`, mesas por local con estado derivado, mapa en el POS, bloqueo con motivo (también el mozo); 361 pruebas |
| `US-018-cuenta-corriente.md` | Sprint 5, entrega 4 (cierra el sprint): libro de cuenta solo de agregado, saldo inicial, ajustes con motivo, tope calculado o fijado que solo recuerda, pantalla Cuenta; 325 pruebas |
| `US-015-agotados-por-local.md` | Sprint 4, entrega 4 (cierra el sprint): agotados por local, reactivación con motivo, permiso `menu.marcar_agotado`; bug de cambiar de local corregido | Hecho, verificado |
| `US-013-programacion-semanal.md` | Sprint 4, entrega 3: semana, días y opciones, borrador → confirmado, en servicio calculado, advertencia por repetición, menú del día para el POS | Hecho, verificado |
| `US-014-menu-normal-y-extra.md` | Sprint 4, entrega 2: menú normal y extra como productos del sistema, precio con `menu.modificar`, RN-023 | Hecho, verificado |
| `US-010-recetas-y-costeo.md` | Sprint 4, entrega 1: recetas y subrecetas valorizadas, recálculo en cascada, costos ocultos sin `recetas.ver_costos` | Hecho, verificado |
| `2026-09-27-sprint3-control-y-trazabilidad.md` | US-006 (auditoría transversal), US-012 (motivos), HT-06 (fronteras de módulos, backend+frontend), P-26/P-29 cerrados | Hecho, verificado -- HT-06 backend con 1 ciclo `comun↔identidad` reportado, no resuelto |
- [Salones y mesas](salones-y-mesas.md) — salones por local, plano visual, V34
- [Rediseño del POS · entrega 1](pos-rediseno-entrega-1-shell.md) — shell, Inicio y Mesas
- [Rediseño del POS · entregas 3 y 5](pos-rediseno-entregas-3-y-5-ordenes-e-historial.md) — Órdenes activas e Historial
- [Rediseño del POS · responsive y pruebas](pos-rediseno-responsive-y-pruebas.md) — fallas a 375/768/1280 px y arreglos
- [Rediseño del POS · mesas y unir](pos-rediseno-mesas-y-unir.md) — mapa como la referencia, unir/separar desde el mapa
- [Rediseño del POS · entrega 4](pos-rediseno-entrega-4-pedido-rapido-y-pantalla-del-pedido.md) — Pedido rápido y pedido a pantalla completa
- [pos-fotos-de-producto-y-pedido-escritorio-v2.md](pos-fotos-de-producto-y-pedido-escritorio-v2.md) -- fotos de producto (V39) y pedido en escritorio v2
