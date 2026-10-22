package pe.rikos.app.feature.mesas.domain

import pe.rikos.app.core.common.Dinero
import pe.rikos.app.core.common.Resultado

enum class EstadoMesa { LIBRE, OCUPADA, POR_COBRAR, LISTO, FUERA_DE_SERVICIO }

/** Vista de una mesa en el salón. `id` es ULID de 26 caracteres. */
data class Mesa(
    val id: String,
    val numero: String,
    val estado: EstadoMesa,
    val comensales: Int = 0,
    val total: Dinero = Dinero.CERO,
)

sealed interface ErrorMesas {
    data object SinConexion : ErrorMesas
    data object SinPermiso : ErrorMesas
    data object Desconocido : ErrorMesas
}

interface MesasRepository {
    /** `GET /api/mesas` del local activo. */
    suspend fun obtenerMesas(): Resultado<List<Mesa>, ErrorMesas>
}

class ObtenerMesas(private val repo: MesasRepository) {
    suspend operator fun invoke(): Resultado<List<Mesa>, ErrorMesas> = repo.obtenerMesas()
}
