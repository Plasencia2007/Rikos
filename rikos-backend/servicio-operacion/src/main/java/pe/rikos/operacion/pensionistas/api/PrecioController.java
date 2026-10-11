package pe.rikos.operacion.pensionistas.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.web.ErrorRespuesta;
import pe.rikos.operacion.pensionistas.aplicacion.AvisoPrecio;
import pe.rikos.operacion.pensionistas.aplicacion.Comida;
import pe.rikos.operacion.pensionistas.aplicacion.ErrorPrecio;
import pe.rikos.operacion.pensionistas.aplicacion.MotorDePrecios;
import pe.rikos.operacion.pensionistas.aplicacion.PrecioCalculado;
import pe.rikos.operacion.pensionistas.aplicacion.SolicitudDePrecio;
import pe.rikos.operacion.pensionistas.aplicacion.TipoLinea;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Consulta del motor de precios (sprint 5, 3.5). Solo lee: no escribe nada,
 * asi que basta pensionistas.ver. La caja (sprint 7) usara el motor
 * directamente; este endpoint sirve para probar las reglas y para que la
 * pantalla de planes muestre "asi se cobraria".
 */
@RestController
@RequestMapping("/precios")
public class PrecioController {

    private final MotorDePrecios motor;

    public PrecioController(MotorDePrecios motor) {
        this.motor = motor;
    }

    /**
     * tipo: menu_del_dia | menu_extra | agregado. comida: desayuno | almuerzo | cena.
     * planId: el plan del pensionista con ticket; null = cliente normal.
     * precioNormal en centimos. fecha null = hoy. segundoDelDia y paraLlevar ausentes = no.
     */
    public record CalculoRequest(String tipo, String comida, Long precioNormal, String planId, Boolean segundoDelDia,
                                 Boolean paraLlevar, LocalDate fecha) {
    }

    /** precio, taper y total en centimos; null = no calculable (ver avisos). */
    public record CalculoRespuesta(Long precio, Long taper, Long total, List<String> avisos, List<String> explicacion) {
        static CalculoRespuesta de(PrecioCalculado p) {
            return new CalculoRespuesta(p.precio(), p.taper(), p.total(),
                    p.avisos().stream().map(AvisoPrecio::name).toList(), p.explicacion());
        }
    }

    @Operation(summary = "Calcular el precio de una línea")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CalculoRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/calculo")
    public ResponseEntity<?> calcular(@RequestBody CalculoRequest cuerpo) {
        TipoLinea tipo = tipoDe(cuerpo.tipo());
        Comida comida = Comida.desde(cuerpo.comida()).orElse(null);
        if (tipo == null || comida == null) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", "SOLICITUD_INVALIDA"));
        }
        if (cuerpo.precioNormal() != null && cuerpo.precioNormal() <= 0) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", "PRECIO_INVALIDO"));
        }
        Resultado<PrecioCalculado, ErrorPrecio> resultado = motor.calcular(new SolicitudDePrecio(tipo, comida,
                cuerpo.precioNormal(), cuerpo.planId(), Boolean.TRUE.equals(cuerpo.segundoDelDia()), Boolean.TRUE.equals(cuerpo.paraLlevar()),
                cuerpo.fecha()));
        if (resultado.esExito()) {
            return ResponseEntity.ok(CalculoRespuesta.de(resultado.valorOLanza()));
        }
        ErrorPrecio error = ((Resultado.Fracaso<?, ErrorPrecio>) resultado).error();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", error.name()));
    }

    private static TipoLinea tipoDe(String codigo) {
        if (codigo == null) {
            return null;
        }
        return switch (codigo) {
            case "menu_del_dia" -> TipoLinea.MENU_DEL_DIA;
            case "menu_extra" -> TipoLinea.MENU_EXTRA;
            case "agregado" -> TipoLinea.AGREGADO;
            default -> null;
        };
    }
}
