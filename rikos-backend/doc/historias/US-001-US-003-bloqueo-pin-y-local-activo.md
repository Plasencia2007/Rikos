# US-001 CA3 / US-003 (DT-039): bloqueo del PIN persistido y `activo=falso` bloquea el PIN (2026-09-25)

## Qué se pidió

Durante una revisión del estado real del sistema contra su propia
documentación técnica, se encontraron y confirmaron dos gaps, y el
usuario pidió corregir ambos:

1. **DT-039 no estaba implementado.** El bloqueo por intentos fallidos de
   PIN seguía siendo el placeholder del Sprint 1 (en memoria, por local),
   aunque `RIKOS-decisiones-tecnicas.md` ya tenía la decisión tomada
   ("Decidido") de que debía ser por **dispositivo** y **persistido**, y el
   modelo de datos (`RIKOS-modelo-de-datos.md`, 3.7 ter) ya especificaba la
   tabla `bloqueo_pin` para eso -- pero nunca se había migrado ni se había
   reescrito el código para usarla.
2. **US-001 CA3 ("`activo = falso` impide nuevas operaciones y conserva
   íntegro el historial") solo cumplía la mitad, y sin ninguna prueba
   automatizada.** La mitad de "conserva el historial" se cumple sola
   (`CambiarEstadoLocal` nunca borra nada). La mitad de "impide nuevas
   operaciones" **no estaba implementada en ningún lado**: ni
   `AutenticarConPin` ni `AutenticarConContrasena` chequeaban
   `local.isActivo()` -- un local desactivado seguía dejando entrar por PIN
   sin ningún bloqueo.

## Qué se construyó

### DT-039: bloqueo persistido por dispositivo

- **Migración `V17__bloqueo_pin.sql`**: tabla `bloqueo_pin` tal como la
  especifica 3.7 ter -- `local_id` (FK a `local`, `ON DELETE RESTRICT`),
  `dispositivo` (`VARCHAR(120) NOT NULL`), `intentos_fallidos`,
  `bloqueado_hasta`, `actualizado_en`, único por (`local_id`,
  `dispositivo`).
- **`BloqueoPin.java`** (entidad, en `identidad.infraestructura.seguridad`):
  mismo patrón que `Usuario.registrarIntentoFallido/estaBloqueado/
  segundosRestantesBloqueo`, pero por fila de `bloqueo_pin` en vez de por
  usuario. `BloqueoPinRepositorio.findByLocalIdAndDispositivo`.
- **`LimitadorIntentosPin.java` reescrito por completo**: pasó de un
  `ConcurrentHashMap<String, Estado>` keyed por `localId` a leer/escribir
  `BloqueoPinRepositorio` keyed por (`localId`, `dispositivo`). Todos los
  métodos (`estaBloqueado`, `segundosRestantes`, `registrarFallo`,
  `registrarExito`) ganaron el parámetro `dispositivo`. El "dispositivo" es
  hoy el header `User-Agent` que ya viajaba hasta `AutenticarConPin`
  (se usaba para `registradorSesion`, nunca para el bloqueo) -- no hubo que
  agregar nada nuevo al contrato de la API, solo reusar el dato. Si el
  cliente no manda `User-Agent`, se usa la huella fija `"desconocido"`
  (la columna es `NOT NULL` y parte de la clave única).
- Queda pendiente y documentado como fuera de alcance: el proceso
  programado que limpia filas vencidas de `bloqueo_pin` (lo menciona 3.7
  ter). No se implementó -- la tabla crece sin límite por ahora, algo a
  resolver si se vuelve un problema real de volumen.

### US-001 CA3: `activo=falso` ahora bloquea el PIN

- **`AutenticarConPin.java`**: justo después de resolver el local por su
  código, si `!local.isActivo()` se corta con un nuevo error
  `ErrorAutenticacion.LOCAL_INACTIVO`, se registra la sesión como
  `"fallido"`, y **no** se llega ni a chequear el limitador ni a probar el
  PIN contra ningún usuario. Se decidió NO tocar
  `AutenticarConContrasena` (login del panel admin): ese login no está
  atado a un local en particular al momento de autenticar (un admin puede
  necesitar entrar precisamente para reactivar un local desde el panel),
  así que bloquearlo ahí habría sido un candado que se traba solo. El
  gate de CA3 tiene sentido en el punto donde "nueva operación" significa
  literalmente algo, que es el POS de ese local.
- `ErrorAutenticacion` ganó el valor `LOCAL_INACTIVO`. El controlador no
  necesitó cambios: cualquier error que no sea `BLOQUEADO` ya caía en
  `401`, que es el código correcto acá también.
- Frontend: `PantallaPin.tsx`, mapa `MENSAJES` gana
  `LOCAL_INACTIVO: 'Este local está desactivado. Contacta a un
  administrador.'` (antes habría caído en el mensaje genérico "No se pudo
  verificar el PIN.", que no ayuda a un mozo a entender qué pasa).
- `RIKOS-decisiones-tecnicas.md`, DT-039: estado actualizado de "Decidido"
  a "Decidido — implementado en V17".

## Qué se dejó fuera y por qué

- El chequeo de local activo **no** se agregó a `AutenticarConContrasena`
  ni a `ResolutorSesionUsuario` (que arma la lista de locales del admin) --
  ver arriba, es una decisión consciente, no un olvido.
- El proceso programado de limpieza de `bloqueo_pin` (filas vencidas) --
  mencionado en el modelo de datos, no implementado.

## Cómo se verificó

Pruebas nuevas con TDD real:

- **`LimitadorIntentosPinTest`** (4 casos, contra Postgres real, no mocks):
  se bloquea al llegar al umbral y la fila queda persistida en
  `bloqueo_pin` con los valores correctos; bloquea **solo** el dispositivo
  que falló (la tablet de al lado sigue libre); un éxito limpia el
  contador de ese dispositivo; un dispositivo sin `User-Agent` usa la
  huella `"desconocido"` en vez de fallar por la columna `NOT NULL`.
- **`AutenticarConPinTest`** (5 casos, no existía ningún test de esta clase
  antes -- otro gap que apareció en el camino): PIN correcto en local
  activo entra, local desactivado rechaza con `LOCAL_INACTIVO` *sin*
  llegar a consultar el limitador ni a los usuarios del local (verificado
  con `verify(..., never())`), código de local inexistente no revela si
  existe (mismo `CREDENCIALES_INVALIDAS` de siempre), dispositivo
  bloqueado rechaza aunque el PIN sea correcto, PIN incorrecto registra el
  fallo en el limitador con el dispositivo correcto.
- **`CambiarEstadoLocalTest`** (4 casos, cubre US-001 CA3 completo por
  primera vez): desactivar un local con un usuario asignado y una sesión
  real registrada no borra ninguna de las dos filas (mitad "conserva
  historial"); desactivar un local y después intentar entrar por PIN con
  el PIN correcto de un mozo real asignado rechaza con `LOCAL_INACTIVO`
  (mitad "impide nuevas operaciones", probado de punta a punta contra el
  `AutenticarConPin` real, no un mock); reactivar el local vuelve a dejar
  entrar; no-encontrado.
- **`CrearLocalTest`** (3 casos) y **`EliminarLocalTest`** (3 casos):
  cobertura básica que tampoco existía para el resto del paquete
  `identidad.aplicacion` de Local.
- Suite completa del backend tras esto: **100/100 verde** (19 tests
  nuevos: 5 + 4 + 3 + 3 + 4).
- `npx tsc -b --force` limpio en el frontend tras el mensaje nuevo de
  `PantallaPin.tsx`.
- No se hizo verificación manual en navegador de este ajuste puntual: la
  cobertura de `CambiarEstadoLocalTest` ya ejercita el camino real
  completo (desactivar → intentar PIN real → reactivar → volver a
  entrar) contra Postgres real, con la misma confianza que un click
  manual.

## Bugs encontrados en el camino (de las propias pruebas nuevas, antes de llegar a Postgres real)

- Primer intento de `LimitadorIntentosPinTest` usaba un código de local de
  más de 20 caracteres (`local.codigo` es `VARCHAR(20)`) -- violación de
  longitud de columna, corregido acortando el código de prueba.
- Los cuatro archivos de test nuevos usaban un `"actor-1"` inventado como
  `creado_por`/`actorId` -- pero esas columnas son FK reales a `usuario`
  (`ON DELETE RESTRICT`), así que insertar con un id que no existe en
  `usuario` revienta con violación de FK. Corregido usando el id del admin
  sembrado real (`01M2QZ606763Q7XC00M8VD7WM1`, el mismo que ya usa
  `EliminarRolTest` para `usuario_rol.asignado_por`).

## Pantalla de Locales en tarjetas y cambio de local (2026-09-28)

**Qué se pidió.** Rehacer la pantalla Locales (diseño en el canvas, tema verde) como lista de
tarjetas, con "Cambiar a este local" que use el mecanismo real de local activo, y arreglar el
selector del encabezado, que "no funcionaba".

**Causa raíz del selector.** El admin solo tenía asignado San Isidro. Un local nuevo (La Era)
no estaba en `sesion.locales`, así que el selector quedaba deshabilitado (≤1 local) y el backend
rechazaba el cambio (`LOCAL_NO_ASIGNADO`).

**Decisión de negocio (DT pendiente de registrar):** el rol **administrador central** ve y puede
operar en **todos los locales activos**, estén o no asignados. Se descartó auto-asignar al
creador porque el local nuevo tendría un usuario asignado y ya no se podría eliminar
(`TIENE_HISTORIAL`). Los demás roles siguen limitados a sus locales asignados. Un local
desactivado deja de ofrecerse y no se puede pasar a él.

**Backend.** `ResolutorSesionUsuario` (lista de locales del central + `puedeOperarEn`),
`CambiarLocalActivo` (usa `puedeOperarEn`; ahora `@Transactional(readOnly = true)`).
Tests nuevos en `ResolutorSesionUsuarioTest` y `CambiarLocalActivoTest`.

**Frontend.** `PantallaLocales.tsx` (tarjetas, buscador, pestañas Todos/Activos/Inactivos,
aviso descartable, menú ⋮ con Escape/clic fuera, desactivar/reactivar, eliminar con mensaje de
historial), estilos `.loc-*`, `activarLocal`/`desactivarLocal` en `cliente.ts`,
`refrescarLocales()` en `SesionContexto` (se llama al entrar y tras cada cambio para que el
selector del encabezado no muestre datos viejos). "Ver local" y "Editar local" abren el
formulario de edición.

**Bug encontrado en la verificación en vivo.** `cambiar-local` daba 500
(`LazyInitializationException` al leer `Rol.getNombre()`): con `open-in-view: false` el caso de
uso necesitaba `@Transactional`. Los tests no lo detectaron porque `CambiarLocalActivoTest` es
`@Transactional` a nivel de clase. Corregido; **queda pendiente** un test sin transacción que
cubra este camino.

**Verificado en vivo** (admin, Chrome del panel): el selector lista ambos locales y cambia; el
botón de la tarjeta cambia el local y actualiza el selector y la etiqueta "Local actual";
desactivar quita el local del selector, reactivar lo devuelve; eliminar San Isidro muestra el
aviso de historial; el menú se cierra con Escape. `npx tsc -b --force` sin errores.
Tests de identidad en verde salvo `ArquitecturaDeModulosTest` (ciclo `comun ↔ identidad`, ya
reportado en ESTADO.md).

## Asistente de 4 pasos para crear y editar locales (2026-09-28)

**Qué se pidió.** Rehacer el formulario de local (antes: 4 pestañas) como asistente de 4 pasos
diseñado primero en el canvas (artboards "Crear local · 1..4"): Datos, Contacto, Operación,
Aspecto. Teléfono y correo obligatorios. El horario pasa a acordeón por día con varios turnos
(desayuno/almuerzo/cena).

**Qué se construyó.**
- Backend: migración `V25__local_identidad_visual.sql` (`color_principal`, `portada_url`);
  `Local`, `LocalController` (request/response con los campos nuevos). El PUT exige `telefono`,
  `correo` (con formato) y valida `colorPrincipal` como `#RRGGBB`. Test `ActualizarLocalTest`.
- Frontend (`features/locales`): `PantallaLocalFormulario` (armazón y guardado), `MarcoAsistente`
  (migas, progreso, pie fijo), `PasoDatos/Contacto/Operacion/Aspecto`, `HorarioSemanal`
  (acordeón, copiar a otros días, días especiales), `formularioLocal.ts` (estado compartido y
  validaciones), `Interruptor` en `comun/componentes`, estilos en `estilos/asistente-local.css`.
  Rutas: `/admin/locales/nuevo` y `/admin/locales/:id/editar/:paso` (`datos|contacto|operacion|aspecto`).
- El local se crea al terminar el paso 1 (POST solo lleva código, nombre y dirección); desde ahí
  cada paso hace un PUT del local entero. El punto del mapa marcado antes de crear viaja por el
  estado de navegación y se guarda con el paso Contacto.
- Un local antiguo sin teléfono/correo (p. ej. San Isidro) puede recorrer el paso 1 sin error: sus
  cambios se guardan junto con el paso Contacto.
- Se eliminaron `PestanaHorarios` y `PestanaOnlineYHorarios` (reemplazadas por `HorarioSemanal`).

**Se dejó fuera, a propósito.**
- **Subir logo y portada como archivo**: no existe servicio de almacenamiento; el paso Aspecto
  guarda enlaces (URL), como ya hacía el logo.
- **Editar un día especial** (lápiz del diseño): no hay endpoint de edición; solo se crea y
  se elimina.
- **"Estado activo" dentro del formulario**: se movió a la lista (menú ⋮ → Desactivar/Reactivar).

**Bug encontrado en la verificación en vivo.** Eliminar un local con horarios o días especiales
daba 500 (`horario_servicio_local_id_fkey`, FK RESTRICT). Como el asistente los carga al crear,
un local recién armado no se podía borrar. `EliminarLocal` ahora borra horarios y días
especiales (configuración, no historial) antes del local; test nuevo en `EliminarLocalTest`
(rojo antes del arreglo, verde después). Los locales con usuarios o sesiones siguen dando 409.

**Verificado en vivo** (admin): crear "Chosica Prueba" (validaciones de paso 1 y 2, código
sugerido, vista previa), tres turnos en Lunes, copiar a Martes y Jueves, día especial,
tiempos de delivery, color y portada; todo quedó guardado en la BD y el selector del encabezado
lista el local nuevo. Editar San Isidro (sin teléfono) avanza a Contacto. Borrado del local de
prueba con horarios: 204. `npx tsc -b --force` sin errores; suite backend en verde salvo
`ArquitecturaDeModulosTest` (ciclo `comun ↔ identidad`, ya reportado).
