package pe.rikos.operacion.catalogo.api;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.ActualizarCategoria;
import pe.rikos.operacion.catalogo.aplicacion.CrearCategoria;
import pe.rikos.operacion.catalogo.aplicacion.ErrorCatalogo;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/categorias")
public class CategoriaController {

    private final CrearCategoria crearCategoria;
    private final ActualizarCategoria actualizarCategoria;
    private final CategoriaProductoRepositorio categorias;

    public CategoriaController(CrearCategoria crearCategoria, ActualizarCategoria actualizarCategoria,
                                CategoriaProductoRepositorio categorias) {
        this.crearCategoria = crearCategoria;
        this.actualizarCategoria = actualizarCategoria;
        this.categorias = categorias;
    }

    public record CategoriaRequest(@NotBlank String nombre, String naturaleza, String componenteMenu,
                                    String categoriaPadreId, int orden) {
    }

    public record CategoriaRespuesta(String id, String nombre, String naturaleza, String componenteMenu, int nivel,
                                      int orden, boolean esHoja, boolean activo, String categoriaPadreId) {
        static CategoriaRespuesta de(CategoriaProducto c, boolean esHoja) {
            return new CategoriaRespuesta(c.getId(), c.getNombre(), c.getNaturaleza(), c.getComponenteMenu(),
                    c.getNivel(), c.getOrden(), esHoja, c.isActivo(),
                    c.getCategoriaPadre() != null ? c.getCategoriaPadre().getId() : null);
        }
    }

    // @Transactional: open-in-view esta apagado, y CategoriaRespuesta.de()
    // lee categoriaPadre (LAZY) -- sin la sesion abierta hasta que el
    // controlador arma la respuesta, explota con LazyInitializationException.
    @GetMapping
    @Transactional(readOnly = true)
    public List<CategoriaRespuesta> listar() {
        return categorias.findAll().stream()
                .map(c -> CategoriaRespuesta.de(c, !categorias.existsByCategoriaPadreIdAndActivoTrue(c.getId())))
                .toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@org.springframework.validation.annotation.Validated @RequestBody CategoriaRequest cuerpo,
                                    @AuthenticationPrincipal String actorId) {
        Resultado<CategoriaProducto, ErrorCatalogo> resultado = crearCategoria.ejecutar(cuerpo.nombre(),
                cuerpo.naturaleza(), cuerpo.componenteMenu(), cuerpo.categoriaPadreId(), cuerpo.orden(), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(CategoriaRespuesta.de(resultado.valorOLanza(), true))
                : errorDe(resultado);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody CategoriaRequest cuerpo,
                                         @AuthenticationPrincipal String actorId) {
        Resultado<CategoriaProducto, ErrorCatalogo> resultado =
                actualizarCategoria.ejecutar(id, cuerpo.nombre(), cuerpo.orden(), actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(CategoriaRespuesta.de(resultado.valorOLanza(),
                        !categorias.existsByCategoriaPadreIdAndActivoTrue(id)))
                : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorCatalogo> resultado) {
        ErrorCatalogo error = ((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error();
        HttpStatus estado = switch (error) {
            case NO_ENCONTRADO, CATEGORIA_PADRE_NO_ENCONTRADA -> HttpStatus.NOT_FOUND;
            case NATURALEZA_INVALIDA -> HttpStatus.UNPROCESSABLE_ENTITY;
            case NOMBRE_DUPLICADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}
