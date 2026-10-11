package pe.rikos.operacion.pensionistas.aplicacion;

import java.util.Arrays;
import java.util.Optional;

/**
 * A que precio va el segundo consumo de la misma comida en el dia, para un
 * pensionista. Por defecto, el de su plan (decision del usuario, sprint 5).
 */
public enum ReglaSegundoConsumo {

    PRECIO_DEL_PLAN("precio_del_plan"),
    PRECIO_NORMAL("precio_normal");

    private final String codigo;

    ReglaSegundoConsumo(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static Optional<ReglaSegundoConsumo> desde(String codigo) {
        return Arrays.stream(values()).filter(r -> r.codigo.equals(codigo)).findFirst();
    }
}
