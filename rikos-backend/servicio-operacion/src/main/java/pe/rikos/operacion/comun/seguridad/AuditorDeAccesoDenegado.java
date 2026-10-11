package pe.rikos.operacion.comun.seguridad;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

import java.io.IOException;

/**
 * Caso critico 19 (13.1): "el intento denegado tambien se registra". Un
 * 403 real (usuario autenticado, sin el permiso/rol que la ruta exige) no
 * dejaba ningun rastro hasta ahora -- la auditoria solo veia lo que si
 * habia pasado.
 *
 * "denegado" no tiene una entidad de negocio propia a la que apuntar (por
 * eso es 403 -- justamente no se llego a resolver ningun registro), asi
 * que entidad/entidadId son un valor fijo y el detalle real (metodo +
 * ruta) va en observacion, el unico campo de texto libre.
 *
 * Delega en AccessDeniedHandlerImpl para la respuesta HTTP real -- esto
 * solo agrega la auditoria antes.
 */
public class AuditorDeAccesoDenegado implements AccessDeniedHandler {

    private final RegistradorAuditoria auditoria;
    private final AccessDeniedHandler delegado = new AccessDeniedHandlerImpl();

    public AuditorDeAccesoDenegado(RegistradorAuditoria auditoria) {
        this.auditoria = auditoria;
    }

    @Override
    public void handle(HttpServletRequest peticion, HttpServletResponse respuesta,
                        AccessDeniedException excepcion) throws IOException, ServletException {
        // Solo hay usuario si la peticion llego autenticada (si no, es un 401,
        // no un 403, y ExceptionTranslationFilter ni llama a este handler).
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            String detalle = peticion.getMethod() + " " + peticion.getRequestURI();
            auditoria.registrar("acceso_denegado", "acceso", "denegado", null, null, null,
                    detalle.length() > 500 ? detalle.substring(0, 500) : detalle);
        }
        delegado.handle(peticion, respuesta, excepcion);
    }
}
