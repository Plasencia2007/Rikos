package pe.rikos.operacion.catalogo.aplicacion;

/** US-015: agotados por local. */
public enum ErrorDisponibilidad {
    /** El token no trae local activo: sin local no hay donde marcar. */
    SIN_LOCAL_ACTIVO,
    PRODUCTO_NO_ENCONTRADO,
    /** Solo lo que el mozo ofrece: productos vendibles y activos. */
    PRODUCTO_NO_VENDIBLE,
    YA_AGOTADO,
    NO_ESTA_AGOTADO,
    /** RN-066: la reactivacion registra motivo. */
    MOTIVO_OBLIGATORIO,
    /** El motivo no existe, no es de tipo "disponibilidad" o esta desactivado. */
    MOTIVO_INVALIDO,
    OBSERVACION_OBLIGATORIA
}
