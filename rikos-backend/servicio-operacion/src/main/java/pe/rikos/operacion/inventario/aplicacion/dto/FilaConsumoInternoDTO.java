package pe.rikos.operacion.inventario.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record FilaConsumoInternoDTO(
        String id,
        String localId,
        String usuarioId,
        String usuarioNombre,
        LocalDate fecha,
        String servicio,
        String turno,
        String productoId,
        String productoCodigo,
        String productoNombre,
        String unidadMedidaSimbolo,
        BigDecimal cantidad,
        BigDecimal costoUnitario,
        BigDecimal costoTotal,
        boolean requirioAutorizacion,
        String autorizadoPorId,
        String autorizadoPorNombre,
        String observacion,
        String movimientoId,
        OffsetDateTime creadoEn
) {}
