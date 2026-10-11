# US-004/US-006: permisos granulares y roles (2026-09-23)

**No hay capturas .png de esta pasada** -- se verifico todo en vivo con el
navegador embebido de esta sesion, pero esa herramienta solo devuelve la
imagen inline al chat, no permite guardarla a disco, y no se armo un paso
aparte para exportarla. Lo que sigue es la descripcion textual completa de
lo que se construyo y como se verifico; si alguien necesita las imagenes,
las rutas de abajo son exactamente las que hay que abrir (logueado como
`admin` / `cambiar123`).

## Que se pidio

Adelantar del sprint 3 (a pedido del usuario) permisos granulares por
usuario (excepciones que ganan sobre el rol, US-004) y administracion de
roles con auditoria (US-006). Requisito explicito: el modelo ya soportaba
esto (secciones 3.4-3.10 y 13.1 de `RIKOS-modelo-de-datos.md`) -- no
inventar tablas nuevas sin avisar.

## Que se construyo

- `ALTER TABLE rol ADD COLUMN color`
- `usuario_permiso_excepcion` (excepcion por usuario, `otorgado` bool +
  `motivo`)
- `motivo` (catalogo de motivos, sin UI todavia)
- `auditoria` (escribe en cada cambio de `rol_permiso` o
  `usuario_permiso_excepcion`)

Migracion: `V13__permisos_y_auditoria.sql`.

### Catalogo de permisos: 49, en 12 modulos

`identidad, catalogo, recetas, menu, pensionistas, venta, caja, inventario,
compras, delivery, facturacion, reportes`, cruzados con hasta 11 acciones
(`ver, crear, modificar, desactivar, aprobar, anular, descontar, reabrir,
ver_costos, exportar, administrar_permisos`) -- curado a mano, no mecanico
(ej. `recetas.ver_costos` existe, `caja.ver_costos` no; `venta.reabrir`
existe, `catalogo.reabrir` no). Sembrado por `SembradorCatalogoPermisos`
(idempotente, se auto-repara si se borra algo a mano -- confirmado en vivo
durante las pruebas).

### Los 6 roles del sistema, con sus permisos reales

`administrador central` (49, todos), `administrador de local` (39, todo
menos `identidad.administrar_permisos` y unos pocos mas),
`cajero` (14: caja + venta + ver catalogo/menu/pensionistas + facturacion),
`mozo` (5), `cocina` (5), `motorizado` (2). Todos `es_sistema=true`: no se
pueden renombrar ni borrar, pero si editar sus permisos/color/descripcion.

### Resolucion de permiso efectivo

**Regla:** el permiso efectivo es la union de los permisos de todos los
roles del usuario, mas las excepciones -- la excepcion siempre gana.
Se resuelve una vez en el login (no por request) y va embebido en el JWT
(claim `permisos`, ademas del ya existente `roles`).

### Aplicacion real, no solo ocultar el boton

`ConfiguracionSeguridad` exige `PERM_identidad.administrar_permisos` para
`PUT /roles/{id}/permisos` y `PUT /usuarios/{id}/permisos`, aparte del
chequeo por rol que ya protegia el resto de `/roles/**` y `/usuarios/**`.

## Que se dejo fuera y por que

Permisos deliberadamente omitidos: `inventario.crear` (el stock solo
cambia via compras/ventas), `caja.modificar` (un movimiento de caja no se
edita), `reportes.crear/modificar/desactivar` (los reportes se generan, no
se autorean), `facturacion.modificar/desactivar` (un comprobante emitido no
se edita ni desactiva, solo se anula via `venta.anular`).

Fuera de alcance (no se pidio, no se toco): marketing/fidelizacion,
reclutamiento/personal, ordenes de compra y cuentas por pagar como
concepto separado de "compras", zonas de delivery, tipos/categorias de
clientes, reservas de mesas, integraciones, salones.

## Como se verifico

- Backend: `mvn test` → **23/23 pruebas verdes** (incluye las 3 nuevas de
  `ResolverPermisosEfectivosTest`, que prueban rol-solo, excepcion que
  agrega, y excepcion que quita ganandole al rol).
- `tsc --noEmit` limpio en `rikos-web`.
- **Prueba end-to-end real** (no solo unit test): se creo el usuario real
  `mtorres` (login `mtorres` / `clave123`) con rol `cajero` (14 permisos),
  se le agrego la excepcion `identidad.ver: otorgado` (con motivo) y
  `venta.reabrir: retirado` desde `/admin/usuarios/{id}/editar`, seccion
  "Permisos individuales". Un login real con ese usuario devuelve un JWT
  cuyo claim `permisos` tiene exactamente 14 codigos: los 13 restantes de
  cajero (sin `venta.reabrir`) mas `identidad.ver`. Confirmado por
  inspeccion directa del JWT decodificado.
- **403 real, no solo boton oculto**: con el JWT real de `mtorres` (que no
  tiene `identidad.administrar_permisos`): `PUT /api/roles/{id}/permisos`
  -> **403**, `GET /api/roles` -> **403** (mtorres es cajero, no
  administrador).
- **Distincion sistema/personalizado**: se creo un rol nuevo
  ("supervisor de turno", 4 permisos de caja) y si se pudo renombrar/editar
  sin restriccion, a diferencia de los 6 de sistema.
- **Auditoria real** (consulta directa a Postgres despues de las pruebas
  de arriba, no son datos de ejemplo):
  ```
  evento=cambio_permiso  entidad=rol_permiso                entidad_id=<id supervisor de turno>
    anterior=[]  nuevo=["caja.anular","caja.crear","caja.reabrir","caja.ver"]

  evento=cambio_permiso  entidad=usuario_permiso_excepcion   entidad_id=<id de mtorres>
    anterior=[]  nuevo=["identidad.ver:otorgado","venta.reabrir:retirado"]
  ```

## Bugs encontrados en el camino

1. **Pre-existente, no introducido en esta pasada**: el rol
   "administrador de local" (con "de") nunca hacia match contra
   `hasAnyRole(..., "ADMINISTRADOR_LOCAL")` (sin "de") --
   `FiltroAutenticacionJwt` arma la autoridad como
   `ROLE_ADMINISTRADOR_DE_LOCAL`, conservando el "DE". Cualquier usuario
   con ese rol recibia 403 en toda `/locales/**`, `/usuarios/**`,
   `/roles/**` desde que el rol existe. Corregido el matcher en
   `ConfiguracionSeguridad`.
2. **Frontend -- edicion de rol**: `PantallaRolFormulario.tsx` cargaba el
   catalogo de permisos y los permisos asignados al rol, pero nunca el
   nombre/descripcion/color del rol mismo -- esos campos quedaban vacios
   al editar. Corregido: ahora tambien llama `listarRoles` y precarga esos
   campos; el nombre queda deshabilitado si `esSistema` (con una nota
   explicando por que).
3. **Frontend -- insignias de rol**: en `/admin/usuarios` (lista de
   usuarios), con nombres largos ("administrador central",
   "administrador de local") la insignia se veia como un bloque de color
   solido sin texto -- `height: 20px` fijo mas el texto envolviendo a dos
   lineas lo dejaba fuera del area visible. Corregido en `global.css`
   (`.usuario-rol-insignia`) con `white-space: nowrap`.

Ninguno de los dos bugs de frontend es un problema del modelo de datos ni
de la logica de permisos -- son bugs de UI que no se habian notado porque
hasta esta pasada nadie habia entrado a editar un rol existente ni mirado
la lista de usuarios con roles de nombre largo asignados.

## Nota operativa (sin relacion con el codigo)

Docker Desktop (WSL2) se corrompio a mitad de sesion (`could not open file
"base/16384/..." : I/O error` en Postgres) y tumbo Postgres/RabbitMQ/
Mailpit y con ellos el backend. Se reinicio Docker Desktop y los tres
contenedores, sin perdida de datos -- las filas de auditoria de arriba se
generaron despues de esa recuperacion, contra la misma base de datos.

## Rutas para volver a mirar en vivo (logueado como admin)

- `/admin/usuarios/roles` -- grilla de 6 roles + 1 de prueba
- `/admin/usuarios/roles/{id}/editar` -- edicion con permisos agrupados
- `/admin/usuarios` -- lista de usuarios (incluye `mtorres`, de prueba)
- `/admin/usuarios/{id}/editar` -- los 4 bloques, incluida la seccion
  colapsable "Permisos individuales" con el origen (rol/excepcion) por
  permiso

## Pantalla de Usuarios: lista con ficha al lado (2026-09-29)

**Qué se pidió.** Rediseñar la lista de usuarios (antes una tabla) como lista de cuentas con la
ficha de la persona seleccionada al lado, en el tema verde. Diseño primero en el canvas
("Usuarios (lista y detalle)"), aprobado antes de programar.

**Qué se construyó.**
- Backend: `UsuarioRespuesta` trae ahora `locales` (todos los asignados, no solo el principal) y
  cada rol su `descripcion`. Caso de uso nuevo `ObtenerActividadUsuario` y ruta
  `GET /usuarios/{id}/actividad`: mezcla los últimos accesos (`sesion`) con lo que la persona
  hizo como actor (`auditoria`), lo más reciente primero, tope de 5. Devuelve datos crudos
  (tipo, evento, entidad); el texto lo arma la pantalla. Test `ObtenerActividadUsuarioTest`.
- Frontend: `PantallaUsuarios.tsx` (lista + `FichaUsuario`), `estilos/usuarios.css`. Filtros por
  local, búsqueda y Todos/Activos/Inactivos. Ficha con Acceso (rol y su descripción, local
  principal, locales), Seguridad (último acceso con IP) y Actividad reciente. Editar abre el
  formulario existente; el menú ⋮ ofrece Eliminar (con el aviso de historial); el botón de abajo
  desactiva o reactiva la cuenta con confirmación.
- El administrador central figura con "Todos los locales" (opera en cualquier local activo), no
  con su única asignación.

**Se dejó fuera, a propósito.**
- **"Restablecer contraseña / Enviar enlace de acceso"** del diseño: no hay envío de correos ni
  flujo de restablecimiento en el sistema. Queda para cuando exista (correo transaccional).
- El texto de los eventos de actividad cubre los eventos de auditoría actuales; uno nuevo se
  muestra con su nombre crudo hasta agregarlo a `VERBO_EVENTO` en la pantalla.

**Corrección de una regresión propia.** El componente `Interruptor` del asistente de locales
usaba la clase `.interruptor`, que ya existía para el formulario de usuario (otro tamaño y
marcado); se renombró a `.conmutador` para no pisarla.

**Verificado en vivo** (admin): lista de 6 cuentas, ficha con rol/descripción/locales/último
acceso/actividad, filtros (local, búsqueda, inactivos), desactivar y reactivar con
confirmación, menú ⋮ (Escape lo cierra). `npx tsc -b --force` sin errores; suite backend 222
pruebas, solo falla `ArquitecturaDeModulosTest` (ciclo `comun ↔ identidad`, ya reportado).

## Roles rediseñados: tarjetas, crear/editar con permisos por módulo y "restaurar predeterminados" (2026-09-29)

**Qué se pidió.** Rehacer Roles (lista), Crear rol y Editar rol en el tema verde, diseñado primero
en el canvas ("Roles (tarjetas)", "Crear rol (permisos por módulo)", "Editar rol cajero
(restaurar predeterminados)"). En la edición de un rol del sistema, poder cambiar permisos y
volver a los predeterminados.

**Qué se construyó.**
- Backend:
  - `GET /roles/{id}/permisos-predeterminados` (caso de uso `ObtenerPermisosPredeterminados`):
    los permisos con los que nació un rol del sistema, tomados del catálogo del sembrador
    (`SembradorCatalogoPermisos.codigosPredeterminados`). Vacío para roles personalizados. No
    modifica nada: la pantalla los carga en el formulario y recién se guardan con "Guardar
    cambios".
  - `RolRespuesta.cantidadUsuarios` (`UsuarioRolRepositorio.countByRolId`).
  - Pruebas en `PermisosPredeterminadosTest`.
- Frontend: `PantallaRoles.tsx` (roles del sistema y personalizados en secciones),
  `PantallaRolFormulario.tsx` (una sola pantalla para crear y editar), `rolesVisual.ts` (iconos,
  descripción de módulos y acciones sensibles), `estilos/roles.css`.
  - Nombre bloqueado en roles del sistema; descripción y color editables.
  - Marcas "Añadido"/"Quitado" y punto ámbar en el módulo, frente al predeterminado.
  - Modal de confirmación que lista lo que se va a deshacer.
  - "Acceso sensible" derivado de la acción (anular, reabrir, descontar, aprobar, ver_costos,
    administrar, administrar_permisos), sin campo nuevo en el backend.
  - "Lo tienen" en cada permiso (qué roles ya lo incluyen).
  - "Revisar accesos seleccionados" con el bloque de cambios.

**Bug de fondo encontrado al diseñar la edición.** `SembradorCatalogoPermisos` corre en cada
arranque y solo agregaba: si un administrador quitaba un permiso a un rol del sistema, el
siguiente arranque lo volvía a poner. Ahora un rol que ya tiene permisos solo recibe los
permisos que acaban de nacer en el catálogo; un rol vacío (primera vez) recibe todos. Prueba:
`elSembradorNoRestituyeUnPermisoQueSeQuito`.

**Se dejó fuera, a propósito.**
- El botón "restaurar" no existe en roles personalizados (no tienen predeterminado).
- La descripción por módulo y los iconos viven en el frontend, no en la base.
- El aviso "3 personas tienen este rol" cuenta asignaciones en `usuario_rol`.

**Verificado en vivo** (admin): lista con 6 roles del sistema (52/14/5/6/2/41 permisos, con
cantidad de usuarios) y los personalizados; editar cajero (14 de 52, nombre bloqueado, sensibles
marcados), quitar y añadir permisos, marcas y puntos, revisión, modal y restaurar; guardar un
cambio y volver a restaurar y guardar (cajero volvió a 14); crear un rol propio (validaciones,
color, guardado) y eliminarlo. `npx tsc -b --force` sin errores; suite backend en verde salvo
`ArquitecturaDeModulosTest` (ciclo `comun ↔ identidad`, ya reportado).
