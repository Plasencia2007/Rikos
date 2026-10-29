package pe.rikos.app.feature.mesas.domain

/**
 * Motivos predefinidos para la anulación de una línea de comanda según el lienzo de diseño M11.
 * Quedan registrados para auditoría y bitácora con el nombre del mozo.
 */
enum class MotivoAnulacion(val texto: String) {
    ERROR_AL_TOMAR("Error al tomar el pedido"),
    CAMBIO_DE_OPINION("El cliente cambió de opinión"),
    DEMORA_EN_COCINA("Demora en cocina"),
    PRODUCTO_AGOTADO("Producto agotado"),
}

/**
 * Opciones de indicaciones rápidas seleccionables en chips para cocina y barra.
 */
val indicacionesPredefinidas: List<String> = listOf(
    "Sin azúcar",
    "Para compartir",
    "Traer con el café",
)
