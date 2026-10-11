# Endurecimiento previo al Sprint 3 (2026-09-25)

## Qué se pidió

El usuario compartió una lista de 5 pendientes "antes del Sprint 3" (aparentemente
de una revisión externa del estado del sistema) y pidió confirmar cuáles ya
estaban resueltos. Investigación primero, sin tocar código:

1. "El 403 que oculta excepciones" -- un fallo interno se presentaría como
   "no tienes permiso", complicando el diagnóstico de bugs.
2. Regenerar `esquema.d.ts` con `npm run generar-tipos`.
3. Persistir el bloqueo del PIN (`bloqueo_pin`) -- **ya resuelto en esta misma
   sesión, ver `US-001-US-003-bloqueo-pin-y-local-activo.md`**.
4. La prueba de US-001 CA3 -- **ya resuelto en esta misma sesión, mismo
   archivo**.
5. Que la semilla de desarrollo (`admin`/`cambiar123`, PIN `1234`) no corra
   en el perfil `nube`.

Este documento cubre los items 1, 2 y 5 (los dos primeros seguían abiertos
o parcialmente al investigar; el 3 y el 4 ya estaban hechos y se documentan
aparte).

## Investigación antes de tocar código

- **Item 1 no era reproducible tal como se describía.** Se revisó
  `ConfiguracionSeguridad`, el filtro JWT y se buscó en todo el módulo
  `@ControllerAdvice`/`@ExceptionHandler`/`AccessDeniedHandler`/
  `AuthenticationEntryPoint`: cero coincidencias. No hay ningún mecanismo
  que convierta una excepción interna en 403 -- Spring Security solo
  traduce `AccessDeniedException`/`AuthenticationException`, nunca un
  `RuntimeException` cualquiera. Lo que sí era cierto: **no existía ningún
  manejador global de errores**, así que una excepción no prevista caía en
  la página de error genérica de Spring Boot (un 500 sin cuerpo útil, sin
  referencia para correlacionar con el log). El síntoma reportado (403) no
  coincidía con el código, pero el problema de fondo (diagnóstico a
  ciegas) sí era real.
- **Item 2 confirmado desactualizado.** `esquema.d.ts` existe y tenía los
  campos parchados a mano de la sesión anterior (`totalProductos`,
  `creadoEn`/`actualizadoEn`/`fechaUltimaCompra`), pero ningún valor de
  `ErrorAutenticacion` (`LOCAL_INACTIVO`, `BLOQUEADO`, etc.) ni nada de
  `bloqueo_pin` aparecía -- evidencia directa de que quedó atrás del
  trabajo de bloqueo de PIN / local activo de este mismo día.
- **Item 5 confirmado como hueco abierto, sin ningún mecanismo.**
  `V9__datos_semilla.sql` inserta `admin`/`cambiar123` sin condición
  alguna. Revisados `application.yaml`, `application-nube.yaml`,
  `application-local.yaml`: no hay `spring.flyway.locations` por perfil,
  ninguna bandera, ningún chequeo de arranque.

## Qué se construyó

### Item 1: `ManejadorGlobalDeErrores`

Nuevo `@RestControllerAdvice` en `comun.web`. Dos partes:

- `AccessDeniedException`/`AuthenticationException` se re-lanzan tal
  cual (un `@ExceptionHandler` que solo hace `throw excepcion`) -- las
  sigue manejando Spring Security, no se les pisa el 401/403 real con un
  500 genérico.
- Cualquier otra `Exception` no prevista: se registra en el log con una
  referencia corta (`UUID` de 8 caracteres) y el stack trace completo
  server-side, y se responde `500` con
  `{"error": "ERROR_INTERNO", "referencia": "<8 chars>"}` -- nunca el
  mensaje ni el tipo de la excepción real al cliente, pero con una
  referencia que sí permite ir al log y encontrar exactamente qué pasó.

No hizo falta tocar los controladores existentes: siguen devolviendo sus
propios cuerpos de error a través del patrón `Resultado` para los fallos
esperados (`404`, `409`, etc.); este manejador solo entra cuando algo
revienta de verdad.

### Item 2: `esquema.d.ts` regenerado

Se reinició `servicio-operacion` con el código del día (antes corría una
compilación vieja, de antes del trabajo de bloqueo de PIN/local activo) y
se corrió `npm run generar-tipos` contra `http://localhost:8081/api/v3/api-docs`
real. Confirmado en el archivo regenerado:

- `totalProductos`, `creadoEn`, `actualizadoEn`, `fechaUltimaCompra`
  ahora aparecen **de verdad** (ya no hace falta el parche a mano de la
  sesión anterior -- el backend ya los exponía, solo faltaba regenerar).
- `LOCAL_INACTIVO`/`BLOQUEADO` (los valores de `ErrorAutenticacion`)
  **siguen sin aparecer**, y no es por estar desactualizado: `/auth/login`
  y `/auth/pin` devuelven sus errores como `ResponseEntity<?>` con un
  `Map<String, Object>` crudo (`AutenticacionController.respuesta`), no
  un DTO tipado -- springdoc no tiene de dónde sacar el esquema. Queda
  anotado como limitación conocida, no como algo que arreglar ahora.

### Item 5: `GuardaSemillaDesarrollo`

Partir la migración `V9` por perfil se descartó: es más riesgoso de lo
que resuelve (los entornos que ya la migraron quedarían con un historial
de Flyway que no coincide con ubicaciones nuevas si se reorganiza).

En su lugar, `GuardaSemillaDesarrollo` (`comun.seguridad`, `@Profile
("nube")`, `ApplicationRunner`): al arrancar, si el perfil activo es
`nube`, chequea si existe el usuario semilla exacto por su ULID fijo
(`01M2QZ606763Q7XC00M8VD7WM1` -- el mismo que usan los tests como
`ADMIN_SEMBRADO_ID`, no por nombre de usuario, para no chocar con un
admin real legítimo que también se llame "admin"). Si lo encuentra
activo, lo **desactiva** (mismo `desactivar()` que usa
`CambiarEstadoLocal`) y deja un `WARN` bien visible en el log.

**Primera versión, corregida por una segunda revisión (P-29):** la
primera versión de esta clase lanzaba `IllegalStateException` y frenaba
el arranque en vez de desactivar. El problema, señalado por la revisión:
la migración `V9` y este guarda corren en el **mismo** arranque, así
que un despliegue nuevo a `nube` nunca habría podido levantar la
primera vez -- alguien habría tenido que borrar la fila a mano contra
la base de producción antes del primer arranque exitoso, un problema de
huevo y gallina. La decisión de fondo (fallar cerrado en vez de dejar
`admin`/`cambiar123` expuesto) seguía siendo correcta; lo que estaba
mal era la forma de "fallar cerrado" -- desactivar la cuenta logra lo
mismo (queda inutilizable) sin impedir que el resto del sistema
arranque.

## Qué se dejó fuera y por qué

- No se partió `V9` por perfil (ver arriba, riesgo de checksum de Flyway
  en entornos ya migrados).
- No se tipó el cuerpo de error de `/auth/login`/`/auth/pin` como un DTO
  propio para que `LOCAL_INACTIVO`/`BLOQUEADO` aparezcan en el esquema
  generado -- cambiaría la forma de la respuesta de un endpoint ya en uso
  por el frontend real; se dejó anotado como limitación conocida del
  esquema generado, no como bug.
- El `ManejadorGlobalDeErrores` no intenta clasificar excepciones de
  negocio conocidas (`DataIntegrityViolationException`, etc.) en códigos
  HTTP específicos -- cada caso de uso que ya necesita eso lo maneja en su
  propio controlador vía `Resultado`; este manejador es solo la red de
  seguridad para lo verdaderamente inesperado.

## Cómo se verificó

- **`ManejadorGlobalDeErroresTest`** (2 casos, MockMvc "standalone" sin
  Spring Security ni base de datos): una excepción no prevista devuelve
  500 con `ERROR_INTERNO` + `referencia` en el cuerpo y **sin** el nombre
  de la excepción real filtrado al cliente; un `AccessDeniedException`
  lanzado desde un controlador de prueba se re-lanza tal cual (no lo
  convierte en 500).
- **`GuardaSemillaDesarrolloTest`** (3 casos, con mocks, reescrito tras
  P-29): desactiva y guarda si el usuario semilla existe y está activo
  (nunca lanza); no toca nada si ya estaba desactivado; no hace nada si
  no existe.
- **`esquema.d.ts`**: regenerado contra el backend real reiniciado,
  confirmado con `grep` que los campos antes parchados a mano ahora
  vienen del esquema real. `npx tsc -b --force` limpio después.
- Suite completa del backend tras esto: **107/107 verde** (incluye los
  4 de este bloque más los de "Deuda que puede esperar" más abajo).

## Bugs encontrados en el camino

- Al reiniciar `servicio-operacion` para regenerar el esquema:
  `./mvnw -pl servicio-operacion -am spring-boot:run` desde la raíz del
  reactor falla con `No plugin found for prefix 'spring-boot'` -- el
  goal `spring-boot:run` necesita correrse desde el propio directorio del
  módulo (`cd servicio-operacion && ../mvnw spring-boot:run`), no desde
  la raíz con `-pl`/`-am`. Anotado acá porque no es obvio y ya hizo
  perder un intento en esta misma sesión.
- **P-29**: ver arriba, la primera versión de `GuardaSemillaDesarrollo`
  frenaba el arranque en vez de desactivar -- encontrado por una segunda
  revisión, no en la implementación original.

## "Deuda que puede esperar, pero no olvidarse" -- 6 items de otra revisión

El usuario compartió una segunda tabla, esta vez de items explícitamente
marcados como que **pueden esperar** (no urgentes). Mismo patrón:
investigar primero con evidencia concreta, después decidir con el
usuario cuáles vale la pena tocar ya. De los 6, el usuario pidió avanzar
con 4; los otros 2 (chequeo de historial en `EliminarProveedor`, editar/
eliminar presentación de compra) se dejaron fuera a propósito -- el
primero porque el propio código ya explica que es intencional hasta que
exista la tabla de compras del sprint 9; el segundo porque no es deuda,
es una funcionalidad que nunca existió y necesita diseño propio.

### P-24: matchers muertos de categoría

`ConfiguracionSeguridad` tenía matchers de seguridad para
`POST /categorias/*/activar` y `POST /categorias/*/desactivar`, pero
`CategoriaController` nunca definió esas rutas (Categoría se borra o no
se borra, vía `EliminarCategoria` -- no tiene concepto de
activar/desactivar como Producto o Proveedor). Confirmado con
`EliminarCategoriaTest` que ya cubre ese camino. Se borraron los dos
matchers muertos; los de Producto/Proveedor en la misma línea se
mantuvieron intactos porque esos sí son reales.

### P-17: puerto de Postgres hardcodeado

Solo estaba hardcodeado en un lugar real: `docker-compose.yml`, el
mapeo `"5433:5432"` del contenedor de desarrollo (la app misma ya leía
el puerto vía `${DB_URL:...}`, eso no tenía el problema). Cambiado a
`"${POSTGRES_HOST_PORT:-5433}:5432"` -- sigue siendo 5433 por defecto
(el motivo de por qué 5433 y no el 5432 estándar ya está explicado en el
comentario del archivo), pero ahora se puede pisar con una variable de
entorno si hace falta.

### `UnidadController` sin `@Transactional`

Era el único controlador de catálogo que hablaba directo con su
repositorio (`UnidadMedidaRepositorio`) en vez de delegar a un caso de
uso -- sin capa de aplicación, sin `@Transactional` en ningún lado.
Inofensivo hoy (una sola operación de escritura, `crear`, con un chequeo
+ un save), pero rompía el patrón que sigue el resto del catálogo.
Extraído `CrearUnidad.java` (`@Transactional`, mismo patrón que
`CrearProveedor`/`CrearCategoria`: `Resultado<UnidadMedida,
ErrorCatalogo>`, error `CODIGO_DUPLICADO` que ya existía en el enum). El
controlador ahora delega y solo traduce el `Resultado` a HTTP.
`CrearUnidadTest` nuevo (2 casos: crea, rechaza código duplicado).

### P-22: 43 endpoints con `ResponseEntity<?>` sin tipo para OpenAPI

El más grande de los 4. Confirmado en la investigación previa: no son
47 sino **55** ocurrencias totales de `ResponseEntity<?>` (43 métodos
públicos de endpoint + 12 helpers privados `errorDe`/`respuesta` que no
cuentan), repartidos en 12 controladores (no 11 -- apareció
`HorarioController` en el conteo real). El wildcard hace que springdoc
no pueda describir el cuerpo real de la respuesta, así que esos
endpoints quedaban con el cuerpo sin tipar en `esquema.d.ts`
(`"*/*": Record<string, never>` en vez de la forma real) -- HT-05
(generación de tipos TS) no cubría esos 43 endpoints, aunque
técnicamente sí aparecían en la lista de rutas.

**Decisión de diseño**: no cambiar el tipo de retorno de Java (sería
mucho más invasivo -- requeriría un tipo unión o un wrapper genérico
tocando cada llamador) sino anotar cada método con `@ApiResponses`/
`@ApiResponse`/`@Schema` de springdoc describiendo la forma real por
código HTTP. Es puramente aditivo: cero cambios de lógica o de tipos de
retorno de Java, solo metadata para OpenAPI.

Se agregó un DTO compartido nuevo, `pe.rikos.operacion.comun.web.
ErrorRespuesta(String error)`, para el patrón repetido en casi todos
los controladores (`Map.of("error", enumName)`), y un record local en
`AutenticacionController` (`FalloAutenticacionRespuesta(String error,
Long segundosRestantes)`) para el único caso con forma distinta (el
`segundosRestantes` extra en `BLOQUEADO`).

Anotados los 43 endpoints reales en los 12 controladores (Categoria 4,
Conversion 1, Presentacion 1, Producto 7, Proveedor 5, Unidad 1,
Autenticacion 2, Horario 3, Local 6, Rol 4, Usuario 9 -- `PermisoController`
no tenía ningún `ResponseEntity<?>`, no necesitó nada).

Verificado end-to-end: reiniciado `servicio-operacion` con el código
anotado, regenerado `esquema.d.ts` de nuevo -- `/auth/login` pasó de
`"*/*": Record<string, never>` a
`"*/*": components["schemas"]["SesionIniciada"]`, con los campos reales
(`token`, `usuarioId`, `nombreUsuario`, `roles`, `locales`,
`localActivoId`, `permisos`). `npx tsc -b --force` limpio, sin que
hiciera falta tocar ningún archivo del frontend -- nada dependía de la
falta de tipo previa.

**Qué se dejó fuera a propósito**: los cuerpos de error usan el mismo
`ErrorRespuesta` genérico en vez de documentar el valor exacto de cada
enum posible por código HTTP -- eso requeriría tocar los propios enums
de error (`@Schema` con `allowableValues`), fuera del alcance de un
cambio "solo anotaciones". Los dos `404` de `obtenerPublico`
(LocalController) y `obtener` (UsuarioController) quedaron sin cuerpo
documentado porque esos caminos usan `ResponseEntity.notFound().build()`
de verdad (sin cuerpo), no el patrón `Map.of("error", ...)`.

### Cómo se verificó (los 4 de esta sección)

- `CrearUnidadTest` (2 casos nuevos).
- Suite completa del backend: **107/107 verde** en total (104 del
  bloque anterior + 3 de `GuardaSemillaDesarrolloTest` reescrito - 2
  viejos + 2 de `CrearUnidadTest`, número final confirmado corriendo la
  suite, no calculado a mano).
- `esquema.d.ts` regenerado una segunda vez (después de P-22) contra el
  backend reiniciado con todo el código de esta sección. `npx tsc -b
  --force` limpio.
- Servidor `servicio-operacion` quedó corriendo en el `:8081` al
  terminar, con el código de todo este documento.
