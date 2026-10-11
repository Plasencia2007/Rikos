package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.comun.auditoria.Auditoria;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Sesion;
import pe.rikos.operacion.identidad.infraestructura.SesionRepositorio;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * "Actividad reciente" de la ficha de usuario: los ultimos accesos (sesion) y lo que la
 * persona hizo como actor (auditoria), mezclados y con lo mas reciente primero. Devuelve
 * datos crudos (tipo, evento, entidad): el texto para mostrar lo arma la pantalla.
 */
@Service
public class ObtenerActividadUsuario {

    static final int MAXIMO = 5;

    /** tipo: "sesion" (un acceso al sistema) o "auditoria" (una accion registrada). */
    public record Actividad(OffsetDateTime ocurridoEn, String tipo, String evento, String entidad) {
    }

    private final SesionRepositorio sesiones;
    private final AuditoriaRepositorio auditoria;

    public ObtenerActividadUsuario(SesionRepositorio sesiones, AuditoriaRepositorio auditoria) {
        this.sesiones = sesiones;
        this.auditoria = auditoria;
    }

    @Transactional(readOnly = true)
    public List<Actividad> ejecutar(String usuarioId) {
        Stream<Actividad> accesos = sesiones.findByUsuarioIdOrderByIniciadaEnDesc(usuarioId).stream()
                .filter(s -> "exitoso".equals(s.getResultado()))
                .limit(MAXIMO)
                .map((Sesion s) -> new Actividad(s.getIniciadaEn(), "sesion", "inicio_sesion", null));
        Stream<Actividad> acciones = auditoria.findTop10ByUsuarioIdOrderByOcurridoEnDesc(usuarioId).stream()
                .map((Auditoria a) -> new Actividad(a.getOcurridoEn(), "auditoria", a.getEvento(), a.getEntidad()));

        return Stream.concat(accesos, acciones)
                .sorted(Comparator.comparing(Actividad::ocurridoEn).reversed())
                .limit(MAXIMO)
                .toList();
    }
}
