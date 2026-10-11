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
    NOMBRE_REPETIDO
}
