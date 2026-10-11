# US-008/US-009/US-011: catálogo de productos, unidades y proveedores (2026-09-24)

Capturas reales en `capturas-catalogo/` (logueado como `admin` /
`cambiar123`), tomadas durante la verificación en vivo de esta misma
sesión.

## Qué se pidió

Sprint 2 completo: US-008 (catálogo central de productos, con un
requisito ad-hoc del usuario -- costo inicial manual, no parte del
backlog original, a reemplazar por datos reales de compra en el sprint
9), US-009 (unidades de medida, presentaciones de compra y conversiones
entre unidades, todo por-producto, nunca global), US-011 (proveedores,
con RUC opcional para permitir compras informales tipo mercado de
abastos). Más infraestructura: HT-05 (generar tipos TypeScript del
frontend desde el OpenAPI del backend, vía springdoc) y HT-07
(componente de teclado numérico compartido, configurable en cantidad de
dígitos y modo oculto, usado hoy por las pantallas de PIN y pensado para
cantidad/efectivo más adelante). Además, "9 bis": generalizar el patrón
de submenú colapsable ya existente en "Usuarios y permisos" a **todo**
el sidebar, con acordeón animado + modo riel (icono-solo, con flyout) en
escritorio.

Instrucción explícita del usuario: usar el sistema de skills
"superpowers" (brainstorming → writing-plans → executing-plans → TDD)
para todo este trabajo, no código ad-hoc. Implementado en la misma
sesión, sin git (decisión explícita del usuario: "No, sin git") -- el
punto de control por tarea fue `mvn test` / el build real de TypeScript,
con cada desviación registrada en
`docs/superpowers/plans/2026-09-24-sprint2-catalogo-progress.md`.

## Qué se construyó

### Backend (`servicio-operacion`, módulo `catalogo`)

- **Migraciones**: `V14__catalogo.sql` (7 tablas: `unidad_medida`,
  `categoria_producto`, `producto`, `producto_servicio`,
  `presentacion_compra`, `conversion_unidad`, `proveedor`),
  `V15__semilla_catalogo.sql` (5 unidades, 37 categorías reales del
  menú), `V16__corregir_longitud_ids_semilla.sql` (corrección de IDs
  sembrados con 25 caracteres en vez de 26 -- ver bugs abajo).
- **Entidades y repositorios**: `CategoriaProducto` (árbol vía
  `categoria_padre_id`), `UnidadMedida`, `Producto`, `ProductoServicio`
  (clave compuesta, patrón `@Id` plano + `@ManyToOne` de solo lectura),
  `PresentacionCompra`, `ConversionUnidad`, `Proveedor`.
- **Casos de uso**: crear/actualizar categoría; crear/actualizar/
  desactivar producto, registrar costo manual (rechaza el reenvío si
  `origenCosto` ya es `compra`, con el error `COSTO_YA_CONFIRMADO_POR_COMPRA`);
  asignar servicios (canales de venta) a un producto; crear presentación
  de compra y conversión de unidad; crear/actualizar/desactivar
  proveedor.
- **Controladores**: `CategoriaController`, `UnidadController`,
  `ProductoController`, `PresentacionController`, `ConversionController`,
  `ProveedorController` -- todos con `@Transactional`/
  `@Transactional(readOnly = true)` a nivel de método (ver bug de
  `LazyInitializationException` abajo).
- **Permisos**: `ConfiguracionSeguridad` extendida --
  GET → `catalogo.ver`, POST desactivar/activar → `catalogo.desactivar`,
  POST general → `catalogo.crear`, PUT → `catalogo.modificar`. `mozo`
  tiene `catalogo.ver` (necesita ver el catálogo para tomar pedidos),
  `cocina` tiene `catalogo.ver` + `catalogo.modificar` (marca agotados),
  `motorizado` no tiene ningún permiso de catálogo.

### HT-05: tipos generados desde el backend real

`springdoc-openapi-starter-webmvc-ui 2.8.6` (única versión disponible,
confirmada compatible con Spring Boot 4.1.1/Spring 7 probando en vivo,
no asumiendo por número de versión) expone `/api/v3/api-docs`.
`openapi-typescript` genera `rikos-web/src/api/generado/esquema.d.ts`
contra ese endpoint (`npm run generar-tipos`). `cliente.ts` usa
`Required<Esquemas['XRespuesta']>` para conservar el contrato de tipos
no-opcionales (springdoc marca todo como opcional por defecto) mientras
la FORMA -- nombres de campos -- viene del backend real. Migrados a este
patrón: `Local`, `Rol`, `Permiso`, `PermisoUsuario`, `RolResumen`,
`LocalPrincipalResumen`, `UltimoAcceso`, `Usuario`, `Categoria`,
`Unidad`, `Producto`, `Presentacion`, `ConversionUnidad`, `Proveedor`.
`UsuarioDetalle` y `ExcepcionPermiso` quedan escritos a mano a propósito
(documentado en el propio archivo por qué).

### HT-07: teclado numérico compartido

`EntradaNumerica` (envuelve `TecladoNumerico`, ya existente) con props
`{valor, onCambiar, largoFijo?, oculto?, alCompletar?, deshabilitado?}`.
Integrado en `PantallaPin.tsx` (login) y el campo PIN de
`PantallaUsuarioFormulario.tsx`.

### Frontend: pantallas del catálogo

- `PantallaCategorias.tsx` -- árbol de categorías con
  "Agregar subcategoría"/"Editar" por fila.
- `PantallaProductos.tsx` -- lista con filtros (buscar, categoría,
  naturaleza, estado).
- `PantallaProductoFormulario.tsx` -- crear/editar producto, con el
  bloque de costo inicial condicionado a `esInsumo` y deshabilitado si
  `origenCosto === 'compra'`.
- `PantallaUnidadesYPresentaciones.tsx` -- 3 pestañas (Unidades,
  Presentaciones de compra, Conversiones), cada una con preview en vivo
  del factor antes de guardar (ej. "1 Saco 50 kg = 50 kg").
- `PantallaProveedores.tsx` / `PantallaProveedorFormulario.tsx` --
  CRUD con RUC opcional; sin RUC se etiqueta "— (informal)".

### 9 bis: sidebar generalizado

`BarraLateral.tsx` -- árbol `NAVEGACION` completo (Inicio, Negocio,
Administración, Usuarios y permisos), acordeón que no navega al hacer
clic en el padre, anima con `grid-template-rows: 0fr → 1fr`, recuerda el
estado abierto/cerrado en `localStorage`, se auto-abre en la ruta activa
(incluso con hijos mixtos pendiente/activo). Modo riel (icono-solo) en
escritorio con flyout por hover/click, accesible por teclado, excluido
en móvil. *Nota: este archivo fue modificado externamente a mitad de
sesión (no por este trabajo) -- ver "Bugs encontrados" para el detalle
de qué cambió y cómo se verificó que no rompió nada de este sprint.*

## Qué se dejó fuera y por qué

- **Costo inicial manual** es explícitamente temporal: el modelo real de
  costeo (sprint 9) vendrá de compras confirmadas, no de un campo
  editable a mano. Se implementó tal cual lo pidió el usuario, con el
  campo deshabilitándose solo una vez que el producto tiene
  `origenCosto = 'compra'`.
- **Demo de drift de HT-05** (Task 15 Step 5) se difirió durante la
  implementación de tipos porque `Proveedor` todavía no tenía
  consumidores reales en el frontend -- se completó al final de esta
  misma sesión, ver "Cómo se verificó".
- El patrón `ResponseEntity<?>` (47 métodos en todo el backend, no solo
  catálogo) que hace invisible un DTO para springdoc es deuda técnica ya
  identificada, fuera de alcance de este sprint.
- Sin capturas de "Recetas" -- ese módulo del sidebar existe como
  entrada de navegación (marcado pendiente) pero su implementación es de
  un sprint futuro, no de este.
- **Diferido tras la revisión final** (severidad baja, no bloquea nada
  real): `CategoriaController` no tiene endpoints `desactivar`/`activar`
  pese a que `ConfiguracionSeguridad` declara matchers para
  `/categorias/*/desactivar` y `/categorias/*/activar` -- no es un bug en
  vivo porque `PantallaCategorias.tsx` no tiene ninguna acción de
  desactivar/activar categorías; es configuración muerta. Si desactivar
  categorías entra en un sprint futuro, implementar entonces.
- **Diferido tras la revisión final**: `UnidadController` y
  `ProveedorController` no usan `@Transactional` a nivel de método, a
  diferencia de los otros 4 controladores del módulo (que sí lo
  necesitan por el bug de `LazyInitializationException` ya documentado
  arriba). Hoy es inofensivo -- `UnidadMedida` y `Proveedor` no tienen
  relaciones LAZY que leer al armar sus respuestas -- pero es una
  inconsistencia que podría romperse silenciosamente si cualquiera de
  esas dos entidades gana una relación LAZY más adelante.

## Cómo se verificó

- Backend: `mvn test` → **58/58 pruebas verdes** (23 previas de sprint 1
  + 31 nuevas del catálogo + 4 de la función "Activar" agregada durante
  la revisión final, ver bug #9), sin regresiones, confirmado como
  último checkpoint de toda la sesión.
- **Frontend -- hallazgo importante sobre el propio proceso de
  verificación**: durante todo el sprint se usó `npx tsc --noEmit` como
  checkpoint tras cada tarea, pero el `tsconfig.json` raíz del proyecto
  está en modo "solution" (`"files": []`, con `references` a
  `tsconfig.app.json`/`tsconfig.node.json`) -- ese comando no revisaba
  nada útil. El build real es `tsc -b` (`package.json`:
  `"build": "tsc -b && vite build"`). Al correr `npx tsc -b --force` por
  primera vez en todo el sprint (durante la demo de drift de abajo), sí
  detectó los errores esperados de la demo, y de paso 2 problemas reales
  que el checkpoint equivocado nunca había atrapado (ver "Bugs
  encontrados"). Verificación final correcta: `npx tsc -b --force` → 0
  errores, y `npm run build` completo (tsc -b + vite build) →
  **BUILD SUCCESS** con bundle real generado.
- **Demo de drift real de HT-05**: se renombró `razonSocial` →
  `nombreLegal` en `ProveedorController.ProveedorRespuesta` (backend),
  se recompiló y reinició el backend, se regeneraron los tipos del
  frontend. `npx tsc -b` reportó **4 errores reales de compilación**,
  apuntando exactamente a los 4 usos reales de `razonSocial` en
  `PantallaProveedores.tsx` (líneas 41, 85, 86) y
  `PantallaProveedorFormulario.tsx` (línea 40). Revertido el rename,
  tipos regenerados de vuelta, 0 errores relacionados confirmados.
- **Prueba end-to-end real en el navegador** (no solo tsc limpio):
  - Producto creado de punta a punta: `ARROZ-001` / "Arroz corriente" /
    categoría Abarrotes / insumo / costo inicial 3.85 -- guardado,
    aparece en la lista, y el costo se precarga correctamente al volver
    a editar (`productos.png`).
  - Categorías: árbol completo con 37 categorías reales, jerarquía
    padre/hijo visible (`categorias.png`).
  - Unidades y presentaciones: pestaña "Unidades" con las 5 unidades
    sembradas; pestaña "Presentaciones de compra" -- creada
    "Saco 50 kg" = 50 kg para "Arroz corriente", con el preview en vivo
    "1 Saco 50 kg = 50 kg" apareciendo mientras se escribe, antes de
    guardar (`presentaciones-factor.png`); pestaña "Conversiones" --
    creada 1 kg = 1000 g, mismo patrón de preview en vivo, confirmado en
    la lista tras guardar.
  - Proveedores: estado vacío real
    (`proveedores-vacio.png`) antes de crear ninguno. Creados 2
    proveedores reales: "Mercado de Abastos San Isidro" sin RUC (columna
    RUC muestra "— (informal)", `proveedores-sin-ruc.png`) y
    "Distribuidora Los Andes SAC" con RUC 20123456789. Editado este
    último (nombre comercial cambiado a "Los Andes Distribuidora"),
    confirmado el cambio reflejado en la lista
    (`proveedores-editado.png`).
- **Conteos reales en la base de datos** (consulta directa a Postgres,
  no estimados): `unidad_medida` 5, `categoria_producto` 37, `producto`
  1, `presentacion_compra` 1, `conversion_unidad` 1, `proveedor` 3 (2
  activos + 1 residuo de prueba inactivo, ver nota abajo).
- Navegación (9 bis): acordeón verificado con Catálogo/Usuarios
  auto-abiertos por ruta activa, "Compras y proveedores" con hijos
  mixtos (Compras pendiente, Proveedores activo) desplegado
  correctamente, "Caja y turnos" (todo pendiente) deshabilitado. Modo
  riel verificado tras corregir 2 bugs reales (ver abajo).

## Bugs encontrados en el camino

1. **BUG REAL -- ruta relativa en el catch-all del router** (preexistente,
   no de este sprint):
   `<Route path="*" element={<Navigate to="usuarios" replace />} />` en
   `apps/admin/index.tsx` usaba una ruta relativa, que en React Router
   dentro de rutas anidadas se resuelve contra la URL NO coincidida, no
   contra la base de las rutas -- causaba un bucle infinito de
   redirecciones que colgó el navegador de pruebas por completo.
   Corregido a `<Navigate to="/admin/usuarios" replace />` (ruta
   absoluta).
2. **BUG REAL -- `LazyInitializationException` disfrazada de 403**:
   `CategoriaController`/`ProductoController`/`PresentacionController`/
   `ConversionController` accedían a relaciones `@ManyToOne` LAZY
   después de que el caso de uso ya cerró su sesión de Hibernate
   (`open-in-view: false`). Spring traducía la excepción a 403, no 500
   -- parecía un problema de permisos. Confirmado con curl usando un
   token con el permiso correcto antes de diagnosticar la causa real.
   Corregido con `@Transactional`/`@Transactional(readOnly = true)` a
   nivel de método en los 4 controladores.
3. **BUG REAL, mío, en la semilla V15**: IDs escritos a mano con 25
   caracteres en vez de 26 (convención ULID de longitud fija del
   proyecto) -- Postgres los guardaba con relleno de espacio en el
   `CHAR(26)`, ensuciando `categoriaId`/`unidadBaseId` en las respuestas
   JSON. V15 ya estaba aplicada (Flyway es autoritativo, no se edita) --
   corregido con `V16__corregir_longitud_ids_semilla.sql`. Verificado:
   37 categorías y 5 unidades con exactamente 26 caracteres tras
   aplicar V16.
4. **BUG REAL -- modo riel del sidebar, 2 hallazgos**: (a) el click del
   botón-icono alternaba abrir/cerrar, pero el `mouseenter` al llegar
   con el mouse ya abría el flyout primero -- el click subsecuente lo
   cerraba en el mismo gesto, rompiendo el flyout con mouse real, no
   solo en pruebas automatizadas. Corregido: el click siempre abre,
   nunca alterna. (b) El flyout usaba `position: absolute` dentro de un
   contenedor con `overflow: hidden` (necesario para el SVG decorativo
   de fondo) -- se recortaba visualmente aunque existiera correctamente
   en el DOM (confirmado: el árbol de accesibilidad SÍ tenía los links,
   la captura de pantalla NO los mostraba). Corregido portando el
   flyout a `document.body` vía `createPortal`, mismo patrón que
   `Modal.tsx`. Lección aplicada: nunca confiar en un solo tipo de
   evidencia para UI nueva.
5. **BUG REAL -- costo inicial no se precargaba al editar**:
   `PantallaProductoFormulario.tsx` seteaba todos los campos al cargar
   un producto existente excepto `costoInicial`. Corregido leyendo
   `p.costo`. Efecto colateral real encontrado al corregir esto:
   `guardar()` reenviaba el costo en CADA guardado sin importar si el
   usuario lo tocó, lo que el backend rechazaría con
   `COSTO_YA_CONFIRMADO_POR_COMPRA` si el origen ya era compra. Corregido
   calculando `costoEditable` y solo reenviando si es editable.
6. **BUG REAL -- factor de presentación no se mostraba en la lista**:
   `PantallaUnidadesYPresentaciones.tsx` renderizaba
   `p.factorAUnidadBase`, un campo que no existe en el DTO real
   (`PresentacionRespuesta` expone el campo como `factor`, no el nombre
   del getter interno de la entidad JPA). El texto mostraba
   "Saco 50 kg — factor" sin el número, sin lanzar ningún error
   (`undefined` en JSX es silencioso). Encontrado creando una
   presentación real y comparando la captura de pantalla contra el
   texto esperado -- el preview en vivo SÍ funcionaba (usa el estado
   local, no el campo del DTO), el bug estaba solo en el render de la
   lista de guardados. Corregido a `p.factor`.
7. **BUG REAL, de este sprint -- import no usado**:
   `PantallaUsuarioFormulario.tsx` seguía importando `Checkbox` después
   de migrar el campo PIN a `EntradaNumerica`. Solo se detectó al correr
   el `tsc -b` real (ver el hallazgo de proceso arriba). Corregido
   quitando el import.
8. **Problema preexistente, no de este sprint -- `@types/node` faltante**:
   `tsconfig.node.json` requiere `"types": ["node"]` para tipar
   `vite.config.ts`, pero `@types/node` nunca estuvo instalado. Bloqueaba
   el build real desde antes de este sprint; nunca se había corrido
   `tsc -b` para descubrirlo. Corregido instalando la dependencia
   (`npm install --save-dev @types/node`).
9. **BUG REAL, CRÍTICO -- "Activar" (reactivar) no existía en el backend**:
   encontrado en la revisión final de todo el código del sprint (agente
   independiente, mandada por el proceso de cierre). El frontend tenía
   `activarProducto`/`activarProveedor` en `cliente.ts`, el botón
   "Activar" con modal de confirmación en `PantallaProductos.tsx`/
   `PantallaProveedores.tsx`, y hasta los matchers de seguridad en
   `ConfiguracionSeguridad` -- pero **ningún** endpoint `/activar` existía
   en `ProductoController`/`ProveedorController`, ni método `activar()`
   en las entidades, ni caso de uso. Cualquier admin que desactivara un
   producto o proveedor real y luego intentara reactivarlo desde la lista
   habría recibido un 404. Ni los 54 tests de backend ni la verificación
   en navegador de la Fase E lo habían atrapado -- ninguno ejercitaba el
   ciclo completo desactivar→reactivar. Corregido con el mismo patrón que
   `desactivar`: `activar()` en `Producto`/`Proveedor`, casos de uso
   `ActivarProducto`/`ActivarProveedor`, endpoints
   `POST /{id}/activar` en ambos controladores. Tests nuevos con TDD real
   (`ActivarProductoTest`, `ActivarProveedorTest`, 4 casos, suite completa
   58/58 tras el fix). Verificado end-to-end en el navegador real:
   desactivado y reactivado "Arroz corriente" y un proveedor de prueba,
   confirmando que el modal de confirmación ya existente ahora sí
   completa la operación.

## Nota operativa (sin relación con el código)

El entorno completo (Docker + backend + gateway + frontend) se cayó una
vez durante la sesión por un corte/reinicio ajeno al código -- se
relevantó todo sin pérdida de datos. `BarraLateral.tsx` fue modificado
externamente a esta sesión a mitad de camino (el harness lo marcó
explícitamente como cambio en disco); el nuevo árbol reestructura
"Usuarios y permisos" como sección propia con items planos y agrega un
telón para móvil -- no se revirtió, se verificó que compila limpio y que
las rutas de este sprint siguen intactas dentro de la nueva estructura.

Quedó una fila residual en la tabla de proveedores (RUC "— (informal)",
razón social vacía, estado "Inactivo") -- resto de una navegación
accidental de la herramienta de pruebas durante el debugging de la
pestaña Presentaciones, no reproducible desde el flujo real de la UI (el
formulario exige razón social). Se dejó tal cual, ya inactiva, en vez de
forzar un hard-delete que el sistema no soporta por convención.

## Ajuste de UX posterior al cierre (1): "Eliminar" en la lista, "Reactivar" en Editar

A pedido del usuario, tras el cierre inicial del sprint: en las listas
de Productos y Proveedores, el botón condicional Activar/Desactivar se
reemplazó por un único botón **"Eliminar"** (siempre visible,
independiente del estado). En esta primera vuelta seguía llamando al
mismo endpoint `desactivar` -- ver el ajuste (2) más abajo, donde a
pedido explícito del usuario pasó a ser un borrado real.

La reactivación se movió a la pantalla de **Editar**:
`PantallaProductoFormulario.tsx`/`PantallaProveedorFormulario.tsx`
muestran un aviso (componente `Aviso`, tono ámbar) justo debajo del
encabezado cuando el registro cargado está inactivo, con un botón
"Reactivar" que llama a `activarProducto`/`activarProveedor` y
actualiza el estado local sin recargar la página (el aviso desaparece
al instante tras reactivar con éxito). Esto no cambió con el ajuste (2).

## Ajuste de UX posterior al cierre (2): "Eliminar" pasa a ser un borrado real

El usuario probó "Eliminar" esperando que la fila desapareciera de la
lista por completo -- el comportamiento de (1) (desactivar, la fila
sigue visible como "Inactivo") no era lo que esperaba. Antes de
implementar un DELETE físico se advirtió explícitamente el riesgo real:
un producto/proveedor con datos relacionados (presentaciones de compra,
conversiones, futuras compras del sprint 9) no se puede borrar sin
romper la integridad referencial o perder historial real de negocio.
El usuario eligió, con ese riesgo explicado: DELETE real, que **bloquee
con un error claro** (no falle con un 500 ni borre en cascada) cuando el
registro ya tiene datos relacionados.

### Backend

Nuevo patrón, calcado de `EliminarLocal.java` (ya existente en
`identidad`, mismo problema: borrado real vs. `CambiarEstadoLocal`):
`EliminarProducto.java`/`EliminarProveedor.java` (casos de uso nuevos)
verifican **antes** de borrar si existe algo que referencie al
registro -- no capturan la excepción SQL de la FK, hacen una consulta
explícita primero, para devolver un error de negocio claro en vez de
una excepción cruda. `ErrorCatalogo.TIENE_HISTORIAL` (el enum ya lo
tenía, sin usar en `catalogo` hasta ahora) se mapea a 409 Conflict.

- **Producto**: `presentacion_compra` referencia `producto_id` con
  `ON DELETE RESTRICT` (V14). Se agregó
  `PresentacionCompraRepositorio.existsByProductoId(id)` y se chequea
  antes del `deleteById`. `producto_servicio` y `conversion_unidad` son
  `ON DELETE CASCADE` -- no bloquean ni hace falta chequearlos, no son
  "historial" real de negocio (asignación de canales de venta y
  conversiones de unidad, no compras).
- **Proveedor**: ninguna tabla lo referencia todavía (las compras reales
  son del sprint 9) -- el chequeo queda documentado en el código como
  el lugar exacto donde agregar la validación cuando esa tabla exista.

Nuevos endpoints `DELETE /api/productos/{id}` y
`DELETE /api/proveedores/{id}`, protegidos con el mismo permiso que
`desactivar`/`activar` (`PERM_catalogo.desactivar`) -- se agregó el
matcher de método DELETE en `ConfiguracionSeguridad` (no existía
ninguno, un DELETE sin matcher explícito caía en
`anyRequest().authenticated()`, que solo exige estar logueado sin
importar el permiso).

Tests nuevos con TDD real: `EliminarProductoTest` (3 casos: borra uno
sin uso, rechaza con `TIENE_HISTORIAL` uno con una presentación de
compra real creada primero, falla `NO_ENCONTRADO`) y
`EliminarProveedorTest` (2 casos). Suite completa: 63/63 verde.

### Frontend

`eliminarProducto`/`eliminarProveedor` en `cliente.ts` (DELETE, en vez
de las funciones `desactivarProducto`/`desactivarProveedor` que
`PantallaProductos.tsx`/`PantallaProveedores.tsx` usaban antes).
`ModalDeConfirmacion` ya tenía una prop `error` pensada exactamente para
este caso ("si la confirmación falló, se muestra acá sin cerrar el
modal") -- se usó tal cual, sin modificar el componente. Si el DELETE
falla con `TIENE_HISTORIAL`, el modal se queda abierto mostrando el
motivo; en cualquier otro fallo, un mensaje genérico. A pedido explícito
del usuario, si falla no se hace nada más automáticamente (no cae a
desactivar como respaldo) -- el usuario ve el error y decide qué hacer.

Verificado end-to-end en el navegador real, ambos casos: (a) bloqueo
real -- "Arroz corriente" (que ya tenía la presentación "Saco 50 kg" de
antes en esta sesión) rechazado con el mensaje "No se puede eliminar:
este producto ya tiene presentaciones de compra registradas...", la
fila se mantuvo intacta; (b) éxito real -- creado un producto de prueba
sin ninguna relación, eliminado, confirmado que desaparece incluso con
el filtro Estado="Todos" (no queda como "Inactivo", ya no existe en la
base de datos) -- mismo patrón repetido y confirmado para un proveedor
de prueba.

### Incidente operativo durante la verificación (sin relación con el código)

Docker Desktop volvió a corromperse (mismo tipo de I/O error de WSL2 ya
documentado antes en esta sesión), esta vez de forma más severa: el
motor Linux nunca terminaba de inicializar tras un reinicio simple del
proceso, ni tras terminar solo la distro `docker-desktop`. Se resolvió
con `wsl --shutdown` (apagado completo de WSL2, confirmado con el
usuario antes de ejecutarlo por ser más disruptivo) seguido de un
reinicio limpio de Docker Desktop, que reconstruyó la VM desde cero.
Los contenedores `rikos-postgres`/`rikos-rabbitmq`/`rikos-mailpit`
sobrevivieron (quedaron detenidos, no se perdieron) y se reactivaron con
`docker start`. El frontend (Vite) también se había caído junto con
Docker y se relanzó aparte. Sin pérdida de datos.

## Ajuste de UX posterior al cierre (2 bis): mismo borrado real para Rol y Usuario

A pedido explícito del usuario ("ACA TAMBIEN AÑÑADE PARA BORRAR EL ROL
PERO TMBN QUE NO TENGA USUARIOS"), se extendió el mismo patrón de
borrado real con bloqueo a **Rol** y, después, a **Usuario** (mismo
diseño conversado y aprobado, dado el radio mucho más grande de FKs que
apuntan a `usuario`).

### Backend -- Rol

`EliminarRol.java`: borrado real, bloqueado en dos casos --
`ES_SISTEMA` (los 6 roles semilla, ya bloqueados para edición, ahora
también para borrado) y `TIENE_HISTORIAL` si `usuario_rol` tiene alguna
fila con ese `rol_id` (`usuario_rol.rol_id` es `ON DELETE RESTRICT`,
`UsuarioRolRepositorio.existsByRolId`). `ErrorRol` ganó ambos valores.
`DELETE /roles/{id}` en `RolController`, mapeado a 400/409. No hizo
falta agregar matcher de seguridad: `/roles/**` ya estaba cubierto por
el matcher genérico `hasAnyRole(...)` para todos los métodos.

### Backend -- Usuario (el caso más difícil de esta sesión)

`usuario` tiene FKs entrantes desde casi cualquier tabla del sistema
(`creado_por`/`actualizado_por`/`asignado_por` en `local`,
`rol_permiso`, `usuario_local`, `usuario_permiso_excepcion`,
`auditoria`, todo `catalogo`, etc.), todas `ON DELETE RESTRICT`. En vez
de un chequeo explícito por cada tabla (que habría que recordar
actualizar cada vez que se agregue una tabla nueva con esas columnas),
se aprobó con el usuario un enfoque de "intentar borrar y capturar la
violación de FK" -- se generaliza solo, sin mantenimiento.

Esto expuso un bug real de manejo de transacciones en Postgres, no
obvio: una vez que un `DataIntegrityViolationException` aborta la
transacción física, **toda la transacción queda en estado
abort-only** -- cualquier sentencia posterior en la misma conexión
falla, incluidas lecturas, aunque la excepción ya se haya capturado en
Java. Con `@Transactional` normal (compartiendo la transacción del
llamador, p. ej. la del test), un `findById` inmediatamente después del
catch devolvía vacío en vez del usuario todavía presente. Probado y
descartado: `entityManager.clear()` (limpia la caché de Hibernate, no
el estado abortado de Postgres) y delegar a un método interno
`@Transactional` separado (el proxy de Spring AOP no intercepta
`this.metodo()`, la anotación no tiene efecto en llamadas internas).

Solución final en `EliminarUsuario.java`: inyectar
`PlatformTransactionManager`, armar un `TransactionTemplate` con
`Propagation.REQUIRES_NEW`, y envolver el `deleteById` + `flush()` en
`nuevaTransaccion.executeWithoutResult(...)` -- abre una transacción y
conexión genuinamente separadas, así un abort ahí no envenena la del
que llama. Patrón nuevo en el código, no usado en ningún otro lado;
documentado en el propio doc comment de la clase para quien lo
mantenga después. `EliminarUsuarioTest` no puede llevar
`@Transactional` a nivel de clase (rompería el aislamiento que el
patrón necesita) -- usa `@AfterEach` con borrado manual en vez de
rollback automático.

`ErrorUsuario` ganó `TIENE_HISTORIAL`. `DELETE /usuarios/{id}` en
`UsuarioController`, 409 en ese caso. Sin cambios en
`ConfiguracionSeguridad` (mismo motivo que Rol).

### Frontend

`eliminarRol`/`eliminarUsuario` en `cliente.ts`. `PantallaRoles.tsx`:
botón "Eliminar" (ícono prohibido) en el pie de cada tarjeta, oculto si
`rol.esSistema`; mensaje de bloqueo "hay usuarios con este rol
asignado. Quítaselo a todos primero." `PantallaUsuarios.tsx`: se quitó
`cambiarEstadoUsuario` por completo, el botón de la columna Acciones
pasa a decir siempre "Eliminar"; mensaje de bloqueo explica que hay
actividad registrada (sesiones, cambios hechos) y sugiere desactivar
desde Editar en su lugar. Mismo componente `ModalDeConfirmacion`
reutilizado sin cambios.

### Cómo se verificó

Tests nuevos con TDD real: `EliminarRolTest` (4 casos: borra rol
personalizado sin uso, rechaza rol de sistema, rechaza rol con
`UsuarioRol` real asignado usando el admin sembrado como
`asignado_por`, no-encontrado) y `EliminarUsuarioTest` (3 casos: borra
usuario sin uso, rechaza usuario con sesiones registradas -- encontró de
paso un bug de datos de prueba, `Sesion.resultado = "exito"` no pasa el
check constraint real de la tabla, que solo acepta `exitoso`/`fallido`/
`bloqueado`, corregido a `"exitoso"` --, no-encontrado). Suite completa
tras esto: 70/70 verde.

### Bugs encontrados en el camino

- **Matcher de seguridad faltante para DELETE de Producto/Proveedor**:
  `/productos/**` y `/proveedores/**` no tenían matcher de método
  DELETE en `ConfiguracionSeguridad` -- una petición DELETE habría caído
  en el `anyRequest().authenticated()` genérico, es decir cualquier
  usuario logueado sin importar su permiso habría podido borrar. Se
  encontró y corrigió de forma proactiva al implementar los endpoints,
  antes de que llegara a ser una vulnerabilidad real en producción.
- Ver arriba: el bug de transacción abortada de Postgres para
  `EliminarUsuario`, el más laborioso de depurar de esta sesión.

## Rediseño posterior al cierre (3): Categorías, Productos y sidebar (2026-09-24/25)

### Qué se pidió

Rediseñar el sidebar del panel ("que no parezca hecho por IA"), y después
reestructurar las pantallas de Categorías y Productos para que reflejen el
modelo de datos real en vez de tablas planas. Cada vista se diseñó primero
en un lienzo de diseño (artifact "Sidebar Riko's") y se aprobó antes de
pasarla a código. Pedidos puntuales en el camino: poder eliminar categorías
y subcategorías, usar el ícono de tacho de Lucide en **todos** los botones de
eliminar, quitar las pastillas de parte-del-menú de la lista de categorías,
y que "Nuevo producto" y "Editar producto" sean el mismo formulario.

### Qué se construyó

**Sidebar** (`BarraLateral.tsx`, bloque `.panel-lateral` de `global.css`):
fondo café plano, títulos de sección en texto normal, botón de contraer en
la cabecera, usuario + cerrar sesión en el pie. Colores `--color-lateral-*`
redefinidos en `tokens.css`.

**Categorías** (`PantallaCategorias.tsx`): lista a la izquierda agrupada por
naturaleza (las 5 naturalezas son fijas del sistema) y panel de detalle a la
derecha con las subcategorías: renombrar en línea, agregar con Enter,
arrastrar para reordenar, eliminar con confirmación. "Nueva categoría" es un
flujo de 2 pasos en el mismo panel (categoría → subcategorías) con una sola
pregunta "¿Qué va a agrupar?" que el frontend traduce a
`naturaleza` + `componente_menu`.

Backend de categorías:
- `ReordenarCategorias` + `PUT /categorias/orden` (guarda el orden de un grupo
  de hermanas de una vez; rechaza listas que no coinciden con las hermanas
  reales).
- `EliminarCategoria` + `DELETE /categorias/{id}`: borrado real solo si está
  vacía (`CATEGORIA_TIENE_SUBCATEGORIAS`, `CATEGORIA_TIENE_PRODUCTOS`), antes
  de chocar con los `ON DELETE RESTRICT`. Permiso `catalogo.desactivar`,
  igual que productos/proveedores.
- `CrearCategoria`: nuevas validaciones `PADRE_NO_ES_RAIZ` (el árbol es de
  2 niveles) y `PADRE_TIENE_PRODUCTOS` (una hoja con productos no puede
  pasar a tener hijas).
- `listar()` devuelve cada raíz seguida de sus hijas ordenadas por `orden`
  (antes `findAll()` sin orden) y agrega `totalProductos`.

**Productos** (`PantallaProductos.tsx`): resumen con barra de proporción por
uso (vendible/insumo/subreceta, que no son excluyentes), filtros (buscar,
categoría hoja, servicio, estado), tabla con mosaico de iniciales, ruta de
categoría, servicios D·A·C (solo categorías `producto_fijo`), precio, costo
(con "estimado" si `origen_costo = manual`), margen con color, e interruptor
para activar/desactivar. Clic en una fila abre una ficha lateral con cifras,
presentaciones de compra e historial. "Exportar" baja un CSV de lo filtrado.

**Formulario de producto** (`PantallaProductoFormulario.tsx`, un solo
componente para crear y editar): mismas secciones en ambos modos; Venta solo
si es vendible, Compra y costo solo si es insumo, servicios solo si la
categoría es `producto_fijo`. Al crear sugiere código (prefijo de la
categoría + siguiente número libre), usos habituales, unidad y "controla
stock" sin pisar lo que el usuario ya tocó; muestra "Para crearlo falta" y
tiene "Crear y agregar otro". Al editar: historial, aviso de cambios sin
guardar, código bloqueado (el backend no lo actualiza), costo bloqueado si
viene de compra, presentaciones de compra con alta en línea, zona de
eliminar e interruptor de activo.

Backend de productos: `ProductoRespuesta` agrega `creadoEn`,
`actualizadoEn` y `fechaUltimaCompra` (ya estaban en la tabla).

Compartido: `productoComun.tsx` (usos, margen, formato, pastillas,
interruptor); colores `--color-uso-*` en `tokens.css`; dependencia nueva
`lucide-react` (`IconoBorrar` = Trash2, `IconoCerrar`, `IconoCheck`).
Ícono de tacho aplicado en Productos, Proveedores, Usuarios, Roles, Locales
y días especiales (antes mezclaban `IconoProhibido` y botones sin ícono).

### Qué se dejó fuera y por qué

- Reordenar las **raíces** arrastrando: el endpoint lo soporta, pero el
  diseño solo lo pidió para subcategorías.
- Editar o borrar **presentaciones de compra**: solo se agregan (no hay
  endpoint de edición/borrado todavía).
- Conversiones de unidad siguen solo en "Unidades y presentaciones".
- "Creado por" / "Editado por" en el historial: la tabla guarda el id del
  usuario pero la respuesta no trae el nombre.
- Los umbrales de margen (≥55 % verde, <35 % rojo) son una decisión de
  diseño, no una regla del negocio: ajustar si el restaurante usa otros.
- `src/api/generado/esquema.d.ts` está en `.gitignore`; se le agregaron a
  mano `totalProductos` y los 3 campos de historial. Correr
  `npm run generar-tipos` con el backend levantado para regenerarlo.

### Cómo se verificó

- Pruebas automatizadas: 38/38 del módulo catálogo pasan
  (`CrearCategoriaTest` 8, `EliminarCategoriaTest` 5 nuevas,
  `ReordenarCategoriasTest` 3 nuevas, más las existentes de productos y el
  `CatalogoAccesoPorRolTest`).
- En vivo, logueado como admin, con datos reales:
  - Categorías: crear categoría + subcategorías, reordenar arrastrando
    (sobrevive a recargar), eliminar vacía, eliminar bloqueada con
    explicación cuando tiene subcategorías.
  - Productos: crear un plato de carta (código `SAN-001` sugerido, servicios
    Desayuno+Cena guardados en `producto_servicio`), "Crear y agregar otro"
    (el código avanzó a `SAN-002`, conservó la categoría), crear uno
    vendible+insumo (margen 58 % en vivo, costo "estimado"), editar precio
    (margen 64 %), agregar presentación (queda predeterminada al ser la
    primera), interruptor de activo (persiste en el servidor, no abre la
    ficha), filtros y estado vacío, eliminar sin historial (ok) y con
    presentación (`TIENE_HISTORIAL`, mensaje de desactivar en su lugar).
  - Los productos de prueba (`Zz …`) se borraron al terminar; el de la
    presentación, por SQL porque la regla de negocio no deja borrarlo.

### Bugs encontrados en el camino

- **`CrearCategoria` reventaba al agregar cualquier subcategoría** (NPE:
  `Set.of(...).contains(null)` lanza en vez de devolver false). El frontend
  nunca manda `naturaleza` para una hija (la hereda), y la validación de
  naturaleza corría antes de mirar si era hija. Spring lo devolvía como
  **403**, lo que parecía un problema de permisos. Arreglado validando la
  naturaleza solo en la rama de raíz; prueba de regresión
  `crea_una_subcategoria_sin_mandar_naturaleza_como_hace_el_frontend_real`.
- **`listar()` de categorías no ordenaba** (`findAll()`): las categorías
  nuevas aparecían antes que las de la semilla.
- **El formulario de producto nunca exponía los servicios**
  (desayuno/almuerzo/cena), aunque el backend (`producto_servicio`,
  `AsignarServiciosProducto`) ya existía. Además, la regla depende de la
  naturaleza de la **categoría** (`producto_fijo`), no de "es vendible".
- Varias veces el servidor en ejecución era una compilación vieja (sin
  devtools): hay que reiniciar `servicio-operacion` para ver cambios de
  backend.

## Ajuste visual posterior al cierre (4): rediseño final del sidebar, "que no parezca hecho por IA" (2026-09-24/25)

### Qué se pidió

En varias vueltas sucesivas, todas sobre el mismo problema: el usuario
fue mandando capturas puntuales (primero del sidebar completo en móvil,
después del módulo activo/colores, después del logo y la tarjeta de
usuario) con el mismo mensaje de fondo -- "se ve muy IA" / "parece
hecho por IA" -- señalando gradientes diagonales, sombras apiladas y
blur que le daban aspecto de plantilla SaaS genérica en vez de sentirse
parte de la marca RIKO'S. El proceso fue iterativo: cada corrección se
verificaba con captura antes de seguir con la siguiente parte del
sidebar, y al menos un intento propio salió mal y tuvo que revertirse en
el momento (ver "bugs" más abajo).

### Qué se construyó

`BarraLateral.tsx` y el bloque `.panel-lateral*` de `global.css` se
reescribieron por completo respecto del primer rediseño (3). Estado
final, verificado en vivo:

**Cabecera** (`panel-lateral-cabecera`): logo (gorro de chef) + nombre
"Riko's Restaurante" a la izquierda, botón de contraer/expandir
(ícono de flecha, sin texto) a la derecha, en la misma fila -- ya no es
un botón de texto separado al pie del sidebar. El logo
(`panel-lateral-marca-logo`) es un cuadrado de 34px con relleno sólido
(token `--color-lateral-logo: #d9622b`), sin gradiente ni sombra.

**Lista de navegación**: fondo café oscuro plano (`--color-lateral-fondo:
#1a100b`) en todo el panel, ítems con `border-radius: 8px` uniforme,
hover con un fondo translúcido único (`--color-lateral-item-hover`,
sin cambiar de tono entre elementos), ítem/grupo activo con fondo sólido
`--color-lateral-activo-bg` + texto en el color de acento
`--color-lateral-activo-acento` (`#f59e4b`) -- un solo color de acento
para todo el sidebar, no uno distinto por módulo. Los subítems activos
usan `--color-lateral-subactivo-bg`, un tono aparte pero sin bordes ni
márgenes negativos.

**Pie** (`panel-lateral-pie`): avatar circular (iniciales, mismo relleno
sólido que el logo) + nombre + rol, y el botón de cerrar sesión como
ícono aparte a la derecha -- ya no hay una "tarjeta" con fondo/borde
propio alrededor del usuario, ni barra de acento lateral, ni punto verde
de "en línea" (ambos se quitaron por completo, no solo su glow: no
aportaban información real y sumaban ruido visual).

**Modo colapsado** (`.panel-lateral.colapsada`, 72px de ancho): la
cabecera pasa a columna (logo arriba, flecha debajo), los textos se
ocultan con `display: none`, los ítems con submenú muestran un flyout al
pasar el cursor (`ItemConFlyout`, ya existente, sin cambios de
comportamiento) y el pie también pasa a columna (avatar arriba, salir
debajo).

Todo el color del sidebar sale de los tokens `--color-lateral-*` en
`tokens.css` (9 variables: fondo, texto, texto-tenue, chevron,
item-hover, borde-tenue, activo-bg, subactivo-bg, activo-acento, logo)
-- ningún color queda hardcodeado suelto en `global.css` para este
bloque.

### Qué se dejó fuera y por qué

- No hay ningún indicador de "usuario en línea" -- se quitó a propósito
  (ver arriba), no es una omisión.
- El flyout del modo riel (`panel-lateral-flyout`) conserva su propio
  fondo oscuro y sombra de elevación (`box-shadow` para separarlo
  visualmente del contenido detrás, ya que flota sobre la página vía
  `createPortal`) -- es la única sombra que queda en todo el componente,
  y es funcional (indica que es una capa flotante), no decorativa.

### Cómo se verificó

Verificado en el navegador (Playwright, logueado como admin) en ambos
modos: expandido y colapsado, en la pantalla de Productos. Capturas
reales guardadas en `capturas-catalogo/sidebar-expandido.png` y
`capturas-catalogo/sidebar-colapsado.png` -- logo y avatar en relleno
sólido sin gradiente, tarjeta de usuario sin blur, módulo activo
("Catálogo y recetas") con un solo color de acento consistente en
ambos modos. `npx tsc -b --force` limpio.

### Bugs encontrados en el camino

- Un primer intento de simplificar el ítem activo de un subítem del
  árbol (`.panel-lateral-subitem.activo`) con `margin-left` negativo +
  `border-radius` asimétrico produjo un rectángulo de bordes duros que
  se veía peor que el original ("se ve todo muy IA, arreglalo eso mira
  fijat") -- se revirtió de inmediato a un fondo de radio uniforme sin
  trucos de margen, confirmado visualmente antes de seguir.
- Una vuelta intermedia (no descrita en detalle acá porque quedó
  superada por la reescritura final) simplificó el logo/avatar/tarjeta
  de usuario manteniendo la estructura de "tarjeta con borde" y un punto
  verde de "en línea" con glow reducido -- ese enfoque quedó obsoleto:
  la reescritura final de esta sección eliminó la tarjeta y el punto por
  completo en vez de solo atenuarlos.

## Cambio de marca a verde y sidebar acordeón (2026-09-28)

**Qué se pidió:** rehacer el sidebar y los colores copiando una captura
de referencia (sidebar verde oscuro, acento verde), en **toda** la app;
que al abrir un módulo se cierre el otro; que el sidebar pueda hacerse
delgado. Primero se diseñó en el lienzo (artboard "Inicio (nuevo tema
verde)"); el panel "Resumen" de la captura es solo ejemplo y **no** se
construyó.

**Qué se construyó:**
- `tokens.css`: `--color-marca` pasa de terracota `#9a4315` a verde
  `#2b7a68` (oscura `#1f5e50`, clara `#e3f1ec`); neutros con tinte verde
  (`--color-fondo-pagina`, `--color-borde`, textos); sidebar
  `--color-lateral-*` verde oscuro `#0f2724` con acento `#3fa58c`. Tokens
  nuevos: `--color-lateral-subtexto`, `--color-lateral-flyout`,
  `--color-fondo-hover`, `--color-borde-fuerte`.
- Colores cálidos fijos en `global.css`, `PantallaLogin.tsx` y
  `PantallaPin.tsx` reemplazados por equivalentes verdes; color por
  defecto de los roles sin color, también.
- `BarraLateral.tsx`: marca en texto (sin cuadro de logo; monograma "R"
  solo con la barra delgada), sección "Operación" separada de "Inicio",
  títulos de sección en mayúsculas con línea divisoria, ítem activo con
  barra verde a la izquierda, **acordeón** (un módulo abierto a la vez,
  guardado en `localStorage` como `rikos-panel-lateral-abierto`; al entrar
  a una ruta se abre el módulo que la contiene). La barra delgada (76 px,
  solo íconos, submenú flotante) ya existía y se re-estilizó.
- Los colores de "uso" de productos (vendible naranja, insumo azul,
  subreceta violeta) se mantienen: son categorías de dato, no marca.

**Cómo se verificó:** `npx tsc -b --force` sin errores; en vivo (servidor
propio en :5180 porque otra sesión usaba :5173): login en verde, Productos
con el sidebar nuevo, acordeón (abrir Caja cierra Catálogo, volver a tocar
lo cierra, Catálogo sigue marcado como activo), barra delgada con
monograma y módulo activo resaltado.

**Quedó fuera:** el dashboard "Resumen" (ventas, pedidos, canales) — el
backend aún no tiene esos datos (sprints posteriores). Los artboards viejos
del lienzo (Sidebar, Contraído, Categorías…) siguen en terracota.

## Rutas para volver a mirar en vivo (logueado como admin)

- `/admin/catalogo/productos` -- lista + `ARROZ-001` de prueba
- `/admin/catalogo/productos/nuevo` -- alta con costo inicial
- `/admin/catalogo/categorias` -- árbol de 37 categorías
- `/admin/catalogo/unidades` -- 3 pestañas (unidades/presentaciones/conversiones)
- `/admin/compras/proveedores` -- lista con RUC opcional
- `/admin/compras/proveedores/nuevo` -- alta de proveedor
