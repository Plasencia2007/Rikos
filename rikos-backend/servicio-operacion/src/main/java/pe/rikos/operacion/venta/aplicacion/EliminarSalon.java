package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Salon;
import pe.rikos.operacion.venta.infraestructura.SalonRepositorio;

/** Borra un salon vacio. Uno con mesas (aunque esten inactivas) no se borra: sus mesas guardan historial. */
@Service
public class EliminarSalon {

    private final SalonRepositorio salones;
    private final MesaRepositorio mesas;
    private final RegistradorAuditoria auditoria;

    public EliminarSalon(SalonRepositorio salones, MesaRepositorio mesas, RegistradorAuditoria auditoria) {
        this.salones = salones;
        this.mesas = mesas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Void, ErrorSalon> ejecutar(String id, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorSalon.SIN_LOCAL_ACTIVO);
        }
        Salon salon = salones.findByIdAndLocalId(id, localId).orElse(null);
        if (salon == null) {
            return Resultado.fracaso(ErrorSalon.NO_ENCONTRADO);
        }
        if (mesas.existsBySalonId(id)) {
            return Resultado.fracaso(ErrorSalon.TIENE_MESAS);
        }
        salones.delete(salon);
        auditoria.registrar("cambio_mesa", "salon", id,
                RegistradorAuditoria.campos("nombre", salon.getNombre(), "accion", "eliminado"), null);
        return Resultado.<Void, ErrorSalon>exito(null);
    }
}
