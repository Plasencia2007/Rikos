package pe.rikos.operacion.venta.aplicacion;

import java.util.Arrays;
import java.util.Optional;

/** Por donde llega el pedido. Recojo programado y delivery llegan en el sprint 12 sobre el mismo pedido. */
public enum Canal {

    MESA("mesa"),
    PARA_LLEVAR("para_llevar");

    private final String codigo;

    Canal(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static Optional<Canal> desde(String codigo) {
        return Arrays.stream(values()).filter(c -> c.codigo.equals(codigo)).findFirst();
    }
}
