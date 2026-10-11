# US-015 — Agotados por local

Sprint 4, entrega 4 de 4 (2026-09-29). Con esta entrega cierra el
sprint 4. Decisiones en `docs/producto/plan/sprint-04-costos-y-menu.md`
(sección "Decisiones de la entrega 4").

## Qué se pidió

Que cocina o administración marquen un producto como agotado en su
local, para que el mozo deje de ofrecerlo en los pedidos nuevos sin tocar
las comandas ya emitidas, y lo reactiven registrando usuario, hora y
motivo. Lo agotado en un local sigue disponible en los demás.

## Qué se construyó

**Base de datos.**

- `V23__disponibilidad_por_local.sql`: `disponibilidad_producto` (una
  fila por local y producto con el último cambio; `observacion` agregada
  al modelo; CHECK "si no está agotado, tiene motivo"), séptimo tipo de
  motivo `disponibilidad`, **un motivo sembrado** ("Se volvió a
  preparar") y el evento de bitácora `reactivacion`.
- `V24__permiso_marcar_agotado.sql`: amplía el CHECK de `permiso.accion`
  (ver "Encontrado en el camino").

**Backend (`catalogo`).**

- `MarcarAgotado` (un toque, sin motivo) y `ReactivarProducto` (motivo de
  tipo `disponibilidad`, activo, y observación si el motivo la pide).
  Auditan `marcado_agotado` y `reactivacion` (con motivo y observación);
  el local lo pone el registrador.
- `LocalActivo`: el local sale del token, nunca de la petición. Sin local
  activo, `SIN_LOCAL_ACTIVO`.
- `ConsultaDisponibilidad` y `DisponibilidadController`:
  `GET /disponibilidad/agotados` (`menu.ver`),
  `POST /disponibilidad/{producto}/agotar` y `/reactivar`
  (`menu.marcar_agotado`).
- `GET /menu/dias/{fecha}` ahora trae `agotado` por opción según el local
  de quien pregunta: es lo que el POS del sprint 6 va a ocultar (CA1).
- Permiso nuevo `menu.marcar_agotado` para el administrador central, el
  de local y cocina.
- `CrearMotivo` acepta el tipo nuevo; `EliminarProducto` cuenta la
  disponibilidad como historial; la bitácora dice "marcó "X" como
  agotado" y "volvió a ofrecer "X"".

**Frontend.** `features/menu/PantallaDisponibilidad` (Menú semanal →
Disponibilidad): el almuerzo de hoy (del menú confirmado), los dos menús y
la carta, cada uno con Disponible/Agotado ("desde las 10:52 p. m.") y el
botón para marcar o volver a ofrecer; el modal de reactivación pide el
motivo y la observación. Buscador y contador de agotados. Solo lectura
sin el permiso. Configuración → Motivos muestra el tipo nuevo
"Disponibilidad".

## Qué se dejó fuera a propósito

- **Ocultar en la toma de pedido (CA1) y la prueba de comandas intactas
  (CA2):** los pedidos son del sprint 6. La regla es de consulta y ya está
  expuesta; la prueba de CA2 se agrega cuando existan las comandas.
- **Quién marcó, en la pantalla:** muestra la hora, no el nombre. El
  nombre está en la bitácora; mostrarlo aquí obligaría a que `catalogo`
  lea usuarios de `identidad`.
- **Insumos:** su falta es de inventario (sprint 9), no de lo que se
  ofrece.

## Cómo se verificó

**Pruebas automatizadas: 214 en verde, sin contar
`ArquitecturaDeModulosTest`** (que solo reporta el ciclo `comun ↔
identidad` conocido). Verificado de dos formas: la suma de los reportes de
surefire y el conteo de `@Test` en el código fuente dan lo mismo.

- `DisponibilidadTest` (9): marcar oculta en ese local y queda en la
  bitácora con el local; en otro local sigue disponible (CA4); reactivar
  exige motivo y registra usuario, hora y motivo (CA3); el motivo tiene
  que ser de disponibilidad y estar activo; observación obligatoria si el
  motivo la pide; no se marca dos veces ni se reactiva lo disponible; el
  ciclo completo se repite; solo productos vendibles; sin local activo no
  se marca.
- `DisponibilidadAccesoPorRolTest` (4), con usuarios asignados a locales
  reales: cocina, administrador de local y central marcan y reactivan en
  su local; mozo y cajero leen y reciben 403 al marcar o reactivar;
  motorizado 403; lo que cocina agota en el local A no aparece agotado
  para el mozo del local B.
- `CambiarLocalActivoSinTransaccionTest` (1): el bug de abajo.

**En vivo** (usuario `admin`, San Isidro): la pantalla mostró el almuerzo
de hoy (arroz con pollo, pollito), los menús y la carta. "Marcar agotado"
en el arroz con pollo pasó a "Agotado, desde las 10:52 p. m.", el contador
a "1 agotado", y `GET /menu/dias/2026-09-28` lo devolvió con
`agotado=true`. "Volver a ofrecer" abrió el modal con "Se volvió a
preparar" preseleccionado; al confirmar volvió a "Disponible", el menú
del día a `agotado=false`, y la bitácora registró "admin marcó "Arroz con
pollo" como agotado" y "admin volvió a ofrecer "Arroz con pollo"", ambas
con el local San Isidro.

## Encontrado en el camino

- **Cambiar de local daba 500 (bug del sprint 3).** 26 errores en el log
  de `POST /auth/cambiar-local`: `CambiarLocalActivo` construía la sesión
  sin transacción y los roles del usuario son perezosos (open-in-view
  apagado, DT-049). Su prueba no lo veía porque es `@Transactional`. Se
  reprodujo con una prueba sin transacción, falló con el mismo error, y se
  corrigió con `@Transactional(readOnly = true)`. Verificado en vivo:
  200.
- **`permiso.accion` tiene un CHECK con la lista cerrada de acciones.**
  El sembrador reventó al crear `menu.marcar_agotado` y tumbó el arranque
  de casi toda la suite. V23 ya estaba aplicada en la base de desarrollo,
  así que la ampliación va en V24 (no se editan migraciones aplicadas).
- **Los motivos no aparecían en el modal** aunque la API los devolvía: la
  pantalla los cargaba solo si el permiso estaba en el primer render, y
  justo después del login llega un render después. Corregido
  (`puedeMarcar` en las dependencias del efecto).
- **Logins repetidos en una prueba:** los primeros caracteres de un ULID
  son la marca de tiempo; dos usuarios del mismo rol creados en la misma
  prueba salían con el mismo nombre. Se usa la parte aleatoria.
- **Conteos de pruebas de las entregas anteriores.** Los números de
  US-010, US-014 y US-013 (162, 177, 196) salieron de los reportes de
  surefire de cada momento y no se pueden reconstruir con certeza. Al
  cerrar el sprint, reportes y código fuente coinciden en 214.
- **Algunas pruebas de sprints anteriores escriben en la bitácora real de
  la base de desarrollo** (filas "eliminó el usuario", "desactivó
  prueba-json" a la hora de cada corrida). Anotado en
  `docs/pendientes/README.md`.
