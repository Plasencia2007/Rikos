package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.CierreCaja;
import pe.rikos.operacion.caja.infraestructura.CierreCajaRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.caja.infraestructura.TurnoCajaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

import java.time.OffsetDateTime;

/**
 * US-036: el cajero cierra SU turno cuando quiera (al terminar su turno, al final del dia o en cualquier momento) contando el
 * efectivo. El sistema compara lo contado con lo esperado: desde S/ 0,01 de diferencia hay alerta, pero el cierre nunca se
 * bloquea (RN-070). El cierre queda inalterable (RN-006) y auditado. Un pedido a medio cobrar no impide cerrar el turno.
 */
@Service
public class CerrarTurno {

    private static final int MAXIMO_OBSERVACION = 500;

    private final TurnoCajaRepositorio turnos;
    private final CierreCajaRepositorio cierres;
    private final ConsultaCierre consulta;
    private final RegistradorAuditoria auditoria;

    public CerrarTurno(TurnoCajaRepositorio turnos, CierreCajaRepositorio cierres, ConsultaCierre consulta,
                       RegistradorAuditoria auditoria) {
        this.turnos = turnos;
        this.cierres = cierres;
        this.consulta = consulta;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<CierreRegistrado, ErrorCaja> ejecutar(String cajeroId, Long efectivoContado, String observacion) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCaja.SIN_LOCAL_ACTIVO);
        }
        TurnoCaja turno = turnos.findByCajeroIdAndEstado(cajeroId, TurnoCaja.ABIERTO)
                .filter(t -> t.getLocalId().equals(localId)).orElse(null);
        if (turno == null) {
            return Resultado.fracaso(ErrorCaja.SIN_TURNO_ABIERTO);
        }
        if (efectivoContado == null || efectivoContado < 0) {
            return Resultado.fracaso(ErrorCaja.CONTADO_INVALIDO);
        }
        String nota = observacion == null || observacion.isBlank() ? null : observacion.strip();
        if (nota != null && nota.length() > MAXIMO_OBSERVACION) {
            return Resultado.fracaso(ErrorCaja.OBSERVACION_LARGA);
        }

        ResumenDeTurno resumen = consulta.resumenDe(turno);
        OffsetDateTime ahora = OffsetDateTime.now();
        CierreCaja cierre = cierres.save(new CierreCaja(Identificador.nuevo().valor(), turno.getId(), localId,
                resumen.efectivoEsperado(), efectivoContado, resumen.totalYape(), resumen.totalPlin(),
                resumen.totalTarjeta(), nota, ahora, cajeroId));
        turno.cerrar(cajeroId, ahora);
        turnos.save(turno);
        boolean alerta = cierre.getDiferencia() != 0;
        auditoria.registrar("cierre_caja", "turno_caja", turno.getId(), null,
                RegistradorAuditoria.campos("esperado", cierre.getEfectivoEsperado(), "contado", cierre.getEfectivoContado(),
                        "diferencia", cierre.getDiferencia(), "alerta", alerta), null, nota);
        return Resultado.exito(new CierreRegistrado(cierre, alerta));
    }
}
