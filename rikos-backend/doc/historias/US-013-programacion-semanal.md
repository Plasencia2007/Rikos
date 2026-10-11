# US-013 — Programación semanal del almuerzo

Sprint 4, entrega 3 de 4 (2026-09-29). Decisiones en
`docs/producto/plan/sprint-04-costos-y-menu.md` (sección "Decisiones de la
entrega 3").

## Qué se pidió

Programar la semana de almuerzos: por día, las opciones de entrada,
segundo, guarnición, refresco y postre con sus porciones estimadas.
Borrador → confirmado → en servicio. Editable después de confirmar sin
tocar el catálogo. Advertir, sin bloquear, si un segundo se repite en los
tres días anteriores. Visible solo dentro del POS. Desayuno y cena no se
programan.

## Qué se construyó

**Base de datos — `V22__programacion_semanal.sql`.** `semana_menu` (lunes
obligatorio por CHECK, `fecha_fin = fecha_inicio + 6`, estado solo
`borrador`/`confirmado`, única por fecha), `dia_menu` (fecha única en toda
la tabla), `opcion_menu` (componente guardado, `tipo_menu` con CHECK "el
extra solo en segundos", porciones > 0, única por día y producto).

**Backend (`catalogo`).**

- `ConsultaMenu`: "hoy" con el `Clock` del sistema, días cerrados,
  advertencias por repetición y el menú confirmado de un día para el POS.
- Casos de uso: `CrearSemanaMenu`, `AgregarOpcionMenu` (componente
  derivado de la categoría; día creado al programarle la primera opción),
  `ActualizarOpcionMenu`, `QuitarOpcionMenu`, `ConfirmarSemanaMenu`
  (audita `cambio_estado` y devuelve las advertencias), `EliminarSemanaMenu`
  (solo borrador).
- `ProgramacionMenuController`: `GET /menu/semanas?lunes=`,
  `POST /menu/semanas`, `POST /menu/semanas/{id}/opciones`,
  `PUT|DELETE /menu/opciones/{id}`, `POST /menu/semanas/{id}/confirmar`,
  `DELETE /menu/semanas/{id}`, `GET /menu/dias/{fecha}` (POS). Todo lo que
  cambia devuelve la semana entera con las advertencias recalculadas.
  Permisos: `menu.ver`, `menu.crear` (crear semana), `menu.modificar`
  (opciones, confirmar), `menu.desactivar` (quitar, descartar).
- `comun.tiempo.ConfiguracionReloj`: `Clock` en zona `America/Lima`.
- `EliminarProducto` también rechaza un producto que ya estuvo en el menú
  de algún día (`TIENE_HISTORIAL`).
- Bitácora: "admin confirmó la semana de menú del 28/09/2026".
- Las opciones salen en el orden del menú (entrada, segundo, guarnición,
  refresco, postre) y el normal antes que el extra; así las recibe el POS.

**Frontend.** `features/menu/PantallaProgramacionSemanal` (Menú semanal →
Programación semanal): navegación por semanas con estado (Borrador,
Confirmada, En servicio, Pasada), crear semana, una tarjeta por día con
las opciones agrupadas por parte del menú, marca "Extra", porciones y
"Se repite"; agregar opción (parte del menú → plato → normal/extra si es
segundo → porciones), editar o quitar al hacer clic en una opción; aviso
de repeticiones arriba y en el diálogo de confirmación ("Confirmar
igual"); descartar borrador. Días cerrados en gris y sin edición.

## Qué se dejó fuera a propósito

- **La pantalla del POS** que consume `GET /menu/dias/{fecha}`: sprint 6.
- **Observación por día** (`dia_menu.observacion`): está en la tabla pero
  ningún criterio la pide; sin pantalla.
- **Copiar la semana anterior**: no lo pide la historia; sería la primera
  mejora útil si programar desde cero resulta lento.

## Cómo se verificó

**Pruebas automatizadas: 196 en verde** (177 al cerrar US-014; sin contar
`ArquitecturaDeModulosTest`, que solo reporta el ciclo `comun ↔ identidad`
conocido).

- `ProgramacionSemanalTest` (15), con "hoy" fijo en un miércoles: semana
  en borrador de lunes a domingo; lunes obligatorio, única, no terminada;
  estados calculados (confirmado, en servicio, pasada); componente
  derivado y porciones; rechazos (producto que no es del menú, extra en
  una entrada, plato repetido el mismo día, fecha fuera de la semana,
  porciones 0, tipo inválido); días anteriores cerrados y hoy abierto;
  editar y quitar, y al día siguiente ya no; advertencia dentro de la
  semana; advertencia que cruza semanas y no pasa de tres días; confirmar
  con repeticiones y devolverlas; no confirmar vacía; editar confirmada
  sin tocar el catálogo; el POS solo ve semanas confirmadas; orden del
  menú; solo se descarta un borrador.
- `ProgramacionMenuAccesoPorRolTest` (4): el administrador central
  programa, confirma y el POS lo ve (antes de confirmar, 404); mozo,
  cajero, cocina y administrador de local leen pero reciben 403 al crear,
  agregar, confirmar, quitar y descartar; motorizado 403; el menú del día
  sin sesión no responde.

**En vivo** (usuario `admin`, semana del 28/09 al 04/10/2026, que se
creó desde el panel): ají de gallina el martes con 40 porciones desde el
modal (solo ofreció segundos); por API se completó el martes (sopa
criolla, lomo saltado como extra, chicha morada, mazamorra morada) y el
ají de gallina se repitió el jueves. La pantalla mostró "Ají de gallina
se repite el jueves, 1 oct.: ya se sirvió el martes, 29 set." y la marca
"Se repite"; el extra en una entrada dio 422. "Confirmar semana" abrió el
diálogo con la repetición y "Confirmar igual"; la semana pasó a "En
servicio", `GET /menu/dias/2026-09-29` devolvió las cinco opciones y la
bitácora registró la confirmación. A 375 px de ancho no hay desplazamiento
horizontal.

## Encontrado en el camino

- **Borrar la semana con días cargados en la sesión de Hibernate**
  reventaba (`TransientPropertyValueException`) aunque la base tenga
  `ON DELETE CASCADE`. Se borran opciones, días y semana en orden.
- **Un `switch` de Java no admite `when` en un `case` de texto**; la frase
  de la bitácora para la semana va en un `if` antes del `switch`.
- **Conteo de pruebas mal dos veces.** El reporte de
  `ArquitecturaDeModulosTest` tiene 2 pruebas, no 1. Los conteos reales
  son 162 (US-010) y 177 (US-014); corregidos en sus documentos.
- La advertencia terminaba en "set.." porque la fecha abreviada ya trae
  punto; corregido.
