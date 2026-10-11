package pe.rikos.operacion.venta.aplicacion;

/** Salones y su plano. */
public enum ErrorSalon {
    SIN_LOCAL_ACTIVO,
    /** No existe, o es de otro local (no se distingue a proposito). */
    NO_ENCONTRADO,
    NOMBRE_INVALIDO,
    NOMBRE_DUPLICADO,
    /** Icono, color, color de piso o medidas del plano fuera de lo permitido. */
    VALOR_INVALIDO,
    /** Un salon con mesas no se elimina: se desactiva (las mesas conservan su historial). */
    TIENE_MESAS,
    /** Al guardar el plano llego una mesa que no es de ese salon. */
    MESA_NO_ES_DEL_SALON
}
