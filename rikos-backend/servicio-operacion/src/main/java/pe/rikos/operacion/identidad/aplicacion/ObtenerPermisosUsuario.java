package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.identidad.infraestructura.Permiso;
import pe.rikos.operacion.identidad.infraestructura.PermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.RolPermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioPermisoExcepcion;
import pe.rikos.operacion.identidad.infraestructura.UsuarioPermisoExcepcionRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRol;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Para el cuarto bloque de la pantalla de usuario (US-005 CA3): el catalogo
 * completo de permisos, con cuales vienen del rol, cual es la excepcion (si
 * hay) y cual es el efectivo -- para que se vea de donde sale cada uno.
 */
@Service
public class ObtenerPermisosUsuario {

    public record PermisoUsuarioVista(String permisoId, String modulo, String accion, String codigo,
                                       String descripcion, boolean vieneDeRol, Boolean excepcionOtorgado,
                                       String excepcionMotivo, boolean efectivo) {
    }

    private final PermisoRepositorio permisos;
    private final UsuarioRolRepositorio usuarioRoles;
    private final RolPermisoRepositorio rolPermisos;
    private final UsuarioPermisoExcepcionRepositorio excepciones;

    public ObtenerPermisosUsuario(PermisoRepositorio permisos, UsuarioRolRepositorio usuarioRoles,
                                   RolPermisoRepositorio rolPermisos,
                                   UsuarioPermisoExcepcionRepositorio excepciones) {
        this.permisos = permisos;
        this.usuarioRoles = usuarioRoles;
        this.rolPermisos = rolPermisos;
        this.excepciones = excepciones;
    }

    @Transactional(readOnly = true)
    public List<PermisoUsuarioVista> ejecutar(String usuarioId) {
        List<String> rolesIds = usuarioRoles.findByUsuarioId(usuarioId).stream()
                .map(UsuarioRol::getRol).map(rol -> rol.getId()).toList();
        Set<String> permisosDeRol = new HashSet<>();
        rolPermisos.findByRolIdIn(rolesIds).forEach(rp -> permisosDeRol.add(rp.getPermiso().getId()));

        Map<String, UsuarioPermisoExcepcion> excepcionPorPermiso = new HashMap<>();
        excepciones.findByUsuarioId(usuarioId).forEach(e -> excepcionPorPermiso.put(e.getPermiso().getId(), e));

        return permisos.findAllByOrderByModuloAscAccionAsc().stream()
                .map(p -> {
                    boolean deRol = permisosDeRol.contains(p.getId());
                    UsuarioPermisoExcepcion excepcion = excepcionPorPermiso.get(p.getId());
                    boolean efectivo = excepcion != null ? excepcion.isOtorgado() : deRol;
                    return new PermisoUsuarioVista(p.getId(), p.getModulo(), p.getAccion(), p.getCodigo(),
                            p.getDescripcion(), deRol, excepcion == null ? null : excepcion.isOtorgado(),
                            excepcion == null ? null : excepcion.getMotivo(), efectivo);
                })
                .toList();
    }
}
