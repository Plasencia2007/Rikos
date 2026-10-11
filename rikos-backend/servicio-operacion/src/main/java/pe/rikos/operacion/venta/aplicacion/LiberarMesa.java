package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;

/** Quita el bloqueo de una mesa. Queda en la bitacora con el motivo que tenia. */
@Service
public class LiberarMesa {

    private final MesaRepositorio mesas;
    private final RegistradorAuditoria auditoria;

    public LiberarMesa(MesaRepositorio mesas, RegistradorAuditoria auditoria) {
        this.mesas = mesas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Mesa, ErrorMesa> ejecutar(String id, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        Mesa mesa = mesas.findByIdAndLocalId(id, localId).orElse(null);
        if (mesa == null) {
            return Resultado.fracaso(ErrorMesa.NO_ENCONTRADA);
        }
        if (!mesa.isBloqueada()) {
            return Resultado.fracaso(ErrorMesa.NO_BLOQUEADA);
        }
        String motivoQueTenia = mesa.getMotivoBloqueo();
        mesa.liberar(actorId);
        mesas.save(mesa);
        auditoria.registrar("bloqueo_mesa", "mesa", id,
                RegistradorAuditoria.campos("bloqueada", true, "motivo", motivoQueTenia, "nombre",
                        "Mesa " + mesa.getNumero()),
                RegistradorAuditoria.campos("bloqueada", false, "nombre", "Mesa " + mesa.getNumero()));
        return Resultado.exito(mesa);
    }
}
