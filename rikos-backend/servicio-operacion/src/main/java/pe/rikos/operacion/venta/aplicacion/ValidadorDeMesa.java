package pe.rikos.operacion.venta.aplicacion;

import java.util.Optional;
import java.util.Set;

/** Las reglas de un numero y de una capacidad, compartidas por CrearMesa y ActualizarMesa. */
final class ValidadorDeMesa {

    static final int LARGO_MAXIMO_NUMERO = 10;
    static final int LARGO_MAXIMO_NOMBRE = 60;
    static final Set<String> FORMAS = Set.of("cuadrada", "redonda", "rectangular");

    private ValidadorDeMesa() {
    }

    /** Texto recortado; null si viene vacio. */
    static String limpio(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    static Optional<ErrorMesa> validar(DatosMesa datos) {
        String numero = limpio(datos.numero());
        if (numero == null || numero.length() > LARGO_MAXIMO_NUMERO) {
            return Optional.of(ErrorMesa.NUMERO_INVALIDO);
        }
        if (datos.capacidad() != null && datos.capacidad() <= 0) {
            return Optional.of(ErrorMesa.CAPACIDAD_INVALIDA);
        }
        if (datos.forma() != null && !FORMAS.contains(datos.forma())) {
            return Optional.of(ErrorMesa.FORMA_INVALIDA);
        }
        if ((datos.ancho() != null && datos.ancho() <= 0) || (datos.alto() != null && datos.alto() <= 0)) {
            return Optional.of(ErrorMesa.MEDIDAS_INVALIDAS);
        }
        return Optional.empty();
    }

    static String forma(DatosMesa datos) {
        return datos.forma() != null ? datos.forma() : "cuadrada";
    }

    /** Las medidas pedidas, o las de la forma cuando no se dan: 84 x 84, y 150 x 78 si es rectangular. */
    static int ancho(DatosMesa datos) {
        return datos.ancho() != null ? datos.ancho() : "rectangular".equals(forma(datos)) ? 150 : 84;
    }

    static int alto(DatosMesa datos) {
        return datos.alto() != null ? datos.alto() : "rectangular".equals(forma(datos)) ? 78 : 84;
    }

    static String nombreLimpio(DatosMesa datos) {
        String nombre = limpio(datos.nombre());
        return nombre != null && nombre.length() > LARGO_MAXIMO_NOMBRE ? nombre.substring(0, LARGO_MAXIMO_NOMBRE) : nombre;
    }
}
