package pe.rikos.operacion.pensionistas.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.aplicacion.ErrorAbono;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.pensionistas.aplicacion.AbonoAprobado;
import pe.rikos.operacion.pensionistas.aplicacion.AprobarAbonoDigital;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaAbonos;
import pe.rikos.operacion.pensionistas.aplicacion.FilaAbono;
import pe.rikos.operacion.pensionistas.aplicacion.RechazarAbonoDigital;
import pe.rikos.operacion.pensionistas.infraestructura.Abono;

import java.util.List;
import java.util.Map;

/**
 * US-024: Endpoints para consulta, aprobación y rechazo de abonos digitales de pensionistas.
 */
@RestController
@RequestMapping("/pensionistas/abonos")
@Tag(name = "Abonos de Pensionistas", description = "Gestión, consulta, aprobación y rechazo de abonos digitales")
public class AbonoController {

    private final ConsultaAbonos consultaAbonos;
    private final AprobarAbonoDigital aprobarAbono;
    private final RechazarAbonoDigital rechazarAbono;

    public AbonoController(ConsultaAbonos consultaAbonos,
                           AprobarAbonoDigital aprobarAbono,
                           RechazarAbonoDigital rechazarAbono) {
        this.consultaAbonos = consultaAbonos;
        this.aprobarAbono = aprobarAbono;
        this.rechazarAbono = rechazarAbono;
    }

    public record SolicitudRechazo(String motivoId, String observacion) {}

    @Operation(summary = "Listar abonos pendientes de aprobación")
    @GetMapping("/pendientes")
    public List<FilaAbono> listarPendientes() {
        return consultaAbonos.listar(Abono.ESTADO_PENDIENTE, null);
    }

    @Operation(summary = "Contar abonos pendientes")
    @GetMapping("/conteo-pendientes")
    public Map<String, Long> contarPendientes() {
        return Map.of("pendientes", consultaAbonos.contarPendientes());
    }

    @Operation(summary = "Listar abonos con filtro opcional por estado y búsqueda")
    @GetMapping
    public List<FilaAbono> listar(@RequestParam(required = false) String estado,
                                  @RequestParam(required = false) String busqueda) {
        return consultaAbonos.listar(estado, busqueda);
    }

    @Operation(summary = "Aprobar un abono digital pendiente (solo administrador central)")
    @PostMapping("/{id}/aprobar")
    public ResponseEntity<?> aprobar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        Resultado<AbonoAprobado, ErrorAbono> res = aprobarAbono.ejecutar(id, actorId);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<AbonoAprobado, ErrorAbono>) res).error());
        }
        return ResponseEntity.ok(res.valorOLanza());
    }

    @Operation(summary = "Rechazar un abono digital pendiente (solo administrador central)")
    @PostMapping("/{id}/rechazar")
    public ResponseEntity<?> rechazar(@PathVariable String id,
                                      @RequestBody(required = false) SolicitudRechazo solicitud,
                                      @AuthenticationPrincipal String actorId) {
        String motivoId = solicitud != null ? solicitud.motivoId() : null;
        String obs = solicitud != null ? solicitud.observacion() : null;
        Resultado<Abono, ErrorAbono> res = rechazarAbono.ejecutar(id, motivoId, obs, actorId);
        if (res.esFracaso()) {
            return mapearError(((Resultado.Fracaso<Abono, ErrorAbono>) res).error());
        }
        return ResponseEntity.ok(res.valorOLanza());
    }

    private ResponseEntity<ErrorRespuesta> mapearError(ErrorAbono error) {
        HttpStatus status = switch (error) {
            case ABONO_NO_ENCONTRADO, PENSIONISTA_NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case ABONO_YA_PROCESADO -> HttpStatus.CONFLICT;
            case MOTIVO_INVALIDO, OBSERVACION_REQUERIDA -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(status).body(new ErrorRespuesta(error.name()));
    }
}
