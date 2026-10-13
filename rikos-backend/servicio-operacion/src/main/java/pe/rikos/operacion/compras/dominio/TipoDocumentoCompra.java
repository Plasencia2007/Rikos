package pe.rikos.operacion.compras.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoDocumentoCompra {
    FACTURA("factura"),
    BOLETA("boleta"),
    TICKET("ticket"),
    SIN_DOCUMENTO("sin_documento");

    private final String codigo;

    TipoDocumentoCompra(String codigo) {
        this.codigo = codigo;
    }

    @JsonValue
    public String getCodigo() {
        return codigo;
    }

    @JsonCreator
    public static TipoDocumentoCompra desdeCodigo(String codigo) {
        if (codigo == null) return null;
        for (TipoDocumentoCompra t : values()) {
            if (t.codigo.equalsIgnoreCase(codigo) || t.name().equalsIgnoreCase(codigo)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Tipo de documento no válido: " + codigo);
    }
}
