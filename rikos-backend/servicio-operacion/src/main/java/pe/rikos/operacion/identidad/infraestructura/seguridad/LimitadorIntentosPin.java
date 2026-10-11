package pe.rikos.operacion.identidad.infraestructura.seguridad;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;

import java.time.Duration;
import java.util.Optional;

/**
 * Bloqueo por intentos fallidos de PIN (US-003 CA... / RN-091), aplicado
 * por LOCAL + DISPOSITIVO (DT-039), no por usuario.
 *
 * DESVIACION DEL MODELO, marcada a proposito: 3.7 dice que intentos_fallidos
 * y bloqueado_hasta de "usuario" implementan este bloqueo, pero el PIN se
 * ingresa a ciegas (4 digitos, sin elegir usuario primero, tal como ya
 * estaba armada la pantalla del Sprint 0) -- un intento fallido no dice
 * *que* usuario fallo, asi que no hay una fila de "usuario" a la que
 * sumarle el contador. Las columnas usuario.intentos_fallidos /
 * bloqueado_hasta se usan en cambio para el login por contrasena (donde el
 * nombre de usuario si se conoce de entrada); este limitador persiste el
 * contador en la tabla bloqueo_pin (3.7 ter), por (local_id, dispositivo).
 *
 * DT-039 corrigio dos cosas del Sprint 1: (a) el bloqueo se cuenta por
 * DISPOSITIVO, no por local completo -- si no, un mozo que olvido su PIN
 * deja inservible la terminal de al lado durante quince minutos, en plena
 * hora de almuerzo; (b) se PERSISTE, no vive en memoria -- un bloqueo que
 * se borra reiniciando el proceso no es un bloqueo, y el nodo del local se
 * reinicia solo con cada corte de luz.
 *
 * "dispositivo" es hoy el header User-Agent (ver AutenticarConPin); si el
 * cliente no lo manda, se usa DISPOSITIVO_DESCONOCIDO como huella -- la
 * columna es NOT NULL y parte de la clave unica.
 */
@Component
@EnableConfigurationProperties(PropiedadesSeguridad.class)
public class LimitadorIntentosPin {

    private static final String DISPOSITIVO_DESCONOCIDO = "desconocido";
    private static final int LARGO_DISPOSITIVO = 120;

    private final BloqueoPinRepositorio bloqueos;
    private final PropiedadesSeguridad propiedades;

    public LimitadorIntentosPin(BloqueoPinRepositorio bloqueos, PropiedadesSeguridad propiedades) {
        this.bloqueos = bloqueos;
        this.propiedades = propiedades;
    }

    private static String normalizar(String dispositivo) {
        if (dispositivo == null || dispositivo.isBlank()) {
            return DISPOSITIVO_DESCONOCIDO;
        }
        // La columna mide 120 y un User-Agent real la supera: se recorta la huella (dos agentes que coincidan en los primeros
        // 120 caracteres comparten bloqueo, que es el lado seguro).
        return dispositivo.length() > LARGO_DISPOSITIVO ? dispositivo.substring(0, LARGO_DISPOSITIVO) : dispositivo;
    }

    public boolean estaBloqueado(String localId, String dispositivo) {
        return bloqueos.findByLocalIdAndDispositivo(localId, normalizar(dispositivo))
                .map(BloqueoPin::estaBloqueado)
                .orElse(false);
    }

    /** Para mostrar en pantalla cuanto falta. Vacio si no esta bloqueado. */
    public Optional<Long> segundosRestantes(String localId, String dispositivo) {
        return bloqueos.findByLocalIdAndDispositivo(localId, normalizar(dispositivo))
                .filter(BloqueoPin::estaBloqueado)
                .map(BloqueoPin::segundosRestantes);
    }

    @Transactional
    public void registrarFallo(String localId, String dispositivo) {
        String huella = normalizar(dispositivo);
        BloqueoPin bloqueo = bloqueos.findByLocalIdAndDispositivo(localId, huella)
                .orElseGet(() -> new BloqueoPin(Identificador.nuevo().valor(), localId, huella));
        bloqueo.registrarFallo(propiedades.getPin().getUmbralIntentos(),
                Duration.ofMinutes(propiedades.getPin().getMinutosBloqueo()));
        bloqueos.save(bloqueo);
    }

    @Transactional
    public void registrarExito(String localId, String dispositivo) {
        bloqueos.findByLocalIdAndDispositivo(localId, normalizar(dispositivo))
                .ifPresent(bloqueo -> {
                    bloqueo.registrarExito();
                    bloqueos.save(bloqueo);
                });
    }
}
