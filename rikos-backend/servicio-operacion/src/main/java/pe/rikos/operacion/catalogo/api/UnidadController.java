package pe.rikos.operacion.catalogo.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.CrearUnidad;
import pe.rikos.operacion.catalogo.aplicacion.ErrorCatalogo;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/unidades")
public class UnidadController {

    private final UnidadMedidaRepositorio unidades;
    private final CrearUnidad crearUnidad;

    public UnidadController(UnidadMedidaRepositorio unidades, CrearUnidad crearUnidad) {
        this.unidades = unidades;
        this.crearUnidad = crearUnidad;
    }

    public record UnidadRequest(@NotBlank String codigo, @NotBlank String nombre, @NotBlank String tipo) {
    }

    public record UnidadRespuesta(String id, String codigo, String nombre, String tipo, boolean activo) {
        static UnidadRespuesta de(UnidadMedida u) {
            return new UnidadRespuesta(u.getId(), u.getCodigo(), u.getNombre(), u.getTipo(), u.isActivo());
        }
    }

    @GetMapping
    public List<UnidadRespuesta> listar() {
        return unidades.findAll().stream().map(UnidadRespuesta::de).toList();
    }

    @Operation(summary = "Crear unidad de medida")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = UnidadRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping
    public ResponseEntity<?> crear(@org.springframework.validation.annotation.Validated @RequestBody UnidadRequest cuerpo) {
        Resultado<UnidadMedida, ErrorCatalogo> resultado =
                crearUnidad.ejecutar(cuerpo.codigo(), cuerpo.nombre(), cuerpo.tipo());
        if (resultado.esExito()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(UnidadRespuesta.de(resultado.valorOLanza()));
        }
        ErrorCatalogo error = ((Resultado.Fracaso<UnidadMedida, ErrorCatalogo>) resultado).error();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", error.name()));
    }
}
