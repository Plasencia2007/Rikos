package pe.rikos.operacion.venta.aplicacion;

import java.time.LocalDate;

/**
 * Los filtros de la lista de pedidos (sprint 6b). Todo opcional: situacion por defecto en_curso, pagina desde 0 y
 * tamano de 1 a 100. desde y hasta son fechas OPERATIVAS, ambas incluidas.
 */
public record FiltroDePedidos(String situacion, String canal, String estado, String q, LocalDate desde,
                              LocalDate hasta, int pagina, int tamano) {
}
