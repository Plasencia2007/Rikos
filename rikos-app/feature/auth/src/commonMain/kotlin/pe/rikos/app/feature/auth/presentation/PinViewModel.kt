package pe.rikos.app.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.rikos.app.core.common.Resultado
import pe.rikos.app.feature.auth.domain.ErrorAuth
import pe.rikos.app.feature.auth.domain.IniciarConPin
import pe.rikos.app.feature.auth.domain.Mozo
import pe.rikos.app.feature.auth.domain.Sesion

data class PinState(
    val digitos: String = "",
    val cargando: Boolean = false,
    val error: ErrorAuth? = null,
    val intentosRestantes: Int = 3,
    val shakeCount: Int = 0,
    val mozos: List<Mozo> = MOZOS_PREDETERMINADOS,
    val mozoSeleccionado: Mozo = MOZOS_PREDETERMINADOS[0],
    val localNombre: String = "Riko's Centro",
) {
    companion object {
        val MOZOS_PREDETERMINADOS = listOf(
            Mozo("1", "LQ", "Luis"),
            Mozo("2", "AM", "Ana"),
            Mozo("3", "PR", "Pedro"),
        )
    }
}

internal fun PinState.registrarFallo(error: ErrorAuth): PinState {
    val nuevosIntentos = (intentosRestantes - 1).coerceAtLeast(0)
    val errorFinal = if (nuevosIntentos == 0) ErrorAuth.Bloqueado else error
    return copy(
        cargando = errorFinal !is ErrorAuth.Bloqueado,
        error = errorFinal,
        intentosRestantes = nuevosIntentos,
        shakeCount = shakeCount + 1,
    )
}

class PinViewModel(
    private val localCodigo: String,
    private val iniciarConPin: IniciarConPin,
    localNombre: String = if (localCodigo == "NORTE") "Riko's Norte" else "Riko's Centro",
    private val alIniciar: (Sesion) -> Unit,
) : ViewModel() {

    private val _estado = MutableStateFlow(
        PinState(
            localNombre = localNombre,
        ),
    )
    val estado: StateFlow<PinState> = _estado.asStateFlow()

    fun alTocarTecla(digito: Char) {
        val actual = _estado.value
        if (actual.cargando || actual.digitos.length >= IniciarConPin.LARGO_PIN) return
        val nuevos = actual.digitos + digito
        _estado.update { it.copy(digitos = nuevos, error = null) }

        // DT-028: pantalla de bloqueo, entra sola al 4.º dígito, sin botón de confirmar.
        if (nuevos.length == IniciarConPin.LARGO_PIN) {
            enviar(nuevos)
        }
    }

    fun alBorrar() {
        _estado.update {
            if (it.cargando) it else it.copy(digitos = it.digitos.dropLast(1), error = null)
        }
    }

    fun alSeleccionarMozo(mozo: Mozo) {
        _estado.update {
            if (it.cargando) it else it.copy(mozoSeleccionado = mozo, digitos = "", error = null)
        }
    }

    private fun enviar(pin: String) {
        _estado.update { it.copy(cargando = true) }
        viewModelScope.launch {
            when (val r = iniciarConPin(localCodigo, pin)) {
                is Resultado.Ok -> {
                    _estado.update { PinState(localNombre = it.localNombre) }
                    alIniciar(r.valor)
                }
                is Resultado.Fallo -> {
                    _estado.update { it.registrarFallo(r.error) }
                    // Espera 700 ms (igual que en el diseño) y limpia los dígitos para reintentar
                    if (_estado.value.error !is ErrorAuth.Bloqueado) {
                        delay(700)
                        _estado.update { it.copy(digitos = "", cargando = false, error = null) }
                    }
                }
            }
        }
    }
}
