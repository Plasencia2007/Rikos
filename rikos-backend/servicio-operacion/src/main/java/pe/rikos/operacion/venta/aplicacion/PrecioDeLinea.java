package pe.rikos.operacion.venta.aplicacion;

/** El resultado de fijar el precio de una linea: por unidad y en centimos. tarifaOrigen: publico | plan | extra | manual. */
public record PrecioDeLinea(long precio, long taperUnitario, String tarifaOrigen, String planId, boolean segundoDelDia) {
}
