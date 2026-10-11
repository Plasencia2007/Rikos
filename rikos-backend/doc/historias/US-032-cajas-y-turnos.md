# US-032 — Cajas y turnos (sprint 7, entrega 1)

**Especificado en:** `docs/producto/plan/sprint-07-cobrar-y-cerrar.md`. En el backlog v3 es US-18. Hecho el 2026-10-05.

## Qué se pidió
Abrir el turno de caja de un cajero con su fondo inicial; que haya varias cajas por local sin que su dinero se cruce; y que un turno no se pueda abrir dos veces.

## Qué se construyó
- **Migración V40:** tablas `caja` (nombre único por local, sin distinguir mayúsculas) y `turno_caja` (fondo en céntimos, estado abierto/cerrado, `cerrado_en` solo si está cerrado). Dos **índices únicos parciales**: un solo turno abierto por caja y un solo turno abierto por cajero, así que dos aperturas simultáneas no pasan ni por una carrera. Eventos de bitácora `turno_abierto` y `caja_creada`.
- **Módulo `caja`** (`caja/{api,aplicacion,infraestructura}`): `AbrirTurno`, `CrearCaja`, `ConsultaCajas` (cajas con su turno abierto y el nombre del cajero; turno actual de un cajero) y `CajaPorDefecto`.
- **"Caja 1" no se siembra en SQL:** se crea sola la primera vez que un local pide sus cajas (igual que el salón "Principal"), así los locales nuevos también la tienen.
- **API:** `GET /cajas` (caja.ver), `POST /cajas` (caja.administrar), `POST /cajas/{id}/turnos` con `{fondoInicial}` en céntimos (caja.crear), `GET /turnos/actual` (caja.ver; 204 si no hay). Códigos de error: `CAJA_NO_ENCONTRADA` (404), `CAJA_CON_TURNO_ABIERTO`, `CAJERO_CON_TURNO_ABIERTO`, `CAJA_INACTIVA`, `NOMBRE_REPETIDO` (409), `FONDO_INVALIDO`, `NOMBRE_INVALIDO`, `SIN_LOCAL_ACTIVO` (422). Un choque en los índices únicos se traduce al mismo 409, no a un 500.
- **Permiso nuevo `caja.administrar`** (crear cajas): administrador de local y central. Cajero y administradores abren turno con `caja.crear`; el mozo, cocina y motorizado no ven ni abren cajas (el mozo no cobra).
- **Bitácora:** frases para `turno_abierto` y `caja_creada` en `ListarAuditoria`.

## Decisiones tomadas al ejecutar
- **Un cajero no tiene dos turnos abiertos a la vez**, aunque haya varias cajas: así "su caja" está siempre definida cuando cobre (entrega 2) y su dinero no se cruza. No estaba escrito en las historias; se agregó a la especificación del sprint.
- El fondo puede ser cero; no puede ser negativo ni nulo.
- Una caja de otro local es 404 (no se filtra que existe).

## Cómo se verificó
- `AbrirTurnoTest` (10 pruebas): caja sembrada una sola vez, apertura con auditoría, fondo, un turno por caja, un turno por cajero, cajas independientes, caja ajena, caja inactiva, nombres repetidos, sin local activo.
- `CajasYTurnosAccesoPorRolTest` (6): acceso por rol con los roles reales, 204 sin turno, 409 en las dos colisiones, 422 y 404, sin sesión.
- Pruebas de permisos y bitácora existentes siguen en verde. `ArquitecturaDeModulosTest` solo falla por el ciclo conocido `comun` ↔ `identidad` (el módulo `caja` no agrega ciclos).
- Sin pantalla todavía: la pantalla de caja la diseña la otra sesión antes de la entrega 2; esta entrega es API.

## Qué queda para las siguientes entregas
Cobrar (entrega 2), descuentos y anulación (3), cierre del turno (4: aquí el turno pasa a `cerrado` y se crea `cierre_caja`), reapertura (5).
