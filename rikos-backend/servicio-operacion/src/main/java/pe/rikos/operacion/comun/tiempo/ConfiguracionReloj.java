package pe.rikos.operacion.comun.tiempo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * "Hoy" en la zona del restaurante, no en la del servidor. Sin esto, un
 * servidor en UTC cambia de dia a las 7 p. m. de Lima -- en plena cena --
 * y cualquier regla por fecha (la programacion semanal cierra los dias
 * anteriores a hoy) se corre un dia.
 *
 * Un bean y no LocalDate.now() suelto: las pruebas lo reemplazan por un
 * reloj fijo y asi pueden decir "hoy es miercoles".
 */
@Configuration
public class ConfiguracionReloj {

    public static final ZoneId ZONA_DEL_RESTAURANTE = ZoneId.of("America/Lima");

    @Bean
    public Clock reloj() {
        return Clock.system(ZONA_DEL_RESTAURANTE);
    }
}
