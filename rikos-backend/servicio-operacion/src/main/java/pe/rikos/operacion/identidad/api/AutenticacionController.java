package pe.rikos.operacion.identidad.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.aplicacion.AutenticarConContrasena;
import pe.rikos.operacion.identidad.aplicacion.AutenticarConPin;
import pe.rikos.operacion.identidad.aplicacion.ErrorAutenticacion;
import pe.rikos.operacion.identidad.aplicacion.FalloAutenticacion;
import pe.rikos.operacion.identidad.aplicacion.SesionIniciada;

import java.util.Map;

/**
 * Rutas reales: POST /api/auth/login (US-002), POST /api/auth/pin (US-003).
 * Ambas abiertas en ConfiguracionSeguridad: son la puerta de entrada.
 */
@RestController
public class AutenticacionController {

    private final AutenticarConContrasena autenticarConContrasena;
    private final AutenticarConPin autenticarConPin;

    public AutenticacionController(AutenticarConContrasena autenticarConContrasena,
                                    AutenticarConPin autenticarConPin) {
        this.autenticarConContrasena = autenticarConContrasena;
        this.autenticarConPin = autenticarConPin;
    }

    public record LoginRequest(@NotBlank String nombreUsuario, @NotBlank String contrasena) {
    }

    public record PinRequest(@NotBlank String localCodigo, @NotBlank @Pattern(regexp = "\\d{4}") String pin) {
    }

    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest cuerpo, HttpServletRequest peticion) {
        Resultado<SesionIniciada, FalloAutenticacion> resultado = autenticarConContrasena.ejecutar(
                cuerpo.nombreUsuario(), cuerpo.contrasena(), peticion.getHeader("User-Agent"),
                peticion.getRemoteAddr());
        return respuesta(resultado);
    }

    @PostMapping("/auth/pin")
    public ResponseEntity<?> pin(@RequestBody PinRequest cuerpo, HttpServletRequest peticion) {
        Resultado<SesionIniciada, FalloAutenticacion> resultado = autenticarConPin.ejecutar(
                cuerpo.localCodigo(), cuerpo.pin(), peticion.getHeader("User-Agent"), peticion.getRemoteAddr());
        return respuesta(resultado);
    }

    private ResponseEntity<?> respuesta(Resultado<SesionIniciada, FalloAutenticacion> resultado) {
        if (resultado.esExito()) {
            return ResponseEntity.ok(resultado.valorOLanza());
        }
        FalloAutenticacion fallo = ((Resultado.Fracaso<SesionIniciada, FalloAutenticacion>) resultado).error();
        HttpStatus estado = fallo.tipo() == ErrorAutenticacion.BLOQUEADO ? HttpStatus.LOCKED : HttpStatus.UNAUTHORIZED;
        Map<String, Object> cuerpo = fallo.segundosRestantes() != null
                ? Map.of("error", fallo.tipo().name(), "segundosRestantes", fallo.segundosRestantes())
                : Map.of("error", fallo.tipo().name());
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
