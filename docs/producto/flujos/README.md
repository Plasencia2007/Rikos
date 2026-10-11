# Flujos

Cómo funciona cada proceso: actores, camino feliz, casos raros, reglas y
qué pasa cuando algo falla. Per `docs/METODO-spec-driven.md` §2.

Un archivo por épica. Cada historia trae actor, historia de usuario,
reglas de negocio (`RN-NNN`), criterios de aceptación en Given/When/Then
(el CA1 suele ser el camino feliz; los siguientes, los casos raros),
dependencias, riesgos y trazabilidad a la especificación funcional.

| Archivo | Épica | Historias |
|---|---|---|
| `01-ep01-fundaciones.md` | Organización, usuarios, permisos y auditoría | US-001 a US-007 |
| `02-ep02-catalogos-costeo.md` | Catálogos, datos maestros y costeo | US-008 a US-012 |
| `03-ep03-menu-programacion.md` | Menú, programación semanal y disponibilidad | US-013 a US-015 |
| `04-ep04-pensionistas.md` | Cuenta corriente, autoservicio, portal y abonos | US-016 a US-026 |
| `05-ep05-pos-mesas-cocina.md` | POS, mesas y cocina | US-027 a US-031 |
| `06-ep06-caja-facturacion.md` | Caja, cobro y facturación electrónica | US-032 a US-037, US-062 |
| `07-ep07-delivery-recojo.md` | Delivery propio y recojo programado | US-038 a US-042 |
| `08-ep08-inventario-produccion-compras.md` | Inventario, producción y compras | US-043 a US-051 |
| `09-ep09-panel-reportes.md` | Panel de administración y reportes | US-052 a US-057 |
| `10-ep10-continuidad-offline.md` | Offline, sincronización, respaldo y seguridad | US-058 a US-061 |
| `11-ep11-habilitantes-tecnicas.md` | Habilitantes técnicas | HT-01 a HT-10 |
| `90-reglas-de-negocio-consolidadas.md` | Todas las `RN-NNN` en un solo lugar | -- |

## Orden de lectura

Para alguien nuevo: `01` (fundaciones, el vocabulario base), `04`
(pensionistas, el corazón del negocio), `05` y `06` (venta y caja), y
el resto según lo que vaya a tocar. `90` es consulta, no lectura de
corrido.

El orden de **construcción** es otro -- está en
`docs/producto/plan/01-plan-de-sprints.md`.

## Lo que falta para que sea "flujos" según el método

Esto está escrito como historias de usuario, no como procesos. La
diferencia importa en un punto: un proceso como "el pensionista almuerza"
cruza cuatro historias (US-019 pre-ticket → US-028 pedido → US-033 cobro
→ US-018 cargo en cuenta) y hoy no hay un documento que lo cuente de
punta a punta. El método también pide en este README los **conceptos
transversales** (vocabulario único) y los **estados principales**
(pedido, mesa, pre-ticket, abono, turno de caja, delivery) -- todavía no
están consolidados acá, están repartidos dentro de cada historia.

> **Nota (2026-10-04):** el backlog v3 (`../prd/03-backlog-v3.md`) reagrupa estas historias en 5 épicas y las renumera US-01..US-64. Estos archivos siguen con la numeración de tres dígitos; la equivalencia está en `../prd/04-traduccion-de-ids-v3.md`.
