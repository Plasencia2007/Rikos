package pe.rikos.app.feature.mesas.presentation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pe.rikos.app.feature.mesas.data.FakeMesasRepository
import pe.rikos.app.feature.mesas.domain.ObtenerMesas
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MesasViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun cargaSalonesAvisosYMesasCorrectamente() = runTest {
        val repo = FakeMesasRepository()
        val vm = MesasViewModel(ObtenerMesas(repo), mozoNombre = "Luis")

        assertTrue(vm.estado.value.cargando)
        advanceUntilIdle()

        val estado = vm.estado.value
        assertFalse(estado.cargando)
        assertEquals("Luis", estado.mozoNombre)
        assertEquals(4, estado.salones.size)
        assertEquals(3, estado.avisos.size)
        assertEquals(11, estado.mesas.size)
        assertEquals(3, estado.libresCount)
        assertEquals(7, estado.ocupadasCount) // ocupadas + listo
        assertEquals(1, estado.porCobrarCount)
    }

    @Test
    fun cambioDeSalonFiltraMesasAdecuadamente() = runTest {
        val repo = FakeMesasRepository()
        val vm = MesasViewModel(ObtenerMesas(repo))
        advanceUntilIdle()

        assertEquals("salon_principal", vm.estado.value.salonActivoId)
        assertEquals(11, vm.estado.value.mesasFiltradas.size)

        vm.alSeleccionarSalon("terraza")
        assertEquals("terraza", vm.estado.value.salonActivoId)
        assertEquals(0, vm.estado.value.mesasFiltradas.size)
    }
}
