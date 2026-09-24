package pe.rikos.operacion.catalogo.api;

import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/unidades")
public class UnidadController {

    private final UnidadMedidaRepositorio unidades;

    public UnidadController(UnidadMedidaRepositorio unidades) {
        this.unidades = unidades;
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

    @PostMapping
    public ResponseEntity<?> crear(@org.springframework.validation.annotation.Validated @RequestBody UnidadRequest cuerpo) {
        if (unidades.existsByCodigo(cuerpo.codigo())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "CODIGO_DUPLICADO"));
        }
        UnidadMedida creada = unidades.save(
                new UnidadMedida(Identificador.nuevo().valor(), cuerpo.codigo(), cuerpo.nombre(), cuerpo.tipo()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UnidadRespuesta.de(creada));
    }
}
