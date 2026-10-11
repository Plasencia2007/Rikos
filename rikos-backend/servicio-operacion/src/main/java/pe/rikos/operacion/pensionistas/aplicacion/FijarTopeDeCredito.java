package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PensionistaRepositorio;

import java.util.Objects;

/**
 * Fija el tope de credito a mano, o (limite nulo) lo devuelve al calculo
 * automatico. Es un recordatorio de cobro, no un bloqueo (RN-017).
 */
@Service
public class FijarTopeDeCredito {

    private final PensionistaRepositorio pensionistas;
    private final RegistradorAuditoria auditoria;

    public FijarTopeDeCredito(PensionistaRepositorio pensionistas, RegistradorAuditoria auditoria) {
        this.pensionistas = pensionistas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Pensionista, ErrorCuenta> ejecutar(String pensionistaId, Long limite, String actorId) {
        Pensionista pensionista = pensionistas.findById(pensionistaId).orElse(null);
        if (pensionista == null) {
            return Resultado.fracaso(ErrorCuenta.NO_ENCONTRADO);
        }
        if (limite != null && limite <= 0) {
            return Resultado.fracaso(ErrorCuenta.LIMITE_INVALIDO);
        }
        Long anterior = pensionista.getLimiteCredito();
        if (Objects.equals(anterior, limite)) {
            return Resultado.exito(pensionista);
        }
        pensionista.fijarLimite(limite, actorId);
        pensionistas.save(pensionista);
        String nombre = pensionista.nombreCompleto();
        auditoria.registrar("cambio_limite", "pensionista", pensionistaId,
                RegistradorAuditoria.campos("limite", anterior, "nombre", nombre),
                RegistradorAuditoria.campos("limite", limite, "nombre", nombre));
        return Resultado.exito(pensionista);
    }
}
