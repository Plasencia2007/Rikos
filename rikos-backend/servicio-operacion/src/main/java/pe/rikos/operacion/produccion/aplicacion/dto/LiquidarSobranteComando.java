package pe.rikos.operacion.produccion.aplicacion.dto;

import java.math.BigDecimal;

public record LiquidarSobranteComando(
        String produccionId,
        String localId,
        BigDecimal sobrante,
        String destinoSobrante,
        String motivoId,
        String observacion,
        String usuarioId
) {}
