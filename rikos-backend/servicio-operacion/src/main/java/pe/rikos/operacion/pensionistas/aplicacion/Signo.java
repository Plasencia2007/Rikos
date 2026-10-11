package pe.rikos.operacion.pensionistas.aplicacion;

import java.util.Arrays;
import java.util.Optional;

/** El sentido de un movimiento: cargo (el pensionista debe mas) o credito (debe menos / tiene a favor). */
public enum Signo {

    CARGO("cargo"),
    CREDITO("credito");

    private final String codigo;

    Signo(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static Optional<Signo> desde(String codigo) {
        return Arrays.stream(values()).filter(s -> s.codigo.equals(codigo)).findFirst();
    }
}
