# Seguimiento Scrum (solo web)

Cómo se registran los sprints del área **web** (`rikos-web/`, `rikos-backend/`, `docs/` y la
raíz) en la hoja «Gestión Scrum». La app (`rikos-app/`) no entra en los sprints ni en la hoja:
ver `CLAUDE.md` § «Cómo trabajamos (áreas y ramas)».

## Equipo

| Nombre completo (como va en la hoja) | Rol |
|---|---|
| Andrey Fidel Mestanza Bazan | Product Owner / Analista / Frontend |
| Eduard Joel Chambilla Quispe | Scrum Master / Analista / Tester |
| William Jhonsons Plasencia Valdez | Development Team / Backend / DBA |

## Sprints

| Sprint | Del | Al | Objetivo |
|---|---|---|---|
| 1 | 22/10/2026 | 04/11/2026 | Atender un día completo en un local sin papel ni cuadernos. |
| 2 | 05/11/2026 | 18/11/2026 | Operar sin depender de Internet ni del salón. |

Hasta el 04/11 se usan **Bitácora S1** e **Incidencias S1**. Desde el 05/11, las **S2**.

**La hoja trabaja solo con estos dos sprints, y es lo que revisa el profesor.** Todo lo que
ya estaba construido antes del 22/10 cuenta dentro del sprint 1.

Los «sprints» que aparecen en el repo (`docs/producto/plan/sprint-04-...` a `sprint-09-...`,
el plan de 15 sprints y las historias que los citan) son la **secuencia interna de
construcción**, no los sprints Scrum de la hoja. Se dejan como están: no se renumeran ni se
cruzan con los sprints 1 y 2. Para la hoja manda lo que hay en el código y en `develop`.

## Hoja «Gestión Scrum»

ID `1B5Pd-6RIqtnSmpi2_-yb6JJbRcLT-mwqEUQn71wWWKM` ·
https://docs.google.com/spreadsheets/d/1B5Pd-6RIqtnSmpi2_-yb6JJbRcLT-mwqEUQn71wWWKM/edit

### Product Backlog
Encabezados en la fila 4, datos desde la fila 5 (US-n está en la fila n+3).

| Col. | Contenido |
|---|---|
| A | Épica |
| B | ID |
| C | Historia |
| D | Historia completa |
| E | Prioridad |
| F | Puntos |
| G | Sprint |
| H | Estado: Por hacer / En curso / En revisión / Terminada |
| I | Tareas — **FÓRMULA, no tocar** |
| J | Tareas hechas — **FÓRMULA, no tocar** |
| K | ID en el repo |
| L | Observación |

### HU técnicas
ID, HU técnica, Historia, HU que habilita, Sprint, Estado, Observación.

### Sprint Backlog
Encabezados en la fila 4, datos desde la fila 5.

| Col. | Contenido |
|---|---|
| A | Sprint — **ARRAYFORMULA: no escribir** |
| B | Épica — **ARRAYFORMULA: no escribir** |
| C | HU |
| D | ID tarea (US-xx-Tnn / HT-xx-Tnn) |
| E | Tarea |
| F | Descripción |
| G | Responsable (nombre completo) |
| H | Estado: To Do / Doing / Done |
| I | Fecha inicio (dd/mm/aaaa) |
| J | Fecha fin (dd/mm/aaaa) |

**Nunca borrar la fila 5**: ahí viven las ARRAYFORMULA de A y B.

### Tablero
Se calcula solo con FILTER sobre el Sprint Backlog. **No se edita.**

### Daily meetings
Datos desde la fila 5. Ya hay una fila por cada día hábil con la fecha y el sprint puestos:
se completa la fila del día, no se agrega otra.

| Col. | Contenido |
|---|---|
| A | Fecha |
| B | Sprint |
| C | Hora |
| D | Asistentes |
| E | ¿Qué hice ayer? |
| F | ¿Qué haré hoy? |
| G | Impedimentos |
| H | Evidencia (enlace a la foto) |

### Bitácora S1 y Bitácora S2
Encabezados en la fila 8, datos desde la fila 9.

| Col. | Contenido |
|---|---|
| B | Día del proyecto |
| C | Fecha |
| D | Colaborador |
| E | Rol |
| F | Hora |
| G | Descripción de la actividad |
| H | Tarea (ID del Sprint Backlog) |

Antes de escribir B y E, revisar si son fórmulas. Si lo son, no se pisan.
(Revisado el 28/10/2026 en Bitácora S1: B9 y E9 tienen ARRAYFORMULA; B sale de la fecha y
E del colaborador. Solo se escriben C, D, F, G y H.)

### Incidencias S1 e Incidencias S2
Encabezados en la fila 8, datos desde la fila 9.

| Col. | Contenido |
|---|---|
| B | Fecha |
| C | Incidencia |
| D | Persona relacionada |
| E | Rol |
| F | Artefacto (HU / tarea) |
| G | Solución |
| H | Referencia |

(Revisado el 28/10/2026 en Incidencias S1: E9 tiene ARRAYFORMULA; el rol sale de la persona.)

### Listas
Catálogos de colaboradores, roles y estados que usan los desplegables.

## Identificadores

- La hoja usa el **backlog v3**: US-02 a US-64. US-01 se retiró: duplicaba US-06 y US-54, y
  su trabajo es la historia técnica HT-07.
- El código y `rikos-backend/doc/historias/` usan los IDs del repo (US-0xx, tres dígitos).
- La equivalencia está en `docs/producto/prd/04-traduccion-de-ids-v3.md`. Se usa **siempre**
  para cruzar el código con la hoja. Ojo con los dígitos: `US-01` ≠ `US-001`.

## Reglas del seguimiento

1. **No se inventa nada.** La bitácora, las dailies y las incidencias solo registran hechos
   reales: commits, pruebas que se corrieron o lo que el integrante diga. Si hay duda, se pregunta.
2. Antes de escribir en la hoja, se muestra una tabla con los cambios
   (pestaña, fila, columna, valor actual → valor nuevo) y se espera el OK.
3. Nunca se borran filas ni se sobrescriben fórmulas. Se lee el rango antes de escribir.
   Las fechas van como dd/mm/aaaa en modo USER_ENTERED, y se comprueba que queden como fecha.
4. Una historia pasa a «Terminada» solo si cumple la **Definición de Hecho**: todas sus tareas
   en Done, criterios de aceptación probados, historia documentada en
   `rikos-backend/doc/historias/` e integrada a `develop`. Si el código está hecho pero falta
   probar o cerrar algo, queda «En revisión».
5. Si no hay acceso a Google Sheets, se dice y se entregan los cambios en una tabla para
   pegarlos a mano.

## Flujo de cada sesión

- **Al empezar:** preguntar si la tarea es de la app o de la web. Cambiarse a la rama del
  integrante para esa área (`app/<nombre>` o `web/<nombre>`) y traer `develop`.
- **Si es de la web:**
  - Al empezar: preguntar qué tarea se va a hacer (ID). Proponer pasarla a Doing con fecha de
    inicio y agregar una fila en la bitácora.
  - Al terminar: proponer pasarla a Done con fecha de fin y agregar una fila en la bitácora.
    Si hubo un problema, proponer también una fila en incidencias. Si todas las tareas de la
    historia quedaron en Done, proponer el nuevo estado de la historia (regla 4).
  - Al cerrar el día: ofrecer un borrador del «¿Qué hice ayer?» de la daily siguiente, armado
    con la bitácora. El integrante lo confirma y agrega el enlace a la foto.
- **Si es de la app:** no se toca la hoja. Se indica qué historias del backlog cubre esa
  pantalla, según `rikos-app/docs/03-pantallas-vs-backlog.md`.
