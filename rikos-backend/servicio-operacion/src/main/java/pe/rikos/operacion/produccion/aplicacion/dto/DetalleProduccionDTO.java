package pe.rikos.operacion.produccion.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record DetalleProduccionDTO(
        String id,
        String localId,
        String productoId,
        String productoCodigo,
        String productoNombre,
        String unidadMedidaSimbolo,
        boolean esSubreceta,
        String recetaId,
        LocalDate fecha,
        String servicio,
        Integer porcionesPlanificadas,
        int porcionesPreparadas,
        int porcionesVendidas,
        BigDecimal rendimientoObtenido,
        BigDecimal costoUnitarioEstimado,
        BigDecimal costoTotalLote,
        String estado,
        String registradoPorNombre,
        OffsetDateTime creadoEn
) {}
