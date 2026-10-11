package pe.rikos.operacion.catalogo.motivos;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;

import java.util.Optional;

/**
 * La unica puerta de otros modulos a los motivos (US-012). Los que piden un
 * motivo -- hoy la cuenta del pensionista para sus ajustes -- no necesitan la
 * entidad, solo saber si el que les llego es valido para lo que van a hacer:
 * existe, esta activo y es del tipo esperado. Asi `catalogo.infraestructura`
 * no se abre a otros modulos.
 */
@Component
public class ConsultaMotivos {

    /** requiereObservacion: el motivo exige que quien lo usa escriba una observacion. */
    public record MotivoResuelto(String id, String descripcion, boolean requiereObservacion) {
    }

    private final MotivoRepositorio motivos;

    public ConsultaMotivos(MotivoRepositorio motivos) {
        this.motivos = motivos;
    }

    /** Vacio si no existe, esta desactivado o es de otro tipo. */
    @Transactional(readOnly = true)
    public Optional<MotivoResuelto> resolver(String motivoId, String tipoEsperado) {
        if (motivoId == null) {
            return Optional.empty();
        }
        return motivos.findById(motivoId)
                .filter(Motivo::isActivo)
                .filter(m -> tipoEsperado.equals(m.getTipo()))
                .map(m -> new MotivoResuelto(m.getId(), m.getDescripcion(), m.isRequiereObservacion()));
    }

    /** La descripcion de un motivo (aunque este desactivado, para mostrar el historial). */
    @Transactional(readOnly = true)
    public Optional<String> descripcion(String motivoId) {
        return motivoId == null ? Optional.empty() : motivos.findById(motivoId).map(Motivo::getDescripcion);
    }
}
