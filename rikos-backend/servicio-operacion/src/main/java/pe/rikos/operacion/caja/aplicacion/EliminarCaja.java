package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.CajaRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCajaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/**
 * Borra una caja que nunca se uso (Configuracion). Una caja con turnos (abiertos o cerrados) tiene historial y no se borra: se
 * desactiva. La principal tampoco se borra. Borrar en vez de desactivar solo sirve para corregir una caja creada por error.
 */
@Service
public class EliminarCaja {

    private final CajaRepositorio cajas;
    private final TurnoCajaRepositorio turnos;
    private final RegistradorAuditoria auditoria;

    public EliminarCaja(CajaRepositorio cajas, TurnoCajaRepositorio turnos, RegistradorAuditoria auditoria) {
        this.cajas = cajas;
        this.turnos = turnos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Void, ErrorCaja> ejecutar(String cajaId, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCaja.SIN_LOCAL_ACTIVO);
        }
        Caja caja = cajaId == null ? null : cajas.findByIdAndLocalId(cajaId, localId).orElse(null);
        if (caja == null) {
            return Resultado.fracaso(ErrorCaja.CAJA_NO_ENCONTRADA);
        }
        if (caja.isPrincipal()) {
            return Resultado.fracaso(ErrorCaja.CAJA_PRINCIPAL);
        }
        if (turnos.existsByCajaId(caja.getId())) {
            return Resultado.fracaso(ErrorCaja.TIENE_HISTORIAL);
        }
        cajas.delete(caja);
        auditoria.registrar("eliminacion", "caja", caja.getId(), RegistradorAuditoria.campos("nombre", caja.getNombre(), "codigo", caja.getCodigo()),
                null);
        return Resultado.exito(null);
    }
}
