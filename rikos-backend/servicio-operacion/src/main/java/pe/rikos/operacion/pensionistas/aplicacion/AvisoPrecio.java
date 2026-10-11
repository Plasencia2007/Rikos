package pe.rikos.operacion.pensionistas.aplicacion;

/** Cosas que la caja debe mostrar porque falta un dato: el motor no las resuelve inventando un precio. */
public enum AvisoPrecio {
    /** El producto no tiene precio normal todavia. */
    SIN_PRECIO_NORMAL,
    /** El plan del pensionista no tiene precio para esa comida en esa fecha. */
    PLAN_SIN_PRECIO,
    /** Es para llevar pero el duenio no ha puesto el monto del taper: no se cobra taper. */
    TAPER_SIN_MONTO
}
