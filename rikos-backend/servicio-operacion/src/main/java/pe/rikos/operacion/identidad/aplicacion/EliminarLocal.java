package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
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
 */
@Service
public class EliminarLocal {

    private final LocalRepositorio locales;
    private final UsuarioLocalRepositorio usuarioLocales;
    private final SesionRepositorio sesiones;

    public EliminarLocal(LocalRepositorio locales, UsuarioLocalRepositorio usuarioLocales,
                          SesionRepositorio sesiones) {
        this.locales = locales;
        this.usuarioLocales = usuarioLocales;
        this.sesiones = sesiones;
    }

    @Transactional
    public Resultado<Void, ErrorLocal> ejecutar(String id) {
        if (locales.findById(id).isEmpty()) {
            return Resultado.fracaso(ErrorLocal.NO_ENCONTRADO);
        }
        if (usuarioLocales.existsByLocalId(id) || sesiones.existsByLocalId(id)) {
            return Resultado.fracaso(ErrorLocal.TIENE_HISTORIAL);
        }
        locales.deleteById(id);
        return Resultado.<Void, ErrorLocal>exito(null);
    }
}
