package pe.rikos.operacion.pensionistas.aplicacion;

import java.util.Arrays;
import java.util.Optional;

/** Los parametros globales que existen (los siembra V26). Agregar uno = nueva migracion + un valor aqui. */
public enum ClaveParametro {

    /** Centimos por menu, para llevar / recojo / delivery. Sin valor = sin monto: no se cobra taper. */
    TAPER_PRECIO("taper_precio"),
    /** "precio_del_plan" (por defecto) o "precio_normal". Siempre tiene valor. */
    SEGUNDO_CONSUMO_REGLA("segundo_consumo_regla");

    private final String codigo;

    ClaveParametro(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static Optional<ClaveParametro> desde(String codigo) {
        return Arrays.stream(values()).filter(c -> c.codigo.equals(codigo)).findFirst();
    }
}
