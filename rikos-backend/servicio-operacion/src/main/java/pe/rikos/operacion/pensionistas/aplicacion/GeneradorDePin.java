package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** El PIN presencial del pensionista: cuatro digitos, generado por el sistema (nunca elegido a mano). */
@Component
class GeneradorDePin {

    private final SecureRandom azar = new SecureRandom();

    String nuevo() {
        return String.format("%04d", azar.nextInt(10_000));
    }
}
