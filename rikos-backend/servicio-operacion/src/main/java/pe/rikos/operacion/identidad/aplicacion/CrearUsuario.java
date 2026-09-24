package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
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
 * US-005: alta de usuario con roles y asignacion multilocal. Las
 * excepciones de permiso por usuario (US-005 CA3) no entran hasta US-004,
 * sprint 3.
 */
@Service
public class CrearUsuario {

    private final UsuarioRepositorio usuarios;
    private final RolRepositorio roles;
    private final LocalRepositorio locales;
    private final UsuarioRolRepositorio usuarioRoles;
    private final UsuarioLocalRepositorio usuarioLocales;
    private final PasswordEncoder passwordEncoder;

    public CrearUsuario(UsuarioRepositorio usuarios, RolRepositorio roles, LocalRepositorio locales,
                         UsuarioRolRepositorio usuarioRoles, UsuarioLocalRepositorio usuarioLocales,
                         PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.roles = roles;
        this.locales = locales;
        this.usuarioRoles = usuarioRoles;
        this.usuarioLocales = usuarioLocales;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Resultado<Usuario, ErrorUsuario> ejecutar(
            String nombreUsuario, String nombres, String apellidos, String dni, String correo, String telefono,
            String contrasena, List<String> rolesIds, List<String> localesIds, String localPrincipalId,
            String actorId) {

        if (usuarios.existsByNombreUsuario(nombreUsuario)) {
            return Resultado.fracaso(ErrorUsuario.NOMBRE_USUARIO_DUPLICADO);
        }

        List<Rol> rolesEncontrados = roles.findByIdIn(rolesIds);
        if (rolesEncontrados.size() != rolesIds.size()) {
            return Resultado.fracaso(ErrorUsuario.ROL_INVALIDO);
        }

        List<Local> localesEncontrados = locales.findAllById(localesIds);
        if (localesEncontrados.size() != localesIds.size()) {
            return Resultado.fracaso(ErrorUsuario.LOCAL_INVALIDO);
        }

        String hash = passwordEncoder.encode(contrasena);
        Usuario usuario = new Usuario(Identificador.nuevo().valor(), nombreUsuario, nombres, apellidos, dni,
                correo, telefono, hash, actorId);
        usuarios.save(usuario);

        for (Rol rol : rolesEncontrados) {
            usuarioRoles.save(new UsuarioRol(usuario, rol, actorId));
        }
        for (Local local : localesEncontrados) {
            boolean esPrincipal = local.getId().equals(localPrincipalId);
            usuarioLocales.save(new UsuarioLocal(usuario, local, esPrincipal, actorId));
        }

        return Resultado.exito(usuario);
    }
}
