# Sprint 5, entrega 2 — Motor de precios

2026-09-29. Especificación en `docs/producto/plan/sprint-05-cuenta-del-pensionista.md`
(sección 3.5). Sin historia de usuario propia: es la pieza que une US-017
(planes) con el cobro que llega en el sprint 7, y que RN-021, RN-022 y RN-023
exigen. La entrega 1 está en `US-017-planes-tarifarios-y-parametros-de-cobro.md`.

## Qué se pidió

Que las reglas de cuánto se cobra (cliente normal, pensionista con ticket,
menú extra, segundo almuerzo del día, agregados, táper) estén **escritas en un
solo lugar y probadas antes de que exista la caja**, para que la caja solo las
use y no las repita.

## Qué se construyó

**Backend (`pensionistas`).**

- `MotorDePrecios.calcular(SolicitudDePrecio)` → `PrecioCalculado`: precio,
  táper, total, avisos y una explicación en español de por qué ese precio.
  Sin estado y sin efectos: no escribe nada.
- Las reglas, en orden:
  1. **Agregado** (huevo frito, porción adicional, plato de carta): precio
     normal para todos, sin tarifa de pensionista, sin táper.
  2. **Menú extra:** precio completo para todos (RN-023).
  3. **Menú del día:** cliente normal → precio normal. Pensionista **con
     ticket** → precio de su plan para esa comida en la fecha del consumo. Su
     segundo consumo de la misma comida ese día va con el plan (por defecto) o a
     precio normal, según el parámetro global.
  4. **Para llevar / recojo / delivery:** se suma el táper por cada menú (del
     día o extra), a todos por igual.
- **Nunca inventa un precio.** Si falta el precio normal, el precio del plan o el
  monto del táper, devuelve `null` y un aviso (`SIN_PRECIO_NORMAL`,
  `PLAN_SIN_PRECIO`, `TAPER_SIN_MONTO`), nunca cero.
- El motor **no conoce productos ni pedidos**: quien lo llama decide el tipo de
  línea y le pasa el precio normal. Así `pensionistas` no depende de `catalogo`
  y la caja podrá usarlo con lo que ya tenga.
- `POST /precios/calculo` (`pensionistas.ver`): el mismo cálculo por API, de solo
  lectura, para probar las reglas y para el simulador.

**Frontend.** En Pensionistas → Planes tarifarios, una tarjeta **"Probar un
precio"**: se elige qué se cobra, la comida, quién (cliente normal o pensionista
con ticket) y el precio normal, con las casillas "segundo del día" y "para
llevar". Muestra precio, táper y total por unidad, los avisos de lo que falta y
la explicación. No cobra ni guarda nada.

## Decisiones de esta entrega

- **Se es pensionista por el ticket, no por la ficha.** Un pensionista sin ticket
  vinculado se trata como cliente normal (lo dijo el usuario). El motor recibe
  `planId` solo cuando hay ticket.
- **El precio depende de la fecha del consumo,** no de hoy: es lo que hace que
  cambiar un plan no toque lo anterior (RN-022, CA3 de US-017). Cada consumo
  cobrado guardará su propio precio cuando exista la caja.
- **El agregado del segundo almuerzo tampoco tiene tarifa:** la regla del
  segundo consumo aplica solo al menú del día.
- **`RN-023` está escrita dos veces** (`TipoMenu.admiteTarifaReducida()` en
  `catalogo` y el caso "menú extra" del motor). No se comparte a propósito para no
  crear una dependencia entre módulos por una línea; las pruebas de los dos
  lados la cubren.

## Qué se dejó fuera a propósito

- **Decidir si un consumo es "el segundo del día":** lo cuenta la caja (sabe qué
  consumió ya esa persona); el motor recibe el dato. Se hace en el sprint 7.
- **Redondeos y descuentos:** no hay ninguno en la especificación. Todo es entero
  en céntimos.
- **Cuál es el producto "menú extra" o "menú normal":** eso ya lo resuelve
  `producto.tipo_menu` (sprint 4); la caja le dirá al motor el tipo de línea.

## Cómo se verificó

**Pruebas automatizadas: 258 en verde, sin contar `ArquitecturaDeModulosTest`**
(suma de reportes = conteo de `@Test` del código; 239 al cerrar la entrega 1).

- `MotorDePreciosTest` (18), la tabla de casos, con plan desayuno 500 / almuerzo
  800 / cena 800, menú 1200, extra 1800, agregado 300, táper 100: cliente normal
  1200; pensionista 800 / 500 / 800 según la comida; el extra 1800 con y sin
  ticket; segundo almuerzo con el plan, con la regla en "precio normal" (1200) y
  sin efecto en el cliente normal; agregado 300 con y sin ticket, también el del
  segundo almuerzo; para llevar suma 100 a cliente normal y a pensionista (total
  1300 y 900), al extra sí y al agregado no; en el local no hay táper; **sin monto
  de táper no se inventa uno y se avisa**; plan sin precio de esa comida o
  producto sin precio normal devuelve `null` con su aviso; plan inexistente es un
  error; el precio depende de la fecha (hoy 800, mañana 900 tras un cambio).
- `PlanesAccesoPorRolTest` (+1): el motor lo consultan el administrador central,
  el de local, el cajero y el mozo; cocina y motorizado reciben 403; datos
  inválidos dan 422 y un plan inexistente 404.

**En vivo** (usuario `admin`, plan de desarrollo con precios inventados; menú
S/ 12, extra S/ 18, agregado S/ 3, táper S/ 1), por la tarjeta "Probar un
precio": cliente normal S/ 12.00; pensionista S/ 8.00 ("precio de su plan para
almuerzo"); extra S/ 18.00 ("sin tarifa del plan"); agregado S/ 3.00; pensionista
para llevar S/ 8.00 + táper S/ 1.00 = S/ 9.00; cliente normal para llevar
S/ 12.00 + S/ 1.00 = S/ 13.00; desayuno de pensionista S/ 5.00.

## Encontrado en el camino

- **La API rechazaba un cuerpo sin los campos sí/no** ("Cannot map null into type
  boolean"). Los campos `segundoDelDia` y `paraLlevar` pasaron a `Boolean`: si no
  vienen, valen "no". Lo encontró la prueba de acceso por rol al mandar un cuerpo
  mínimo.
