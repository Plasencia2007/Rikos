package pe.rikos.operacion.identidad.infraestructura.seguridad;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueo por intentos fallidos de PIN (US-003 CA... / RN-091), aplicado
 * por LOCAL y no por usuario.
 *
 * DESVIACION DEL MODELO, marcada a proposito: 3.7 dice que intentos_fallidos
 * y bloqueado_hasta de "usuario" implementan este bloqueo, pero el PIN se
 * ingresa a ciegas (4 digitos, sin elegir usuario primero, tal como ya
 * estaba armada la pantalla del Sprint 0) -- un intento fallido no dice
 * *que* usuario fallo, asi que no hay una fila de "usuario" a la que
 * sumarle el contador. Las columnas usuario.intentos_fallidos /
 * bloqueado_hasta se usan en cambio para el login por contrasena (donde el
 * nombre de usuario si se conoce de entrada), y este limitador -- en
 * memoria, se pierde al reiniciar el proceso -- cubre el PIN por local con
 * los mismos umbrales de configuracion. Si el diseño real de la pantalla
 * de PIN termina siendo "elige tu nombre y despues tu PIN", esto se
 * reemplaza facil por el mismo esquema que ya usa la contrasena.
 */
@Component
@EnableConfigurationProperties(PropiedadesSeguridad.class)
public class LimitadorIntentosPin {

    private record Estado(int intentos, OffsetDateTime bloqueadoHasta) {
    }

    private final Map<String, Estado> porLocal = new ConcurrentHashMap<>();
    private final PropiedadesSeguridad propiedades;

    public LimitadorIntentosPin(PropiedadesSeguridad propiedades) {
        this.propiedades = propiedades;
    }

    public boolean estaBloqueado(String localId) {
        Estado estado = porLocal.get(localId);
        return estado != null && estado.bloqueadoHasta() != null && estado.bloqueadoHasta().isAfter(OffsetDateTime.now());
    }

    /** Para mostrar en pantalla cuanto falta. Vacio si no esta bloqueado. */
    public Optional<Long> segundosRestantes(String localId) {
        if (!estaBloqueado(localId)) {
            return Optional.empty();
        }
        OffsetDateTime bloqueadoHasta = porLocal.get(localId).bloqueadoHasta();
        return Optional.of(Duration.between(OffsetDateTime.now(), bloqueadoHasta).getSeconds());
    }

    public void registrarFallo(String localId) {
        porLocal.compute(localId, (id, actual) -> {
            int intentos = (actual == null ? 0 : actual.intentos()) + 1;
            OffsetDateTime bloqueadoHasta = intentos >= propiedades.getPin().getUmbralIntentos()
                    ? OffsetDateTime.now().plusMinutes(propiedades.getPin().getMinutosBloqueo())
                    : null;
            return new Estado(intentos, bloqueadoHasta);
        });
    }

    public void registrarExito(String localId) {
        porLocal.remove(localId);
    }
}
