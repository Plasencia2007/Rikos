package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.CajaRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.caja.infraestructura.TurnoCajaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/**
 * US-032: abre el turno de un cajero en una caja del local activo, con su fondo inicial (en centimos, puede ser cero).
 * Una caja tiene a lo sumo un turno abierto y un cajero tambien, asi que el dinero de cajas distintas no se cruza. El
 * cajero es quien abre: el turno se ata a la persona.
 */
@Service
public class AbrirTurno {

    private final CajaRepositorio cajas;
    private final TurnoCajaRepositorio turnos;
    private final RegistradorAuditoria auditoria;

    public AbrirTurno(CajaRepositorio cajas, TurnoCajaRepositorio turnos, RegistradorAuditoria auditoria) {
        this.cajas = cajas;
        this.turnos = turnos;
        this.auditoria = auditoria;
    }

    /** Sin nota de apertura. */
    public Resultado<TurnoCaja, ErrorCaja> ejecutar(String cajaId, Long fondoInicial, String cajeroId) {
        return ejecutar(cajaId, fondoInicial, cajeroId, null);
    }

    @Transactional
    public Resultado<TurnoCaja, ErrorCaja> ejecutar(String cajaId, Long fondoInicial, String cajeroId, String nota) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCaja.SIN_LOCAL_ACTIVO);
        }
        Caja caja = cajaId == null ? null : cajas.findByIdAndLocalId(cajaId, localId).orElse(null);
        if (caja == null) {
            return Resultado.fracaso(ErrorCaja.CAJA_NO_ENCONTRADA);
        }
        if (!caja.isActivo()) {
            return Resultado.fracaso(ErrorCaja.CAJA_INACTIVA);
        }
        if (fondoInicial == null || fondoInicial < 0) {
            return Resultado.fracaso(ErrorCaja.FONDO_INVALIDO);
        }
        String notaLimpia = nota == null || nota.isBlank() ? null : nota.strip();
        if (notaLimpia != null && notaLimpia.length() > 250) {
            return Resultado.fracaso(ErrorCaja.NOTA_LARGA);
        }
        if (turnos.findByCajaIdAndEstado(caja.getId(), TurnoCaja.ABIERTO).isPresent()) {
            return Resultado.fracaso(ErrorCaja.CAJA_CON_TURNO_ABIERTO);
        }
        if (turnos.findByCajeroIdAndEstado(cajeroId, TurnoCaja.ABIERTO).isPresent()) {
            return Resultado.fracaso(ErrorCaja.CAJERO_CON_TURNO_ABIERTO);
        }
        TurnoCaja turno = new TurnoCaja(Identificador.nuevo().valor(), caja.getId(), localId, cajeroId, fondoInicial);
        turno.anotarApertura(notaLimpia);
        turno = turnos.save(turno);
        auditoria.registrar("turno_abierto", "turno_caja", turno.getId(), null,
                RegistradorAuditoria.campos("caja", caja.getNombre(), "fondoInicial", fondoInicial));
        return Resultado.exito(turno);
    }
}
