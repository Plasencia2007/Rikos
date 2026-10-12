package pe.rikos.operacion.pensionistas.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.pensionistas.aplicacion.EmitirPreTicket;
import pe.rikos.operacion.pensionistas.aplicacion.ErrorPreTicket;
import pe.rikos.operacion.pensionistas.aplicacion.FalloPreTicket;
import pe.rikos.operacion.pensionistas.aplicacion.PreTicketEmitido;
import pe.rikos.operacion.pensionistas.aplicacion.SolicitudEmitirPreTicket;

import java.util.HashMap;
import java.util.Map;

/**
 * US-019: Terminal de autoservicio del pensionista (kiosco).
 * Emite pre-tickets de atencion verificando DNI y PIN, sin cargo a cuenta corriente.
 */
@RestController
@RequestMapping("/kiosco")
public class KioscoController {

    private final EmitirPreTicket emitirPreTicket;

    public KioscoController(EmitirPreTicket emitirPreTicket) {
        this.emitirPreTicket = emitirPreTicket;
    }

    public record EmitirPreTicketRequest(
            String localCodigo,
            String dni,
            String pin,
            String servicioManual
    ) {
    }

    @Operation(summary = "Emitir pre-ticket de autoservicio para pensionista")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PreTicketEmitido.class))),
            @ApiResponse(responseCode = "400", description = "DNI invalido o local no encontrado"),
            @ApiResponse(responseCode = "401", description = "DNI o PIN incorrecto"),
            @ApiResponse(responseCode = "403", description = "Pensionista o local inactivo"),
            @ApiResponse(responseCode = "409", description = "Fuera de horario de atencion"),
            @ApiResponse(responseCode = "423", description = "Pensionista bloqueado temporalmente por intentos fallidos")
    })
    @PostMapping("/pre-ticket")
    public ResponseEntity<?> emitirPreTicket(@RequestBody EmitirPreTicketRequest peticion) {
        SolicitudEmitirPreTicket solicitud = new SolicitudEmitirPreTicket(
                peticion.localCodigo(),
                peticion.dni(),
                peticion.pin(),
                peticion.servicioManual()
        );

        Resultado<PreTicketEmitido, FalloPreTicket> resultado = emitirPreTicket.ejecutar(solicitud);

        if (resultado.esExito()) {
            return ResponseEntity.ok(resultado.valorOLanza());
        }

        FalloPreTicket fallo = ((Resultado.Fracaso<PreTicketEmitido, FalloPreTicket>) resultado).error();
        HttpStatus estado = switch (fallo.error()) {
            case CREDENCIALES_INVALIDAS -> HttpStatus.UNAUTHORIZED;
            case PENSIONISTA_BLOQUEADO -> HttpStatus.LOCKED;
            case PENSIONISTA_INACTIVO, LOCAL_INACTIVO -> HttpStatus.FORBIDDEN;
            case DNI_INVALIDO, LOCAL_NO_ENCONTRADO -> HttpStatus.BAD_REQUEST;
            case FUERA_DE_HORARIO -> HttpStatus.CONFLICT;
        };

        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("error", fallo.error().name());
        if (fallo.segundosRestantes() != null) {
            cuerpo.put("segundosRestantes", fallo.segundosRestantes());
        }
        if (fallo.intentosRestantes() != null) {
            cuerpo.put("intentosRestantes", fallo.intentosRestantes());
        }

        return ResponseEntity.status(estado).body(cuerpo);
    }
}
