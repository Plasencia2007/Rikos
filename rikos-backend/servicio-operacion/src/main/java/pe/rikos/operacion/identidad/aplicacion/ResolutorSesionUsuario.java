package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocal;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;
import pe.rikos.operacion.identidad.infraestructura.seguridad.JwtServicio;

import java.util.List;

/**
 * Arma el token y el resumen de roles/locales para cualquiera de los dos
 * flujos de entrada (contrasena o PIN). US-002: "si tiene varios locales,
 * se elige el activo al entrar; si tiene uno, se asigna solo".
 */
@Component
class ResolutorSesionUsuario {

    private final UsuarioRolRepositorio usuarioRoles;
    private final UsuarioLocalRepositorio usuarioLocales;
    private final ResolverPermisosEfectivos resolverPermisosEfectivos;
    private final JwtServicio jwtServicio;

    ResolutorSesionUsuario(UsuarioRolRepositorio usuarioRoles, UsuarioLocalRepositorio usuarioLocales,
                            ResolverPermisosEfectivos resolverPermisosEfectivos, JwtServicio jwtServicio) {
        this.usuarioRoles = usuarioRoles;
        this.usuarioLocales = usuarioLocales;
        this.resolverPermisosEfectivos = resolverPermisosEfectivos;
        this.jwtServicio = jwtServicio;
    }

    /**
     * @param localActivoSugerido si no es nulo (p. ej. el POS ya sabe en que
     *                            local esta), se usa tal cual; si es nulo, se
     *                            elige el local principal o, a falta de uno,
     *                            el primero asignado.
     */
    SesionIniciada construir(Usuario usuario, String localActivoSugerido) {
        List<String> roles = usuarioRoles.findByUsuarioId(usuario.getId()).stream()
                .map(ur -> ur.getRol().getNombre())
                .toList();

        List<UsuarioLocal> asignaciones = usuarioLocales.findByUsuarioId(usuario.getId());
        List<LocalResumen> locales = asignaciones.stream()
                .map(ul -> new LocalResumen(ul.getLocal().getId(), ul.getLocal().getCodigo(),
                        ul.getLocal().getNombre(), ul.isEsPrincipal()))
                .toList();

        String localActivoId = localActivoSugerido != null
                ? localActivoSugerido
                : asignaciones.stream()
                        .filter(UsuarioLocal::isEsPrincipal)
                        .map(ul -> ul.getLocal().getId())
                        .findFirst()
                        .orElseGet(() -> asignaciones.isEmpty() ? null : asignaciones.get(0).getLocal().getId());

        List<String> localesIds = locales.stream().map(LocalResumen::id).toList();
        List<String> permisos = resolverPermisosEfectivos.resolver(usuario.getId());
        String token = jwtServicio.generar(usuario.getId(), usuario.getNombreUsuario(), roles, permisos, localesIds,
                localActivoId);

        return new SesionIniciada(token, usuario.getId(), usuario.getNombreUsuario(), roles, locales, localActivoId,
                permisos);
    }
}
