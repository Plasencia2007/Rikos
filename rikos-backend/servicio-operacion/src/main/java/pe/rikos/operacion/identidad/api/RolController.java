package pe.rikos.operacion.identidad.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.aplicacion.ActualizarRol;
import pe.rikos.operacion.identidad.aplicacion.AsignarPermisosARol;
import pe.rikos.operacion.identidad.aplicacion.CrearRol;
import pe.rikos.operacion.identidad.aplicacion.ErrorRol;
import pe.rikos.operacion.identidad.aplicacion.ListarRoles;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolPermisoRepositorio;

import java.util.List;
import java.util.Map;

/** US-004/US-006 (adelantadas). Alta y edicion de roles, y asignacion de sus permisos. */
@RestController
@RequestMapping("/roles")
public class RolController {

    private final ListarRoles listarRoles;
    private final CrearRol crearRol;
    private final ActualizarRol actualizarRol;
    private final AsignarPermisosARol asignarPermisosARol;
    private final RolPermisoRepositorio rolPermisos;

    public RolController(ListarRoles listarRoles, CrearRol crearRol, ActualizarRol actualizarRol,
                          AsignarPermisosARol asignarPermisosARol, RolPermisoRepositorio rolPermisos) {
        this.listarRoles = listarRoles;
        this.crearRol = crearRol;
        this.actualizarRol = actualizarRol;
        this.asignarPermisosARol = asignarPermisosARol;
        this.rolPermisos = rolPermisos;
    }

    public record RolRequest(@NotBlank String nombre, String descripcion, String color) {
    }

    public record AsignarPermisosRequest(@NotNull List<String> permisosIds) {
    }

    public record RolRespuesta(String id, String nombre, String descripcion, boolean esSistema, String color,
                                int cantidadPermisos) {
        static RolRespuesta de(Rol rol, int cantidadPermisos) {
            return new RolRespuesta(rol.getId(), rol.getNombre(), rol.getDescripcion(), rol.isEsSistema(),
                    rol.getColor(), cantidadPermisos);
        }
    }

    @GetMapping
    public List<RolRespuesta> listar() {
        return listarRoles.ejecutar().stream()
                .map(rol -> RolRespuesta.de(rol, rolPermisos.findByRolId(rol.getId()).size()))
                .toList();
    }

    @GetMapping("/{id}/permisos")
    public List<String> permisosDe(@PathVariable String id) {
        return rolPermisos.findByRolId(id).stream().map(rp -> rp.getPermiso().getId()).toList();
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody RolRequest cuerpo) {
        Resultado<Rol, ErrorRol> resultado = crearRol.ejecutar(cuerpo.nombre(), cuerpo.descripcion(), cuerpo.color());
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(RolRespuesta.de(resultado.valorOLanza(), 0))
                : errorDe(resultado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @Valid @RequestBody RolRequest cuerpo) {
        Resultado<Rol, ErrorRol> resultado = actualizarRol.ejecutar(id, cuerpo.nombre(), cuerpo.descripcion(),
                cuerpo.color());
        return resultado.esExito()
                ? ResponseEntity.ok(RolRespuesta.de(resultado.valorOLanza(), rolPermisos.findByRolId(id).size()))
                : errorDe(resultado);
    }

    /** Requiere identidad.administrar_permisos -- ver ConfiguracionSeguridad. */
    @PutMapping("/{id}/permisos")
    public ResponseEntity<?> asignarPermisos(@PathVariable String id, @Valid @RequestBody AsignarPermisosRequest cuerpo,
                                              @AuthenticationPrincipal String actorId) {
        Resultado<Void, ErrorRol> resultado = asignarPermisosARol.ejecutar(id, cuerpo.permisosIds(), actorId);
        if (resultado.esExito()) {
            return ResponseEntity.noContent().build();
        }
        ErrorRol error = ((Resultado.Fracaso<Void, ErrorRol>) resultado).error();
        return ResponseEntity.status(estadoDe(error)).body(Map.of("error", error.name()));
    }

    private ResponseEntity<?> errorDe(Resultado<Rol, ErrorRol> resultado) {
        ErrorRol error = ((Resultado.Fracaso<Rol, ErrorRol>) resultado).error();
        return ResponseEntity.status(estadoDe(error)).body(Map.of("error", error.name()));
    }

    private HttpStatus estadoDe(ErrorRol error) {
        return switch (error) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case NOMBRE_DUPLICADO -> HttpStatus.CONFLICT;
            case PERMISO_INVALIDO -> HttpStatus.BAD_REQUEST;
        };
    }
}
