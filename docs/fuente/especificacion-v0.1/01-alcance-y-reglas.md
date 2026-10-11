# Alcance y reglas generales

## Contexto del negocio

Riko’s es un restaurante criollo peruano que atiende público general y una proporción importante de estudiantes pensionistas. El almuerzo cambia diariamente; desayuno y cena usan productos fijos.

## Actores

- Administrador central.
- Administrador de local.
- Cajero.
- Mozo.
- Cocina.
- Motorizado.
- Pensionista y responsable, con acceso compartido.
- Cliente regular.

## Canales

- Consumo en mesa.
- Para llevar.
- Delivery propio gratuito inicialmente.
- Recojo programado.

Los pensionistas pueden utilizar su cuenta en todos los canales. Para llevar, recojo y delivery añaden un cargo de táper por cada menú.

## Multilocal

- El sistema será multilocal desde la primera versión.
- Catálogo, recetas, precios, planes y programación del menú se administran centralmente y son comunes.
- Cada local mantiene inventario, compras, producción, caja, mesas, disponibilidad y delivery propios.
- Cada local tiene un único inventario general.
- La cuenta del pensionista es única y utilizable en cualquier local.
- Una transferencia simple entre locales genera ajustes enlazados de salida y entrada al confirmarse.

## Auditoría transversal

Registrar usuario, fecha, hora, local, valores anteriores y nuevos, y motivo cuando aplique en:

- Descuentos y anulaciones.
- Reaperturas.
- Ajustes y transferencias de inventario.
- Cambios de recetas, precios y permisos.
- Aprobación o rechazo de abonos.
- Cierres de caja.

## Reglas de historial

- Las ventas cerradas conservan precio, costo y composición históricos.
- Los cierres mensuales de pensionistas y los cierres de caja son inalterables después de cerrados.
- Desactivar registros no elimina su historial.

