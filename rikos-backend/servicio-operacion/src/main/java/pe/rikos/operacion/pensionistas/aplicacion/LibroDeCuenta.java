package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuenta;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuentaRepositorio;

import java.time.OffsetDateTime;

/**
 * La unica puerta de escritura al libro de la cuenta: agrega una fila. Es
 * idempotente por id (3.9): si el mismo movimiento llega dos veces -- un local
 * que reenvia tras un corte de internet -- devuelve el que ya estaba.
 */
@Component
class LibroDeCuenta {

    private final MovimientoCuentaRepositorio movimientos;

    LibroDeCuenta(MovimientoCuentaRepositorio movimientos) {
        this.movimientos = movimientos;
    }

    MovimientoCuenta agregar(String id, String cuentaId, String localId, TipoMovimiento tipo, Signo signo,
                             long importe, String pedidoId, String motivoId, String descripcion,
                             OffsetDateTime ocurridoEn, String actorId) {
        return agregar(id, cuentaId, localId, tipo, signo, importe, pedidoId, null, motivoId, descripcion, ocurridoEn, actorId);
    }

    MovimientoCuenta agregar(String id, String cuentaId, String localId, TipoMovimiento tipo, Signo signo,
                             long importe, String pedidoId, String abonoId, String motivoId, String descripcion,
                             OffsetDateTime ocurridoEn, String actorId) {
        String movimientoId = id != null ? id : Identificador.nuevo().valor();
        var existente = movimientos.findById(movimientoId);
        if (existente.isPresent()) {
            return existente.get();
        }
        return movimientos.save(new MovimientoCuenta(movimientoId, cuentaId, localId, tipo.codigo(), signo.codigo(),
                importe, pedidoId, abonoId, motivoId, descripcion, ocurridoEn, actorId));
    }
}
