package pe.rikos.app.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.rikos.app.core.common.Resultado
import pe.rikos.app.feature.auth.domain.ErrorAuth
import pe.rikos.app.feature.auth.domain.IniciarConPin
import pe.rikos.app.feature.auth.domain.Sesion

data class PinState(
    val digitos: String = "",
    val cargando: Boolean = false,
    val error: ErrorAuth? = null,
)

class PinViewModel(
    private val localCodigo: String,
    private val iniciarConPin: IniciarConPin,
    private val alIniciar: (Sesion) -> Unit,
) : ViewModel() {

    private val _estado = MutableStateFlow(PinState())
    val estado: StateFlow<PinState> = _estado.asStateFlow()

    fun alTocarTecla(digito: Char) {
        val actual = _estado.value
        if (actual.cargando || actual.digitos.length >= IniciarConPin.LARGO_PIN) return
        val nuevos = actual.digitos + digito
        _estado.update { it.copy(digitos = nuevos, error = null) }
        // DT-028: pantalla de bloqueo, entra sola al 4.º dígito, sin botón de confirmar.
        if (nuevos.length == IniciarConPin.LARGO_PIN) enviar(nuevos)
    }

    fun alBorrar() {
        _estado.update { if (it.cargando) it else it.copy(digitos = it.digitos.dropLast(1), error = null) }
    }

    private fun enviar(pin: String) {
        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            when (val r = iniciarConPin(localCodigo, pin)) {
                is Resultado.Ok -> {
                    _estado.update { PinState() }
                    alIniciar(r.valor)
                }
                is Resultado.Fallo -> _estado.update { PinState(error = r.error) }
            }
        }
    }
}
