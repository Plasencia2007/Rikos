package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.identidad.infraestructura.Sesion;
import pe.rikos.operacion.identidad.infraestructura.SesionRepositorio;

/**
 * US-002 CA5 / RN-092: todo inicio de sesion se registra, exitoso o
 * fallido. usuario_id puede ser nulo (nombre de usuario inexistente, PIN
 * que no matchea con nadie): es justo el caso que mas interesa vigilar.
 */
@Component
class RegistradorSesion {

    private final SesionRepositorio sesiones;

    RegistradorSesion(SesionRepositorio sesiones) {
        this.sesiones = sesiones;
    }

    void registrar(String usuarioId, String localId, String tipo, String resultado, String dispositivo,
                    String direccionIp) {
        sesiones.save(new Sesion(Identificador.nuevo().valor(), usuarioId, localId, tipo, resultado, dispositivo,
                direccionIp));
    }
}
