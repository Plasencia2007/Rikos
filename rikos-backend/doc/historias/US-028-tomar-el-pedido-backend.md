# US-028 — Tomar el pedido (backend)

Sprint 6, entrega 2A de 4 (2026-09-30). Especificación en
`docs/producto/plan/sprint-06-vender-en-mesa.md` (3.3, 3.4, 3.5, 3.11 y 4.1); plan táctico en
`docs/superpowers/plans/2026-09-30-sprint6-entrega2a-pedido-backend.md`. La pantalla del POS es la **2B** (plan aparte).

## Qué se pidió

Que el mozo abra un pedido en una mesa o para llevar, lo reparta entre las personas de la mesa (clientes normales y
pensionistas) y le agregue líneas con el precio correcto: el menú del día con sus opciones, la carta, la comida del plan
del pensionista y un extra. Sin comandar ni cobrar todavía.

## Qué se construyó

**Base de datos — `V32__pedidos.sql`.** `pedido`, `cuenta_persona`, `detalle_pedido` y `detalle_pedido_componente`. Reglas de
forma en la base: un pedido de mesa lleva mesa y uno para llevar no; número único por local y día; **una mesa tiene un solo
pedido en curso**; un pensionista aparece una vez por pedido; una línea nunca lleva precio ni cantidad cero; el extra libre
exige descripción. Difiere del modelo original en tres cosas, todas decididas en la especificación: **subtotal, total y
táper no se guardan** (se derivan al leer); `detalle_pedido.tipo_linea` separa menú, comida del plan y extra libre (por eso
`producto_id` puede ser nulo); el costo y la receta se congelan al cerrar (sprint 7). Evento de bitácora `precio_manual`.

**Backend (módulo `venta`).**

- `AbrirPedido`: mesa libre (no bloqueada, no unida, activa, del local, sin otro pedido) o para llevar; el servicio sale del
  horario vigente del local o lo elige el mozo; el id lo puede traer el cliente (reenviar no duplica); crea "Persona 1".
- `AgregarPersona`, `AsignarPensionista` (activo, sin repetirse en el pedido, solo a una persona sin líneas), `QuitarPersona`.
- `CalculadorDePrecioDeLinea`: traduce la línea al `MotorDePrecios` del sprint 5. El mozo **nunca** escribe un precio salvo
  el extra libre. Cuenta el **segundo consumo del día entre locales** y no cuenta lo anulado.
- `AgregarLinea`: carta (vendible, con precio, no agotado en el local), menú normal y extra (solo almuerzo, con el menú
  confirmado de hoy; una sola opción se asigna sola y la fila se guarda igual, varias se eligen), comida del plan
  ("Desayuno del plan" / "Cena del plan", solo pensionistas y solo en su servicio) y extra libre (descripción y precio del
  mozo, marcado como manual y en la bitácora). Idempotente por id.
- `ModificarLinea` y `QuitarLinea` (solo pendientes: una comandada se anula en la entrega 3), `ConsultaPedidos` (el pedido
  con totales por persona, el pedido en curso de una mesa y la **oferta del día**: menú por componente y carta sin agotados).
- **La mesa "ocupada" de verdad:** `ConsultaMesas` deriva ocupada y pendiente de cierre de los pedidos; una mesa con pedido
  no se bloquea ni se desactiva.
- `PedidoController`: `POST /pedidos`, `GET /pedidos/{id}`, `GET /mesas/{id}/pedido`, `GET /pedidos/{id}/oferta`, personas
  (`POST`, `PUT …/pensionista`, `DELETE`) y líneas (`POST`, `PUT`, `DELETE`). Cada escritura devuelve el pedido completo.
  Ver, `venta.ver`; escribir, `venta.crear`. Los choques de concurrencia dan **409** (`PEDIDO_MODIFICADO`,
  `NUMERO_DE_PEDIDO_REPETIDO`, `MESA_OCUPADA`, `PENSIONISTA_YA_EN_EL_PEDIDO`), no 500.
- Se expusieron con `@NamedInterface` los paquetes que `venta` consume (`catalogo`, `pensionistas`, `identidad`); solo el
  ciclo `comun ↔ identidad` sigue fallando en `ArquitecturaDeModulosTest`.
- Bitácora: "admin puso el precio de un extra a mano: "Huevo frito" a S/ 3.00 (pedido "0001")".

## Qué se dejó fuera y por qué

- **La pantalla del POS** (2B): depende de la forma de esta API.
- **Comandar y anular** (entrega 3), **reorganizar y recalcular al cambiar de persona** (entrega 4, por eso asignar un
  pensionista a una persona con líneas se rechaza), **cobrar y el cargo a la cuenta del pensionista** (sprint 7), **descuento
  de inventario** (sprint 9).
- Un pensionista pide **una** comida del plan por línea (para poder contar el segundo consumo del día).

## Cómo se verificó

- **70 pruebas nuevas** (449 en la suite tras el pase de arreglos, todas en verde, sin contar `ArquitecturaDeModulosTest`): `DependenciasDeVentaTest`,
  `TablasDePedidoTest` (9), `AbrirPedidoTest` (7), `PersonasDelPedidoTest` (6), `PreciosDeLineaTest` (6),
  `LineasDelPedidoTest` (11), `MenuDelDiaEnPedidoTest` (7), `ConsultaDelPedidoTest` (7), `MesasConPedidoTest` (5),
  `PedidosAccesoPorRolTest` (6), `PedidosConcurrenciaTest` (4) y una en `ListarAuditoriaTest`.
- **Por API contra la base de desarrollo:** abrir un pedido de mesa, agregar un extra de 2 × S/ 3 (total S/ 6), la mesa 1 pasa
  a "ocupada", una segunda apertura da 409 `MESA_OCUPADA` y bloquearla da 409 `MESA_NO_LIBRE`. Se limpió el pedido de prueba.
- Roles: mozo, cajero y administradores toman y corrigen pedidos; cocina y motorizado los **ven** (tienen `venta.ver`) pero no
  los tocan; un pedido de otro local es 404 en cada verbo.

## Decisiones tomadas al ejecutar (rulings)

- Sin worktree ni commits (regla del proyecto; el trabajo previo sigue sin commitear).
- Orden de tareas 5 → 6 → 4 y dos pruebas de personas movidas a `LineasDelPedidoTest` (necesitaban `AgregarLinea`).
- `OpcionMenu.isActivo()` agregado (el campo existía sin getter).
- La prueba del segundo consumo usa la regla por defecto (precio del plan) y comprueba que queda marcado como segundo.
- `DELETE` de personas y líneas devuelve 200 con el pedido completo (no 204).
- El desayuno y la cena del plan para llevar **sí** llevan táper (RN-009): lo corrigió una prueba mía.

## Bugs y tropiezos

- Postgres deja la transacción abortada tras la primera violación: una violación por prueba.
- `reloj.withZone(...)` devuelve `null` con un reloj de prueba (Mockito): se usa `LocalDateTime.now(reloj)`.
- Recompilar con el backend corriendo dejó `target/classes` incompleto (otra vez): se detiene el 8081 antes de correr `mvn`.

## Ojo con los datos de desarrollo

En la base de desarrollo **no hay menú confirmado para hoy ni productos de carta con precio**: `GET /pedidos/{id}/oferta`
devuelve `hayMenu: false` y `carta: []`. Para probar la pantalla del POS (2B) hay que programar y confirmar el menú de la
semana y cargar algunos productos de las categorías "producto fijo" o "extra" con su precio.

## Pase de arreglos tras la revisión final (2026-09-30)

Un revisor independiente encontró **1 Critical y 7 Important**; los ocho se arreglaron, cada uno con su prueba escrita primero (vista fallar):

1. **Critical — ids reutilizables.** `CuentaPersona` y `DetallePedido` no tienen `@Version` ni `Persistable`: `save()` con un id asignado hace *merge*. Un id de persona o de línea que ya existía en **otro pedido (incluso de otro local)** sobrescribía esa fila (la movía de pedido, borraba su pensionista, volvía a "pendiente" una línea ya comandada). Ahora un id que existe fuera del pedido o de la persona actual da `NO_ENCONTRADO`, y uno del mismo lugar es un reenvío que devuelve lo que ya estaba.
2. **Agotados en el menú del día.** Una opción del menú agotada en el local ya no se ofrece ni se acepta (`PRODUCTO_AGOTADO`); con una sola disponible se asigna sola; si todas las de un componente están agotadas, no se puede pedir el menú.
3. **Origen del precio.** Se decidía comparando montos: un plan que cuesta lo mismo que el precio normal quedaba como "público". Ahora se decide por la regla que aplicó el motor.
4. **Segundo desayuno o cena del plan** con la regla "segundo a precio normal": no existe un precio normal de la comida del plan, así que daba "sin precio" y no se podía vender. Ahora se cobra al plan (y queda marcado como segundo). *Decisión mía; si el dueño lo quiere distinto, se cambia.*
5. **Aviso de táper sin monto.** El pedido responde `avisos: ["TAPER_SIN_MONTO"]` cuando es para llevar, lleva menús y el dueño no puso el monto (la especificación pedía "se agrega sin cargo y se avisa").
6. **Elección nula o repetida en el menú:** un `opcionMenuId` nulo daba 500; ahora cuenta como "sin elección", y un componente repetido o desconocido da `OPCION_INVALIDA`.
7. **Número correlativo repetido** entre dos mozos que abren a la vez, y 8. **reenvío concurrente del mismo id** (clave primaria repetida): daban 409/500; ahora el controlador **reintenta hasta 3 veces** (los casos de uso son idempotentes por id).

Suite completa tras el pase: **449 pruebas, 0 fallos** (suma de surefire, sin `ArquitecturaDeModulosTest`).
