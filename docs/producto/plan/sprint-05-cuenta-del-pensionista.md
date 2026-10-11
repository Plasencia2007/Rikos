# Sprint 5 — La cuenta del pensionista

**Historias (del backlog):** US-016 (ficha y responsable), US-017 (planes
tarifarios), US-018 (cuenta corriente). Flujos en
`flujos/04-ep04-pensionistas.md`; modelo en
`modelo-de-datos/05-pensionistas-planes-cuenta-corriente--ep04.md`.

**Estado:** ✅ cerrado el 2026-09-29 (las 4 entregas). El usuario pidió avanzar el 2026-09-29
("por ahora inventa los precios, avanza"): **entregas 1, 2 y 3 hechas** (ver
`rikos-backend/doc/historias/US-017-planes-tarifarios-y-parametros-de-cobro.md`);
y la cuenta corriente quedó en `US-018-cuenta-corriente.md`. Los precios de desarrollo son inventados.

**Cómo se llegó a esto:** brainstorming con el usuario el 2026-09-29,
una parte a la vez (ficha, planes y precios, cuenta y tope,
configuración y entregas). Cada decisión lleva su razón. Lo que dijo el
usuario va marcado como tal; lo que asumí yo, como supuesto.

## 1. Qué queremos lograr

Que el restaurante tenga a sus pensionistas en el sistema, con lo que
cada uno debe o tiene a favor, y que las reglas de cuánto se le cobra
estén escritas y probadas **antes** de que exista la caja. La caja (sprint
7) solo las va a usar.

**Éxito del sprint:** dar de alta a los ~25 pensionistas reales (algunos
con deuda, otros con saldo a favor), ver su cuenta con el tope de cada
uno, y correr una tabla de casos de precios que dé lo mismo que hace el
dueño a mano.

## 2. Contexto que dio el usuario

- Unos **25 pensionistas activos**, con tendencia a crecer.
- Casi todos comen **el mismo menú que todos**; el menú extra
  prácticamente no aparece. Lo que sí pasa: comer dos veces, pedir para
  llevar (táper) o agregar algo suelto.
- **Todos pagan lo mismo**: hoy basta un solo plan.
- Desayuno, almuerzo y cena tienen precio especial para el pensionista.
  En desayuno y cena **no elige**: recoge lo que hay ese día. En la cena,
  igual.
- Algunos ya tienen **deuda o saldo a favor** al empezar.
- El **tope** no es un monto fijo: se calcula por los días que atiende el
  restaurante y las comidas que consume. Es más un **recordatorio para
  pagar la deuda** que un límite.
- Un pensionista **come en cualquier local**.
- Al pensionista se le reconoce **por su ticket** (pre-ticket con DNI y
  PIN); sin ticket vinculado se le trata como cliente normal.
- Los locales pueden quedar **sin internet**; la cuenta tiene que
  aguantarlo.

## 3. Decisiones, con su razón

### 3.1 Un módulo propio `pensionistas`

Ficha, responsable, planes, cuenta, libro y configuración global van en
un módulo aparte, no dentro de `catalogo`.

**Por qué:** los pensionistas son lo que distingue a RIKO'S de un POS
común, y la caja del sprint 7 los llama para cobrar. Con módulo propio la
conexión es explícita y la prueba de arquitectura (Spring Modulith) la
vigila. Dentro de `catalogo` es más rápido hoy y caro de separar después.
Lee productos (`catalogo`) y locales (`identidad`); esa dependencia se
declara con `@NamedInterface`, no se asume.

### 3.2 La ficha (US-016)

**Se registra:** DNI (único en toda la empresa, RN-036), nombres,
apellidos, teléfono, correo; institución, código de estudiante, carrera,
ciclo (opcional); plan tarifario; **comidas habituales** (desayuno,
almuerzo, cena); frecuencia habitual de abono; estado activo/inactivo.

**Responsable obligatorio:** nombre completo, DNI, teléfono, correo,
parentesco y dirección. Sin responsable no se guarda.

**Comidas habituales** (decisión nueva): se usan para calcular el tope.
**No limitan lo que puede consumir**: si alguien que solo almuerza pide
una cena, se le cobra igual.

**Se descartó el "local habitual".** Lo propuse para elegir qué horario
usar en el tope; el usuario señaló que come en cualquier local. Ahora un
día cuenta si **al menos un local atiende ese día**.

**Credenciales:**

- **PIN del kiosco:** lo genera el sistema (4 dígitos) al crear la
  ficha, se muestra **una sola vez** para entregarlo y se guarda cifrado
  (RN-090). Si lo olvida, se genera otro. Lo usará el kiosco (sprint 8).
- **Contraseña del portal:** **no se pide todavía.** La especificación la
  exige en el alta, pero cómo se asigna, entrega y recupera no está
  definido (RF-027) y el portal es de un sprint posterior. **Es una
  desviación de la fuente, hecha a propósito**: la columna existe, nula,
  y se llena cuando se defina el portal.

**Al guardar** se abre su cuenta corriente, vacía.

**Desactivar** (RN-034): no genera tickets ni consumos, conserva cuenta,
historial y (a futuro) portal, y admite abonos posteriores. Reactivar
reutiliza la misma cuenta. **Adelantado del sprint 13** (US-026): en el
plan es de allá, pero una ficha que no se puede desactivar obliga a
borrar o a dejar activo a quien ya no viene. Se hace aquí lo mínimo (el
estado y su efecto en la cuenta); lo que dependa del portal o del kiosco
queda para su sprint.

**Permisos:** `pensionistas.crear`, `.modificar` y `.desactivar` (ya
existen): administrador central y de local. Mozo y cajero solo
`pensionistas.ver`.

### 3.3 Las fichas se crean y editan **solo con internet**

Consumir, abonar y consultar la cuenta funcionan sin conexión; **crear o
editar la ficha, no**.

**Por qué:** si dos locales sin internet dan de alta al mismo DNI, se
crean dos cuentas para la misma persona, y unirlas después es lo que el
DNI único existe para evitar. Aceptado por el usuario.

### 3.4 Planes tarifarios (US-017)

**Un solo plan hoy**, pero el sistema permite crear otros. Cada plan
tiene **tres precios fijos: desayuno, almuerzo y cena** (precio por
comida, no por producto).

**Desviación de la fuente, con su razón:** la especificación pide "precios
especiales por producto fijo de desayuno y cena"; el usuario explicó que
el pensionista no elige, recoge lo del día a un precio único. El precio
por producto no hace falta. Si algún día hay opciones distintas, todas
cuestan lo mismo y el mozo solo anota cuál tomó (para cocina e
inventario), igual que en el menú del almuerzo.

**Vigencia e historial (RN-021):** cambiar un precio no reescribe la
fila: crea una **versión nueva** con su fecha de inicio y cierra la
anterior (`vigencia_hasta`). Siempre se puede saber cuánto costaba el
almuerzo un día cualquiera.

**Alcance central:** mismo plan y precios en todos los locales. Solo el
administrador central los cambia; el de local y caja los ven.

### 3.5 El motor de precios

Una función del módulo `pensionistas`, sin pantalla propia, que recibe
qué se pide y a quién, y devuelve **el precio con su explicación**. La
caja la usa sin repetir ninguna regla.

**Entrada:** el producto (menú normal, menú extra, agregado), la comida
(desayuno, almuerzo, cena), si la persona **trae ticket de pensionista**,
si es **el segundo consumo de esa comida en el día** y si es **para
llevar, recojo o delivery**. El precio se decide **por persona**, no por
mesa: una mesa puede ser mixta.

| Caso | Precio |
|---|---|
| Menú del día, cliente normal (sin ticket) | Precio del "Menú normal" (US-014) |
| Menú del día, pensionista con ticket | Precio de su plan para esa comida |
| Menú extra, cualquiera | Precio completo del extra (RN-023) |
| Segundo consumo de la misma comida, pensionista | Precio del plan, o precio normal si la configuración global lo dice |
| Agregado (huevo frito, porción adicional) | Precio normal, sumado al menú. Sin tarifa de pensionista (resuelve RF-019) |
| Táper | Un precio para todos, por cada menú, en llevar, recojo y delivery |
| Consumo ya cobrado | Conserva el precio con que se cobró (RN-022) |

**Por qué se ata al ticket y no a la ficha:** un pensionista que llega
sin ticket es tratado como cliente normal (así lo describió el usuario).
Es el ticket lo que vincula la cuenta a la persona (`06` § Autoservicio).

**Sin precio configurado** (plan sin precio de esa comida, táper sin
monto) el motor **no inventa**: devuelve "sin precio" y la caja lo
muestra como aviso. Mismo criterio que el menú sin precio del sprint 4.

### 3.6 Configuración global

Una tabla de parámetros (nombre, valor, quién y cuándo lo cambió), con
dos por ahora:

- **Precio del táper:** nace **sin monto** (`07` lo deja pendiente).
  Mientras no haya monto, no se cobra táper y la pantalla avisa.
- **Regla del segundo consumo del día:** `precio_del_plan` (por defecto)
  o `precio_normal`.

Solo el administrador central los cambia; cada cambio queda en la
bitácora. Se editan en Configuración, junto a Motivos.

**Por qué una tabla de parámetros y no una constante en el código:** el
usuario pidió que fueran editables ("configuración global"). Es simple, y
después sirve para otros valores sin migración nueva.

### 3.7 La cuenta corriente (US-018) — un libro

**Una cuenta por pensionista, válida en todos los locales.** Todo es un
**libro de movimientos**: solo se agregan filas, nunca se editan ni se
borran (RN-015 a RN-019; el modelo §7.6).

**Cada movimiento:** tipo (`consumo`, `abono`, `ajuste`, `arrastre`,
más `saldo_inicial`), signo (cargo o crédito), importe positivo, **local
donde ocurrió**, cuándo ocurrió, cuándo llegó al servidor, quién lo
registró.

**Nuevo tipo `saldo_inicial`:** el modelo lo tenía solo como `arrastre`
de un cierre mensual. La carga de los 25 pensionistas con su deuda o
saldo a favor es distinta (no hay mes que cerrar), así que se distingue
para poder verla en el libro y en la bitácora. Solo el administrador
central lo registra, con fecha de corte.

**Se calcula al consultar, no se guarda:**

- **Saldo** = créditos − cargos. No hay columna de saldo.
- **Deuda:** el saldo a favor se aplica primero (RN-018); solo el
  remanente sube la deuda.
- **Crédito disponible** = tope − deuda (RN-016).

**Ajustes:** corrigen sin tocar el libro; el administrador agrega un
movimiento con **motivo obligatorio** (lista nueva de motivos de tipo
`ajuste_cuenta`, que carga el dueño; se siembra uno como en el sprint 4,
para que se pueda operar).

**Consumos:** solo al cerrar en caja (RN-015). **En este sprint no hay
consumos reales**: la cuenta se prueba con saldo inicial y ajustes. La
operación "registrar consumo" queda hecha y probada para que la caja la
llame.

**Multilocal:** una sola lista con los movimientos de todos los locales,
y el local de cada uno.

### 3.8 El tope de crédito: calculado, editable, y solo un recordatorio

**Fórmula (por defecto):** los días que atiende **algún local** desde
el inicio del mes (o desde el día en que empieza el pensionista) hasta
fin de mes × lo que consume al día a precio de plan. Ejemplo: 22 días ×
almuerzo S/ 8 = S/ 176; almuerzo y cena a S/ 8 → 22 × S/ 16.

**Editable a mano** cuando el responsable pide otro límite; un
indicador dice si el tope es calculado o fijado. Se **calcula al
consultar, no se guarda**: si cambia el precio del plan o el horario de
un local, el tope se actualiza solo.

**Es un recordatorio, no un freno** (RN-017; el usuario lo reforzó:
"más que todo para recordar pagar la deuda"). El texto dice "Debe S/ X y
superó su tope de S/ Y: recuérdale el pago", nunca "límite excedido".
**Ninguna pantalla ni regla lo bloquea.** Es una alerta que la caja
mostrará en el sprint 7.

**Qué días atiende un local:** sus horarios por día de la semana y sus
días especiales cerrados (sprint 1). El día cuenta si al menos un local
activo abre.

### 3.9 Aguantar la operación sin internet

La sincronización real es del sprint 10 (HT-04, HT-08). En este sprint
solo se garantiza que **ninguna decisión la haga imposible**:

- El libro solo agrega filas: dos locales no se pisan.
- No hay columna de saldo: al reconectarse se juntan los movimientos y
  ninguno se pierde.
- Cada movimiento nace con su identificador en el local (ULID): enviar el
  mismo dos veces no lo duplica.
- Dos horas por movimiento (`ocurrido_en`, `registrado_en`): el estado de
  cuenta usa la primera.
- El tope solo avisa: un saldo desactualizado en un local sin internet
  puede avisar de más o de menos, **pero nunca impide que coma**.

**Cómo se prueba:** movimientos de dos locales sobre la misma cuenta,
en cualquier orden, dan el mismo saldo; el mismo movimiento enviado dos
veces cuenta una sola vez.

### 3.10 Decisiones de la entrega 4 (2026-09-29)

Afinan 3.7 y 3.8 al programarlas; las tres primeras salen de mirar qué datos
ya existen.

- **El tope cuenta por comida, no por día.** Para cada día del mes y cada comida
  habitual del pensionista, se suma el precio del plan **solo si algún local
  activo sirve esa comida ese día** (según sus horarios por servicio y día de la
  semana, y sus días especiales: uno cerrado no cuenta). Es más fiel a lo que
  dijo el usuario ("los días que atiende el restaurante y las comidas que
  consumiría") que contar días enteros: si nadie sirve cena los domingos, un
  pensionista de almuerzo y cena no suma una cena el domingo. El precio es el
  del plan **de ese día**, así un cambio de plan a mitad de mes se refleja.
- **Desde cuándo:** desde el inicio del mes, o desde el día del alta si fue este
  mes. La ficha no tiene una "fecha de inicio" propia; la fecha del alta hace de
  proxy, y se puede fijar el tope a mano si no calza.
- **Nunca un tope falso:** si el plan no tiene precio de alguna comida contada,
  o ningún local tiene horarios, el tope queda **sin calcular** con su aviso, no
  con un número que subestime. Con un tope fijado a mano no hace falta nada de
  eso.
- **Sin `periodo_cuenta` todavía.** El modelo pide `periodo_id` en cada
  movimiento, pero los periodos son del cierre mensual (sprint 13). Se agrega
  entonces, con una migración que asigna los movimientos por mes de
  `ocurrido_en`; ahora sería una tabla vacía y una columna obligatoria sin uso.
- **El libro es inmutable en la base,** no solo en el código: un disparador
  rechaza UPDATE y DELETE sobre `movimiento_cuenta`.
- **Idempotente por id:** registrar dos veces el mismo movimiento (mismo ULID
  generado en origen) deja una sola fila. Es la garantía de la sincronización
  del sprint 10, probada aquí con dos locales.
- **Un solo saldo inicial por cuenta** (índice único parcial): cargarlo dos
  veces duplicaría la deuda. Un error se corrige con un ajuste con motivo.
- **Permisos:** el saldo inicial es del administrador central
  (`pensionistas.administrar`); los ajustes y fijar el tope los hace quien puede
  editar pensionistas (`pensionistas.modificar`: central y de local); ver la
  cuenta es `pensionistas.ver`.
- **Bitácora:** saldo inicial y ajustes (evento `movimiento_cuenta`) y el cambio
  de tope (`cambio_limite`), además de quedar en el libro con usuario y local.
- **Motivos de ajuste:** tipo nuevo `ajuste_cuenta`, con uno sembrado
  ("Corrección de cobro") para poder operar, como en el sprint 4.
- **Registrar un consumo** queda hecho y probado (`RegistrarConsumo`) para que la
  caja lo llame; **no tiene pantalla ni endpoint** porque sin caja no hay
  consumos reales (RN-015). Un pensionista inactivo no consume (RN-034).

## 4. Auditoría antes de programar

### 4.1 La numeración de reglas del modelo (bloque 5) estaba corrida

Mismo problema que en los bloques 3 y 4. Contra
`flujos/90-reglas-de-negocio-consolidadas.md`:

| El modelo decía | Es en realidad |
|---|---|
| RN-017 (crédito disponible) | RN-016 |
| RN-018 (límite preventivo) | RN-017 |
| RN-019 (saldo a favor primero) | RN-018 |
| RN-020 (varios consumos por día) | RN-019 |
| RN-025 (arrastre sin atraso) | RN-020 |
| RN-033 (vigencia e historial del plan) | RN-021 |
| RN-034 (consumos anteriores conservan tarifa) | RN-022 |
| RN-041 (extra sin tarifa reducida) | RN-023 |
| RN-030 (pre-ticket vence, sin cargo) | RN-026 y RN-027 |
| RN-035 (desactivar) | RN-034 |
| RN-021 (abono en efectivo) | RN-030 |
| RN-022 (abono digital pendiente) | RN-031 |
| RN-023 (alerta por número de operación repetido) | RN-032 |
| RN-024 (periodo cerrado inalterable) | sin regla propia en la lista v2.0 |
| RN-029 (notificaciones) | RN-029 (coincide) |

Corregido en el modelo al escribir esta especificación.
**RN-024 del modelo** (periodo cerrado inalterable) no tiene una regla
consolidada; sale de `01` § Reglas de historial. Queda como está, con esa
referencia.

### 4.2 Lo que no está en el modelo y este sprint agrega

- `pensionista.comidas_habituales` (y se descarta el local habitual, que
  no llegó a estar).
- `pensionista.limite_credito` pasa a **nulo = calculado**; con valor =
  fijado a mano.
- `plan_tarifario`: tres precios por comida (`precio_desayuno`,
  `precio_almuerzo`, `precio_cena`); `precio_plan` por producto deja de
  usarse. Se conserva `precio_almuerzo_normal` como `precio_almuerzo`.
- `movimiento_cuenta.tipo` gana `saldo_inicial`.
- `parametro_global` (nombre, valor, cambiado por, cuándo): tabla nueva.
- Tipo de motivo `ajuste_cuenta`: tabla `motivo` y su CHECK, con una
  migración nueva (no se edita V13).
- `pensionista.hash_contrasena_portal`: nulo por ahora (3.2).

### 4.3 Contraste con la fuente del dueño

`02` no dice a qué precio va el segundo consumo (RF-019 lo señala), ni el
del agregado, ni el del táper: son decisiones del usuario, marcadas arriba.
Las desviaciones de la fuente son dos y están justificadas: el precio por
comida en lugar de por producto (3.4) y la contraseña del portal
diferida (3.2).

## 5. Entregas verificables

1. **Configuración global y planes (US-017).** ✅ **Hecha (2026-09-29).** Tabla de parámetros,
   plan único con tres precios, versiones con vigencia e historial, y las
   pantallas de Planes y de Configuración. *Se demuestra:* subir el
   precio del almuerzo y ver la versión anterior cerrada con su fecha, y
   el táper "sin monto" avisando.
2. **Motor de precios.** ✅ **Hecha (2026-09-29)**, ver `rikos-backend/doc/historias/sprint5-motor-de-precios.md`. Las reglas de 3.5 como función probada.
   *Se demuestra:* una tabla de casos con su precio y su explicación.
3. **Ficha del pensionista (US-016).** ✅ **Hecha (2026-09-29)**, ver `rikos-backend/doc/historias/US-016-ficha-del-pensionista.md`. Alta con responsable, DNI único,
   PIN generado una vez, cuenta abierta; desactivar y reactivar; lista
   con buscador. *Se demuestra:* dar de alta a uno de los reales, un DNI
   repetido rechazado, y el PIN visible una sola vez.
4. **Cuenta corriente (US-018).** ✅ **Hecha (2026-09-29)**, ver `rikos-backend/doc/historias/US-018-cuenta-corriente.md`. Libro, saldo inicial, ajustes con
   motivo, saldo, deuda, tope calculado y editable, recordatorio. Pestaña
   Cuenta. *Se demuestra:* cargar a uno que debe y a otro con saldo a
   favor y ver su tope; y la prueba de dos locales en paralelo.

**Estándar de cada entrega** (el del sprint 4): prueba antes o junto con
el código, una prueba por rol en cada endpoint nuevo, bitácora al
cambiar algo, pantallas con sus cuatro estados, y el documento de la
historia en `rikos-backend/doc/historias/`.

## 6. Qué queda fuera del sprint (y dónde va)

| Fuera | Va en |
|---|---|
| Registrar un consumo real y el cierre de cuenta | Sprint 7 (caja) |
| Toma del pedido y vinculación del ticket | Sprint 6 (venta) |
| Kiosco y pre-ticket | Sprint 8 (US-019, US-020) |
| Abonos (efectivo y digitales) | Sprint 8 (US-021, US-024) |
| Cierre mensual y estado de cuenta en PDF | Sprint 13 (US-025) |
| Portal del pensionista y su contraseña | Sprint 13 (US-022, US-023; RF-027) |
| La sincronización sin internet en sí | Sprint 10 |
| Comprobante electrónico por consumo | Sprint 11 (RF-014, contador) |
| El monto real del táper y los precios reales del plan | El dueño (`07`, RF-002); no bloquea |

## 7. Riesgos y cosas abiertas

- **Los precios reales del plan y el monto del táper** los tiene que dar
  el dueño; hasta entonces el sistema los muestra "sin precio". No
  bloquea el sprint, sí bloquea cobrar a un pensionista real.
- **Los datos de los 25 pensionistas** (deuda o saldo a favor, DNI,
  responsable) hay que juntarlos antes de la carga real. En este sprint
  se prueba con datos de ejemplo; **la carga masiva no está en el
  alcance** (`README`: excluida). Si son 25 a mano, es viable.
- **Datos de menores de edad** (Ley 29733): parte de los pensionistas
  puede serlo. El responsable ya es obligatorio y sirve de consentimiento;
  conviene registrarlo en el charter. No se programa nada al respecto en
  este sprint.
