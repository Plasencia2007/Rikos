package pe.rikos.operacion.catalogo.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.*;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.ProductoServicioRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class ProductoController {

    private final CrearProducto crearProducto;
    private final ActualizarProducto actualizarProducto;
    private final DesactivarProducto desactivarProducto;
    private final ActivarProducto activarProducto;
    private final EliminarProducto eliminarProducto;
    private final RegistrarCostoManual registrarCostoManual;
    private final AsignarServiciosProducto asignarServiciosProducto;
    private final ProductoRepositorio productos;
    private final CategoriaProductoRepositorio categorias;
    private final UnidadMedidaRepositorio unidades;
    private final ProductoServicioRepositorio productoServicios;

    public ProductoController(CrearProducto crearProducto, ActualizarProducto actualizarProducto,
            DesactivarProducto desactivarProducto, ActivarProducto activarProducto,
            EliminarProducto eliminarProducto, RegistrarCostoManual registrarCostoManual,
            AsignarServiciosProducto asignarServiciosProducto, ProductoRepositorio productos,
            CategoriaProductoRepositorio categorias, UnidadMedidaRepositorio unidades,
            ProductoServicioRepositorio productoServicios) {
        this.crearProducto = crearProducto;
        this.actualizarProducto = actualizarProducto;
        this.desactivarProducto = desactivarProducto;
        this.activarProducto = activarProducto;
        this.eliminarProducto = eliminarProducto;
        this.registrarCostoManual = registrarCostoManual;
        this.asignarServiciosProducto = asignarServiciosProducto;
        this.productos = productos;
        this.categorias = categorias;
        this.unidades = unidades;
        this.productoServicios = productoServicios;
    }

    public record ProductoRequest(@NotBlank String codigo, @NotBlank String nombre, @NotBlank String categoriaId,
                                   @NotBlank String unidadBaseId, boolean esVendible, boolean esInsumo,
                                   boolean esSubreceta, Long precioVenta, boolean controlaStock) {
    }

    public record CostoManualRequest(BigDecimal costo) {
    }

    public record ServiciosRequest(Set<String> servicios) {
    }

    /** imagen: una "data URL" de imagen (JPEG, PNG o WEBP), o null para quitar la foto. */
    public record ImagenRequest(String imagen) {
    }

    /** Tope de la foto (caracteres de la data URL): ~300 KB. El navegador la reduce antes de enviarla. */
    private static final int MAXIMO_DE_IMAGEN = 400_000;

    public record ProductoRespuesta(String id, String codigo, String nombre, String categoriaId,
                                     String categoriaNombre, String unidadBaseId, boolean esVendible,
                                     boolean esInsumo, boolean esSubreceta, Long precioVenta, BigDecimal costo,
                                     String origenCosto, boolean controlaStock, boolean activo,
                                     List<String> servicios, OffsetDateTime creadoEn,
                                     OffsetDateTime actualizadoEn, LocalDate fechaUltimaCompra,
                                     String tipoMenu, String imagen) {
        static ProductoRespuesta de(Producto p, List<String> servicios) {
            return new ProductoRespuesta(p.getId(), p.getCodigo(), p.getNombre(), p.getCategoria().getId(),
                    p.getCategoria().getNombre(), p.getUnidadBase().getId(), p.isEsVendible(), p.isEsInsumo(),
                    p.isEsSubreceta(), p.getPrecioVenta(), p.getCostoUltimaCompra(), p.getOrigenCosto(),
                    p.isControlaStock(), p.isActivo(), servicios, p.getCreadoEn(), p.getActualizadoEn(),
                    p.getFechaUltimaCompra(), p.getTipoMenu(), p.getImagen());
        }
    }

    // @Transactional a nivel de controlador (ademas del que ya tiene cada
    // caso de uso) porque open-in-view esta apagado (application.yaml):
    // sin esto, la sesion de Hibernate se cierra en cuanto el caso de uso
    // retorna, y ProductoRespuesta.de() -- que lee categoria/unidadBase,
    // relaciones LAZY -- explota con LazyInitializationException al armar
    // la respuesta. Bug real encontrado al probar el costo manual en el
    // navegador: Spring lo traducia a 403 en vez de 500, lo que parecia
    // (incorrectamente) un problema de permisos.
    @GetMapping("/productos")
    @Transactional(readOnly = true)
    public List<ProductoRespuesta> listar() {
        return productos.findAll().stream()
                .map(p -> ProductoRespuesta.de(p, productoServicios.findByProductoId(p.getId()).stream()
                        .map(ps -> ps.getServicio()).toList()))
                .toList();
    }

    @Operation(summary = "Crear producto")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = ProductoRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/productos")
    @Transactional
    public ResponseEntity<?> crear(@org.springframework.validation.annotation.Validated @RequestBody ProductoRequest cuerpo,
                                    @AuthenticationPrincipal String actorId) {
        Resultado<Producto, ErrorCatalogo> resultado = crearProducto.ejecutar(cuerpo.codigo(), cuerpo.nombre(),
                cuerpo.categoriaId(), cuerpo.unidadBaseId(), cuerpo.esVendible(), cuerpo.esInsumo(),
                cuerpo.esSubreceta(), cuerpo.precioVenta(), cuerpo.controlaStock(), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(ProductoRespuesta.de(resultado.valorOLanza(), List.of()))
                : errorDe(resultado);
    }

    @Operation(summary = "Actualizar producto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProductoRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/productos/{id}")
    @Transactional
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody ProductoRequest cuerpo,
                                         @AuthenticationPrincipal String actorId) {
        CategoriaProducto categoria = categorias.findById(cuerpo.categoriaId()).orElse(null);
        UnidadMedida unidad = unidades.findById(cuerpo.unidadBaseId()).orElse(null);
        if (categoria == null || unidad == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "NO_ENCONTRADO"));
        }
        Producto.DatosActualizacion datos = new Producto.DatosActualizacion(cuerpo.nombre(), categoria, unidad,
                cuerpo.esVendible(), cuerpo.esInsumo(), cuerpo.esSubreceta(), cuerpo.precioVenta(),
                cuerpo.controlaStock());
        Resultado<Producto, ErrorCatalogo> resultado = actualizarProducto.ejecutar(id, datos, actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(ProductoRespuesta.de(resultado.valorOLanza(),
                        productoServicios.findByProductoId(id).stream().map(ps -> ps.getServicio()).toList()))
                : errorDe(resultado);
    }

    @Operation(summary = "Desactivar producto")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/productos/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        Resultado<Producto, ErrorCatalogo> resultado = desactivarProducto.ejecutar(id, actorId);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    @Operation(summary = "Activar producto")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping("/productos/{id}/activar")
    public ResponseEntity<?> activar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        Resultado<Producto, ErrorCatalogo> resultado = activarProducto.ejecutar(id, actorId);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    @Operation(summary = "Eliminar producto")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @DeleteMapping("/productos/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String id) {
        Resultado<Void, ErrorCatalogo> resultado = eliminarProducto.ejecutar(id);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    @Operation(summary = "Registrar costo manual")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ProductoRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/productos/{id}/costo-manual")
    @Transactional
    public ResponseEntity<?> registrarCosto(@PathVariable String id, @RequestBody CostoManualRequest cuerpo,
                                             @AuthenticationPrincipal String actorId) {
        Resultado<Producto, ErrorCatalogo> resultado = registrarCostoManual.ejecutar(id, cuerpo.costo(), actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(ProductoRespuesta.de(resultado.valorOLanza(),
                        productoServicios.findByProductoId(id).stream().map(ps -> ps.getServicio()).toList()))
                : errorDe(resultado);
    }

    @Operation(summary = "Poner o quitar la foto de un producto")
    @PutMapping("/productos/{id}/imagen")
    @Transactional
    public ResponseEntity<?> ponerImagen(@PathVariable String id, @RequestBody ImagenRequest cuerpo) {
        Producto producto = productos.findById(id).orElse(null);
        if (producto == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ErrorCatalogo.NO_ENCONTRADO.name()));
        }
        String imagen = cuerpo.imagen() == null || cuerpo.imagen().isBlank() ? null : cuerpo.imagen();
        if (imagen != null && (!imagen.startsWith("data:image/") || imagen.length() > MAXIMO_DE_IMAGEN)) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(Map.of("error", "IMAGEN_INVALIDA"));
        }
        producto.setImagen(imagen);
        productos.save(producto);
        return ResponseEntity.ok(ProductoRespuesta.de(producto,
                productoServicios.findByProductoId(id).stream().map(ps -> ps.getServicio()).toList()));
    }

    @Operation(summary = "Asignar servicios a producto")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/productos/{id}/servicios")
    public ResponseEntity<?> asignarServicios(@PathVariable String id, @RequestBody ServiciosRequest cuerpo,
                                               @AuthenticationPrincipal String actorId) {
        Resultado<Void, ErrorCatalogo> resultado =
                asignarServiciosProducto.ejecutar(id, cuerpo.servicios(), actorId);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorCatalogo> resultado) {
        ErrorCatalogo error = ((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error();
        HttpStatus estado = switch (error) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case CODIGO_DUPLICADO, TIENE_HISTORIAL, PRODUCTO_DE_MENU -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}
