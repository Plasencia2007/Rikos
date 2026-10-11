# Sprint 6b — Rediseño del POS

**Qué es:** un sprint corto entre el 6 (vender en mesa, completo salvo HT-10) y el
7 (cobrar). No agrega reglas de negocio nuevas: reorganiza cómo se ve y se
navega el POS, y pide al backend **consultas** que hoy no existen. Diseño y
decisiones en `../ui-ux/03-pos-rediseno.md`.

**Estado:** especificado y diseño aprobado por el usuario el 2026-09-30.
Entregas 1 (shell, Inicio y Mesas), 3 (Órdenes activas) y 5 (Historial, vacío hasta el sprint 7) hechas el 2026-09-30, ver `rikos-backend/doc/historias/pos-rediseno-entrega-1-shell.md` y `pos-rediseno-entregas-3-y-5-ordenes-e-historial.md`; la 4 (Pedido rápido) hecha el 2026-10-01, ver `pos-rediseno-entrega-4-pedido-rapido-y-pantalla-del-pedido.md`. Se avanza "poco a poco": diseño primero, ajustes después
(palabras del usuario).

## 1. Qué queremos lograr

Que el mozo navegue el POS con un menú claro (Inicio, Mesas, Pedido rápido,
Órdenes activas, Historial), vea las órdenes en curso de un vistazo (tarjetas
con su contenido) y abra un pedido rápido para llevar sin pasar por el mapa.

**Éxito del sprint:** desde el PIN se entra a Inicio; se abre una mesa desde el
mapa por salón; se crea un pedido para llevar desde Pedido rápido; ambas
órdenes aparecen en Órdenes activas con sus líneas y se abren con "Ver pedido".
El Historial existe con su estado vacío: los pedidos cerrados llegan con el
sprint 7.

## 2. Quién hace qué (dos sesiones en paralelo)

| Parte | Responsable |
|---|---|
| Shell del POS (`apps/pos/index.tsx`, estilos `pos-*`), Inicio, Órdenes activas, Historial, Pedido rápido, mapa de mesas | Sesión de salones y mesas (frontend) |
| `PantallaPedido` y sus diálogos, `PedidoController` y el módulo `venta` de pedidos | Sesión de pedidos |
| Consultas nuevas de pedidos (sección 3) | Acordado por mensaje: la sesión de pedidos, salvo que diga lo contrario |

Regla para no pisarse: nadie edita archivos de la otra parte sin avisar; si
un diseño cambia el encabezado del pedido, se pide por mensaje con los datos y
la estructura.

## 3. Lo que el backend debe ofrecer (contrato propuesto)

1. `GET /pedidos?situacion=en_curso|cerrados&canal=&estado=&q=&desde=&hasta=&pagina=&tamano=`
   (especificado por la sesión de pedidos en `sprint-06b-lista-de-pedidos.md`;
   devuelve `{contenido, pagina, tamano, total, totalPaginas}`, líneas sin las
   anuladas y `mozoNombre` = nombre de usuario) -> página de resúmenes: `id, numero, canal, servicio, estado, mesaId,
   mesaNumero, mesaNombre, salonNombre, nombreCliente, comensales, mozoNombre,
   abiertoEn, total` y, solo en curso, `lineas:[{cantidad, nombre, detalle,
   componentes[]}]`. Permiso `venta.ver`.
2. `GET /pedidos/{id}` gana `mozoNombre` y `abiertoEn`; y `GET /pedidos/resumen`
   -> `{enCurso, porCobrar}` para el contador de la barra lateral.
3. Ya existe (sesión de pedidos, 2026-09-30): `POST /pedidos` con `comensales`
   y `nombreCliente`; `GET /mesas` con `abiertaEn`.

**Por qué una consulta nueva y no reusar `GET /mesas/{id}/pedido`:** ese
devuelve el pedido de una mesa; las pantallas nuevas necesitan la lista del
local, filtrable y paginada, y también los pedidos sin mesa (para llevar).

## 4. Entregas (cada una verificable)

| # | Entrega | Backend | Cómo se verifica |
|---|---|---|---|
| 1 | Shell del POS (barra lateral, cabezal, avisos), Inicio con lo que ya hay, Mesas dentro del shell | -- | `tsc -b --force`, lint limpio, navegador: PIN -> Inicio -> Mesas -> abrir una mesa -> volver |
| 2 | Consultas de pedidos (sección 3) con tests y documentación | `GET /pedidos`, mozo y abiertoEn | Pruebas de integración: filtros, paginación, permisos, líneas, aislamiento por local |
| 3 | Órdenes activas en tarjetas + contador en la barra lateral | usa 2 | Navegador con pedidos reales (mesa y para llevar) |
| 4 | Pedido rápido (para llevar) con catálogo y carrito | usa lo existente | Crear un pedido para llevar, comandar, verlo en Órdenes activas |
| 5 | Historial de órdenes (vacío hasta el sprint 7: el pedido aún no tiene estado `cerrado`) | usa 2 con `situacion=cerrados` | Devuelve lista vacía y la pantalla muestra su estado vacío; con datos de prueba en la base de pruebas se ven las tarjetas |

- **Responsive (criterio de aceptación de todas las entregas):** móvil 375 px,
  tableta 768 px y escritorio 1280 px sin desplazamiento horizontal; ver
  `ui-ux/03-pos-rediseno.md` § 4b.

## 5. Decisiones que condicionan

- **Barra lateral en el POS** contradice `01-mapa-de-pantallas` ("sin barra
  lateral"); resuelto en `03-pos-rediseno.md` §1.
- **Mostrador no entra;** Delivery queda atenuado hasta el sprint 12.
- **Cobro y caja** quedan atenuados ("Pronto") hasta el sprint 7.

## 6. Fuera del sprint (y por qué)

- Cobrar, pre-cuenta real y filtro de pagos: sprint 7.
- Fotos de producto: el catálogo no guarda imagen.
- Notificaciones ("Entrantes por aprobar", campana): dependen de canales no
  presenciales (sprint 12).
