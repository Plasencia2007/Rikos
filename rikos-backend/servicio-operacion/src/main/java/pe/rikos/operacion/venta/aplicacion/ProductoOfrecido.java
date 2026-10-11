package pe.rikos.operacion.venta.aplicacion;

/**
 * Un producto de la carta: precio en centimos. grupo: "entrada", "segundo" o "extra" segun la categoria RAIZ del producto
 * (nunca por su nombre); null para el resto (bebidas, postres...). categoria: el nombre de su subcategoria. imagen: su foto
 * (data URL) o null.
 */
public record ProductoOfrecido(String id, String nombre, long precio, String grupo, String categoria, String imagen) {
}
