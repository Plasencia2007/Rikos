package pe.rikos.operacion.pensionistas.aplicacion;

import java.util.Arrays;
import java.util.Optional;

/** Las tres comidas del dia: el plan tiene un precio por cada una. Los codigos son los de producto_servicio.servicio. */
public enum Comida {

    DESAYUNO("desayuno"),
    ALMUERZO("almuerzo"),
    CENA("cena");

    private final String codigo;

    Comida(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static Optional<Comida> desde(String codigo) {
        return Arrays.stream(values()).filter(c -> c.codigo.equals(codigo)).findFirst();
    }
}
