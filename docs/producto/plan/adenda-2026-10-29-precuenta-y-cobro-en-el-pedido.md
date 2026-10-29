# Adenda 2026-10-29 — Pre-cuenta, comanda y cobro desde el pedido (mozo con rol de cajero)

## Qué pidió el usuario

Que un mozo que también tenga el rol de **cajero** pueda, desde la pantalla del pedido de la mesa (el panel
«Pedido de la mesa»):

- ver el **total**;
- **previsualizar** la pre-cuenta y la comanda (botones con ojo: solo mirar);
- **imprimir** la pre-cuenta y la comanda (botones sin ojo: «esto ya es para enviarlo e imprimirlo»);
- **cobrar** el pedido.

Todo eso aparece **solo si el usuario tiene el permiso de cajero**. Si no, el pie queda como está («Enviar a
cocina»). Referencia: capturas de otro sistema con tickets de 80 mm (pre-cuenta y comanda).

## Reglas

1. **Quién lo ve:** quien tiene `caja.crear` (el permiso con el que ya se ofrece «Cobrar»). El mozo de prueba
   `mozo1` recibe además el rol `cajero` en los datos de prueba, para poder probarlo.
2. **Total:** el total de la mesa, como ya se muestra.
3. **Prev. Pre-cuenta:** abre el ticket de pre-cuenta en pantalla. No imprime ni cambia nada.
4. **Pre-cuenta:** abre el mismo ticket y lo manda a imprimir. **No cambia el pedido**: no lo cierra ni lo cobra. No
   se registra en la base (no hay tabla de pre-cuentas; es una hoja informativa).
5. **Prev. Comanda:** muestra en pantalla el ticket de la **última comanda** enviada. Sin comandas, dice que todavía
   no se ha enviado nada a cocina.
6. **Comanda:**
   - Si hay productos nuevos sin enviar: los **envía a cocina** (lo mismo que «Enviar a cocina») e imprime la comanda
     nueva.
   - Si no hay nada nuevo: **reimprime la última** comanda; el servidor la registra y sale con la leyenda COPIA
     (RN-063), como en el detalle de comanda.
7. **Cobrar — S/ total:** lleva a la Caja con este pedido (lo que ya hace «Cobrar»). Disponible cuando hay algo
   enviado a cocina, como hoy.
8. **Ticket de pre-cuenta** (80 mm, letra de máquina):
   - Cabecera: «RIKO'S», el local.
   - «PRE-CUENTA» y «** NO ES COMPROBANTE DE PAGO **».
   - Pedido, mesa (o «Para llevar»), mozo, fecha y hora.
   - Cantidad, producto e importe; debajo, las opciones del menú y las notas.
   - Táper si hay, total y «GRACIAS POR SU PREFERENCIA».
   - Las líneas anuladas no salen.
9. **Ticket de comanda** (80 mm): «COMANDA», pedido y número de comanda, mesa, mozo, hora; por persona, cantidad,
   producto, opciones etiquetadas (entrada, segundo, bebida, postre) y notas. Si es reimpresión, «COPIA».

## Fuera de esta adenda

- RUC, razón social y dirección del restaurante en la pre-cuenta: no existen en el sistema. Llegan con
  Configuración → Mi restaurante (pendiente).
- Guardar las pre-cuentas emitidas o contarlas: no se pidió.
- Imprimir directo a una impresora térmica sin el diálogo del navegador (HT-10, impresora real pendiente).
