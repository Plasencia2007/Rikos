package pe.rikos.operacion.pensionistas.aplicacion;

/**
 * Errores posibles al emitir el pre-ticket de autoservicio en el kiosco (US-019).
 */
public enum ErrorPreTicket {
    LOCAL_NO_ENCONTRADO,
    LOCAL_INACTIVO,
    DNI_INVALIDO,
    CREDENCIALES_INVALIDAS,
    PENSIONISTA_BLOQUEADO,
    PENSIONISTA_INACTIVO,
    FUERA_DE_HORARIO,
    SERVICIO_MANUAL_NO_PERMITIDO
}
