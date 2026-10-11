package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Salon;
import pe.rikos.operacion.venta.infraestructura.SalonRepositorio;

/** Activa o desactiva un salon: uno desactivado deja de aparecer en el mapa del POS pero conserva sus mesas. */
@Service
public class CambiarEstadoSalon {

    private final SalonRepositorio salones;
    private final RegistradorAuditoria auditoria;

    public CambiarEstadoSalon(SalonRepositorio salones, RegistradorAuditoria auditoria) {
        this.salones = salones;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Salon, ErrorSalon> ejecutar(String id, boolean activo, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorSalon.SIN_LOCAL_ACTIVO);
        }
        Salon salon = salones.findByIdAndLocalId(id, localId).orElse(null);
        if (salon == null) {
            return Resultado.fracaso(ErrorSalon.NO_ENCONTRADO);
        }
        if (salon.isActivo() != activo) {
            boolean antes = salon.isActivo();
            salon.cambiarEstado(activo, actorId);
            salones.save(salon);
            auditoria.registrar("cambio_mesa", "salon", id,
                    RegistradorAuditoria.campos("nombre", salon.getNombre(), "activo", antes),
                    RegistradorAuditoria.campos("nombre", salon.getNombre(), "activo", activo));
        }
        return Resultado.exito(salon);
    }
}
