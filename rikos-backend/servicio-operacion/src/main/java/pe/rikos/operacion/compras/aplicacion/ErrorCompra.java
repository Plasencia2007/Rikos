package pe.rikos.operacion.compras.aplicacion;

public enum ErrorCompra {
    LOCAL_NO_ENCONTRADO("Local no encontrado"),
    LOCAL_INACTIVO("El local está inactivo"),
    PROVEEDOR_NO_ENCONTRADO("Proveedor no encontrado"),
    PROVEEDOR_INACTIVO("El proveedor está inactivo"),
    TIPO_DOCUMENTO_REQUERIDO("Tipo de documento es requerido"),
    SERIE_NUMERO_REQUERIDOS("Serie y número son obligatorios para este tipo de comprobante"),
    FECHA_DOCUMENTO_REQUERIDA("Fecha del comprobante es obligatoria"),
    DETALLES_VACIOS("La compra debe contener al menos un producto"),
    COMPROBANTE_DUPLICADO("Ya existe un comprobante con la misma serie y número para este proveedor"),
    PRODUCTO_NO_ENCONTRADO("Producto no encontrado"),
    PRODUCTO_INACTIVO("El producto está inactivo"),
    CANTIDAD_INVALIDA("La cantidad debe ser mayor a cero"),
    PRECIO_INVALIDO("El precio unitario no puede ser negativo"),
    PRESENTACION_NO_PERTENECE("La presentación indicada no corresponde al producto"),
    COMPRA_NO_ENCONTRADA("Compra no encontrada"),
    ESTADO_INVALIDO("La compra no se encuentra en un estado que permita esta acción");

    private final String mensaje;

    ErrorCompra(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() {
        return mensaje;
    }
}
