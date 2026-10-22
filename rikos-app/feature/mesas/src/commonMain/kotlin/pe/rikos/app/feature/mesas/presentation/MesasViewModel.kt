package pe.rikos.app.feature.mesas.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.rikos.app.core.common.Resultado
import pe.rikos.app.feature.mesas.domain.ErrorMesas
import pe.rikos.app.feature.mesas.domain.Mesa
import pe.rikos.app.feature.mesas.domain.ObtenerMesas

sealed interface MesasState {
    data object Cargando : MesasState
    data class Listo(val mesas: List<Mesa>) : MesasState
    data class Error(val error: ErrorMesas) : MesasState
}

class MesasViewModel(private val obtenerMesas: ObtenerMesas) : ViewModel() {
    private val _estado = MutableStateFlow<MesasState>(MesasState.Cargando)
    val estado: StateFlow<MesasState> = _estado.asStateFlow()

    init { cargar() }

    fun cargar() {
        _estado.value = MesasState.Cargando
        viewModelScope.launch {
            _estado.value = when (val r = obtenerMesas()) {
                is Resultado.Ok -> MesasState.Listo(r.valor)
                is Resultado.Fallo -> MesasState.Error(r.error)
            }
        }
    }
}
