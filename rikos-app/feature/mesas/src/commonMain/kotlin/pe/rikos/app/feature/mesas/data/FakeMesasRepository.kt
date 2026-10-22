package pe.rikos.app.feature.mesas.data

import kotlinx.coroutines.delay
import pe.rikos.app.core.common.Dinero
import pe.rikos.app.core.common.Resultado
import pe.rikos.app.feature.mesas.domain.ErrorMesas
import pe.rikos.app.feature.mesas.domain.EstadoMesa
import pe.rikos.app.feature.mesas.domain.Mesa
import pe.rikos.app.feature.mesas.domain.MesasRepository

/** TODO: reemplazar por `MesasApiRepository` (Ktor) contra `GET /api/mesas`. */
class FakeMesasRepository : MesasRepository {
    override suspend fun obtenerMesas(): Resultado<List<Mesa>, ErrorMesas> {
        delay(300)
        val datos = listOf(
            Mesa("01J0000000000000000000M001", "1", EstadoMesa.LIBRE),
            Mesa("01J0000000000000000000M002", "2", EstadoMesa.OCUPADA, 4, Dinero(8650)),
            Mesa("01J0000000000000000000M003", "3", EstadoMesa.LISTO, 2, Dinero(4200)),
            Mesa("01J0000000000000000000M004", "4", EstadoMesa.POR_COBRAR, 3, Dinero(12900)),
            Mesa("01J0000000000000000000M005", "5", EstadoMesa.LIBRE),
            Mesa("01J0000000000000000000M006", "6", EstadoMesa.FUERA_DE_SERVICIO),
        )
        return Resultado.Ok(datos)
    }
}
