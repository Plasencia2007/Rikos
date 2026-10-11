package pe.rikos.operacion.identidad.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import pe.rikos.operacion.identidad.aplicacion.EliminarRol;
import pe.rikos.operacion.identidad.aplicacion.ErrorRol;
import pe.rikos.operacion.identidad.aplicacion.ListarRoles;
import pe.rikos.operacion.identidad.aplicacion.ObtenerPermisosPredeterminados;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolPermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;
import pe.rikos.operacion.comun.web.ErrorRespuesta;

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
    private final EliminarRol eliminarRol;
    private final RolPermisoRepositorio rolPermisos;
    private final ObtenerPermisosPredeterminados obtenerPredeterminados;
    private final UsuarioRolRepositorio usuarioRoles;

    public RolController(ListarRoles listarRoles, CrearRol crearRol, ActualizarRol actualizarRol,
                          AsignarPermisosARol asignarPermisosARol, EliminarRol eliminarRol,
                          RolPermisoRepositorio rolPermisos, ObtenerPermisosPredeterminados obtenerPredeterminados,
                          UsuarioRolRepositorio usuarioRoles) {
        this.listarRoles = listarRoles;
        this.crearRol = crearRol;
        this.actualizarRol = actualizarRol;
        this.asignarPermisosARol = asignarPermisosARol;
        this.eliminarRol = eliminarRol;
        this.rolPermisos = rolPermisos;
        this.obtenerPredeterminados = obtenerPredeterminados;
        this.usuarioRoles = usuarioRoles;
    }

    public record RolRequest(@NotBlank String nombre, String descripcion, String color) {
    }

    public record AsignarPermisosRequest(@NotNull List<String> permisosIds) {
    }

    public record RolRespuesta(String id, String nombre, String descripcion, boolean esSistema, String color,
                                int cantidadPermisos, long cantidadUsuarios) {
        static RolRespuesta de(Rol rol, int cantidadPermisos, long cantidadUsuarios) {
            return new RolRespuesta(rol.getId(), rol.getNombre(), rol.getDescripcion(), rol.isEsSistema(),
                    rol.getColor(), cantidadPermisos, cantidadUsuarios);
        }
    }

    @GetMapping
    public List<RolRespuesta> listar() {
        return listarRoles.ejecutar().stream()
                .map(rol -> RolRespuesta.de(rol, rolPermisos.findByRolId(rol.getId()).size(),
                        usuarioRoles.countByRolId(rol.getId())))
                .toList();
    }

    @GetMapping("/{id}/permisos")
    public List<String> permisosDe(@PathVariable String id) {
        return rolPermisos.findByRolId(id).stream().map(rp -> rp.getPermiso().getId()).toList();
    }

    /** Para "restaurar predeterminados": no cambia nada, solo dice con que permisos nacio el rol. */
    @Operation(summary = "Permisos predeterminados de un rol")
    @GetMapping("/{id}/permisos-predeterminados")
    public ResponseEntity<?> permisosPredeterminados(@PathVariable String id) {
        Resultado<List<String>, ErrorRol> resultado = obtenerPredeterminados.ejecutar(id);
        if (resultado.esExito()) {
            return ResponseEntity.ok(resultado.valorOLanza());
        }
        ErrorRol error = ((Resultado.Fracaso<List<String>, ErrorRol>) resultado).error();
        return ResponseEntity.status(estadoDe(error)).body(Map.of("error", error.name()));
    }

    @Operation(summary = "Crear rol")
    @ApiResponses({
            @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = RolRespuesta.class))),
            @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody RolRequest cuerpo) {
        Resultado<Rol, ErrorRol> resultado = crearRol.ejecutar(cuerpo.nombre(), cuerpo.descripcion(), cuerpo.color());
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(RolRespuesta.de(resultado.valorOLanza(), 0, 0))
                : errorDe(resultado);
    }

    @Operation(summary = "Actualizar rol")
    @ApiResponses({
            @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = RolRespuesta.class))),
            @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @Valid @RequestBody RolRequest cuerpo) {
        Resultado<Rol, ErrorRol> resultado = actualizarRol.ejecutar(id, cuerpo.nombre(), cuerpo.descripcion(),
                cuerpo.color());
        return resultado.esExito()
                ? ResponseEntity.ok(RolRespuesta.de(resultado.valorOLanza(), rolPermisos.findByRolId(id).size(),
                        usuarioRoles.countByRolId(id)))
                : errorDe(resultado);
    }

    /** Requiere identidad.administrar_permisos -- ver ConfiguracionSeguridad. */
    @Operation(summary = "Asignar permisos a rol")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
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

    @Operation(summary = "Eliminar rol")
    @ApiResponses({
            @ApiResponse(responseCode = "204"),
            @ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class))),
            @ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorRespuesta.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable String id) {
        Resultado<Void, ErrorRol> resultado = eliminarRol.ejecutar(id);
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
            case NOMBRE_DUPLICADO, TIENE_HISTORIAL -> HttpStatus.CONFLICT;
            case PERMISO_INVALIDO, ES_SISTEMA -> HttpStatus.BAD_REQUEST;
        };
    }
}
