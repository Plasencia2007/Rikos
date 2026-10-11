<!-- Extraído de rikos-backend/doc/RIKOS-modelo-de-datos.md (fuente completa, con la numeración de sección original intacta -- ver esa numeración si algún comentario en el código cita p. ej. "13.1"). Partido por bloque el 2026-09-29 per docs/METODO-spec-driven.md §5. -->

# 4. Bloque 2 — Catálogo, unidades y proveedores

Corresponde a EP-02 y a las historias US-008, US-009, US-011. Todo este bloque es **central**: no lleva `local_id`, porque el catálogo es común a todos los locales.

## 4.1 `categoria_producto`

Jerarquía de **dos niveles**: categoría y subcategoría. La lista de `04` § Catálogo —entradas, segundos y guisos, guarniciones y menestras, refrescos, postres, extras, productos fijos de desayuno y cena, insumos y subrecetas— **mezcla dos cosas distintas** que este modelo separa: la *naturaleza* de un producto dentro del sistema, y su *clasificación temática* dentro de esa naturaleza.

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `categoria_padre_id` | identificador | Sí | FK → `categoria_producto`. Nulo en las categorías raíz |
| `nombre` | texto(80) | No | Único dentro de su padre |
| `naturaleza` | enum('componente_menu', 'producto_fijo', 'extra', 'insumo', 'subreceta') | No | Se declara en la raíz y las hijas la heredan |
| `componente_menu` | enum('entrada', 'segundo', 'guarnicion', 'refresco', 'postre') | Sí | Solo si `naturaleza = componente_menu` |
| `nivel` | entero | No | 1 raíz, 2 subcategoría. Derivado, guardado para validar |
| `orden` | entero | No | Orden de presentación en el POS |
| `activo` | booleano | No | |

**Reglas**

- **Profundidad máxima: dos niveles.** No es una limitación de la especificación, es una decisión: con tres o más niveles, el mozo necesita tres toques para llegar a un plato, y los reportes por categoría dejan de ser comparables entre sí. Si algún día hicieran falta tres, el modelo ya lo soporta —basta subir el tope— pero conviene no empezar ahí.
- `naturaleza` y `componente_menu` **solo se declaran en la raíz**. Una subcategoría los hereda y no puede contradecir a su padre: «Pollo» bajo «Segundos y guisos» es `componente_menu = segundo`, igual que su padre.
- **Los productos cuelgan de la hoja**, no de la raíz. `producto.categoria_id` debe apuntar a una categoría sin hijas. Un producto colgado de una categoría que tiene subcategorías queda fuera de todos los reportes desglosados, y es el error más fácil de cometer al cargar el catálogo.
- `componente_menu` es lo que permite que el sistema sepa qué hueco del menú llena un plato **sin preguntarlo dos veces**. Ver la nota de 6.3 sobre `opcion_menu`.

### 4.1.1 El árbol propuesto para Riko's

Derivado de `04` § Catálogo, `04` § Almuerzo y `01` § Contexto del negocio. Los nombres son una propuesta: el propietario puede renombrarlos, pero la **forma** del árbol es la que sostiene los reportes.

| Nivel 1 — Categoría | Naturaleza | Nivel 2 — Subcategorías propuestas |
|---|---|---|
| **Entradas** | componente_menu · entrada | Sopas y caldos · Ensaladas · Causas y fríos |
| **Segundos y guisos** | componente_menu · segundo | Pollo · Res · Cerdo · Pescados y mariscos · Sin carne |
| **Guarniciones y menestras** | componente_menu · guarnicion | Arroces · Menestras · Papas y tubérculos |
| **Refrescos** | componente_menu · refresco | — |
| **Postres** | componente_menu · postre | — |
| **Extras** | extra | Segundos especiales · Porciones adicionales |
| **Platos fijos** | producto_fijo | Caldos y sopas · Platos de fondo · Sándwiches y panes · Bebidas calientes · Jugos y refrescos |
| **Insumos** | insumo | Carnes y aves · Pescados · Verduras y frutas · Abarrotes · Lácteos y huevos · Condimentos y especias · Descartables y táperes |
| **Subrecetas** | subreceta | Fondos y bases · Salsas y aderezos · Guisos base |

**Por qué «Platos fijos» y no «Desayuno» y «Cena» como categorías separadas.** Es el punto que obliga al cambio del apartado siguiente. Un té se vende en el desayuno **y** en la cena; un caldo de gallina también. Si «Bebidas calientes» colgara de «Desayuno», el té no podría aparecer en la cena sin duplicarlo como producto, y un producto duplicado significa dos stocks, dos costos y dos líneas en cada reporte. **El servicio no es una propiedad de la categoría: es una propiedad del producto.**

**Nota sobre los táperes.** El cargo de táper de RN-009 corresponde a un producto real que se descuenta del inventario, y por eso «Descartables y táperes» es una subcategoría de insumos y no un concepto aparte.

## 4.1 bis `producto_servicio`

En qué servicios se ofrece un producto fijo. Resuelve el hueco descrito arriba.

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `producto_id` | identificador | No | PK compuesta. FK → `producto` |
| `servicio` | enum('desayuno', 'almuerzo', 'cena') | No | PK compuesta |
| `activo` | booleano | No | |

**Reglas**

- Solo aplica a productos cuya categoría tiene `naturaleza = producto_fijo`. Los componentes del menú no la necesitan: su servicio es siempre el almuerzo y su disponibilidad la fija la programación semanal.
- **Es la tabla que responde la pregunta del POS**: «son las 7 de la noche, ¿qué puedo vender?». Sin ella, la carta de la cena habría que deducirla de la categoría, y eso obliga a que cada producto pertenezca a un solo servicio.
- Se combina con `disponibilidad_producto` (6.4): esta tabla dice qué se ofrece **normalmente** en ese servicio; aquella dice qué está agotado **hoy en ese local**.
- El precio del pensionista por servicio ya lo resuelve `precio_plan`, que lleva su propia columna `servicio` (7.2). Las dos tablas son coherentes: una dice qué se ofrece, la otra a cuánto para cada plan.

## 4.2 `unidad_medida`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `codigo` | texto(10) | No | Único. `kg`, `g`, `l`, `ml`, `und` |
| `nombre` | texto(60) | No | |
| `tipo` | enum('masa','volumen','unidad') | No | |
| `activo` | booleano | No | |

## 4.3 `producto`

La entidad más referenciada del modelo. Un mismo producto puede ser vendible, insumo, preparación, o varias cosas a la vez — de ahí que sean tres banderas y no un tipo único.

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `codigo` | texto(30) | No | Único |
| `nombre` | texto(150) | No | |
| `categoria_id` | identificador | No | FK → `categoria_producto`. **Debe ser una categoría hoja** |
| `unidad_base_id` | identificador | No | FK → `unidad_medida`. La unidad en que se lleva el stock |
| `es_vendible` | booleano | No | Puede aparecer en un pedido |
| `es_insumo` | booleano | No | Puede aparecer en una receta |
| `es_subreceta` | booleano | No | Es una preparación con receta propia |
| `precio_venta` | dinero | Sí | Obligatorio si `es_vendible`. Precio de público general |
| `costo_ultima_compra` | costo | Sí | Actualizado al confirmar una compra (RN-047) |
| `origen_costo` | enum('manual','compra') | Sí | De dónde salió el costo vigente |
| `fecha_ultima_compra` | fecha | Sí | |
| `controla_stock` | booleano | No | |
| `activo` | booleano | No | |
| `version` | entero | No | |

**Reglas**

- **RN-047, caso crítico 10.** Confirmar una compra actualiza `costo_ultima_compra` y recalcula los costos de las recetas **futuras**. Las ventas anteriores no cambian, porque guardaron su propio snapshot (2.4).
- **`origen_costo` resuelve un vacío del arranque (DT-048).** RN-047 valoriza con el costo de última compra, pero las compras llegan en el sprint 9: sin una salida, todo el catálogo nace en cero y el costeo de recetas del sprint 4 no se puede probar con números reales. Se permite cargar un costo **manual**, marcado como provisional. Cuando la primera compra confirmada lo reemplaza, el origen pasa a `compra` y **el campo deja de ser editable a mano**: un intento posterior se rechaza. Es lo que impide que alguien pise con una estimación un costo que ya viene de una factura.
- **Supuesto de modelado:** tres banderas booleanas en lugar de un enumerado `tipo`. `06` dice que un producto «puede ser vendible, insumo, preparación o **ambos**». Un enumerado no admite «ambos». El caso real es el arroz: se compra como insumo, entra en recetas, y puede venderse como guarnición.
- `stock_minimo` **no** va aquí: es configurable por producto **y local** (`04` § Inventario), así que vive en `existencia`.

## 4.4 `presentacion_compra`

Las formas en que se compra un producto: el saco de 50 kg, la caja de 24 unidades.

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `producto_id` | identificador | No | FK → `producto` |
| `nombre` | texto(80) | No | «Saco 50 kg» |
| `unidad_id` | identificador | No | FK → `unidad_medida` |
| `factor_a_unidad_base` | cantidad | No | Cuántas unidades base entrega una presentación |
| `es_predeterminada` | booleano | No | |
| `activo` | booleano | No | |

**Regla.** Una sola presentación predeterminada por producto entre las activas.

## 4.5 `conversion_unidad`

Conversiones generales entre unidades de un mismo producto, cuando no corresponden a una presentación de compra.

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `producto_id` | identificador | No | FK → `producto` |
| `unidad_origen_id` | identificador | No | FK → `unidad_medida` |
| `unidad_destino_id` | identificador | No | FK → `unidad_medida` |
| `factor` | cantidad | No | |

**Regla.** Único por (`producto_id`, `unidad_origen_id`, `unidad_destino_id`). Las conversiones se definen **por producto**, no globalmente: un litro de aceite y un litro de chicha no pesan lo mismo, y una conversión global de litros a kilos sería falsa.

## 4.6 `proveedor`

| Atributo | Tipo | Nulo | Descripción |
|---|---|---|---|
| `id` | identificador | No | PK |
| `ruc` | texto(11) | Sí | Único cuando no es nulo |
| `razon_social` | texto(200) | No | |
| `nombre_comercial` | texto(120) | Sí | |
| `telefono` | texto(20) | Sí | |
| `correo` | texto(160) | Sí | |
| `direccion` | texto(250) | Sí | |
| `contacto` | texto(120) | Sí | |
| `activo` | booleano | No | |

**Nota.** Los campos opcionales adicionales están pendientes (`07`, RF-009). El RUC admite nulo porque el mercado de abastos existe: una compra a un proveedor informal es real y el sistema no debe impedirla.

---

