package pe.rikos.operacion.venta.api;

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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.venta.aplicacion.ActualizarMesa;
import pe.rikos.operacion.venta.aplicacion.BloquearMesa;
import pe.rikos.operacion.venta.aplicacion.CambiarEstadoMesa;
import pe.rikos.operacion.venta.aplicacion.ConsultaMesas;
import pe.rikos.operacion.venta.aplicacion.CrearMesa;
import pe.rikos.operacion.venta.aplicacion.DatosMesa;
import pe.rikos.operacion.venta.aplicacion.ErrorMesa;
import pe.rikos.operacion.venta.aplicacion.FilaDelMapa;
import pe.rikos.operacion.venta.aplicacion.LiberarMesa;
import pe.rikos.operacion.venta.aplicacion.SepararMesa;
import pe.rikos.operacion.venta.aplicacion.UnirMesas;
import pe.rikos.operacion.venta.infraestructura.Mesa;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * US-027: las mesas del local de la sesion. Ver el mapa, venta.ver; bloquear
 * y liberar, venta.bloquear_mesa; crear, editar y activar, venta.administrar.
 * El local nunca viaja en la peticion: es siempre el activo del token.
 */
@RestController
@RequestMapping("/mesas")
public class MesaController {

    private final ConsultaMesas consulta;
    private final CrearMesa crearMesa;
    private final ActualizarMesa actualizarMesa;
    private final CambiarEstadoMesa cambiarEstado;
    private final BloquearMesa bloquearMesa;
    private final LiberarMesa liberarMesa;
    private final UnirMesas unirMesas;
    private final SepararMesa separarMesa;

    public MesaController(ConsultaMesas consulta, CrearMesa crearMesa, ActualizarMesa actualizarMesa,
                          CambiarEstadoMesa cambiarEstado, BloquearMesa bloquearMesa, LiberarMesa liberarMesa,
                          UnirMesas unirMesas, SepararMesa separarMesa) {
        this.unirMesas = unirMesas;
        this.separarMesa = separarMesa;
        this.consulta = consulta;
        this.crearMesa = crearMesa;
        this.actualizarMesa = actualizarMesa;
        this.cambiarEstado = cambiarEstado;
        this.bloquearMesa = bloquearMesa;
        this.liberarMesa = liberarMesa;
    }

    /** salonId, forma y medidas son opcionales: sin salon va al "Principal", sin forma es cuadrada. */
    public record MesaRequest(String numero, String nombre, Integer capacidad, String salonId, String forma,
                              Boolean vip, Boolean zonaExterior, Boolean zonaFumadores, Integer ancho, Integer alto) {
        DatosMesa aDatos() {
            return new DatosMesa(numero, nombre, capacidad, salonId, forma, Boolean.TRUE.equals(vip),
                    Boolean.TRUE.equals(zonaExterior), Boolean.TRUE.equals(zonaFumadores), ancho, alto);
        }
    }

    public record BloqueoRequest(String motivo) {
    }

    /** estado: libre | ocupada | unida | pendiente_cierre | bloqueada. */
    public record MesaRespuesta(String id, String numero, String nombre, Integer capacidad, String estado,
                                String motivoBloqueo, String mesaPrincipalId, boolean activo, String salonId,
                                String forma, boolean vip, boolean zonaExterior, boolean zonaFumadores, Integer posX,
                                Integer posY, int ancho, int alto, java.time.OffsetDateTime abiertaEn, boolean tomandoPedido) {
    }

    private static MesaRespuesta de(FilaDelMapa f) {
        Mesa m = f.mesa();
        return new MesaRespuesta(m.getId(), m.getNumero(), m.getNombre(), m.getCapacidad(), f.estado().codigo(),
                m.getMotivoBloqueo(), m.getMesaPrincipalId(), m.isActivo(), m.getSalonId(), m.getForma(), m.isVip(),
                m.isZonaExterior(), m.isZonaFumadores(), m.getPosX(), m.getPosY(), m.getAncho(), m.getAlto(),
                f.abiertaEn(), f.tomandoPedido());
    }

    @Operation(summary = "El mapa de mesas del local activo (todas=true incluye las inactivas)")
    @ApiResponses({
            @ApiResponse(responseCode = "200"),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<?> mapa(@RequestParam(defaultValue = "false") boolean todas) {
        Resultado<List<FilaDelMapa>, ErrorMesa> resultado = todas ? consulta.todas() : consulta.mapa();
        if (!resultado.esExito()) {
            return errorDe(resultado);
        }
        return ResponseEntity.ok(resultado.valorOLanza().stream().map(MesaController::de).toList());
    }

    @Operation(summary = "Crear una mesa en el local activo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = MesaRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody MesaRequest cuerpo, @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Mesa, ErrorMesa> resultado = crearMesa.ejecutar(
                    cuerpo.aDatos(), actorId);
            return resultado.esExito() ? ResponseEntity.status(HttpStatus.CREATED).body(unaMesa(resultado.valorOLanza()))
                    : errorDe(resultado);
        });
    }

    @Operation(summary = "Editar el numero, nombre y capacidad de una mesa")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MesaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody MesaRequest cuerpo,
                                        @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Mesa, ErrorMesa> resultado = actualizarMesa.ejecutar(id,
                    cuerpo.aDatos(), actorId);
            return resultado.esExito() ? ResponseEntity.ok(unaMesa(resultado.valorOLanza())) : errorDe(resultado);
        });
    }

    @Operation(summary = "Desactivar una mesa (sale del mapa, conserva su historial)")
    @PostMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Mesa, ErrorMesa> resultado = cambiarEstado.ejecutar(id, false, actorId);
            return resultado.esExito() ? ResponseEntity.ok(unaMesa(resultado.valorOLanza())) : errorDe(resultado);
        });
    }

    @Operation(summary = "Reactivar una mesa")
    @PostMapping("/{id}/activar")
    public ResponseEntity<?> activar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Mesa, ErrorMesa> resultado = cambiarEstado.ejecutar(id, true, actorId);
            return resultado.esExito() ? ResponseEntity.ok(unaMesa(resultado.valorOLanza())) : errorDe(resultado);
        });
    }

    @Operation(summary = "Bloquear una mesa libre, con motivo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MesaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/{id}/bloquear")
    public ResponseEntity<?> bloquear(@PathVariable String id, @RequestBody BloqueoRequest cuerpo,
                                      @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Mesa, ErrorMesa> resultado = bloquearMesa.ejecutar(id, cuerpo.motivo(), actorId);
            return resultado.esExito() ? ResponseEntity.ok(unaMesa(resultado.valorOLanza())) : errorDe(resultado);
        });
    }

    public record UnirRequest(String principalId) {
    }

    @Operation(summary = "Unir esta mesa libre a una principal (venta.modificar); el pedido se opera en conjunto")
    @PostMapping("/{id}/unir")
    public ResponseEntity<?> unir(@PathVariable String id, @RequestBody UnirRequest cuerpo,
                                  @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Mesa, ErrorMesa> resultado = unirMesas.ejecutar(id, cuerpo.principalId(), actorId);
            return resultado.esExito() ? ResponseEntity.ok(unaMesa(resultado.valorOLanza())) : errorDe(resultado);
        });
    }

    @Operation(summary = "Separar una mesa unida (venta.modificar): queda libre e independiente")
    @PostMapping("/{id}/separar")
    public ResponseEntity<?> separar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Mesa, ErrorMesa> resultado = separarMesa.ejecutar(id, actorId);
            return resultado.esExito() ? ResponseEntity.ok(unaMesa(resultado.valorOLanza())) : errorDe(resultado);
        });
    }

    @Operation(summary = "Liberar una mesa bloqueada")
    @PostMapping("/{id}/liberar")
    public ResponseEntity<?> liberar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Mesa, ErrorMesa> resultado = liberarMesa.ejecutar(id, actorId);
            return resultado.esExito() ? ResponseEntity.ok(unaMesa(resultado.valorOLanza())) : errorDe(resultado);
        });
    }


    /**
     * Las escrituras no van dentro de una transaccion del controlador: cada caso de uso confirma la suya, asi
     * un choque de concurrencia (dos personas actuando a la vez sobre la misma mesa) sale AQUI, ya cerrado,
     * y se puede contestar con un 409 claro en vez de un 500. Una violacion de integridad que no sea del
     * numero de mesa no se disfraza: sigue su camino.
     */
    private ResponseEntity<?> protegido(Supplier<ResponseEntity<?>> accion) {
        try {
            return accion.get();
        } catch (ObjectOptimisticLockingFailureException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "MESA_MODIFICADA"));
        } catch (DataIntegrityViolationException e) {
            if (String.valueOf(e.getMessage()).contains("mesa_numero_unico_por_local")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", ErrorMesa.NUMERO_DUPLICADO.name()));
            }
            throw e;
        }
    }
    /** Devuelve la mesa con su estado recien derivado (la consulta ya sabe hacerlo). */
    private MesaRespuesta unaMesa(Mesa mesa) {
        return consulta.todas().valorOLanza().stream().filter(f -> f.mesa().getId().equals(mesa.getId()))
                .findFirst().map(MesaController::de).orElseThrow();
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorMesa> resultado) {
        ErrorMesa e = ((Resultado.Fracaso<?, ErrorMesa>) resultado).error();
        HttpStatus estado = switch (e) {
            case NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
            case NUMERO_DUPLICADO, YA_BLOQUEADA, NO_BLOQUEADA, MESA_NO_LIBRE, YA_UNIDA, NO_ESTA_UNIDA,
                 TIENE_MESAS_UNIDAS, TIENE_PEDIDO_ABIERTO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", e.name()));
    }
}
