package pe.rikos.app

import pe.rikos.app.feature.auth.data.FakeAuthRepository
import pe.rikos.app.feature.auth.domain.AuthRepository
import pe.rikos.app.feature.auth.domain.IniciarConPin
import pe.rikos.app.feature.mesas.data.FakeMesasRepository
import pe.rikos.app.feature.mesas.domain.MesasRepository
import pe.rikos.app.feature.mesas.domain.ObtenerMesas

/**
 * Raíz de composición a mano (sin framework de inyección por ahora).
 * Cuando exista el backend conectado, aquí se cambian los Fake por los repositorios de Ktor.
 */
class AppContainer {
    /** Código del local. Vendrá de la pantalla M01 «Configurar local» (DT-040). */
    val localCodigo: String = "LOCAL1"

    private val authRepo: AuthRepository = FakeAuthRepository()
    private val mesasRepo: MesasRepository = FakeMesasRepository()

    val iniciarConPin = IniciarConPin(authRepo)
    val obtenerMesas = ObtenerMesas(mesasRepo)
}
