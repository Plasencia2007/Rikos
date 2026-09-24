package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocal;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRol;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;

import java.util.List;

/**
 * Reemplaza por completo los roles y los locales asignados a un usuario.
 * US-005: un usuario opera en todos los locales asignados y en ninguno mas.
 */
@Service
public class AsignarRolesYLocales {

    private final UsuarioRepositorio usuarios;
    private final RolRepositorio roles;
    private final LocalRepositorio locales;
    private final UsuarioRolRepositorio usuarioRoles;
    private final UsuarioLocalRepositorio usuarioLocales;

    public AsignarRolesYLocales(UsuarioRepositorio usuarios, RolRepositorio roles, LocalRepositorio locales,
                                 UsuarioRolRepositorio usuarioRoles, UsuarioLocalRepositorio usuarioLocales) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.locales = locales;
        this.usuarioRoles = usuarioRoles;
        this.usuarioLocales = usuarioLocales;
    }

    @Transactional
    public Resultado<Void, ErrorUsuario> ejecutar(
            String usuarioId, List<String> rolesIds, List<String> localesIds, String localPrincipalId,
            String actorId) {

        var usuarioOptional = usuarios.findById(usuarioId);
        if (usuarioOptional.isEmpty()) {
            return Resultado.fracaso(ErrorUsuario.NO_ENCONTRADO);
        }
        Usuario usuario = usuarioOptional.get();

        List<Rol> rolesEncontrados = roles.findByIdIn(rolesIds);
        if (rolesEncontrados.size() != rolesIds.size()) {
            return Resultado.fracaso(ErrorUsuario.ROL_INVALIDO);
        }

        List<Local> localesEncontrados = locales.findAllById(localesIds);
        if (localesEncontrados.size() != localesIds.size()) {
            return Resultado.fracaso(ErrorUsuario.LOCAL_INVALIDO);
        }

        usuarioRoles.deleteByUsuarioId(usuarioId);
        usuarioLocales.deleteByUsuarioId(usuarioId);

        for (Rol rol : rolesEncontrados) {
            usuarioRoles.save(new UsuarioRol(usuario, rol, actorId));
        }
        for (Local local : localesEncontrados) {
            boolean esPrincipal = local.getId().equals(localPrincipalId);
            usuarioLocales.save(new UsuarioLocal(usuario, local, esPrincipal, actorId));
        }

        return Resultado.exito(null);
    }
}
