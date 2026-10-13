package pe.rikos.operacion.inventario.aplicacion.dto;

import java.math.BigDecimal;

/**
 * Solicitud para fijar o limpiar el stock mínimo de un producto en un local (RN-051).
 * Si stockMinimo es null, se limpia el umbral.
 */
public record SolicitudStockMinimo(
    BigDecimal stockMinimo
) {}
