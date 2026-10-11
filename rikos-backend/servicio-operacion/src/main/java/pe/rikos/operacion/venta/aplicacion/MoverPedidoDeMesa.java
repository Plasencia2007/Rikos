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
 * US-029: mueve un pedido de mesa a otra mesa LIBRE. El estado de las mesas se deriva de los pedidos, asi que la
 * vieja vuelve a estar libre y la nueva ocupada sin tocar nada mas. Solo antes del cierre. Una mesa con mesas unidas
 * no se deja atras: se separan primero.
 */
@Service
public class MoverPedidoDeMesa {

    private final PedidoRepositorio pedidos;
    private final MesaRepositorio mesas;
    private final RegistradorAuditoria auditoria;

    public MoverPedidoDeMesa(PedidoRepositorio pedidos, MesaRepositorio mesas, RegistradorAuditoria auditoria) {
        this.pedidos = pedidos;
        this.mesas = mesas;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Pedido, ErrorPedido> ejecutar(String pedidoId, String mesaDestinoId, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        Pedido pedido = pedidos.findByIdAndLocalId(pedidoId, localId).orElse(null);
        if (pedido == null) {
            return Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
        }
        if (pedido.getMesaId() == null) {
            return Resultado.fracaso(ErrorPedido.CANAL_INVALIDO);
        }
        if (!List.of("abierto", "comandado").contains(pedido.getEstado())) {
            return Resultado.fracaso(ErrorPedido.PEDIDO_NO_REORGANIZABLE);
        }
        if (pedido.getMesaId().equals(mesaDestinoId)) {
            return Resultado.fracaso(ErrorPedido.MISMO_DESTINO);
        }
        Mesa destino = mesaDestinoId == null ? null : mesas.findByIdAndLocalId(mesaDestinoId, localId).orElse(null);
        if (destino == null) {
            return Resultado.fracaso(ErrorPedido.MESA_NO_ENCONTRADA);
        }
        if (!destino.isActivo() || destino.isBloqueada() || destino.getMesaPrincipalId() != null) {
            return Resultado.fracaso(ErrorPedido.MESA_NO_DISPONIBLE);
        }
        if (pedidos.findByMesaIdAndEstadoIn(mesaDestinoId, AbrirPedido.ESTADOS_EN_CURSO).isPresent()) {
            return Resultado.fracaso(ErrorPedido.MESA_OCUPADA);
        }
        Mesa origen = mesas.findById(pedido.getMesaId()).orElseThrow();
        if (mesas.existsByMesaPrincipalId(origen.getId())) {
            return Resultado.fracaso(ErrorPedido.TIENE_MESAS_UNIDAS);
        }

        pedido.moverAMesa(destino.getId());
        pedidos.save(pedido);
        auditoria.registrar("transferencia", "pedido", pedido.getId(), null,
                RegistradorAuditoria.campos("tipo", "cambio_de_mesa", "nombre", "Pedido " + pedido.getNumero(),
                        "desde", origen.getNumero(), "hacia", destino.getNumero()));
        return Resultado.exito(pedido);
    }
}
