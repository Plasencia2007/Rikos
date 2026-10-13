package pe.rikos.operacion.inventario.aplicacion.dto;

/**
 * Resumen de métricas y alertas del inventario por local para los KPI cards (ADM-16).
 */
public record ResumenAlertasInventario(
    long totalProductos,
    long totalBajoMinimo,
    long totalNegativos,
    long totalSinStock
) {}
