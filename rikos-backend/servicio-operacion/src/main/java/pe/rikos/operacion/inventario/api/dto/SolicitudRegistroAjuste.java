package pe.rikos.operacion.inventario.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Payload HTTP para el registro de un ajuste de inventario (US-045).
 */
public record SolicitudRegistroAjuste(
        String productoId,
        String motivoId,
        String tipoAjuste,
        BigDecimal cantidad,
        BigDecimal stockFinal,
        String observacion,
        OffsetDateTime fecha
) {
}
