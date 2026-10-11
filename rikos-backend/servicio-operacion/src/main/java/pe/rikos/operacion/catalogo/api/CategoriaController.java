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
import pe.rikos.operacion.catalogo.aplicacion.ActualizarCategoria;
import pe.rikos.operacion.catalogo.aplicacion.CrearCategoria;
import pe.rikos.operacion.catalogo.aplicacion.EliminarCategoria;
import pe.rikos.operacion.catalogo.aplicacion.ErrorCatalogo;
import pe.rikos.operacion.catalogo.aplicacion.ReordenarCategorias;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CrearCategoria crearCategoria;
    private final ActualizarCategoria actualizarCategoria;
    private final ReordenarCategorias reordenarCategorias;
    private final EliminarCategoria eliminarCategoria;
    private final CategoriaProductoRepositorio categorias;
    private final ProductoRepositorio productos;

    public CategoriaController(CrearCategoria crearCategoria, ActualizarCategoria actualizarCategoria,
                                ReordenarCategorias reordenarCategorias, EliminarCategoria eliminarCategoria,
                                CategoriaProductoRepositorio categorias, ProductoRepositorio productos) {
        this.crearCategoria = crearCategoria;
        this.actualizarCategoria = actualizarCategoria;
        this.reordenarCategorias = reordenarCategorias;
        this.eliminarCategoria = eliminarCategoria;
        this.categorias = categorias;
        this.productos = productos;
    }

    public record CategoriaRequest(@NotBlank String nombre, String naturaleza, String componenteMenu,
                                    String categoriaPadreId, int orden) {
    }

    public record ReordenarRequest(String categoriaPadreId, List<String> ids) {
    }

    public record CategoriaRespuesta(String id, String nombre, String naturaleza, String componenteMenu, int nivel,
                                      int orden, boolean esHoja, boolean activo, String categoriaPadreId,
                                      int totalProductos) {
        static CategoriaRespuesta de(CategoriaProducto c, boolean esHoja, int totalProductos) {
            return new CategoriaRespuesta(c.getId(), c.getNombre(), c.getNaturaleza(), c.getComponenteMenu(),
                    c.getNivel(), c.getOrden(), esHoja, c.isActivo(),
                    c.getCategoriaPadre() != null ? c.getCategoriaPadre().getId() : null, totalProductos);
        }
    }

    // @Transactional: open-in-view esta apagado, y CategoriaRespuesta.de()
    // lee categoriaPadre (LAZY) -- sin la sesion abierta hasta que el
    // controlador arma la respuesta, explota con LazyInitializationException.
    //
    // Se devuelve en el mismo orden que pinta la pantalla (cada raiz
    // seguida de sus hijas por `orden`), no el orden que da findAll(). Las
    // hijas son siempre hoja: el arbol tiene dos niveles nada mas, y
    // CrearCategoria ya rechaza crear una hija de una hija.
    @GetMapping
    @Transactional(readOnly = true)
    public List<CategoriaRespuesta> listar() {
        Map<String, Long> totalesPorCategoria = new java.util.HashMap<>();
        for (Object[] fila : productos.contarActivosPorCategoria()) {
            totalesPorCategoria.put((String) fila[0], (Long) fila[1]);
        }

        List<CategoriaRespuesta> resultado = new ArrayList<>();
        for (CategoriaProducto raiz : categorias.findByCategoriaPadreIdIsNullOrderByOrden()) {
            List<CategoriaProducto> hijas = categorias.findByCategoriaPadreIdOrderByOrden(raiz.getId());
            resultado.add(CategoriaRespuesta.de(raiz, hijas.isEmpty(),
                    totalesPorCategoria.getOrDefault(raiz.getId(), 0L).intValue()));
            for (CategoriaProducto hija : hijas) {
                resultado.add(CategoriaRespuesta.de(hija, true,
                        totalesPorCategoria.getOrDefault(hija.getId(), 0L).intValue()));
            }
        }
        return resultado;
    }

    @Operation(summary = "Crear categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CategoriaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@org.springframework.validation.annotation.Validated @RequestBody CategoriaRequest cuerpo,
                                    @AuthenticationPrincipal String actorId) {
        Resultado<CategoriaProducto, ErrorCatalogo> resultado = crearCategoria.ejecutar(cuerpo.nombre(),
                cuerpo.naturaleza(), cuerpo.componenteMenu(), cuerpo.categoriaPadreId(), cuerpo.orden(), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(CategoriaRespuesta.de(resultado.valorOLanza(), true, 0))
                : errorDe(resultado);
    }

    @Operation(summary = "Actualizar categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = CategoriaRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody CategoriaRequest cuerpo,
                                         @AuthenticationPrincipal String actorId) {
        Resultado<CategoriaProducto, ErrorCatalogo> resultado =
                actualizarCategoria.ejecutar(id, cuerpo.nombre(), cuerpo.orden(), actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(CategoriaRespuesta.de(resultado.valorOLanza(),
                        !categorias.existsByCategoriaPadreIdAndActivoTrue(id), contarProductos(id)))
                : errorDe(resultado);
    }

    private int contarProductos(String categoriaId) {
        for (Object[] fila : productos.contarActivosPorCategoria()) {
            if (categoriaId.equals(fila[0])) {
                return ((Long) fila[1]).intValue();
            }
        }
        return 0;
    }

    @Operation(summary = "Eliminar categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> eliminar(@PathVariable String id) {
        Resultado<Void, ErrorCatalogo> resultado = eliminarCategoria.ejecutar(id);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    @Operation(summary = "Reordenar categorias")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/orden")
    @Transactional
    public ResponseEntity<?> reordenar(@RequestBody ReordenarRequest cuerpo,
                                        @AuthenticationPrincipal String actorId) {
        Resultado<List<CategoriaProducto>, ErrorCatalogo> resultado =
                reordenarCategorias.ejecutar(cuerpo.categoriaPadreId(), cuerpo.ids(), actorId);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorCatalogo> resultado) {
        ErrorCatalogo error = ((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error();
        HttpStatus estado = switch (error) {
            case NO_ENCONTRADO, CATEGORIA_PADRE_NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
            case NATURALEZA_INVALIDA, PADRE_NO_ES_RAIZ -> HttpStatus.UNPROCESSABLE_ENTITY;
            case NOMBRE_DUPLICADO, PADRE_TIENE_PRODUCTOS, CATEGORIA_TIENE_SUBCATEGORIAS, CATEGORIA_TIENE_PRODUCTOS ->
                HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}
