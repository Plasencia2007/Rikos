package pe.rikos.app.feature.auth

import kotlinx.coroutines.test.runTest
import pe.rikos.app.core.common.Resultado
import pe.rikos.app.feature.auth.data.FakeAuthRepository
import pe.rikos.app.feature.auth.domain.ErrorAuth
import pe.rikos.app.feature.auth.domain.IniciarConPin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IniciarConPinTest {
    private val caso = IniciarConPin(FakeAuthRepository())

    @Test fun pinCorrectoEntra() = runTest {
        assertTrue(caso("LOCAL1", "1234") is Resultado.Ok)
    }

    @Test fun pinConFormatoInvalidoNoLlegaAlRepositorio() = runTest {
        assertEquals(Resultado.Fallo(ErrorAuth.PinIncorrecto), caso("LOCAL1", "12a4"))
        assertEquals(Resultado.Fallo(ErrorAuth.PinIncorrecto), caso("LOCAL1", "123"))
    }

    @Test fun bloqueoSeReportaComoBloqueado() = runTest {
        assertEquals(Resultado.Fallo(ErrorAuth.Bloqueado), caso("LOCAL1", "0000"))
    }
}
