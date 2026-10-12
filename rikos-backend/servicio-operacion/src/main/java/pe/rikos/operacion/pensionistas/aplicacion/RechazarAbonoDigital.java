package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.aplicacion.ErrorAbono;
import pe.rikos.operacion.catalogo.motivos.ConsultaMotivos;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.pensionistas.infraestructura.Abono;
import pe.rikos.operacion.pensionistas.infraestructura.AbonoRepositorio;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * US-024 (CA3): Rechazar un abono digital en estado pendiente.
 * No altera el saldo del pensionista (CA4). Exige un motivo formal (RN-004) y audita la acción.
 */
@Service
public class RechazarAbonoDigital {

    private final AbonoRepositorio abonos;
    private final ConsultaMotivos motivos;
    private final RegistradorAuditoria auditoria;
    private final Clock reloj;

    public RechazarAbonoDigital(AbonoRepositorio abonos, ConsultaMotivos motivos,
                                RegistradorAuditoria auditoria, Clock reloj) {
        this.abonos = abonos;
        this.motivos = motivos;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado<Abono, ErrorAbono> ejecutar(String abonoId, String motivoId, String observacion, String actorId) {
        Abono abono = abonos.findById(abonoId).orElse(null);
        if (abono == null) {
            return Resultado.fracaso(ErrorAbono.ABONO_NO_ENCONTRADO);
        }
        if (!abono.esPendiente()) {
            return Resultado.fracaso(ErrorAbono.ABONO_YA_PROCESADO);
        }

        var motivoOpt = motivos.resolver(motivoId, "rechazo_abono");
        if (motivoOpt.isEmpty()) {
            return Resultado.fracaso(ErrorAbono.MOTIVO_INVALIDO);
        }
        var motivo = motivoOpt.get();

        String obsLimpia = observacion == null || observacion.isBlank() ? null : observacion.strip();
        if (motivo.requiereObservacion() && obsLimpia == null) {
            return Resultado.fracaso(ErrorAbono.OBSERVACION_REQUERIDA);
        }

        OffsetDateTime ahora = OffsetDateTime.now(reloj);

        // Actualizar abono a rechazado con motivo y observación
        abono.rechazar(actorId, motivoId, obsLimpia, ahora);
        abonos.save(abono);

        // Auditoría transversal con motivo y observación (RN-004)
        auditoria.registrar("rechazo_abono", "abono", abono.getId(),
                RegistradorAuditoria.campos("estado", "pendiente"),
                RegistradorAuditoria.campos("estado", "rechazado", "monto", abono.getMonto(),
                        "medio", abono.getMedio(), "numeroOperacion", abono.getNumeroOperacion()),
                motivoId, obsLimpia);

        return Resultado.exito(abono);
    }
}
