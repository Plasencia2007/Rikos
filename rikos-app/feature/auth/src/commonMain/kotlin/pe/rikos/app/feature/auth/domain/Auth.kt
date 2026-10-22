package pe.rikos.app.feature.auth.domain

import pe.rikos.app.core.common.Resultado

/** Respuesta de `POST /api/auth/pin` (SesionIniciada). */
data class Sesion(
    val token: String,
    val usuarioId: String,
    val nombreUsuario: String,
    val roles: List<String>,
    val localActivoId: String,
    val permisos: Set<String>,
)

sealed interface ErrorAuth {
    /** 401 */ data object PinIncorrecto : ErrorAuth
    /** 423: bloqueo por intentos, persistido por dispositivo (DT-039). */ data object Bloqueado : ErrorAuth
    data object SinConexion : ErrorAuth
    data object Desconocido : ErrorAuth
}

interface AuthRepository {
    /** El POS identifica el local por código (`localCodigo`, DT-040). */
    suspend fun iniciarConPin(localCodigo: String, pin: String): Resultado<Sesion, ErrorAuth>
}

/** Caso de uso: valida el formato antes de llamar al repositorio (4 dígitos, DT-028). */
class IniciarConPin(private val repo: AuthRepository) {
    suspend operator fun invoke(localCodigo: String, pin: String): Resultado<Sesion, ErrorAuth> {
        if (pin.length != LARGO_PIN || !pin.all { it in '0'..'9' }) {
            return Resultado.Fallo(ErrorAuth.PinIncorrecto)
        }
        return repo.iniciarConPin(localCodigo, pin)
    }

    companion object { const val LARGO_PIN = 4 }
}
