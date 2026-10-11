package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

/**
 * Quita una linea PENDIENTE (se agrego por error y nunca salio a cocina): es un borrado real. Una ya comandada
 * no se quita: se anula con permiso y motivo (entrega 3, US-031), porque la cocina ya la vio.
 */
@Service
public class QuitarLinea {

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;

    public QuitarLinea(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas, DetallePedidoRepositorio lineas) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
    }

    @Transactional
    public Resultado<Void, ErrorPedido> ejecutar(String pedidoId, String lineaId, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        pe.rikos.operacion.venta.infraestructura.Pedido pedidoActual = pedidos.findByIdAndLocalId(pedidoId, localId).orElse(null);
        if (pedidoActual == null) {
            return Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
        }
        if (AbrirPedido.estaCerrado(pedidoActual)) {
            return Resultado.fracaso(ErrorPedido.PEDIDO_CERRADO);
        }
        DetallePedido linea = lineas.findByIdAndCuentaPersonaIdIn(lineaId,
                personas.findByPedidoIdOrderByOrden(pedidoId).stream().map(CuentaPersona::getId).toList()).orElse(null);
        if (linea == null) {
            return Resultado.fracaso(ErrorPedido.LINEA_NO_ENCONTRADA);
        }
        if ("cobrado".equals(linea.getEstado())) {
            return Resultado.fracaso(ErrorPedido.LINEA_COBRADA);
        }
        if (!"pendiente".equals(linea.getEstado())) {
            return Resultado.fracaso(ErrorPedido.LINEA_YA_COMANDADA);
        }
        lineas.delete(linea);
        return Resultado.exito(null);
    }
}
