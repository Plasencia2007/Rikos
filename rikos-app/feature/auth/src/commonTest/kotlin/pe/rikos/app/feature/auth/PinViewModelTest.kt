package pe.rikos.app.feature.auth

import kotlinx.coroutines.test.runTest
import pe.rikos.app.feature.auth.data.FakeAuthRepository
import pe.rikos.app.feature.auth.domain.ErrorAuth
import pe.rikos.app.feature.auth.domain.IniciarConPin
import pe.rikos.app.feature.auth.domain.Mozo
import pe.rikos.app.feature.auth.domain.Sesion
import pe.rikos.app.feature.auth.presentation.PinViewModel
import pe.rikos.app.feature.auth.presentation.PinState
import pe.rikos.app.feature.auth.presentation.registrarFallo
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
    fun intentoFallidoMantieneLaEntradaBloqueadaMientrasMuestraError() {
        val estado = PinState(digitos = "9999", cargando = true)

        val resultado = estado.registrarFallo(ErrorAuth.PinIncorrecto)

        assertTrue(resultado.cargando)
        assertEquals("9999", resultado.digitos)
        assertEquals(ErrorAuth.PinIncorrecto, resultado.error)
    }
}
