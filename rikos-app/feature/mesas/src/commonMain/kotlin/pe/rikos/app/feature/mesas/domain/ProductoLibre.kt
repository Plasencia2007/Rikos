package pe.rikos.app.feature.mesas.domain

import pe.rikos.app.core.common.Dinero

enum class DestinoPreparacion(val etiqueta: String) {
    COCINA_CALIENTE("Cocina caliente"),
    BARRA("Barra"),
    NO_VA_A_COCINA("No va a cocina"),
}

data class ProductoLibre(
    val nombre: String,
    val precio: Dinero,
    val destino: DestinoPreparacion = DestinoPreparacion.NO_VA_A_COCINA,
) {
    fun formatearObservacion(): String? {
        return if (destino != DestinoPreparacion.NO_VA_A_COCINA) {
            "Prep: ${destino.etiqueta}"
        } else {
            null
        }
    }
}
