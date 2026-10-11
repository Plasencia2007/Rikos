package pe.rikos.operacion.catalogo.api;

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
import pe.rikos.operacion.catalogo.aplicacion.CrearPresentacion;
import pe.rikos.operacion.catalogo.aplicacion.ErrorCatalogo;
import pe.rikos.operacion.catalogo.infraestructura.PresentacionCompra;
import pe.rikos.operacion.catalogo.infraestructura.PresentacionCompraRepositorio;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
public class PresentacionController {

    private final CrearPresentacion crearPresentacion;
    private final PresentacionCompraRepositorio presentaciones;

    public PresentacionController(CrearPresentacion crearPresentacion,
                                   PresentacionCompraRepositorio presentaciones) {
        this.crearPresentacion = crearPresentacion;
        this.presentaciones = presentaciones;
    }

    public record PresentacionRequest(String productoId, String nombre, String unidadId, BigDecimal factor,
                                       boolean esPredeterminada) {
    }

    public record PresentacionRespuesta(String id, String nombre, String unidadId, BigDecimal factor,
                                         boolean esPredeterminada, boolean activo) {
        static PresentacionRespuesta de(PresentacionCompra p) {
            return new PresentacionRespuesta(p.getId(), p.getNombre(), p.getUnidad().getId(),
                    p.getFactorAUnidadBase(), p.isEsPredeterminada(), p.isActivo());
        }
    }

    // @Transactional: open-in-view esta apagado y PresentacionRespuesta.de()
    // lee unidad (LAZY) -- ver la nota igual en ProductoController.
    @GetMapping("/productos/{productoId}/presentaciones")
    @Transactional(readOnly = true)
    public List<PresentacionRespuesta> listar(@PathVariable String productoId) {
        return presentaciones.findByProductoId(productoId).stream().map(PresentacionRespuesta::de).toList();
    }

    @Operation(summary = "Crear presentacion de compra")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = PresentacionRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/presentaciones")
    @Transactional
    public ResponseEntity<?> crear(@RequestBody PresentacionRequest cuerpo,
                                    @AuthenticationPrincipal String actorId) {
        Resultado<PresentacionCompra, ErrorCatalogo> resultado = crearPresentacion.ejecutar(cuerpo.productoId(),
                cuerpo.nombre(), cuerpo.unidadId(), cuerpo.factor(), cuerpo.esPredeterminada(), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(PresentacionRespuesta.de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorCatalogo> resultado) {
        ErrorCatalogo error = ((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error();
        HttpStatus estado = switch (error) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case PREDETERMINADA_DUPLICADA -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}
