package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.CajaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/** Elige la caja principal del local (la que viene elegida al abrir turno): una sola, siempre activa. */
@Service
public class DefinirCajaPrincipal {

    private final CajaRepositorio cajas;
    private final RegistradorAuditoria auditoria;

    public DefinirCajaPrincipal(CajaRepositorio cajas, RegistradorAuditoria auditoria) {
        this.cajas = cajas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Caja, ErrorCaja> ejecutar(String cajaId, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCaja.SIN_LOCAL_ACTIVO);
        }
        Caja nueva = cajaId == null ? null : cajas.findByIdAndLocalId(cajaId, localId).orElse(null);
        if (nueva == null) {
            return Resultado.fracaso(ErrorCaja.CAJA_NO_ENCONTRADA);
        }
        if (!nueva.isActivo()) {
            return Resultado.fracaso(ErrorCaja.CAJA_INACTIVA);
        }
        if (nueva.isPrincipal()) {
            return Resultado.exito(nueva);
        }
        // Se quita primero la anterior y se escribe en la base antes de marcar la nueva: el indice unico parcial no admite dos a la vez.
        for (Caja otra : cajas.findByLocalIdOrderByNombre(localId)) {
            if (otra.isPrincipal()) {
                otra.marcarPrincipal(false);
                cajas.saveAndFlush(otra);
            }
        }
        nueva.marcarPrincipal(true);
        cajas.save(nueva);
        auditoria.registrar("caja_modificada", "caja", nueva.getId(), RegistradorAuditoria.campos("principal", false),
                RegistradorAuditoria.campos("nombre", nueva.getNombre(), "principal", true));
        return Resultado.exito(nueva);
    }
}
