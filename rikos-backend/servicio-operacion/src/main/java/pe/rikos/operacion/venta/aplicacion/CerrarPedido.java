package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.List;

/**
 * El cierre de un pedido, compartido por "Terminar pedido" (sin cobro) y por el cobro de caja (sprint 7): el pedido pasa a
 * "cerrado" con quien y cuando, y las mesas unidas a la suya se separan solas (la mesa queda libre porque su estado se
 * deriva de los pedidos). Lo llama el modulo caja, por eso es publico.
 */
@Service
public class CerrarPedido {

    private final PedidoRepositorio pedidos;
    private final MesaRepositorio mesas;

    public CerrarPedido(PedidoRepositorio pedidos, MesaRepositorio mesas) {
        this.pedidos = pedidos;
        this.mesas = mesas;
    }

    /** True si a este pedido no le queda nada por cobrar: ninguna linea pendiente ni comandada (todas cobradas o anuladas). */
    public static boolean sinNadaPorCobrar(List<DetallePedido> lineas) {
        return lineas.stream().noneMatch(l -> "pendiente".equals(l.getEstado()) || "comandado".equals(l.getEstado()));
    }

    /** Cierra el pedido y separa las mesas unidas; devuelve cuantas separo. */
    public int cerrar(Pedido pedido, String actorId) {
        int separadas = 0;
        if (pedido.getMesaId() != null) {
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
        return separadas;
    }
}
