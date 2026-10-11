# Pantalla de caja del POS (sprint 7)

**Hecha el 2026-10-11** a pedido del usuario, para ver el sprint 7 funcionando mientras llega el diseño de la sesión de diseño. Es una pantalla **básica y provisional**: usa los componentes
y la API existentes; si la sesión de diseño entrega la suya, la reemplaza (la API y los textos de error no cambian).

## Qué hay
Una vista nueva "Caja" en el POS (menú lateral, antes "Mi sesión · Caja y turno" deshabilitado). Código en `rikos-web/src/features/caja/`, CSS `caja-*` al final de `estilos/global.css`.

- **Sin turno → "Abrir turno de caja":** el fondo inicial **siempre se escribe** (puede ser cero); si el local tiene varias cajas libres se elige una. Al abrir, la caja se rotula **"Caja de <nombre del cajero>"**.
- **Con turno → panel de la caja:** efectivo esperado (fondo + cobrado), Yape, Plin, tarjeta y número de cobros; **"Por cobrar"** (pedidos comandados del local) y **"Cobrado en este turno"**.
- **Cobrar por líneas:** se marcan los productos (de una o varias personas, "Marcar todo"); descuento por línea (porcentaje o monto, con un **descuento habilitado** = motivo de tipo
  descuento activo, solo si el usuario tiene `caja.descontar`); pago dividido en efectivo (se escribe lo **recibido**, el sistema da el vuelto), Yape, Plin y tarjeta (con número de operación
  opcional); "Exacto en efectivo". Lo de un pensionista no pide medios: se carga a su cuenta y el resultado avisa si supera su tope. Al terminar, el resultado muestra total, vuelto, pagos,
  cargos y si el pedido quedó cerrado (mesa libre) o quedan productos por cobrar.
- **Reabrir un cobro** desde "Cobrado en este turno" (permiso `caja.reabrir`, motivo de tipo reapertura): los productos vuelven a estar por cobrar y el dinero sale de la caja.
- **Cerrar turno:** muestra el efectivo esperado ANTES de contar, el cajero escribe lo contado, la diferencia sale al instante ("Faltan S/ 1.00") y el cierre muestra el resultado con la alerta.
- **Permisos:** sin `caja.ver` la pantalla dice "La caja es del cajero" (el mozo pide pero no cobra); sin `caja.crear` no hay botones de cobrar ni de cerrar; sin `caja.reabrir` no hay "Reabrir".

## Botón "Cobrar" en la pantalla del pedido (2026-10-11)
La pantalla del pedido (Orden de la mesa) muestra un botón **Cobrar** junto a "Comandar" cuando el usuario tiene `caja.crear` y hay productos ya comandados. Lleva a la Caja con ese pedido ya
abierto para cobrar (si no hay turno abierto, primero pide el fondo y luego abre el cobro). Toca `OrdenDeLaMesa.tsx`, `PantallaPedido.tsx` y `apps/pos/index.tsx` (archivos de la sesión de diseño), con cambios mínimos.
Verificado en el navegador: Órdenes activas → Ver pedido → Cobrar → "Cobrar pedido 0002". El pedido de prueba se limpió anulando su línea y terminándolo (sin tocar el turno abierto).

## Backend agregado
`GET /turnos/actual/cobros`: los cobros del turno abierto del cajero (id, pedido, total, vuelto, si se reabrió), el más reciente primero. Una prueba nueva en `CierreYReaperturaAccesoPorRolTest`.
Cliente de la API en `api/cliente.ts` (`cobrosDelTurno`, y las demás funciones de caja de las entregas 1 a 5).

## Cómo se verificó
En el navegador con el backend real (usuario de desarrollo): abrir turno con fondo S/ 100; un pedido de prueba de S/ 66 (mesa 1) cobrado con descuento del 10% a una línea, efectivo con
vuelto de S/ 39; reabierto (el esperado volvió a S/ 100 y el pedido a "por cobrar"); cobrado de nuevo con efectivo S/ 36 + Yape S/ 30 con número de operación; cierre con S/ 135 contados contra
S/ 136 esperados (alerta y "no se puede modificar"). A 375 px sin scroll horizontal. `tsc -b` y lint sin errores.
Datos de desarrollo que quedaron: 3 motivos creados por la API (descuento "Cortesía de la casa" y "Cliente frecuente", reapertura "Error al cobrar") y el turno cerrado con su cierre (un cierre no se puede borrar).

## Pendiente
- El diseño real de la pantalla (esta es provisional) y su versión para impresora/tableta si se quiere.
- El ticket o recibo imprimible del cobro (no se pidió todavía).
- Mostrar el nombre del pensionista en la orden y los nombres de los cobros de otras cajas.
