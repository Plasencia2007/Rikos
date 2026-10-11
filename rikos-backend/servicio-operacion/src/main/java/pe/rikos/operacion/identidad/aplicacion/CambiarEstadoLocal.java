package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;

/**
 * US-001 CA3: activo = falso impide nuevas operaciones y conserva integro
 * el historial. Nunca se borra un local (2.5).
 */
@Service
public class CambiarEstadoLocal {

    private final LocalRepositorio locales;
    private final RegistradorAuditoria auditoria;

    public CambiarEstadoLocal(LocalRepositorio locales, RegistradorAuditoria auditoria) {
        this.locales = locales;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Local, ErrorLocal> ejecutar(String id, boolean activo, String actorId) {
        return locales.findById(id)
                .map(local -> {
                    if (activo) {
                        local.activar(actorId);
                    } else {
                        local.desactivar(actorId);
                    }
                    auditoria.registrar("cambio_estado", "local", id,
                            RegistradorAuditoria.campos("activo", !activo, "nombre", local.getNombre()),
                            RegistradorAuditoria.campos("activo", activo, "nombre", local.getNombre()));
                    return Resultado.<Local, ErrorLocal>exito(local);
                })
                .orElseGet(() -> Resultado.fracaso(ErrorLocal.NO_ENCONTRADO));
    }
}
