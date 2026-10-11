# Usuario de referencia

## El negocio hoy

- **Un solo local.** Va a haber más, pero hoy es uno.
- **Unos 25 pensionistas activos**, y creciendo.
- **El dueño cobra.** Hoy es a la vez cajero, administrador y quien
  aprueba todo.

## Cuando el sistema esté en uso

**Un cajero fijo por local.** El dueño deja la caja y pasa a controlar
desde el panel.

## El usuario de referencia: el cajero del local a la hora del almuerzo

Es el momento en que se junta todo lo que el sistema tiene que resolver:
cola de pensionistas, mesas mixtas con clientes regulares, cobro dividido
entre efectivo y Yape, cargos a cuenta corriente y, si se cae Internet,
es el primero en enterarse. Si el sistema le sirve a las 13:00, le sirve
a todos los demás.

**Por qué el cajero y no el dueño:** el dueño paga y mira reportes, pero
el que decide si el sistema se usa o se vuelve al cuaderno es quien lo
tiene en la mano en hora punta.

## El segundo usuario: el dueño que deja de estar en la caja

La transición que viene es el dueño **delegando** la caja por primera
vez. Eso explica por qué el control administrativo no es un extra:
auditoría de descuentos y anulaciones, conciliación de caja con alerta
desde S/ 0.01, aprobación de abonos. Hoy el dueño no necesita nada de
eso porque cobra él. Mañana es lo único que le permite confiar en que la
caja cuadra sin estar ahí.

## El cajero es un rol, no una aplicación

Es uno de los seis roles base (`modelo-de-datos/01-...` §3.4), con sus
responsabilidades propias: apertura y cierre de turno, cobro, abonos en
efectivo, conciliación, comprobantes, aceptar direcciones de delivery.
Pero **no tiene una superficie propia**: las cinco superficies son POS,
kiosco, portal, motorizado y panel (DT-021). Sus pantallas viven dentro
del POS -- POS-08 (cobro), POS-09 (descuento), POS-10 (apertura de
turno), POS-11 (cierre de turno), POS-12 (abono), POS-13 (consulta de
pensionista), POS-18 (reapertura) -- y el administrador ve lo mismo desde
arriba en ADM-02.

**La diferencia entre mozo y cajero es de permisos, no de pantalla.** El
mismo dispositivo POS muestra u oculta el módulo de caja según quién
entró con su PIN. El sistema nunca pregunta "¿es cajero?", pregunta
"¿puede cobrar?" (`modelo-de-datos/01-...` §3.5).

**El turno se ata a la persona, no al dispositivo.** `turno_caja` lleva
`cajero_id`, `caja_id` y `local_id`, y una caja tiene un solo turno
abierto a la vez. La caja es una entidad física del local.

Todo esto se construye en el sprint 7. Hoy la única pantalla del POS que
existe es POS-01, el bloqueo por PIN.

## Decisiones que salen de esto

- **Multilocal desde el inicio, aunque haya un solo local.** Regla 5 del
  método: el caso de un local es el caso general con una lista de un
  elemento. Todo registro transaccional ya lleva `local_id` (ver
  `trd/00-arquitectura.md`); no se construye un camino aparte para "solo
  hay uno".
- **Una persona con varios roles tiene que funcionar bien.** Durante la
  transición el dueño es administrador central, administrador de local y
  cajero a la vez. El modelo ya lo permite (usuario N—N rol); no hay que
  asumir que cada rol es una persona distinta.
- **El volumen no es un riesgo hoy.** 25 pensionistas y un local están
  muy por debajo del supuesto con el que se dimensionaron los índices
  (tres locales, 200 pedidos diarios por local, ver
  `modelo-de-datos/92-referencia-completa-y-trazabilidad.md` §26.2).
  RF-010 deja de bloquear algo en el corto plazo.

## Huecos abiertos

- **¿Quién cobra cuando el cajero fijo no está?** Se resuelve en parte
  con lo de arriba: cualquier usuario con permiso de caja puede cobrar
  desde el mismo POS, y como el turno se ata a la persona, el reemplazo
  abre su propio turno. Lo que queda abierto es **RF-021, pregunta para
  el dueño**: ¿cuántas cajas tiene el local y cómo se cortan los turnos
  respecto de los servicios? Con una sola caja, el cajero tiene que
  cerrar su turno antes de que otro abra el suyo -- ¿eso es aceptable a
  mitad del almuerzo, o hace falta una segunda caja?
- **El local real no tiene código todavía.** "San Isidro" / `sanisidro`
  es un dato de desarrollo, pero el código del local es parte del
  contrato del POS (DT-040, subdominio por local) y aparece como valor
  por defecto en el frontend (`VITE_LOCAL_CODIGO`). Hay que definir el
  código real antes del primer despliegue.
