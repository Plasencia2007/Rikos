package pe.rikos.app.feature.mesas

import kotlinx.coroutines.test.runTest
import pe.rikos.app.core.common.Resultado
import pe.rikos.app.core.common.Ulid
import pe.rikos.app.feature.mesas.data.FakeMesasRepository
import pe.rikos.app.feature.mesas.domain.ObtenerMesas
import kotlin.test.Test
import kotlin.test.assertTrue

class ObtenerMesasTest {
    @Test fun devuelveMesasConIdsUlidValidos() = runTest {
        val r = ObtenerMesas(FakeMesasRepository())()
        assertTrue(r is Resultado.Ok)
        assertTrue(r.valor.isNotEmpty())
        assertTrue(r.valor.all { Ulid.esValido(it.id) })
    }
}
