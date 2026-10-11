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
import pe.rikos.operacion.catalogo.aplicacion.ActualizarMotivo;
import pe.rikos.operacion.catalogo.aplicacion.CambiarEstadoMotivo;
import pe.rikos.operacion.catalogo.aplicacion.CrearMotivo;
import pe.rikos.operacion.catalogo.aplicacion.ErrorMotivo;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

import java.util.List;
import java.util.Map;

/** US-012: motivos configurables (ajuste de inventario, anulacion, descuento, rechazo de abono, sobrante de produccion, reapertura). */
@RestController
@RequestMapping("/motivos")
public class MotivoController {

    private final CrearMotivo crearMotivo;
    private final ActualizarMotivo actualizarMotivo;
    private final CambiarEstadoMotivo cambiarEstadoMotivo;
    private final MotivoRepositorio motivos;

    public MotivoController(CrearMotivo crearMotivo, ActualizarMotivo actualizarMotivo,
                             CambiarEstadoMotivo cambiarEstadoMotivo, MotivoRepositorio motivos) {
        this.crearMotivo = crearMotivo;
        this.actualizarMotivo = actualizarMotivo;
        this.cambiarEstadoMotivo = cambiarEstadoMotivo;
        this.motivos = motivos;
    }

    public record MotivoRequest(@NotBlank String tipo, @NotBlank String descripcion, boolean requiereObservacion) {
    }

    public record ActualizarMotivoRequest(@NotBlank String descripcion, boolean requiereObservacion) {
    }

    public record MotivoRespuesta(String id, String tipo, String descripcion, boolean requiereObservacion,
                                   boolean activo) {
        static MotivoRespuesta de(Motivo m) {
            return new MotivoRespuesta(m.getId(), m.getTipo(), m.getDescripcion(), m.isRequiereObservacion(),
                    m.isActivo());
        }
    }

    @GetMapping
    public List<MotivoRespuesta> listar(@RequestParam(required = false) String tipo) {
        List<Motivo> encontrados = tipo != null
                ? motivos.findByTipoOrderByDescripcionAsc(tipo)
                : motivos.findAllByOrderByTipoAscDescripcionAsc();
        return encontrados.stream().map(MotivoRespuesta::de).toList();
    }

    @Operation(summary = "Crear motivo")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = MotivoRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping
    public ResponseEntity<?> crear(@org.springframework.validation.annotation.Validated @RequestBody MotivoRequest cuerpo) {
        Resultado<Motivo, ErrorMotivo> resultado =
                crearMotivo.ejecutar(cuerpo.tipo(), cuerpo.descripcion(), cuerpo.requiereObservacion());
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(MotivoRespuesta.de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    @Operation(summary = "Actualizar motivo")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = MotivoRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id,
            @org.springframework.validation.annotation.Validated @RequestBody ActualizarMotivoRequest cuerpo) {
        Resultado<Motivo, ErrorMotivo> resultado =
                actualizarMotivo.ejecutar(id, cuerpo.descripcion(), cuerpo.requiereObservacion());
        return resultado.esExito()
                ? ResponseEntity.ok(MotivoRespuesta.de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    @Operation(summary = "Activar motivo")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/{id}/activar")
    public ResponseEntity<?> activar(@PathVariable String id) {
        Resultado<Motivo, ErrorMotivo> resultado = cambiarEstadoMotivo.ejecutar(id, true);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    @Operation(summary = "Desactivar motivo")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable String id) {
        Resultado<Motivo, ErrorMotivo> resultado = cambiarEstadoMotivo.ejecutar(id, false);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorMotivo> resultado) {
        ErrorMotivo error = ((Resultado.Fracaso<?, ErrorMotivo>) resultado).error();
        HttpStatus estado = switch (error) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case DESCRIPCION_DUPLICADA, ULTIMO_DE_DISPONIBILIDAD -> HttpStatus.CONFLICT;
            case TIPO_INVALIDO -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}
