package pe.rikos.operacion.comun.web;

import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Endpoint minimo para comprobar que el servicio responde, que sabe
 * en que perfil corre y que su conexion a base de datos funciona.
 *
 * Con el context-path /api, la ruta real es:  GET /api/salud
 * Y a traves del gateway:                     GET http://localhost:8080/api/salud
 */
@RestController
public class SaludController {

    private final Environment entorno;
    private final DataSource dataSource;

    public SaludController(Environment entorno, DataSource dataSource) {
        this.entorno = entorno;
        this.dataSource = dataSource;
    }

    @GetMapping("/salud")
    public Map<String, Object> salud() {
        return Map.of(
                "servicio", "servicio-operacion",
                "perfilActivo", perfilActivo(),
                "baseDeDatos", baseDeDatos(),
                "estado", "ok",
                "hora", OffsetDateTime.now().toString()
        );
    }

    private String perfilActivo() {
        String[] activos = entorno.getActiveProfiles();
        return activos.length == 0 ? "default" : String.join(",", activos);
    }

    private String baseDeDatos() {
        try (Connection conexion = dataSource.getConnection()) {
            return conexion.isValid(2) ? "conectada" : "desconectada";
        } catch (SQLException excepcion) {
            return "desconectada";
        }
    }
}
