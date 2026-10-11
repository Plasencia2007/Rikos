<!-- Extraído del Product Backlog v2.0 (RIKOS-product-backlog.md, 2026-09-11), líneas 3013-3062. Distribuido por paso del método el 2026-09-29, ver docs/METODO-spec-driven.md. -->

# 7. Reglas de diseño transversales

Aplican a todas las pantallas y salen de las reglas de negocio, no del gusto.

## 7.1 El POS se usa de pie

El mozo opera con una mano, con prisa y con el cliente esperando. Eso no es una observación estética: define medidas.

- **Objetivo táctil mínimo de 56 px.** Por debajo, el dedo falla y el mozo repite el toque.
- **En pantalla de celular, las acciones van en la mitad inferior.** El pulgar no llega arriba.
- **Contraste alto.** Hay reflejo y luz de sala.
- **Menos toques siempre gana.** El PIN entra solo al cuarto dígito, sin botón de confirmar.

## 7.2 Toda acción sensible pide motivo

Descuentos, anulaciones, ajustes de inventario y reactivación de productos agotados exigen motivo antes de confirmar (RN-004, RN-072). La pantalla no permite continuar sin él, y el motivo sale de una lista configurable, no de un campo libre.

## 7.3 Los permisos ocultan, el backend decide

Un usuario sin permiso no debe ver el botón de anular, descontar ni los costos (RN-012). Eso se resuelve en un solo lugar del frontend.

Ocultar es comodidad, no seguridad: el backend valida igual. Pero si la comprobación se reparte por las pantallas, tarde o temprano una se olvida.

## 7.4 Alertar sin bloquear

Tres reglas del negocio dicen explícitamente que el sistema avisa pero deja continuar:

- El límite de crédito del pensionista (RN-017)
- El stock negativo (RN-050)
- La repetición de un segundo en el menú semanal (RN-041)

Ninguna de esas tres debe implementarse como un error que impide seguir. Son avisos visibles que no cortan la operación.

## 7.5 Cada pantalla tiene cuatro estados

Cargando, con datos, vacía y con error. La pantalla vacía es la que más se olvida y la que más se ve el primer día de uso, cuando todavía no hay nada registrado: merece un texto que explique qué hacer, no un espacio en blanco.

## 7.6 El POS avisa cuando no hay conexión

Un indicador discreto pero visible cuando el dispositivo perdió la red del local. No una alerta que interrumpa: el sistema sigue funcionando, y el mozo solo necesita saberlo.

## 7.7 El estado compartido se actualiza solo

Mesas, pedidos y comandas cambian en la pantalla de todos los dispositivos del local sin recargar (DT-018). Si el mozo A agrega un plato, la tablet del mozo B y la de caja se enteran.

---


---

