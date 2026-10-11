package pe.rikos.operacion.pensionistas.aplicacion;

public enum ErrorParametro {
    /** La clave no existe. */
    NO_ENCONTRADO,
    /** El valor no corresponde al parametro (taper: centimos > 0 o vacio; regla: una de las dos). */
    VALOR_INVALIDO
}
