package pe.rikos.app.feature.configuracion.domain

class GuardarConfiguracionLocal(private val repo: ConfiguracionRepository) {
    suspend operator fun invoke(localCodigo: String) {
        repo.guardarLocal(localCodigo)
    }
}
