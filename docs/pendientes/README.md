# Pendientes

> **Corte actualizado del 11/10/2026:** consultar primero
> [el registro de auditoría integral](02-auditoria-integral-sprints-0-7-2026-10-11.md).
> Las listas fechadas de esta portada conservan historial y contienen asuntos ya
> corregidos; el registro identifica cuáles. Las soluciones propuestas no se
> implementaron en esta sesión.

Cosas identificadas pero no resueltas todavía, con su razón. Distinto del
backlog: el backlog es trabajo planeado; un pendiente es algo que se notó
y se decidió no tocar todavía, o una pregunta que nadie respondió.

| Archivo | Qué contiene |
|---|---|
| `01-preguntas-de-refinamiento.md` | Los vacíos y preguntas del Product Backlog v2.0 (sección 10): clasificación de impacto, tabla de refinamiento (RF-NNN), contradicciones y duplicidades detectadas, preguntas que bloquean pantallas concretas |
| [02-auditoria-integral-sprints-0-7-2026-10-11.md](02-auditoria-integral-sprints-0-7-2026-10-11.md) | Registro por ID de hallazgos, riesgos y validaciones: prioridad, solución, aceptación y dependencia; incluye conciliación de pendientes antiguos |
| [03-traspaso-sprint-8.md](03-traspaso-sprint-8.md) | Nota de traspaso al sprint 8 (circuito del pensionista): por dónde empezar, qué se reutiliza, trampas conocidas y pendientes heredados |

Los pendientes técnicos del código (P-NN) siguen en
`rikos-backend/doc/RIKOS-decisiones-tecnicas.md`, sección de pendientes.

## Pendientes abiertos hoy

### Del rediseño del POS y de salones y mesas (2026-09-30)

- **Pensionista en la toma del pedido: del DNI al ticket (sprint 8).** El diálogo de pensionista hoy busca por DNI o nombre; con US-019/US-020 pasa a teclear el código del pre-ticket (la búsqueda queda de respaldo si el kiosco falla). Diseño con ambas pestañas en el canvas (`PosPedidoPensionista`).
- **Botón "+ Persona" del pedido parte el texto en dos líneas en móvil** (375 px), de `PantallaPedido` (sesión de pedidos). Cosmético.
- **Sin pruebas automáticas de frontend:** la verificación responsive y funcional del POS es manual en navegador. Evaluar Playwright cuando el POS se estabilice.
- **Pedido de prueba abierto en dev:** quedó "Pedido 0001" en Mesa 1 (2 comensales, "Prueba responsive") de las pruebas del 2026-10-01; no hay forma de cancelar un pedido entero hasta el sprint 7.
- **Sin forma de crear un pedido para llevar hasta la entrega 4 (Pedido rápido).** El usuario quitó "Pedido para llevar" del mapa el 2026-10-01 y "Pedido rápido" aparece atenuado ("Pronto") en el menú. Hacer la entrega 4 antes de usar el POS con para llevar.
- **Unir varias mesas es secuencial en el cliente** (una llamada por mesa); si una falla, las anteriores quedan unidas. Si se vuelve común, pedir un endpoint que una varias en una transacción.
- **"Terminar pedido" (cierre sin cobro): HECHO el 2026-10-01** (sesión de pedidos, migración V38, `POST /pedidos/{id}/terminar`). Es **provisional**: no cobra ni toca cuentas; el sprint 7 lo completa con el cobro y el cierre de caja. Con él se liberaron las mesas de prueba de desarrollo sin borrar pedidos (6 pedidos pasaron a "Cerrada").
- **Regla "la mesa se ocupa al comandar" (2026-10-01): decidida, en implementación.** Backend (sesión de pedidos): estado de mesa derivado de pedido comandado, abrir una mesa con pedido abierto devuelve ese pedido, campo para saber si está "tomando pedido", bloqueo rechazado con pedido abierto. Frontend (mapa): marca "tomando pedido". Ver `plan/sprint-06-vender-en-mesa.md` §3.2.
- **Botón "Separar mesas" dentro de la pantalla del pedido: HECHO el 2026-10-01** (sesión de pedidos; probado con la sesión del mapa).
- **Categorías del catálogo del pedido (Todo, Entrada, Segundo, Extra):** datos hechos (5 productos nuevos, precios inventados) y decisión en `plan/sprint-06b-categorias-del-pedido.md`; falta `grupo` en la oferta del backend y las pestañas en la pantalla del pedido. Limpieza de categorías de prueba del catálogo de desarrollo ("oño", "siuuu", duplicadas) sin hacer.
- **Pantalla del pedido (probada 2026-10-01):** falta barra fija "Comandar (N)" en celular; el nombre del pensionista no viene del backend (`pensionistaNombre`); confirmar dónde sale la tarjeta "Extra libre". Detalle en `plan/sprint-06b-categorias-del-pedido.md`.
- **Elementos de la pantalla del pedido que no están en la referencia (2026-10-01), con propuesta de lugar:** (a) **lista de Comandas** (ver y reimprimir, aparece al comandar): debajo de las líneas, plegable, o un botón "Comandas (N)" en la cabecera de la orden; (b) **Mover mesa / Unir mesa / Separar mesas**: "Mover" y "Unir" dentro de un menú "Más acciones" (⋮) en la barra superior junto a "Cambiar mesa" (que ya es lo mismo que Mover); "Separar mesas" se queda visible; (c) **Quitar persona**: la papelera de su tarjeta (ya está); (d) **Anular una línea ya comandada** (pide motivo): en el menú ⋮ de la línea; (e) **Aviso de táper sin monto** (para llevar) y avisos de agotado/precio: franja amarilla bajo el resumen de la orden; (f) **Pre-cuenta / cobrar** (sprint 7): botón junto a Comandar, atenuado "Pronto"; (g) el botón "← Mesas" de un pedido para llevar debería decir "← Volver"; (h) **Seleccionar varias** (pasar o dividir líneas): ya está en la cabecera de la orden.
- **Canal "Mostrador".** El usuario lo quitó del diseño por ahora: el backend solo tiene `mesa` y `para_llevar`. Decidir en el sprint 12 (junto con delivery) si hace falta.
- **Historial: ya muestra pedidos "Cerrada"** (por "Terminar pedido", sin cobro). Sigue sin existir *anular un pedido entero* (solo líneas, US-031): decidir en el sprint 7 la etiqueta "Anulada" y el filtro por estado de pago.
- **Mapa de mesas: "personas" por mesa.** El mapa no trae comensales; el diseño original lo mostraba. Pedirlo a la sesión de pedidos si se quiere.
- **Modo colapsado de la barra lateral del POS** sin diseñar.
- **Mesas sin salón o sin posición.** V34 deja `salon_id` nullable; el mapa las junta en "Sin salón" y las acomoda en cuadrícula. Decidir si el alta debe exigir salón.
- **Conteo 7 mesas vs 6 dibujadas** visto en datos de desarrollo: revisar si hay una mesa desactivada o sin posición.
- **Choque posible de numeración de migraciones** (V34 salones y V35 nombre de cliente están bien; avisar antes de crear la siguiente).

### Encontrados en la auditoría entre documentos (2026-09-29)

- **Numeración de reglas corrida en el modelo de datos.** El modelo cita
  reglas (`RN-NNN`) con la numeración del backlog v1.0. Corregido en los
  bloques 3 y 4 (sprint 4, ver `docs/producto/plan/sprint-04-costos-y-menu.md`).
  Los bloques 5 en adelante casi seguro tienen el mismo problema:
  corregir cada uno cuando llegue su sprint, contra
  `flujos/90-reglas-de-negocio-consolidadas.md`.
- **EP-09 mal asignado en el modelo de datos.** El bloque 9 dice
  "EP-08 y EP-09", pero en el backlog v2.0 EP-09 es *panel y reportes*,
  no inventario. Casi seguro quedó de la numeración del backlog v1.0. El
  nombre del archivo ya se corrigió (`--ep08`); el texto interno del
  documento todavía dice "EP-08 y EP-09" -- corregir cuando se toque ese
  bloque.
### Del usuario de referencia (2026-09-29)

- **RF-021, para el dueño: cuántas cajas y cómo se cortan los turnos.**
  Cualquier usuario con permiso de caja puede cubrir al cajero fijo (el
  turno se ata a la persona), pero con una sola caja el cajero tiene que
  cerrar su turno antes de que otro abra -- a mitad del almuerzo puede no
  ser aceptable. Hay que resolverlo antes del sprint 7.
- **Código real del local.** `sanisidro` es dato de desarrollo, pero el
  código del local es contrato del POS (DT-040). Definirlo antes del
  primer despliegue.

### Del sprint 4 (2026-09-29)

- **Precios reales del menú normal y del extra (RF-002).** Los productos
  existen desde V21 sin precio; hasta que el dueño los dé, la caja no
  puede cobrarlos. En desarrollo el normal tiene S/ 12,00 de prueba.
- **¿El pensionista paga los agregados a precio completo?** RN-023 lo
  dice del menú extra, no de los agregados (naturaleza `extra`). Resolver
  antes del sprint 5, planes tarifarios.
  - **Respuesta del usuario (2026-09-29):** el pensionista come casi
    siempre el mismo menú que todos; el menú extra prácticamente no
    aparece. Lo que sí pasa es que coma **dos veces** o pida **para
    llevar (táper)**. Las dos cosas ya están en la fuente: varios
    consumos del mismo servicio en el día se cobran cada uno (`02` §
    Cuenta corriente) y el táper se cobra por menú en llevar, recojo y
    delivery, también al pensionista (`01` § Canales).
  - **Resuelto con el usuario (2026-09-29)**, ver
    `docs/producto/plan/sprint-05-cuenta-del-pensionista.md`: el segundo
    almuerzo va con la tarifa del plan salvo que una configuración global
    diga otra cosa; el táper tiene un solo precio para todos, editable; el
    agregado se cobra a precio normal, además del menú. **Sigue abierto
    solo el monto del táper** (`07`), que ahora es un dato editable, no un
    bloqueo.

- **De la entrega 1 (recetas), en `RIKOS-decisiones-tecnicas.md`:**
  P-30 (el catálogo muestra el costo de los insumos a quien no tiene
  permiso de ver costos), P-31 (cambiar la unidad base de un producto que
  ya está en recetas), P-32 (desactivar recetas).

- **Del cierre del sprint 4:** algunas pruebas de sprints anteriores no
  son `@Transactional` y dejan filas reales en la bitácora de la base de
  desarrollo en cada corrida ("eliminó el usuario", "desactivó
  prueba-json"). Ensucia lo que se ve en la pantalla de Bitácora. Revisar
  cuáles son y limpiarlas después de correr, o correrlas contra otra base.
- **CA2 de US-015 (comandas intactas al agotar)** queda sin prueba hasta
  que existan las comandas (sprint 6).

### Técnicos, arrastrados de sprints anteriores

- **Ciclo de módulos `comun ↔ identidad`** que encontró
  `ArquitecturaDeModulosTest` (HT-06). Reportado, no resuelto: arreglarlo
  en serio es decidir dónde vive `ConfiguracionSeguridad`, no un parche.
- **Limpieza programada de `bloqueo_pin`** (filas vencidas). Mencionada
  en el modelo de datos, nunca implementada.
- **`periodo_cuenta` / `periodo_id` en el libro (sprint 13).** La cuenta ya tiene `arrastre` en el CHECK pero aún no periodos: el cierre mensual los agrega con una migración nueva, sin tocar V28.
- **Datos inventados en la cuenta de desarrollo.** El libro no se puede borrar: el saldo inicial (S/ 50) y el ajuste (S/ 3) de "Luis Alberto Quispe Rojas" quedan; al pasar a datos reales, recrear la base de desarrollo.
- **Lint roto en `features/locales/HorarioSemanal.tsx`** (`_clave` sin usar, línea 119), ajeno al sprint 5.
- **Cantidad y disposición real de mesas por local (RF-010).** Sprint 6: las carga el administrador; los datos de desarrollo serán inventados. No bloquea.
- **Impresora de cocina (RF-012, RF-016, HT-10).** Sin hardware: la comanda es una vista imprimible del navegador y la prueba real queda pendiente.
- **Tres copias de "el local de la sesión"**: `catalogo.LocalActivo`, `pensionistas.LocalDeSesion` y `venta.LocalDeSesion` hacen lo mismo (leen `Authentication.getDetails()`). Conviene unificarlas en una clase de `comun` expuesta.
- **Login por PIN da 500 si el User-Agent pasa de 120 caracteres** y el PIN es incorrecto (`bloqueo_pin.dispositivo` VARCHAR(120)); ver la tarea aparte propuesta en la sesión del sprint 6.
- **Sprint 6, entrega 1: minors diferidos de la revisión final** (los 3 Important ya están arreglados, ver `US-027-mapa-de-mesas.md`): un motivo de bloqueo de más de 250 caracteres se corta en silencio; la frase de bitácora repite "la mesa" (`bloqueó la mesa "Mesa 7"`) y el campo `nombre` significa cosas distintas en `cambio_mesa` y `bloqueo_mesa`; si falla activar o desactivar una mesa se reemplaza toda la tabla por el error; una respuesta vieja puede pisar a la nueva al cambiar de local (sin cancelación); `.mapa-mesa` sin `overflow-wrap` (número o nombre largo desborda en celular); comentario desactualizado en `MesasAccesoPorRolTest` ("cocina y motorizado: nada de mesas", ya ven el mapa); faltan pruebas de `SIN_LOCAL_ACTIVO` y 404 por HTTP en editar, activar y liberar, y de bloquear una mesa unida; "1" y "01" son números distintos; `GET /mesas?todas=true` lo ve cualquiera con `venta.ver`; **entrega 4:** `BloquearMesa` solo mira `mesaPrincipalId` de la propia mesa, así que una mesa que encabeza una unión se podría bloquear.
- **Sprint 6, entrega 2A: minors diferidos de la revisión final** (el Critical y los 7 Important ya están arreglados, ver `US-028-tomar-el-pedido-backend.md`): `AsignarPensionista` no es idempotente (repetir el mismo `PUT` da 409 porque `existsByPedidoIdAndPensionistaId` también encuentra a la propia persona); carreras entre casos de uso sin bloqueo (bloquear o desactivar una mesa a la vez que se abre un pedido; asignar un pensionista a la vez que se agrega una línea; quitar una persona a la vez que se le agrega una línea, que termina en 500); `Pedido.@Version` nunca cambia porque ningún caso de uso escribe el pedido, así que `PEDIDO_MODIFICADO` por versión no puede ocurrir hoy; `PedidosConcurrenciaTest` simula el choque con dobles, no con hilos reales; `AgregarLinea` de tipo carta acepta cualquier producto vendible aunque no sea de las categorías que muestra la oferta (`producto_fijo`/`extra`) y no revisa que el producto del menú esté activo; ninguna escritura revisa `pedido.estado` (uno pendiente de cierre sigue aceptando líneas) ni vuelve a comprobar que el pensionista siga activo; `ModificarLinea` puede subir la cantidad de un extra libre sin rastro en la bitácora; el segundo consumo se cuenta con `LocalDate.now` y no con `pedido.fechaOperativa` (una cena que cruza la medianoche cuenta contra el día siguiente); los paquetes `catalogo.infraestructura`, `pensionistas.infraestructura` e `identidad.aplicacion` se expusieron enteros (mejor una interfaz de consulta angosta); la especificación 3.10 dice "cocina y motorizado no entran al pedido" pero tienen `venta.ver` y lo ven (decidido así, hay que confirmarlo con el dueño).
- **Sprint 6, entrega 2B (pantalla del pedido):** crear productos de carta en las categorías "Bebidas calientes" (S017) y "Sándwiches y panes" (S016) dio HTTP 500 (en "Bebidas" y "Porciones adicionales" sí funcionó): investigar la causa en `POST /productos`. El `mozotest` quedó con un PIN aleatorio desconocido: reasignarlo desde el panel para volver a probar el POS. Un 409 de otro mozo en el mismo pedido solo se comprobó por las pruebas del backend, no en el navegador con dos pestañas. Además, `LocalDeSesion` ya tiene **cuatro copias** (`catalogo.LocalActivo`, `pensionistas`, `venta.aplicacion` y la de `identidad`): conviene unificarlas.
- **`EliminarProveedor` sin chequeo de historial.** Intencional hasta que
  exista la tabla de compras (sprint 9).

### De negocio, que bloquean sprints futuros

- **P-01 a P-05** (datos fiscales, cuenta demo del proveedor de
  comprobantes, consulta al contador) bloquean el sprint 11. El backlog
  recomienda iniciarlas ya.
- **Sprint 6, entrega 3 (comanda y anulaciones):** la hoja impresa (`@media print` en `estilos/global.css`) no se pudo ver en una vista previa de impresión: revisar al hacer HT-10 con la impresora real; comandar dos veces a la vez solo se cubre con la segunda petición secuencial (`SIN_LINEAS_PENDIENTES`), no con hilos reales; `Pedido.@Version` ahora sí cambia al comandar; el modelo de datos (`docs/producto/modelo-de-datos/06-...`) aún describe `detalle_pedido` sin las columnas de anulación (`observacion_anulacion`) ni `comanda.impresiones` con valor inicial 0 (el modelo dice 1): actualizar; al anular una comandada no se revisa `pedido.estado` ni se avisa a cocina de la anulación (cocina se entera de palabra); los PIN de `mozotest` y `mtorres` quedaron aleatorios; el error 500 de `POST /auth/pin` con un `User-Agent` largo (`bloqueo_pin` guarda el dispositivo en 120 caracteres) sigue sin arreglar y se ve como "No se pudo verificar el PIN" al equivocarse de PIN; `MesasAccesoPorRolTest` y `MesasConcurrenciaTest` fallan (500 al crear una mesa) mientras el trabajo de salones (V34) está a medias: es de esa sesión, no de esta entrega.
- **Mapa de mesas nuevo (otra sesión) y el backend:** el mapa calcula el tiempo ocupado con `abiertaEn`; "Por cobrar" (`pendiente_cierre`) sigue sin activarse hasta el sprint 7. El modelo de datos (`docs/producto/modelo-de-datos/06-...`) todavía no menciona `pedido.nombre_cliente` (V35).
- **Pruebas y datos de desarrollo:** los motivos de anulación creados a mano en la base (p. ej. "Error del mozo") chocaban con los de las pruebas por la restricción de unicidad; las pruebas ahora usan nombres únicos.
- **Sprint 6, entrega 4 (reorganizar):** el "segundo del día" de un pensionista no se recalcula en las líneas que se quedan cuando se pasa otra (p. ej. dos menús y se pasa el primero: el segundo conserva su marca); al cerrar el pedido de una mesa principal (sprint 7) hay que separar las mesas unidas; desde el mapa de mesas todavía no se puede mover, unir ni separar (solo desde la pantalla del pedido); una mesa unida sin pedido en la principal no muestra nada que operar (se abre el pedido en la principal); la prueba de concurrencia de "dos mozos mueven a la misma mesa" se apoya en la restricción única de la base pero no tiene una prueba con hilos reales; el modelo de datos aún no menciona `detalle_comanda.etiqueta_persona` (V36) ni `pedido.nombre_cliente` (V35).
- **Sprint 6, revisión de las entregas 3 y 4 (minors diferidos):** carreras sobre una mesa entre mover el pedido, unir y bloquear (a la vez, una mesa podría quedar unida o bloqueada y con su propio pedido; conviene bloquear la fila de la mesa destino); dos uniones cruzadas (A→B y B→A) simultáneas podrían formar un ciclo de principales; `AnularLinea` no revisa `pedido.estado` (decidir con el cierre del sprint 7 si se puede anular en `pendiente_cierre`); la comanda agrupa las líneas por el texto de la etiqueta (dos personas con el mismo nombre escrito a mano salen como un grupo) y no hay UNIQUE en `(pedido_id, orden)`.
- **Sprint 6b (lista de pedidos):** el Historial (`situacion=cerrados`) sale vacío hasta el sprint 7 (estados `cerrado` y `anulado` del pedido, forma de pago); la lista trae las líneas sin precio (solo qué se pidió y su estado); `q` busca por número de pedido, cliente y mesa, no por producto ni por mozo; Mostrador y Delivery del Pedido rápido siguen sin especificar.
- **"Terminar pedido" (provisional):** es un cierre sin cobro; el sprint 7 lo completa con el cobro (turno, forma de pago, cargo al pensionista, comprobante) y agrega `anulado` y la reapertura. Al terminar, las mesas unidas se separan solas. Queda resuelto el pendiente de que ninguna escritura revisaba el estado del pedido (un cerrado da `PEDIDO_CERRADO`); `pendiente_cierre` sigue aceptando líneas hasta que el sprint 7 decida.
- **Regla "ocupada = comandado" (2026-10-01):** un pedido abierto sin comandar queda "en curso" (Órdenes activas, conteos) pero no ocupa la mesa, así que puede acumularse si nadie lo termina; se limpia con "Terminar pedido". La apertura concurrente sobre la misma mesa se cubre con una prueba con dobles (reintento), no con hilos reales.
