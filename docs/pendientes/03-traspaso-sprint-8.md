# Traspaso al sprint 8 (circuito del pensionista)

**Fecha:** 2026-10-11. **De:** la sesión que construyó los sprints 4 a 7. **Para:** quien haga el sprint 8 y siguientes. El sprint 7 está cerrado y subido a `main`.

## Qué es el sprint 8
El circuito del pensionista de punta a punta (caso crítico 1 de la especificación): **kiosco → pre-ticket → mozo → caja → cargo a su cuenta**. Historias: US-019, US-020, US-021 y US-024
(en el backlog v3: US-23, US-24, US-25 y US-26). Criterios en `docs/producto/flujos/04-ep04-pensionistas.md`; reglas RN-024 a RN-027, RN-030, RN-031, RN-034, RN-091 y RN-094.
Modelo de datos ya escrito (sin migración todavía): `pre_ticket` (§7.10) y `abono` (§7.11) en `docs/producto/modelo-de-datos/05-pensionistas-planes-cuenta-corriente--ep04.md`.

## Orden sugerido para empezar
1. **Leer** `ESTADO.md`, `docs/METODO-spec-driven.md`, `docs/producto/plan/sprint-07-cobrar-y-cerrar.md` (es el ejemplo más completo de especificación de sprint) y la guía `docs/RIKOS-guia-del-equipo.pdf`.
2. **Especificar con el dueño antes de programar:** una decisión por vez, con opciones y una recomendación, y escribir `docs/producto/plan/sprint-08-...md`. No se programa hasta que lo apruebe.
   Decisiones que probablemente haya que tomar: qué dispositivo e impresora usa el kiosco, los horarios exactos de cada servicio, cuánto dura un pre-ticket, qué pasa con un pre-ticket vencido o anulado,
   si el abono en efectivo lo registra solo caja, y cómo se aprueba un abono digital.
3. **Partir en entregas pequeñas y verificables** (como las 5 del sprint 7) y escribir cada prueba antes o junto con el código.
4. **Documentar al cerrar cada entrega:** una historia en `rikos-backend/doc/historias/`, las decisiones técnicas como DT-069 en adelante en `RIKOS-decisiones-tecnicas.md`, y actualizar `ESTADO.md` y `docs/producto/plan/README.md`.

## Lo que ya existe y se reutiliza
- **Caja:** `movimiento_caja.tipo` acepta `abono_pensionista` (V41); `CobrarPedido`, `CerrarTurno` y `ConsultaCierre` ya suman los movimientos del turno por medio, así que un abono en efectivo entra solo al efectivo esperado.
  Los cobros reabiertos se excluyen por `cobro.revertido_en`: un abono necesita su propia regla de reversión.
- **Cuenta corriente:** libro de solo agregado (`LibroDeCuenta`, `RegistrarConsumo`, `RegistrarAjuste`, `CargarSaldoInicial`); idempotente por id. El abono es un movimiento de tipo `abono` con signo crédito.
- **Cargo al pensionista al cobrar:** ya funciona (`cobro_cargo`, avisa si supera el tope sin bloquear).
- **Pensionista en la mesa:** hoy se busca por DNI o nombre (`DialogoPensionista`); el pre-ticket lo reemplaza (el diálogo queda como respaldo si el kiosco falla). Hay un pendiente anotado en `docs/pendientes/README.md`.
- **PIN del pensionista:** se genera y se muestra una vez (sprint 5); el bloqueo por intentos fallidos existe para el PIN del mozo (`LimitadorIntentosPin`) y se puede tomar como patrón para RN-091.
- **Convenciones:** ULID de 26 caracteres, `Resultado<T, E>` para errores esperados, montos en céntimos, Flyway nunca se edita (la siguiente migración es **V46**), un test de acceso por rol por cada endpoint nuevo,
  eventos de bitácora nuevos requieren su fila en `auditoria_evento_check` y su frase en `ListarAuditoria`.

## Cosas que muerden (aprendidas)
- **`ArquitecturaDeModulosTest` falla por el ciclo `comun` ↔ `identidad`** desde antes: es el único error de la suite (689 pruebas). No es del sprint 8; está en `ESTADO.md` como pendiente.
- **Fines de línea:** hay archivos con CRLF y otros con LF. Un script que edita por texto puede no encontrar su cadena; hay que normalizar `\n`/`\r\n`.
- **La suite tarda varios minutos** y recompila `target/`: si hay un backend de desarrollo corriendo desde ahí, puede romperse con `NoClassDefFoundError`; hay que reiniciarlo después.
- **Docker/Postgres se cae seguido (WSL2).** Primero `docker ps`; si falta, `docker compose up -d` desde `rikos-backend/`.
- **Una sola base de desarrollo, compartida:** no borrar pedidos ni turnos ajenos. Un cierre de caja no se puede borrar (trigger a propósito).
- **Dos sesiones de Claude trabajaron en paralelo** (una de diseño del POS, otra de backend): antes de tocar `PosShell`, `PantallaPedido`, `OrdenDeLaMesa` o `pos.css` conviene avisar a quien diseña el POS.

## Pendientes heredados (no son del sprint 8, pero los verá)
- **Auditoría integral:** `docs/pendientes/02-auditoria-integral-sprints-0-7-2026-10-11.md`. Abiertos del cobro: CAJ-02 (el snapshot no guarda el árbol de recetas), CAJ-04 (reintento tras respuesta perdida), CAJ-07 (el total no distingue cobrado de pendiente);
  y POS-07. PEN-01 (el libro acepta un id existente con otro contenido) **debe resolverse antes de la sincronización del sprint 10**; el sprint 8 agrega más movimientos al libro, así que conviene hacerlo antes o junto.
- **HT-10:** prueba de impresión con la comandera real (depende del hardware). El kiosco también imprime: conviene decidir una sola estrategia de impresión.
- **Datos reales del dueño:** precios del plan y del táper, pensionistas reales con su saldo inicial; los de desarrollo son inventados (el pensionista "Luis Alberto Quispe Rojas" y los planes de prueba).
- **Preguntas abiertas de caja:** tope de descuento por rol, motivos iniciales de descuento y de reapertura, y si un administrador puede cerrar el turno de otro cajero.
- **La pantalla de caja del POS es provisional**; si el diseño llega, se reemplaza sin cambiar la API.
- **Backlog v3:** los IDs de dos dígitos (US-23) no son los de tres (US-019) que usa el código; la tabla está en `docs/producto/prd/04-traduccion-de-ids-v3.md`.

## Cómo levantar el sistema
`docs/EMPEZAR.md`. En corto: `docker compose up -d` en `rikos-backend/`, luego `../mvnw spring-boot:run` **desde la carpeta de cada módulo** (`servicio-operacion` y `gateway`), y `npm run dev` en `rikos-web`.
El POS está en `/pos` (PIN de desarrollo en `EMPEZAR.md`) y la administración en `/admin`.
