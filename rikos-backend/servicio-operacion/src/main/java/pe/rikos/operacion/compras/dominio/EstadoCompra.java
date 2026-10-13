package pe.rikos.operacion.compras.dominio;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EstadoCompra {
    BORRADOR("borrador"),
    CONFIRMADA("confirmada"),
    ANULADA("anulada");

    private final String codigo;

    EstadoCompra(String codigo) {
        this.codigo = codigo;
    }

    @JsonValue
    public String getCodigo() {
        return codigo;
    }

    @JsonCreator
    public static EstadoCompra desdeCodigo(String codigo) {
        if (codigo == null) return null;
        for (EstadoCompra e : values()) {
            if (e.codigo.equalsIgnoreCase(codigo) || e.name().equalsIgnoreCase(codigo)) {
                return e;
            }
        }
        throw new IllegalArgumentException("Estado de compra no válido: " + codigo);
    }
}
