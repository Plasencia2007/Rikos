package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.operacion.identidad.infraestructura.RolPermisoRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioPermisoExcepcion;
import pe.rikos.operacion.identidad.infraestructura.UsuarioPermisoExcepcionRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRol;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 3.9, US-005 CA3, RN-013: el permiso efectivo es la union de los permisos
 * de TODOS los roles del usuario, MAS las excepciones, y la excepcion gana
 * sobre el rol. Se resuelve una vez al iniciar sesion (no en cada
 * peticion) -- por eso vive en aplicacion y no en el filtro de seguridad.
 */
@Component
public class ResolverPermisosEfectivos {

    private final UsuarioRolRepositorio usuarioRoles;
    private final RolPermisoRepositorio rolPermisos;
    private final UsuarioPermisoExcepcionRepositorio excepciones;

    public ResolverPermisosEfectivos(UsuarioRolRepositorio usuarioRoles, RolPermisoRepositorio rolPermisos,
                                      UsuarioPermisoExcepcionRepositorio excepciones) {
        this.usuarioRoles = usuarioRoles;
        this.rolPermisos = rolPermisos;
        this.excepciones = excepciones;
    }

    public List<String> resolver(String usuarioId) {
        Set<String> efectivos = new HashSet<>();

        List<String> rolesIds = usuarioRoles.findByUsuarioId(usuarioId).stream().map(UsuarioRol::getRol)
                .map(rol -> rol.getId()).toList();
        rolPermisos.findByRolIdIn(rolesIds).forEach(rp -> efectivos.add(rp.getPermiso().getCodigo()));

        for (UsuarioPermisoExcepcion excepcion : excepciones.findByUsuarioId(usuarioId)) {
            String codigo = excepcion.getPermiso().getCodigo();
            if (excepcion.isOtorgado()) {
                efectivos.add(codigo);
            } else {
                efectivos.remove(codigo);
            }
        }

        return efectivos.stream().sorted().toList();
    }
}
