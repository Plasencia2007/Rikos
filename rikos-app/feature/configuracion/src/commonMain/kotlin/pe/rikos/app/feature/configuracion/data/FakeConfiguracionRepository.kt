package pe.rikos.app.feature.configuracion.data

import pe.rikos.app.core.common.Resultado
import pe.rikos.app.feature.configuracion.domain.ConfiguracionRepository
import pe.rikos.app.feature.configuracion.domain.Local

/**
 * Repositorio falso que provee los locales y latencia observados en el diseño (M01).
 */
class FakeConfiguracionRepository : ConfiguracionRepository {
    private var localGuardado: String? = null

    override suspend fun obtenerLocales(servidorUrl: String): Resultado<List<Local>, String> {
        return Resultado.Ok(
            listOf(
                Local(
                    codigo = "CENTRO",
                    nombre = "Riko's Centro",
                    direccion = "Jr. Lima 245",
                    salonesTexto = "2 salones",
                ),
                Local(
                    codigo = "NORTE",
                    nombre = "Riko's Norte",
                    direccion = "Av. Perú 1020",
                    salonesTexto = "1 salón",
                ),
            )
        )
    }

    override suspend fun obtenerLatencia(servidorUrl: String): Resultado<Long, String> {
        return Resultado.Ok(42L)
    }

    override suspend fun guardarLocal(localCodigo: String) {
        localGuardado = localCodigo
    }

    override suspend fun obtenerLocalGuardado(): String? = localGuardado
}
