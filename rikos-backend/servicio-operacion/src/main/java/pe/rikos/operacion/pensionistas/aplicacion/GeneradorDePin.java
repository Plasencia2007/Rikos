package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;

import java.security.SecureRandom;

/** PIN unico de seis digitos. El llamador mantiene una transaccion hasta guardar la huella. */
@Component
class GeneradorDePin {

    static final int LONGITUD = 6;
    private final PensionistaRepositorio pensionistas;
    private final HuellaPinPensionista huellas;
    private final JdbcTemplate jdbc;

    GeneradorDePin(PensionistaRepositorio pensionistas, HuellaPinPensionista huellas, JdbcTemplate jdbc) {
        this.pensionistas = pensionistas;
        this.huellas = huellas;
        this.jdbc = jdbc;
    }

    private final SecureRandom azar = new SecureRandom();

    String nuevo() {
        jdbc.execute("SELECT pg_advisory_xact_lock(82647101)");
        for (int i = 0; i < 100; i++) {
            String pin = String.format(java.util.Locale.ROOT, "%06d", azar.nextInt(1_000_000));
            if (!pensionistas.existsByHuellaPin(huellas.de(pin))) return pin;
        }
        throw new IllegalStateException("No se pudo reservar un PIN; reintenta la operacion");
    }

    String huella(String pin) { return huellas.de(pin); }
}
