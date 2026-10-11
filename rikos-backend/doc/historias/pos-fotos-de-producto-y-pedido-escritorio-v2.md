# Fotos de producto y pedido en escritorio, versión 2 (2026-10-05)

Pedido del usuario: "haz este diseño igualito" (referencia con fotos de los platos) y "sí, ponle fotos". Diseño aprobado en el canvas
(`PosPedidoSola` y `PosPedidoUnida`, escritorio).

## Fotos de producto
- **Backend (V39):** columna `producto.imagen TEXT` con la foto como *data URL* (JPEG chico). Se eligió así a propósito: el catálogo
  es corto y no hace falta un servicio de archivos ni rutas sin autenticación (una `<img>` no manda el token). `Producto.imagen`,
  `ProductoRespuesta.imagen`, `PUT /productos/{id}/imagen` (`{imagen}`; `null` la quita; 422 `IMAGEN_INVALIDA` si no es `data:image/…`
  o pasa de ~400 000 caracteres), y `ProductoOfrecido.imagen` en la carta del pedido (`ConsultaPedidos`). Test:
  `OfertaDeLaCartaTest.la_carta_trae_la_foto_del_producto_y_nula_si_no_tiene` en verde.
- **Subir la foto:** en el formulario de producto (panel, al editar uno que ya existe) hay una tarjeta "Foto" con "Subir foto",
  "Cambiar foto" y "Quitar" (`FotoDelProducto.tsx`). El navegador la reduce antes de enviarla (lado mayor 480 px, JPEG 82 %).
  Para un producto nuevo hay que crearlo primero.
- **Dónde se ve:** tarjetas del catálogo (mosaico y lista) y miniatura de cada línea de la orden. Sin foto sigue el icono.
- Los menús ("Menú del día", "Menú extra") no tienen foto: no son un producto de la carta.
- **Datos de prueba:** las 6 fotos de la carta de desarrollo son **círculos de colores generados** para probar, no fotos reales.
  Hay que reemplazarlas desde el panel.

## Pedido en escritorio (≥ 901 px)
- Barra lateral **contraída a iconos** mientras hay un pedido abierto (`apps/pos/index.tsx`).
- Cabecera: "← Mesas" verde suave, título con punto de estado y "Cambiar mesa" con borde verde.
- Personas con icono delante del nombre. Buscador y selector Mosaico/Lista **en la misma fila**, pestañas debajo, aviso de menú y
  la lista. Orden de la mesa con icono de cubiertos y botón "Comandar" con icono de envío (verde; el naranja de la referencia
  se descartó por el tema del proyecto).
- Filas fijas de la grilla de la pantalla: con un aviso entre la cabecera y las personas, estas ya no se van al fondo.
- Solo CSS (`pos.css`) más los cambios chicos de `OrdenDeLaMesa.tsx`, `CatalogoDelPedido.tsx` y `PantallaPedido.tsx`.

## Verificación
`tsc -b --force` y `lint` sin errores. En el navegador a 1280x800: fotos en las tarjetas, 3 miniaturas en la orden, orden a todo el
alto, personas arriba. No se probó la subida con el selector de archivos real (las fotos de prueba se pusieron con la API).
Una vez el backend en marcha falló con `NoClassDefFoundError` porque otra sesión recompiló mientras corría: se reinició y siguió bien.
