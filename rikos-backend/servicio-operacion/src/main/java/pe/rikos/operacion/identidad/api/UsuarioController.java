package pe.rikos.operacion.identidad.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
import pe.rikos.operacion.identidad.aplicacion.ActualizarUsuario;
import pe.rikos.operacion.identidad.aplicacion.AsignarExcepcionesPermiso;
import pe.rikos.operacion.identidad.aplicacion.AsignarPin;
import pe.rikos.operacion.identidad.aplicacion.AsignarRolesYLocales;
import pe.rikos.operacion.identidad.aplicacion.CambiarEstadoUsuario;
import pe.rikos.operacion.identidad.aplicacion.CrearUsuario;
import pe.rikos.operacion.identidad.aplicacion.ErrorUsuario;
import pe.rikos.operacion.identidad.aplicacion.ListarUsuarios;
import pe.rikos.operacion.identidad.aplicacion.ObtenerPermisosUsuario;
import pe.rikos.operacion.identidad.aplicacion.ObtenerResumenUsuarios;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;

import java.util.List;
import java.util.Map;

/**
 * US-005. Rutas reales bajo /api/usuarios.
 */
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final CrearUsuario crearUsuario;
    private final ActualizarUsuario actualizarUsuario;
    private final CambiarEstadoUsuario cambiarEstadoUsuario;
    private final AsignarRolesYLocales asignarRolesYLocales;
    private final AsignarPin asignarPin;
    private final ListarUsuarios listarUsuarios;
    private final ObtenerPermisosUsuario obtenerPermisosUsuario;
    private final AsignarExcepcionesPermiso asignarExcepcionesPermiso;
    private final ObtenerResumenUsuarios obtenerResumenUsuarios;
    private final UsuarioRepositorio usuarios;
    private final UsuarioRolRepositorio usuarioRoles;
    private final UsuarioLocalRepositorio usuarioLocales;

    public UsuarioController(CrearUsuario crearUsuario, ActualizarUsuario actualizarUsuario,
                              CambiarEstadoUsuario cambiarEstadoUsuario, AsignarRolesYLocales asignarRolesYLocales,
                              AsignarPin asignarPin, ListarUsuarios listarUsuarios,
                              ObtenerPermisosUsuario obtenerPermisosUsuario,
                              AsignarExcepcionesPermiso asignarExcepcionesPermiso,
                              ObtenerResumenUsuarios obtenerResumenUsuarios, UsuarioRepositorio usuarios,
                              UsuarioRolRepositorio usuarioRoles, UsuarioLocalRepositorio usuarioLocales) {
        this.crearUsuario = crearUsuario;
        this.actualizarUsuario = actualizarUsuario;
        this.cambiarEstadoUsuario = cambiarEstadoUsuario;
        this.asignarRolesYLocales = asignarRolesYLocales;
        this.asignarPin = asignarPin;
        this.listarUsuarios = listarUsuarios;
        this.obtenerPermisosUsuario = obtenerPermisosUsuario;
        this.asignarExcepcionesPermiso = asignarExcepcionesPermiso;
        this.obtenerResumenUsuarios = obtenerResumenUsuarios;
        this.usuarios = usuarios;
        this.usuarioRoles = usuarioRoles;
        this.usuarioLocales = usuarioLocales;
    }

    public record CrearUsuarioRequest(
            @NotBlank String nombreUsuario, @NotBlank String nombres, @NotBlank String apellidos,
            String dni, String correo, String telefono, @NotBlank String contrasena,
            @NotEmpty List<String> rolesIds, @NotEmpty List<String> localesIds, String localPrincipalId) {
    }

    public record ActualizarUsuarioRequest(
            @NotBlank String nombres, @NotBlank String apellidos, String dni, String correo, String telefono) {
    }

    public record AsignarRolesYLocalesRequest(
            @NotEmpty List<String> rolesIds, @NotEmpty List<String> localesIds, String localPrincipalId) {
    }

    public record AsignarPinRequest(@NotBlank @Pattern(regexp = "\\d{4}") String pin) {
    }

    public record UsuarioRespuesta(String id, String nombreUsuario, String nombres, String apellidos, String dni,
                                    String correo, String telefono, boolean activo,
                                    List<ObtenerResumenUsuarios.RolResumen> roles,
                                    ObtenerResumenUsuarios.LocalPrincipalResumen localPrincipal,
                                    ObtenerResumenUsuarios.UltimoAcceso ultimoAcceso) {
        static UsuarioRespuesta de(Usuario usuario) {
            return de(usuario, null);
        }

        static UsuarioRespuesta de(Usuario usuario, ObtenerResumenUsuarios.Resumen resumen) {
            return new UsuarioRespuesta(usuario.getId(), usuario.getNombreUsuario(), usuario.getNombres(),
                    usuario.getApellidos(), usuario.getDni(), usuario.getCorreo(), usuario.getTelefono(),
                    usuario.isActivo(), resumen == null ? List.of() : resumen.roles(),
                    resumen == null ? null : resumen.localPrincipal(), resumen == null ? null : resumen.ultimoAcceso());
        }
    }

    @GetMapping
    public List<UsuarioRespuesta> listar() {
        return listarUsuarios.ejecutar().stream()
                .map(u -> UsuarioRespuesta.de(u, obtenerResumenUsuarios.resolver(u.getId())))
                .toList();
    }

    public record UsuarioDetalleRespuesta(String id, String nombreUsuario, String nombres, String apellidos,
                                           String dni, String correo, String telefono, boolean activo,
                                           List<String> rolesIds, List<String> localesIds, String localPrincipalId) {
    }

    /** Para precargar el formulario de edicion: a diferencia de listar(), trae TODOS los locales asignados, no solo el principal. */
    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable String id) {
        return usuarios.findById(id)
                .map(usuario -> {
                    List<String> rolesIds = usuarioRoles.findByUsuarioId(id).stream()
                            .map(ur -> ur.getRol().getId()).toList();
                    var asignaciones = usuarioLocales.findByUsuarioId(id);
                    List<String> localesIds = asignaciones.stream().map(ul -> ul.getLocal().getId()).toList();
                    String localPrincipalId = asignaciones.stream()
                            .filter(ul -> ul.isEsPrincipal())
                            .map(ul -> ul.getLocal().getId())
                            .findFirst().orElse(null);
                    return ResponseEntity.ok((Object) new UsuarioDetalleRespuesta(usuario.getId(),
                            usuario.getNombreUsuario(), usuario.getNombres(), usuario.getApellidos(),
                            usuario.getDni(), usuario.getCorreo(), usuario.getTelefono(), usuario.isActivo(),
                            rolesIds, localesIds, localPrincipalId));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CrearUsuarioRequest cuerpo,
                                    @AuthenticationPrincipal String actorId) {
        Resultado<Usuario, ErrorUsuario> resultado = crearUsuario.ejecutar(
                cuerpo.nombreUsuario(), cuerpo.nombres(), cuerpo.apellidos(), cuerpo.dni(), cuerpo.correo(),
                cuerpo.telefono(), cuerpo.contrasena(), cuerpo.rolesIds(), cuerpo.localesIds(),
                cuerpo.localPrincipalId(), actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(UsuarioRespuesta.de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @Valid @RequestBody ActualizarUsuarioRequest cuerpo,
                                         @AuthenticationPrincipal String actorId) {
        Resultado<Usuario, ErrorUsuario> resultado = actualizarUsuario.ejecutar(id, cuerpo.nombres(),
                cuerpo.apellidos(), cuerpo.dni(), cuerpo.correo(), cuerpo.telefono(), actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(UsuarioRespuesta.de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    @PutMapping("/{id}/roles-locales")
    public ResponseEntity<?> asignarRolesYLocales(@PathVariable String id,
                                                   @Valid @RequestBody AsignarRolesYLocalesRequest cuerpo,
                                                   @AuthenticationPrincipal String actorId) {
        Resultado<Void, ErrorUsuario> resultado = asignarRolesYLocales.ejecutar(id, cuerpo.rolesIds(),
                cuerpo.localesIds(), cuerpo.localPrincipalId(), actorId);
        if (resultado.esExito()) {
            return ResponseEntity.noContent().build();
        }
        ErrorUsuario error = ((Resultado.Fracaso<Void, ErrorUsuario>) resultado).error();
        return ResponseEntity.status(estadoDe(error)).body(Map.of("error", error.name()));
    }

    @PutMapping("/{id}/pin")
    public ResponseEntity<?> asignarPin(@PathVariable String id, @Valid @RequestBody AsignarPinRequest cuerpo,
                                         @AuthenticationPrincipal String actorId) {
        Resultado<Usuario, ErrorUsuario> resultado = asignarPin.ejecutar(id, cuerpo.pin(), actorId);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    public record ExcepcionRequest(@NotBlank String permisoId, boolean otorgado, String motivo) {
    }

    public record AsignarExcepcionesRequest(@NotNull List<ExcepcionRequest> excepciones) {
    }

    /** El catalogo completo con cual permiso viene del rol, cual es la excepcion (si hay) y cual es el efectivo. */
    @GetMapping("/{id}/permisos")
    public List<ObtenerPermisosUsuario.PermisoUsuarioVista> permisosDe(@PathVariable String id) {
        return obtenerPermisosUsuario.ejecutar(id);
    }

    /** Requiere identidad.administrar_permisos -- ver ConfiguracionSeguridad. */
    @PutMapping("/{id}/permisos")
    public ResponseEntity<?> asignarExcepciones(@PathVariable String id,
                                                 @Valid @RequestBody AsignarExcepcionesRequest cuerpo,
                                                 @AuthenticationPrincipal String actorId) {
        List<AsignarExcepcionesPermiso.Excepcion> excepciones = cuerpo.excepciones().stream()
                .map(e -> new AsignarExcepcionesPermiso.Excepcion(e.permisoId(), e.otorgado(), e.motivo()))
                .toList();
        Resultado<Void, ErrorUsuario> resultado = asignarExcepcionesPermiso.ejecutar(id, excepciones, actorId);
        if (resultado.esExito()) {
            return ResponseEntity.noContent().build();
        }
        ErrorUsuario error = ((Resultado.Fracaso<Void, ErrorUsuario>) resultado).error();
        return ResponseEntity.status(estadoDe(error)).body(Map.of("error", error.name()));
    }

    @PostMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        return cambiarEstado(id, false, actorId);
    }

    @PostMapping("/{id}/activar")
    public ResponseEntity<?> activar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        return cambiarEstado(id, true, actorId);
    }

    private ResponseEntity<?> cambiarEstado(String id, boolean activo, String actorId) {
        Resultado<Usuario, ErrorUsuario> resultado = cambiarEstadoUsuario.ejecutar(id, activo, actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(UsuarioRespuesta.de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<Usuario, ErrorUsuario> resultado) {
        ErrorUsuario error = ((Resultado.Fracaso<Usuario, ErrorUsuario>) resultado).error();
        return ResponseEntity.status(estadoDe(error)).body(Map.of("error", error.name()));
    }

    private HttpStatus estadoDe(ErrorUsuario error) {
        return switch (error) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case NOMBRE_USUARIO_DUPLICADO -> HttpStatus.CONFLICT;
            case ROL_INVALIDO, LOCAL_INVALIDO, PERMISO_INVALIDO -> HttpStatus.BAD_REQUEST;
        };
    }
}
