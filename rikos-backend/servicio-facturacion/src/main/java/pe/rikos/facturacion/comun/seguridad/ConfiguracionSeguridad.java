package pe.rikos.facturacion.comun.seguridad;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del esqueleto: deja todos los endpoints abiertos.
 *
 * Existe porque Spring Security, con solo estar en el classpath, bloquea todo
 * y genera una contrasena aleatoria en consola. Sin esto no se puede probar
 * ni el endpoint de salud.
 *
 * Esta configuracion NO se activa con el perfil "nube". Si alguien despliega
 * al servidor central sin haber implementado todavia la autenticacion real,
 * Spring Security vuelve a su comportamiento por defecto y bloquea todo:
 * falla cerrado, que es lo correcto.
 *
 * Se reemplaza al implementar US-002 (autenticacion con usuario y contrasena,
 * mas PIN de mozo) y US-004 (permisos granulares por modulo y accion).
 */
@Configuration
@Profile("!nube")
public class ConfiguracionSeguridad {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionSeguridad.class);

    @Bean
    SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(rutas -> rutas.anyRequest().permitAll());
        return http.build();
    }

    @PostConstruct
    void avisar() {
        log.warn("=================================================================");
        log.warn(" SEGURIDAD ABIERTA: todos los endpoints estan sin autenticacion.");
        log.warn(" Solo para desarrollo del esqueleto. Pendiente US-002 y US-004.");
        log.warn("=================================================================");
    }
}
