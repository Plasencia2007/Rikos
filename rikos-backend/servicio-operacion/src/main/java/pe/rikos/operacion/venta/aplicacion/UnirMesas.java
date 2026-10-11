package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

/**
 * US-029: une una mesa LIBRE (la secundaria) a una principal para atender a un grupo grande; el pedido se opera en
 * conjunto, por cualquiera de las dos. La principal puede tener o no pedido. Una secundaria no puede ser principal
 * de otras, ni una principal estar unida a otra. Separarlas las deja independientes (SepararMesa).
 */
@Service
public class UnirMesas {

    private final MesaRepositorio mesas;
    private final PedidoRepositorio pedidos;
    private final RegistradorAuditoria auditoria;

    public UnirMesas(MesaRepositorio mesas, PedidoRepositorio pedidos, RegistradorAuditoria auditoria) {
        this.mesas = mesas;
        this.pedidos = pedidos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Mesa, ErrorMesa> ejecutar(String secundariaId, String principalId, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        Mesa secundaria = secundariaId == null ? null : mesas.findByIdAndLocalId(secundariaId, localId).orElse(null);
        Mesa principal = principalId == null ? null : mesas.findByIdAndLocalId(principalId, localId).orElse(null);
        if (secundaria == null || principal == null) {
            return Resultado.fracaso(ErrorMesa.NO_ENCONTRADA);
        }
        if (!secundaria.isActivo()) {
            return Resultado.fracaso(ErrorMesa.MESA_INACTIVA);
        }
        if (secundaria.getMesaPrincipalId() != null) {
            return Resultado.fracaso(ErrorMesa.YA_UNIDA);
        }
        if (mesas.existsByMesaPrincipalId(secundariaId)) {
            return Resultado.fracaso(ErrorMesa.TIENE_MESAS_UNIDAS);
        }
        var enCurso = pedidos.findByMesaIdAndEstadoIn(secundariaId, AbrirPedido.ESTADOS_EN_CURSO);
        if (enCurso.isPresent() && "abierto".equals(enCurso.get().getEstado())) {
            // Una mesa "libre" con un pedido abierto sin comandar puede ser principal, pero no secundaria.
            return Resultado.fracaso(ErrorMesa.TIENE_PEDIDO_ABIERTO);
        }
        if (secundaria.isBloqueada() || enCurso.isPresent()) {
            return Resultado.fracaso(ErrorMesa.MESA_NO_LIBRE);
        }
        if (secundariaId.equals(principalId) || !principal.isActivo() || principal.isBloqueada()
                || principal.getMesaPrincipalId() != null) {
            return Resultado.fracaso(ErrorMesa.PRINCIPAL_INVALIDA);
        }

        secundaria.unirA(principalId, actorId);
        mesas.save(secundaria);
        auditoria.registrar("transferencia", "mesa", secundariaId, null,
                RegistradorAuditoria.campos("tipo", "union_de_mesas", "nombre", "Mesa " + secundaria.getNumero(),
                        "principal", principal.getNumero()));
        return Resultado.exito(secundaria);
    }
}
