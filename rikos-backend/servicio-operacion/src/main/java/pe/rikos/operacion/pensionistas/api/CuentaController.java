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
import pe.rikos.operacion.catalogo.motivos.ConsultaMotivos;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.pensionistas.aplicacion.CargarSaldoInicial;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaCuenta;
import pe.rikos.operacion.pensionistas.aplicacion.ErrorCuenta;
import pe.rikos.operacion.pensionistas.aplicacion.FijarTopeDeCredito;
import pe.rikos.operacion.pensionistas.aplicacion.RegistrarAjuste;
import pe.rikos.operacion.pensionistas.aplicacion.ResumenDeCuenta;
import pe.rikos.operacion.pensionistas.aplicacion.Signo;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuenta;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * US-018: la cuenta corriente del pensionista. Verla, pensionistas.ver; ajustar
 * y fijar el tope, pensionistas.modificar; cargar el saldo inicial,
 * pensionistas.administrar (oficina central). No hay endpoint para consumir ni
 * abonar aqui: los consumos los carga la caja (sprint 7) y los abonos son la
 * historia US-019.
 *
 * El tope es un recordatorio: ninguna respuesta lo trata como limite.
 */
@RestController
@RequestMapping("/pensionistas/{id}")
public class CuentaController {

    private final ConsultaCuenta consulta;
    private final CargarSaldoInicial cargarSaldoInicial;
    private final RegistrarAjuste registrarAjuste;
    private final FijarTopeDeCredito fijarTope;
    private final LocalRepositorio locales;
    private final UsuarioRepositorio usuarios;
    private final ConsultaMotivos motivos;

    public CuentaController(ConsultaCuenta consulta, CargarSaldoInicial cargarSaldoInicial,
                            RegistrarAjuste registrarAjuste, FijarTopeDeCredito fijarTope, LocalRepositorio locales,
                            UsuarioRepositorio usuarios, ConsultaMotivos motivos) {
        this.consulta = consulta;
        this.cargarSaldoInicial = cargarSaldoInicial;
        this.registrarAjuste = registrarAjuste;
        this.fijarTope = fijarTope;
        this.locales = locales;
        this.usuarios = usuarios;
        this.motivos = motivos;
    }

    /** signo: cargo | credito. importe en centimos, siempre mayor que cero. */
    public record SaldoInicialRequest(long importe, String signo, LocalDate fechaCorte, String descripcion) {
    }

    public record AjusteRequest(long importe, String signo, String motivoId, String observacion) {
    }

    /** limite en centimos; null = que el tope se calcule. */
    public record TopeRequest(Long limite) {
    }

    /** monto null = no se pudo calcular (ver aviso); origen: calculado | fijado. */
    public record TopeRespuesta(Long monto, String origen, String aviso, int comidasContadas) {
    }

    public record ResumenRespuesta(long saldo, long deuda, long saldoAFavor, TopeRespuesta tope,
                                   Long creditoDisponible, boolean superaTope) {
    }

    public record MovimientoRespuesta(String id, String tipo, String signo, long importe, String localId,
                                      String localNombre, String pedidoId, String motivo, String descripcion,
                                      OffsetDateTime ocurridoEn, OffsetDateTime registradoEn,
                                      String registradoPorNombre) {
    }

    public record CuentaRespuesta(ResumenRespuesta resumen, List<MovimientoRespuesta> movimientos) {
    }

    private static ResumenRespuesta resumenDe(ResumenDeCuenta r) {
        var t = r.tope();
        return new ResumenRespuesta(r.saldo(), r.deuda(), r.saldoAFavor(),
                new TopeRespuesta(t.monto(), t.origen().name().toLowerCase(),
                        t.aviso() == null ? null : t.aviso().name(), t.comidasContadas()),
                r.creditoDisponible(), r.superaTope());
    }

    @Operation(summary = "La cuenta de un pensionista: saldo, deuda, tope y movimientos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CuentaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @GetMapping("/cuenta")
    @Transactional(readOnly = true)
    public ResponseEntity<?> cuenta(@PathVariable String id) {
        var resumen = consulta.resumen(id);
        if (resumen.isEmpty()) {
            return error(HttpStatus.NOT_FOUND, ErrorCuenta.NO_ENCONTRADO.name());
        }
        Map<String, String> nombreLocal = new HashMap<>();
        Map<String, String> nombreUsuario = new HashMap<>();
        List<MovimientoRespuesta> movimientos = consulta.movimientos(id).stream()
                .map(m -> movimientoDe(m, nombreLocal, nombreUsuario)).toList();
        return ResponseEntity.ok(new CuentaRespuesta(resumenDe(resumen.get()), movimientos));
    }

    private MovimientoRespuesta movimientoDe(MovimientoCuenta m, Map<String, String> nombreLocal,
                                             Map<String, String> nombreUsuario) {
        String local = nombreLocal.computeIfAbsent(m.getLocalId(),
                lid -> locales.findById(lid).map(Local::getNombre).orElse(null));
        String usuario = nombreUsuario.computeIfAbsent(m.getRegistradoPor(),
                uid -> usuarios.findById(uid).map(Usuario::getNombreUsuario).orElse(null));
        String motivo = m.getMotivoId() == null ? null : motivos.descripcion(m.getMotivoId()).orElse(null);
        return new MovimientoRespuesta(m.getId(), m.getTipo(), m.getSigno(), m.getImporte(), m.getLocalId(), local,
                m.getPedidoId(), motivo, m.getDescripcion(), m.getOcurridoEn(), m.getRegistradoEn(), usuario);
    }

    @Operation(summary = "Cargar el saldo inicial (una sola vez, solo oficina central)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CuentaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/cuenta/saldo-inicial")
    @Transactional
    public ResponseEntity<?> saldoInicial(@PathVariable String id, @RequestBody SaldoInicialRequest cuerpo,
                                          @AuthenticationPrincipal String actorId) {
        var signo = Signo.desde(cuerpo.signo());
        if (signo.isEmpty()) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, "SIGNO_INVALIDO");
        }
        Resultado<MovimientoCuenta, ErrorCuenta> resultado = cargarSaldoInicial.ejecutar(id, cuerpo.importe(),
                signo.get(), cuerpo.fechaCorte(), cuerpo.descripcion(), actorId);
        if (!resultado.esExito()) {
            return errorDe(resultado);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(cuenta(id).getBody());
    }

    @Operation(summary = "Ajustar la cuenta con un cargo o un credito y su motivo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CuentaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/cuenta/ajustes")
    @Transactional
    public ResponseEntity<?> ajustar(@PathVariable String id, @RequestBody AjusteRequest cuerpo,
                                     @AuthenticationPrincipal String actorId) {
        var signo = Signo.desde(cuerpo.signo());
        if (signo.isEmpty()) {
            return error(HttpStatus.UNPROCESSABLE_ENTITY, "SIGNO_INVALIDO");
        }
        Resultado<MovimientoCuenta, ErrorCuenta> resultado = registrarAjuste.ejecutar(id, cuerpo.importe(),
                signo.get(), cuerpo.motivoId(), cuerpo.observacion(), actorId);
        if (!resultado.esExito()) {
            return errorDe(resultado);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(cuenta(id).getBody());
    }

    @Operation(summary = "Fijar el tope de credito a mano, o dejar que se calcule (limite nulo)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CuentaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/tope")
    @Transactional
    public ResponseEntity<?> tope(@PathVariable String id, @RequestBody TopeRequest cuerpo,
                                  @AuthenticationPrincipal String actorId) {
        Resultado<?, ErrorCuenta> resultado = fijarTope.ejecutar(id, cuerpo.limite(), actorId);
        return resultado.esExito() ? cuenta(id) : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorCuenta> resultado) {
        ErrorCuenta e = ((Resultado.Fracaso<?, ErrorCuenta>) resultado).error();
        HttpStatus estado = switch (e) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case SALDO_INICIAL_YA_REGISTRADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return error(estado, e.name());
    }

    private static ResponseEntity<?> error(HttpStatus estado, String codigo) {
        return ResponseEntity.status(estado).body(Map.of("error", codigo));
    }
}
