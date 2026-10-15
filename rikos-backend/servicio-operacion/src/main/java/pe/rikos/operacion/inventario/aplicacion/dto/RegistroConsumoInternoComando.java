package pe.rikos.operacion.inventario.aplicacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RegistroConsumoInternoComando(
        String localId,
        String usuarioId,
        LocalDate fecha,
        String servicio,
        String turno,
        String productoId,
        BigDecimal cantidad,
        String observacion,
        String operadorId,
        String supervisorPin
) {}
