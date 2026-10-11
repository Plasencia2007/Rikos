package pe.rikos.operacion.venta.aplicacion;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Un pedido tal como sale en la lista del POS. mesa* y salonNombre son null en un pedido para llevar; lineas es null
 * fuera de la situacion "en_curso". total en centimos (sin lo anulado).
 */
public record PedidoResumen(String id, String numero, String canal, String servicio, String estado, String mesaId,
                            String mesaNumero, String mesaNombre, String salonNombre, String nombreCliente,
                            int comensales, String mozoNombre, OffsetDateTime abiertoEn, long total,
                            List<LineaDeLista> lineas) {

    /** Una linea para la tarjeta: que se pidio y en que estado esta (pendiente o comandado). */
    public record LineaDeLista(int cantidad, String nombre, List<String> componentes, String estado) {
    }
}
