# POS, caja, cocina y delivery

## POS y mesas

- PWA responsive para celular, tablet y terminal fija.
- Mapa de mesas con estados libre, ocupada, unida, pendiente de cierre y bloqueada.
- Mover pedidos, unir mesas, dividir por productos y trasladar productos entre cuentas.
- Permitir mesas mixtas con clientes regulares y varios pensionistas.
- Asignar productos a cada persona al tomar el pedido.
- Permitir cierres parciales sin liberar la mesa mientras haya consumos pendientes.

## Comandas

- Una sola impresora central de cocina.
- Mostrar pedido/mesa/canal, mozo, hora, productos por persona, componentes, observaciones y tipo de entrega.
- Marcar toda reimpresión como copia.
- Cocina informa pedido listo verbalmente o mediante campana; no requiere cambio digital de estado.
- Un producto anulado después de comandar no se cobra, pero siempre descuenta receta como consumo/pérdida.
- Un producto anulado antes de comandar no descuenta inventario.

## Disponibilidad

- Cocina o administración marca manualmente un producto agotado.
- Ocultarlo en pedidos nuevos sin modificar comandas existentes.
- Permitir reactivación; registrar usuario, hora y motivo.

## Caja

- Apertura y cierre por turno.
- El cajero puede ver el efectivo esperado antes de contar.
- Conciliar fondo inicial, cobros, abonos en efectivo y medios digitales/tarjeta.
- No permitir gastos, retiros ni ingresos extraordinarios; única excepción: abono en efectivo de pensionista.
- Alertar por cualquier diferencia desde S/0.01, sin bloquear el cierre.
- Permitir dividir libremente una cuenta entre efectivo, Yape, Plin y tarjeta.
- Saldo de pensionista se usa para consumos vinculados a su cuenta.
- Descuentos y anulaciones requieren permisos específicos y motivo.
- Solo un administrador autorizado puede reabrir una venta y únicamente mientras el turno de origen siga abierto.
- Después del cierre de turno, corregir mediante operación compensatoria/documento relacionado, sin alterar el cierre original.

## Facturación electrónica

- Integración con proveedor externo por API.
- Enviar boleta, factura y notas; guardar respuesta, estado y errores.
- Cola offline, reintentos idempotentes y prevención de comprobantes duplicados.
- Consulta y reimpresión de representación del comprobante.

## Recojo programado

- Recibido por WhatsApp y registrado manualmente en POS.
- Guardar fecha/hora futura, nombre y celular de quien recoge.
- Cliente regular: pago anticipado antes de confirmar.
- Pensionista: cargo al cerrar el pedido.
- Añadir táper por cada menú.
- Cancelación sin cobro antes de enviar la comanda.
- Si ya se preparó y no se recoge: descontar inventario y registrar no recogido.
- Cliente regular: no devolver pago; pensionista: cargar pedido completo y emitir comprobante.

## Delivery propio

- Recibido por WhatsApp y registrado manualmente.
- Inicialmente gratuito y sin zonas: caja decide si acepta la dirección.
- Administrador/caja asigna motorizado y controla estados: pendiente, asignado, aceptado, recogido, en ruta, entregado y fallido.
- Motorizado usa vista web móvil, consulta dirección/teléfono y cambia estados.
- Compartir última ubicación mientras está en ruta.
- La entrega se confirma solo marcando `entregado`.
- Motorizados con sueldo fijo; no calcular comisiones.
- Caja cierra la venta después de revisar/liquidar el pedido.
- Controlar dinero cobrado e incidencias por motorizado.

