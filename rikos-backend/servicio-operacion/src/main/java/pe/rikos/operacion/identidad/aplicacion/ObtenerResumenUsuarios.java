package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.identidad.infraestructura.Sesion;
import pe.rikos.operacion.identidad.infraestructura.SesionRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocal;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRol;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Para la lista de usuarios (US-005): roles asignados (con color), local
 * principal, y ultimo acceso -- que sale de `sesion`, ya se viene llenando
 * desde el sprint 1 (US-002 CA5).
 */
@Service
public class ObtenerResumenUsuarios {

    public record RolResumen(String id, String nombre, String color, String descripcion) {
    }

    public record LocalPrincipalResumen(String id, String nombre) {
    }

    public record UltimoAcceso(OffsetDateTime ocurridoEn, String direccionIp) {
    }

    public record Resumen(List<RolResumen> roles, LocalPrincipalResumen localPrincipal,
                           List<LocalPrincipalResumen> locales, UltimoAcceso ultimoAcceso) {
    }

    private final UsuarioRolRepositorio usuarioRoles;
    private final UsuarioLocalRepositorio usuarioLocales;
    private final SesionRepositorio sesiones;

    public ObtenerResumenUsuarios(UsuarioRolRepositorio usuarioRoles, UsuarioLocalRepositorio usuarioLocales,
                                   SesionRepositorio sesiones) {
        this.usuarioRoles = usuarioRoles;
        this.usuarioLocales = usuarioLocales;
        this.sesiones = sesiones;
    }

    @Transactional(readOnly = true)
    public Resumen resolver(String usuarioId) {
        List<RolResumen> roles = usuarioRoles.findByUsuarioId(usuarioId).stream()
                .map(UsuarioRol::getRol)
                .map(rol -> new RolResumen(rol.getId(), rol.getNombre(), rol.getColor(), rol.getDescripcion()))
                .toList();

        List<UsuarioLocal> asignaciones = usuarioLocales.findByUsuarioId(usuarioId);
        LocalPrincipalResumen localPrincipal = asignaciones.stream()
                .filter(UsuarioLocal::isEsPrincipal)
                .map(ul -> new LocalPrincipalResumen(ul.getLocal().getId(), ul.getLocal().getNombre()))
                .findFirst()
                .orElse(null);

        Optional<Sesion> ultima = sesiones.findByUsuarioIdOrderByIniciadaEnDesc(usuarioId).stream()
                .filter(s -> "exitoso".equals(s.getResultado()))
                .findFirst();
        UltimoAcceso ultimoAcceso = ultima.map(s -> new UltimoAcceso(s.getIniciadaEn(), s.getDireccionIp()))
                .orElse(null);

        List<LocalPrincipalResumen> locales = asignaciones.stream()
                .map(ul -> new LocalPrincipalResumen(ul.getLocal().getId(), ul.getLocal().getNombre()))
                .sorted(java.util.Comparator.comparing(LocalPrincipalResumen::nombre))
                .toList();

        return new Resumen(roles, localPrincipal, locales, ultimoAcceso);
    }
}
