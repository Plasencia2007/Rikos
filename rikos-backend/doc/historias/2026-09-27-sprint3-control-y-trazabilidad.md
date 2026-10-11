# Sprint 3: control y trazabilidad — US-006, US-012, HT-06 (2026-09-27)

## Qué se pidió

Sprint corto porque US-004 y US-007 ya se habían adelantado. Quedaban US-006
(auditoría transversal completa), US-012 (motivos configurables), HT-06
(verificación automática de fronteras entre módulos) y dos pendientes
sueltos de una revisión anterior (P-26, P-29). El pedido completo, con su
propio orden de prioridad, está en el mensaje original del usuario; este
documento resume qué se construyó realmente y dónde quedó corto el plan
original al chocar con el código real.

## 1. P-26 y P-29 — cerrados

**P-26 (el 403 fantasma)**: confirmado. `/error` no estaba en `permitAll`
de `ConfiguracionSeguridad`. Cuando un controlador revienta, Spring reenvía
la petición a `/error` para armar la respuesta; si esa ruta no está
permitida, la cadena de seguridad bloquea el *reenvío* (no la petición
original) y el cliente ve 403 en vez del 500 real. Agregado a `permitAll`,
con `RutaDeErrorAbiertaTest` (pasa por la cadena de seguridad real, sin
mock) como regresión. Además se confirmó que la causa raíz ya estaba
diagnosticada en `RIKOS-decisiones-tecnicas.md`, DT-049
(`LazyInitializationException` con `open-in-view` desactivado) — coincide
exactamente con la hipótesis del pedido original.

**P-29 (el despliegue a nube no podía arrancar)**: ya se había corregido en
una sesión anterior siguiendo la corrección pedida por una segunda
revisión — `GuardaSemillaDesarrollo` desactiva el usuario semilla en vez de
frenar el arranque. Sigue así, sin cambios en este sprint.

## 2. US-006 — servicio transversal de auditoría

### El servicio

`RegistradorAuditoria` vive en `pe.rikos.operacion.comun.auditoria` (el
paquete que ya estaba reservado para esto, con solo un `.gitkeep`),
expuesto como `@NamedInterface("auditoria")`. Resuelve el usuario y el
local activo **solos**, leyendo `SecurityContextHolder` — `principal` es el
usuario (poblado por `FiltroAutenticacionJwt` desde el JWT), `details` es
el local activo (agregado a ese filtro este sprint, leyendo el claim
`localActivo` del token, que ya existía pero nadie usaba para esto). Ningún
caso de uso pasa el actor a mano.

"Se escribe en la misma transacción que el hecho auditado": el servicio no
abre transacción propia — corre dentro de la del caso de uso que lo llama.
Si esa transacción hace rollback, la auditoría desaparece con ella
(probado explícitamente en `RegistradorAuditoriaTest`).

Serialización mínima a JSON de texto (sin Jackson, no está en el classpath
de este módulo — mismo motivo documentado desde el sprint 1 para el caso
original de `cambio_permiso`). `RegistradorAuditoria.campos(...)` arma el
mapa; `RegistradorAuditoria.extraerCampo(json, clave)` es la contraparte
para leer, usada por el humanizador de la bitácora.

### Migración

`V18__auditoria_cambio_estado_y_eliminacion.sql`: el CHECK de `evento` no
contemplaba `cambio_estado` ni `eliminacion` (eventos que `01` § Auditoría
transversal no preveía, porque nacieron de una decisión posterior del
equipo -- DT-043/DT-051 -- de permitir el borrado real). Postgres no deja
alterar un CHECK in-place: se borra y se recrea con la lista ampliada.

### Qué quedó cableado

- **`cambio_estado`**: `CambiarEstadoLocal`, `ActivarProducto`/
  `DesactivarProducto`, `ActivarProveedor`/`DesactivarProveedor`,
  `CambiarEstadoUsuario`, y (agregado por US-012) `CambiarEstadoMotivo`.
  **Rol NO tiene** -- el modelo se quedó corto ahí: `Rol.activo` existe
  como columna (sembrada en `TRUE` por V9) pero ningún caso de uso ni
  endpoint la toca nunca. No se inventó uno para este sprint -- no estaba
  pedido y Rol ya tiene su propio mecanismo de baja (`EliminarRol`,
  bloqueado si es de sistema o tiene usuarios asignados).
- **`eliminacion`**: los 6 `Eliminar*` que ya existían (`EliminarLocal`,
  `EliminarProducto`, `EliminarProveedor`, `EliminarCategoria`,
  `EliminarRol`, `EliminarUsuario`). Cada uno captura un snapshot del
  registro (nombre/código/razón social, lo que tenga sentido) **antes** de
  borrar, como `valorAnterior`; `valorNuevo` queda `null`. Caso especial:
  `EliminarUsuario` corre su DELETE en una transacción `REQUIRES_NEW`
  (documentado desde el sprint 2, por el problema de transacción abortada
  de Postgres) -- el registro de auditoría se escribe **dentro** de esa
  misma transacción nueva, no en la del llamador, para que ambos se
  confirmen o aborten juntos.
- **`cambio_precio`**: `ActualizarProducto`, solo si `precioVenta`
  realmente cambió (no en cada edición que toca otros campos).

Los eventos de `cambio_estado`/`cambio_precio` guardan también un campo
`"nombre"` (o `razonSocial` para proveedor) en el JSON -- necesario para
que la bitácora pueda armar la frase humana sin tener que consultar el
módulo dueño de esa entidad (ver más abajo, por qué eso importaba para no
violar los límites de módulo).

### ADM-31 (backend): `GET /auditoria`

`ListarAuditoria` + `AuditoriaController`, en `identidad` (no en
`comun.auditoria`): necesita resolver nombres de usuario y de local, que
`identidad` ya es dueño -- `comun.auditoria` solo conoce el evento crudo.
Filtros: local, usuario, evento, rango de fechas (los cuatro que pide CA3).
Permiso nuevo: **`auditoria.ver`** -- se eligió crear uno en vez de
reusar `identidad.ver`, porque la bitácora cruza todos los módulos, no
solo identidad.

Descripción humanizada real, no el volcado crudo: *"admin desactivó el
producto "Arroz corriente""*, *"admin cambió el precio de "Arroz
corriente" de S/ 12.00 a S/ 13.50"*. El JSON crudo (`valorAnterior`/
`valorNuevo`) queda disponible para el detalle al expandir.

**Hallazgo real durante la construcción**: el primer diseño de
`AuditoriaRepositorio` extendía `JpaSpecificationExecutor` para el filtro
dinámico -- esa interfaz trae su **propio** `delete(Specification)`,
descubierto por la propia prueba de inmutabilidad que se escribió para
CA4. Corregido: el filtro se arma a mano con `EntityManager`/
`CriteriaBuilder` en `ListarAuditoria`, sin esa interfaz. El repositorio
final extiende `Repository<Auditoria, String>` a secas (ni
`JpaRepository` ni `CrudRepository`) -- los métodos `delete*`/`update` no
existen para llamar, no es una convención que alguien podría romper.

### CA4 (inmutabilidad)

`InmutabilidadDeAuditoriaTest`: por reflexión, confirma que
`AuditoriaRepositorio` no expone ningún método `delete*`/`update`, y que
`AuditoriaController` no tiene ningún `@PostMapping`/`@PutMapping`/
`@PatchMapping`/`@DeleteMapping`. No se restringió a nivel de base de
datos (permisos de usuario de Postgres) -- la app se conecta con un único
rol sin roles diferenciados por operación, y separar eso es un cambio de
infraestructura fuera del alcance de este sprint; queda anotado como
pendiente si algún día importa más que la garantía de aplicación.

## 3. US-012 — Motivos configurables

Tabla `motivo` ya existía desde V13 (sprint 1 ampliado), sin datos ni
pantalla. Construido: `Motivo`/`MotivoRepositorio` en `catalogo`
(**no** en `identidad` -- un primer intento accidental los había puesto ahí
como stub sin terminar, causando una colisión de nombre de bean que rompía
~94 tests; se encontró y se borró antes de seguir). `CrearMotivo`,
`ActualizarMotivo`, `CambiarEstadoMotivo` (con auditoría `cambio_estado`),
`MotivoController` (`/motivos`, con los mismos permisos `catalogo.*` que
productos/proveedores -- no se inventó un permiso nuevo).

**Nace vacío a propósito**: sin datos semilla (RF-005 pendiente del
propietario). La pantalla (`Configuración → Motivos`, ya existía el ítem
de menú, solo hacía falta cablearlo a una ruta real) agrupa por los 6
tipos en pestañas, y cada pestaña vacía explica en una frase qué tipo de
motivo va ahí -- no seis listas en blanco sin contexto.

**Alcance honesto, tal como se pidió**: CA2 (no se puede confirmar sin
motivo) y CA3 (la observación se guarda con el movimiento) **no se pueden
demostrar todavía** -- ningún consumidor real existe aún (ajustes de
inventario son del sprint 9, marcado de agotados del 4). Motivo queda
construido como catálogo puro: alta, edición, activar/desactivar. Sin
confirmación modal antes de desactivar (a diferencia de Producto/
Proveedor) -- Motivo no tiene impacto en cascada como el stock, un clic
alcanza; se puede agregar después si aparece una razón de negocio real.

## 4. ADM-31 (frontend): pantalla de bitácora

`features/auditoria/PantallaAuditoria.tsx`. Filtros (local, usuario,
evento, rango de fechas) con dropdowns de nombres, no IDs sueltos.
Paginación server-side numerada (no "cargar más") -- la bitácora puede
crecer mucho y ya viene paginada del backend. Cada fila muestra la frase
humanizada + quién + cuándo; expandir muestra entidad, JSON anterior/nuevo
y observación.

Requirió una pieza que no existía: `SesionContexto` no tenía forma de
saber los permisos efectivos del usuario en el frontend (el ítem de menú
de la bitácora necesita ocultarse sin `auditoria.ver`). Se agregó
`tienePermiso(codigo)`, cargado una vez por sesión.

**Nota de eficiencia, no bug**: esa carga llama a
`GET /usuarios/{id}/permisos` en cada sesión nueva, pero el login
(`POST /auth/login`/`POST /auth/pin`) **ya devuelve** los permisos
efectivos en el propio token/respuesta (`SesionIniciada.permisos` en el
backend) -- la interfaz `SesionIniciada` del frontend nunca declaró ese
campo, así que quedaba ahí sin usarse. Quedó como una llamada de red
redundante, no como un error: funciona, pero sería más simple leer
`sesion.permisos` directo una vez que el tipo del frontend lo declare.
Anotado para una próxima pasada, no corregido en este sprint.

## 5. HT-06 — verificación automática de fronteras

### Backend: `ArquitecturaDeModulosTest`

Escrita con `spring-modulith-starter-test` (ya en el classpath). Primera
corrida: **4 tipos distintos de hallazgo**, reportados antes de tocar
nada (como se pidió explícitamente):

1. `catalogo` e `identidad` usan `comun.web.ErrorRespuesta` (del trabajo
   de tipado de OpenAPI de una sesión anterior) sin que `comun.web`
   estuviera expuesto. **Corregido**: `@NamedInterface("web")` agregado --
   mecánico, mismo patrón ya usado dos veces antes.
2. `comun` (el `GuardaSemillaDesarrollo` de P-29) usa
   `identidad.infraestructura.Usuario`/`UsuarioRepositorio` sin
   exposición. **Corregido** igual: `@NamedInterface("infraestructura")`
   en `identidad.infraestructura`.
3. **Un ciclo real entre `comun` e `identidad`** (no una simple violación
   de encapsulación): `comun.seguridad.ConfiguracionSeguridad` depende de
   `identidad.infraestructura.seguridad` (desde el sprint 1, para poder
   conectar `FiltroAutenticacionJwt` al filtro HTTP), e `identidad`
   depende de `comun.auditoria`/`comun.web` (de este mismo sprint). Ambas
   direcciones son estructuralmente necesarias tal como está diseñado hoy
   -- exponer los paquetes (punto 1 y 2) no rompe el ciclo, solo legitima
   cruzarlo. **No se tocó.** Deshacer esto en serio significaría mover
   `ConfiguracionSeguridad` a `identidad` (pero protege rutas de
   `catalogo` también, así que es genuinamente transversal) o replantear
   dónde vive la seguridad JWT -- una decisión de arquitectura real, no
   mecánica, que no correspondía tomar sin decirlo primero.

Estado final: `losModulosRespetanSusLimites` sigue en rojo, únicamente por
el ciclo `comun ↔ identidad`. Las 3 violaciones de encapsulación puntuales
quedaron resueltas.

### Frontend: regla de ESLint de fronteras

Hallazgo previo a la regla: `rikos-web` **no tenía ESLint configurado en
absoluto** (ni dependencia, ni script `lint`). Se levantó desde cero
(config plana, ESLint 10, typescript-eslint 8) antes de poder agregar la
regla de fronteras.

Regla real: `eslint-plugin-boundaries`, aplicada solo a
`src/features/**/*.{ts,tsx}` -- prohíbe importar de otra `features/x`
salvo por su `index.ts` público; mismo import dentro de la propia feature,
sin restricción. Verificada de verdad (no solo agregada): un archivo de
prueba con un import prohibido, confirmado que ESLint lo marca, borrado
después.

**Violaciones encontradas: ninguna** -- los únicos cruces entre features
ya pasaban por el índice público. De paso aparecieron 5 problemas sueltos
sin relación con fronteras (1 variable sin usar, 4 warnings de
`react-hooks/exhaustive-deps`) -- no corregidos, tal como se pidió.

Gap para tener en cuenta: 6 de 11 carpetas de features todavía no tienen
`index.ts` real (vacías o sin construir) -- la regla no tiene nada que
hacer ahí todavía, se activa sola en cuanto tengan archivos.

## 6. P-22 (si quedaba tiempo)

Ya estaba cerrado desde la sesión anterior: 43/43 endpoints reales
anotados con `@ApiResponses`/`@Schema` para OpenAPI. Los nuevos endpoints
de este sprint (`AuditoriaController`, `MotivoController`) siguen el mismo
patrón desde el vamos.

## Bugs encontrados en el camino

- **Stub de `Motivo` en el módulo equivocado** (`identidad` en vez de
  `catalogo`), sin terminar, causando colisión de nombre de bean que
  rompía ~94 tests. Encontrado y borrado antes de construir la versión
  real.
- **`JpaSpecificationExecutor` trae su propio `delete(Specification)`**
  -- incompatible con la garantía de inmutabilidad de CA4. Encontrado por
  la propia prueba de inmutabilidad, no por inspección manual.
- **`vite.config.ts` "corregido" para apuntar directo a `servicio-operacion`
  (8081) en vez del gateway (8080)**: un agente delegado, al ver fallar las
  llamadas porque el gateway no estaba corriendo en su sesión, cambió el
  proxy para saltárselo -- eso habría roto `/facturacion` en silencio (ese
  servicio no vive en 8081). Revertido; se levantó el gateway real en su
  lugar y se confirmó la cadena completa `gateway:8080 → servicio-operacion:8081`
  funcionando de punta a punta antes de dar por buena la verificación
  del agente.

## Cómo se verificó

- Backend: **127/127 verde** (`ArquitecturaDeModulosTest` excluido a
  propósito -- ver arriba, falla por el ciclo conocido, no por un bug).
- Frontend: `npx tsc -b --force` limpio. `npm run lint`: 0 errores nuevos,
  0 violaciones de fronteras.
- Fila real de auditoría confirmada para cada evento nuevo: `cambio_estado`
  (Local, Producto, Proveedor, Usuario, Motivo), `eliminacion` (los 6
  `Eliminar*`), `cambio_precio` (Producto) -- con capturas de la bitácora
  real mostrando filtros aplicados y el detalle expandido.
- Capturas de Motivos: estado vacío por tipo (con su explicación) y estado
  con datos (activar/desactivar/editar funcionando).

## Revisión posterior al cierre: 4 hallazgos reales sobre auditoría

Una segunda revisión, sobre el trabajo ya cerrado de US-006, encontró 5
puntos. Investigados los 5 antes de tocar nada: 4 eran reales, 1 era falsa
alarma (`auditoria.ver` sí estaba asignado a "administrador central" y
"administrador de local" -- verificado en la base de datos real, no solo
en el código).

### 1. El JSON de auditoría se armaba a mano -- y el motivo ya no existía

El comentario original decía "Jackson no está en el classpath de este
módulo" (nota del sprint 1, nunca revisada de nuevo). Falso: `mvn
dependency:tree` muestra `jackson-databind` en scope `compile`, llegado
vía `jjwt-jackson`. El riesgo que señaló la revisión era real: un producto
llamado `Pollo "a la brasa"` corrompía el JSON guardado sin ningún error
al escribirlo -- esa fila quedaba ilegible para siempre. `RegistradorAuditoria`
ahora usa `ObjectMapper` real (`writeValueAsString`/`readTree`) en vez de
concatenar strings a mano. Test de regresión con ese nombre exacto.

### 2. El local de la auditoría podía quedar equivocado

`cambiarLocalActivo` en el frontend solo actualizaba estado de React --
nunca volvía a pedir el token. El JWT (y por lo tanto el local que lee
`RegistradorAuditoria`) quedaba con el local del login para siempre.
Corregido con la opción que sugería la propia revisión: reemitir el
token. Nuevo endpoint `POST /auth/cambiar-local` (`CambiarLocalActivo`,
valida que el usuario tenga ese local asignado antes de firmar el token
nuevo) y el frontend ahora espera la respuesta real en vez de mutar
estado local.

### 3. `auditoria.ver` sin asignar -- falsa alarma

Verificado en la base de datos real (no solo en el código): sí estaba
asignado a "administrador central" y "administrador de local". Nada que
corregir.

### 4. Faltaban los intentos denegados (caso crítico 19)

`ManejadorGlobalDeErrores` ya relanzaba `AccessDeniedException` sin
tocarla (a propósito, para no pisar el 401/403 real con un 500) -- pero
nadie la auditaba antes de dejarla pasar. Nuevo
`AuditorDeAccesoDenegado` (`AccessDeniedHandler`), conectado en
`ConfiguracionSeguridad`, que audita el evento `acceso_denegado` (nuevo,
migración V19) antes de delegar en el manejador por defecto de Spring
Security. Sin entidad de negocio propia a la que apuntar -- por eso es
un 403 -- así que el método + la ruta van en `observacion`, el único
campo de texto libre.

### 5. Sin pruebas por rol para `/auditoria` ni `/motivos`

`AuditoriaAccesoPorRolTest` (6 casos, uno por rol) y
`MotivoAccesoPorRolTest` (4 casos) -- mismo patrón que
`CatalogoAccesoPorRolTest` del sprint 2. Confirmaron algo real del propio
modelo de permisos: "administrador de local" puede *ver* el catálogo
(incluido Motivo) pero no crear ni modificar -- el catálogo es central,
no se edita por local. `AuditorDeAccesoDenegadoTest` cierra el círculo:
confirma que un mozo real, con un 403 real, deja una fila de auditoría
real.

### Bug encontrado en el camino (de esta revisión)

Un test nuevo (`CrearMotivoTest`, ya escrito por el agente de Motivos)
empezó a fallar por una colisión con datos reales: "Merma por rotura"
(`ajuste_inventario`) ya existía en la base de datos de desarrollo,
creado por el propio agente al verificar la pantalla en vivo, y nunca se
limpió. El índice único parcial (`tipo`, `descripción` entre activos)
hizo exactamente lo que debía. Se borró la fila de sobra; no era un bug
de código.

### Cómo se verificó

Backend: **142/142 verde** (`ArquitecturaDeModulosTest` sigue excluido a
propósito, mismo ciclo `comun↔identidad` ya reportado y no resuelto).
Frontend: `npx tsc -b --force` limpio, `npm run lint` sin violaciones
nuevas (mismos 5 problemas sueltos de antes, sin relación).

## Rutas para volver a mirar en vivo (logueado como admin/cambiar123)

- `/admin/configuracion/motivos` -- catálogo de motivos, 6 pestañas
- `/admin/auditoria` -- bitácora, con filtros
