package pe.rikos.operacion.inventario.aplicacion.dto;

import java.util.List;

/**
 * Contenedor paginado de resultados de Kardex para cliente REST (US-044).
 */
public record PaginaKardex(
        List<FilaKardex> elementos,
        long totalElementos,
        int totalPaginas,
        int pagina,
        int tamano
) {
}
