package pe.rikos.operacion.catalogo.aplicacion;

/** productoId: el ingrediente que provoco el fallo, o null si el fallo es de la receta entera. */
public record FalloReceta(ErrorReceta error, String productoId) {

    public static FalloReceta de(ErrorReceta error) {
        return new FalloReceta(error, null);
    }

    public static FalloReceta en(ErrorReceta error, String productoId) {
        return new FalloReceta(error, productoId);
    }
}
