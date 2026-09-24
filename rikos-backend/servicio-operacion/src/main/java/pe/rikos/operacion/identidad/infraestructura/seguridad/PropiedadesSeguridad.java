package pe.rikos.operacion.identidad.infraestructura.seguridad;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RF-006 sigue pendiente de definicion: el umbral de intentos y la duracion
 * del bloqueo son valores provisionales, por eso viven en configuracion y
 * no como constantes en el codigo (US-003).
 */
@ConfigurationProperties(prefix = "rikos.seguridad")
public class PropiedadesSeguridad {

    private final Jwt jwt = new Jwt();
    private final Pin pin = new Pin();

    public Jwt getJwt() {
        return jwt;
    }

    public Pin getPin() {
        return pin;
    }

    public static class Jwt {
        private String secreto;
        private long expiracionMinutos = 480;

        public String getSecreto() {
            return secreto;
        }

        public void setSecreto(String secreto) {
            this.secreto = secreto;
        }

        public long getExpiracionMinutos() {
            return expiracionMinutos;
        }

        public void setExpiracionMinutos(long expiracionMinutos) {
            this.expiracionMinutos = expiracionMinutos;
        }
    }

    public static class Pin {
        private int umbralIntentos = 5;
        private int minutosBloqueo = 15;

        public int getUmbralIntentos() {
            return umbralIntentos;
        }

        public void setUmbralIntentos(int umbralIntentos) {
            this.umbralIntentos = umbralIntentos;
        }

        public int getMinutosBloqueo() {
            return minutosBloqueo;
        }

        public void setMinutosBloqueo(int minutosBloqueo) {
            this.minutosBloqueo = minutosBloqueo;
        }
    }
}
