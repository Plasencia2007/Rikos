package pe.rikos.operacion.compras.aplicacion.dto;

import java.math.BigDecimal;

public record ItemDetalleCompraDto(
        String productoId,
        String presentacionId,
        BigDecimal cantidad,
        Long precioUnitario
) {}
