package pe.rikos.operacion.produccion.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistroProduccionComando(
        String localId,
        String productoId,
        String recetaId,
        LocalDate fecha,
        String servicio,
        Integer porcionesPlanificadas,
        int porcionesPreparadas,
        BigDecimal rendimientoObtenido,
        String usuarioId
) {}
