package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.motivos.ConsultaMotivos;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionista;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionistaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuenta;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;

import java.time.OffsetDateTime;

/**
 * Corrige la cuenta sin tocar el pasado: el libro no se edita, se agrega un
 * cargo o un credito con su motivo (tipo "ajuste_cuenta"). Vale tambien para un
 * pensionista inactivo. Queda en la bitacora.
 */
@Service
public class RegistrarAjuste {

    static final String TIPO_MOTIVO = "ajuste_cuenta";

    private final PensionistaRepositorio pensionistas;
    private final CuentaPensionistaRepositorio cuentas;
    private final LibroDeCuenta libro;
    private final ConsultaMotivos motivos;
    private final RegistradorAuditoria auditoria;

    public RegistrarAjuste(PensionistaRepositorio pensionistas, CuentaPensionistaRepositorio cuentas,
                           LibroDeCuenta libro, ConsultaMotivos motivos, RegistradorAuditoria auditoria) {
        this.pensionistas = pensionistas;
        this.cuentas = cuentas;
        this.libro = libro;
        this.motivos = motivos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<MovimientoCuenta, ErrorCuenta> ejecutar(String pensionistaId, long importe, Signo signo,
                                                            String motivoId, String observacion, String actorId) {
        var pensionista = pensionistas.findById(pensionistaId).orElse(null);
        CuentaPensionista cuenta = cuentas.findByPensionistaId(pensionistaId).orElse(null);
        if (pensionista == null || cuenta == null) {
            return Resultado.fracaso(ErrorCuenta.NO_ENCONTRADO);
        }
        if (importe <= 0) {
            return Resultado.fracaso(ErrorCuenta.IMPORTE_INVALIDO);
        }
        if (motivoId == null || motivoId.isBlank()) {
            return Resultado.fracaso(ErrorCuenta.MOTIVO_OBLIGATORIO);
        }
        var motivo = motivos.resolver(motivoId, TIPO_MOTIVO).orElse(null);
        if (motivo == null) {
            return Resultado.fracaso(ErrorCuenta.MOTIVO_INVALIDO);
        }
        String texto = observacion == null || observacion.isBlank() ? null : observacion.trim();
        if (motivo.requiereObservacion() && texto == null) {
            return Resultado.fracaso(ErrorCuenta.OBSERVACION_OBLIGATORIA);
        }
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCuenta.SIN_LOCAL_ACTIVO);
        }
        MovimientoCuenta movimiento = libro.agregar(null, cuenta.getId(), localId, TipoMovimiento.AJUSTE, signo,
                importe, null, motivoId, texto, OffsetDateTime.now(), actorId);
        auditoria.registrar("movimiento_cuenta", "pensionista", pensionistaId, null,
                RegistradorAuditoria.campos("tipo", "ajuste", "signo", signo.codigo(), "importe", importe,
                        "motivo", motivo.descripcion(), "nombre", pensionista.nombreCompleto()));
        return Resultado.exito(movimiento);
    }
}
