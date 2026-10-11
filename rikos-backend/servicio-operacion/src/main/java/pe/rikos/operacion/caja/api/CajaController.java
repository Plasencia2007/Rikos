package pe.rikos.operacion.caja.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.aplicacion.AbrirTurno;
import pe.rikos.operacion.caja.aplicacion.CerrarTurno;
import pe.rikos.operacion.caja.aplicacion.CierreRegistrado;
import pe.rikos.operacion.caja.aplicacion.ConsultaCierre;
import pe.rikos.operacion.caja.aplicacion.ResumenDeTurno;
import pe.rikos.operacion.caja.infraestructura.CierreCaja;
import pe.rikos.operacion.caja.aplicacion.ConsultaCajas;
import pe.rikos.operacion.caja.aplicacion.CrearCaja;
import pe.rikos.operacion.caja.aplicacion.ErrorCaja;
import pe.rikos.operacion.caja.aplicacion.FilaDeCaja;
import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * US-032: las cajas del local activo y sus turnos. Ver, caja.ver; abrir un turno, caja.crear (cajero y
 * administradores; el mozo no cobra); crear una caja, caja.administrar. El local sale de la sesion, nunca del cuerpo.
 */
@RestController
public class CajaController {

    public record TurnoRespuesta(String id, String cajaId, String cajeroId, String cajeroNombre, long fondoInicial,
                                 OffsetDateTime abiertoEn, String estado) {
    }

    public record CajaRespuesta(String id, String nombre, boolean activo, TurnoRespuesta turnoAbierto) {
    }

    public record CajaRequest(String nombre) {
    }

    public record TurnoRequest(Long fondoInicial) {
    }

    public record CierreRequest(Long efectivoContado, String observacion) {
    }

    /** Lo que ve el cajero ANTES de contar (RN-067). Centimos. */
    public record ResumenRespuesta(TurnoRespuesta turno, long fondoInicial, long efectivoCobrado, long efectivoEsperado,
                                   long totalYape, long totalPlin, long totalTarjeta, long cobros) {
    }

    /** El cierre: diferencia = contado - esperado (negativa si falta dinero); alerta desde S/ 0,01 en cualquier sentido. */
    public record CierreRespuesta(String id, String turnoId, long efectivoEsperado, long efectivoContado, long diferencia,
                                  long totalYape, long totalPlin, long totalTarjeta, String observacion,
                                  OffsetDateTime cerradoEn, boolean alerta) {
    }

    private final ConsultaCajas consulta;
    private final CrearCaja crearCaja;
    private final AbrirTurno abrirTurno;
    private final CerrarTurno cerrarTurno;
    private final ConsultaCierre consultaCierre;

    public CajaController(ConsultaCajas consulta, CrearCaja crearCaja, AbrirTurno abrirTurno, CerrarTurno cerrarTurno,
                          ConsultaCierre consultaCierre) {
        this.cerrarTurno = cerrarTurno;
        this.consultaCierre = consultaCierre;
        this.consulta = consulta;
        this.crearCaja = crearCaja;
        this.abrirTurno = abrirTurno;
    }

    private static TurnoRespuesta turno(TurnoCaja t, String cajeroNombre) {
        return new TurnoRespuesta(t.getId(), t.getCajaId(), t.getCajeroId(), cajeroNombre, t.getFondoInicial(),
                t.getAbiertoEn(), t.getEstado());
    }

    private static CajaRespuesta caja(FilaDeCaja f) {
        return new CajaRespuesta(f.caja().getId(), f.caja().getNombre(), f.caja().isActivo(),
                f.turnoAbierto() == null ? null : turno(f.turnoAbierto(), f.cajeroNombre()));
    }

    @Operation(summary = "Las cajas del local activo, cada una con su turno abierto (Caja 1 se crea sola la primera vez)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CajaRespuesta[].class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping("/cajas")
    public ResponseEntity<?> listar() {
        Resultado<List<FilaDeCaja>, ErrorCaja> resultado = consulta.listar();
        return resultado.esExito()
                ? ResponseEntity.ok(resultado.valorOLanza().stream().map(CajaController::caja).toList())
                : errorDe(resultado);
    }

    @Operation(summary = "Crear una caja en el local activo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CajaRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/cajas")
    public ResponseEntity<?> crear(@RequestBody CajaRequest cuerpo, @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<Caja, ErrorCaja> resultado = crearCaja.ejecutar(cuerpo.nombre(), actorId);
            return resultado.esExito()
                    ? ResponseEntity.status(HttpStatus.CREATED)
                            .body(new CajaRespuesta(resultado.valorOLanza().getId(), resultado.valorOLanza().getNombre(),
                                    resultado.valorOLanza().isActivo(), null))
                    : errorDe(resultado);
        });
    }

    @Operation(summary = "Abrir el turno de la caja con su fondo inicial (en centimos)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = TurnoRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/cajas/{id}/turnos")
    public ResponseEntity<?> abrirTurno(@PathVariable String id, @RequestBody TurnoRequest cuerpo,
                                        @AuthenticationPrincipal String actorId) {
        return protegido(() -> {
            Resultado<TurnoCaja, ErrorCaja> resultado = abrirTurno.ejecutar(id, cuerpo.fondoInicial(), actorId);
            return resultado.esExito()
                    ? ResponseEntity.status(HttpStatus.CREATED).body(turno(resultado.valorOLanza(), null))
                    : errorDe(resultado);
        });
    }

    @Operation(summary = "El turno abierto del cajero que pregunta (204 si no tiene)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TurnoRespuesta.class))),
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping("/turnos/actual")
    @Transactional(readOnly = true)
    public ResponseEntity<?> turnoActual(@AuthenticationPrincipal String actorId) {
        var resultado = consulta.turnoActual(actorId);
        if (resultado.esFracaso()) {
            return errorDe(resultado);
        }
        return resultado.valorOLanza().map(t -> ResponseEntity.ok((Object) turno(t, null)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private static CierreRespuesta cierre(CierreCaja c, boolean alerta) {
        return new CierreRespuesta(c.getId(), c.getTurnoCajaId(), c.getEfectivoEsperado(), c.getEfectivoContado(),
                c.getDiferencia(), c.getTotalYape(), c.getTotalPlin(), c.getTotalTarjeta(), c.getObservacion(), c.getCerradoEn(),
                alerta);
    }

    @Operation(summary = "El resumen del turno abierto ANTES de contar: fondo, cobrado por medio y efectivo esperado")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ResumenRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping("/turnos/actual/resumen")
    public ResponseEntity<?> resumen(@AuthenticationPrincipal String actorId) {
        Resultado<ResumenDeTurno, ErrorCaja> resultado = consultaCierre.previo(actorId);
        if (resultado.esFracaso()) {
            return errorDe(resultado);
        }
        ResumenDeTurno r = resultado.valorOLanza();
        return ResponseEntity.ok(new ResumenRespuesta(turno(r.turno(), null), r.fondoInicial(), r.efectivoCobrado(),
                r.efectivoEsperado(), r.totalYape(), r.totalPlin(), r.totalTarjeta(), r.cobros()));
    }

    @Operation(summary = "Cerrar el turno del cajero con el efectivo contado (en centimos); una diferencia alerta pero no bloquea")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CierreRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/turnos/actual/cierre")
    public ResponseEntity<?> cerrar(@RequestBody CierreRequest cuerpo, @AuthenticationPrincipal String actorId) {
        Resultado<CierreRegistrado, ErrorCaja> resultado = cerrarTurno.ejecutar(actorId, cuerpo.efectivoContado(),
                cuerpo.observacion());
        if (resultado.esFracaso()) {
            return errorDe(resultado);
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cierre(resultado.valorOLanza().cierre(), resultado.valorOLanza().alerta()));
    }

    @Operation(summary = "El cierre ya registrado de un turno")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CierreRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping("/turnos/{id}/cierre")
    public ResponseEntity<?> cierreDelTurno(@PathVariable String id) {
        Resultado<CierreCaja, ErrorCaja> resultado = consultaCierre.deTurno(id);
        return resultado.esExito() ? ResponseEntity.ok(cierre(resultado.valorOLanza(), resultado.valorOLanza().getDiferencia() != 0))
                : errorDe(resultado);
    }

    /**
     * Dos aperturas a la vez chocan en los indices unicos parciales de la base: el que pierde recibe el mismo error que
     * si hubiera llegado segundo, no un 500.
     */
    private ResponseEntity<?> protegido(java.util.function.Supplier<ResponseEntity<?>> accion) {
        try {
            return accion.get();
        } catch (DataIntegrityViolationException e) {
            String mensaje = String.valueOf(e.getMessage());
            if (mensaje.contains("turno_caja_un_abierto_por_caja")) {
                return error(HttpStatus.CONFLICT, ErrorCaja.CAJA_CON_TURNO_ABIERTO.name());
            }
            if (mensaje.contains("turno_caja_un_abierto_por_cajero")) {
                return error(HttpStatus.CONFLICT, ErrorCaja.CAJERO_CON_TURNO_ABIERTO.name());
            }
            if (mensaje.contains("caja_nombre_unico_por_local")) {
                return error(HttpStatus.CONFLICT, ErrorCaja.NOMBRE_REPETIDO.name());
            }
            throw e;
        }
    }

    private static ResponseEntity<?> errorDe(Resultado<?, ErrorCaja> resultado) {
        ErrorCaja e = ((Resultado.Fracaso<?, ErrorCaja>) resultado).error();
        HttpStatus estado = switch (e) {
            case CAJA_NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
            case CAJA_CON_TURNO_ABIERTO, CAJERO_CON_TURNO_ABIERTO, CAJA_INACTIVA, NOMBRE_REPETIDO, SIN_TURNO_ABIERTO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return error(estado, e.name());
    }

    private static ResponseEntity<?> error(HttpStatus estado, String codigo) {
        return ResponseEntity.status(estado).body(Map.of("error", codigo));
    }
}
