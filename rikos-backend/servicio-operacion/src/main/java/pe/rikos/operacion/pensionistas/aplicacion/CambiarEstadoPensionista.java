package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;

/**
 * RN-034: desactivar no genera tickets ni consumos nuevos, pero conserva la
 * cuenta, el historial y los abonos posteriores; reactivar reutiliza LA MISMA
 * cuenta (por eso la cuenta no se toca aqui). Queda en la bitacora.
 * Nunca se borra un pensionista: tiene una cuenta con historial.
 */
@Service
public class CambiarEstadoPensionista {

    private final PensionistaRepositorio pensionistas;
    private final RegistradorAuditoria auditoria;

    public CambiarEstadoPensionista(PensionistaRepositorio pensionistas, RegistradorAuditoria auditoria) {
        this.pensionistas = pensionistas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Pensionista, ErrorPensionista> ejecutar(String id, boolean activo, String actorId) {
        Pensionista pensionista = pensionistas.findById(id).orElse(null);
        if (pensionista == null) {
            return Resultado.fracaso(ErrorPensionista.NO_ENCONTRADO);
        }
        boolean estabaActivo = pensionista.isActivo();
        if (estabaActivo == activo) {
            return Resultado.exito(pensionista);
        }
        pensionista.cambiarEstado(activo, actorId);
        pensionistas.save(pensionista);
        String nombre = pensionista.nombreCompleto();
        auditoria.registrar("cambio_estado", "pensionista", id,
                RegistradorAuditoria.campos("activo", estabaActivo, "nombre", nombre),
                RegistradorAuditoria.campos("activo", activo, "nombre", nombre));
        return Resultado.exito(pensionista);
    }
}
