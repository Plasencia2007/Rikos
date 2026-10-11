# US-027 — El mapa de mesas

Sprint 6, entrega 1 de 4 (2026-09-29 y 2026-09-30). Especificación en
`docs/producto/plan/sprint-06-vender-en-mesa.md` (secciones 3.2, 3.10, 4.1 y 5);
plan táctico en `docs/superpowers/plans/2026-09-29-sprint6-entrega1-mesas-y-mapa.md`.

## Qué se pidió

Que el mozo vea el mapa de mesas de su local con su estado actual, y que un
administrador cree y edite las mesas. El usuario decidió que **el mozo también
puede bloquear y liberar mesas** (con motivo), y que el estado de la mesa **se
deriva** en vez de guardarse.

## Qué se construyó

**Base de datos.** `V29__mesas.sql`: tabla `mesa` (número único por local,
nombre y capacidad opcionales, `bloqueada` con motivo, quién y cuándo,
`mesa_principal_id` para las uniones de la entrega 4, `activo`). **No hay columna
`estado`**: es la desviación del modelo original, decidida con el usuario. Un
CHECK de la base impide una mesa bloqueada sin motivo. Eventos de bitácora
`cambio_mesa` y `bloqueo_mesa`. `V30__permiso_bloquear_mesa.sql`: `permiso.accion`
tiene un CHECK cerrado y no admitía `bloquear_mesa` (el plan lo había omitido;
V29 ya estaba aplicada, así que va en una migración nueva).

**Backend (módulo nuevo `venta`).**

- `EstadoDeMesa` + `CalculadorDeEstadoDeMesa`: los cinco estados con su
  prioridad (bloqueada, unida, pendiente de cierre, ocupada, libre), como
  función pura. En esta entrega no hay pedidos, así que solo se ven libre,
  unida y bloqueada; ocupada y pendiente de cierre aparecen con la entrega 2.
- `CrearMesa`, `ActualizarMesa`, `CambiarEstadoMesa` (desactivar/reactivar,
  idempotente; una mesa no se borra), `BloquearMesa` (solo mesa **libre**, activa
  y no unida; motivo obligatorio), `LiberarMesa`, `ConsultaMesas` (mapa de
  activas, o todas para administración; los números se ordenan como números:
  "2" antes que "10").
- Todo trabaja sobre el **local de la sesión** (`LocalDeSesion`); una mesa de otro
  local se responde "no encontrada", sin revelar que existe.
- `MesaController` (`/mesas`): `GET` (mapa; `?todas=true`), `POST`, `PUT /{id}`,
  `POST /{id}/desactivar|activar|bloquear|liberar`. Errores: 404, 409 (número
  repetido, ya bloqueada, no bloqueada, no libre) y 422.
- Permisos: nuevos `venta.bloquear_mesa` (mozo, cajero, administradores) y
  `venta.administrar` (administradores, ya cabía en `administrar`); **el mozo
  gana `venta.modificar`** (reorganizar cuentas, entrega 4).
- Bitácora en español: "admin creó la mesa "7"", "mozotest bloqueó la mesa "Mesa
  3": Mesa rota", "liberó…", "desactivó…".

**Frontend.** `PantallaMesas` (administración): tarjeta "Salones y mesas" de
Configuración → `/admin/configuracion/mesas`; tabla con editar, desactivar y
reactivar, y los cuatro estados. `MapaDeMesas` en el POS (`/pos`, tras el PIN):
rejilla de botones grandes con color por estado, leyenda con conteos,
actualización cada 15 s, y un diálogo por mesa para bloquear (con motivo) o
liberar. La pantalla del POS decide si mostrar los botones según el permiso
`venta.bloquear_mesa` del usuario; el servidor es quien decide de verdad.

## Qué se dejó fuera y por qué

- **Abrir un pedido desde la mesa** y los estados ocupada / pendiente de cierre:
  entrega 2 (US-028).
- **Unir y separar mesas:** entrega 4 (US-029); la columna `mesa_principal_id` ya
  está, y `BloquearMesa` rechaza una mesa unida.
- **Desactivar una mesa con pedido abierto:** todavía no puede pasar (no hay
  pedidos); se valida con la entrega 2.
- **Zonas ("Salón principal", "Terraza"):** la tarjeta de Configuración las
  mencionaba; no se construyeron, solo número, nombre y capacidad.

## Cómo se verificó

- **Pruebas:** `CalculadorDeEstadoDeMesaTest` (6), `GestionDeMesasTest` (9),
  `BloqueoDeMesasTest` (7), `MapaDeMesasTest` (4), `MesasAccesoPorRolTest` (5) y
  una más en `ListarAuditoriaTest`. `PermisosPredeterminadosTest` se actualizó
  (el cajero pasa de 14 a 15 permisos). Cada una se vio fallar antes de
  implementar. El conteo total de la suite está en `ESTADO.md`.
- **Roles:** administradores hacen todo; cajero y mozo ven el mapa y bloquean o
  liberan pero no crean, editan ni desactivan; cocina y motorizado **ven** el
  mapa (ya tienen `venta.ver` sembrado; cocina lo necesita para comandas) pero
  no bloquean ni administran.
- **Navegador:** estado vacío, crear las mesas 1 a 6, error de número repetido,
  editar, desactivar; en el POS, un mozo ve solo las 5 activas, bloquear sin
  motivo da error, con motivo la mesa pasa a "Bloqueada", liberar la devuelve a
  "Libre"; celular (2 columnas), tablet (4) y escritorio sin desborde; la
  bitácora muestra las frases.
- `ArquitecturaDeModulosTest` solo falla por el ciclo `comun ↔ identidad` ya
  conocido. `npx tsc -b --force` limpio; el lint no marca nada de lo nuevo.

## Bugs y tropiezos

- El plan olvidó el CHECK de `permiso.accion` (ver arriba).
- El plan asumía 403 para cocina en `GET /mesas`; ya tienen `venta.ver`, se ajustó
  la prueba (no la seguridad).
- `target/classes` quedó incompleto tras un recompilado con el backend corriendo;
  se resolvió borrando `target/classes` y `target/test-classes`. Ojo: un `mvn
  clean` falla en Windows si algún shell tiene su directorio dentro de `target`.
- **Bug ajeno encontrado:** un PIN incorrecto con un User-Agent de más de 120
  caracteres da 500 en `/auth/pin` (no cabe en `bloqueo_pin.dispositivo`). Se dejó
  como tarea aparte; no se tocó.

## Ojo con los datos de desarrollo

Las 6 mesas del local `sanisidro` son **inventadas** (la cantidad real la define
el dueño, RF-010; no bloquea). Para probar el POS se asignó un PIN aleatorio de
prueba a los usuarios `mozotest` (activo) y `pruebamozo` (inactivo, que sigue
inactivo); no se tocó el PIN de `jhon`.

## Pase de arreglos tras la revisión final (2026-09-30)

Un revisor independiente encontró 0 Critical y 3 Important; los tres se arreglaron, cada uno con su prueba escrita primero (vista fallar):

1. **Permisos del POS.** El mapa consultaba `/usuarios/{id}/permisos` (solo administradores): al mozo le daba 403 y dejaba una fila `acceso_denegado` en la bitácora en cada ingreso. Ahora existe `GET /auth/mis-permisos` (permisos efectivos del propio usuario, cualquier rol) y el mapa lo usa; si falla, oculta los botones. Verificado: el contador de accesos denegados no subió tras un ingreso de mozo.
2. **Carreras.** Dos mozos bloqueando la misma mesa, o dos altas con el mismo número, daban 500. Las escrituras de `MesaController` ya no van dentro de una transacción del controlador (el choque sale ya cerrado del caso de uso) y se traducen a **409** `MESA_MODIFICADA` (choque de versión) o `NUMERO_DUPLICADO` (restricción única); otra violación de integridad no se disfraza. Pruebas: `MesasConcurrenciaTest` (4).
3. **`venta.modificar` del mozo.** El sembrador solo agrega a un rol existente los permisos *nuevos* del catálogo, así que el mozo de una base ya sembrada no lo recibía. `V31__mozo_puede_modificar_pedidos.sql` se lo otorga (idempotente). Prueba sobre la base: `PermisosDelMozoEnLaBaseTest`.

Suite completa tras el pase: 368 pruebas, 0 fallos (suma de surefire, sin `ArquitecturaDeModulosTest`). Los minors quedaron en `docs/pendientes/README.md`.

Tropiezo del pase: Docker Desktop se había caído (Connection refused en el 5433); se recuperó abriéndolo y esperando `healthy`.

## El administrador también opera el salón desde su panel (2026-09-30)

El usuario preguntó si el administrador general podía hacer todo desde su panel: no podía. Las mesas solo se creaban desde una tarjeta escondida en Configuración, y el mapa solo existía en el POS (con PIN). Se agregó a **Operación** el ítem **Salón y mesas** (con `venta.ver`) con dos entradas: **Mapa de mesas** (`/admin/salon/mesas`, el mismo componente del POS: ver el estado y bloquear o liberar) y **Administrar mesas** (`/admin/configuracion/mesas`, crear, editar y activar). El mapa muestra el local que el administrador tenga activo en el encabezado. Verificado en el navegador como administrador central: ítem visible, mapa con las 5 mesas activas, bloquear y liberar la mesa 2, botón "Administrar mesas".
