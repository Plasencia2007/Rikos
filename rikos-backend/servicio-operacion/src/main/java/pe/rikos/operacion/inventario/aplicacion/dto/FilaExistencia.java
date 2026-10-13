package pe.rikos.operacion.inventario.aplicacion.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Representa una fila en la vista de existencias por local (ADM-16, US-043).
 * Muestra el producto, su unidad base, su stock actual, su stock mínimo configurado
 * y el estado de alerta calculado.
 */
public record FilaExistencia(
    String localId,
    String productoId,
    String codigo,
    String nombre,
    String categoriaId,
    String categoriaNombre,
    String unidadId,
    String unidadCodigo,
    String unidadNombre,
    boolean controlaStock,
    BigDecimal stockActual,
    BigDecimal stockMinimo,
    String estadoAlerta,
    OffsetDateTime recalculadoEn
) {}
