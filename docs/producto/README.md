# Producto: especificación (spec-driven)

Especificación de RIKOS siguiendo `docs/METODO-spec-driven.md`. Esta carpeta
es la fuente de verdad de **qué se construye y por qué** — separada de
`rikos-backend/doc/historias/` (que documenta qué ya se construyó y cómo
se verificó, después del hecho).

Todo lo de acá deriva de la especificación que entregó el dueño, que vive
sin tocar en `docs/fuente/especificacion-v0.1/`. Si algo de acá la
contradice, tiene que ser una decisión anotada con su razón.

## Los siete pasos

| # | Carpeta | Qué responde | Estado |
|---|---|---|---|
| 1 | `prd/` | Qué problema resuelve, para quién, qué entra y qué no | **Base hecha** (desde el backlog) -- falta usuario de referencia concreto, diferenciador y riesgos del producto |
| 2 | `flujos/` | Cómo funciona cada proceso, con reglas y casos raros | **Base hecha** -- 72 historias con criterios Given/When/Then, una épica por archivo. Falta: procesos de punta a punta, vocabulario y estados consolidados |
| 3 | `ui-ux/` | Design system, wireframes de pantallas críticas | **Base hecha** -- mapa de las ~70 pantallas y reglas de diseño. Falta: design system documentado (los tokens existen solo en código) y wireframes |
| 4 | `trd/` | Stack, arquitectura, integraciones, infraestructura | **Hecho** -- `00-arquitectura.md`, `01-stack-y-despliegue.md`. La bitácora de decisiones (`RIKOS-decisiones-tecnicas.md`) queda aparte a propósito, ver `trd/README.md` |
| 5 | `modelo-de-datos/` | Tablas, relaciones, tipos, índices, restricciones | **Hecho** -- partido en 15 archivos por bloque/fase, movido desde `RIKOS-modelo-de-datos.md` (que ahora redirige acá) |
| 6 | `plan/` | Fases y entregas chicas y verificables | **Hecho** (estado actual) -- falta el detalle de entregas para los sprints 4+ cuando se retomen |
| 7 | Código | -- | Sprints 0-3 completos, ver `rikos-backend/doc/historias/` |

## Orden de lectura vs. orden de construcción

- **Construcción**: PRD → Flujos → UI/UX → TRD → Modelo de datos → Plan → Código (en ese orden, cada uno se apoya en el anterior).
- **Lectura** (para alguien nuevo entrando al proyecto): `prd/00-README.md` primero (contexto y vocabulario), después `flujos/00-README.md`, después el resto según lo que necesite tocar.

## Por qué esto es retroactivo en parte

RIKOS ya tiene sprints 0-3 construidos (identidad/permisos, catálogo,
auditoría transversal, motivos) antes de empezar a usar este método. La
reconstrucción de la especificación para eso ya construido va sección por
sección, igual que si fuera nuevo -- no se asume nada por estar "ya
hecho", se documenta la decisión real y su razón tal como quedó.
