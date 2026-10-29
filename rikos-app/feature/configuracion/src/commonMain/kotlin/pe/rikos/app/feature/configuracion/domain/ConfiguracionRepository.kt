package pe.rikos.app.feature.configuracion.domain

import pe.rikos.app.core.common.Resultado

interface ConfiguracionRepository {
    suspend fun obtenerLocales(servidorUrl: String): Resultado<List<Local>, String>
    suspend fun obtenerLatencia(servidorUrl: String): Resultado<Long, String>
    suspend fun guardarLocal(localCodigo: String)
    suspend fun obtenerLocalGuardado(): String?
}
