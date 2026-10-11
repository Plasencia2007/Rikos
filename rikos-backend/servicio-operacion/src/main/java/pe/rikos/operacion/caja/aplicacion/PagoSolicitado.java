package pe.rikos.operacion.caja.aplicacion;

/**
 * Un pago que pide el cajero: el medio (efectivo, yape, plin o tarjeta), el monto en centimos y el numero de operacion
 * (opcional). En efectivo el monto es lo RECIBIDO: el sistema calcula el vuelto.
 */
public record PagoSolicitado(String medio, long monto, String referencia) {
}
