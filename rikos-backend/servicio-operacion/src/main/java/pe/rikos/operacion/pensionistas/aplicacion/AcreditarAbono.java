package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionista;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionistaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuenta;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;

import java.time.OffsetDateTime;

/**
 * Acredita un abono confirmado a la cuenta corriente del pensionista (RN-030).
 * Genera un crédito en el libro de la cuenta con el abono_id correspondiente.
 * RN-034: Si el pensionista está inactivo, el abono se permite para amortizar deudas pendientes.
 */
@Service
public class AcreditarAbono {

    private final PensionistaRepositorio pensionistas;
    private final CuentaPensionistaRepositorio cuentas;
    private final LibroDeCuenta libro;

    public AcreditarAbono(PensionistaRepositorio pensionistas, CuentaPensionistaRepositorio cuentas,
                          LibroDeCuenta libro) {
        this.pensionistas = pensionistas;
        this.cuentas = cuentas;
        this.libro = libro;
    }

    @Transactional
    public Resultado<MovimientoCuenta, ErrorCuenta> ejecutar(String movimientoId, String pensionistaId,
                                                            String localId, long importe, String abonoId,
                                                            String descripcion, OffsetDateTime ocurridoEn,
                                                            String actorId) {
        Pensionista pensionista = pensionistas.findById(pensionistaId).orElse(null);
        CuentaPensionista cuenta = cuentas.findByPensionistaId(pensionistaId).orElse(null);
        if (pensionista == null || cuenta == null) {
            return Resultado.fracaso(ErrorCuenta.NO_ENCONTRADO);
        }
        if (importe <= 0) {
            return Resultado.fracaso(ErrorCuenta.IMPORTE_INVALIDO);
        }
        return Resultado.exito(libro.agregar(movimientoId, cuenta.getId(), localId, TipoMovimiento.ABONO,
                Signo.CREDITO, importe, null, abonoId, null, descripcion, ocurridoEn, actorId));
    }
}
