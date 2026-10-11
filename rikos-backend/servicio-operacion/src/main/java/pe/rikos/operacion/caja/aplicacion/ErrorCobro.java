package pe.rikos.operacion.caja.aplicacion;

/** Errores esperados del cobro (nunca se lanzan como excepcion). */
public enum ErrorCobro {
    SIN_LOCAL_ACTIVO,
    PEDIDO_NO_ENCONTRADO,
    /** El pedido ya esta cerrado: no hay nada que cobrar. */
    PEDIDO_CERRADO,
    /** El cajero no tiene un turno abierto en este local: sin turno no hay a que caja asociar el cobro. */
    SIN_TURNO_ABIERTO,
    SIN_LINEAS,
    LINEA_NO_ENCONTRADA,
    LINEA_YA_COBRADA,
    LINEA_ANULADA,
    /** Solo se cobra lo que ya salio a cocina. */
    LINEA_SIN_COMANDAR,
    /** El medio no es efectivo, yape, plin ni tarjeta. */
    MEDIO_INVALIDO,
    MONTO_INVALIDO,
    REFERENCIA_INVALIDA,
    /** La suma de los pagos no cubre lo que se cobra en medios. */
    PAGO_INSUFICIENTE,
    /** Solo el efectivo puede dar vuelto: lo digital (y la suma de lo digital) no puede pasar del total. */
    PAGO_EXCEDE_EL_TOTAL,
    /** Todo lo que se cobra va a la cuenta de un pensionista: no hay nada que pagar en medios. */
    PAGO_SIN_NADA_QUE_PAGAR,
    /** RN-034: un pensionista desactivado no consume. */
    PENSIONISTA_INACTIVO
}
