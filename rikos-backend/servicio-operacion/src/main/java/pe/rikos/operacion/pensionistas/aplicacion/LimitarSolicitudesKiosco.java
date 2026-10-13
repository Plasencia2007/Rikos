package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.OffsetDateTime;

/** Ventana de un minuto por origen de red. No imputa PIN desconocidos a una cuenta. */
@Service
public class LimitarSolicitudesKiosco {
    private final JdbcTemplate jdbc;
    private final HuellaPinPensionista huellas;
    private final Clock reloj;
    public LimitarSolicitudesKiosco(JdbcTemplate jdbc, HuellaPinPensionista huellas, Clock reloj) {
        this.jdbc = jdbc; this.huellas = huellas; this.reloj = reloj;
    }
    @Transactional
    public boolean permitir(String origen) {
        OffsetDateTime ahora = OffsetDateTime.now(reloj);
        // La ventana se serializa en PostgreSQL aun si llegan peticiones simultaneas.
        Integer cantidad = jdbc.queryForObject("""
            INSERT INTO kiosco_intentos (origen_hash, ventana, cantidad) VALUES (?, ?, 1)
            ON CONFLICT (origen_hash) DO UPDATE SET
              cantidad = CASE WHEN kiosco_intentos.ventana <= ? THEN 1 ELSE kiosco_intentos.cantidad + 1 END,
              ventana = CASE WHEN kiosco_intentos.ventana <= ? THEN EXCLUDED.ventana ELSE kiosco_intentos.ventana END
            RETURNING cantidad
            """, Integer.class, huellas.origen(origen == null ? "desconocido" : origen), ahora,
                ahora.minusMinutes(1), ahora.minusMinutes(1));
        jdbc.update("DELETE FROM kiosco_intentos WHERE ventana < ?", ahora.minusDays(1));
        return cantidad != null && cantidad <= 60;
    }
}
