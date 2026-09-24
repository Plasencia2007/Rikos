package pe.rikos.contratos;

import java.security.SecureRandom;
import java.util.Objects;

/**
 * ULID (Universally Unique Lexicographically Sortable Identifier) generado
 * en el origen de la aplicacion, nunca autoincremental de la base de datos.
 *
 * 26 caracteres en Base32 de Crockford: 48 bits de marca de tiempo en
 * milisegundos (10 caracteres) + 80 bits de aleatoriedad (16 caracteres).
 * Es ordenable como texto porque la marca de tiempo va primero.
 *
 * Por que no autoincremental: un local sin conexion tiene que poder crear
 * registros sin colisionar con los de otro local (ver ESTRUCTURA.md, seccion 5).
 */
public final class Identificador {

    private static final char[] ALFABETO_CROCKFORD = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final int LARGO = 26;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private final String valor;

    private Identificador(String valor) {
        this.valor = valor;
    }

    public static Identificador nuevo() {
        return new Identificador(generar(System.currentTimeMillis()));
    }

    public static Identificador de(String valor) {
        validar(valor);
        return new Identificador(valor);
    }

    public String valor() {
        return valor;
    }

    private static String generar(long marcaDeTiempoMs) {
        byte[] aleatoriedad = new byte[10];
        ALEATORIO.nextBytes(aleatoriedad);

        char[] resultado = new char[LARGO];
        long tiempo = marcaDeTiempoMs;
        for (int i = 9; i >= 0; i--) {
            resultado[i] = ALFABETO_CROCKFORD[(int) (tiempo & 0x1F)];
            tiempo >>>= 5;
        }

        long altos = ((long) (aleatoriedad[0] & 0xFF) << 32)
                | ((long) (aleatoriedad[1] & 0xFF) << 24)
                | ((long) (aleatoriedad[2] & 0xFF) << 16)
                | ((long) (aleatoriedad[3] & 0xFF) << 8)
                | (aleatoriedad[4] & 0xFF);
        long bajos = ((long) (aleatoriedad[5] & 0xFF) << 32)
                | ((long) (aleatoriedad[6] & 0xFF) << 24)
                | ((long) (aleatoriedad[7] & 0xFF) << 16)
                | ((long) (aleatoriedad[8] & 0xFF) << 8)
                | (aleatoriedad[9] & 0xFF);

        for (int i = 15; i >= 8; i--) {
            resultado[i] = ALFABETO_CROCKFORD[(int) (altos & 0x1F)];
            altos >>>= 5;
        }
        for (int i = 25; i >= 16; i--) {
            resultado[i] = ALFABETO_CROCKFORD[(int) (bajos & 0x1F)];
            bajos >>>= 5;
        }

        return new String(resultado);
    }

    private static void validar(String valor) {
        if (valor == null || valor.length() != LARGO) {
            throw new IllegalArgumentException(
                    "Un identificador debe tener " + LARGO + " caracteres: " + valor);
        }
        for (char c : valor.toCharArray()) {
            if (indiceEnAlfabeto(c) < 0) {
                throw new IllegalArgumentException(
                        "Caracter fuera del alfabeto Crockford Base32: " + c);
            }
        }
    }

    private static int indiceEnAlfabeto(char c) {
        char mayuscula = Character.toUpperCase(c);
        for (int i = 0; i < ALFABETO_CROCKFORD.length; i++) {
            if (ALFABETO_CROCKFORD[i] == mayuscula) return i;
        }
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Identificador otro)) return false;
        return valor.equals(otro.valor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(valor);
    }

    @Override
    public String toString() {
        return valor;
    }
}
