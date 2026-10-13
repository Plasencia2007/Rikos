package pe.rikos.operacion.inventario.aplicacion.dto;

import java.time.LocalDate;

/**
 * Parámetros de filtrado multicriterio y paginación para consulta de Kardex (US-044).
 */
public record FiltroKardex(
        String productoId,
        String origen,
        LocalDate desde,
        LocalDate hasta,
        String orden,
        Integer pagina,
        Integer tamano
) {
    public int paginaSegura() {
        return pagina == null || pagina < 0 ? 0 : pagina;
    }

    public int tamanoSeguro() {
        return tamano == null || tamano <= 0 ? 25 : Math.min(tamano, 200);
    }

    public boolean esAscendente() {
        return "asc".equalsIgnoreCase(orden);
    }
}
