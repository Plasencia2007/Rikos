# Estado del proyecto

Se actualiza al cerrar cada sesión. Ver `docs/METODO-spec-driven.md` para
el método y `CLAUDE.md` para el resto del contexto del proyecto.

## En qué paso vamos

Usando el método spec-driven desde el 2026-09-28, aplicado también en
forma **retroactiva** a lo ya construido (sprints 0-3). El código va
adelantado a la especificación: sprints 0-3 ya están construidos
(identidad/permisos, catálogo, auditoría transversal, motivos).

**Pasos 4 (TRD) y 5 (modelo de datos) ya reorganizados** (2026-09-29):
el modelo de datos se partió en 15 archivos por bloque/fase
(`docs/producto/modelo-de-datos/`), y el TRD se escribió de verdad a
partir de la arquitectura que ya estaba bien descrita en `README.md`
(`docs/producto/trd/`). El paso 6 (plan) tiene la tabla de estado real
por sprint (`docs/producto/plan/`).

**Pasos 1, 2, 3 y 6 con base real** (2026-09-29): el Product Backlog
v2.0 (3.268 líneas) se repartió por paso -- PRD (visión, alcance,
épicas), flujos (72 historias, una épica por archivo), UI/UX (mapa de
pantallas y reglas de diseño), plan (15 sprints con su porqué) y
pendientes (preguntas de refinamiento). Cada carpeta tiene su README con
lo que todavía le falta para cumplir el método.

**El método se aplica sprint por sprint**, no puliendo el PRD en
abstracto: el backlog ya es la idea del sistema. Sprint 3 cerrado
(2026-09-29, HT-08 movido al sprint 10).

**Sprint 4 cerrado (2026-09-29)**, ver
`docs/producto/plan/sprint-04-costos-y-menu.md` y los cuatro documentos
en `rikos-backend/doc/historias/` (US-010, US-014, US-013, US-015):
recetas con costo en cascada (DT-054), menú normal y extra, programación
semanal con estados calculados por fecha, agotados por local. Migraciones
V20 a V24. **214 pruebas en verde** (verificado contra el código fuente).
Se corrigió de paso un bug del sprint 3: cambiar de local daba 500.

**Sprint 5 cerrado (2026-09-29)**: especificado en
`docs/producto/plan/sprint-05-cuenta-del-pensionista.md`. **Las 4 entregas
hechas y verificadas**: planes tarifarios con versiones, parámetros de cobro,
motor de precios con su tarjeta "Probar un precio", la ficha del pensionista
con su responsable (DNI único, PIN generado y mostrado una vez, cuenta abierta,
desactivar y reactivar) y la cuenta corriente (libro de solo agregado, saldo
inicial, ajustes con motivo, tope calculado o fijado que solo recuerda, nunca
bloquea; ver `US-018-cuenta-corriente.md`). Módulo `pensionistas`,
migraciones V26 a V28. **325 pruebas en verde** (suma de surefire, sin
`ArquitecturaDeModulosTest`). La cuenta de prueba quedó con un saldo inicial
y un ajuste inventados que el libro no permite borrar. Los precios de desarrollo (plan y táper) y el
pensionista "Luis Alberto Quispe Rojas" son **inventados**: reemplazar por los
reales del dueño antes de cobrar a alguien de verdad.

**Sprint 6 casi cerrado (2026-09-30)**: las 4 entregas están hechas y verificadas en el navegador (mesas y mapa, pedido, comanda y anulaciones, reorganizar cuentas; ver `US-027` a `US-031` en `rikos-backend/doc/historias/`). La suite tiene 556 pruebas y solo falla el ciclo conocido de Modulith; la revisión independiente de las entregas 3 y 4 y su pase de arreglos están hechos (ver `US-029-reorganizar-cuentas.md`). **Falta**: HT-10 (prueba con la impresora real) y cerrar el sprint. Migraciones V29 a V37 (V34 es del rediseño de salones, V35 de comensales y nombre del cliente, V37 de la versión de las líneas). Además, el **sprint 6b** (`docs/producto/plan/sprint-06b-lista-de-pedidos.md`): `GET /pedidos`, `GET /pedidos/resumen` y `mozoNombre`/`abiertoEn`, para el rediseño del POS que hace otra sesión (`sprint-06b-pos-rediseno.md`). Y la adenda **"Terminar pedido"** (2026-10-01, V38): un cierre sin cobro que deja el pedido `cerrado` y la mesa libre; el sprint 7 lo completa con el cobro. La pantalla del pedido se rediseñó por completo (2026-10-01, ver `US-028-tomar-el-pedido-pantalla.md`): catálogo con pestañas Todo/Entrada/Segundo/Extra (el backend ya devuelve `grupo` y `categoria`, §8 de `sprint-06b-lista-de-pedidos.md`) y "Orden de la mesa" con todas las líneas; el traslado de varias líneas a la vez ya no está en pantalla, solo línea por línea (el backend sí lo admite). La suite tiene 600 pruebas y solo falla el ciclo conocido de Modulith. Decisiones del sprint registradas como **DT-055 a DT-061** en `RIKOS-decisiones-tecnicas.md` (estado de mesa derivado, bloqueo optimista, comanda como foto, precio del servidor, "Terminar pedido" provisional, grupo de la carta por categoría raíz, recorte del User-Agent); el TRD menciona la concurrencia. Se arregló el bug del PIN con User-Agent largo (daba 500 y no contaba el intento) y se agregó la prueba de agotados (US-04 CA2). **Siguen abiertos:** HT-10 (impresora real), el campo "tipo de entrega" de la comanda y los 56 px de US-01. **Sprint 7 (cobrar y cerrar) especificado el 2026-10-05**, pendiente de que el usuario apruebe el documento: `docs/producto/plan/sprint-07-cobrar-y-cerrar.md` (varias cajas por local con una sembrada, cobro por líneas con pago dividido y cargo al pensionista, descuentos por línea, cierre con conciliación, reapertura dentro del turno; compensación al sprint 11, inventario al 9). **Entrega 1 (cajas y turnos) hecha el 2026-10-05** (V40, módulo `caja`, `US-032-cajas-y-turnos.md`): API de cajas y apertura de turno, con un turno abierto por caja y por cajero garantizado por la base de datos; sin pantalla todavía. Siguen la entrega 2 (cobro) y las demás.
**Backlog v3 recibido (2026-10-04)**: 5 épicas, 64 historias (US-01..US-64), mismas 94 reglas; copia en `docs/producto/prd/03-backlog-v3.md` y tabla de IDs en `04-traduccion-de-ids-v3.md`. El código sigue con los IDs de tres dígitos. La comparación contra lo construido está en `docs/producto/plan/04-comparacion-backlog-v3.md`.
Pendiente con el dueño: los precios reales
del plan y del táper, y los datos reales de los pensionistas con su saldo
inicial.

## Decisiones tomadas, con su razón

- **Método spec-driven adoptado para todo lo nuevo Y retroactivo para lo
  ya construido.** El usuario lo pidió explícito.
- **`doc/historias/` sigue existiendo en paralelo, no se reemplaza.** El
  método spec-driven cubre el "qué se va a construir" (antes de
  programar); historias cubre el "qué se construyó de verdad" (después).
- **La estructura de carpetas se adaptó del ejemplo que trajo el
  usuario** (screenshot de otro proyecto), sin copiar carpetas
  específicas de ese otro proyecto.
- **El modelo de datos se partió preservando la numeración de sección
  original** (`3.11`, `13.1`, etc.) -- el código y los historia docs
  citan esos números, no un archivo. `rikos-backend/doc/RIKOS-modelo-de-datos.md`
  quedó como archivo de redirección (no se borró, para no romper los ~9
  enlaces existentes) en vez de duplicar el contenido.
- **`RIKOS-decisiones-tecnicas.md` NO se migró al TRD.** Es una bitácora
  cronológica viva (DT-001 en adelante) citada por número en código; el
  TRD nuevo es el resumen organizado por tema para alguien que recién
  llega. Dos documentos con roles distintos, no uno reemplazando al otro.
- **La tabla de estado de `README.md` estaba desactualizada** (decía
  sprint 2 "en curso" y sprint 3 "pendiente", ambos ya completos) --
  corregida como parte de esta reorganización, porque el método pide que
  los documentos sean vivos.

## Pendientes abiertos (ver también `docs/pendientes/`)

- Completar PRD (usuario de referencia, diferenciador, riesgos), flujos
  (procesos de punta a punta, vocabulario y estados) y UI/UX (design
  system documentado, wireframes) -- ver el README de cada carpeta.
- EP-09 mal asignado en el texto del bloque 9 del modelo de datos.
- Ciclo de módulos `comun ↔ identidad` (Spring Modulith,
  `ArquitecturaDeModulosTest`) -- reportado, no resuelto.
- Proceso programado de limpieza de `bloqueo_pin` -- no implementado.
- `EliminarProveedor` sin chequeo de historial -- intencional hasta que
  exista la tabla de compras (sprint 9).
- Plan de fases detallado para sprints 4+ (hoy solo hay la tabla de
  estado, no entregas verificables planeadas).

## Estructura del repositorio (resumen)

```
RIKOS/
├── CLAUDE.md                      -- mapa del proyecto + convenciones
├── ESTADO.md                      -- este archivo
├── docs/
│   ├── fuente/especificacion-v0.1/ -- la especificación del dueño, fija, no se edita
│   ├── METODO-spec-driven.md
│   ├── EMPEZAR.md                 -- guía de arranque del entorno de desarrollo
│   ├── producto/                  -- los 7 pasos del método
│   │   ├── prd/                   -- base desde el backlog: visión, alcance, épicas
│   │   ├── flujos/                -- 72 historias, una épica por archivo + reglas de negocio
│   │   ├── ui-ux/                 -- mapa de pantallas + reglas de diseño
│   │   ├── trd/                   -- arquitectura + stack/despliegue
│   │   ├── modelo-de-datos/       -- 15 archivos por bloque/fase
│   │   └── plan/                  -- 15 sprints con su porqué + estado real
│   ├── auditorias/ aprendizajes/ pendientes/ designs/
│   └── superpowers/plans/         -- planes tácticos por sprint (formato anterior a este método)
├── rikos-backend/
│   └── doc/
│       ├── RIKOS-modelo-de-datos.md         -- redirección a docs/producto/modelo-de-datos/
│       ├── RIKOS-decisiones-tecnicas.md     -- bitácora de decisiones (DT-001, DT-002...), viva, no migrada
│       └── historias/                        -- qué se construyó y cómo se verificó, un archivo por historia/sprint
└── rikos-web/                     -- React 19 + TypeScript + Vite
```
