package pe.rikos.operacion.identidad.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.operacion.identidad.infraestructura.Permiso;
import pe.rikos.operacion.identidad.infraestructura.PermisoRepositorio;

import java.util.List;

/** Solo lectura: el catalogo lo siembra SembradorCatalogoPermisos, no se edita desde la API. */
@RestController
@RequestMapping("/permisos")
public class PermisoController {

    private final PermisoRepositorio permisos;

    public PermisoController(PermisoRepositorio permisos) {
        this.permisos = permisos;
    }

    public record PermisoRespuesta(String id, String modulo, String accion, String codigo, String descripcion) {
        static PermisoRespuesta de(Permiso permiso) {
            return new PermisoRespuesta(permiso.getId(), permiso.getModulo(), permiso.getAccion(),
                    permiso.getCodigo(), permiso.getDescripcion());
        }
    }

    @GetMapping
    public List<PermisoRespuesta> listar() {
        return permisos.findAllByOrderByModuloAscAccionAsc().stream().map(PermisoRespuesta::de).toList();
    }
}
