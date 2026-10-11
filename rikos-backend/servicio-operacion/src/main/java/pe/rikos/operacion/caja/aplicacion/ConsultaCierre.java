package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.CierreCaja;
import pe.rikos.operacion.caja.infraestructura.CierreCajaRepositorio;
import pe.rikos.operacion.caja.infraestructura.MovimientoCajaRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.caja.infraestructura.TurnoCajaRepositorio;

import java.util.HashMap;
import java.util.Map;

/** US-036: el resumen del turno antes de contar y el cierre ya registrado de un turno. */
@Service
public class ConsultaCierre {

    private final TurnoCajaRepositorio turnos;
    private final MovimientoCajaRepositorio movimientos;
    private final CierreCajaRepositorio cierres;

    public ConsultaCierre(TurnoCajaRepositorio turnos, MovimientoCajaRepositorio movimientos, CierreCajaRepositorio cierres) {
        this.turnos = turnos;
        this.movimientos = movimientos;
        this.cierres = cierres;
    }

    /** El resumen del turno abierto de este cajero en el local activo. */
    @Transactional(readOnly = true)
    public Resultado<ResumenDeTurno, ErrorCaja> previo(String cajeroId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCaja.SIN_LOCAL_ACTIVO);
        }
        TurnoCaja turno = turnos.findByCajeroIdAndEstado(cajeroId, TurnoCaja.ABIERTO)
                .filter(t -> t.getLocalId().equals(localId)).orElse(null);
        if (turno == null) {
            return Resultado.fracaso(ErrorCaja.SIN_TURNO_ABIERTO);
        }
        return Resultado.exito(resumenDe(turno));
    }

    ResumenDeTurno resumenDe(TurnoCaja turno) {
        Map<String, Long> porMedio = new HashMap<>();
        for (Object[] fila : movimientos.sumasPorMedio(turno.getId())) {
            porMedio.put((String) fila[0], ((Number) fila[1]).longValue());
        }
        long efectivo = porMedio.getOrDefault("efectivo", 0L);
        return new ResumenDeTurno(turno, turno.getFondoInicial(), efectivo, turno.getFondoInicial() + efectivo,
                porMedio.getOrDefault("yape", 0L), porMedio.getOrDefault("plin", 0L), porMedio.getOrDefault("tarjeta", 0L),
                movimientos.contarCobrosVigentes(turno.getId()));
    }

    /** El cierre de un turno del local activo (un turno de otro local no se encuentra). */
    @Transactional(readOnly = true)
    public Resultado<CierreCaja, ErrorCaja> deTurno(String turnoId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCaja.SIN_LOCAL_ACTIVO);
        }
        return turnoId == null ? Resultado.fracaso(ErrorCaja.CAJA_NO_ENCONTRADA)
                : cierres.findByTurnoCajaId(turnoId).filter(c -> c.getLocalId().equals(localId))
                        .<Resultado<CierreCaja, ErrorCaja>>map(Resultado::exito)
                        .orElseGet(() -> Resultado.fracaso(ErrorCaja.CAJA_NO_ENCONTRADA));
    }
}
