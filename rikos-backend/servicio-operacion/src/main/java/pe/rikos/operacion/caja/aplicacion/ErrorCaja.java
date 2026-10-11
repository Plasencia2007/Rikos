package pe.rikos.operacion.caja.aplicacion;

/** Errores esperados de las cajas y los turnos (nunca se lanzan como excepcion). */
public enum ErrorCaja {
    SIN_LOCAL_ACTIVO,
    CAJA_NO_ENCONTRADA,
    CAJA_INACTIVA,
    CAJA_CON_TURNO_ABIERTO,
    CAJERO_CON_TURNO_ABIERTO,
    FONDO_INVALIDO,
    NOMBRE_INVALIDO,
    NOMBRE_REPETIDO,
    /** El cajero no tiene un turno abierto en este local. */
    SIN_TURNO_ABIERTO,
    /** El efectivo contado falta o es negativo. */
    CONTADO_INVALIDO,
    OBSERVACION_LARGA,
    /** El codigo de la caja pasa de 20 caracteres. */
    CODIGO_INVALIDO,
    CODIGO_REPETIDO,
    /** La caja principal no se desactiva ni se borra: primero se elige otra como principal. */
    CAJA_PRINCIPAL,
    /** La caja ya tuvo turnos: no se borra, se desactiva. */
    TIENE_HISTORIAL,
    NOTA_LARGA
}
