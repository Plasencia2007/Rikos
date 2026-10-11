package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Cobro;
import pe.rikos.operacion.caja.infraestructura.CobroLinea;
import pe.rikos.operacion.caja.infraestructura.CobroLineaRepositorio;
import pe.rikos.operacion.caja.infraestructura.CobroRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Los cobros de un pedido, con las lineas de cada uno (para reabrirlos o ver que ya se pago). */
@Service
public class ConsultaCobros {

    /** Un cobro con las lineas que cobro. */
    public record CobroConLineas(Cobro cobro, List<CobroLinea> lineas) {
    }

    private final PedidoRepositorio pedidos;
    private final CobroRepositorio cobros;
    private final CobroLineaRepositorio cobroLineas;

    public ConsultaCobros(PedidoRepositorio pedidos, CobroRepositorio cobros, CobroLineaRepositorio cobroLineas) {
        this.pedidos = pedidos;
        this.cobros = cobros;
        this.cobroLineas = cobroLineas;
    }

    @Transactional(readOnly = true)
    public Resultado<List<CobroConLineas>, ErrorCobro> deLaPedido(String pedidoId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCobro.SIN_LOCAL_ACTIVO);
        }
        if (pedidoId == null || pedidos.findByIdAndLocalId(pedidoId, localId).isEmpty()) {
            return Resultado.fracaso(ErrorCobro.PEDIDO_NO_ENCONTRADO);
        }
        List<Cobro> lista = cobros.findByPedidoIdOrderByCobradoEn(pedidoId);
        Map<String, List<CobroLinea>> lineas = cobroLineas.findByCobroIdIn(lista.stream().map(Cobro::getId).toList()).stream()
                .collect(Collectors.groupingBy(CobroLinea::getCobroId));
        return Resultado.exito(lista.stream().map(c -> new CobroConLineas(c, lineas.getOrDefault(c.getId(), List.of()))).toList());
    }
}
