# Cajas registradoras (Configuración) y sesión de caja del POS

**Hecho el 2026-10-11** a pedido del usuario, que mostró como referencia la administración de "Cajas registradoras" de otro sistema: las cajas se **configuran desde Configuración** y el POS
solo elige entre las registradas al abrir la sesión. Es parte del sprint 7 (cajas y turnos).

## Qué se pidió
Que las cajas físicas se administren desde el panel (Configuración → Cajas registradoras), con código, caja principal y estado, y que abrir una sesión de caja en el POS sea un cuadro con
la caja registrada, el fondo y una nota, seguido de una pantalla de sesión activa con su resumen.

## Qué se construyó
- **Migración V46:** `caja.codigo` (único por local, sin distinguir mayúsculas) y `caja.principal` (a lo sumo una por local, índice único parcial); `turno_caja.nota_apertura`; evento de bitácora
  `caja_modificada`. Las cajas que ya existían reciben código CAJ-NN y la más antigua queda como principal; la sembrada "Caja 1" pasa a llamarse **"Caja Principal" (CAJ-01)**.
- **Casos de uso** (`caja/aplicacion`): `CrearCaja` (nombre y código; sin código se asigna el siguiente), `ActualizarCaja`, `CambiarEstadoCaja` (no desactiva la principal ni una con turno abierto),
  `DefinirCajaPrincipal` (una sola, siempre activa), `EliminarCaja` (solo si nunca tuvo turnos y no es la principal; con historial se desactiva) y `AbrirTurno` ahora recibe una nota opcional (hasta 250 caracteres).
- **API (permiso `caja.administrar`):** `POST /cajas` `{nombre, codigo?}`, `PUT /cajas/{id}`, `POST /cajas/{id}/principal`, `/activar`, `/desactivar` y `DELETE /cajas/{id}`. `GET /cajas` (caja.ver) devuelve ahora
  `codigo` y `principal`, con la principal primero. `POST /cajas/{id}/turnos` acepta `nota`. Errores nuevos: `CODIGO_REPETIDO`, `CODIGO_INVALIDO`, `CAJA_PRINCIPAL`, `TIENE_HISTORIAL`, `NOTA_LARGA`.
- **Panel de administración** (`/admin/configuracion/cajas`, tarjeta "Cajas registradoras" de Configuración, que antes decía "Próximamente"): totales (total, activas, abiertas ahora, inactivas), búsqueda y filtro por estado,
  tabla con nombre, código, insignia Principal, quién la tiene abierta, interruptores de principal y de estado, editar y borrar, y una nota "Cómo se usan las cajas".
- **POS, sesión de caja:** sin sesión, "Caja cerrada" con **Abrir sesión**; el cuadro pide la caja registrada (`nombre - código ★`, la principal viene elegida), el **fondo inicial** (siempre se escribe, puede ser cero) y **notas**
  (opcional). Con sesión: banner "Sesión activa" (caja, código, "Caja de <cajero>", nota, tiempo transcurrido, Cerrar caja), tarjetas (monto inicial, efectivo cobrado, monto esperado, cobros), "Resumen de métodos de pago",
  "Por cobrar" y "Transacciones recientes".

## Decisiones tomadas al ejecutar
- **El POS no crea cajas**: solo elige entre las registradas y activas que no tengan turno abierto.
- **Una sola caja principal por local**, la que viene elegida; no se desactiva ni se borra mientras sea la principal.
- **Una caja que ya tuvo turnos no se borra** (conserva el historial de cobros y cierres): se desactiva.
- **Se dejó fuera, por decisión del dueño/diseño:** el botón "Movimiento" (gastos, retiros e ingresos; lo prohíbe RN-069), la impresora por caja (depende de HT-10 y del sprint 11) y "Entrantes por aprobar" (abonos digitales, sprint 8).
- La caja se rotula con su nombre y debajo "Caja de <cajero>" mientras tiene sesión.

## Cómo se verificó
- `CajasRegistradorasTest` (14 pruebas) y 2 pruebas nuevas en `CajasYTurnosAccesoPorRolTest` (administradores sí, cajero no; códigos, principal, estado, borrado con y sin historial, nota de apertura). Las pruebas de caja existentes siguen en verde.
- En el navegador con el backend real: en Configuración se crearon "Caja Barra" (CAJ-BA) y "Caja Delivery" (CAJ-03), se cambió la principal y se desactivó y reactivó una; en el POS se abrió una sesión con el selector
  (`Caja Principal - CAJ-01 ★`), fondo S/ 80 y nota, se vio el banner y las tarjetas, y se cerró con el conteo. `tsc -b` y lint sin errores.
- Datos de desarrollo que quedaron: las cajas "Caja Barra" y "Caja Delivery", y un turno abierto y cerrado de prueba (un cierre no se puede borrar).

## Pendiente
- Impresora por caja y validación de IP (la tarjeta de Configuración lo mencionaba antes): depende de HT-10 y del sprint 11.
- El diseño definitivo de la sesión de caja (esta pantalla sigue siendo provisional).
