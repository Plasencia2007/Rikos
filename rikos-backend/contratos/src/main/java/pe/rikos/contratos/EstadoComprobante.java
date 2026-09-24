package pe.rikos.contratos;

/**
 * Estado fiscal del comprobante. Los tres ultimos corresponden
 * a los codigos observados en el proveedor: 01, 05 y 09.
 */
public enum EstadoComprobante {
    PENDIENTE,
    ENVIADO,
    REGISTRADO,
    ACEPTADO,
    RECHAZADO
}
