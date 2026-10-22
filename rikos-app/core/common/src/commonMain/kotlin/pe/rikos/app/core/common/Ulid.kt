package pe.rikos.app.core.common

import kotlin.random.Random
import kotlin.time.Clock

/**
 * Identificador ULID de 26 caracteres (Crockford base32). El cliente genera los IDs de pedido,
 * línea y comanda, tal como espera el backend en los cuerpos de POST.
 */
object Ulid {
    private const val ALFABETO = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
    const val LARGO = 26

    fun nuevo(
        ahoraMs: Long = Clock.System.now().toEpochMilliseconds(),
        azar: Random = Random.Default,
    ): String {
        val sb = StringBuilder(LARGO)
        var t = ahoraMs
        val tiempo = CharArray(10)
        for (i in 9 downTo 0) {
            tiempo[i] = ALFABETO[(t and 31).toInt()]
            t = t shr 5
        }
        sb.append(tiempo)
        repeat(16) { sb.append(ALFABETO[azar.nextInt(32)]) }
        return sb.toString()
    }

    fun esValido(valor: String): Boolean =
        valor.length == LARGO && valor.all { it in ALFABETO }
}
