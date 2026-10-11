# Sprint 6b — Categorías del catálogo del pedido (2026-10-01)

**Qué se pidió (usuario):** que el catálogo del pedido tenga las categorías
**Todo, Entrada, Segundo, Extra**, con estos productos: Entrada = Ensalada,
Ceviche, Sopa; Segundo = Arroz con pollo, Ají de gallina; Extra = Chaufa, Pollo
frito, Milanesa. "Primero el backend", después la pantalla.

## Decisión: se usan las categorías que ya existen
El catálogo ya trae una jerarquía sembrada (sprint 2) con **tres grupos
raíz** que son justo estos: `Entradas` (naturaleza `componente_menu`,
componente `entrada`), `Segundos y guisos` (componente `segundo`) y `Extras`
(naturaleza `extra`). Cada producto cuelga de una subcategoría (Ensaladas, Pollo,
Segundos especiales…) y su **grupo es la categoría raíz**. No se crearon
categorías nuevas ni se duplicaron con nombres en singular. **"Todo" no es una
categoría: es el filtro que muestra todos los productos.**
En pantalla las pestañas se rotulan en singular: Entrada, Segundo, Extra.

## Productos (datos de desarrollo, precios INVENTADOS)
| Grupo | Producto | Código | Subcategoría | Precio | Origen |
|---|---|---|---|---|---|
| Entrada | Ensalada | ENT-0103 | Ensaladas | S/ 8,00 | nuevo |
| Entrada | Ceviche | ENT-0104 | Causas y fríos | S/ 25,00 | nuevo |
| Entrada | Sopa criolla | ENT-0101 | Sopas y caldos | S/ 10,00 | ya existía (hace de "Sopa") |
| Entrada | Papa a la huancaína | ENT-0102 | Causas y fríos | S/ 10,00 | ya existía |
| Segundo | Arroz con pollo | POL-001 | Pollo | S/ 10,00 | ya existía |
| Segundo | Ají de gallina | SEG-0101 | Pollo | S/ 10,00 | ya existía |
| Segundo | Lomo saltado, Seco de res | SEG-0102, PLT-0101 | Res | S/ 10,00 / 15,00 | ya existían |
| Extra | Chaufa | EXT-0101 | Segundos especiales | S/ 18,00 | nuevo |
| Extra | Pollo frito | EXT-0102 | Segundos especiales | S/ 16,00 | nuevo |
| Extra | Milanesa | EXT-0103 | Segundos especiales | S/ 17,00 | nuevo |
| Extra | Huevo frito adicional | CARTA-05 | Porciones adicionales | S/ 3,00 | ya existía |

Los productos nuevos se crearon por la API del catálogo (`POST /productos`),
como vendibles, sin control de stock. Las bebidas y postres (Gaseosa, Jugo,
Chicha, Mazamorra) **no pertenecen a ninguno de los tres grupos**: se ven solo en
"Todo". Los precios reales siguen pendientes con el dueño (RF-002).

## Lo que falta en el backend (sesión de pedidos)
`GET /pedidos/{id}/oferta` devuelve la carta como `ProductoOfrecido(id, nombre,
precio)` sin categoría. Se pide agregarle **`grupo`** (`entrada` | `segundo` |
`extra` | `null`, calculado desde la categoría raíz del producto, no por el
nombre) y, para mostrar mejor, **`categoria`** (nombre de la subcategoría);
ordenar la carta por grupo (entrada, segundo, extra, otros) y nombre. Con tests.

## Decisión del usuario (2026-10-01): solo cuatro pestañas
El catálogo del pedido tiene **solo Todo · Entrada · Segundo · Extra** ("las de
un restaurante de verdad"); desaparecen las pestañas "Menú del día", "Menú
extra", "Carta" y el "Extra" libre. **Para no romper el negocio de los
pensionistas** (el menú del día lleva componentes y el precio del plan lo decide
el motor), se conservan sus flujos y solo cambia dónde se ven:
- **Menú del día y Menú extra** salen como **las dos primeras tarjetas de
  "Todo"** (solo si hay menú programado y confirmado para hoy), y siguen abriendo
  su diálogo de opciones.
- **Extra libre** (precio puesto por el mozo, queda marcado como manual y
  auditado) sale como **tarjeta dentro de "Extra"**, con permiso.
- **Entrada, Segundo y Extra** muestran los productos de su grupo; **Todo**,
  además de las dos tarjetas de menú, todos los productos (también bebidas y
  postres, que no tienen grupo).
Diseño actualizado en el canvas (`PosPedidoUnida`, `PosPedidoSola`).

## Lo que falta en la pantalla
Las pestañas del catálogo del pedido pasan a **Todo · Entrada · Segundo ·
Extra** filtrando por `grupo` (sin romper "Menú del día" y "Menú extra", que
vienen de la programación semanal). Diseño aprobado en el canvas
(`PosPedidoUnida` / `PosPedidoSola`) con el buscador sobre las categorías.

## Verificación de la pantalla (2026-10-01, sesión del mapa y del shell)
Probada en el navegador con el backend real: pestañas **Todo / Entrada / Segundo / Extra** filtran bien (Entrada: Ceviche, Ensalada, Papa a la huancaína, Sopa criolla; Segundo: Ají de gallina, Arroz con pollo, Lomo saltado, Seco de res; Extra: Chaufa, Pollo frito, Milanesa, Huevo frito adicional; Todo: las dos tarjetas de menú y todo lo demás, con las bebidas y postres solo ahí); agregar Ceviche deja la línea "Por enviar" con S/ 25,00; a 375 px sin desborde, personas con scroll, tarjetas en 2 columnas y la orden debajo.
Observaciones abiertas: (1) en celular el botón **Comandar** queda al final de una página larga: conviene una barra fija inferior "Comandar (N)"; (2) el nombre del pensionista solo se ve si se identificó en esa pantalla (el pedido trae `pensionistaId`, no el nombre): pedir `pensionistaNombre` al backend; (3) la pestaña "Extra" no mostró la tarjeta de "Extra libre" con el usuario admin: confirmar con la sesión de pedidos que depende de un permiso.
