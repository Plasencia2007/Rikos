# Motivos de operación: pantalla rediseñada (2026-09-30)

**Qué se pidió.** Rehacer la pantalla de Motivos (US-012) en el tema verde, diseñada primero en el
canvas ("Motivos de operación"): una tarjeta por tipo con su tabla, formulario en línea para
agregar y editar, y los tipos sin motivos compactos. Después se pidió que no quedaran huecos
blancos entre tarjetas de distinta altura.

**Qué se construyó.**
- `features/configuracion/PantallaMotivos.tsx` + `estilos/motivos.css` (reemplaza las pestañas y el
  modal anteriores).
- Tarjetas solo para los tipos con motivos (o con el formulario abierto), en **columnas tipo
  masonry** (`column-count: 2`, `break-inside: avoid`): sin huecos entre filas. Los tipos vacíos van
  abajo, en una sección "Sin motivos todavía" con una fila por tipo.
- Cada tipo explica **cuándo se pide** ("Se piden al anular una venta o un pedido ya registrado").
- Formulario en línea (nombre + "Exigir observación"): foco automático, Enter guarda, Esc cancela;
  valida nombre vacío y el duplicado que responde el backend.
- Tabla con Motivo / Observación / Estado / Acciones; "Editar" y menú ⋮ (Desactivar / Reactivar,
  se cierra con Esc o clic fuera). Interruptor "Ver inactivos" (apagado por defecto).
- Resumen "8 tipos · N motivos activos". La tarjeta Operación de Configuración abre esta pantalla.

**Backend (regla nueva, con prueba).** `CambiarEstadoMotivo` ya no deja desactivar el **último motivo
activo de Disponibilidad** (`ErrorMotivo.ULTIMO_DE_DISPONIBILIDAD`, HTTP 409): sin él nadie podría
volver a habilitar un producto agotado (V23). Antes solo lo advertía un comentario. Prueba:
`no_deja_desactivar_el_ultimo_motivo_activo_de_disponibilidad`; los tests de motivos pasan (5 + 4 + 3).

**Se dejó fuera, a propósito.** "Usado N veces" por motivo: el backend no cuenta usos y habría que
consultar cada módulo que guarda motivos. Tampoco hay "Eliminar": un motivo puede tener historial;
solo se desactiva.

**Verificado en vivo** (admin): datos reales (3 tarjetas con motivos, 5 tipos vacíos compactos, sin
huecos); agregar un motivo con foco automático, Enter, "Exigir observación" y error de nombre vacío;
duplicado ("Ya existe un motivo activo con ese nombre en este tipo"); Esc cierra el formulario;
editar (precarga y "Guardar cambios"); desactivar con el menú ⋮, aparición con "Ver inactivos";
intentar desactivar el único motivo de Disponibilidad -> el backend responde 409 y la pantalla lo
explica. `npx tsc -b --force` sin errores.

**Dato de prueba que quedó:** el motivo "Prueba motivo editado" (tipo Anulación, inactivo). Un motivo
no se puede eliminar, así que hay que dejarlo inactivo.
