package pe.rikos.operacion.identidad.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.aplicacion.AutenticarConContrasena;
import pe.rikos.operacion.identidad.aplicacion.AutenticarConPin;
import pe.rikos.operacion.identidad.aplicacion.CambiarLocalActivo;
import pe.rikos.operacion.identidad.aplicacion.ErrorAutenticacion;
import pe.rikos.operacion.identidad.aplicacion.ResolverPermisosEfectivos;
import pe.rikos.operacion.identidad.aplicacion.FalloAutenticacion;
import pe.rikos.operacion.identidad.aplicacion.SesionIniciada;

import java.util.List;
import java.util.Map;

/**
 * Rutas reales: POST /api/auth/login (US-002), POST /api/auth/pin (US-003).
 * Ambas abiertas en ConfiguracionSeguridad: son la puerta de entrada.
 */
@RestController
public class AutenticacionController {

    private final AutenticarConContrasena autenticarConContrasena;
    private final AutenticarConPin autenticarConPin;
    private final CambiarLocalActivo cambiarLocalActivo;
    private final ResolverPermisosEfectivos resolverPermisos;

    public AutenticacionController(AutenticarConContrasena autenticarConContrasena,
                                    AutenticarConPin autenticarConPin, CambiarLocalActivo cambiarLocalActivo,
                                    ResolverPermisosEfectivos resolverPermisos) {
        this.autenticarConContrasena = autenticarConContrasena;
        this.autenticarConPin = autenticarConPin;
        this.cambiarLocalActivo = cambiarLocalActivo;
        this.resolverPermisos = resolverPermisos;
    }

    public record LoginRequest(@NotBlank String nombreUsuario, @NotBlank String contrasena) {
    }

    public record PinRequest(@NotBlank String localCodigo, @NotBlank @Pattern(regexp = "\\d{4}") String pin) {
    }

    public record CambiarLocalRequest(@NotBlank String localId) {
    }

    /** Forma del cuerpo de error de autenticacion: segundosRestantes solo viaja cuando el fallo es BLOQUEADO. */
    public record FalloAutenticacionRespuesta(String error, Long segundosRestantes) {
    }

    @Operation(summary = "Login con usuario y contrasena")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SesionIniciada.class))),
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = FalloAutenticacionRespuesta.class))),
            @ApiResponse(responseCode = "423", content = @Content(schema = @Schema(implementation = FalloAutenticacionRespuesta.class)))
    })
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest cuerpo, HttpServletRequest peticion) {
        Resultado<SesionIniciada, FalloAutenticacion> resultado = autenticarConContrasena.ejecutar(
                cuerpo.nombreUsuario(), cuerpo.contrasena(), peticion.getHeader("User-Agent"),
                peticion.getRemoteAddr());
        return respuesta(resultado);
    }

    @Operation(summary = "Login con PIN de local")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SesionIniciada.class))),
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = FalloAutenticacionRespuesta.class))),
            @ApiResponse(responseCode = "423", content = @Content(schema = @Schema(implementation = FalloAutenticacionRespuesta.class)))
    })
    @PostMapping("/auth/pin")
    public ResponseEntity<?> pin(@RequestBody PinRequest cuerpo, HttpServletRequest peticion) {
        Resultado<SesionIniciada, FalloAutenticacion> resultado = autenticarConPin.ejecutar(
                cuerpo.localCodigo(), cuerpo.pin(), peticion.getHeader("User-Agent"), peticion.getRemoteAddr());
        return respuesta(resultado);
    }

    // Hallazgo de revision (US-006): cambiar de local en el sidebar solo
    // actualizaba estado local de React -- el JWT (y con el, el local que
    // lee RegistradorAuditoria) quedaba con el del login. Este endpoint
    // reemite el token con el local nuevo.
    @Operation(summary = "Cambiar el local activo y reemitir el token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = SesionIniciada.class))),
            @ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = FalloAutenticacionRespuesta.class))),
            @ApiResponse(responseCode = "403", content = @Content(schema = @Schema(implementation = FalloAutenticacionRespuesta.class)))
    })
    @PostMapping("/auth/cambiar-local")
    public ResponseEntity<?> cambiarLocal(@RequestBody CambiarLocalRequest cuerpo,
                                           @AuthenticationPrincipal String actorId) {
        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                cambiarLocalActivo.ejecutar(actorId, cuerpo.localId());
        if (resultado.esExito()) {
            return ResponseEntity.ok(resultado.valorOLanza());
        }
        FalloAutenticacion fallo = ((Resultado.Fracaso<SesionIniciada, FalloAutenticacion>) resultado).error();
        HttpStatus estado = fallo.tipo() == ErrorAutenticacion.LOCAL_NO_ASIGNADO
                ? HttpStatus.FORBIDDEN : HttpStatus.UNAUTHORIZED;
        return ResponseEntity.status(estado).body(Map.of("error", fallo.tipo().name()));
    }

    /**
     * Los permisos efectivos del usuario autenticado, para que el POS decida que botones mostrar sin
     * llamar a /usuarios/{id}/permisos (que es de administradores). El servidor sigue decidiendo cada accion.
     */
    @Operation(summary = "Los permisos efectivos del usuario autenticado")
    @GetMapping("/auth/mis-permisos")
    @Transactional(readOnly = true)
    public List<String> misPermisos(@AuthenticationPrincipal String actorId) {
        return resolverPermisos.resolver(actorId);
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
