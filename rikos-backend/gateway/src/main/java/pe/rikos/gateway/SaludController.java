package pe.rikos.gateway;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Ruta real: GET /salud (el gateway no se enruta a si mismo).
 */
@RestController
public class SaludController {

    private final Environment entorno;

    public SaludController(Environment entorno) {
        this.entorno = entorno;
    }

    @GetMapping("/salud")
    public Map<String, Object> salud() {
        return Map.of(
                "servicio", "gateway",
                "perfilActivo", perfilActivo(),
                "estado", "ok",
                "hora", OffsetDateTime.now().toString()
        );
    }

    private String perfilActivo() {
        String[] activos = entorno.getActiveProfiles();
        return activos.length == 0 ? "default" : String.join(",", activos);
    }
}
