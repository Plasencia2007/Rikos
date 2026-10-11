# Menú, recetas, producción, inventario y compras

## Catálogo

Clasificar productos como entradas, segundos/guisos, guarniciones/menestras, refrescos, postres, extras, productos fijos de desayuno/cena, insumos y subrecetas.

## Almuerzo

- Menú normal: entrada + segundo con guarnición + refresco + postre.
- Precio único, sin depender de las opciones elegidas.
- Si hay varias opciones, el mozo informa y registra la elegida; si hay una, se asigna directamente.
- Menú extra: menú completo cuyo segundo es especial; tiene precio propio.

## Programación semanal

- Programar una semana completa por día y servicio.
- Estados: borrador, confirmado y en servicio.
- Registrar opciones y porciones estimadas.
- El menú confirmado es editable sin modificar el catálogo maestro.
- Advertir si un segundo/guiso aparece en los tres días anteriores, pero permitir confirmar.
- El menú solo se publica dentro del POS.
- Desayuno y cena usan productos fijos elegidos individualmente; no llevan programación semanal.

## Recetas y costos

- Cada componente tiene receta valorizada y costo por porción.
- Permitir subrecetas dentro de otras recetas.
- Campos: insumos/subrecetas, cantidades, unidades, rendimiento y porciones.
- Valorizar con costo de última compra.
- Una nueva compra recalcula costos actuales y márgenes futuros.
- Actualizar directamente la receta maestra; la actualización aplica a nuevas ventas.
- Guardar en cada venta copia histórica de componentes, receta aplicada, costos, precio, utilidad y margen.
- Permisos configurables para ver costos, crear, modificar y desactivar recetas.

## Producción

- Al producir una subreceta, descontar sus insumos e ingresar el rendimiento producido.
- Al vender un plato final, descontar su receta, incluidas las cantidades de subrecetas.
- Registrar por plato porciones planificadas, preparadas y vendidas.
- No calcular automáticamente una lista de insumos requeridos desde la planificación.
- El encargado decide el destino de sobrantes: conservar, personal, pérdida, reutilización u otro.
- El destino produce el ajuste de inventario correspondiente.

## Inventario y Kardex

- Un inventario general por local.
- Permitir stock negativo con alerta, sin bloquear movimientos.
- Stock mínimo como cantidad configurable por producto y local.
- Unidades base y conversiones por presentación: cajas/unidades, sacos/kg, litros/ml, etc.
- Ajustes positivos/negativos con lista configurable de motivos y observación.
- Guardar stock anterior/nuevo, diferencia, costo, usuario, fecha y local.
- Transferencias entre locales mediante dos ajustes enlazados aplicados simultáneamente.
- Conteos físicos totales periódicos sin detener la operación.
- Tomar snapshot inicial y conciliar compras, ventas, producción y ajustes ocurridos durante el conteo.

## Compras

- Catálogo de proveedores y productos.
- Registro básico de ingreso, sin orden de compra ni cuenta por pagar.
- Asociar factura o boleta: tipo, serie, número, proveedor, fecha, productos, presentaciones, cantidades, precios, total y archivo adjunto.
- Confirmar compra aumenta stock y actualiza costo de última compra.

## Consumo interno

- Una comida sin cobro por trabajador y turno.
- Una segunda requiere autorización según permiso.
- Registrar trabajador, platos, turno, costo y descuento de inventario.
- Reportar separado de ventas.

