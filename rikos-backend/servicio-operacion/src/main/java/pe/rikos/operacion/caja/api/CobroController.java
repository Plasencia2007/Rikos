package pe.rikos.operacion.caja.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.aplicacion.CargoRegistrado;
import pe.rikos.operacion.caja.aplicacion.CobrarPedido;
import pe.rikos.operacion.caja.aplicacion.CobroRegistrado;
import pe.rikos.operacion.caja.aplicacion.ErrorCobro;
import pe.rikos.operacion.caja.aplicacion.PagoSolicitado;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * US-033: cobrar lineas de un pedido. Exige caja.crear (cajero y administradores; el mozo pide pero no cobra) y que quien
 * cobra tenga un turno abierto: el cobro entra a SU caja. Dos cajeros cobrando la misma linea: el segundo recibe 409.
 */
@RestController
public class CobroController {

    public record PagoRequest(String medio, Long monto, String referencia) {
    }

    public record CobroRequest(List<String> lineaIds, List<PagoRequest> pagos) {
    }

    public record PagoRespuesta(String medio, long importe, Long montoRecibido, String referencia) {
    }

    public record CobroRespuesta(String id, String pedidoId, String turnoId, OffsetDateTime cobradoEn, long total,
                                 long totalMedios, long totalCuentas, long vuelto, List<PagoRespuesta> pagos,
                                 List<CargoRegistrado> cargos, boolean pedidoCerrado) {
    }

    private final CobrarPedido cobrarPedido;

    public CobroController(CobrarPedido cobrarPedido) {
        this.cobrarPedido = cobrarPedido;
    }

    @Operation(summary = "Cobrar lineas de un pedido: pagos en medios (efectivo con vuelto) y cargo a la cuenta de los pensionistas")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CobroRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/pedidos/{id}/cobros")
    public ResponseEntity<?> cobrar(@PathVariable String id, @RequestBody CobroRequest cuerpo,
                                    @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            List<PagoSolicitado> pagos = cuerpo.pagos() == null ? List.of() : cuerpo.pagos().stream()
                    .map(p -> new PagoSolicitado(p.medio(), p.monto() == null ? 0L : p.monto(), p.referencia())).toList();
            Resultado<CobroRegistrado, ErrorCobro> resultado = cobrarPedido.ejecutar(id, cuerpo.lineaIds(), pagos, actorId);
            if (resultado.esFracaso()) {
                return errorDe(resultado);
            }
            CobroRegistrado r = resultado.valorOLanza();
            var c = r.cobro();
            return ResponseEntity.status(HttpStatus.CREATED).body(new CobroRespuesta(c.getId(), c.getPedidoId(),
                    c.getTurnoCajaId(), c.getCobradoEn(), c.getTotal(), c.getTotalMedios(), c.getTotalCuentas(),
                    c.getVuelto(), r.pagos().stream().map(p -> new PagoRespuesta(p.getMedio(), p.getImporte(),
                            p.getMontoRecibido(), p.getReferencia())).toList(), r.cargos(), r.pedidoCerrado()));
        });
    }

    /** Dos cobros a la vez sobre la misma linea chocan en la version de la linea o en el indice unico de cobro_linea. */
    private ResponseEntity<?> protegido(Supplier<ResponseEntity<?>> accion) {
        try {
            return accion.get();
        } catch (ObjectOptimisticLockingFailureException e) {
            return error(HttpStatus.CONFLICT, "PEDIDO_MODIFICADO");
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMessage()).contains("cobro_linea_una_vez")) {
                return error(HttpStatus.CONFLICT, ErrorCobro.LINEA_YA_COBRADA.name());
            }
            throw e;
        }
    }

    private static ResponseEntity<?> errorDe(Resultado<?, ErrorCobro> resultado) {
        ErrorCobro e = ((Resultado.Fracaso<?, ErrorCobro>) resultado).error();
        HttpStatus estado = switch (e) {
            case PEDIDO_NO_ENCONTRADO, LINEA_NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
            case PEDIDO_CERRADO, LINEA_YA_COBRADA, LINEA_ANULADA, LINEA_SIN_COMANDAR, SIN_TURNO_ABIERTO,
                 PENSIONISTA_INACTIVO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return error(estado, e.name());
    }

    private static ResponseEntity<?> error(HttpStatus estado, String codigo) {
        return ResponseEntity.status(estado).body(Map.of("error", codigo));
    }
}
