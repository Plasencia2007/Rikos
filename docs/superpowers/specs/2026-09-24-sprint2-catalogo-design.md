# Sprint 2 — Catálogo central, unidades y proveedores

**Fecha:** 2026-09-24
**Estado:** aprobado para plan de implementación

## Contexto

Primer módulo de negocio real del sistema (hasta ahora solo existían identidad,
locales y permisos — infraestructura administrativa). Cubre US-008 (productos),
US-009 (unidades/presentaciones/conversiones), US-011 (proveedores), más tres
piezas de infraestructura que el módulo necesita para no acumular deuda:
HT-05 (tipos generados desde OpenAPI), HT-07 (teclado numérico compartido) y
la generalización del patrón de submenú plegable de la barra lateral a toda
la navegación, incluido un modo de escritorio colapsado a solo-iconos con
panel flotante.

Fuente: `rikos-backend/doc/RIKOS-modelo-de-datos.md` secciones 2.6, 4.1–4.6,
22.3, 23.2. Pedido verbatim del usuario, dos mensajes (sprint 2 completo +
ampliación 9bis de navegación), 2026-09-24.

## Alcance y exclusiones

**Dentro:** las 7 tablas del bloque catálogo, semilla de unidades y árbol de
categorías, alta/edición/desactivación de productos/unidades/presentaciones/
conversiones/proveedores, `producto_servicio` para productos fijos, costo
inicial manual, generación de tipos OpenAPI, teclado numérico compartido,
navegación con submenús plegables + modo riel, 4 pantallas nuevas, pruebas
de acceso por rol.

**Fuera (explícito):** menú semanal, recetas, ventas, caja, pensionistas
(sprints posteriores — no se tocan). Órdenes de compra y cuentas por pagar.
Importación masiva desde Excel. Zonas de delivery, reservas, integraciones
(ya excluidas en sprints anteriores, se mantienen).

## Decisiones de diseño (mías, no pedidas literalmente — quedan registradas para que se puedan revisar)

1. **Costo inicial**: `producto.costo_ultima_compra` (ya existe en el modelo)
   más una columna nueva `producto.origen_costo` `enum('manual','compra')`.
   Un costo cargado a mano queda `origen_costo='manual'`, editable, mostrado
   como "costo estimado, se reemplaza con la primera compra". Cuando el
   sprint 9 (compras) confirme una compra, el caso de uso que la registre
   debe poner `origen_costo='compra'` y desde ahí el campo deja de ser
   editable a mano — esa regla se implementa ahora en el dominio de
   `producto`, aunque el flujo de compra todavía no exista.
2. **HT-05, herramienta**: `openapi-typescript` (solo tipos, cero runtime)
   en vez de un generador de cliente completo. `cliente.ts` conserva sus
   funciones `fetch` escritas a mano (manejan `ErrorApi`, token, etc.); lo
   que cambia es que sus interfaces (`Local`, `Rol`, `Usuario`, `Permiso`...)
   pasan a ser alias de los tipos generados en vez de redefiniciones a mano.
3. **HT-07, forma del componente**: dos patrones distintos bajo un mismo
   componente `EntradaNumerica` (PIN fijo-oculto-autoconfirma vs cantidad
   libre-visible-confirma con acción explícita), con `TecladoNumerico`
   (ya existe) como la pieza de solo-teclas que usa por dentro.
4. **Sidebar, modo riel**: confirmado con el usuario que se construye en
   este sprint (no existía ningún modo colapsado a iconos antes de este
   sprint — ver sección Navegación).
5. **`producto_servicio` con ID plano**: el patrón DT-041/2.8 bis aplica al
   lado `producto_id` (FK real); `servicio` es un enum-texto en la propia
   clave, no un ULID, así que no necesita la asociación de solo-lectura que
   sí necesita `producto_id`.

## Fase A — Backend: módulo `catalogo`

### Migración `V14__catalogo.sql`

Siete tablas según 4.1–4.6 del modelo:

- `categoria_producto`: autorreferencia (`categoria_padre_id` → sí mismo,
  RESTRICT), `naturaleza` enum-check, `componente_menu` enum-check nulo,
  `nivel` entero, `orden`, `activo`. Único `(categoria_padre_id, nombre)`
  para que el nombre sea único dentro de su padre (incluye raíces con
  `categoria_padre_id IS NULL`, tratado con índice único parcial por si
  Postgres no las considera iguales por defecto — sí las trata como
  distintas, así que un único parcial `WHERE categoria_padre_id IS NULL`
  sobre `nombre` cubre las raíces).
- `unidad_medida`: `codigo` único, `tipo` enum-check.
- `producto`: `codigo` único, `categoria_id` (RESTRICT), `unidad_base_id`
  (RESTRICT), tres banderas booleanas, `precio_venta` nulo (dinero,
  céntimos), `costo_ultima_compra` nulo (costo, 6 decimales),
  `origen_costo` enum-check nulo, `fecha_ultima_compra` nula,
  `controla_stock`, `activo`, `version`, columnas de traza completas.
- `producto_servicio`: PK compuesta `(producto_id, servicio)`,
  `producto_id` CASCADE, `servicio` enum-check, `activo`.
- `presentacion_compra`: `producto_id` RESTRICT, `unidad_id` RESTRICT,
  `factor_a_unidad_base` numeric, `es_predeterminada`, `activo`. Único
  parcial `(producto_id) WHERE es_predeterminada AND activo` — una sola
  predeterminada activa por producto.
- `conversion_unidad`: `producto_id` CASCADE, `unidad_origen_id`/
  `unidad_destino_id` RESTRICT, `factor` numeric. Único
  `(producto_id, unidad_origen_id, unidad_destino_id)`.
- `proveedor`: `ruc` único parcial `WHERE ruc IS NOT NULL`, resto de
  columnas de 4.6, `activo`.

Índices de 23.2: `producto_servicio (servicio, producto_id)`. Más los
obvios para FKs frecuentes (`producto.categoria_id`, `presentacion_compra.
producto_id`, `conversion_unidad.producto_id`) siguiendo el patrón de
migraciones anteriores (`ix_local_empresa_id`, etc.).

### Entidades JPA (paquete `catalogo/infraestructura`)

`CategoriaProducto` (autorreferencia `@ManyToOne` a sí misma), `UnidadMedida`,
`Producto`, `ProductoServicio` + `ProductoServicioId` (ID plano en el campo
`productoId`, asociación de solo lectura `producto` con
`insertable=false, updatable=false` — el campo `servicio` no necesita
tratamiento especial, es un `String`/enum simple dentro del `@IdClass`),
`PresentacionCompra`, `ConversionUnidad`, `Proveedor`, y sus repositorios.

### Casos de uso (paquete `catalogo/aplicacion`)

`CrearProducto` / `ActualizarProducto` / `DesactivarProducto`: validan
categoría-hoja (`categoria_id` no puede tener hijas activas — se resuelve
con una consulta `existsByCategoriaPadreId` antes de guardar, no hay
constraint SQL para esto), `precio_venta` obligatorio si `es_vendible`,
unidad base obligatoria. `CrearCategoria` / `ActualizarCategoria`: valida
que `naturaleza`/`componente_menu` solo se fijen en la raíz y que una
subcategoría herede los de su padre sin contradecirlos. `AsignarServicios`
(para `producto_servicio`, solo si la categoría del producto tiene
`naturaleza = producto_fijo`). `CrearPresentacion` /
`CrearConversion`: validan la unicidad de predeterminada y de par de
unidades respectivamente. `CrearProveedor` / `ActualizarProveedor` /
`DesactivarProveedor`.

Patrón `Resultado<T,E>` para todos, como el resto del código. Reutilizar el
fix de flush conocido (Hibernate reordena DELETE después de INSERT) si
algún caso de uso reemplaza un conjunto completo (no se anticipa
necesario aquí, ningún caso de uso hace delete-all-then-reinsert).

### Controladores (paquete `catalogo/api`)

`CategoriaController`, `UnidadController`, `ProductoController`,
`PresentacionController`, `ConversionController`, `ProveedorController`.
Protegidos con `PERM_catalogo.ver` / `.crear` / `.modificar` /
`.desactivar` vía `@PreAuthorize` o el filtro de rutas existente en
`ConfiguracionSeguridad` (seguir el patrón ya usado para `/roles/**` /
`/usuarios/**`, extendido a `/catalogo/**` o rutas específicas por
recurso — a decidir en el plan de implementación según cómo estén
organizadas las rutas).

### Semilla (`V15__semilla_catalogo.sql`, separada de la de esquema por
convención del proyecto — comparar con V4+V9 para roles/usuario)

5 unidades (kg, g, l, ml, und), árbol completo de 4.1.1: 9 raíces con sus
subcategorías tal como están escritas en el modelo, sin inventar nombres.
Nada de productos ni proveedores — el catálogo arranca vacío.

## Fase B — HT-05

- `pom.xml` de `servicio-operacion`: `springdoc-openapi-starter-webmvc-ui`.
  Expone `/api/v3/api-docs` (JSON) y opcionalmente `/api/swagger-ui.html`
  para inspección manual.
- `rikos-web`: dependencia `openapi-typescript` (dev), script
  `"generar-tipos": "openapi-typescript http://localhost:8081/api/v3/api-docs -o src/api/generado/esquema.d.ts"`
  en `package.json`. Requiere el backend corriendo — documentarlo en el
  propio script o en un comentario.
- `src/api/generado/` en `.gitignore` (es código generado, no se versiona
  ni se edita a mano — un comentario de cabecera lo deja explícito además).
- Migrar `cliente.ts`: las interfaces hoy escritas a mano (`Local`, `Rol`,
  `Usuario`, `UsuarioDetalle`, `Permiso`, `PermisoUsuario`, etc.) pasan a
  `type Local = components['schemas']['LocalRespuesta']` (o el nombre real
  que springdoc genere para cada DTO), usando el tipo `components` del
  esquema generado. Las funciones `fetch` no cambian de forma, solo de
  dónde toman sus tipos.
- Demostración para el checklist: cambiar un campo de un DTO en el backend,
  correr `npm run generar-tipos`, mostrar el diff del `.d.ts` generado, y
  señalar (sin arreglarlo a propósito, o arreglándolo para dejar la
  demostración completa — a decidir en el momento) un uso en el frontend
  que `tsc --noEmit` marca roto por el cambio.

## Fase C — HT-07

**`comun/componentes/EntradaNumerica.tsx`** (nuevo): envuelve
`TecladoNumerico` (existente, sin cambios en su API) y agrega el estado del
valor tecleado, con props:

```ts
interface Props {
  valor: string
  onCambiar: (valor: string) => void
  largoFijo?: number       // si se da: auto-completa y llama alCompletar al llegar al largo
  oculto?: boolean         // true: muestra puntos (PIN). false/ausente: muestra los dígitos
  alCompletar?: (valor: string) => void
  deshabilitado?: boolean
}
```

- Modo PIN (`largoFijo=4, oculto=true`): reemplaza la lógica manual de
  `PantallaPin.tsx` (hoy tiene su propio `pin`/`agregar`/puntos inline).
- Modo cantidad (`oculto` ausente, sin `largoFijo` o con uno alto): para
  cuando existan las pantallas de cantidad de pedido y cierre de caja
  (sprints posteriores) — se deja listo, no se usa todavía salvo en el
  campo PIN del formulario de usuario.
- Migrar `PantallaUsuarioFormulario.tsx`: el campo PIN hoy es un
  `<input maxLength={4}>` suelto (`.pin-code-inputs`) — pasa a usar
  `EntradaNumerica` en modo oculto, quedando consistente con el login.
- `comun/formato/moneda.ts`: `formatearMoneda(centimos: number): string`,
  redondeo mitad-arriba solo al formatear (regla 2.6), nunca en cálculos
  intermedios — la función solo formatea, no redondea valores que ya vienen
  en céntimos enteros (los céntimos nunca tienen fracción por definición).

## Fase D — Navegación

### Acordeón generalizado

`BarraLateral.tsx`: el árbol completo que diste (Operación / Negocio /
Administración con todos los módulos y sub-ítems, marcando pendiente lo
que no existe) reemplaza el array `NAVEGACION` actual. Cada `ItemNav` con
`subItems` se vuelve un `<button aria-expanded>` que no navega, con:

- Estado de abierto/cerrado por módulo en `localStorage` (clave por
  `etiqueta` del módulo), persistido entre recargas.
- Si la ruta activa es un hijo de un módulo cerrado, el módulo se abre
  solo al montar (efecto que revisa `location.pathname` contra las rutas
  de los hijos).
- Un módulo con **todos** los hijos pendientes (sin `ruta`) queda
  deshabilitado igual que hoy un ítem sin hijos y sin ruta.
- Un módulo con hijos mixtos (algunos con `ruta`, otros pendientes) se
  despliega igual — los pendientes se ven atenuados con la misma
  insignia "Pendiente" que ya existe.
- Animación: `max-height`/`grid-template-rows` con transición CSS (no
  librería nueva), respetando `prefers-reduced-motion`.

### Modo riel (nuevo)

- Botón de colapsar/expandir arriba de la barra (icono), estado en
  `localStorage`, independiente del estado de los acordeones.
- Colapsada: la barra muestra solo íconos (ancho fijo reducido), sin
  texto. Un módulo con hijos, al pasar el cursor (`mouseenter`, con
  pequeño delay) o al hacer clic, despliega un panel flotante
  (`position: fixed` o `absolute` respecto al ítem) con los mismos
  hijos que el acordeón mostraría expandido, en el mismo orden y con
  el mismo tratamiento de pendientes.
- El panel flotante se cierra al mover el cursor fuera (con delay para
  poder cruzar el hueco hasta el panel) o al hacer clic afuera.
- Accesibilidad: el trigger es el mismo `<button aria-expanded>` del
  acordeón — en modo riel controla el panel flotante en vez de la
  sección inline. Navegable por teclado: foco en el botón + `Enter`/
  `Espacio` abre el panel, `Escape` lo cierra, `Tab` recorre los hijos
  dentro del panel abierto.
- No se aplica en móvil (donde la barra ya es un cajón que se abre/cierra
  por completo) — el modo riel es exclusivamente de escritorio.

Solo aplicable a esta barra lateral (`apps/admin`); no toca `pos`,
`kiosco`, `portal`, `motorizado`.

## Fase E — Pantallas

Bajo "Catálogo y recetas" en el árbol de navegación (Fase D), 4 rutas
activas: `/admin/catalogo/productos`, `/admin/catalogo/categorias`,
`/admin/catalogo/unidades`, y Proveedores bajo "Compras y proveedores" en
`/admin/compras/proveedores` (confirmado: por dónde lo busca el usuario,
no por la épica del backlog).

- **Productos**: lista con búsqueda + filtro (categoría, naturaleza,
  estado), alta/edición con las tres banderas, categoría (selector de
  hoja — el árbol se muestra pero solo las hojas son seleccionables),
  unidad base, precio de venta condicional a `es_vendible`, bloque de
  costo inicial (solo si `es_insumo`) con el aviso de "estimado". Estado
  vacío: explica los pasos (crear categorías primero si no hay, luego
  productos), no una tabla en blanco.
- **Categorías**: árbol de dos niveles, alta de raíz (con naturaleza +
  componente_menu si aplica) y de subcategoría (hereda de la raíz,
  estos dos campos no editables en la subcategoría).
- **Unidades y presentaciones**: pestañas o secciones — unidades base
  (normalmente ya sembradas, pero editable/creable), presentaciones por
  producto (con el resultado en vivo "1 saco = 50 kg" mientras se
  escribe el factor), conversiones por producto (mismo patrón de
  resultado en vivo).
- **Proveedores**: alta/edición/desactivación, RUC opcional con
  validación de unicidad solo si no es nulo.

Los 4 estados (vacío, cargando, con datos, error) en las 4 pantallas.
Sin importación masiva.

## Fase F — Pruebas

**6 pruebas de acceso por rol** contra un endpoint representativo de
`catalogo` (p. ej. `POST /catalogo/productos` y `GET /catalogo/productos`):
administrador central puede crear, administrador de local puede crear,
cajero solo puede leer (403 al crear), mozo/cocina/motorizado sin acceso
(403 incluso a leer). Mismo patrón que ya existe para `/roles/**` en el
sprint anterior, adaptado a los permisos `catalogo.*`.

**Pruebas de dominio**: categoría obligatoria (rechaza producto sin
categoría), producto colgado de categoría-raíz-con-hijas rechazado,
conversión aplicada correctamente (factor × cantidad = resultado
esperado), desactivación de producto conserva su historial (no lo borra,
`activo=false` y sigue siendo consultable), unicidad de presentación
predeterminada (la segunda predeterminada activa desplaza o rechaza a la
primera — a decidir en el plan: lo más simple es rechazar con un error
claro en vez de desplazar en silencio).

## Verificación de cierre (checklist del pedido, mapeado a evidencia)

1. Capturas de las 4 pantallas incluida una vacía — vía navegador, con
   el mismo patrón de documentación que sprints anteriores
   (`doc/historias/US-008-US-009-US-011-catalogo.md`, siguiendo la
   convención ya establecida el 2026-09-23).
2. Conteo real de productos/categorías/unidades sembrados, verificado
   por consulta directa a la base, no solo "debería haber sembrado".
3. Explicación de `origen_costo` en el mismo documento.
4. Demostración de HT-05 con el diff real de un cambio de campo.
5. Las 6 pruebas de rol en verde, más el resto de la suite (no debe
   bajar de 23/23 existentes + las nuevas).
6. Registro de cualquier punto donde el modelo se haya quedado corto
   (se anota si aparece durante la implementación — no se anticipa
   ninguno ahora mismo).

## Riesgos / puntos a vigilar durante la implementación

- El único parcial de nombre-único-entre-raíces en `categoria_producto`
  necesita confirmarse contra el comportamiento real de Postgres con
  `NULL` en índices únicos parciales (debería funcionar como se describe,
  pero se verifica con una prueba de integridad, no se asume).
- El modo riel es la pieza más nueva y con más superficie de UI/
  accesibilidad de todo el sprint — si durante la implementación resulta
  más grande de lo estimado, se aísla como su propia tarea dentro del
  plan en vez de bloquear el resto de la Fase D.
