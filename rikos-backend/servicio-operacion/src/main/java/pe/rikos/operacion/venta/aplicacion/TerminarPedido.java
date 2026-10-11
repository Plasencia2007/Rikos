package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.List;

/**
 * "Terminar pedido": un cierre SIN COBRO, provisional hasta el sprint 7. Deja el pedido en estado "cerrado" (con quien y
 * cuando), la mesa queda libre (su estado se deriva de los pedidos) y las mesas unidas a ella se separan solas. No toca
 * la cuenta de ningun pensionista ni genera comprobante: el sprint 7 completa este mismo cierre con el cobro. Un pedido
 * cerrado ya no se modifica, pero su comanda se sigue consultando (el Historial la reimprime).
 */
@Service
public class TerminarPedido {

    private final PedidoRepositorio pedidos;
    private final MesaRepositorio mesas;
    private final RegistradorAuditoria auditoria;

    public TerminarPedido(PedidoRepositorio pedidos, MesaRepositorio mesas, RegistradorAuditoria auditoria) {
        this.pedidos = pedidos;
        this.mesas = mesas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Pedido, ErrorPedido> ejecutar(String pedidoId, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        Pedido pedido = pedidoId == null ? null : pedidos.findByIdAndLocalId(pedidoId, localId).orElse(null);
        if (pedido == null) {
            return Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
        }
        if (!AbrirPedido.ESTADOS_EN_CURSO.contains(pedido.getEstado())) {
            return Resultado.fracaso(ErrorPedido.PEDIDO_NO_TERMINABLE);
        }

        String numeroDeMesa = "";
        int separadas = 0;
        if (pedido.getMesaId() != null) {
            Mesa mesa = mesas.findById(pedido.getMesaId()).orElse(null);
            numeroDeMesa = mesa == null ? "" : mesa.getNumero();
            // Una mesa unida a la de este pedido queda libre e independiente: si no, se quedaria "unida" a una mesa libre.
            List<Mesa> unidas = mesas.findByMesaPrincipalId(pedido.getMesaId());
            for (Mesa unida : unidas) {
                unida.separar(actorId);
                mesas.save(unida);
            }
            separadas = unidas.size();
        }
        pedido.terminar(actorId);
        pedidos.save(pedido);
        auditoria.registrar("pedido_terminado", "pedido", pedido.getId(), null,
                RegistradorAuditoria.campos("nombre", "Pedido " + pedido.getNumero(), "mesa", numeroDeMesa,
                        "mesasSeparadas", separadas));
        return Resultado.exito(pedido);
    }
}
