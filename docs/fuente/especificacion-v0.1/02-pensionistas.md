# Módulo de pensionistas

## Ficha

Datos del pensionista:

- DNI, nombres y apellidos.
- Teléfono y correo.
- Universidad o instituto, código de estudiante y carrera.
- Ciclo o semestre opcional.
- Plan tarifario, límite de crédito y frecuencia habitual de abono.
- PIN presencial y contraseña del portal.
- Estado activo/inactivo.

Responsable obligatorio:

- Nombre completo, DNI, teléfono, correo, parentesco o relación y dirección.

## Cuenta corriente

- Registrar cada consumo únicamente al cerrar en caja.
- Mantener cargos, abonos, ajustes, saldo a favor, deuda y crédito disponible.
- Crédito disponible = límite individual - deuda pendiente.
- El límite es preventivo: alertar al superarlo, pero permitir el consumo.
- Aplicar primero el saldo a favor; el remanente aumenta la deuda.
- Permitir múltiples consumos del mismo servicio en un día y cobrar cada uno.
- Cierre mensual con saldo inicial, consumos, abonos, ajustes y saldo final.
- Arrastrar deuda o saldo a favor al siguiente mes sin marcar atraso automáticamente.

## Planes tarifarios

- Configurables y asignables a pensionistas.
- Definen precio de almuerzo normal y precios especiales por producto fijo de desayuno/cena.
- Tienen vigencia, estado e historial de modificaciones.
- Los consumos anteriores conservan la tarifa aplicada.
- Un menú extra se cobra al precio completo del extra, sin tarifa reducida.

## Autoservicio presencial

1. Pensionista ingresa DNI y PIN corto.
2. El sistema determina desayuno, almuerzo o cena automáticamente por horario configurable.
3. Imprime pre-ticket sin selección de platos.
4. El mozo recibe el papel, toma el pedido e ingresa manualmente su código.
5. Vincula pensionista, mesa/persona y productos.
6. Caja cierra el consumo; recién entonces crea el cargo y comprobante.

Estados del pre-ticket: pendiente, vinculado, cerrado, vencido y anulado. Un pendiente vence al terminar el horario del servicio. Un vencido o anulado no genera cargo.

El pre-ticket debe mostrar código único, pensionista, servicio, fecha/hora y la leyenda de que no es un consumo cargado. Debe impedir doble vinculación.

## Portal compartido

- Un solo acceso compartido por pensionista/responsable mediante DNI y contraseña.
- Consulta de movimientos, saldo, límite, crédito disponible, comprobantes y estados mensuales.
- Registro de abonos digitales pendientes.
- Notificaciones internas solo para abono aprobado/rechazado y estado mensual generado.
- Sin notificación por cada consumo ni por superar límite.
- El portal no permite modificar consumos ni aprobar abonos.

## Abonos

- Medios: efectivo, Yape, Plin y transferencia bancaria.
- Efectivo: lo registra caja, queda confirmado inmediatamente y entra en conciliación.
- Digital: puede registrarlo caja o el pensionista desde el portal; queda pendiente.
- El reporte desde portal exige imagen de comprobante, número de operación, monto, fecha, medio y cuenta destino.
- Validar posible duplicidad del número de operación.
- Solo el administrador aprueba o rechaza; mientras esté pendiente no modifica el saldo.
- Permitir abonos parciales y frecuencias habituales semanal, quincenal o mensual.

## Desactivación

- Bloquear nuevos tickets y consumos.
- Conservar cuenta, historial y portal de consulta.
- Permitir abonos posteriores.
- Una reactivación reutiliza la misma cuenta.

## Facturación

- Emitir un comprobante electrónico por cada consumo al cerrar en caja.
- Emitir la boleta a nombre del pensionista y con su DNI.
- Un abono no genera otra venta; puede generar constancia interna.

