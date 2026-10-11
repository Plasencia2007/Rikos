package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;

/** US-029: separa una mesa unida: queda libre e independiente. El pedido de la principal no se toca. */
@Service
public class SepararMesa {

    private final MesaRepositorio mesas;
    private final RegistradorAuditoria auditoria;

    public SepararMesa(MesaRepositorio mesas, RegistradorAuditoria auditoria) {
        this.mesas = mesas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Mesa, ErrorMesa> ejecutar(String secundariaId, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        Mesa secundaria = secundariaId == null ? null : mesas.findByIdAndLocalId(secundariaId, localId).orElse(null);
        if (secundaria == null) {
            return Resultado.fracaso(ErrorMesa.NO_ENCONTRADA);
        }
        if (secundaria.getMesaPrincipalId() == null) {
            return Resultado.fracaso(ErrorMesa.NO_ESTA_UNIDA);
        }
        String principal = mesas.findById(secundaria.getMesaPrincipalId()).map(Mesa::getNumero).orElse("");
        secundaria.separar(actorId);
        mesas.save(secundaria);
        auditoria.registrar("transferencia", "mesa", secundariaId, null,
                RegistradorAuditoria.campos("tipo", "separacion_de_mesas", "nombre", "Mesa " + secundaria.getNumero(),
                        "principal", principal));
        return Resultado.exito(secundaria);
    }
}
