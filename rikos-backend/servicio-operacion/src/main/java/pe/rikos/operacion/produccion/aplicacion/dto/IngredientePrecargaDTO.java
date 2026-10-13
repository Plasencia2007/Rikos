package pe.rikos.operacion.produccion.aplicacion.dto;

import java.math.BigDecimal;

public record IngredientePrecargaDTO(
        String productoId,
        String productoNombre,
        BigDecimal cantidadBase,
        String unidadMedidaSimbolo,
        BigDecimal stockActual,
        BigDecimal costoUnitario
) {}
