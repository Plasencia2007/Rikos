package pe.rikos.operacion.venta.aplicacion;

import java.util.List;

/** Una pagina de la lista de pedidos: total = filas que cumplen el filtro (no solo las de esta pagina). */
public record PaginaDePedidos(List<PedidoResumen> contenido, int pagina, int tamano, long total, int totalPaginas) {
}
