package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.CajaRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.caja.infraestructura.TurnoCajaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/**
 * Activa o desactiva una caja (Configuracion). Una caja desactivada deja de ofrecerse al abrir turno pero conserva su historial. No se
 * desactiva la principal (primero se elige otra) ni una con un turno abierto.
 */
@Service
public class CambiarEstadoCaja {

    private final CajaRepositorio cajas;
    private final TurnoCajaRepositorio turnos;
    private final RegistradorAuditoria auditoria;

    public CambiarEstadoCaja(CajaRepositorio cajas, TurnoCajaRepositorio turnos, RegistradorAuditoria auditoria) {
        this.cajas = cajas;
        this.turnos = turnos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Caja, ErrorCaja> ejecutar(String cajaId, boolean activa, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCaja.SIN_LOCAL_ACTIVO);
        }
        Caja caja = cajaId == null ? null : cajas.findByIdAndLocalId(cajaId, localId).orElse(null);
        if (caja == null) {
            return Resultado.fracaso(ErrorCaja.CAJA_NO_ENCONTRADA);
        }
        if (!activa) {
            if (caja.isPrincipal()) {
                return Resultado.fracaso(ErrorCaja.CAJA_PRINCIPAL);
            }
            if (turnos.findByCajaIdAndEstado(caja.getId(), TurnoCaja.ABIERTO).isPresent()) {
                return Resultado.fracaso(ErrorCaja.CAJA_CON_TURNO_ABIERTO);
            }
        }
        boolean antes = caja.isActivo();
        if (activa) {
            caja.activar();
        } else {
            caja.desactivar();
        }
        cajas.save(caja);
        auditoria.registrar("caja_modificada", "caja", caja.getId(), RegistradorAuditoria.campos("activa", antes),
                RegistradorAuditoria.campos("nombre", caja.getNombre(), "activa", activa));
        return Resultado.exito(caja);
    }
}
