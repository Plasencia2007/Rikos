# US-018 — La cuenta corriente del pensionista

Sprint 5, entrega 4 de 4 (2026-09-29), cierra el sprint. Especificación en
`docs/producto/plan/sprint-05-cuenta-del-pensionista.md` (secciones 3.7 a 3.10).

## Qué se pidió

Una sola cuenta por pensionista, válida en todos los locales, con lo que debe o
tiene a favor; que algunos lleguen ya con deuda o saldo a favor; un **tope de
crédito calculado** (solo días en que el restaurante atiende × comidas que toma
× precio de su plan) que también se puede fijar a mano y que es **solo un
recordatorio para cobrar, nunca un bloqueo**; y que consumir, abonar y consultar
funcionen sin internet (la sincronización es del sprint 10).

## Qué se construyó

**Base de datos — `V28__libro_de_cuenta.sql`.** `movimiento_cuenta`: un libro
**solo de agregado**. No hay columna de saldo: el saldo es la suma de créditos
menos la de cargos. El `id` (ULID) lo puede traer el dispositivo, así que
reenviar no duplica. Guarda `ocurrido_en` (cuándo pasó) y `registrado_en`
(cuándo llegó): tras un corte de internet pueden diferir en horas. `local_id` es
obligatorio: cada movimiento dice dónde ocurrió. Un disparador rechaza `UPDATE`
y `DELETE`; restricciones de fila: un ajuste lleva motivo, un consumo es cargo,
un abono es crédito; un solo `saldo_inicial` por cuenta. También: motivo tipo
`ajuste_cuenta` (con "Corrección de cobro" sembrado) y los eventos de bitácora
`movimiento_cuenta` y `cambio_limite`.

**Backend (`pensionistas`).**

- `LibroDeCuenta`: única puerta de escritura; idempotente por id.
- `CargarSaldoInicial` (una vez, fecha de corte no futura), `RegistrarConsumo`
  (siempre cargo, con local; un inactivo no consume; **el tope no lo impide**),
  `RegistrarAjuste` (motivo activo del tipo `ajuste_cuenta`, observación si el
  motivo la pide; vale también para inactivos), `FijarTopeDeCredito` (fijar o
  volver al cálculo).
- `ConsultaCuenta`: `resumen` (saldo, deuda, saldo a favor, tope, crédito
  disponible, `superaTope`) y `movimientos` (por `ocurrido_en`, no por llegada).
- `CalculadoraDeTope` + `ServiciosDeAtencion`: lee los horarios de servicio y
  días especiales que cada local ya cargó (cerrado = nada; un local inactivo no
  cuenta). Cuenta desde el inicio del mes o el día del alta, con el precio del
  plan **de cada día** (respeta las versiones por vigencia). Si falta un precio
  o no hay días de atención, dice "sin calcular" con su aviso en vez de mostrar
  un tope que subestima.
- `ConsultaMotivos` (`catalogo.motivos`, interfaz expuesta) para que
  `pensionistas` valide motivos sin acceder a las tablas de `catalogo`; y
  `comun.tiempo` se expuso con `@NamedInterface` (la zona horaria del
  restaurante).
- `CuentaController`: `GET /pensionistas/{id}/cuenta` (`pensionistas.ver`),
  `POST …/cuenta/saldo-inicial` (`pensionistas.administrar`, solo central),
  `POST …/cuenta/ajustes` y `PUT …/tope` (`pensionistas.modificar`). El local es
  siempre el de la sesión; nunca viaja en la petición.
- **No hay endpoint de consumo ni de abono**: el consumo lo carga la caja
  (sprint 7) y el abono es US-021.
- Bitácora en español: "admin cargó el saldo inicial de "X": debe S/ 50.00",
  "admin ajustó la cuenta de "X": crédito de S/ 3.00 (motivo)", "admin fijó el
  tope de crédito de "X" en S/ 100.00" / "dejó que se calcule el tope".

**Frontend.** `PantallaCuentaPensionista` en `/admin/pensionistas/:id/cuenta`
(enlace "Ver cuenta" desde la lista y desde la ficha): debe / saldo a favor /
tope (calculado o fijado, con sus avisos) / crédito disponible, lista de
movimientos con local y quién registró, filtro por local, y diálogos de saldo
inicial, ajuste y tope. El recordatorio dice "Debe S/ X y superó su tope de
S/ Y: recuérdale el pago. No se le impide consumir." — nunca "excedido" ni
"bloqueado". Sin permiso, solo lectura. El tipo de motivo `ajuste_cuenta` se
agregó a `CrearMotivo` y a la pantalla de Motivos; los dos eventos nuevos, al
filtro de la Bitácora.

## La lista de cuentas corrientes (añadida al cierre)

El menú "Cuentas corrientes" no llevaba a ninguna página. Se agregó
`GET /pensionistas/cuentas` (`pensionistas.ver`) y la pantalla
`/admin/pensionistas/cuentas`: todos los pensionistas (activos e inactivos) con
su deuda, saldo a favor y tope, la mayor deuda primero; totales arriba (lo que
deben, lo que tienen a favor, cuántos pasaron su tope); filtros Todos / Con
deuda / Pasaron su tope / Con saldo a favor y buscador por nombre o DNI; clic en
una fila abre su cuenta. Los saldos salen de **una sola consulta agrupada** y los
días de atención se leen **una sola vez** para toda la lista (no una por
pensionista). Pruebas: `CuentasCorrientesTest` (5) y una en
`CuentaAccesoPorRolTest`. Siguen sin pantalla "Abonos" (US-021) y "Estados de
cuenta" (US-025).

## Qué se dejó fuera y por qué

- **Abonos (US-021) y consumo desde la caja (sprint 7):** el libro ya los
  soporta (tipos `abono`, `consumo`), pero sus pantallas son de otras historias.
- **Cierre mensual y arrastre (sprint 13):** el tipo `arrastre` existe en el
  CHECK, pero no hay `periodo_cuenta` ni `periodo_id` todavía (ver
  `docs/pendientes/README.md`).
- **Sincronización sin internet (sprint 10):** se dejó lista la base (id del
  cliente, idempotencia, dos fechas, local por movimiento); falta el cliente que
  guarda y reenvía.

## Cómo se verificó

- **Tests: 325 en verde** (suma de surefire, sin `ArquitecturaDeModulosTest`).
  Nuevos: `LibroDeCuentaTest` (19: saldo inicial, saldo a favor aplicado primero,
  varios consumos del día, inactivo, el tope no bloquea, ajustes y motivos,
  importes, idempotencia, dos locales en cualquier orden, orden por
  `ocurrido_en`, el repositorio sin borrado y el disparador de la base),
  `TopeDeCreditoTest` (12), `ServiciosDeAtencionTest` (4),
  `CuentaAccesoPorRolTest` (5: central todo; admin de local todo salvo saldo
  inicial; cajero y mozo solo ven; cocina y motorizado, 403) y una prueba más en
  `ListarAuditoriaTest`.
- **Navegador:** cargar saldo inicial de S/ 50 (aparece "Debe S/ 50.00 y superó
  su tope de S/ 17.00", crédito disponible negativo), ajuste de S/ 3 a favor,
  fijar el tope en S/ 100 y volver al cálculo. El tope calculado (S/ 17 = S/ 8
  del 29 + S/ 9 del 30) usa el precio programado por día.
- `ArquitecturaDeModulosTest` solo falla por el ciclo `comun ↔ identidad` ya
  conocido; la violación propia (`ConfiguracionReloj` no expuesta) se corrigió.
- `npx tsc -b --force` limpio. `npm run lint` tiene un error en
  `features/locales/HorarioSemanal.tsx` (`_clave` sin usar), ajeno a este
  trabajo.

## Bugs y tropiezos

- Los heredocs largos del shell fallaron varias veces; los archivos se
  escribieron de a uno.
- Python no está instalado en esta máquina: las ediciones de archivos van con
  `sed` o con la herramienta de edición.
- Al reiniciar el backend el proceso anterior seguía ocupando el puerto 8081
  (detener la tarea no mata el `java` hijo).

## Ojo con los datos de desarrollo

El libro **no se puede borrar** (ni siquiera a mano: lo bloquea el disparador),
así que el saldo inicial de S/ 50 y el ajuste de S/ 3 cargados al pensionista de
prueba "Luis Alberto Quispe Rojas" quedan en la base de desarrollo. Son datos
inventados, igual que su plan y el precio del táper.
