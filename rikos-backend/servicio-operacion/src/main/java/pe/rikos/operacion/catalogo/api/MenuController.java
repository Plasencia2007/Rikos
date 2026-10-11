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
import pe.rikos.operacion.catalogo.aplicacion.CambiarPrecioMenu;
import pe.rikos.operacion.catalogo.aplicacion.ErrorMenu;
import pe.rikos.operacion.catalogo.aplicacion.TipoMenu;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * US-014: menu normal y menu extra. Permisos del modulo "menu" (no
 * "catalogo"): menu.ver para leer, menu.modificar para el precio.
 */
@RestController
@RequestMapping("/menu")
public class MenuController {

    private final CambiarPrecioMenu cambiarPrecioMenu;
    private final ProductoRepositorio productos;

    public MenuController(CambiarPrecioMenu cambiarPrecioMenu, ProductoRepositorio productos) {
        this.cambiarPrecioMenu = cambiarPrecioMenu;
        this.productos = productos;
    }

    public record PrecioMenuRequest(Long precioVenta) {
    }

    /** precioVenta en centimos; null mientras el dueno no lo defina (RF-002). */
    public record TipoMenuRespuesta(String tipo, String productoId, String codigo, String nombre, Long precioVenta,
                                    List<String> componentes, boolean admiteTarifaReducida) {
        static TipoMenuRespuesta de(TipoMenu tipo, Producto p) {
            return new TipoMenuRespuesta(tipo.codigo(), p.getId(), p.getCodigo(), p.getNombre(), p.getPrecioVenta(),
                    tipo.componentes(), tipo.admiteTarifaReducida());
        }
    }

    @GetMapping("/tipos")
    @Transactional(readOnly = true)
    public List<TipoMenuRespuesta> listar() {
        return Arrays.stream(TipoMenu.values())
                .flatMap(t -> productos.findByTipoMenu(t.codigo()).map(p -> TipoMenuRespuesta.de(t, p)).stream())
                .toList();
    }

    @Operation(summary = "Cambiar precio del menú normal o extra")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = TipoMenuRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "422", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/{tipo}/precio")
    @Transactional
    public ResponseEntity<?> cambiarPrecio(@PathVariable String tipo, @RequestBody PrecioMenuRequest cuerpo,
                                           @AuthenticationPrincipal String actorId) {
        Resultado<Producto, ErrorMenu> resultado = cambiarPrecioMenu.ejecutar(tipo, cuerpo.precioVenta(), actorId);
        if (resultado.esExito()) {
            return ResponseEntity.ok(TipoMenuRespuesta.de(TipoMenu.desde(tipo).orElseThrow(), resultado.valorOLanza()));
        }
        ErrorMenu error = ((Resultado.Fracaso<?, ErrorMenu>) resultado).error();
        HttpStatus estado = switch (error) {
            case TIPO_INVALIDO -> HttpStatus.NOT_FOUND;
            case PRECIO_INVALIDO -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}
