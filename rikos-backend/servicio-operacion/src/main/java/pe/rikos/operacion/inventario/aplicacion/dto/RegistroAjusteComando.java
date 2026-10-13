package pe.rikos.operacion.inventario.aplicacion.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Comando para registrar un ajuste de inventario (US-045, RN-052).
 * Soporta modo variación ("INCREMENTO", "DISMINUCION") y modo conteo ("REEMPLAZO_STOCK").
 */
public record RegistroAjusteComando(
        String localId,
        String productoId,
        String motivoId,
        String tipoAjuste,
        BigDecimal cantidad,
        BigDecimal stockFinal,
        String observacion,
        String usuarioId,
        OffsetDateTime fecha
) {
}
