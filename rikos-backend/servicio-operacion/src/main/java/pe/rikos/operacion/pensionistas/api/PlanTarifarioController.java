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
import pe.rikos.operacion.pensionistas.aplicacion.CambiarPreciosPlan;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaPlanes;
import pe.rikos.operacion.pensionistas.aplicacion.CrearPlanTarifario;
import pe.rikos.operacion.pensionistas.aplicacion.ErrorPlan;
import pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.pensionistas.infraestructura.VersionPlan;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * US-017: planes tarifarios. Verlos es pensionistas.ver (caja y mozo los
 * necesitan); crearlos y cambiar sus precios es pensionistas.administrar,
 * solo del administrador central (CA5: un solo plan para todos los locales).
 * Precios en centimos; null = sin precio todavia.
 */
@RestController
@RequestMapping("/planes")
public class PlanTarifarioController {

    private final CrearPlanTarifario crearPlan;
    private final CambiarPreciosPlan cambiarPrecios;
    private final ConsultaPlanes consulta;
    private final Clock reloj;

    public PlanTarifarioController(CrearPlanTarifario crearPlan, CambiarPreciosPlan cambiarPrecios,
                                   ConsultaPlanes consulta, Clock reloj) {
        this.crearPlan = crearPlan;
        this.cambiarPrecios = cambiarPrecios;
        this.consulta = consulta;
        this.reloj = reloj;
    }

    public record CrearPlanRequest(String nombre, String descripcion, Long precioDesayuno, Long precioAlmuerzo,
                                   Long precioCena, LocalDate vigenteDesde) {
    }

    public record CambiarPreciosRequest(Long precioDesayuno, Long precioAlmuerzo, Long precioCena,
                                        LocalDate vigenteDesde) {
    }

    /** estado: 'vigente' (rige hoy), 'programada' (empieza despues) o 'cerrada' (ya termino). */
    public record VersionRespuesta(LocalDate vigenteDesde, LocalDate vigenteHasta, Long precioDesayuno,
                                   Long precioAlmuerzo, Long precioCena, String estado) {
        static VersionRespuesta de(VersionPlan v, LocalDate hoy) {
            String estado = v.rigeEn(hoy) ? "vigente" : v.getVigenteDesde().isAfter(hoy) ? "programada" : "cerrada";
            return new VersionRespuesta(v.getVigenteDesde(), v.getVigenteHasta(), v.getPrecioDesayuno(),
                    v.getPrecioAlmuerzo(), v.getPrecioCena(), estado);
        }
    }

    /** versiones: el historial completo, la mas nueva primero. */
    public record PlanRespuesta(String id, String nombre, String descripcion, LocalDate hoy,
                                List<VersionRespuesta> versiones) {
    }

    private PlanRespuesta respuestaDe(PlanTarifario plan) {
        LocalDate hoy = LocalDate.now(reloj);
        return new PlanRespuesta(plan.getId(), plan.getNombre(), plan.getDescripcion(), hoy,
                consulta.versiones(plan.getId()).stream().map(v -> VersionRespuesta.de(v, hoy)).toList());
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<PlanRespuesta> listar() {
        return consulta.planes().stream().map(this::respuestaDe).toList();
    }

    @Operation(summary = "Crear plan tarifario")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = PlanRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@RequestBody CrearPlanRequest cuerpo, @AuthenticationPrincipal String actorId) {
        Resultado<PlanTarifario, ErrorPlan> resultado = crearPlan.ejecutar(cuerpo.nombre(), cuerpo.descripcion(),
                new PreciosPlan(cuerpo.precioDesayuno(), cuerpo.precioAlmuerzo(), cuerpo.precioCena()),
                cuerpo.vigenteDesde(), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(respuestaDe(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    @Operation(summary = "Cambiar los precios de un plan (crea una version nueva)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PlanRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/{id}/precios")
    @Transactional
    public ResponseEntity<?> cambiar(@PathVariable String id, @RequestBody CambiarPreciosRequest cuerpo,
                                     @AuthenticationPrincipal String actorId) {
        Resultado<PlanTarifario, ErrorPlan> resultado = cambiarPrecios.ejecutar(id,
                new PreciosPlan(cuerpo.precioDesayuno(), cuerpo.precioAlmuerzo(), cuerpo.precioCena()),
                cuerpo.vigenteDesde(), actorId);
        return resultado.esExito() ? ResponseEntity.ok(respuestaDe(resultado.valorOLanza())) : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorPlan> resultado) {
        ErrorPlan e = ((Resultado.Fracaso<?, ErrorPlan>) resultado).error();
        HttpStatus estado = switch (e) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case NOMBRE_DUPLICADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", e.name()));
    }
}
