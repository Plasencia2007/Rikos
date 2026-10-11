package pe.rikos.operacion.pensionistas.aplicacion;

/**
 * Que se esta cobrando. El motor no conoce productos: quien lo llama (la caja)
 * decide de que tipo es cada linea y le pasa el precio normal.
 */
public enum TipoLinea {

    /** El menu normal del almuerzo, o la comida fija de desayuno / cena de un pensionista. Lleva taper para llevar. */
    MENU_DEL_DIA,
    /** El menu extra (RN-023): precio completo para todos. Lleva taper para llevar. */
    MENU_EXTRA,
    /** Cualquier otra cosa: un huevo frito, una porcion adicional, un plato de carta. Precio normal, sin taper. */
    AGREGADO
}
