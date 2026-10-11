package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.DisponibilidadProducto;
import pe.rikos.operacion.catalogo.infraestructura.DisponibilidadProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/**
 * US-015 CA3 / RN-066: volver a ofrecer un agotado en el local activo.
 * Registra usuario, hora y motivo (de tipo "disponibilidad", activo; con
 * observacion si el motivo la pide), en la fila y en la bitacora como
 * "reactivacion".
 */
@Service
public class ReactivarProducto {

    static final String TIPO_MOTIVO = "disponibilidad";

    private final DisponibilidadProductoRepositorio disponibilidades;
    private final MotivoRepositorio motivos;
    private final RegistradorAuditoria auditoria;

    public ReactivarProducto(DisponibilidadProductoRepositorio disponibilidades, MotivoRepositorio motivos,
                             RegistradorAuditoria auditoria) {
        this.disponibilidades = disponibilidades;
        this.motivos = motivos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<DisponibilidadProducto, ErrorDisponibilidad> ejecutar(String productoId, String motivoId,
                                                                        String observacion) {
        String localId = LocalActivo.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorDisponibilidad.SIN_LOCAL_ACTIVO);
        }
        DisponibilidadProducto disponibilidad = disponibilidades.findByLocalIdAndProductoId(localId, productoId)
                .orElse(null);
        if (disponibilidad == null || !disponibilidad.isAgotado()) {
            return Resultado.fracaso(ErrorDisponibilidad.NO_ESTA_AGOTADO);
        }
        if (motivoId == null || motivoId.isBlank()) {
            return Resultado.fracaso(ErrorDisponibilidad.MOTIVO_OBLIGATORIO);
        }
        Motivo motivo = motivos.findById(motivoId).orElse(null);
        if (motivo == null || !motivo.isActivo() || !TIPO_MOTIVO.equals(motivo.getTipo())) {
            return Resultado.fracaso(ErrorDisponibilidad.MOTIVO_INVALIDO);
        }
        String observacionLimpia = observacion == null || observacion.isBlank() ? null : observacion.trim();
        if (motivo.isRequiereObservacion() && observacionLimpia == null) {
            return Resultado.fracaso(ErrorDisponibilidad.OBSERVACION_OBLIGATORIA);
        }
        disponibilidad.reactivar(motivo, observacionLimpia, LocalActivo.usuario().orElse(null));
        DisponibilidadProducto guardada = disponibilidades.save(disponibilidad);
        String nombre = disponibilidad.getProducto().getNombre();
        auditoria.registrar("reactivacion", "producto", productoId,
                RegistradorAuditoria.campos("agotado", true, "nombre", nombre),
                RegistradorAuditoria.campos("agotado", false, "nombre", nombre),
                motivoId, observacionLimpia);
        return Resultado.exito(guardada);
    }
}
