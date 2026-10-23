package pe.rikos.app.feature.configuracion

import kotlinx.coroutines.test.runTest
import pe.rikos.app.core.common.Resultado
import pe.rikos.app.feature.configuracion.data.FakeConfiguracionRepository
import pe.rikos.app.feature.configuracion.domain.GuardarConfiguracionLocal
import pe.rikos.app.feature.configuracion.presentation.ConfigurarLocalViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConfigurarLocalTest {
    private val repo = FakeConfiguracionRepository()
    private val guardarCaso = GuardarConfiguracionLocal(repo)

    @Test
    fun obtenerLocalesDevuelveLocalesDelLienzo() = runTest {
        val res = repo.obtenerLocales("https://api.rikos.pe")
        assertTrue(res is Resultado.Ok)
        val locales = res.valor
        assertEquals(2, locales.size)
        assertEquals("CENTRO", locales[0].codigo)
        assertEquals("Riko's Centro", locales[0].nombre)
        assertEquals("NORTE", locales[1].codigo)
        assertEquals("Riko's Norte", locales[1].nombre)
    }

    @Test
    fun guardarLocalPersisteSeleccion() = runTest {
        guardarCaso("CENTRO")
        assertEquals("CENTRO", repo.obtenerLocalGuardado())
    }

    @Test
    fun viewModelInicializaConPrimerLocalYPermiteCambiar() = runTest {
        var localConfirmado: String? = null
        val vm = ConfigurarLocalViewModel(
            repo = repo,
            guardarConfiguracion = guardarCaso,
            onConfigurado = { localConfirmado = it },
        )

        // Inicialmente debe estar cargado Riko's Centro
        assertEquals("Riko's Centro", vm.estado.value.localSeleccionado?.nombre)

        // Cambiar selección a Norte
        val norte = vm.estado.value.locales.first { it.codigo == "NORTE" }
        vm.seleccionarLocal(norte)
        assertEquals("NORTE", vm.estado.value.localSeleccionado?.codigo)

        // Confirmar selección
        vm.confirmar()
        assertEquals("NORTE", localConfirmado)
        assertEquals("NORTE", repo.obtenerLocalGuardado())
    }
}
