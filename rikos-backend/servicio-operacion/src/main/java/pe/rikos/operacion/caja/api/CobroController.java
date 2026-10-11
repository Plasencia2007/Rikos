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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.aplicacion.CargoRegistrado;
import pe.rikos.operacion.caja.aplicacion.CobrarPedido;
import pe.rikos.operacion.caja.aplicacion.CobroRegistrado;
import pe.rikos.operacion.caja.aplicacion.ConsultaCobros;
import pe.rikos.operacion.caja.aplicacion.ReabrirCobro;
import pe.rikos.operacion.caja.infraestructura.Cobro;
import pe.rikos.operacion.caja.aplicacion.DescuentoSolicitado;
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

    /** Un descuento para UNA linea: tipo "monto" (centimos) o "porcentaje" (1 a 100), con motivo de tipo descuento. */
    public record DescuentoRequest(String lineaId, String tipo, Long valor, String motivoId, String observacion) {
    }

    public record CobroRequest(List<String> lineaIds, List<PagoRequest> pagos, List<DescuentoRequest> descuentos) {
    }

    public record PagoRespuesta(String medio, long importe, Long montoRecibido, String referencia) {
    }

    public record CobroRespuesta(String id, String pedidoId, String turnoId, OffsetDateTime cobradoEn, long total,
                                 long totalMedios, long totalCuentas, long vuelto, List<PagoRespuesta> pagos,
                                 List<CargoRegistrado> cargos, boolean pedidoCerrado) {
    }

    public record ReaperturaRequest(String motivoId, String observacion) {
    }

    public record CobroDelPedido(String id, OffsetDateTime cobradoEn, long total, long totalMedios, long totalCuentas,
                                 boolean revertido, List<String> lineaIds) {
    }

    /** Un cobro del turno del cajero, con el pedido que cobro: lo que la caja ve para reabrir. */
    public record CobroDelTurnoRespuesta(String id, String pedidoId, String pedidoNumero, OffsetDateTime cobradoEn, long total,
                                         long totalMedios, long totalCuentas, long vuelto, boolean revertido) {
    }

    private final CobrarPedido cobrarPedido;
    private final ReabrirCobro reabrirCobro;
    private final ConsultaCobros consultaCobros;

    public CobroController(CobrarPedido cobrarPedido, ReabrirCobro reabrirCobro, ConsultaCobros consultaCobros) {
        this.cobrarPedido = cobrarPedido;
        this.reabrirCobro = reabrirCobro;
        this.consultaCobros = consultaCobros;
    }

    @Operation(summary = "Los cobros del turno abierto del cajero, el mas reciente primero")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CobroDelTurnoRespuesta[].class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping("/turnos/actual/cobros")
    public ResponseEntity<?> cobrosDelTurno(@AuthenticationPrincipal String actorId) {
        var resultado = consultaCobros.delTurnoActual(actorId);
        if (resultado.esFracaso()) {
            return errorDe(resultado);
        }
        return ResponseEntity.ok(resultado.valorOLanza().stream().map(c -> new CobroDelTurnoRespuesta(c.cobro().getId(),
                c.cobro().getPedidoId(), c.pedidoNumero(), c.cobro().getCobradoEn(), c.cobro().getTotal(),
                c.cobro().getTotalMedios(), c.cobro().getTotalCuentas(), c.cobro().getVuelto(), c.cobro().estaRevertido()))
                .toList());
    }

    @Operation(summary = "Los cobros de un pedido, con las lineas que cobro cada uno y si ya se reabrio")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CobroDelPedido[].class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping("/pedidos/{id}/cobros")
    public ResponseEntity<?> cobrosDelPedido(@PathVariable String id) {
        var resultado = consultaCobros.deLaPedido(id);
        if (resultado.esFracaso()) {
            return errorDe(resultado);
        }
        return ResponseEntity.ok(resultado.valorOLanza().stream().map(c -> new CobroDelPedido(c.cobro().getId(),
                c.cobro().getCobradoEn(), c.cobro().getTotal(), c.cobro().getTotalMedios(), c.cobro().getTotalCuentas(),
                c.cobro().estaRevertido(), c.lineas().stream().map(l -> l.getDetallePedidoId()).toList())).toList());
    }

    @Operation(summary = "Reabrir un cobro mientras su turno de origen siga abierto (permiso caja.reabrir, con motivo)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CobroDelPedido.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/cobros/{id}/reapertura")
    public ResponseEntity<?> reabrir(@PathVariable String id, @RequestBody ReaperturaRequest cuerpo,
                                     @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Cobro, ErrorCobro> resultado = reabrirCobro.ejecutar(id, cuerpo.motivoId(), cuerpo.observacion(), actorId);
            if (resultado.esFracaso()) {
                return errorDe(resultado);
            }
            Cobro c = resultado.valorOLanza();
            return ResponseEntity.ok(new CobroDelPedido(c.getId(), c.getCobradoEn(), c.getTotal(), c.getTotalMedios(),
                    c.getTotalCuentas(), c.estaRevertido(), List.of()));
        });
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
                                    @AuthenticationPrincipal String actorId, Authentication autenticacion) {
        boolean pideDescuento = cuerpo.descuentos() != null && !cuerpo.descuentos().isEmpty();
        // US-034: un descuento exige su propio permiso (caja.descontar), ademas de caja.crear para cobrar.
        if (pideDescuento && autenticacion.getAuthorities().stream().noneMatch(a -> "PERM_caja.descontar".equals(a.getAuthority()))) {
            return error(HttpStatus.FORBIDDEN, "SIN_PERMISO_PARA_DESCONTAR");
        }
        return protegido(() -> {
            // Un elemento nulo llega al caso de uso como un pago sin medio: lo rechaza el, no un NullPointerException.
            List<PagoSolicitado> pagos = cuerpo.pagos() == null ? List.of() : cuerpo.pagos().stream()
                    .map(p -> p == null ? null : new PagoSolicitado(p.medio(), p.monto() == null ? 0L : p.monto(), p.referencia()))
                    .collect(java.util.stream.Collectors.toList());
            List<DescuentoSolicitado> descuentos = !pideDescuento ? List.of() : cuerpo.descuentos().stream()
                    .map(d -> d == null ? null : new DescuentoSolicitado(d.lineaId(), d.tipo(), d.valor() == null ? 0L : d.valor(),
                            d.motivoId(), d.observacion()))
                    .collect(java.util.stream.Collectors.toList());
            Resultado<CobroRegistrado, ErrorCobro> resultado = cobrarPedido.ejecutar(id, cuerpo.lineaIds(), pagos, descuentos, actorId);
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
            case PEDIDO_NO_ENCONTRADO, LINEA_NO_ENCONTRADA, COBRO_NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case PEDIDO_CERRADO, LINEA_YA_COBRADA, LINEA_ANULADA, LINEA_SIN_COMANDAR, SIN_TURNO_ABIERTO,
                 PENSIONISTA_INACTIVO, COBRO_YA_REVERTIDO, TURNO_CERRADO, MESA_OCUPADA -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return error(estado, e.name());
    }

    private static ResponseEntity<?> error(HttpStatus estado, String codigo) {
        return ResponseEntity.status(estado).body(Map.of("error", codigo));
    }
}
