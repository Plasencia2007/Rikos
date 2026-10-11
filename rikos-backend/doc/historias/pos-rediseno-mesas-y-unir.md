# Rediseño del POS: mapa de mesas como la referencia y "Unir mesas" (2026-10-01)

Especificación: `docs/producto/ui-ux/03-pos-rediseno.md` (decisiones de Mesas y Unir). Diseño aprobado por el usuario en el canvas (`PosMesas`, `PosMesasUnir`, `PosMesasUnida`) a partir de una imagen de referencia.

## Qué se pidió
1. Dejar el mapa de mesas "igualito" al modelo de la imagen.
2. Unir mesas desde el mapa. 3. (Durante la sesión) quitar el botón "Pedido para llevar".

## Qué se construyó (solo frontend; el backend de unir/separar ya existía en US-029)
- `features/venta/MapaDeMesas.tsx`: barra de acciones con "Unir mesas"; encabezado del salón con "n de m ocupadas · %" y "n libres", **Ajustar vista y zoom** (50 % a 160 %); plano encuadrado sobre las mesas, con margen y centrado (ya no se dibuja el lienzo entero del salón); leyenda centrada con separadores.
- **Modo "Unir mesas"**: guía de dos pasos, principal con etiqueta "Principal", las que se unen con "Se une", las no elegibles atenuadas, línea punteada entre ellas, y pie "Principal: X · Se unen: Y" con Cancelar y "Unir N mesas". Llama a `POST /mesas/{id}/unir` por cada mesa y avisa con `useAvisos` ("Mesa unida" o el error del servidor).
- **Mesa unida**: al tocarla, diálogo "Unida a X" con "Separar mesa" (`POST /mesas/{id}/separar`) y "Ver pedido de X" (el pedido de su principal).
- Permisos: los botones de unir y separar aparecen con `venta.modificar`.
- Se quitó "Pedido para llevar" del mapa y su diálogo.
- Responsive: en pantallas angostas el mapa son fichas y el modo unir funciona sobre ellas (selección con contorno); el zoom se oculta por debajo de 900 px.
- `estilos/mapa.css`.

## Cómo se verificó
- `npx tsc -b --force` limpio y lint sin errores (5 advertencias antiguas).
- Navegador a 1280 px con datos reales: Unir mesas -> Terraza 1 (principal) -> T4; pie "Principal: Terraza 1 · Se unen: T4 · Unir 2 mesas"; confirmar da el aviso "Mesa unida" y T4 pasa a "Unida"; al tocarla sale "Unida a Terraza 1" con "Ver pedido de Terraza 1" y "Separar mesa"; separar la deja "Libre". Sin desplazamiento horizontal en el plano.
- Navegador a 375 px: fichas, modo unir con selección y pie, botones de 44 px, sin desborde de página.

## Decisiones y límites
- "Ajustar vista" encuadra las mesas, no el lienzo: así se ven grandes y centradas como en la referencia, sea cual sea el tamaño guardado del salón.
- Unir varias mesas es una llamada por mesa (ver `docs/pendientes/README.md`).
- **Sin "Pedido para llevar" en el mapa hasta que exista Pedido rápido (entrega 4)**: anotado como pendiente.

## Liberar las mesas de desarrollo (2026-10-01)
El usuario pidió dejar todas las mesas libres. Al no haber forma de cerrar un pedido, primero se intentó borrarlos por SQL, pero el sistema de permisos bloqueó el borrado masivo y el usuario prefirió **no borrar nada**. Se pidió a la sesión de pedidos un "Terminar pedido" (cierre sin cobro, V38, `POST /pedidos/{id}/terminar`) y, una vez hecho, se **terminaron desde el POS los 6 pedidos abiertos** (Mesa 1, Terraza 2, Ventana, Para llevar, Salón y Terraza 1). Resultado verificado: las 8 mesas "Libre", Inicio en 0 / 8 y 0 pedidos en curso, y el Historial lista los 6 como "Cerrada". Ningún dato se borró.

## Diálogo "Unir mesas" (2026-10-01)
El usuario pidió que al confirmar la unión se vea como su referencia: un diálogo con "n mesas · Capacidad total: X personas", la lista "Mesas a unir" con la **principal marcada** (la primera elegida), "Total de comensales" con − y +, "Nombre del cliente (opcional)" y Cancelar / Unir.
- `DialogoUnirMesas` en `MapaDeMesas.tsx`. Si la principal está **libre** (y estamos en el POS), tras unir las mesas **se abre su pedido** con el total de comensales (por defecto la capacidad sumada, máximo 30) y el cliente, y se entra a él; si la principal **ya tiene pedido**, el diálogo solo une las mesas a ese pedido y no pide comensales ni cliente.
- Si abrir el pedido falla después de unir (p. ej. "fuera de horario", pide el servicio), al reintentar no se vuelve a unir lo ya unido.
- Se mantuvo el vocabulario "Unir" del proyecto (la referencia dice "Fusionar").
- Verificado en navegador: Mesa 1 + Salón -> diálogo con capacidad 8 -> pide servicio -> pedido 0002 con 8 comensales (Persona 1 a 8); luego se terminó el pedido con "Terminar pedido" y las mesas quedaron libres (no se borró nada). `tsc` y lint sin errores.

## Unir más de 2 mesas, mesas pegadas y separar (2026-10-01)
El usuario reportó: (1) no le deja unir más de 2 mesas, (2) al unirse, las mesas deberían verse juntas, (3) "Separar" debe separar.
- **Más de 2 mesas:** no se pudo reproducir un límite: en el navegador se unieron 4 mesas (Mesa 1 con Salón, Ventana y Terraza 1; 16 comensales) y 3 libres a Ventana. La regla del backend es que solo se unen mesas **libres**: si el resto de mesas tiene pedido abierto, no se pueden elegir (en pruebas había mesas ocupadas por otra sesión). Ahora la guía lo dice: "las ocupadas no se pueden unir", y el aviso "Solo se pueden unir mesas libres" ya salía al tocarlas. **Pendiente de confirmar con el usuario qué mesas intentaba unir.**
- **Mesas juntas:** `acoplar()` en `MapaDeMesas.tsx`: la principal se queda en su sitio y cada unida se pega a ella en el primer lado libre (derecha, abajo, izquierda, arriba) **sin pisar a otra mesa**; si no hay sitio, se queda donde estaba. Se ven como una sola mesa grande (sin sillas entre ellas, sin bordes redondeados). Al separar, vuelven a su lugar guardado. Verificado: sin solapes entre mesas.
- **Separar:** el diálogo de una **mesa principal** con unidas muestra "Mesas unidas a esta: …" y **"Separar todas las mesas"** (una llamada a `/separar` por mesa); el de una mesa unida mantiene "Separar mesa". Una principal libre con unidas también abre ese diálogo. Verificado: separó Terraza 1 y Terraza 2 y volvieron a su sitio.
- Datos de prueba: se terminó con "Terminar pedido" el pedido que abrió la prueba (sin borrar nada).

## Mesas unidas como UNA sola mesa y unión "todo o nada" (2026-10-01)
El usuario mostró que el otro sistema junta las mesas unidas en **una sola mesa** y que al separarlas vuelven a su lugar; y reportó un fallo: al unir 3 mesas fuera de horario salía el aviso del servicio, pero **las mesas ya quedaban unidas** y una aparecía con pedido.
- **Una sola mesa:** `agrupar()` en `MapaDeMesas.tsx` sustituye al "acoplar" anterior. La mesa principal se dibuja sola, con el nombre "Principal +N", borde punteado, color de unida y la **capacidad sumada** (más sillas); las unidas **no se dibujan**. En el celular (fichas) igual. Al separar, cada mesa reaparece en su lugar guardado. Tocar la mesa unida abre el diálogo con "Separar todas las mesas" y "Ver pedido".
- **Todo o nada:** `DialogoUnirMesas` ahora **primero abre el pedido de la principal** (así el servicio fuera de horario se pide ANTES de unir nada) y **después une** las mesas; si una unión falla, se **deshace** lo hecho (separa las ya unidas y termina el pedido recién abierto). Antes se unía primero y el pedido fallaba después.
- Verificado en navegador: 3 mesas fuera de horario -> pide el servicio y **ninguna mesa queda unida**; al elegirlo se une y se ve "Mesa 1 +2 · 10 sillas"; "Separar todas" las devuelve a su lugar. Las mesas con pedido de las pruebas se cerraron con "Terminar pedido" (sin borrar) para poder seguir probando.

## Ajustes tras la prueba del usuario con 3 mesas (2026-10-01)
El usuario unió 3 mesas (T4 + Terraza 2 + Terraza 1) y preguntó "por qué sale así" con capturas. Lo que se vio mal y se corrigió:
- **La mesa fusionada era un cuadrado chico con sillas rojas amontonadas** (las sillas conservaban el color de "ocupada" y se repartían en una línea). Ahora es **una mesa grande** (hasta 3 mesas de ancho y las filas necesarias, centrada donde estaba la principal), con **sillas del color de "unida"** repartidas por los lados largos. El encuadre del plano cuenta el tamaño real de la mesa fusionada, así que no se corta.
- **"Estamos fuera de horario" aparecía como error rojo** dentro del diálogo. No es un error: ahora es un **aviso amarillo** ("elige el servicio y vuelve a pulsar el botón") junto al selector de servicio, en "Abrir mesa", "Unir mesas" y el diálogo de mesa.
- **Nombres inconsistentes:** el mapa decía "T4" y el pedido "Mesa 7". Ahora el diálogo y el título del pedido usan el mismo nombre que el mapa (`nombreDeMesa`: el nombre si existe, si no "Mesa N").
Verificado en navegador (3 mesas libres: aviso amarillo, unión, mesa grande dentro del plano sin desplazamiento, "Separar todas" las devuelve y "Terminar pedido" las libera). `tsc` y lint sin errores.

## Al separar, la principal vuelve a quedar libre si su pedido está vacío (2026-10-01)
El usuario unió 3 mesas, entró al pedido, volvió, **separó y una mesa quedó ocupada**: era la principal, que conservaba el pedido que se abrió al unir aunque nadie hubiera tomado nada.
- `cerrarPedidoVacio()` en `MapaDeMesas.tsx`: al "Separar todas las mesas" (o al separar la última mesa unida), si el pedido de la principal **no tiene ninguna línea**, se **termina** (cierre sin cobro, no se borra) y la mesa queda **libre**; si ya tiene productos, los **conserva** y solo se liberan las demás. El diálogo lo explica antes de separar.
- Verificado en navegador: unir Mesa 1, Salón y Terraza 1 (pedido 0013 con 14 comensales, vacío) -> separar todas -> las tres quedan **Libre**.
- Nota: el pedido vacío cerrado aparece en el Historial como "Cerrada" (queda rastro, no se borra).

## Regla "la mesa se ocupa al comandar" en el mapa (2026-10-01)
Regla decidida por el usuario (`docs/producto/plan/sprint-06-vender-en-mesa.md` §3.2, "Cambio de regla") e implementada en el backend por la sesión de pedidos (campo `tomandoPedido` en `GET /mesas`, `POST /pedidos` idempotente sobre un pedido abierto, `TIENE_PEDIDO_ABIERTO`). En el mapa:
- Una mesa **libre con pedido abierto sin comandar** se dibuja libre con la marca **"Tomando pedido"** (en el plano y en las fichas del celular, con el tiempo "Tomando · N min") y un ítem en la leyenda. Ya no cuenta como ocupada en "n de m ocupadas".
- Al tocarla, el diálogo ofrece **"Continuar pedido"** (entra al mismo pedido) en lugar de "Abrir mesa", y **no ofrece bloquearla**. El mensaje `TIENE_PEDIDO_ABIERTO` sale en español.
- **Unir**: una mesa con pedido abierto sin comandar no se elige como secundaria (se atenúa); como principal sí, y entonces el diálogo no pide comensales (se une al pedido existente).
- Verificado en navegador: abrir Salón sin comandar -> en el mapa queda "Libre" con la marca y "Continuar pedido"; se terminó el pedido de la prueba y la marca desapareció. `tsc` y lint sin errores.
- Sigue vigente "Separar todas": si el pedido de la principal está vacío se cierra (así no queda una marca "tomando pedido" suelta).

## Mesas del tamaño del diseño y mesa fusionada sin pisar a otras (2026-10-01)
El usuario pidió las mesas "un poco más pequeñas, como el mockup".
- "Ajustar vista" **ya no agranda** el plano por encima de su tamaño real (antes llegaba al 130 %, mesas de ~110 px): ahora el máximo es 100 % (mesa de 84 px), como el diseño; solo se reduce si no cabe en el ancho. Los botones − y + siguen acercando y alejando.
- La **mesa fusionada** crece primero centrada, y si así pisaría a otra mesa prueba creciendo hacia un lado u otro (derecha, izquierda, arriba, abajo); si no cabe, queda del tamaño de la principal. Verificado: sin solapes (Mesa 1 +2 de 230 × 77 px junto a Salón de 77 px).

## Panel del plano más alto (2026-10-01)
El usuario pidió que el panel del plano use el espacio hacia abajo. `.mapa-plano` ahora ocupa **todo el alto disponible** (`min-height: max(contenido, 320 px, 100vh − 380 px)`) y **centra las mesas** en vertical y en horizontal; no hay desplazamiento de página (a 1280 × 800: plano de 420 px de alto). El alto del contenido pasa por una variable CSS (`--alto-contenido`) para que no lo anule un estilo en línea (se vio en la primera prueba: el plano medía 210 px).

## Tocar una mesa formada lleva directo al pedido (2026-10-01)
El usuario pidió que al tocar una mesa ya formada (unida) se entre **directo a su pedido**, sin diálogo, con "Separar" dentro de la pantalla del pedido (como en el otro sistema, que titula "Terraza 4 + Terraza 10").
- `alElegirMesa()` en `MapaDeMesas.tsx`: si la mesa tiene unidas y estamos en el POS (con `onAbrirPedido`), se abre el pedido de la principal con el título **"A + B"** (`nombreDeMesa` de cada mesa). Si no se puede leer el pedido, se cae al diálogo de siempre. En el panel del administrador (sin pantalla de pedido) se mantiene el diálogo, donde está "Separar todas las mesas".
- Verificado en navegador: la mesa "Terraza 2 +1" abre el pedido 0020 titulado "Terraza 2 + Terraza 1" sin diálogo.
- **Hecho por la sesión de pedidos (probado el mismo día):** botón **"Separar mesas"** dentro del pedido cuando tiene mesas unidas, con confirmación; vuelve a titular con solo la principal y cierra el pedido si queda vacío. Pendiente hasta que lo implemente; mientras tanto se puede separar desde el diálogo "Unir mesa" del pedido o desde el mapa del administrador.

## Mesas en celular: plano compacto, lista y hoja inferior (2026-10-01)

Diseño aprobado en el canvas (`PosMesasMovil`, `PosMesasMovilLista`, `PosMesasMovilHoja`). Reemplaza las fichas de color
del mapa angosto (< 640 px de contenedor), que eran tarjetas iguales sin relación con el salón.

- **Plano compacto** (vista por defecto): las mesas se acomodan en una cuadrícula de 3 columnas en el orden del salón
  (por fila y por posición), como círculos de 78 px (cuadradas con esquinas redondeadas) con el número grande, el prefijo
  chico ("Terraza") y debajo el dato (tiempo, "Cobrar", "Bloqueada", "Tomando"). Marca VIP. La mesa unida es una píldora
  punteada que ocupa dos columnas. Un nombre sin número ("Salón") se muestra en dos líneas. La posición exacta del salón
  queda solo en escritorio.
- **Lista**: filas compactas (círculo con número, nombre, sillas, estado y "hace N min" o "Toca para abrir"), con lo que
  está en servicio primero (ocupadas, por cobrar, libres, bloqueadas).
- **Selector Plano / Lista** (44 px de alto) y, en la tarjeta del salón, una **barra de estado** (ocupadas, por cobrar,
  bloqueadas) y sin controles de zoom en celular. Modo "Unir mesas" funciona en ambas vistas.
- **Hoja inferior**: en celular (≤ 600 px) los diálogos del POS (mesa, abrir mesa, confirmaciones) salen desde abajo con
  esquinas redondeadas y una manija, en vez de una caja al centro (`body:has(.pos-raiz)` porque los modales van al `body`).
- Archivos: `MapaDeMesas.tsx` (rama compacta de `PlanoDelSalon`), `mapa.css`, `pos.css`. Sin backend nuevo.
- Verificado a 390x844 en el navegador: 6 círculos en el plano, 6 filas en la lista, hoja de 390 px pegada al borde inferior,
  sin desplazamiento horizontal. `tsc -b --force` y `lint` sin errores. No se probó con mesas ocupadas, por cobrar ni unidas
  (el local de desarrollo solo tenía mesas libres).
