package pe.rikos.operacion.pensionistas.aplicacion;

/** US-018: cuenta corriente. */
public enum ErrorCuenta {
    /** El pensionista (o su cuenta) no existe. */
    NO_ENCONTRADO,
    /** El saldo inicial y los ajustes se registran en el local activo de quien los hace. */
    SIN_LOCAL_ACTIVO,
    /** Todo importe es un entero de centimos mayor que cero; el sentido lo da el signo. */
    IMPORTE_INVALIDO,
    /** Un solo saldo inicial por cuenta: un error se corrige con un ajuste. */
    SALDO_INICIAL_YA_REGISTRADO,
    /** La fecha de corte del saldo inicial no puede ser futura. */
    FECHA_FUTURA,
    MOTIVO_OBLIGATORIO,
    /** No existe, esta desactivado o no es un motivo de ajuste de cuenta. */
    MOTIVO_INVALIDO,
    OBSERVACION_OBLIGATORIA,
    /** RN-034: un pensionista inactivo no consume (sus abonos y ajustes si se aceptan). */
    PENSIONISTA_INACTIVO,
    /** Un tope fijado a mano es mayor que cero. */
    LIMITE_INVALIDO
}
