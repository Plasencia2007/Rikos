package pe.rikos.operacion.caja.aplicacion;

import pe.rikos.operacion.caja.infraestructura.Cobro;
import pe.rikos.operacion.caja.infraestructura.Pago;

import java.util.List;

/** Lo que devuelve un cobro: el cobro, sus pagos, los cargos a cuentas y si el pedido quedo cerrado (la mesa libre). */
public record CobroRegistrado(Cobro cobro, List<Pago> pagos, List<CargoRegistrado> cargos, boolean pedidoCerrado) {
}
