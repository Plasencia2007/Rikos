# Sprint 6 — cierre (2026-10-05)

Vender en mesa: US-027 a US-031 más el sprint 6b. Este documento junta lo que no cabía en cada historia: qué queda abierto, qué se decidió distinto del
backlog y cómo se verificó el cierre.

## Qué se entregó
Mapa de mesas, pedido por persona, comanda con foto y anulaciones, reorganizar cuentas (mover, unir, separar, trasladar), lista de pedidos, "Terminar pedido", categorías de la
carta y el rediseño completo del POS (hecho con la sesión de diseño). Migraciones V29 a V38.

## Qué se decidió distinto del backlog v3
- **US-07 CA2 (unir mesas):** se une una mesa libre sin pedido a una principal; el criterio cambió, no el código. `docs/producto/prd/05-enmiendas-al-backlog-v3.md`.
- **"Terminar pedido":** queda provisional hasta el sprint 7 (DT-059).
- **Traslado de varias líneas a la vez:** el backend lo admite, pero la pantalla ya no lo ofrece (decisión de diseño); solo línea por línea desde el ⋮.

## Cómo se verificó
Suite completa: 600 pruebas, 0 fallos y 1 error, el ciclo conocido de Modulith (`comun` ↔ `identidad`, `ArquitecturaDeModulosTest`). Pantalla del pedido verificada en el navegador a 1280 y 375 px.
Comparación contra el backlog v3 (23 historias): `docs/producto/plan/04-comparacion-backlog-v3.md`.

## Arreglos de última hora
Bug del PIN con User-Agent largo (DT-061, prueba en `LimitadorIntentosPinTest`) y prueba de agotados `ComandasIntactasAlAgotarTest` (US-04 CA2).

## Qué queda abierto
- **HT-10:** prueba de impresión con la comandera real (depende del hardware).
- **Tipo de entrega** en la comanda (US-06 CA6): falta saber qué valores quiere el dueño.
- **56 px y teclado único (US-01 CA1 y CA3):** el POS usa 44 px y botones − / + para cantidades; decidir si se sube a 56 px o se enmienda el criterio.
- La comanda muestra al mozo que abrió el pedido, no al que comandó (US-08 CA1).
- Brechas heredadas de sprints anteriores listadas en el documento de comparación (inmutabilidad de la bitácora solo en la aplicación, evento `transferencia` sobrecargado, costos visibles por P-30).
- Datos de prueba: PIN de `mozotest` y `mtorres` asignados al azar (hay que reasignarlos), y 6 pedidos de desarrollo abiertos que no son de esta sesión.
