package pe.rikos.operacion.catalogo.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.ActualizarReceta;
import pe.rikos.operacion.catalogo.aplicacion.CalculadoraCostoReceta;
import pe.rikos.operacion.catalogo.aplicacion.ComposicionReceta.IngredienteEntrada;
import pe.rikos.operacion.catalogo.aplicacion.CrearReceta;
import pe.rikos.operacion.catalogo.aplicacion.FalloReceta;
import pe.rikos.operacion.catalogo.infraestructura.IngredienteReceta;
import pe.rikos.operacion.catalogo.infraestructura.Receta;
import pe.rikos.operacion.catalogo.infraestructura.RecetaRepositorio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * US-010: recetas y subrecetas valorizadas.
 *
 * CA6 / RN-012: sin "recetas.ver_costos" la respuesta sale con los costos
 * en null y costosVisibles=false. Es un filtro de esta capa -- ocultarlo
 * solo en la pantalla dejaria el costo a un clic de las herramientas del
 * navegador.
 *
 * @Transactional en los metodos porque open-in-view esta apagado (DT-049):
 * armar la respuesta lee producto, unidad e ingredientes, todos LAZY.
 */
@RestController
@RequestMapping("/recetas")
public class RecetaController {

    private static final String VER_COSTOS = "PERM_recetas.ver_costos";

    private final CrearReceta crearReceta;
    private final ActualizarReceta actualizarReceta;
    private final RecetaRepositorio recetas;
    private final CalculadoraCostoReceta calculadora;

    public RecetaController(CrearReceta crearReceta, ActualizarReceta actualizarReceta, RecetaRepositorio recetas,
                            CalculadoraCostoReceta calculadora) {
        this.crearReceta = crearReceta;
        this.actualizarReceta = actualizarReceta;
        this.recetas = recetas;
        this.calculadora = calculadora;
    }

    public record IngredienteRequest(@NotBlank String productoId, @NotNull BigDecimal cantidad,
                                     @NotBlank String unidadId) {
    }

    public record CrearRecetaRequest(@NotBlank String productoId, @NotNull BigDecimal rendimiento, int porciones,
                                     List<IngredienteRequest> ingredientes) {
    }

    public record ActualizarRecetaRequest(@NotNull BigDecimal rendimiento, int porciones,
                                          List<IngredienteRequest> ingredientes) {
    }

    public record RecetaResumen(String id, String productoId, String productoCodigo, String productoNombre,
                                boolean esSubreceta, BigDecimal rendimiento, String unidadBaseCodigo,
                                int porciones, int cantidadIngredientes, boolean costosVisibles,
                                BigDecimal costoTotal, BigDecimal costoPorPorcion, Boolean costoIncompleto) {
    }

    /** costoUnitario y costoLinea: null sin permiso, o si ese ingrediente todavia no tiene costo. */
    public record IngredienteRespuesta(String productoId, String productoCodigo, String productoNombre,
                                       boolean esSubreceta, BigDecimal cantidad, String unidadId,
                                       String unidadCodigo, String unidadBaseCodigo, BigDecimal costoUnitario,
                                       BigDecimal costoLinea) {
    }

    public record RecetaDetalle(String id, String productoId, String productoCodigo, String productoNombre,
                                boolean esSubreceta, BigDecimal rendimiento, String unidadBaseId,
                                String unidadBaseCodigo, int porciones, int version, boolean costosVisibles,
                                BigDecimal costoTotal, BigDecimal costoPorPorcion, Boolean costoIncompleto,
                                List<IngredienteRespuesta> ingredientes, OffsetDateTime creadoEn,
                                OffsetDateTime actualizadoEn) {
    }

    public record ErrorRecetaRespuesta(String error, String productoId) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<RecetaResumen> listar(Authentication autenticacion) {
        boolean verCostos = puedeVerCostos(autenticacion);
        return recetas.findAll().stream()
                .filter(Receta::isActivo)
                .sorted(Comparator.comparing(r -> r.getProducto().getNombre()))
                .map(r -> new RecetaResumen(r.getId(), r.getProducto().getId(), r.getProducto().getCodigo(),
                        r.getProducto().getNombre(), r.getProducto().isEsSubreceta(), r.getRendimiento(),
                        r.getProducto().getUnidadBase().getCodigo(), r.getPorciones(), r.getIngredientes().size(),
                        verCostos, verCostos ? r.getCostoTotal() : null, verCostos ? r.getCostoPorPorcion() : null,
                        verCostos ? r.getCostoTotal() == null : null))
                .toList();
    }

    @Operation(summary = "Detalle de receta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = RecetaDetalle.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRecetaRespuesta.class)))
    })
    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<?> detalle(@PathVariable String id, Authentication autenticacion) {
        Receta receta = recetas.findById(id).orElse(null);
        if (receta == null || !receta.isActivo()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorRecetaRespuesta("NO_ENCONTRADO", null));
        }
        return ResponseEntity.ok(detalleDe(receta, puedeVerCostos(autenticacion)));
    }

    @Operation(summary = "Crear receta")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = RecetaDetalle.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRecetaRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRecetaRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRecetaRespuesta.class)))
    })
    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@org.springframework.validation.annotation.Validated @RequestBody CrearRecetaRequest cuerpo,
                                   @AuthenticationPrincipal String actorId, Authentication autenticacion) {
        Resultado<Receta, FalloReceta> resultado = crearReceta.ejecutar(cuerpo.productoId(), cuerpo.rendimiento(),
                cuerpo.porciones(), entradas(cuerpo.ingredientes()), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED)
                        .body(detalleDe(resultado.valorOLanza(), puedeVerCostos(autenticacion)))
                : errorDe(resultado);
    }

    @Operation(summary = "Actualizar receta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = RecetaDetalle.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRecetaRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRecetaRespuesta.class)))
    })
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> actualizar(@PathVariable String id,
            @org.springframework.validation.annotation.Validated @RequestBody ActualizarRecetaRequest cuerpo,
            @AuthenticationPrincipal String actorId, Authentication autenticacion) {
        Resultado<Receta, FalloReceta> resultado = actualizarReceta.ejecutar(id, cuerpo.rendimiento(),
                cuerpo.porciones(), entradas(cuerpo.ingredientes()), actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(detalleDe(resultado.valorOLanza(), puedeVerCostos(autenticacion)))
                : errorDe(resultado);
    }

    private static List<IngredienteEntrada> entradas(List<IngredienteRequest> ingredientes) {
        return ingredientes == null ? List.of() : ingredientes.stream()
                .map(i -> new IngredienteEntrada(i.productoId(), i.cantidad(), i.unidadId()))
                .toList();
    }

    private static boolean puedeVerCostos(Authentication autenticacion) {
        return autenticacion != null && autenticacion.getAuthorities().stream()
                .anyMatch(a -> VER_COSTOS.equals(a.getAuthority()));
    }

    private RecetaDetalle detalleDe(Receta r, boolean verCostos) {
        List<IngredienteRespuesta> ingredientes = r.getIngredientes().stream().map(i -> ingredienteDe(i, verCostos))
                .toList();
        return new RecetaDetalle(r.getId(), r.getProducto().getId(), r.getProducto().getCodigo(),
                r.getProducto().getNombre(), r.getProducto().isEsSubreceta(), r.getRendimiento(),
                r.getProducto().getUnidadBase().getId(), r.getProducto().getUnidadBase().getCodigo(),
                r.getPorciones(), r.getVersion(), verCostos, verCostos ? r.getCostoTotal() : null,
                verCostos ? r.getCostoPorPorcion() : null, verCostos ? r.getCostoTotal() == null : null,
                ingredientes, r.getCreadoEn(), r.getActualizadoEn());
    }

    private IngredienteRespuesta ingredienteDe(IngredienteReceta i, boolean verCostos) {
        BigDecimal unitario = null;
        BigDecimal linea = null;
        if (verCostos) {
            unitario = calculadora.costoUnitario(i.getProducto());
            BigDecimal cantidadBase = calculadora.aUnidadBase(i.getProducto(), i.getCantidad(), i.getUnidad());
            if (unitario != null && cantidadBase != null) {
                linea = cantidadBase.multiply(unitario).setScale(6, RoundingMode.HALF_UP);
            }
        }
        return new IngredienteRespuesta(i.getProducto().getId(), i.getProducto().getCodigo(),
                i.getProducto().getNombre(), i.getProducto().isEsSubreceta(), i.getCantidad(), i.getUnidad().getId(),
                i.getUnidad().getCodigo(), i.getProducto().getUnidadBase().getCodigo(), unitario, linea);
    }

    private ResponseEntity<?> errorDe(Resultado<?, FalloReceta> resultado) {
        FalloReceta fallo = ((Resultado.Fracaso<?, FalloReceta>) resultado).error();
        HttpStatus estado = switch (fallo.error()) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            // Sin productoId es el producto de la receta el que no existe;
            // con productoId es un ingrediente de la lista -- dato invalido.
            case PRODUCTO_NO_ENCONTRADO -> fallo.productoId() == null
                    ? HttpStatus.NOT_FOUND : HttpStatus.UNPROCESSABLE_ENTITY;
            case RECETA_YA_EXISTE -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("error", fallo.error().name());
        cuerpo.put("productoId", fallo.productoId());
        return ResponseEntity.status(estado).body(cuerpo);
    }
}
