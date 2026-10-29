package pe.rikos.app.feature.auth.data

import kotlinx.coroutines.delay
import pe.rikos.app.core.common.Resultado
import pe.rikos.app.feature.auth.domain.AuthRepository
import pe.rikos.app.feature.auth.domain.ErrorAuth
import pe.rikos.app.feature.auth.domain.Sesion

/**
 * Repositorio de mentira para desarrollar la pantalla sin backend.
 * PIN 1234 entra; 0000 simula el bloqueo (423); cualquier otro da 401.
 * TODO: reemplazar por `AuthApiRepository` (Ktor) contra `POST /api/auth/pin`.
 */
class FakeAuthRepository : AuthRepository {
    override suspend fun iniciarConPin(localCodigo: String, pin: String): Resultado<Sesion, ErrorAuth> {
        delay(400)
        return when (pin) {
            "1234" -> Resultado.Ok(
                Sesion(
                    token = "token-de-prueba",
                    usuarioId = "01J00000000000000000000001",
                    nombreUsuario = "Luis Quispe",
                    roles = listOf("MOZO"),
                    localActivoId = "01J00000000000000000000002",
                    permisos = emptySet(),
                ),
            )
            "0000" -> Resultado.Fallo(ErrorAuth.Bloqueado)
            else -> Resultado.Fallo(ErrorAuth.PinIncorrecto)
        }
    }
}
