package pe.rikos.operacion.inventario.aplicacion.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Representa la respuesta detallada de un ajuste de inventario registrado (US-045 CA3).
 */
public record DetalleAjusteDTO(
        String id,
        String localId,
        String productoId,
        String productoCodigo,
        String productoNombre,
        String unidadMedida,
        String signo,
        BigDecimal cantidad,
        BigDecimal stockAnterior,
        BigDecimal stockNuevo,
        BigDecimal costoUnitario,
        BigDecimal costoTotal,
        String motivoId,
        String motivoDescripcion,
        String observacion,
        OffsetDateTime ocurridoEn,
        String registradoPorId,
        String registradoPorNombre
) {
}
