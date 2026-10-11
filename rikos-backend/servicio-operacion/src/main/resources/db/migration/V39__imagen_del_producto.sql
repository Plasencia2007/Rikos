-- Foto del producto para el POS (pantalla del pedido). Se guarda como "data URL" (JPEG chico, ya reducido en el navegador):
-- el catalogo es corto y asi no hace falta un servicio de archivos ni exponer rutas sin autenticacion.
ALTER TABLE producto ADD COLUMN imagen TEXT;
