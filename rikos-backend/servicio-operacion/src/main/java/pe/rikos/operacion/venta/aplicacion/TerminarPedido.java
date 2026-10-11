package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;


/**
 * "Terminar pedido": un cierre SIN cobro, solo para un pedido sin nada que cobrar (sprint 7, decision del dueño): vacio,
 * o con todas sus lineas anuladas o aun sin comandar (no hay nada que cobrar de ellas). Con productos comandados hay que
 * cobrarlos o anularlos con motivo y permiso. Deja el pedido "cerrado" con quien y cuando; la mesa queda libre y las mesas
 * unidas a ella se separan solas. No toca la cuenta de ningun pensionista ni genera comprobante. Un pedido cerrado ya no se
 * modifica, pero su comanda se sigue consultando (el Historial la reimprime).
 */
@Service
public class TerminarPedido {

    private final PedidoRepositorio pedidos;
    private final MesaRepositorio mesas;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;
    private final CerrarPedido cerrarPedido;
    private final RegistradorAuditoria auditoria;

    public TerminarPedido(PedidoRepositorio pedidos, MesaRepositorio mesas, CuentaPersonaRepositorio personas,
                          DetallePedidoRepositorio lineas, CerrarPedido cerrarPedido, RegistradorAuditoria auditoria) {
        this.pedidos = pedidos;
        this.mesas = mesas;
        this.personas = personas;
        this.lineas = lineas;
        this.cerrarPedido = cerrarPedido;
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
        var delPedido = personas.findByPedidoIdOrderByOrden(pedidoId).stream().map(CuentaPersona::getId).toList();
        if (lineas.findByCuentaPersonaIdInOrderByCreadoEn(delPedido).stream().anyMatch(l -> "comandado".equals(l.getEstado()))) {
            return Resultado.fracaso(ErrorPedido.PEDIDO_CON_LINEAS_POR_COBRAR);
        }

        String numeroDeMesa = "";
        if (pedido.getMesaId() != null) {
            Mesa mesa = mesas.findById(pedido.getMesaId()).orElse(null);
            numeroDeMesa = mesa == null ? "" : mesa.getNumero();
        }
        int separadas = cerrarPedido.cerrar(pedido, actorId);
        auditoria.registrar("pedido_terminado", "pedido", pedido.getId(), null,
                RegistradorAuditoria.campos("nombre", "Pedido " + pedido.getNumero(), "mesa", numeroDeMesa,
                        "mesasSeparadas", separadas));
        return Resultado.exito(pedido);
    }
}
