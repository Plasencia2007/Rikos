package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocal;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;
import pe.rikos.operacion.identidad.infraestructura.seguridad.JwtServicio;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Arma el token y el resumen de roles/locales para cualquiera de los dos
 * flujos de entrada (contrasena o PIN). US-002: "si tiene varios locales,
 * se elige el activo al entrar; si tiene uno, se asigna solo".
 */
@Component
class ResolutorSesionUsuario {

    /** Como se guarda en rol.nombre: administra todo el negocio, no un local. */
    static final String ROL_ADMINISTRADOR_CENTRAL = "administrador central";

    private final UsuarioRolRepositorio usuarioRoles;
    private final UsuarioLocalRepositorio usuarioLocales;
    private final LocalRepositorio localRepositorio;
    private final ResolverPermisosEfectivos resolverPermisosEfectivos;
    private final JwtServicio jwtServicio;

    ResolutorSesionUsuario(UsuarioRolRepositorio usuarioRoles, UsuarioLocalRepositorio usuarioLocales,
                            LocalRepositorio localRepositorio,
                            ResolverPermisosEfectivos resolverPermisosEfectivos, JwtServicio jwtServicio) {
        this.usuarioRoles = usuarioRoles;
        this.usuarioLocales = usuarioLocales;
        this.localRepositorio = localRepositorio;
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
        List<LocalResumen> locales = roles.contains(ROL_ADMINISTRADOR_CENTRAL)
                ? localesDelAdministradorCentral(asignaciones)
                : asignaciones.stream()
                        .map(ul -> new LocalResumen(ul.getLocal().getId(), ul.getLocal().getCodigo(),
                                ul.getLocal().getNombre(), ul.isEsPrincipal()))
                        .toList();

        String localActivoId = localActivoSugerido != null
                ? localActivoSugerido
                : asignaciones.stream()
                        .filter(UsuarioLocal::isEsPrincipal)
                        .map(ul -> ul.getLocal().getId())
                        .findFirst()
                        .orElseGet(() -> locales.isEmpty() ? null : locales.get(0).id());

        List<String> localesIds = locales.stream().map(LocalResumen::id).toList();
        List<String> permisos = resolverPermisosEfectivos.resolver(usuario.getId());
        String token = jwtServicio.generar(usuario.getId(), usuario.getNombreUsuario(), roles, permisos, localesIds,
                localActivoId);

        return new SesionIniciada(token, usuario.getId(), usuario.getNombreUsuario(), roles, locales, localActivoId,
                permisos);
    }

    /**
     * El administrador central opera en cualquier local activo, asignado o no:
     * un local recien creado tiene que aparecer en su selector sin que nadie
     * tenga que asignarselo a mano. Mantiene como principal el que ya tenia.
     */
    private List<LocalResumen> localesDelAdministradorCentral(List<UsuarioLocal> asignaciones) {
        Set<String> principales = asignaciones.stream()
                .filter(UsuarioLocal::isEsPrincipal)
                .map(ul -> ul.getLocal().getId())
                .collect(Collectors.toSet());
        return localRepositorio.findAllByOrderByNombreAsc().stream()
                .filter(Local::isActivo)
                .map(l -> new LocalResumen(l.getId(), l.getCodigo(), l.getNombre(), principales.contains(l.getId())))
                .toList();
    }

    /** true si el usuario puede pasar a operar en ese local (asignado, o central con el local activo). */
    boolean puedeOperarEn(Usuario usuario, String localId, boolean asignado) {
        if (asignado) {
            return true;
        }
        boolean esCentral = usuarioRoles.findByUsuarioId(usuario.getId()).stream()
                .anyMatch(ur -> ROL_ADMINISTRADOR_CENTRAL.equals(ur.getRol().getNombre()));
        return esCentral && localRepositorio.findById(localId).map(Local::isActivo).orElse(false);
    }
}
