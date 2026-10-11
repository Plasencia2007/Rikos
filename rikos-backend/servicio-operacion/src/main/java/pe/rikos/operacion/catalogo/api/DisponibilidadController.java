package pe.rikos.operacion.catalogo.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.ConsultaDisponibilidad;
import pe.rikos.operacion.catalogo.aplicacion.ErrorDisponibilidad;
import pe.rikos.operacion.catalogo.aplicacion.MarcarAgotado;
import pe.rikos.operacion.catalogo.aplicacion.ReactivarProducto;
import pe.rikos.operacion.catalogo.infraestructura.DisponibilidadProducto;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * US-015: agotados del local activo. El local sale del token, nunca de la
 * peticion -- nadie marca un agotado en un local donde no esta (CA4).
 */
@RestController
@RequestMapping("/disponibilidad")
public class DisponibilidadController {

    private final MarcarAgotado marcarAgotado;
    private final ReactivarProducto reactivarProducto;
    private final ConsultaDisponibilidad consulta;

    public DisponibilidadController(MarcarAgotado marcarAgotado, ReactivarProducto reactivarProducto,
                                    ConsultaDisponibilidad consulta) {
        this.marcarAgotado = marcarAgotado;
        this.reactivarProducto = reactivarProducto;
        this.consulta = consulta;
    }

    public record ReactivarRequest(String motivoId, String observacion) {
    }

    public record AgotadoRespuesta(String productoId, String productoNombre, OffsetDateTime marcadoEn) {
        static AgotadoRespuesta de(DisponibilidadProducto d) {
            return new AgotadoRespuesta(d.getProducto().getId(), d.getProducto().getNombre(), d.getMarcadoEn());
        }
    }

    public record DisponibilidadRespuesta(String productoId, boolean agotado, OffsetDateTime marcadoEn) {
        static DisponibilidadRespuesta de(DisponibilidadProducto d) {
            return new DisponibilidadRespuesta(d.getProducto().getId(), d.isAgotado(), d.getMarcadoEn());
        }
    }

    @Operation(summary = "Productos agotados en el local activo")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping("/agotados")
    @Transactional(readOnly = true)
    public ResponseEntity<?> agotados(Authentication autenticacion) {
        Object local = autenticacion == null ? null : autenticacion.getDetails();
        if (local == null) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, ErrorDisponibilidad.SIN_LOCAL_ACTIVO.name());
        }
        List<AgotadoRespuesta> lista = consulta.agotadosDelLocal(String.valueOf(local)).stream()
                .map(AgotadoRespuesta::de).toList();
        return ResponseEntity.ok(lista);
    }

    @Operation(summary = "Marcar agotado en el local activo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = DisponibilidadRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/{productoId}/agotar")
    @Transactional
    public ResponseEntity<?> agotar(@PathVariable String productoId) {
        return responder(marcarAgotado.ejecutar(productoId));
    }

    @Operation(summary = "Volver a ofrecer un agotado (con motivo)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = DisponibilidadRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/{productoId}/reactivar")
    @Transactional
    public ResponseEntity<?> reactivar(@PathVariable String productoId, @RequestBody ReactivarRequest cuerpo) {
        return responder(reactivarProducto.ejecutar(productoId, cuerpo.motivoId(), cuerpo.observacion()));
    }

    private ResponseEntity<?> responder(Resultado<DisponibilidadProducto, ErrorDisponibilidad> resultado) {
        if (resultado.esExito()) {
            return ResponseEntity.ok(DisponibilidadRespuesta.de(resultado.valorOLanza()));
        }
        ErrorDisponibilidad e = ((Resultado.Fracaso<?, ErrorDisponibilidad>) resultado).error();
        HttpStatus estado = switch (e) {
            case PRODUCTO_NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case YA_AGOTADO, NO_ESTA_AGOTADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return error(estado, e.name());
    }

    private static ResponseEntity<?> error(HttpStatus estado, String codigo) {
        return ResponseEntity.status(estado).body(Map.of("error", codigo));
    }
}
