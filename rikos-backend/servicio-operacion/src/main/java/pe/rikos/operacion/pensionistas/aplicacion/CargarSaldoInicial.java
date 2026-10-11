package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionista;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionistaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuenta;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuentaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Lo que el pensionista debe (cargo) o tiene a favor (credito) al empezar en el
 * sistema. Se carga UNA vez por cuenta; un error se corrige con un ajuste. La
 * fecha de corte (por defecto hoy) no puede ser futura.
 */
@Service
public class CargarSaldoInicial {

    private final PensionistaRepositorio pensionistas;
    private final CuentaPensionistaRepositorio cuentas;
    private final MovimientoCuentaRepositorio movimientos;
    private final LibroDeCuenta libro;
    private final RegistradorAuditoria auditoria;
    private final Clock reloj;

    public CargarSaldoInicial(PensionistaRepositorio pensionistas, CuentaPensionistaRepositorio cuentas,
                              MovimientoCuentaRepositorio movimientos, LibroDeCuenta libro,
                              RegistradorAuditoria auditoria, Clock reloj) {
        this.pensionistas = pensionistas;
        this.cuentas = cuentas;
        this.movimientos = movimientos;
        this.libro = libro;
        this.auditoria = auditoria;
        this.reloj = reloj;
    }

    @Transactional
    public Resultado<MovimientoCuenta, ErrorCuenta> ejecutar(String pensionistaId, long importe, Signo signo,
                                                            LocalDate fechaCorte, String descripcion, String actorId) {
        var pensionista = pensionistas.findById(pensionistaId).orElse(null);
        CuentaPensionista cuenta = cuentas.findByPensionistaId(pensionistaId).orElse(null);
        if (pensionista == null || cuenta == null) {
            return Resultado.fracaso(ErrorCuenta.NO_ENCONTRADO);
        }
        if (importe <= 0) {
            return Resultado.fracaso(ErrorCuenta.IMPORTE_INVALIDO);
        }
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCuenta.SIN_LOCAL_ACTIVO);
        }
        LocalDate hoy = LocalDate.now(reloj);
        LocalDate corte = fechaCorte != null ? fechaCorte : hoy;
        if (corte.isAfter(hoy)) {
            return Resultado.fracaso(ErrorCuenta.FECHA_FUTURA);
        }
        if (movimientos.existsByCuentaIdAndTipo(cuenta.getId(), TipoMovimiento.SALDO_INICIAL.codigo())) {
            return Resultado.fracaso(ErrorCuenta.SALDO_INICIAL_YA_REGISTRADO);
        }
        OffsetDateTime ocurrido = corte.equals(hoy) ? OffsetDateTime.now(reloj)
                : corte.atStartOfDay(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toOffsetDateTime();
        MovimientoCuenta movimiento = libro.agregar(null, cuenta.getId(), localId, TipoMovimiento.SALDO_INICIAL,
                signo, importe, null, null, descripcion, ocurrido, actorId);
        auditoria.registrar("movimiento_cuenta", "pensionista", pensionistaId, null,
                RegistradorAuditoria.campos("tipo", "saldo_inicial", "signo", signo.codigo(), "importe", importe,
                        "nombre", pensionista.nombreCompleto()));
        return Resultado.exito(movimiento);
    }
}
