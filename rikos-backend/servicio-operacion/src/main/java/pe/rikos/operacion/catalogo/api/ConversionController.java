package pe.rikos.operacion.catalogo.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.CrearConversion;
import pe.rikos.operacion.catalogo.aplicacion.ErrorCatalogo;
import pe.rikos.operacion.catalogo.infraestructura.ConversionUnidad;
import pe.rikos.operacion.catalogo.infraestructura.ConversionUnidadRepositorio;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
public class ConversionController {

    private final CrearConversion crearConversion;
    private final ConversionUnidadRepositorio conversiones;

    public ConversionController(CrearConversion crearConversion, ConversionUnidadRepositorio conversiones) {
        this.crearConversion = crearConversion;
        this.conversiones = conversiones;
    }

    public record ConversionRequest(String productoId, String unidadOrigenId, String unidadDestinoId,
                                     BigDecimal factor) {
    }

    public record ConversionRespuesta(String id, String unidadOrigenId, String unidadDestinoId, BigDecimal factor) {
        static ConversionRespuesta de(ConversionUnidad c) {
            return new ConversionRespuesta(c.getId(), c.getUnidadOrigen().getId(), c.getUnidadDestino().getId(),
                    c.getFactor());
        }
    }

    // @Transactional: open-in-view esta apagado y ConversionRespuesta.de()
    // lee unidadOrigen/unidadDestino (LAZY) -- ver la nota igual en
    // ProductoController.
    @GetMapping("/productos/{productoId}/conversiones")
    @Transactional(readOnly = true)
    public List<ConversionRespuesta> listar(@PathVariable String productoId) {
        return conversiones.findByProductoId(productoId).stream().map(ConversionRespuesta::de).toList();
    }

    @PostMapping("/conversiones")
    @Transactional
    public ResponseEntity<?> crear(@RequestBody ConversionRequest cuerpo,
                                    @AuthenticationPrincipal String actorId) {
        Resultado<ConversionUnidad, ErrorCatalogo> resultado = crearConversion.ejecutar(cuerpo.productoId(),
                cuerpo.unidadOrigenId(), cuerpo.unidadDestinoId(), cuerpo.factor(), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(ConversionRespuesta.de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorCatalogo> resultado) {
        ErrorCatalogo error = ((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error();
        HttpStatus estado = switch (error) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case PAR_UNIDADES_DUPLICADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}
