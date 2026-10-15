package pe.rikos.operacion.inventario.aplicacion.dto;

import java.math.BigDecimal;

public record ResumenConsumoInternoDTO(
        long totalComidas,
        BigDecimal costoTotal,
        long totalConAutorizacion
) {}
