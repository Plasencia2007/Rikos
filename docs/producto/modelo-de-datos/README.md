# Modelo de datos

Tablas, relaciones, tipos, índices y restricciones -- un archivo por
bloque, etiquetado con su fase (`EP-NN`), per `docs/METODO-spec-driven.md`
§5. Movido acá el 2026-09-29 desde `rikos-backend/doc/RIKOS-modelo-de-datos.md`
(ese archivo queda como redirección, con el mapeo completo).

**La numeración de sección original se conservó** (`3.11`, `13.1`, etc.)
-- el código y los historia docs la citan así, no por archivo.

## Índice

| # | Archivo | Bloque | Fase / historias |
|---|---|---|---|
| — | `00-como-leer-y-convenciones.md` | Cómo leer este documento + convenciones transversales (tipos, nombres, ULID, dinero) | — |
| 1 | `01-organizacion-usuarios-permisos--ep01.md` | Organización, usuarios y permisos | EP-01, US-001 a US-007 |
| 2 | `02-catalogo-unidades-proveedores--ep02.md` | Catálogo, unidades y proveedores (central, sin `local_id`) | EP-02, US-008/009/011 |
| 3 | `03-recetas-costeo--ep02.md` | Recetas y costeo (central) | EP-02, US-010 |
| 4 | `04-menu-programacion-semanal--ep03.md` | Menú y programación semanal | EP-03, US-013/014/015 |
| 5 | `05-pensionistas-planes-cuenta-corriente--ep04.md` | Pensionistas, planes y cuenta corriente -- lo que distingue a RIKO'S de un POS comprado | EP-04, US-016 a US-026 |
| 6 | `06-venta-pos-comandas--ep05.md` | Venta, POS y comandas (por local) | EP-05, US-027 a US-031 |
| 7 | `07-caja-turnos-pagos--ep06.md` | Caja, turnos y pagos | EP-06, US-032 a US-036 |
| 8 | `08-facturacion-electronica--ep06.md` | Facturación electrónica | EP-06, US-037, DT-003 a DT-010 |
| 9 | `09-inventario-produccion-compras--ep08.md` | Inventario, producción y compras (por local) | EP-08, US-043 a US-051 (el texto original dice "EP-08 y EP-09" -- error heredado del backlog v1.0, ver `docs/pendientes/README.md`) |
| 10 | `10-delivery-recojo-programado--ep07.md` | Delivery y recojo programado | EP-07, US-038 a US-042 |
| 11 | `11-auditoria-sincronizacion-soporte--ep01-ep10.md` | Auditoría, sincronización y soporte | EP-01 (US-006) + EP-10 (US-058 a US-061) |
| — | `90-reglas-integridad-y-pendientes.md` | Reglas de integridad transversales, qué queda fuera a propósito, orden de construcción de tablas, pendientes abiertos, notación | — |
| — | `91-arquitectura-microservicios-y-esquemas.md` | Frontera entre microservicios, esquema de `servicio-operacion`, esquema de `servicio-facturacion` | — |
| — | `92-referencia-completa-y-trazabilidad.md` | Tabla por tabla (todas las conexiones), índices sugeridos, campos de auditoría estándar, matriz de trazabilidad, hallazgos de revisión | — |

## Lo que sigue pendiente de esta reorganización

- Bloques 9 (inventario/producción/compras, sprint 9) y siguientes todavía
  no tienen tablas reales en Flyway -- este modelo es lógico/propuesto para
  esos bloques, no lo que ya existe en la base de datos. Los bloques 1, 2 y
  parte del 11 (auditoría) sí están implementados -- ver
  `rikos-backend/servicio-operacion/src/main/resources/db/migration/`.
- `92-referencia-completa-y-trazabilidad.md` tiene la matriz de
  trazabilidad y los hallazgos de la revisión original -- vale la pena
  revisarla cuando se cierre cada bloque nuevo, no solo al final.
