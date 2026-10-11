# PRD

Qué problema resuelve RIKOS, para quién, qué entra y qué no. Per
`docs/METODO-spec-driven.md` §1.

| Archivo | Qué contiene |
|---|---|
| `00-vision-y-alcance.md` | Propósito, los cinco problemas de negocio, actores, módulos, procesos principales, **qué queda fuera** (exclusiones confirmadas), inventario de las 8 fuentes de la especificación funcional |
| `01-epicas.md` | Las 11 épicas (EP-01 a EP-11) con objetivo y prioridad MoSCoW |
| `02-usuario-de-referencia.md` | El cajero del local a la hora del almuerzo, y el dueño que deja la caja por primera vez |
| `03-backlog-v3.md` | **Backlog v3 (recibido 2026-10-04), la versión vigente del producto:** 5 épicas (E1-E5), 64 historias US-01 a US-64 con criterios Given/When/Then, 8 tareas técnicas, 94 reglas de negocio. Copia íntegra, no se edita |
| `05-enmiendas-al-backlog-v3.md` | Lo que el equipo decidió distinto del v3, con fecha y razón (el v3 no se edita) |
| `04-traduccion-de-ids-v3.md` | Tabla v3 → IDs del repo (US-01 ↔ HT-07, US-06 ↔ US-028, ...). **Atención:** `US-01` (dos dígitos) es del v3; `US-001` (tres) es del repo y del código |

`00` y `01` salen de las secciones 1-3 del Product Backlog v2.0
(2026-09-11); `02` se escribió con el usuario el 2026-09-29.

## Lo que falta para que sea un PRD completo según el método

- **Diferenciador** -- el backlog dice que la cuenta del pensionista es
  lo que distingue a RIKO'S de un POS comprado, pero no lo desarrolla.
- **Riesgos del producto** -- distintos de los riesgos por historia, que
  sí están en `flujos/`.

## Dos numeraciones conviven

El código, los commits y `rikos-backend/doc/historias/` usan los IDs de tres dígitos (US-001 a US-062, HT-01 a HT-10) y se mantienen así para no romper enlaces. El backlog v3 usa US-01 a US-64 y 5 épicas. Los archivos de `docs/producto/flujos/` siguen por las 11 épicas viejas hasta que se decida reorganizarlos; la tabla de `04-traduccion-de-ids-v3.md` es el puente. La comparación de lo construido contra el v3 está en `docs/producto/plan/04-comparacion-backlog-v3.md`.
