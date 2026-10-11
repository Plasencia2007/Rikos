# Sprint 6, entrega 3 — Comanda y anulaciones (US-030 y US-031) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que el mozo mande el pedido a cocina (comandar) y pueda imprimir la comanda en el navegador, con la leyenda COPIA desde la segunda impresión; y que quien tenga permiso anule una línea con motivo, antes o después de comandar, dejando todo en la bitácora.

**Architecture:** Backend en el módulo `venta`. Una migración nueva (V33) agrega `comanda`, `detalle_comanda`, las columnas de anulación de `detalle_pedido` y los eventos de bitácora. Casos de uso `ComandarPedido`, `RegistrarImpresion`, `ConsultaComandas` y `AnularLinea`, con `Resultado<T,E>` y su enum de error. El controlador agrega endpoints a `PedidoController` y uno nuevo, `ComandaController`. Frontend: botón "Comandar" en la pantalla del pedido, diálogo de anulación con motivo, y una vista de comanda imprimible (`window.print()` con CSS de impresión).

**Tech Stack:** Spring Boot 4.1.1 + Spring Modulith, JPA, Flyway, Postgres; React 19 + TypeScript.

**Spec:** `docs/producto/plan/sprint-06-vender-en-mesa.md` 3.6 (comanda), 3.7 (anular) y 5.3. Modelo: `docs/producto/modelo-de-datos/06-venta-pos-comandas--ep05.md` 8.4, 8.6, 8.7.

## Decisiones de esta entrega (a confirmar al revisar)

1. **Se comanda el pedido entero**, no por persona: toma todas las líneas `pendiente` de todas las personas y las pasa a `comandado`. Sin líneas pendientes no se puede comandar (`SIN_LINEAS_PENDIENTES`).
2. **Una comanda es una foto** (`detalle_comanda` guarda línea y cantidad). Agregar platos después y comandar otra vez crea la comanda 2 solo con lo nuevo (RN-062). El número es correlativo por pedido.
3. **Imprimir se registra aparte de comandar.** Comandar deja la comanda en `pendiente` con 0 impresiones. Cada impresión (`POST /comandas/{id}/impresiones`) suma 1 y la pone `impresa`. **Es COPIA si ya había al menos una impresión antes de esta** (RN-063): la primera sale limpia, desde la segunda lleva la leyenda.
4. **La comanda muestra el estado de esa foto**, no el actual del pedido: nombre del producto o descripción, componentes elegidos, nota y persona (etiqueta), tal como se mandó. Como `detalle_comanda` solo guarda línea y cantidad, la vista lee producto, componentes y nota de la línea (que no cambian después de comandar, porque una línea comandada no se modifica).
5. **Anular una línea comandada** exige `venta.anular` y un motivo del tipo `anulacion`; deja `anulado_post_comanda = true` (RN-065: la receta ya se gastó; el descuento de inventario es del sprint 9). **Una línea pendiente sigue teniendo "Quitar"** (borrado real, sin permiso, ya hecho en la 2A): es corregir un error antes de que cocina la vea. `AnularLinea` también acepta una pendiente (queda `anulado_post_comanda = false`) por si otro cliente lo pide, pero la pantalla solo ofrece "Anular" en las comandadas.
6. **Lo anulado no suma** al total pero sigue visible, tachado, con su motivo.
7. **Permisos:** comandar, imprimir y ver comandas con los existentes (`venta.crear`, `venta.crear`, `venta.ver`); anular con `venta.anular` (cajero y administradores; el mozo no, salvo que se lo den desde Roles).
8. **Bitácora:** eventos nuevos `comanda_emitida` y `reimpresion_comanda` (solo copias); `anulacion` ya existe en la restricción.

## Global Constraints

- **`ULID` de 26 caracteres**, generado por el cliente donde el caso de uso es idempotente (`comanda.id`), como en 2A.
- **`Resultado<T,E>`** para errores esperados; nada de excepciones de negocio.
- **Flyway manda:** V33 es nueva; no se edita V32 ni anteriores. Nunca `DROP TABLE`.
- **TDD real:** cada caso de uso lleva su prueba escrita primero y vista fallar. Una prueba por rol en cada endpoint nuevo.
- **Auditoría:** `RegistradorAuditoria` resuelve usuario y local solo. Las pruebas de casos de uso auditados necesitan `ContextoDeSeguridadDePrueba.autenticarComo(...)` en `@BeforeEach`.
- **Spring Modulith:** `ArquitecturaDeModulosTest` solo puede fallar por el ciclo conocido `comun ↔ identidad`. Si `venta` necesita `Motivo` de `catalogo`, ya está expuesto (`catalogo.infraestructura` tiene `@NamedInterface`).
- **Concurrencia:** las escrituras del controlador van con `protegido(...)` (409 en choques), como en 2A. Comandar dos veces a la vez no debe duplicar comandas ni líneas.
- **`tsc -b --force`, `npm run lint`**, y verificación en el navegador (escritorio, tablet, móvil). Sin commits salvo que el usuario lo pida.
- **Antes de tocar el backend**, detener el 8081 (recompilar con el backend corriendo deja `target/classes` incompleto).
- **Docker/Postgres:** si algo falla con "Connection refused", `docker ps` primero.

## Review Focus

- **Doble comandar:** dos peticiones simultáneas no crean dos comandas con las mismas líneas ni pasan una línea dos veces; reenviar el mismo `id` de comanda devuelve la misma.
- **Una línea comandada nunca cambia:** modificar, quitar y reasignar siguen rechazadas (`LINEA_YA_COMANDADA`); solo se anula.
- **Anular sin permiso o sin motivo** no cambia nada; un motivo de otro tipo o inexistente se rechaza; una línea de otro pedido o de otro local es 404.
- **Totales:** una línea anulada no suma en persona, pedido ni táper; el pedido con todo anulado queda en S/ 0.00 sin error.
- **COPIA exacta:** la primera impresión no lleva la leyenda, la segunda sí, aunque dos personas impriman a la vez (el contador no se pierde).
- **Estado del pedido:** pasa a `comandado` al comandar y las mesas siguen ocupadas; agregar líneas después sigue permitido.
- **Bitácora legible** en español y con producto y motivo.

---

## File Structure

**Backend (`rikos-backend/servicio-operacion/src`)**
- `main/resources/db/migration/V33__comandas_y_anulaciones.sql`
- `main/java/pe/rikos/operacion/venta/infraestructura/{Comanda,DetalleComanda,ComandaRepositorio,DetalleComandaRepositorio}.java`; `DetallePedido` gana los campos de anulación.
- `main/java/pe/rikos/operacion/venta/aplicacion/{ErrorComanda,ComandarPedido,RegistrarImpresion,ConsultaComandas,ComandaDetallada,LineaDeComanda,ImpresionRegistrada,AnularLinea,SolicitudDeAnulacion}.java`; `ErrorPedido` gana `LINEA_ANULADA`, `MOTIVO_*` según se decida; `ConsultaPedidos`/`LineaDetallada` exponen `anuladaPostComanda`, `motivoAnulacion`.
- `main/java/pe/rikos/operacion/venta/api/{PedidoController,ComandaController}.java`
- `main/java/pe/rikos/operacion/identidad/.../ListarAuditoria` (frases nuevas).
- `test/java/pe/rikos/operacion/venta/{TablasDeComandaTest,ComandarPedidoTest,RegistrarImpresionTest,AnularLineaTest,ConsultaDeComandasTest}.java`, `api/{ComandasAccesoPorRolTest,AnulacionesAccesoPorRolTest,ComandasConcurrenciaTest}.java`.

**Frontend (`rikos-web/src`)**
- `api/cliente.ts` (comandas, anulación, `listarMotivos` por tipo si no existe).
- `features/venta/{PantallaPedido,DialogoAnular,VistaComanda,mensajes}.tsx|ts`, `estilos/global.css` (clases `com-*` y `@media print`).

---

### Task 1: Migración V33 y entidades

**Files:** Create `V33__comandas_y_anulaciones.sql`, `Comanda.java`, `DetalleComanda.java`, sus repositorios; Modify `DetallePedido.java`; Test `TablasDeComandaTest`.

**Interfaces:**
- Produces: tablas `comanda(id, pedido_id, local_id, numero, emitida_en, emitida_por, impresiones, ultima_impresion_en, estado_impresion)` con `UNIQUE(pedido_id, numero)`, `impresiones >= 0`, `estado_impresion IN ('pendiente','impresa','fallida')`; `detalle_comanda(id, comanda_id, detalle_pedido_id, cantidad>0)` con `UNIQUE(detalle_pedido_id)` (una línea sale en una sola comanda); columnas de `detalle_pedido`: `anulado_en`, `anulado_por`, `motivo_anulacion_id` (FK `motivo`), `anulado_post_comanda BOOLEAN NOT NULL DEFAULT FALSE`, y `CHECK (estado <> 'anulado' OR (anulado_en IS NOT NULL AND anulado_por IS NOT NULL AND motivo_anulacion_id IS NOT NULL))`; eventos de bitácora `comanda_emitida`, `reimpresion_comanda` agregados a `auditoria_evento_check`.
- Produces: `Comanda` con `id, pedidoId, localId, numero, emitidaEn, emitidaPor, impresiones, ultimaImpresionEn, estadoImpresion, @Version`; `ComandaRepositorio.findByPedidoIdOrderByNumero`, `findByIdAndLocalId`, `maxNumero(pedidoId)`.

- [ ] **Step 1: Write the failing test** — `TablasDeComandaTest` (mismo estilo de `TablasDePedidoTest`, una violación por prueba): número repetido en el mismo pedido; una línea en dos comandas; `impresiones` negativas; línea anulada sin motivo; evento `comanda_emitida` aceptado por la bitácora.
- [ ] **Step 2: Run, verify FAIL** (`../mvnw test -Dtest=TablasDeComandaTest` desde `servicio-operacion`): tablas inexistentes.
- [ ] **Step 3: Write V33 + entidades + repositorios.**
- [ ] **Step 4: Run, verify PASS.** Comprobar que la migración corre sobre la base de desarrollo al reiniciar el backend.
- [ ] **Step 5: Commit** (solo si el usuario lo pide).

---

### Task 2: `ComandarPedido`

**Files:** Create `ErrorComanda`, `ComandarPedido`, `ComandaDetallada`, `LineaDeComanda`; Test `ComandarPedidoTest`.

**Interfaces:**
- Produces: `Resultado<ComandaDetallada, ErrorComanda> ComandarPedido.ejecutar(String pedidoId, String comandaId /* nullable: el servidor genera */, String actorId)`.
- `ErrorComanda`: `NO_ENCONTRADO, SIN_LOCAL_ACTIVO, SIN_LINEAS_PENDIENTES, PEDIDO_NO_COMANDABLE` (estado distinto de abierto/comandado), `COMANDA_NO_ENCONTRADA`, `LINEA_NO_ENCONTRADA`, `LINEA_ANULADA`, `MOTIVO_NO_ENCONTRADO`, `MOTIVO_INVALIDO`, `MOTIVO_OBSERVACION_OBLIGATORIA`.
- Efectos: crea `comanda` número siguiente, un `detalle_comanda` por línea pendiente con su cantidad, pasa las líneas a `comandado`, el pedido a `comandado`, bitácora `comanda_emitida` ("… comandó el pedido 0001: 3 productos (comanda 1)").

- [ ] **Step 1: Write the failing tests** (`ComandarPedidoTest`, patrón de `PedidosBase`): (a) comandar pasa las líneas de todas las personas a `comandado` y crea la comanda 1 con su foto; (b) sin líneas pendientes → `SIN_LINEAS_PENDIENTES`; (c) agregar una línea después y comandar crea la comanda 2 solo con la nueva; (d) reenviar el mismo `comandaId` devuelve la misma comanda sin duplicar; (e) un `comandaId` que existe en otro pedido o local → `NO_ENCONTRADO` (lección del Critical de 2A: `save()` con id asignado hace *merge*; comprobar `findById` global antes); (f) pedido de otro local → `NO_ENCONTRADO`; (g) la bitácora registra el evento; (h) el pedido queda `comandado` y la mesa sigue ocupada.
- [ ] **Step 2: Run, verify FAIL.**
- [ ] **Step 3: Implement** (`@Transactional`; `LocalDeSesion.actual()`; calcular número con `maxNumero + 1`; el controlador reintenta hasta 3 veces ante `comanda_numero_unico` / `*_pkey`, como en 2A).
- [ ] **Step 4: Run, verify PASS.**
- [ ] **Step 5: Commit** (solo si el usuario lo pide).

---

### Task 3: `RegistrarImpresion` y `ConsultaComandas`

**Files:** Create `RegistrarImpresion`, `ImpresionRegistrada`, `ConsultaComandas`; Test `RegistrarImpresionTest`, `ConsultaDeComandasTest`.

**Interfaces:**
- `ConsultaComandas.delPedido(pedidoId)` → lista de resúmenes (número, emitida en, emitida por, impresiones, estado); `ConsultaComandas.detalle(comandaId)` → `ComandaDetallada(id, numero, pedidoNumero, mesa|canal, servicio, mozo, emitidaEn, impresiones, personas[{etiqueta, lineas[{cantidad, nombre, componentes[], observacion}]}])`.
- `RegistrarImpresion.ejecutar(comandaId, actorId)` → `Resultado<ImpresionRegistrada, ErrorComanda>` con `ImpresionRegistrada(comanda, copia: boolean)`; `copia = impresionesAntes >= 1`. Actualiza contador con **UPDATE atómico** (`impresiones = impresiones + 1`) para no perder cuentas simultáneas; bitácora `reimpresion_comanda` solo si es copia.

- [ ] **Step 1: Write the failing tests:** primera impresión → `copia=false`, `impresiones=1`, `impresa`; segunda → `copia=true`, `impresiones=2`, bitácora con el evento; dos impresiones "simultáneas" (dos llamadas seguidas sobre la misma fila) cuentan 2; comanda de otro local → `COMANDA_NO_ENCONTRADA`; el detalle refleja componentes, nota y etiqueta de persona; la lista está ordenada por número.
- [ ] **Step 2: Run, verify FAIL.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run, verify PASS.**
- [ ] **Step 5: Commit** (solo si el usuario lo pide).

---

### Task 4: `AnularLinea` y el pedido con anuladas

**Files:** Create `AnularLinea`, `SolicitudDeAnulacion`; Modify `DetallePedido`, `LineaDetallada`, `ConsultaPedidos`, `ErrorPedido`/`ErrorComanda`, `ListarAuditoria`; Test `AnularLineaTest`, extend `ConsultaDelPedidoTest`.

**Interfaces:**
- `AnularLinea.ejecutar(pedidoId, lineaId, motivoId, observacion, actorId)` → `Resultado<Void, ErrorComanda>`. Reglas: la línea es del pedido y del local; no está ya anulada (`LINEA_ANULADA`); el motivo existe, está activo y es de tipo `anulacion` (`MOTIVO_NO_ENCONTRADO`/`MOTIVO_INVALIDO`); si el motivo `requiereObservacion`, la observación es obligatoria. Marca `estado='anulado'`, `anulado_en`, `anulado_por`, `motivo_anulacion_id`, `anulado_post_comanda = (estado anterior == 'comandado')`. Bitácora `anulacion` ("… anuló 'Menú normal' del pedido 0001 (después de comandar): <motivo>").
- `LineaDetallada` gana `anuladaPostComanda` y `motivoAnulacion` (descripción); `ConsultaPedidos` sigue excluyendo lo anulado de subtotal, táper y total.
- **El permiso `venta.anular` se exige en el controlador** (`@PreAuthorize`/matcher como en 2A); el caso de uso no lo repite.

- [ ] **Step 1: Write the failing tests:** anular una comandada → post_comanda=true, no suma, sigue visible con motivo; anular una pendiente → post_comanda=false; ya anulada → `LINEA_ANULADA`; motivo de otro tipo → `MOTIVO_INVALIDO`; motivo que exige observación sin ella → error; el total del pedido baja por lo anulado y con todo anulado es 0 sin fallar; pedido de otro local → `NO_ENCONTRADO`; una línea anulada tampoco cuenta para el "segundo consumo del día" (ya cubierto en 2A, se comprueba); la bitácora sale con la frase.
- [ ] **Step 2: Run, verify FAIL.**
- [ ] **Step 3: Implement.** Si no existe ningún motivo de tipo `anulacion` en la base de desarrollo, sembrar dos ("Error del mozo", "Cliente se retiró") **desde la pantalla/API de Motivos existente**, no por migración (son datos del dueño).
- [ ] **Step 4: Run, verify PASS.**
- [ ] **Step 5: Commit** (solo si el usuario lo pide).

---

### Task 5: La API y los permisos

**Files:** Modify `PedidoController`, `ConfiguracionSeguridad` (si hace falta el matcher), Create `ComandaController`; Test `ComandasAccesoPorRolTest`, `AnulacionesAccesoPorRolTest`, `ComandasConcurrenciaTest`.

**Endpoints:**
- `POST /pedidos/{id}/comandas {id?}` → 201 con la comanda (`venta.crear`).
- `GET /pedidos/{id}/comandas` → lista (`venta.ver`).
- `GET /comandas/{id}` → detalle imprimible (`venta.ver`).
- `POST /comandas/{id}/impresiones` → `{comanda, copia}` (`venta.crear`).
- `POST /pedidos/{id}/lineas/{lineaId}/anular {motivoId, observacion?}` → 200 con el **pedido completo** (`venta.anular`).
- Errores: 404 (`NO_ENCONTRADO`, `COMANDA_NO_ENCONTRADA`, `LINEA_NO_ENCONTRADA`), 409 (`SIN_LINEAS_PENDIENTES`, `LINEA_ANULADA`, `PEDIDO_MODIFICADO`, `NUMERO_DE_COMANDA_REPETIDO`), 422 (motivos), 403 sin permiso. Escrituras con `protegido(...)`.

- [ ] **Step 1: Write the failing tests:** por rol — mozo comanda e imprime pero **no anula** (403); cajero y administradores anulan; cocina y motorizado ven comandas (`venta.ver`) pero no comandan, imprimen ni anulan; otro local → 404 en cada verbo; comandar dos veces seguidas en paralelo deja una sola comanda con las líneas y la otra recibe `SIN_LINEAS_PENDIENTES`.
- [ ] **Step 2: Run, verify FAIL.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run, verify PASS; luego la suite completa** (`../mvnw test` desde `servicio-operacion`, sumar surefire; `ArquitecturaDeModulosTest` solo puede fallar por el ciclo conocido). Regenerar los tipos del frontend (`npm run generar-tipos`).
- [ ] **Step 5: Commit** (solo si el usuario lo pide).

---

### Task 6: Frontend — comandar, anular y la vista imprimible

**Files:** Modify `api/cliente.ts`, `features/venta/PantallaPedido.tsx`, `features/venta/mensajes.ts`, `features/venta/index.ts`, `estilos/global.css`; Create `DialogoAnular.tsx`, `VistaComanda.tsx`.

**Interfaces:**
- `api/cliente.ts`: `comandarPedido(token, pedidoId)`, `listarComandas(token, pedidoId)`, `obtenerComanda(token, id)`, `registrarImpresion(token, comandaId)`, `anularLinea(token, pedidoId, lineaId, {motivoId, observacion})`, y `listarMotivosPorTipo(token, 'anulacion')` (reusar el de Motivos si ya existe); tipos `Comanda`, `ComandaDetalle`, `LineaDelPedido` con `anuladaPostComanda` y `motivoAnulacion`.
- `PantallaPedido`: el botón **"Comandar"** (habilitado si hay líneas pendientes) llama a `comandarPedido` y abre `VistaComanda`; las líneas comandadas muestran la insignia "En cocina" y el botón **"Anular"** (solo con `venta.anular`, que se lee con `permisosPropios`); las anuladas se ven tachadas con su motivo; una lista "Comandas" del pedido permite **reabrir/reimprimir** cada una.
- `VistaComanda`: diálogo con la comanda (mesa o para llevar, pedido, mozo, hora, personas con productos, componentes y notas). **Imprimir** llama a `registrarImpresion` y *después* `window.print()`; si la respuesta trae `copia: true` muestra la leyenda **COPIA** antes de imprimir. CSS `@media print` oculta todo salvo la comanda.
- `DialogoAnular`: elige el motivo (lista de tipo `anulacion`), nota si el motivo la exige, confirma.

- [ ] **Step 1: Write the failing check:** importar `DialogoAnular` y `VistaComanda` en `PantallaPedido.tsx` antes de crearlos; `npx tsc -b --force` falla.
- [ ] **Step 2: Implement api + componentes + estilos.**
- [ ] **Step 3: Run:** `npx tsc -b --force` y `npm run lint` (solo puede quedar el error ajeno de `HorarioSemanal.tsx`).
- [ ] **Step 4: Verificar en el navegador** (ver Task 7).

---

### Task 7: Verificación y cierre

- [ ] **Step 1:** Reiniciar backend (8081) con V33; comprobar que migró. Crear al menos dos motivos de anulación desde el panel si no hay.
- [ ] **Step 2: Recorrido como mozo (POS):** abrir mesa, agregar dos productos, **Comandar** → la vista muestra la comanda 1 sin COPIA; imprimir (la pestaña de impresión del navegador se abre; comprobar `window.print` invocado y `impresiones = 1`); reabrir e imprimir otra vez → aparece **COPIA**. Agregar otro producto, comandar → comanda 2 con solo lo nuevo. El mozo **no ve** "Anular".
- [ ] **Step 3: Recorrido como cajero o administrador:** anular una línea comandada con motivo → tachada, total baja; intentar sin motivo → mensaje en español; ver la bitácora en el panel con la frase.
- [ ] **Step 4: Bordes:** comandar sin pendientes (botón deshabilitado); línea comandada sin "Quitar" ni "−/+"; pedido con todo anulado (total 0.00). Móvil 375 y tablet 768 sin desborde; impresión con `@media print` (`emulateMedia print` o `window.matchMedia`).
- [ ] **Step 5:** Suite completa del backend y `tsc`/lint del frontend.
- [ ] **Step 6:** Documentar: `rikos-backend/doc/historias/US-030-US-031-comanda-y-anulaciones.md`, actualizar `historias/README.md`, `sprint-06` (entrega 3 ✅), `plan/README.md`, `ESTADO.md` y `docs/pendientes/README.md` (HT-10 sigue pendiente; minors que salgan). Limpiar los pedidos de prueba.
- [ ] **Step 7: Revisión independiente** (agente opus) y un pase de arreglos con pruebas, como en 2A.

---

## Self-review

**Cobertura de la especificación (3.6 y 3.7):** comandar con foto y comandas adicionales (Tasks 1–2); vista imprimible con pedido, mesa/canal, mozo, hora, productos por persona, componentes y observaciones (Task 3 y 6); registro de impresiones y COPIA desde la segunda (Task 3); sin estado "listo" (no se modela); `estado_impresion` pendiente/impresa (Task 1 y 3); anular con permiso y motivo del tipo `anulacion`, antes y después de comandar con `anulado_post_comanda` (Task 4); bitácora (Tasks 2–4); pantallas con sus cuatro estados (Task 6). **Fuera:** reporte de anulaciones (sprint 14), descuento real de inventario (sprint 9), prueba con la impresora real (HT-10), cobrar (sprint 7).

**Riesgos abiertos:** (1) el tipo tiene que ser `anulacion` y puede no haber motivos cargados; (2) `window.print()` se verifica solo hasta que el navegador abre el diálogo (la impresora real es HT-10); (3) `detalle_comanda` con `UNIQUE(detalle_pedido_id)` asume que una línea nunca vuelve a comandarse: si la entrega 4 (reorganizar) moviera líneas comandadas, hay que recordar que conservan su lugar en la comanda que salió (spec 3.8).

**Consistencia:** los nombres `ComandarPedido`, `RegistrarImpresion`, `ConsultaComandas`, `AnularLinea`, `ErrorComanda` y los endpoints de la Task 5 son los que consumen la Task 6 y las pruebas de la Task 5.
