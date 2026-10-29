package pe.rikos.app.feature.configuracion.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.rikos.app.core.common.Resultado
import pe.rikos.app.feature.configuracion.domain.ConfiguracionRepository
import pe.rikos.app.feature.configuracion.domain.GuardarConfiguracionLocal
import pe.rikos.app.feature.configuracion.domain.Local

data class ConfigurarLocalState(
    val servidorUrl: String = "https://api.rikos.pe",
    val locales: List<Local> = emptyList(),
    val localSeleccionado: Local? = null,
    val latenciaMs: Long? = 42L,
    val cargando: Boolean = false,
    val error: String? = null,
)

class ConfigurarLocalViewModel(
    private val repo: ConfiguracionRepository,
    private val guardarConfiguracion: GuardarConfiguracionLocal,
    private val onConfigurado: (localCodigo: String) -> Unit,
) : ViewModel() {

    private val _estado = MutableStateFlow(ConfigurarLocalState())
    val estado: StateFlow<ConfigurarLocalState> = _estado.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, error = null) }
            val resLocales = repo.obtenerLocales(_estado.value.servidorUrl)
            val resLatencia = repo.obtenerLatencia(_estado.value.servidorUrl)

            val locales = when (resLocales) {
                is Resultado.Ok -> resLocales.valor
                is Resultado.Fallo -> emptyList()
            }
            val latencia = when (resLatencia) {
                is Resultado.Ok -> resLatencia.valor
                is Resultado.Fallo -> null
            }
            // Selecciona el primer local ("Riko's Centro") por defecto, como en el lienzo.
            _estado.update {
                it.copy(
                    cargando = false,
                    locales = locales,
                    localSeleccionado = locales.firstOrNull(),
                    latenciaMs = latencia,
                )
            }
        }
    }

    fun cambiarServidor(nuevaUrl: String) {
        _estado.update { it.copy(servidorUrl = nuevaUrl) }
    }

    fun seleccionarLocal(local: Local) {
        _estado.update { it.copy(localSeleccionado = local) }
    }

    fun confirmar() {
        val seleccionado = _estado.value.localSeleccionado ?: return
        viewModelScope.launch {
            guardarConfiguracion(seleccionado.codigo)
            onConfigurado(seleccionado.codigo)
        }
    }
}
