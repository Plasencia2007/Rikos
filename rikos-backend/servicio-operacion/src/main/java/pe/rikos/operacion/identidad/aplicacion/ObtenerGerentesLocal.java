package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Columna "gerente" de la lista de locales (seccion 3): el usuario con rol
 * "administrador de local" asignado a ese local, via usuario_local -- no
 * hace falta un campo nuevo, ya esta en el modelo (3.8).
 */
@Service
public class ObtenerGerentesLocal {

    private static final String ROL_ADMINISTRADOR_LOCAL = "administrador de local";

    private final UsuarioLocalRepositorio usuarioLocales;
    private final UsuarioRolRepositorio usuarioRoles;

    public ObtenerGerentesLocal(UsuarioLocalRepositorio usuarioLocales, UsuarioRolRepositorio usuarioRoles) {
        this.usuarioLocales = usuarioLocales;
        this.usuarioRoles = usuarioRoles;
    }

    @Transactional(readOnly = true)
    public Map<String, String> resolver(List<String> localIds) {
        Map<String, String> gerentes = new HashMap<>();
        for (String localId : localIds) {
            usuarioLocales.findByLocalIdAndUsuario_ActivoTrue(localId).stream()
                    .filter(ul -> tieneRolAdministradorLocal(ul.getUsuario().getId()))
                    .findFirst()
                    .ifPresent(ul -> gerentes.put(localId,
                            ul.getUsuario().getNombres() + " " + ul.getUsuario().getApellidos()));
        }
        return gerentes;
    }

    private boolean tieneRolAdministradorLocal(String usuarioId) {
        return usuarioRoles.findByUsuarioId(usuarioId).stream()
                .anyMatch(ur -> ROL_ADMINISTRADOR_LOCAL.equalsIgnoreCase(ur.getRol().getNombre()));
    }
}
