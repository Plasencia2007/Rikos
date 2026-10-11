<!-- Comparación del backlog v3 (prd/03-backlog-v3.md) contra el backlog v2.0 (flujos/) y contra lo construido (sprints 0-6). Escrito el 2026-10-04. Solo lectura de las fuentes; este es el único archivo escrito. -->

# Comparación del backlog v3 contra v2.0 y contra lo construido

Alcance: las 23 historias construidas o en construcción (US-001 a US-018 y US-027 a US-031, sin US-019 a US-026) más HT-07 (= US-01 nueva). Los IDs de dos dígitos son del v3; los de tres dígitos, del repo (tabla en `prd/04-traduccion-de-ids-v3.md`).

## Resumen (10 líneas)

1. v3 es el v2.0 reordenado, no reescrito: comparé por programa, campo por campo, las 23 historias y sus criterios CA1.. contra `flujos/`; no hay un solo CA, prioridad, release, estimación, actor ni dependencia con cambio de fondo (solo cambia la numeración y el formato `Given/When/Then`).
2. Las 94 reglas RN son idénticas al v2.0 en texto y en historias asignadas (comparación automática de las 94 filas); la única rareza es interna del v3: RN-058 no lista a US-01 aunque US-01 la declara.
3. El único cambio real es HT-07 -> US-01: pasa de tarea técnica ("Componentes base y diseño táctil", sprint 2, sin CAs) a historia de mozo con tres CAs verificables (56 px, teclado en la mitad inferior, un solo teclado para cuatro pantallas).
4. Esos CAs nuevos de US-01 no se cumplen hoy: el rediseño del POS fijó objetivos de 44 px (`ui-ux/03-pos-rediseno.md`) y las cantidades del pedido usan botones − / +, no el teclado compartido.
5. Como v3 repite los CAs del v2.0, las brechas que aparecen son las que ya existían y la historia de cada US declaró o no vio; las más serias: "Terminar pedido" salta el permiso y el motivo de anulación (RN-072/RN-065) y deja un pedido `cerrado` sin snapshot de costo (RN-005/RN-046).
6. Contradicción directa con una decisión tomada: US-07 CA2 pide unir "dos mesas ocupadas"; lo construido solo une una mesa libre a una principal, y la mesa solo está ocupada al comandar.
7. Otras brechas medianas: US-54 CA3 (500 con User-Agent largo, el bloqueo no cuenta), US-57 CA4 (inmutabilidad solo en la aplicación) y evento de bitácora `transferencia` usado para mover líneas y mesas, que choca con "transferencia de inventario" de RN-004/US-45.
8. Desviaciones de la fuente ya documentadas y vigentes: contraseña del portal sin pedir (US-15 CA1), precio de plan por comida y no por producto fijo (US-16 CA1), plan sin estado activo/inactivo (US-16 CA2, RN-021), borrado físico (DT-043/DT-051) frente a RN-007.
9. Lo que falta por diseño de sprint (caja, inventario, offline, reportes, kiosco) no es brecha del v3: está en el plan.
10. Antes de cerrar el sprint 6: terminar-pedido, regla de unir, 44 vs 56 px, bug del PIN, prueba de US-04 CA2 y definir "tipo de entrega"; el resto pasa al sprint 7+ (lista al final).

## Qué leí y qué no

Leí completos: `prd/03-backlog-v3.md` (historias US-01 a US-09, US-15 a US-17, US-32 a US-37, US-52 a US-58, secciones 4, 5 y 6), `prd/04-traduccion-de-ids-v3.md`, `flujos/01` y `flujos/11`, `flujos/90`, `plan/01-plan-de-sprints.md`, `plan/03-historial...`, `plan/sprint-06-vender-en-mesa.md`, la sección 3 de `plan/sprint-05...`, `docs/pendientes/README.md`, y las historias en `rikos-backend/doc/historias/`: US-001-003, US-004-006, US-008-009-011 (parcial), US-010, US-013, US-014, US-015, US-016, US-017, US-018, US-027, US-028 (backend y pantalla), US-029, US-030-031, sprint-06b-lista, pos-rediseno-mesas-y-unir, pos-rediseno-entrega-4, motivos-de-operacion, sprint 3 (control y trazabilidad), y DT-028, DT-043 a DT-047, DT-051 de `RIKOS-decisiones-tecnicas.md`.

La comparación v3 contra v2.0 no se hizo a ojo: un script extrajo cada historia de ambos documentos (traduciendo IDs con la tabla de la sección 6) y comparó descripción, RN asociadas, dependencias, datos, riesgos, trazabilidad y cada CA. Sale sin diferencias salvo HT-07 y HT-09 (esta última = US-47, fuera de alcance).

Del código solo abrí lo necesario para verificar afirmaciones concretas (`TerminarPedido`, `ConfiguracionSeguridad` en la parte de `/pedidos/*/terminar`, `ComandaDetallada`/`LineaDeComanda`, `Producto`, CSS del teclado y de las cantidades, migraciones donde aparece el evento `transferencia`). **No leí** `docs/fuente/especificacion-v0.1/`, `docs/producto/modelo-de-datos/`, `plan/sprint-04-costos-y-menu.md` (solo lo que citan las historias), `plan/02-grafo-de-dependencias.md`, ni la mayor parte de los tests; donde digo "no verificado" es por eso.

## Tabla de veredictos

Veredicto: **igual** = v3 coincide con v2.0 y con lo construido (o lo que falta es trabajo planeado de un sprint posterior); **cambio menor** = diferencia documentada o de alcance que no obliga a rehacer; **brecha** = un CA de v3 que lo construido no cumple, contradice o no cubre sin que esté planeado.

| ID v3 | ID repo | Historia | Veredicto |
|---|---|---|---|
| US-01 | HT-07 | Operar el POS con una sola mano | **brecha** (CA1, CA3; CA2 no verificado) |
| US-02 | US-013 | Programar el menú semanal del almuerzo | igual |
| US-03 | US-014 | Configurar el menú normal y el menú extra | igual (precios reales pendientes, RF-002) |
| US-04 | US-015 | Marcar productos agotados y reactivarlos | **brecha** menor (CA2 sin prueba) |
| US-05 | US-027 | Visualizar y operar el mapa de mesas | cambio menor (regla "ocupada al comandar") |
| US-06 | US-028 | Tomar el pedido asignando productos por persona | **brecha** (CA6 tipo de entrega; recojo/delivery fuera) |
| US-07 | US-029 | Unir, mover, dividir y trasladar cuentas | **brecha** (CA2 contradice decisión) |
| US-08 | US-030 | Emitir la comanda a la impresora central | **brecha** (CA1 "mozo", CA2, CA5 sin verificar; HT-10) |
| US-09 | US-031 | Anular productos antes y después de comandar | **brecha** ("Terminar pedido" salta CA3) |
| US-15 | US-016 | Ficha del pensionista y su responsable | **brecha** (CA1 contraseña del portal) |
| US-16 | US-017 | Planes tarifarios y precios especiales | **brecha** (CA1 por producto fijo; CA2 estado) |
| US-17 | US-018 | Cuenta corriente del pensionista | cambio menor (tope calculado; CA4 y CA1 dependen de caja) |
| US-33 | US-008 | Catálogo central de productos | cambio menor (borrado físico, DT-051) |
| US-34 | US-009 | Unidades base, presentaciones y conversiones | igual (CA3 es del sprint 9) |
| US-35 | US-011 | Catálogo de proveedores | cambio menor (borrado sin chequeo de compras) |
| US-36 | US-012 | Listas de motivos del sistema | cambio menor (ampliado a 8 tipos; CA2/CA3 diferidos) |
| US-37 | US-010 | Recetas y subrecetas valorizadas | **brecha** menor (CA6 vs P-30; CA5 sin versionado) |
| US-52 | US-001 | Administrar empresa y locales | cambio menor (DT-043; local inactivo solo se frena en el PIN) |
| US-53 | US-002 | Autenticar con usuario y contraseña | igual (CA4 es del sprint 10) |
| US-54 | US-003 | Acceder como mozo con PIN individual | **brecha** (CA3 con User-Agent largo) |
| US-55 | US-005 | Usuarios y asignación multilocal | cambio menor (DT-051 borra usuarios sin actividad) |
| US-56 | US-004 | Roles y permisos granulares | igual |
| US-57 | US-006 | Auditoría transversal | **brecha** (CA4 solo en la aplicación; evento `transferencia` sobrecargado) |
| US-58 | US-007 | Horarios de servicio por local | cambio menor (DT-044: por día y con turnos, más rico que v3) |

Sin diferencia entre v2.0 y v3 en prioridad, release ni estimación para ninguna de las 24 filas. Las 24 son Must/MVP en ambas versiones; estimaciones idénticas (US-06 XL, US-37 XL, etc.). Dependencias: solo renumeración, con una excepción: US-01 pasa de depender de HT-01 a "ninguna funcional".

## Cambios del v3 que no son criterios

- **HT-07 -> US-01, HT-09 -> US-47** salen de las tareas técnicas; las demás HT se renombran (HT-02 "Migraciones con un único dueño", HT-08 "Identificador de correlación en los registros" en vez de "Observabilidad mínima") y se reducen a ocho. Impacto en el plan: `plan/01-plan-de-sprints.md` y `plan/README.md` todavía listan HT-07 dentro del sprint 2 como habilitante; US-01 ahora es una historia con CAs que ese sprint no cerró del todo (ver brecha 1).
- **v3 no trae sprints**, solo Release (MVP / Post-MVP). La secuencia de 15 sprints sigue siendo la del v2.0; v3 no la cambia.
- **Interna del v3:** la tabla RN de la sección 5 asocia RN-058 solo a US-05 y US-06, y US-01 la declara en su ficha. Pedir a quien mantiene el v3 que agregue US-01 a RN-058.

## Detalle de brechas y cambios relevantes

### 1. US-01 (HT-07): CA1, CA2 y CA3 no se cumplen o no están verificados

- **CA1** ("área activa no menor a 56 px por lado en cualquier pantalla del POS"). El token `--toque: 56px` existe (`estilos/tokens.css`) y el teclado numérico mide 58 px, pero el rediseño del POS decidió **44 px** como mínimo (`ui-ux/03-pos-rediseno.md`: "ítems de 44 px o más"; §"cualquiera": "objetivos táctiles de 44 px o más") y varios documentos lo repiten ("botones de 44 px", personas de 44 px de alto, botón de cerrar de los diálogos 44 px). `ui-ux/02-reglas-de-diseno-transversales.md` §7.1 sí dice 56 px: los dos documentos de diseño se contradicen, y v3 CA1 hace cumplir el de 56.
- **CA3** ("PIN del mozo, DNI y PIN del kiosco, cantidades del pedido y conteo de efectivo usan el mismo teclado"). Hoy solo el PIN (login del mozo y el formulario de usuario) usa `EntradaNumerica`/`TecladoNumerico`. Las cantidades del pedido usan botones − / + (`OrdenDeLaMesa.tsx`), no el teclado; el kiosco es del sprint 8 y el conteo de efectivo del sprint 7. `EntradaNumerica` ya tiene un "modo cantidad" pensado para esto que nadie usa.
- **CA2** (teclado en la mitad inferior en celular): DT-028 lo decide para la pantalla de PIN; no lo verifiqué en el código ni hay prueba registrada. No verificado.
- **Acción:** decidir y registrar (DT nuevo) si el POS adopta 56 px o se enmienda el v3 a 44 px; si 56, subir `.ped2-*`, `.ped3-*`, `.ped4-*` y los controles de cantidad. Hacer que el selector de cantidad del pedido use el teclado compartido o dejar escrito por qué no. Corregir la ubicación de HT-07 en el plan.

### 2. "Terminar pedido" y US-09 CA3 / RN-072 / RN-065 / RN-005

`TerminarPedido` (V38, `POST /pedidos/{id}/terminar`) exige solo `venta.modificar`, que **el mozo tiene**; acepta pedidos `comandado` y `pendiente_cierre`; no pide motivo ni revisa si hay líneas ya en cocina; deja las líneas `comandado` vivas (no anuladas) y el pedido `cerrado` con su total. Efectos frente a v3:

- **US-09 CA3 / RN-072:** una anulación requiere permiso `venta.anular` y motivo. Con "Terminar pedido" un mozo cierra sin cobrar un pedido ya comandado, que es una anulación masiva sin permiso ni motivo.
- **RN-065 / US-09 CA2:** lo comandado y no cobrado debería quedar como consumo o pérdida; aquí no hay marca `anulado_post_comanda` ni descuento futuro.
- **RN-005 / RN-046:** el snapshot de precio, costo y receta se congela "al cerrar" (sprint 6 §3.3); este cierre no lo congela. Los reportes del sprint 14 (US-60) podrían contar estos `cerrado` como ventas, o dejarlos sin costo.
- **US-17 CA1 / RN-015:** hoy es coherente (no toca la cuenta del pensionista), pero un `cerrado` sin cobro de un pensionista es un consumo que nunca se cargó.
- Está declarada "provisional" (`pendientes`, `sprint-06b-lista-de-pedidos.md` §6), pero sin guarda.
- **Acción:** antes de cerrar el sprint 6, limitar "Terminar" a pedidos sin líneas comandadas, o exigir `venta.anular` más motivo del tipo `anulacion` cuando haya comandadas (y marcarlas `anulado_post_comanda`). Anotar en el plan del sprint 7 que el cierre con cobro reemplaza esta operación y define `anulado` de pedido entero.

### 3. US-07 CA2 ("unir dos mesas ocupadas") contradice la regla de unir

v3 CA2: Given dos mesas **ocupadas**, When las une, Then quedan unidas. Construido (`US-029`, `sprint-06b-lista-de-pedidos.md` §7): se une una mesa **libre** (secundaria, sin pedido, tampoco con pedido abierto sin comandar) a una principal; la principal puede o no tener pedido; no se unen dos mesas con pedido. Además "ocupada" solo significa pedido **comandado** (decisión del usuario 2026-10-01). Con esa decisión el CA2 literal no puede cumplirse, y el v2.0 tenía el mismo texto.
- Resto de US-07: CA1 (mover a mesa libre) cumple; CA3 (dividir por productos conservando detalle y precio) cumple, con recálculo solo si el origen o el destino es pensionista; CA4 cumple; CA5 (recalcular a qué cuenta va el cargo al cierre) se cumple en el precio, el cargo es del sprint 7.
- **Acción:** enmendar el CA2 del v3 ("una mesa libre se une a una principal; si la secundaria ya tiene pedido, primero se mueve o se termina") o abrir una historia para unir dos pedidos. Dejarlo registrado como decisión (DT) porque contradice la fuente.

### 4. US-08: comanda, impresora y sin Internet

- **CA1** pide pedido/mesa/canal, mozo, hora, productos por persona, componentes, observaciones y **tipo de entrega**. La vista imprimible muestra mesa o "Para llevar", pedido, servicio, hora de emisión, mozo, personas, componentes y notas. Dos matices: `mozo` es quien **abrió** el pedido (`ComandaDetallada`: "el usuario que abrió el pedido"), no quien comandó, y "tipo de entrega" no tiene campo propio (ver 5).
- **CA2** (una sola impresora central): hoy no hay impresora ni agente; es la impresora que elija el navegador. **CA5** (imprimir sin Internet): la vista funciona local, pero no hay prueba sin red, y HT-10 sigue pendiente. La hoja `@media print` ni siquiera se vio en vista previa (`US-030-US-031`, "No verificado").
- **CA3** y **CA4** cumplen (COPIA desde la segunda impresión con contador atómico; sin estado "listo").
- **Acción:** HT-10 sigue como lo previsto; sumarle "con Internet desconectado" y la vista previa de impresión. Decidir si el mozo de la comanda es el que comanda y guardarlo en la comanda.

### 5. US-06 CA6 "tipo de entrega" y canales

- El v3 CA6 pide que el mozo "agregue una observación y defina el tipo de entrega" **por producto** y que ambos lleguen a la comanda. Observación por línea: cumple. "Tipo de entrega" no existe como dato (V32 no tiene columna; el canal es del pedido: `mesa` o `para_llevar`); el v2.0 ya lo decía y la especificación fuente no lo define. Interpretación vigente: canal del pedido.
- Recojo programado y delivery (US-10 a US-14) están fuera por plan (sprint 12); los CA5 de táper para esos canales solo se cumplen para `para_llevar`.
- CA5: cuando el dueño no ha fijado el precio del táper, el pedido avisa `TAPER_SIN_MONTO` y suma cero; el v3 dice "debe añadir un cargo". Es el comportamiento acordado en sprint 5, pero queda un CA que solo se cumple con un dato pendiente (precio real del táper).
- **Acción:** definir con el dueño qué significa "tipo de entrega" (¿mesa/llevar por línea?) y anotarlo en `flujos/` y el modelo; hasta entonces cerrar US-06 CA6 como "canal del pedido".

### 6. US-04 CA2: comandas intactas al agotar

`US-015` declaró que la prueba de CA2 se agregaba "cuando existan las comandas" (`docs/pendientes`: "CA2 de US-015 ... sin prueba hasta que existan las comandas"). Las comandas existen desde el sprint 6; en `DisponibilidadTest` no hay un caso que agote un producto ya comandado y compruebe que la comanda y la línea siguen iguales (solo vi los nombres de las pruebas de esa clase). **Acción:** escribir esa prueba y cerrar el pendiente.

### 7. US-05: regla "ocupada al comandar"

v3 US-05 y RN-059 hablan de cinco estados; la decisión del 2026-10-01 conserva los cinco pero **ocupada = pedido comandado** y agrega una marca no-estado `tomandoPedido` (libre con pedido abierto). Compatible con RN-059, pero conviene decirlo: la regla escrita en v3 no define ocupada. Además "pendiente de cierre" (RN-059/US-05 CA1) hoy es inalcanzable: lo produce el cobro del sprint 7 (`resumen.porCobrar` = 0). El motivo del bloqueo (RF-028, que v3 menciona como sin definir) ya está resuelto: mozo, cajero y administradores, solo mesa libre y con motivo. **Acción:** registrar en el v3 (nota) o en el PRD; no requiere código.

### 8. US-15 CA1 y CA4: contraseña del portal

v3 CA1 exige registrar la contraseña del portal en el alta y CA4 que PIN y contraseña sean credenciales distintas. Construido: el PIN lo genera el sistema y se muestra una vez; la contraseña del portal **no se pide** (columna nula) hasta definir RF-027 (`sprint-05...` §3.2, desviación a propósito). También: el límite de crédito no es un campo de la ficha sino el tope (calculado o fijado) de la cuenta. **Acción:** mantener la desviación; marcar en el v3 que CA1 queda parcial hasta el sprint 13 (portal) y resolver RF-027 antes.

### 9. US-16: CA1 "por producto fijo" y CA2 "estado"

- CA1 pide "precios especiales por producto fijo de desayuno y cena". Construido: un precio por comida (desayuno, almuerzo, cena) por plan, decisión del usuario (el pensionista no elige; `sprint-05...` §3.4). Es una desviación de la fuente ya documentada; el v3 no la recoge.
- CA2 / RN-021 piden "vigencia, **estado** e historial". Hay vigencia e historial por versiones; el **estado** del plan (activo/inactivo) no existe ("Renombrar o desactivar un plan: ningún criterio lo pide", `US-017`), pero RN-021 sí lo pide. CA3, CA4 (extra a precio completo) y CA5 (mismo plan en todos los locales) cumplen.
- **Acción:** agregar desactivar plan (barato) antes de cargar los planes reales; registrar la desviación "por comida" en el v3.

### 10. US-17: tope y alerta

v3 usa "límite individual"; lo construido es un **tope** calculado (días de atención × comidas × precio) o fijado, solo recordatorio. CA2 (crédito disponible = límite − deuda) se cumple con ese tope; CA3, CA5 y CA6 cumplen (libro de solo agregado, saldo a favor primero, local por movimiento). CA1 y CA4 (cargo solo al cerrar en caja; alerta al cerrar) dependen de caja (sprint 7): no hay endpoint de consumo. Cambio menor, sin acción hasta el sprint 7.

### 11. US-54 CA3: el bloqueo del PIN falla con User-Agent largo

`bloqueo_pin.dispositivo` es `VARCHAR(120)` y el dispositivo es el User-Agent: con uno de más de 120 caracteres, un PIN incorrecto da 500 y el fallo no se cuenta, así que el bloqueo temporal de CA3 no se aplica a esos navegadores (`docs/pendientes`; DT-042 solo trunca `sesion.dispositivo`). Bug conocido, sin arreglar. El plazo del bloqueo sigue pendiente (RF-006). **Acción:** truncar o hashear el dispositivo en `LimitadorIntentosPin` y probar con un User-Agent de 300 caracteres.

### 12. US-57: inmutabilidad y el evento `transferencia`

- **CA4** (nadie puede modificar o eliminar un registro de auditoría): hoy es solo de aplicación (repositorio sin `delete*`, controlador solo GET, `InmutabilidadDeAuditoriaTest`); la base no lo impide (`sprint 3` lo dejó anotado). El libro de cuenta sí tiene disparador que rechaza `UPDATE`/`DELETE`. **Acción:** mismo disparador para `auditoria` (migración nueva), sprint 7.
- **CA1** (eventos): cubiertos hoy anulación, cambio de receta, cambio de precio, cambio de permisos; descuento, reapertura y cierre de caja (sprint 7), ajuste y transferencia de inventario (sprint 9), abono (sprint 8).
- **Evento sobrecargado:** el sprint 6 registra con el evento `transferencia` el traslado de líneas entre personas, mover pedido, unir y separar mesas (`TrasladarLineas`, `MoverPedidoDeMesa`, `UnirMesas`, `SepararMesa`; humanizador `descripcionDeTransferencia`). RN-004, US-45 y US-57 CA1 usan "transferencia de inventario" con el mismo nombre. Cuando llegue la transferencia entre locales (sprint 14) los filtros y reportes mezclarían las dos cosas. **Acción:** migrar a un evento propio (por ejemplo `traslado`) con la migración de CHECK que ya hará el sprint 7; las filas viejas se re-etiquetan por `entidad`.

### 13. US-37 CA6 y CA5

- **CA6** (sin permiso de ver costos no se ven costos "en una receta o en un reporte"): recetas cumplen (costos nulos y `costosVisibles=false`), pero P-30 sigue abierto: el catálogo de productos muestra el costo de los insumos a cualquiera con `catalogo.ver`, con lo que se reconstruye lo que CA6 oculta. Es brecha real, no nueva.
- **CA5** ("modificar la receta aplica solo a nuevas ventas"): la receta se edita en su lugar y sube `version`; no hay versiones históricas hasta que la venta congele su snapshot (sprint 7, junto con la brecha 2). **CA4** (recálculo por compra) es del sprint 9.
- **Acción:** esconder el costo del catálogo sin `recetas.ver_costos` (sprint 7).

### 14. Decisiones registradas que chocan con RN-007 y con CAs de desactivación

DT-043 (local) y DT-051 (producto, proveedor, categoría, rol y usuario) permiten borrar físicamente mientras no haya historial, aunque RN-007 dice "desactivar no elimina historial" y US-52 CA3, US-33 CA4, US-35 CA3 y US-55 CA2 piden conservarlo. Las protege la restricción `ON DELETE RESTRICT` y el chequeo previo; sigue siendo una contradicción con la regla escrita, asumida por el equipo. Dos matices: `EliminarProveedor` no revisa compras (no existen hasta el sprint 9), y el local inactivo se frena en el login por PIN y en el cambio de local, no está revisado qué pasa con una sesión que ya tiene ese local activo (US-52 CA3, "impedir nuevas operaciones"). Sin cambio de v3; **acción:** conservar el registro en el PRD como excepción y probar el caso de la sesión abierta.

### 15. Cambios menores que no requieren acción

- **US-02:** la transición a "en servicio" (v3 la marca como indefinida, RF-025) se resolvió: los estados borrador, confirmado, en servicio y pasada se **calculan por fecha**. CA1 a CA7 cumplen; CA7 (desayuno y cena sin programación, productos elegibles en el pedido) cumple con la carta y "Desayuno/Cena del plan".
- **US-03 / US-04:** precios reales del menú pendientes (RF-002); CA2 y CA3 de US-03 se cumplen en el motor de precios y la toma del pedido, el cobro es del sprint 7. CA1, CA3 y CA4 de US-04 cumplen (agotados por local, reactivación con motivo, "Se volvió a preparar" sembrado; no se puede desactivar el último motivo de disponibilidad).
- **US-36:** el catálogo de motivos es más amplio que v3 (8 tipos: se sumaron `disponibilidad` y `ajuste_cuenta`); CA2 y CA3 se demuestran en anulación y reactivación, y en ajustes de inventario hasta el sprint 9.
- **US-58:** DT-044 guarda horarios por día de la semana, con varios turnos y días especiales, que es más de lo que pide el v3; CA2 y CA3 son del kiosco (sprint 8).
- **US-53, US-56 y US-34:** CA4 de US-53 es del sprint 10; los 11 permisos mínimos de US-56 CA2 están y hay más acciones de módulo (`bloquear_mesa`, `marcar_agotado`, `administrar`), compatible con "al menos".

## Lista priorizada de brechas

### Antes de cerrar el sprint 6

1. **Terminar pedido** (sección 2): guarda de comandadas o permiso + motivo. Riesgo de fuga de ventas, el más alto.
2. **Regla de unir** (sección 3): enmendar US-07 CA2 y registrar la decisión en el log de decisiones.
3. **44 vs 56 px y teclado en cantidades** (sección 1): decidir y registrar; es lo único que el v3 cambió de verdad.
4. **Bug del PIN con User-Agent largo** (sección 11): arreglo pequeño y ya conocido; compromete US-54 CA3.
5. **Prueba de US-04 CA2** (sección 6): pendiente explícito de la historia.
6. **"Tipo de entrega" y "mozo de la comanda"** (secciones 4 y 5): definir; no requiere código si la respuesta es "canal del pedido".
7. **Documentos:** corregir HT-07 en `plan/01` y `plan/README`, y la nota de RN-058 / US-01; escribir la historia de cierre del sprint 6 incluyendo estas decisiones.
8. **HT-10** (impresora real, sin Internet, vista previa de impresión): sigue pendiente por hardware; no bloquea el cierre si el usuario lo acepta, pero debe constar.

### Sprint 7 en adelante

- Sprint 7 (caja): disparador de inmutabilidad en `auditoria` (US-57 CA4); renombrar el evento `transferencia` de líneas y mesas (US-57/US-45); ocultar costos del catálogo (P-30, US-37 CA6); snapshot de costo y receta al cerrar (US-37 CA5, RN-005); estado `anulado` del pedido entero; el cierre con cobro reemplaza a "Terminar pedido"; alerta del tope al cobrar (US-17 CA4) y cargo único al cerrar (US-17 CA1); desactivar plan tarifario (US-16 CA2).
- Sprint 8: kiosco y pre-ticket; teclado compartido para DNI y PIN del kiosco (US-01 CA3).
- Sprint 9: descuento de receta por anulación después de comandar (US-09 CA2), motivos en ajustes (US-36 CA2/CA3), chequeo de compras al eliminar proveedor (US-35 CA3).
- Sprint 10: autenticación local (US-53 CA4), impresión sin Internet verificada (US-08 CA5).
- Sprint 12: recojo y delivery sobre el pedido (US-06 CA5, CA6 en esos canales).
- Sprint 13: contraseña del portal (RF-027, US-15 CA1/CA4).
- Sprint 14: reporte de anulaciones después de comanda (US-09 CA4).
- Cuando el dueño responda: precios reales de menú, plan y táper (RF-002); sin ellos, US-03 CA2, US-06 CA5 y US-16 siguen dependiendo de datos inventados.


## Decisiones tomadas el 2026-10-05

- **Brecha 2 ("Terminar pedido"):** se espera al sprint 7; no se endurece antes. Ver `prd/05-enmiendas-al-backlog-v3.md`.
- **Brecha 3 (unir mesas):** se cambia el criterio, no el código. Ver el mismo documento.

## Arreglos hechos el 2026-10-05

- **US-54 CA3 (PIN con User-Agent largo):** `LimitadorIntentosPin` recorta la huella a 120 caracteres (la columna `bloqueo_pin.dispositivo` mide 120) y `Sesion` recorta el dispositivo a 255. Prueba nueva en `LimitadorIntentosPinTest` (falló con "value too long" antes del arreglo).
- **US-04 CA2:** `ComandasIntactasAlAgotarTest` (2 pruebas): agotar un producto ya comandado no toca la comanda ni la línea, y deja de ofrecerse en pedidos nuevos. Ya se cumplía; faltaba la prueba.
- **HT-07 -> US-01** corregido en `plan/01-plan-de-sprints.md` y `plan/README.md`.
- **Siguen abiertos:** "tipo de entrega" de la comanda (falta saber los valores que quiere el dueño) y los 56 px de US-01 (decisión de diseño, ver brecha 1).
