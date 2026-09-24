package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;

@Service
public class ActualizarLocal {

    private final LocalRepositorio locales;

    public ActualizarLocal(LocalRepositorio locales) {
        this.locales = locales;
    }

    @Transactional
    public Resultado<Local, ErrorLocal> ejecutar(String id, Local.DatosActualizacion datos, String actorId) {
        return locales.findById(id)
                .map(local -> {
                    local.actualizar(datos, actorId);
                    return Resultado.<Local, ErrorLocal>exito(local);
                })
                .orElseGet(() -> Resultado.fracaso(ErrorLocal.NO_ENCONTRADO));
    }
}
