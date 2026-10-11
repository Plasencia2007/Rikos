# US-014 — Menú normal y menú extra

Sprint 4, entrega 2 de 4 (2026-09-29). Decisiones en
`docs/producto/plan/sprint-04-costos-y-menu.md` (sección "Decisiones de la
entrega 2").

## Qué se pidió

Definir la composición y el precio del menú normal (precio único, sin
importar qué opciones elija el cliente) y del menú extra (menú completo
con segundo especial, precio propio, sin tarifa reducida de pensionista).

## Qué se construyó

**Base de datos — `V21__menu_normal_y_extra.sql`.** Sexta naturaleza de
categoría, `menu`. Columna `producto.tipo_menu` ('normal' | 'extra') con
índice único parcial: exactamente un producto de cada tipo. Siembra la
categoría "Menú del día" y los productos "Menú normal" (`MENU-NORMAL`) y
"Menú extra" (`MENU-EXTRA`), vendibles, por unidad, **sin precio**
(RF-002: el dueño no dio los precios definitivos).

**Backend (`catalogo`).**

- `TipoMenu`: las reglas fijas en un solo lugar. Composición (CA1):
  entrada, segundo, guarnición, refresco, postre (los códigos de
  `componente_menu`). `admiteTarifaReducida()` (RN-023): verdadero en el
  normal, falso en el extra, para que el sprint 5 lo consulte.
- `CambiarPrecioMenu`: única vía para el precio del menú; rechaza nulo,
  cero o negativo; audita `cambio_precio` igual que el catálogo.
- Protecciones con `ErrorCatalogo.PRODUCTO_DE_MENU` y `CATEGORIA_RESERVADA`:
  no se desactiva ni se borra un producto de menú; la ficha de producto no
  le cambia precio, usos ni categoría (sí nombre, unidad y control de
  stock); no se crean ni se mueven otros productos a la categoría del menú.
- `MenuController`: `GET /menu/tipos` (`menu.ver`) y
  `PUT /menu/{tipo}/precio` (`menu.modificar`). `ProductoRespuesta` ahora
  trae `tipoMenu`.

**Frontend.**

- `features/menu/PantallaMenuNormalYExtra` (Menú semanal → Menú normal y
  extra): una tarjeta por menú con su precio ("Sin precio" + aviso de que
  la caja no podrá cobrarlo), edición del precio para quien tiene
  `menu.modificar`, lo que trae ("Segundo con guarnición") y la regla del
  pensionista.
- Ficha de producto de un menú: categoría, usos y precio bloqueados, con
  la indicación de dónde se cambia el precio; sin "Eliminar" ni
  interruptor de activo. En la lista de Productos, "Siempre" en vez del
  interruptor. La categoría del menú no aparece al crear un producto.
- La pantalla de Categorías no muestra "Menú del día" (solo lista las
  cinco naturalezas editables), así que no se le pueden colgar
  subcategorías.

## Qué se dejó fuera a propósito

- **Precios reales:** pendientes del dueño (RF-002). En la base de
  desarrollo se puso S/ 12,00 al normal solo para la prueba.
- **Cobro (CA2, CA3):** las ventas son del sprint 6 y los planes del
  sprint 5. Queda el precio único en un solo producto y la regla RN-023
  escrita y probada.
- **Composición configurable:** la especificación no la pide cambiar; es
  código, no una tabla que se pueda dejar incompleta.

## Cómo se verificó

**Pruebas automatizadas: 177 en verde** (162 al cerrar US-010; sin contar `ArquitecturaDeModulosTest`).

- `MenuTest` (11), contra la semilla real de V21: existen los dos y son
  vendibles; composición; RN-023; cambio de precio con bitácora; precio
  nulo/cero/negativo rechazado; tipo inexistente; no se desactiva ni se
  borra; la ficha no cambia precio ni usos, pero sí renombra aunque no
  haya precio; no se crean productos en la categoría del menú; no se mueve
  otro producto a ella.
- `MenuAccesoPorRolTest` (4): administrador central lee y cambia el
  precio; mozo, cajero, cocina y administrador de local leen pero reciben
  403 al cambiarlo; motorizado 403 hasta para leer; **cocina, que tiene
  `catalogo.modificar`, recibe 409 si intenta mover el precio por
  `PUT /productos/{id}`**.
- `ArquitecturaDeModulosTest`: solo el ciclo `comun ↔ identidad` conocido.

**En vivo** (usuario `admin`): la pantalla mostró los dos menús sin
precio y el aviso; se puso S/ 12,00 al normal (Enter guarda), el aviso
pasó a nombrar solo al extra, y la bitácora registró "admin cambió el
precio de "Menú normal" de sin precio a S/ 12.00". La ficha del menú
normal mostró categoría, usos y precio bloqueados y sin eliminar; un
producto nuevo no ofrece la categoría del menú.

## Encontrado en el camino

- El conteo de pruebas de la entrega 1 estaba inflado: se sumaba el
  reporte viejo de `ArquitecturaDeModulosTest` (2 pruebas, excluida de la
  corrida). Eran 162, no 164; corregido en `US-010-recetas-y-costeo.md`
  (primero se corrigió a 163, también mal: se descontó 1 en vez de 2).
- Docker Desktop volvió a arrancar sin problemas esta vez.
