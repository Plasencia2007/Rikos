package pe.rikos.app.feature.configuracion.domain

/**
 * Representa un local físico disponible en el servidor (M01 · Configurar local).
 */
data class Local(
    val codigo: String,
    val nombre: String,
    val direccion: String,
    val salonesTexto: String,
)
