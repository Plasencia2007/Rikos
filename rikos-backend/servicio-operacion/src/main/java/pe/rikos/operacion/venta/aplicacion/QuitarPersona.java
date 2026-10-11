package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

/**
 * Quita una persona sin lineas. Un pedido nunca se queda sin personas: la ultima no se quita (se responde
 * PERSONA_CON_LINEAS, "esta persona no se puede quitar", igual que con lineas).
 */
@Service
public class QuitarPersona {

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;

    public QuitarPersona(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas, DetallePedidoRepositorio lineas) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
    }

    @Transactional
    public Resultado<Void, ErrorPedido> ejecutar(String pedidoId, String personaId) {
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
        CuentaPersona persona = personas.findByIdAndPedidoId(personaId, pedidoId).orElse(null);
        if (persona == null) {
            return Resultado.fracaso(ErrorPedido.PERSONA_NO_ENCONTRADA);
        }
        if (lineas.existsByCuentaPersonaId(personaId) || personas.findByPedidoIdOrderByOrden(pedidoId).size() <= 1) {
            return Resultado.fracaso(ErrorPedido.PERSONA_CON_LINEAS);
        }
        personas.delete(persona);
        return Resultado.exito(null);
    }
}
