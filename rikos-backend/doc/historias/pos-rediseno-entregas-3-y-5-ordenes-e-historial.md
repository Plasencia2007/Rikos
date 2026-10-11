# Rediseño del POS, entregas 3 y 5: Órdenes activas e Historial (2026-09-30)

Especificación: `docs/producto/ui-ux/03-pos-rediseno.md` y
`docs/producto/plan/sprint-06b-pos-rediseno.md`. Backend de la sesión de pedidos:
`sprint-06b-lista-de-pedidos.md` (esta misma carpeta).

## Qué se pidió
Después del shell (entrega 1), conectar las pantallas que necesitan la lista de
pedidos del local: Órdenes activas (tarjetas con el contenido de cada orden) e
Historial, y dar datos reales al Inicio y al contador del menú.

## Qué se construyó (solo frontend)
- `api/cliente.ts`: `listarPedidos` (filtros situación, canal, estado, texto, fechas, página), `resumenDePedidos`, y los tipos `ResumenDePedido`, `PaginaDePedidos`, `FiltrosDePedidos`; `Pedido` gana `mozoNombre` y `abiertoEn` (opcionales).
- `features/pos/PantallaOrdenes.tsx`: una tarjeta por pedido en curso (cabecera verde si es mesa, azul si es para llevar, con mesa, número, hora, mozo, salón y cliente; líneas con sus componentes; tiempo abierto, estado y total; botón "Ver pedido"). Pestañas Todas / Mesas / Para llevar con contadores (Delivery atenuado "Pronto") y "Actualizar"; se refresca solo cada 15 s.
- `features/pos/PantallaHistorial.tsx`: filtros (texto con espera de 350 ms, tipo, desde y hasta), tarjetas de pedidos cerrados y paginación. Hoy muestra su estado vacío (el pedido aún no tiene estado "cerrado").
- `features/pos/PantallaInicio.tsx`: ahora usa `GET /pedidos/resumen` ("Pedidos en curso") y la lista de pedidos activos reales; "Acciones rápidas" enlaza a Órdenes activas.
- `apps/pos/index.tsx` y `PosShell`: contador de pedidos en curso en el menú, Órdenes e Historial habilitados, y el cabezal del shell se oculta cuando hay un pedido abierto (el pedido trae su propio encabezado).
- `estilos/pos.css`: tarjetas, pestañas, estados y filtros.

## Qué se dejó fuera y por qué
- Botones Imprimir y Pre-cuenta en la tarjeta: la impresión vive dentro del pedido (vista de comanda) y la pre-cuenta real llega con el cobro (sprint 7).
- Etiqueta "Anulada" y filtro por estado de pago: no existen en el modelo hasta el sprint 7.
- Pedido rápido (entrega 4): sigue pendiente.

## Cómo se verificó
- `npx tsc -b --force` limpio y `npm run lint` sin errores (5 advertencias antiguas).
- Navegador con datos reales de desarrollo: Inicio con 4 pedidos en curso y 3 / 8 mesas ocupadas; Órdenes activas con 4 tarjetas (3 mesas y 1 para llevar, con su tiempo abierto y estado); Historial con estado vacío; "Ver pedido" abre la pantalla de pedido sin cabezal duplicado.
- Sin pruebas automáticas de frontend (el proyecto no las tiene).

## Observación
Las tarjetas muestran "Sin productos todavía" cuando el pedido no tiene líneas; los pedidos de prueba que existen en desarrollo están vacíos, así que el contenido de las tarjetas con líneas y componentes no se vio en pantalla con datos reales (lo cubre el contrato probado por la otra sesión).
