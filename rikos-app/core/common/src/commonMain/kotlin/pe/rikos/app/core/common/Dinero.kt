package pe.rikos.app.core.common

/**
 * Dinero en céntimos (entero), igual que el backend. Nunca se usa Double para montos.
 */
@kotlin.jvm.JvmInline
value class Dinero(val centimos: Long) {
    operator fun plus(otro: Dinero) = Dinero(centimos + otro.centimos)
    operator fun minus(otro: Dinero) = Dinero(centimos - otro.centimos)
    operator fun times(cantidad: Int) = Dinero(centimos * cantidad)

    val soles: Long get() = centimos / 100
    val resto: Int get() = (centimos % 100).toInt().let { if (it < 0) -it else it }

    /** «S/ 12.50» */
    fun formatear(): String = "S/ ${soles}.${resto.toString().padStart(2, '0')}"

    companion object {
        val CERO = Dinero(0)
    }
}
