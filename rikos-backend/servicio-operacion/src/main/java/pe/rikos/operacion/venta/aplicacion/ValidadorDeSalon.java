package pe.rikos.operacion.venta.aplicacion;

import pe.rikos.operacion.venta.infraestructura.Salon;

import java.util.Optional;
import java.util.Set;

/** Las reglas de un salon, compartidas por CrearSalon y ActualizarSalon. */
final class ValidadorDeSalon {

    static final int LARGO_MAXIMO_NOMBRE = 60;
    static final int LARGO_MAXIMO_DESCRIPCION = 250;
    static final Set<String> ICONOS = Set.of("terraza", "cubiertos", "copa", "arbol", "taza", "mesa");
    static final Set<String> COLORES = Set.of("verde", "azul", "violeta", "ambar", "turquesa", "gris");
    static final int PLANO_MAXIMO = 10_000;

    private ValidadorDeSalon() {
    }

    /** Texto recortado; null si viene vacio. */
    static String limpio(String texto) {
        if (texto == null) {
            return null;
        }
        String recortado = texto.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    static Optional<ErrorSalon> validar(DatosSalon datos) {
        String nombre = limpio(datos.nombre());
        if (nombre == null || nombre.length() > LARGO_MAXIMO_NOMBRE) {
            return Optional.of(ErrorSalon.NOMBRE_INVALIDO);
        }
        String descripcion = limpio(datos.descripcion());
        if (descripcion != null && descripcion.length() > LARGO_MAXIMO_DESCRIPCION) {
            return Optional.of(ErrorSalon.VALOR_INVALIDO);
        }
        if (!ICONOS.contains(datos.icono()) || !COLORES.contains(datos.color())) {
            return Optional.of(ErrorSalon.VALOR_INVALIDO);
        }
        if (!medidaValida(datos.anchoPlano()) || !medidaValida(datos.altoPlano())) {
            return Optional.of(ErrorSalon.VALOR_INVALIDO);
        }
        if (datos.colorPiso() != null && !datos.colorPiso().matches("^#[0-9A-Fa-f]{6}$")) {
            return Optional.of(ErrorSalon.VALOR_INVALIDO);
        }
        return Optional.empty();
    }

    private static boolean medidaValida(Integer medida) {
        return medida == null || (medida > 0 && medida <= PLANO_MAXIMO);
    }

    static int ancho(DatosSalon datos) {
        return datos.anchoPlano() != null ? datos.anchoPlano() : Salon.ANCHO_PLANO_POR_DEFECTO;
    }

    static int alto(DatosSalon datos) {
        return datos.altoPlano() != null ? datos.altoPlano() : Salon.ALTO_PLANO_POR_DEFECTO;
    }

    static String colorPiso(DatosSalon datos) {
        return datos.colorPiso() != null ? datos.colorPiso().toUpperCase() : Salon.COLOR_PISO_POR_DEFECTO;
    }
}
