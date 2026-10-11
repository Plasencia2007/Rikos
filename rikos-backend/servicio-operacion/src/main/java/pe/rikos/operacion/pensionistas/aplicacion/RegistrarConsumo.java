package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionista;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionistaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuenta;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;

import java.time.OffsetDateTime;

/**
 * Carga un consumo a la cuenta (RN-018, RN-019): siempre un cargo, con el local
 * donde ocurrio. No hay que decidir "primero el saldo a favor": el saldo es la
 * suma del libro, asi que un credito previo se consume solo. El tope NUNCA lo
 * impide (RN-017). El id lo trae el dispositivo para que reenviar no duplique.
 * Lo llama la caja (sprint 7); aun no tiene endpoint.
 */
@Service
public class RegistrarConsumo {

    private final PensionistaRepositorio pensionistas;
    private final CuentaPensionistaRepositorio cuentas;
    private final LibroDeCuenta libro;

    public RegistrarConsumo(PensionistaRepositorio pensionistas, CuentaPensionistaRepositorio cuentas,
                            LibroDeCuenta libro) {
        this.pensionistas = pensionistas;
        this.cuentas = cuentas;
        this.libro = libro;
    }

    @Transactional
    public Resultado<MovimientoCuenta, ErrorCuenta> ejecutar(String movimientoId, String pensionistaId,
                                                            String localId, long importe, String pedidoId,
                                                            OffsetDateTime ocurridoEn, String actorId) {
        var pensionista = pensionistas.findById(pensionistaId).orElse(null);
        CuentaPensionista cuenta = cuentas.findByPensionistaId(pensionistaId).orElse(null);
        if (pensionista == null || cuenta == null) {
            return Resultado.fracaso(ErrorCuenta.NO_ENCONTRADO);
        }
        if (importe <= 0) {
            return Resultado.fracaso(ErrorCuenta.IMPORTE_INVALIDO);
        }
        if (!pensionista.isActivo()) {
            return Resultado.fracaso(ErrorCuenta.PENSIONISTA_INACTIVO);
        }
        return Resultado.exito(libro.agregar(movimientoId, cuenta.getId(), localId, TipoMovimiento.CONSUMO,
                Signo.CARGO, importe, pedidoId, null, null, ocurridoEn, actorId));
    }
}
