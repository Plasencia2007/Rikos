package pe.rikos.app.core.common

/**
 * Resultado de una operación con errores esperables (mismo criterio que `Resultado<T,E>` del backend).
 * Los errores de negocio nunca se lanzan como excepción: se devuelven.
 */
sealed interface Resultado<out T, out E> {
    data class Ok<T>(val valor: T) : Resultado<T, Nothing>
    data class Fallo<E>(val error: E) : Resultado<Nothing, E>
}

inline fun <T, E, R> Resultado<T, E>.mapear(f: (T) -> R): Resultado<R, E> = when (this) {
    is Resultado.Ok -> Resultado.Ok(f(valor))
    is Resultado.Fallo -> this
}

inline fun <T, E> Resultado<T, E>.alFallar(f: (E) -> Unit): Resultado<T, E> {
    if (this is Resultado.Fallo) f(error)
    return this
}
