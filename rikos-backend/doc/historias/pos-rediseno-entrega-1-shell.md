# Rediseño del POS, entrega 1: shell, Inicio y Mesas (2026-09-30)

Especificación: `docs/producto/ui-ux/03-pos-rediseno.md` y
`docs/producto/plan/sprint-06b-pos-rediseno.md`. Diseño aprobado por el usuario
en el canvas (tableros PosDashboard, PosMesas, PosOrdenes, PosPedido, PosRapido,
PosHistorial).

## Qué se pidió
"Vamos haciendo el diseño, luego poco a poco lo arreglamos": pasar el POS al
diseño nuevo por entregas, con el método spec-driven y todo documentado,
coordinando con la sesión de pedidos.

## Qué se construyó (solo frontend)
- `features/pos/PosShell.tsx`: barra lateral colapsable (250 px / 78 px), cabezal con título y local, mozo activo y "Cambiar de mozo". Los ítems cuya pantalla aún no existe (Pedido rápido, Órdenes activas, Historial, Mi sesión) se ven atenuados con "Pronto".
- `features/pos/PantallaInicio.tsx`: mesas ocupadas (n / total y %), ventas del turno atenuadas ("Pronto"), acciones rápidas, "Mesas en uso" con su tiempo abierto y ocupación por salón. Todo sale de `GET /mesas` y `GET /salones`.
- `apps/pos/index.tsx`: ahora compone el shell; navega entre Inicio, Mesas y el pedido; cambiar de mozo o inactividad vuelven al PIN y a Inicio (US-003 intacta).
- `comun/componentes/Avisos.tsx`: `ProveedorDeAvisos` y `useAvisos()` (aviso flotante que se cierra a los 4 s). La sesión de pedidos puede usarlo en `PantallaPedido`; hoy el POS lo provee pero nadie lo invoca todavía.
- `comun/formato/tiempo.ts`: `tiempoTranscurrido` ("42 min", "1 h 12 min"), ahora compartido con el mapa de mesas.
- `estilos/pos.css` (importado en `main.tsx`).

## Qué se dejó fuera y por qué
- Pedido rápido, Órdenes activas e Historial: dependen de `GET /pedidos` (entrega 2, sesión de pedidos) o de su propia pantalla (entregas 3 a 5).
- "Pedidos activos" en Inicio usa las mesas en uso, no la lista de pedidos: el pedido para llevar no aparece hasta la entrega 3.
- Sin pruebas automáticas de frontend (el proyecto no las tiene); la verificación fue en navegador.

## Cómo se verificó
- `npx tsc -b --force` limpio; `npm run lint` sin errores (quedan 5 advertencias antiguas).
- Navegador (PIN de desarrollo): Inicio con 2 / 8 mesas ocupadas, tiempos abiertos reales y ocupación por salón; clic en Mesas muestra el mapa por salón; el botón de contraer deja la barra en 78 px; "Inicio" vuelve.

## Decisión registrada
La barra lateral en el POS contradecía `01-mapa-de-pantallas.md` ("sin barra lateral"). Resuelto en `03-pos-rediseno.md` § 1 y anotado en el mapa de pantallas.

## Pendiente
Ver `docs/pendientes/README.md`, sección "Del rediseño del POS y de salones y mesas".

## Ajuste posterior (2026-10-01)
La sesión de pedidos notó que en móvil (375 px) la barra lateral quedaba abierta y dejaba una columna de ~200 px al contenido. Ahora el shell **arranca colapsado en pantallas de menos de 900 px** (`apps/pos/index.tsx`); el botón sigue permitiendo expandirla. Verificado a 375 px: barra de 78 px, sin desplazamiento horizontal.
