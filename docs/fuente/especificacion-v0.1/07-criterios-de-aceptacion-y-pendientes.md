# Criterios de aceptación y pendientes

## Casos críticos de aceptación

1. Un pensionista ingresa DNI/PIN durante almuerzo, obtiene pre-ticket, el mozo lo vincula, toma opciones, comanda y caja cierra. Solo al cierre aparecen cargo y boleta.
2. Un pre-ticket no usado vence al terminar el horario y no afecta saldo.
3. Un segundo consumo del mismo servicio se permite y genera otro cargo.
4. Superar límite individual alerta, pero no bloquea el cierre.
5. Un abono digital con imagen queda pendiente y no cambia saldo hasta aprobación.
6. El cierre mensual arrastra deuda o crédito al siguiente mes y conserva el PDF anterior.
7. Una mesa mixta separa correctamente particulares y pensionistas desde la toma del pedido.
8. Un producto agotado desaparece de nuevos pedidos y no altera comandas emitidas.
9. Una anulación posterior a comanda descuenta receta como pérdida y no cobra al cliente.
10. Una compra confirmada aumenta stock, actualiza último costo y recalcula recetas futuras sin cambiar ventas anteriores.
11. Una subreceta descuenta insumos al producir; su uso se descuenta al vender el plato sin duplicar consumos.
12. Un conteo físico permite ventas simultáneas y concilia correctamente los movimientos ocurridos.
13. Un recojo regular exige pago anticipado; si se prepara y no retira, no devuelve el pago.
14. Un recojo de pensionista preparado y no retirado se carga completo a su cuenta.
15. Motorizado marca entregado; la venta no se cierra hasta liquidación de caja.
16. Ante caída de Internet, el local continúa operando y sincroniza sin duplicar pedidos, cargos ni comprobantes.
17. Una venta no puede reabrirse después de cerrar su turno.
18. Diferencia de caja de S/0.01 genera alerta.
19. Un usuario sin permiso no puede ver costos, anular, descontar ni ajustar inventario.
20. Una cuenta de pensionista funciona en todos los locales y muestra el local de cada movimiento.

## Pendientes funcionales

- Horarios exactos de desayuno, almuerzo y cena.
- Precios definitivos, planes y vigencias.
- Precio del táper por menú.
- Datos fiscales y proveedor de facturación API.
- Motivos iniciales de ajuste, anulación y rechazo de pagos.
- Plazo de bloqueo por intentos fallidos y método de recuperación de acceso.
- Frecuencia concreta de conteos físicos.
- Reglas de cortesía y productos compartidos.
- Campos opcionales adicionales de clientes y proveedores.
- Cantidad de locales iniciales, mesas, usuarios concurrentes y volumen pico.
- Política de retención de respaldos y adjuntos.
- Dispositivo/impresora del kiosco y modelo de impresora central.
- Reglas exactas de resolución de conflictos entre operaciones offline de varios locales.

## Validaciones externas pendientes

- Contrastar con contador el tratamiento tributario de ventas a crédito, abonos, notas y cierres mensuales.
- Validar con proveedor de comprobantes sus endpoints, estados, contingencia e idempotencia.
- Probar impresión y operación offline con el hardware real.

