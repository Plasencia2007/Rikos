# Sprint 6, entrega 4 — Reorganizar cuentas (US-029) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Que el mozo reorganice una mesa sin perder nada: pasar productos de una persona a otra (o a una persona nueva, que es "dividir la cuenta"), con el precio recalculado si cambia el tipo de cliente; mover el pedido a otra mesa libre; y unir mesas para atender un grupo grande, y separarlas después.

**Architecture:** Backend en el módulo `venta`, tres casos de uso nuevos (`TrasladarLineas`, `MoverPedidoDeMesa`, `UnirMesas`/`SepararMesa`) sobre el modelo que ya existe (`mesa.mesa_principal_id`, `detalle_pedido.cuenta_persona_id`). Una migración (V36) congela la etiqueta de la persona en la foto de la comanda, porque mover una línea comandada no debe cambiar la comanda que ya salió. Frontend: todo se opera **desde la pantalla del pedido** (botones "Pasar a…", "Mover a otra mesa", "Unir mesa"); el mapa de mesas (del otro chat de diseño) no se toca.

**Tech Stack:** Spring Boot 4.1.1 + Spring Modulith, JPA, Flyway, Postgres; React 19 + TypeScript.

**Spec:** `docs/producto/plan/sprint-06-vender-en-mesa.md` 3.8. Sigue al pie de la letra: trasladar y dividir son la misma operación, el recálculo depende del tipo de la persona de destino, una línea comandada conserva su lugar en la comanda que salió, y todo vale solo antes del cierre.

## Decisiones de esta entrega (a confirmar al revisar)

1. **Un solo caso de uso para trasladar y dividir.** `TrasladarLineas(pedido, lineas[], destino)`: el destino es una persona existente del mismo pedido o "persona nueva". Dividir la cuenta es trasladar a una persona nueva.
2. **Qué se puede mover:** líneas pendientes y comandadas. **Una línea anulada no** (`LINEA_ANULADA`). Una persona nueva sale con la etiqueta siguiente ("Persona N+1").
3. **Recálculo con el motor, solo donde el precio depende de la persona:**
   - **Menú normal** (`menu_normal`): el precio cambia entre cliente normal (precio público) y pensionista (precio de su plan, con "segundo del día" si ya consumió). Se recalcula con `CalculadorDePrecioDeLinea`, que también devuelve el táper.
   - **Comida del plan** (`plan_desayuno`, `plan_cena`): solo tiene sentido para un pensionista; moverla a una persona normal da `SOLO_PARA_PENSIONISTA`. Entre pensionistas se recalcula con el plan del destino.
   - **Menú extra, carta y extra libre:** el precio **no** depende de la persona; se conserva el que se guardó al agregarla (el extra libre es manual, nunca se toca).
4. **La comanda que salió no cambia.** `detalle_comanda` guarda la etiqueta de la persona **al comandar** (`etiqueta_persona`, migración V36, con relleno de lo ya existente); la vista imprimible usa esa etiqueta, no la persona actual de la línea.
5. **Mover el pedido a otra mesa:** solo un pedido de mesa en curso (`abierto` o `comandado`), a una mesa **libre** (activa, sin bloqueo, sin pedido, no unida). El pedido queda con la mesa nueva y la vieja vuelve a libre sola (el estado se deriva).
6. **Unir mesas:** se une una mesa **secundaria libre** (sin pedido, sin bloqueo, activa, que no sea principal de otras) a una **principal** (activa, no bloqueada, no unida a otra). La principal puede tener o no pedido; **el pedido de la principal se opera por las dos**: `GET /mesas/{secundaria}/pedido` devuelve el pedido de su principal. **Separar** deja la secundaria independiente (`mesa_principal_id` en null).
7. **Reglas de seguridad de la unión:** no se abre un pedido en una mesa unida (ya existía); una principal con mesas unidas no se bloquea ni se desactiva hasta separarlas (`TIENE_MESAS_UNIDAS`); una secundaria no se une a sí misma ni a una secundaria.
8. **Permisos:** todo con `venta.modificar` (el mozo ya lo tiene por la migración V31; cajero y administradores también).
9. **Bitácora:** el evento `transferencia` (ya permitido por la restricción) cubre los tres casos, con frases distintas: "pasó 2 productos de "Persona 1" a "Persona 2" en el pedido "0004"", "movió el pedido "0004" de la mesa 3 a la mesa 5", "unió la mesa 4 a la mesa 3" / "separó la mesa 4 de la mesa 3".
10. **Solo antes del cierre.** El pedido en `pendiente_cierre` o más allá no se reorganiza (`PEDIDO_NO_REORGANIZABLE`).

## Global Constraints

- **`Resultado<T,E>`**, `ULID`, Flyway (V36 nueva; nunca se edita una aplicada), TDD real con RED visto antes de implementar, una prueba por rol en cada endpoint nuevo.
- **`RegistradorAuditoria`** resuelve usuario y local solo; las pruebas usan `ContextoDeSeguridadDePrueba.autenticarComo(...)`.
- **Spring Modulith:** `ArquitecturaDeModulosTest` solo puede fallar por el ciclo conocido `comun ↔ identidad`.
- **Concurrencia:** las escrituras van con `protegido(...)` (409 en choques) y las reglas de unicidad de la base (`ux_pedido_mesa_en_curso`) respaldan "mover a una mesa libre": dos mozos moviendo a la misma mesa a la vez, uno gana y el otro recibe 409 `MESA_OCUPADA`.
- **Sin pisar al otro chat:** `MapaDeMesas.tsx`, `estilos/mapa.css` y `features/salones/**` son de la sesión de diseño. Esta entrega solo toca `PantallaPedido`, los diálogos de `features/venta` que son míos, `api/cliente.ts` y el backend de `venta`.
- **Datos de desarrollo:** el pedido 0001 a 0003 son de `jhon`; las pruebas manuales usan sus propios pedidos y se limpian solo esos.
- Antes de compilar con Maven conviene que no haya otro `mvn` ni un backend reiniciándose; si Docker se cayó, abrirlo y esperar `healthy`. Sin commits salvo que el usuario lo pida.

## Review Focus

- **Dinero:** mover una línea nunca cambia el total del pedido salvo el recálculo legítimo; la suma por persona sigue igual al total; una línea anulada no viaja.
- **Recálculo exacto:** menú normal de cliente normal → pensionista baja al precio del plan, y de vuelta sube al público; el "segundo del día" se marca bien y no se cuenta dos veces; la comida del plan no pasa a una persona normal.
- **La comanda ya impresa no cambia** aunque se mueva la línea (foto con la etiqueta original).
- **Mover el pedido:** una mesa ocupada, bloqueada, unida o de otro local se rechaza; la mesa vieja queda libre; un pedido para llevar no se mueve.
- **Unir y separar:** las reglas de 6 y 7; el pedido se ve por la principal y por la secundaria; separar deja todo como estaba.
- **Un pedido, una persona o una línea de otro local** son 404 en cada verbo.

---

## File Structure

**Backend (`rikos-backend/servicio-operacion/src`)**
- `main/resources/db/migration/V36__etiqueta_persona_en_la_comanda.sql`
- `main/java/pe/rikos/operacion/venta/infraestructura/{DetalleComanda,Mesa}.java` (+ `DetallePedido.pasarA(...)`, `Pedido.moverAMesa(...)`), repositorios.
- `main/java/pe/rikos/operacion/venta/aplicacion/{TrasladarLineas,SolicitudDeTraslado,MoverPedidoDeMesa,UnirMesas,SepararMesa}.java`, `ErrorPedido`/`ErrorMesa` (códigos nuevos), `ComandarPedido` (guarda la etiqueta), `ConsultaComandas` (la usa), `ConsultaPedidos.abiertoDeMesa` (pedido de la principal), `BloquearMesa`/`CambiarEstadoMesa` (regla de mesas unidas), `ListarAuditoria` (frases).
- `main/java/pe/rikos/operacion/venta/api/{PedidoController,MesaController}.java`
- `test/java/pe/rikos/operacion/venta/aplicacion/{TablasDeReorganizacionTest,TrasladarLineasTest,RecalculoAlTrasladarTest,MoverPedidoDeMesaTest,UnirMesasTest}.java`, `api/ReorganizarAccesoPorRolTest.java`.

**Frontend (`rikos-web/src`)**
- `api/cliente.ts`, `features/venta/{PantallaPedido,DialogoPasarA,DialogoMoverMesa,DialogoUnirMesa,mensajes}.tsx|ts`, `estilos/global.css` (solo clases `ped-*` nuevas).

---

### Task 1: Migración V36 — la comanda guarda la etiqueta de la persona

**Files:** Create `V36__etiqueta_persona_en_la_comanda.sql`; Modify `DetalleComanda`, `ComandarPedido`, `ConsultaComandas`; Test `TablasDeReorganizacionTest`, extend `ComandarPedidoTest`/`ConsultaDeComandasTest`.

**Interfaces:**
- `detalle_comanda.etiqueta_persona VARCHAR(120) NOT NULL` (relleno de lo existente con la etiqueta actual de la persona de la línea; `NOT NULL` después del relleno).
- `DetalleComanda(id, comandaId, detallePedidoId, cantidad, etiquetaPersona)`; `ComandarPedido` la llena con la etiqueta de la persona de cada línea **en ese momento**; `ConsultaComandas.detalle` agrupa por `etiquetaPersona` (en el orden de las personas del pedido al comandar).

- [ ] **Step 1: Write the failing tests:** una comanda guarda la etiqueta; si después la línea se pasa a otra persona (se simula con un `UPDATE`), el detalle imprimible sigue mostrando la etiqueta original; las comandas ya existentes quedan con etiqueta tras la migración.
- [ ] **Step 2: Run, verify FAIL.** (`cd servicio-operacion && ../mvnw -q test -Dtest=...`)
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run, verify PASS;** reiniciar el backend y comprobar que V36 migra la base de desarrollo con sus comandas.
- [ ] **Step 5: Commit** (solo si el usuario lo pide).

---

### Task 2: `TrasladarLineas` con recálculo

**Files:** Create `TrasladarLineas`, `SolicitudDeTraslado`; Modify `DetallePedido` (`pasarA(persona, precio, taper, origen, planId, segundo)`), `ErrorPedido`; Test `TrasladarLineasTest`, `RecalculoAlTrasladarTest`.

**Interfaces:**
- `TrasladarLineas.ejecutar(String pedidoId, List<String> lineasIds, String personaDestinoId /* null = persona nueva */, String actorId)` → `Resultado<PedidoDetallado-o-Pedido, ErrorPedido>`.
- `ErrorPedido` nuevos: `PEDIDO_NO_REORGANIZABLE`, `SIN_LINEAS_PARA_MOVER` (lista vacía), `MISMO_DESTINO` (todas ya están en el destino), `SOLO_PARA_PENSIONISTA`.
- Reglas: todas las líneas son del pedido y del local (si alguna no, `LINEA_NO_ENCONTRADA` y no se mueve **ninguna**); ninguna anulada (`LINEA_ANULADA`); el destino es del mismo pedido (`PERSONA_NO_ENCONTRADA`); una persona nueva toma la etiqueta "Persona N+1" y el orden siguiente; todo en **una sola transacción** (o se mueven todas o ninguna).
- Recálculo (decisión 3) con `CalculadorDePrecioDeLinea.calcular(...)` para `menu_normal` y `plan_*`; las demás líneas conservan su precio. Si el motor no puede calcular (plan sin precio), error `PLAN_SIN_PRECIO` y no se mueve nada.
- Una persona que queda **sin líneas** se conserva (no se borra sola): el mozo la quita con "Quitar esta persona".
- Bitácora `transferencia` con la frase de la decisión 9.

- [ ] **Step 1: Write the failing tests** (`TrasladarLineasTest`): mover una línea a otra persona; mover varias; dividir a una persona nueva (etiqueta y orden); lista vacía; línea de otro pedido/local sin mover ninguna; línea anulada; destino de otro pedido; mismo destino; pedido `pendiente_cierre`; el total del pedido no cambia si el tipo de persona es el mismo; la bitácora. (`RecalculoAlTrasladarTest`): menú normal normal→pensionista baja al plan y marca `segundoDelDia` si ya consumió; pensionista→normal sube al público y quita `segundo`; pensionista→pensionista con otro plan; comida del plan a persona normal → `SOLO_PARA_PENSIONISTA`; carta y extra libre conservan su precio; táper para llevar se recalcula con el menú.
- [ ] **Step 2: Run, verify FAIL.**
- [ ] **Step 3: Implement.**
- [ ] **Step 4: Run, verify PASS.**
- [ ] **Step 5: Commit** (solo si el usuario lo pide).

---

### Task 3: `MoverPedidoDeMesa`

**Files:** Create `MoverPedidoDeMesa`; Modify `Pedido` (`moverAMesa(mesaId)`); Test `MoverPedidoDeMesaTest`.

**Interfaces:** `ejecutar(String pedidoId, String mesaDestinoId, String actorId)` → `Resultado<Pedido, ErrorPedido>`. Reglas de la decisión 5; errores `MESA_NO_ENCONTRADA`, `MESA_NO_DISPONIBLE` (inactiva, bloqueada o unida), `MESA_OCUPADA`, `PEDIDO_NO_REORGANIZABLE`, `CANAL_INVALIDO` (para llevar), `MISMO_DESTINO`. Bitácora `transferencia`.

- [ ] **Step 1: Write the failing tests:** la mesa nueva queda ocupada y la vieja libre en `ConsultaMesas`; mesa ocupada/bloqueada/unida/inactiva/de otro local/inexistente rechazadas; misma mesa; para llevar; `pendiente_cierre`; pedido de otro local → `NO_ENCONTRADO`; la bitácora dice de qué mesa a cuál.
- [ ] **Step 2: Run, verify FAIL.** **Step 3: Implement.** **Step 4: Run, verify PASS.**
- [ ] **Step 5: Commit** (solo si el usuario lo pide).

---

### Task 4: `UnirMesas` y `SepararMesa`

**Files:** Create `UnirMesas`, `SepararMesa`; Modify `Mesa` (`unirA(principalId)`, `separar()`), `ErrorMesa` (`MESA_NO_LIBRE`, `PRINCIPAL_INVALIDA`, `YA_UNIDA`, `NO_ESTA_UNIDA`, `TIENE_MESAS_UNIDAS` según haga falta), `BloquearMesa`/`CambiarEstadoMesa` (rechazan una principal con mesas unidas), `ConsultaPedidos.abiertoDeMesa` (resuelve la principal); Test `UnirMesasTest`.

**Interfaces:** `UnirMesas.ejecutar(String secundariaId, String principalId, String actorId)`, `SepararMesa.ejecutar(String secundariaId, String actorId)`; ambos `Resultado<Mesa, ErrorMesa>`. Reglas de las decisiones 6 y 7. Bitácora `transferencia`.

- [ ] **Step 1: Write the failing tests:** unir una libre a una libre y a una ocupada; el mapa muestra la secundaria como `unida`; el pedido de la principal sale por `abiertoDeMesa(secundaria)`; no se abre un pedido en la secundaria (ya cubierto, se comprueba); la secundaria no puede tener pedido ni bloqueo, ni ser principal de otras; la principal no puede estar unida, bloqueada ni inactiva; no se une a sí misma ni a una de otro local; bloquear o desactivar una principal con unidas → `TIENE_MESAS_UNIDAS`; separar deja la secundaria libre; separar una que no está unida → `NO_ESTA_UNIDA`; la bitácora.
- [ ] **Step 2: Run, verify FAIL.** **Step 3: Implement.** **Step 4: Run, verify PASS.**
- [ ] **Step 5: Commit** (solo si el usuario lo pide).

---

### Task 5: La API y los permisos

**Files:** Modify `PedidoController`, `MesaController`, `ConfiguracionSeguridad`, `ListarAuditoria`; Test `ReorganizarAccesoPorRolTest`, `ListarAuditoriaTest` (frases).

**Endpoints (todos `venta.modificar`):**
- `POST /pedidos/{id}/lineas/trasladar {lineaIds[], personaDestinoId?}` → 200 con el **pedido completo** (sin `personaDestinoId` = persona nueva).
- `PUT /pedidos/{id}/mesa {mesaId}` → 200 con el pedido completo.
- `POST /mesas/{id}/unir {principalId}` y `POST /mesas/{id}/separar` → 200 con la mesa (la respuesta de mesa del mapa).
- Errores: 404 (`NO_ENCONTRADO`, `LINEA_NO_ENCONTRADA`, `PERSONA_NO_ENCONTRADA`, `MESA_NO_ENCONTRADA`), 409 (`MESA_OCUPADA`, `LINEA_ANULADA`, `PEDIDO_NO_REORGANIZABLE`, `TIENE_MESAS_UNIDAS`…), 422 el resto.
- `ConfiguracionSeguridad`: los nuevos caminos van **antes** de los matchers genéricos de `/pedidos/**` (`venta.crear`) y `/mesas/**` (`venta.administrar`), con `PERM_venta.modificar`.

- [ ] **Step 1: Write the failing tests:** mozo, cajero y administradores reorganizan (200); cocina y motorizado no (403); otro local 404 en cada verbo; códigos de error claros; las frases de bitácora.
- [ ] **Step 2: Run, verify FAIL.** **Step 3: Implement.**
- [ ] **Step 4: Run, verify PASS; luego la suite completa.** Regenerar los tipos del frontend si se usan.
- [ ] **Step 5: Commit** (solo si el usuario lo pide).

---

### Task 6: Frontend — reorganizar desde la pantalla del pedido

**Files:** Modify `api/cliente.ts`, `features/venta/PantallaPedido.tsx`, `features/venta/mensajes.ts`, `features/venta/index.ts`, `estilos/global.css`; Create `DialogoPasarA.tsx`, `DialogoMoverMesa.tsx`, `DialogoUnirMesa.tsx`.

**Interfaces:**
- `api/cliente.ts`: `trasladarLineasDelPedido(token, pedidoId, lineaIds, personaDestinoId | null)`, `moverPedidoDeMesa(token, pedidoId, mesaId)`, `unirMesa(token, mesaId, principalId)`, `separarMesa(token, mesaId)`; `Mesa.mesaPrincipalId` ya viene del mapa.
- `PantallaPedido`:
  - En la lista de líneas, cada línea (no anulada) tiene un **selector** y, con alguna marcada, aparece **"Pasar a…"** (`DialogoPasarA`: elegir una persona del pedido o "Persona nueva"; muestra el aviso "El precio se recalcula si cambia el tipo de cliente").
  - Botón **"Dividir la cuenta"**: atajo que marca las líneas de la persona activa y las pasa a "Persona nueva".
  - Botón **"Mover a otra mesa"** (solo pedido de mesa): `DialogoMoverMesa` lista las mesas libres (de `listarMesas`) y mueve; al terminar avisa y el título cambia a la mesa nueva.
  - Botón **"Unir mesa"** (`DialogoUnirMesa`: mesas libres) y, si hay unidas, la lista "Unidas: Mesa 4" con **"Separar"**.
  - Todo con los cuatro estados (cargando, vacío "no hay mesas libres", error, con datos), táctil (44 px), y errores en español.

- [ ] **Step 1: Write the failing check:** importar los tres diálogos antes de crearlos; `npx tsc -b --force` falla.
- [ ] **Step 2: Implement.** **Step 3:** `npx tsc -b --force` y `npm run lint` (solo puede quedar el error ajeno de `HorarioSemanal.tsx`).
- [ ] **Step 4: Verificar en el navegador** (Task 7).

---

### Task 7: Verificación, documentos y revisión

- [ ] **Step 1:** Reiniciar el backend con V36 (o comprobar que el que corre la tiene). Crear datos de prueba propios (pedido en una mesa con un cliente normal y un pensionista).
- [ ] **Step 2: Dividir una cuenta:** 3 productos de una persona → "Dividir la cuenta" → persona nueva con sus líneas y totales bien; el total del pedido no cambia.
- [ ] **Step 3: Recálculo:** pasar un menú de un cliente normal al pensionista → baja al precio del plan y se ve "precio de su plan"; devolverlo → vuelve al público.
- [ ] **Step 4: Comanda intacta:** comandar, pasar una línea comandada a otra persona, reabrir la comanda → sigue con la etiqueta original.
- [ ] **Step 5: Mover y unir:** mover el pedido a una mesa libre (la vieja queda libre en el mapa); unir una mesa libre a la del pedido (el mapa la muestra unida), abrir "Ver pedido" por la secundaria, separarla.
- [ ] **Step 6: Bordes y pantallas:** sin mesas libres; una línea anulada no se puede marcar; móvil (375) y tablet (768) sin desborde; errores en español.
- [ ] **Step 7:** Suite completa del backend y `tsc`/lint; documentar en `rikos-backend/doc/historias/US-029-reorganizar-cuentas.md`, actualizar `historias/README.md`, `sprint-06` (entrega 4 ✅ y el sprint completo salvo HT-10), `plan/README.md`, `ESTADO.md`, `pendientes/README.md`; limpiar solo los pedidos de prueba propios.
- [ ] **Step 8: Revisión independiente** (agente opus) y un pase de arreglos con pruebas, como en la 2A y la 3. Cerrar el sprint 6 a la espera de HT-10.

---

## Self-review

**Cobertura de la especificación (3.8):** trasladar líneas entre personas y dividir por productos (Task 2); recálculo CA5 (Task 2); una línea comandada conserva su lugar en la comanda que salió (Task 1); mover el pedido a una mesa **libre** con ambas mesas cambiando de estado solas (Task 3); unir y separar mesas operando el pedido en conjunto (Task 4); todo solo antes del cierre (decisión 10); permisos (`venta.modificar` para el mozo, ya sembrado); pantallas con sus cuatro estados (Task 6). **Fuera:** dividir el pago o cobrar por persona (sprint 7), y la reorganización desde el mapa de mesas (lo decide el diseño del otro chat).

**Riesgos abiertos:** (1) `CalculadorDePrecioDeLinea` se creó para agregar líneas nuevas; reutilizarlo al mover requiere pasarle la persona de destino y el "hoy" del pedido (`fechaOperativa`), no el reloj; (2) el "segundo del día" al mover: el conteo excluye la propia línea (ahora es de otra persona), hay que probarlo con cuidado; (3) unir mesas y el sprint 7: cuando se cobre y cierre el pedido de una principal, hay que separar las unidas (se anota para el sprint 7); (4) la unión cambia el significado de "mesa ocupada" en el mapa del otro chat (una unida sin pedido propio se ve `unida`): se le avisa.

**Consistencia de nombres:** `TrasladarLineas`, `MoverPedidoDeMesa`, `UnirMesas`, `SepararMesa` y los endpoints de la Task 5 son los que usan la Task 6 y las pruebas; `etiqueta_persona` (Task 1) es lo que lee `ConsultaComandas`.
