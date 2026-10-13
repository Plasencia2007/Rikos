package pe.rikos.operacion.produccion.aplicacion.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductoConRecetaDTO(
        String id,
        String codigo,
        String nombre,
        String unidadMedidaSimbolo,
        boolean esSubreceta,
        String recetaId,
        BigDecimal rendimientoBase,
        int porcionesBase,
        List<IngredientePrecargaDTO> ingredientes
) {}
