package pe.rikos.operacion.venta.aplicacion;

/**
 * Lo que se pide para crear o editar un salon. Descripcion, tamano del plano y color de piso son
 * opcionales: sin ellos valen los de fabrica (1200 x 800 px, piso beige).
 */
public record DatosSalon(String nombre, String descripcion, String icono, String color, Integer anchoPlano,
                         Integer altoPlano, String colorPiso) {
}
