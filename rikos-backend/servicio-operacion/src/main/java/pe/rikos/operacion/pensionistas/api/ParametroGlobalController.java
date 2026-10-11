package pe.rikos.operacion.pensionistas.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.pensionistas.aplicacion.CambiarParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ClaveParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ErrorParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ParametrosGlobales;
import pe.rikos.operacion.pensionistas.infraestructura.ParametroGlobal;

import java.util.Map;

/**
 * Parametros de cobro (sprint 5, 3.6): precio del taper y regla del segundo
 * consumo. Verlos, pensionistas.ver; cambiarlos, pensionistas.administrar
 * (solo el administrador central).
 */
@RestController
@RequestMapping("/parametros")
public class ParametroGlobalController {

    private final ParametrosGlobales parametros;
    private final CambiarParametro cambiarParametro;

    public ParametroGlobalController(ParametrosGlobales parametros, CambiarParametro cambiarParametro) {
        this.parametros = parametros;
        this.cambiarParametro = cambiarParametro;
    }

    public record CambiarParametroRequest(String valor) {
    }

    /** taperPrecio en centimos; null = sin monto todavia (no se cobra taper). */
    public record ParametrosRespuesta(Long taperPrecio, String reglaSegundoConsumo) {
    }

    private ParametrosRespuesta respuesta() {
        return new ParametrosRespuesta(parametros.taperPrecio().orElse(null),
                parametros.reglaSegundoConsumo().codigo());
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ParametrosRespuesta obtener() {
        return respuesta();
    }

    @Operation(summary = "Cambiar un parámetro de cobro")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ParametrosRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/{clave}")
    @Transactional
    public ResponseEntity<?> cambiar(@PathVariable String clave, @RequestBody CambiarParametroRequest cuerpo,
                                     @AuthenticationPrincipal String actorId) {
        ClaveParametro claveConocida = ClaveParametro.desde(clave).orElse(null);
        if (claveConocida == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ErrorParametro.NO_ENCONTRADO.name()));
        }
        Resultado<ParametroGlobal, ErrorParametro> resultado =
                cambiarParametro.ejecutar(claveConocida, cuerpo.valor(), actorId);
        if (resultado.esExito()) {
            return ResponseEntity.ok(respuesta());
        }
        ErrorParametro e = ((Resultado.Fracaso<?, ErrorParametro>) resultado).error();
        HttpStatus estado = e == ErrorParametro.NO_ENCONTRADO ? HttpStatus.NOT_FOUND : HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(estado).body(Map.of("error", e.name()));
    }
}
