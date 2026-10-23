package pe.rikos.app.feature.auth

import kotlinx.coroutines.test.runTest
import pe.rikos.app.feature.auth.data.FakeAuthRepository
import pe.rikos.app.feature.auth.domain.IniciarConPin
import pe.rikos.app.feature.auth.domain.Mozo
import pe.rikos.app.feature.auth.domain.Sesion
import pe.rikos.app.feature.auth.presentation.PinViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PinViewModelTest {
    private val repo = FakeAuthRepository()
    private val caso = IniciarConPin(repo)

    @Test
    fun cambioDeMozoLimpiaDigitosYActualizaSeleccion() = runTest {
        var sesionIniciada: Sesion? = null
        val vm = PinViewModel("LOCAL1", caso, "Riko's Centro") { sesionIniciada = it }

        vm.alTocarTecla('1')
        vm.alTocarTecla('2')
        assertEquals("12", vm.estado.value.digitos)

        val ana = Mozo("2", "AM", "Ana")
        vm.alSeleccionarMozo(ana)

        assertEquals("Ana", vm.estado.value.mozoSeleccionado.nombre)
        assertEquals("", vm.estado.value.digitos)
        assertNull(sesionIniciada)
    }

    @Test
    fun pinCorrecto1234IniciaSesion() = runTest {
        var sesionIniciada: Sesion? = null
        val vm = PinViewModel("LOCAL1", caso, "Riko's Centro") { sesionIniciada = it }

        vm.alTocarTecla('1')
        vm.alTocarTecla('2')
        vm.alTocarTecla('3')
        vm.alTocarTecla('4')

        assertNotNull(sesionIniciada)
        assertEquals("01J00000000000000000000001", sesionIniciada?.usuarioId)
    }
}
