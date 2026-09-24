package pe.rikos.contratos;

/**
 * Tipos de comprobante que se transmiten a SUNAT a traves del proveedor.
 * Nota de venta y ticket no aparecen aqui: son documentos internos
 * y no pasan por servicio-facturacion (ver DT-005).
 */
public enum TipoComprobante {
    BOLETA,
    FACTURA,
    NOTA_CREDITO
}
