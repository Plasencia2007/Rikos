package pe.rikos.operacion.pensionistas.aplicacion;

/** US-017: planes tarifarios. */
public enum ErrorPlan {
    NO_ENCONTRADO,
    NOMBRE_OBLIGATORIO,
    NOMBRE_DUPLICADO,
    /** Un precio, si se pone, es mayor que cero (centimos). */
    PRECIO_INVALIDO,
    /** La vigencia no puede empezar en el pasado ni antes de la ultima version del plan. */
    VIGENCIA_INVALIDA,
    /** Los precios son los mismos que ya tiene el plan: no hay nada que versionar. */
    SIN_CAMBIOS
}
