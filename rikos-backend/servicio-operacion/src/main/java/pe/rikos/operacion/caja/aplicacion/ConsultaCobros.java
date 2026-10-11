package pe.rikos.operacion.caja.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Cobro;
import pe.rikos.operacion.caja.infraestructura.CobroLinea;
import pe.rikos.operacion.caja.infraestructura.CobroLineaRepositorio;
import pe.rikos.operacion.caja.infraestructura.CobroRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.caja.infraestructura.TurnoCajaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
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

    /** Un cobro de un turno con el numero del pedido que cobro (para la lista de la caja). */
    public record CobroDelTurno(Cobro cobro, String pedidoNumero) {
    }

    private final PedidoRepositorio pedidos;
    private final CobroRepositorio cobros;
    private final CobroLineaRepositorio cobroLineas;
    private final TurnoCajaRepositorio turnos;

    public ConsultaCobros(PedidoRepositorio pedidos, CobroRepositorio cobros, CobroLineaRepositorio cobroLineas,
                          TurnoCajaRepositorio turnos) {
        this.turnos = turnos;
        this.pedidos = pedidos;
        this.cobros = cobros;
        this.cobroLineas = cobroLineas;
    }

    /** Los cobros del turno abierto de este cajero (para ver lo cobrado y reabrir uno), el mas reciente primero. */
    @Transactional(readOnly = true)
    public Resultado<List<CobroDelTurno>, ErrorCobro> delTurnoActual(String cajeroId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorCobro.SIN_LOCAL_ACTIVO);
        }
        TurnoCaja turno = turnos.findByCajeroIdAndEstado(cajeroId, TurnoCaja.ABIERTO)
                .filter(t -> t.getLocalId().equals(localId)).orElse(null);
        if (turno == null) {
            return Resultado.fracaso(ErrorCobro.SIN_TURNO_ABIERTO);
        }
        List<Cobro> lista = cobros.findByTurnoCajaIdOrderByCobradoEnDesc(turno.getId());
        Map<String, String> numeros = pedidos.findAllById(lista.stream().map(Cobro::getPedidoId).distinct().toList()).stream()
                .collect(Collectors.toMap(Pedido::getId, Pedido::getNumero));
        return Resultado.exito(lista.stream().map(c -> new CobroDelTurno(c, numeros.getOrDefault(c.getPedidoId(), ""))).toList());
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
