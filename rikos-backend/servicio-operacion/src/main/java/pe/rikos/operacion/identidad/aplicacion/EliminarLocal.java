package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicioRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.SesionRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;

/**
 * Borrado real de un local -- distinto de {@link CambiarEstadoLocal}, que es
 * la via normal (US-001 CA3) para sacar un local de operacion sin perder su
 * historial. Este caso de uso es la excepcion explicita: solo procede si el
 * local nunca llego a usarse (sin usuarios asignados, sin sesiones
 * iniciadas ahi). Con historial, la base de datos igual lo impediria --
 * usuario_local y sesion referencian local con ON DELETE RESTRICT -- pero
 * se valida antes para devolver un error claro en vez de una excepcion SQL.
 *
 * US-006: se audita con evento "eliminacion".
 */
@Service
public class EliminarLocal {

    private final LocalRepositorio locales;
    private final UsuarioLocalRepositorio usuarioLocales;
    private final SesionRepositorio sesiones;
    private final HorarioServicioRepositorio horarios;
    private final DiaEspecialRepositorio diasEspeciales;
    private final RegistradorAuditoria auditoria;

    public EliminarLocal(LocalRepositorio locales, UsuarioLocalRepositorio usuarioLocales,
                          SesionRepositorio sesiones, HorarioServicioRepositorio horarios,
                          DiaEspecialRepositorio diasEspeciales, RegistradorAuditoria auditoria) {
        this.locales = locales;
        this.usuarioLocales = usuarioLocales;
        this.sesiones = sesiones;
        this.horarios = horarios;
        this.diasEspeciales = diasEspeciales;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Void, ErrorLocal> ejecutar(String id) {
        Local local = locales.findById(id).orElse(null);
        if (local == null) {
            return Resultado.fracaso(ErrorLocal.NO_ENCONTRADO);
        }
        if (usuarioLocales.existsByLocalId(id) || sesiones.existsByLocalId(id)) {
            return Resultado.fracaso(ErrorLocal.TIENE_HISTORIAL);
        }
        // Horarios y dias especiales son configuracion del local, no historial: el asistente
        // de locales los carga al crear, asi que un local recien armado los tiene y aun asi
        // debe poder borrarse. Se borran antes (sus FK a local son RESTRICT).
        horarios.deleteByLocalId(id);
        diasEspeciales.deleteByLocalId(id);
        horarios.flush();
        diasEspeciales.flush();
        locales.deleteById(id);
        auditoria.registrar("eliminacion", "local", id,
                RegistradorAuditoria.campos("codigo", local.getCodigo(), "nombre", local.getNombre()), null);
        return Resultado.<Void, ErrorLocal>exito(null);
    }
}
