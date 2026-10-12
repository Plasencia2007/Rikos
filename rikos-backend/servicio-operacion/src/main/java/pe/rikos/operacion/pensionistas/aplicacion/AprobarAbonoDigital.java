package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.aplicacion.ErrorAbono;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.pensionistas.infraestructura.Abono;
import pe.rikos.operacion.pensionistas.infraestructura.AbonoRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * US-024 (CA2): Aprobar un abono digital en estado pendiente.
 * Acredita el saldo a la cuenta corriente del pensionista y audita la acción.
 */
@Service
public class AprobarAbonoDigital {

    private final AbonoRepositorio abonos;
    private final PensionistaRepositorio pensionistas;
    private final AcreditarAbono acreditarAbono;
    private final ConsultaCuenta consultaCuenta;
    private final RegistradorAuditoria auditoria;
    private final Clock reloj;

    public AprobarAbonoDigital(AbonoRepositorio abonos, PensionistaRepositorio pensionistas,
                               AcreditarAbono acreditarAbono, ConsultaCuenta consultaCuenta,
                               RegistradorAuditoria auditoria, Clock reloj) {
        this.abonos = abonos;
        this.pensionistas = pensionistas;
        this.acreditarAbono = acreditarAbono;
        this.consultaCuenta = consultaCuenta;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado<AbonoAprobado, ErrorAbono> ejecutar(String abonoId, String actorId) {
        Abono abono = abonos.findById(abonoId).orElse(null);
        if (abono == null) {
            return Resultado.fracaso(ErrorAbono.ABONO_NO_ENCONTRADO);
        }
        if (!abono.esPendiente()) {
            return Resultado.fracaso(ErrorAbono.ABONO_YA_PROCESADO);
        }

        Pensionista pensionista = pensionistas.findById(abono.getPensionistaId()).orElse(null);
        if (pensionista == null) {
            return Resultado.fracaso(ErrorAbono.PENSIONISTA_NO_ENCONTRADO);
        }

        OffsetDateTime ahora = OffsetDateTime.now(reloj);

        // Acreditar a la cuenta corriente del pensionista (RN-030/RN-031)
        String movId = Identificador.nuevo().valor();
        String desc = "Abono digital " + abono.getMedio()
                + (abono.getNumeroOperacion() != null ? " op. " + abono.getNumeroOperacion() : "");
        var resCredito = acreditarAbono.ejecutar(movId, abono.getPensionistaId(), abono.getLocalId(),
                abono.getMonto(), abono.getId(), desc, ahora, actorId);
        if (resCredito.esFracaso()) {
            return Resultado.fracaso(ErrorAbono.CUENTA_NO_ENCONTRADA);
        }

        // Actualizar abono
        abono.aprobar(actorId, ahora);
        abonos.save(abono);

        // Auditoría transversal
        auditoria.registrar("aprobacion_abono", "abono", abono.getId(),
                RegistradorAuditoria.campos("estado", "pendiente"),
                RegistradorAuditoria.campos("estado", "aprobado", "monto", abono.getMonto(),
                        "medio", abono.getMedio(), "numeroOperacion", abono.getNumeroOperacion()));

        var resumen = consultaCuenta.resumen(abono.getPensionistaId());
        long nuevoSaldo = resumen.map(ResumenDeCuenta::deuda).orElse(0L);
        long saldoAFavor = resumen.map(ResumenDeCuenta::saldoAFavor).orElse(0L);

        return Resultado.exito(new AbonoAprobado(
                abono,
                pensionista.getNombres() + " " + pensionista.getApellidos(),
                pensionista.getDni(),
                nuevoSaldo,
                saldoAFavor
        ));
    }
}
