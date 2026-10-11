package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.List;

/**
 * Cambia la cantidad y la observacion de una linea PENDIENTE (antes de comandar). Una linea que ya salio a
 * cocina no se toca aqui: se anula con permiso y motivo (entrega 3, US-031). Un pensionista pide una sola comida
 * del plan por linea, asi que su menu o comida del plan sigue en cantidad 1.
 */
@Service
public class ModificarLinea {

    private static final int LARGO_MAXIMO_OBSERVACION = 250;

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;

    public ModificarLinea(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas, DetallePedidoRepositorio lineas) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
    }

    @Transactional
    public Resultado<DetallePedido, ErrorPedido> ejecutar(String pedidoId, String lineaId, int cantidad,
                                                          String observacion, String actorId) {
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
        List<CuentaPersona> delPedido = personas.findByPedidoIdOrderByOrden(pedidoId);
        DetallePedido linea = lineas.findByIdAndCuentaPersonaIdIn(lineaId,
                delPedido.stream().map(CuentaPersona::getId).toList()).orElse(null);
        if (linea == null) {
            return Resultado.fracaso(ErrorPedido.LINEA_NO_ENCONTRADA);
        }
        if ("cobrado".equals(linea.getEstado())) {
            return Resultado.fracaso(ErrorPedido.LINEA_COBRADA);
        }
        if (!"pendiente".equals(linea.getEstado())) {
            return Resultado.fracaso(ErrorPedido.LINEA_YA_COMANDADA);
        }
        if (cantidad < 1) {
            return Resultado.fracaso(ErrorPedido.CANTIDAD_INVALIDA);
        }
        CuentaPersona persona = delPedido.stream().filter(p -> p.getId().equals(linea.getCuentaPersonaId()))
                .findFirst().orElseThrow();
        boolean comidaDelPlan = List.of("menu_normal", "plan_desayuno", "plan_cena").contains(linea.getTipoLinea());
        if (persona.getPensionistaId() != null && comidaDelPlan && cantidad != 1) {
            return Resultado.fracaso(ErrorPedido.CANTIDAD_INVALIDA);
        }
        String texto = observacion == null || observacion.isBlank() ? null : observacion.trim();
        if (texto != null && texto.length() > LARGO_MAXIMO_OBSERVACION) {
            return Resultado.fracaso(ErrorPedido.OBSERVACION_LARGA);
        }
        linea.cambiar(cantidad, texto);
        return Resultado.exito(lineas.save(linea));
    }
}
