package pe.rikos.operacion.inventario.aplicacion.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Representa una fila detallada del libro de movimientos de inventario (Kardex, ADM-17, US-044).
 * Cumple con RN-057 y RN-012 (los costos son nulos si el usuario no tiene permiso).
 */
public record FilaKardex(
        String id,
        String localId,
        String productoId,
        String productoCodigo,
        String productoNombre,
        String categoriaNombre,
        String origen,
        String signo,
        BigDecimal cantidad,
        String unidadMedidaId,
        String unidadMedidaNombre,
        String unidadMedidaSimbolo,
        BigDecimal cantidadBase,
        BigDecimal costoUnitario,
        BigDecimal costoTotal,
        BigDecimal stockAnterior,
        BigDecimal stockNuevo,
        BigDecimal diferencia,
        String referenciaTipo,
        String referenciaId,
        String motivoDescripcion,
        String observacion,
        OffsetDateTime ocurridoEn,
        OffsetDateTime registradoEn,
        String registradoPorId,
        String registradoPorNombre
) {
}
