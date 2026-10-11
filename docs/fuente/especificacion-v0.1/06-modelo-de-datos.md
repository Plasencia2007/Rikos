# Modelo conceptual de datos

Este modelo expresa responsabilidades y relaciones; no obliga a usar tablas ni nombres concretos.

## Organización

- Empresa 1—N Local.
- Local 1—N Caja, Mesa, Inventario, Compra, Pedido y Producción.
- Usuario N—N Local.
- Rol N—N Permiso; Usuario puede recibir roles y excepciones.

## Catálogo y costos

- Producto pertenece a una categoría y puede ser vendible, insumo, preparación o ambos.
- Producto 1—N PresentaciónCompra y ConversiónUnidad.
- Receta pertenece a un producto vendible o subreceta.
- Receta 1—N IngredienteReceta; un ingrediente referencia producto o subreceta.
- Compra N—1 Proveedor y 1—N DetalleCompra.
- DetalleCompra actualiza stock y costo vigente.

## Menú y producción

- SemanaMenu 1—N DiaMenu.
- DiaMenu 1—N OpcionMenu, clasificada por componente y servicio.
- OpcionMenu referencia Producto y guarda porciones estimadas/estado/disponibilidad por local.
- Produccion referencia subreceta o plato, local, rendimiento/porciones y movimientos de inventario.

## Inventario

- Existencia se identifica por Local + Producto.
- MovimientoInventario referencia origen: compra, producción, venta, ajuste, transferencia, conteo o consumo interno.
- Ajuste usa MotivoAjuste configurable.
- Transferencia enlaza movimiento de salida y entrada.
- ConteoFisico 1—N DetalleConteo y conserva snapshot/movimientos del periodo.

## Venta

- Pedido pertenece a local, canal, turno y usuario.
- Pedido puede asociarse a mesa, cliente regular o uno/más pensionistas.
- Pedido 1—N CuentaPersona; CuentaPersona 1—N DetallePedido.
- DetallePedido referencia producto, componentes seleccionados, precio y snapshot de costo/receta.
- Comanda 1—N DetalleComanda y registra impresiones.
- Pago N—1 Pedido/CierreParcial y usa MedioPago.
- ComprobanteElectronico referencia venta y respuestas del proveedor.

## Pensionistas

- Pensionista 1—1 CuentaPensionista y 1—1 Responsable.
- Pensionista N—1 PlanTarifario.
- PlanTarifario 1—N PrecioPlan por servicio/producto.
- CuentaPensionista 1—N MovimientoCuenta: consumo, abono, ajuste o arrastre.
- PeriodoCuenta 1—N MovimientoCuenta y genera EstadoCuentaMensual.
- PreTicket referencia pensionista, servicio, horario y pedido vinculado.
- Abono puede tener estado pendiente/aprobado/rechazado, adjunto y aprobador.

## Delivery y recojo

- PedidoDelivery referencia cliente, dirección, motorizado y estados con timestamps.
- HistorialDelivery registra cambios de estado, ubicación e incidencias.
- PedidoRecojo guarda fecha/hora, persona que recoge, celular y estado de preparación/retiro.

## Caja

- TurnoCaja pertenece a caja/local/cajero.
- TurnoCaja 1—N MovimientoCaja; solo ventas y abonos efectivos.
- CierreCaja conserva esperado, contado, diferencia y observación.

## Reglas de integridad esenciales

- DNI de pensionista único en la empresa.
- Código de pre-ticket único y de un solo uso.
- Número de operación digital detecta posibles duplicados.
- Una venta cerrada conserva snapshot económico inmutable.
- Un estado mensual cerrado no acepta movimientos retroactivos.
- No reabrir ventas de turnos cerrados.
- Todo movimiento de inventario debe tener origen rastreable.

