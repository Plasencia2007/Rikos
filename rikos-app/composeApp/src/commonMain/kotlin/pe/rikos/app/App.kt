package pe.rikos.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import pe.rikos.app.core.designsystem.theme.RikosTheme
import pe.rikos.app.feature.auth.domain.Sesion
import pe.rikos.app.feature.auth.presentation.PinScreen
import pe.rikos.app.feature.auth.presentation.PinViewModel
import pe.rikos.app.feature.mesas.presentation.MesasScreen
import pe.rikos.app.feature.mesas.presentation.MesasViewModel

/** Punto de entrada compartido. Navegación mínima por estado: PIN → Mesas. */
@Composable
fun App(container: AppContainer = remember { AppContainer() }) {
    RikosTheme {
        var sesion by remember { mutableStateOf<Sesion?>(null) }

        if (sesion == null) {
            val vm: PinViewModel = viewModel(factory = viewModelFactory {
                initializer {
                    PinViewModel(container.localCodigo, container.iniciarConPin) { sesion = it }
                }
            })
            PinScreen(vm)
        } else {
            val vm: MesasViewModel = viewModel(factory = viewModelFactory {
                initializer { MesasViewModel(container.obtenerMesas) }
            })
            MesasScreen(vm, alElegirMesa = { /* TODO: abrir mesa / pedido (M04, M07) */ })
        }
    }
}
