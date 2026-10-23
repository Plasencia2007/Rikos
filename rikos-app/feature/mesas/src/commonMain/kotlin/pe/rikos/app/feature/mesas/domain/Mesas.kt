package pe.rikos.app.feature.mesas.domain

import pe.rikos.app.core.common.Dinero
import pe.rikos.app.core.common.Resultado

enum class EstadoMesa {
    LIBRE,
    OCUPADA,
    POR_COBRAR,
    LISTO,
    FUERA_DE_SERVICIO,
}

enum class FormaMesa {
    RECTANGULAR,
    REDONDA,
    UNIDA,
}

/** Vista de una mesa en el salón con su estado físico y operativo (DT-M03). */
data class Mesa(
    val id: String,
    val numero: String,
    val estado: EstadoMesa,
    val comensales: Int = 0,
    val total: Dinero = Dinero.CERO,
    val forma: FormaMesa = FormaMesa.RECTANGULAR,
    val mozoNombre: String? = null,
    val minutosTranscurridos: Int? = null,
    val unidaCon: String? = null,
    val mesaPrincipalNumero: String? = null,
    val comidaLista: Boolean = false,
    val seleccionada: Boolean = false,
    val salonId: String = "salon_principal",
)

enum class TipoAviso {
    PLATOS_LISTOS,
    PIDIO_CUENTA,
    TIEMPO_SIN_PASAR,
}

data class AvisoMesa(
    val id: String,
    val mesaNumero: String,
    val tipo: TipoAviso,
    val titulo: String,
    val subtitulo: String,
)

data class Salon(
    val id: String,
    val nombre: String,
    val totalMesas: Int,
)

sealed interface ErrorMesas {
    data object SinConexion : ErrorMesas
    data object SinPermiso : ErrorMesas
    data object Desconocido : ErrorMesas
}

interface MesasRepository {
    /** `GET /api/mesas` del local activo. */
    suspend fun obtenerMesas(): Resultado<List<Mesa>, ErrorMesas>
    suspend fun obtenerSalones(): List<Salon> = listOf(
        Salon("salon_principal", "Salón principal", 12),
        Salon("terraza", "Terraza", 6),
        Salon("para_llevar", "Para llevar", 2),
        Salon("delivery", "Delivery", 1),
    )
    suspend fun obtenerAvisos(): List<AvisoMesa> = emptyList()
}

class ObtenerMesas(private val repo: MesasRepository) {
    suspend operator fun invoke(): Resultado<List<Mesa>, ErrorMesas> = repo.obtenerMesas()
    suspend fun salones(): List<Salon> = repo.obtenerSalones()
    suspend fun avisos(): List<AvisoMesa> = repo.obtenerAvisos()
}
