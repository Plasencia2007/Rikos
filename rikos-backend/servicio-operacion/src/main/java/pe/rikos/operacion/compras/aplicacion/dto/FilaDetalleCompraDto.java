package pe.rikos.operacion.compras.aplicacion.dto;

import java.math.BigDecimal;

public record FilaDetalleCompraDto(
        String id,
        String productoId,
        String productoCodigo,
        String productoNombre,
        String unidadBaseSimbolo,
        String presentacionId,
        String presentacionNombre,
        BigDecimal cantidad,
        BigDecimal cantidadBase,
        Long precioUnitario,
        Long subtotal
) {}
