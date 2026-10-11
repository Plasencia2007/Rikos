package pe.rikos.operacion.venta.aplicacion;

import pe.rikos.operacion.venta.infraestructura.Pedido;

import java.util.List;

/** El pedido completo: personas, lineas y totales derivados (subtotal + taper = total). Centimos. */
public record PedidoDetallado(Pedido pedido, List<PersonaDetallada> personas, long subtotal, long taper, long total,
                              List<String> avisos, String mozoNombre) {
}
