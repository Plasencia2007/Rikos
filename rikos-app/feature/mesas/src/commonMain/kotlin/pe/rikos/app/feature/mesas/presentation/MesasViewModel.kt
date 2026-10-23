package pe.rikos.app.feature.mesas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.rikos.app.core.common.Resultado
import pe.rikos.app.feature.mesas.domain.AvisoMesa
import pe.rikos.app.feature.mesas.domain.ErrorMesas
import pe.rikos.app.feature.mesas.domain.EstadoMesa
import pe.rikos.app.feature.mesas.domain.Mesa
import pe.rikos.app.feature.mesas.domain.ObtenerMesas
import pe.rikos.app.feature.mesas.domain.Salon

data class MesasState(
    val cargando: Boolean = true,
    val error: ErrorMesas? = null,
    val salones: List<Salon> = emptyList(),
    val salonActivoId: String = "salon_principal",
    val avisos: List<AvisoMesa> = emptyList(),
    val mesas: List<Mesa> = emptyList(),
    val mozoNombre: String = "Luis",
    val turnoNombre: String = "Turno almuerzo",
    val enLinea: Boolean = true,
) {
    val mesasFiltradas: List<Mesa>
        get() = mesas.filter { it.salonId == salonActivoId }

    val libresCount: Int
        get() = mesasFiltradas.count { it.estado == EstadoMesa.LIBRE }

    val ocupadasCount: Int
        get() = mesasFiltradas.count { it.estado == EstadoMesa.OCUPADA || it.estado == EstadoMesa.LISTO }

    val porCobrarCount: Int
        get() = mesasFiltradas.count { it.estado == EstadoMesa.POR_COBRAR }
}

class MesasViewModel(
    private val obtenerMesas: ObtenerMesas,
    mozoNombre: String = "Luis",
) : ViewModel() {

    private val _estado = MutableStateFlow(MesasState(mozoNombre = mozoNombre))
    val estado: StateFlow<MesasState> = _estado.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        _estado.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            val salones = obtenerMesas.salones()
            val avisos = obtenerMesas.avisos()
            when (val r = obtenerMesas()) {
                is Resultado.Ok -> {
                    _estado.update {
                        it.copy(
                            cargando = false,
                            error = null,
                            salones = salones,
                            avisos = avisos,
                            mesas = r.valor,
                        )
                    }
                }
                is Resultado.Fallo -> {
                    _estado.update {
                        it.copy(
                            cargando = false,
                            error = r.error,
                            salones = salones,
                            avisos = avisos,
                        )
                    }
                }
            }
        }
    }

    fun alSeleccionarSalon(salonId: String) {
        _estado.update { it.copy(salonActivoId = salonId) }
    }
}
