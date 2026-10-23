package pe.rikos.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import pe.rikos.app.core.designsystem.theme.Rikos
import pe.rikos.app.core.designsystem.theme.RikosTheme
import pe.rikos.app.feature.auth.domain.Sesion
import pe.rikos.app.feature.auth.presentation.PinScreen
import pe.rikos.app.feature.auth.presentation.PinViewModel
import pe.rikos.app.feature.bienvenida.BienvenidaScreen
import pe.rikos.app.feature.configuracion.presentation.ConfigurarLocalScreen
import pe.rikos.app.feature.configuracion.presentation.ConfigurarLocalViewModel
import pe.rikos.app.feature.mesas.presentation.MesasScreen
import pe.rikos.app.feature.mesas.presentation.MesasViewModel

/**
 * Punto de entrada compartido.
 * Flujo: M00 (Bienvenida) → M01 (Configurar local) → M02 (PIN de mozo) → M03 (Mesas).
 */
@Composable
fun App(container: AppContainer = remember { AppContainer() }) {
    RikosTheme {
        var bienvenidaVisible by rememberSaveable { mutableStateOf(true) }
        var localConfigurado by rememberSaveable { mutableStateOf<String?>(null) }
        var sesion by remember { mutableStateOf<Sesion?>(null) }

        val enPantallaPin = !bienvenidaVisible && localConfigurado != null && sesion == null
        val fondoRaiz = if (enPantallaPin) Rikos.colors.navy else Rikos.colors.bg

        Box(Modifier.fillMaxSize().background(fondoRaiz).safeDrawingPadding()) {
            when {
                bienvenidaVisible -> {
                    BienvenidaScreen(alConfigurar = { bienvenidaVisible = false })
                }
                localConfigurado == null -> {
                    val vm: ConfigurarLocalViewModel = viewModel(factory = viewModelFactory {
                        initializer {
                            ConfigurarLocalViewModel(
                                repo = container.configuracionRepo,
                                guardarConfiguracion = container.guardarConfiguracionLocal,
                                onConfigurado = { codigo -> localConfigurado = codigo },
                            )
                        }
                    })
                    ConfigurarLocalScreen(vm)
                }
                sesion == null -> {
                    val codigoActivo = localConfigurado ?: container.localCodigo
                    val nombreLocal = if (codigoActivo == "NORTE") "Riko's Norte" else "Riko's Centro"
                    val vm: PinViewModel = viewModel(factory = viewModelFactory {
                        initializer {
                            PinViewModel(
                                localCodigo = codigoActivo,
                                iniciarConPin = container.iniciarConPin,
                                localNombre = nombreLocal,
                            ) { sesion = it }
                        }
                    })
                    PinScreen(vm)
                }
                else -> {
                    val vm: MesasViewModel = viewModel(factory = viewModelFactory {
                        initializer { MesasViewModel(container.obtenerMesas) }
                    })
                    MesasScreen(vm, alElegirMesa = { /* TODO: abrir mesa / pedido (M04, M07) */ })
                }
            }
        }
    }
}
