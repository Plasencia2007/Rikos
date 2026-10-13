package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Comanda;
import pe.rikos.operacion.venta.infraestructura.ComandaRepositorio;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetalleComanda;
import pe.rikos.operacion.venta.infraestructura.DetalleComandaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * US-030: manda a cocina lo pendiente de TODO el pedido (todas las personas) en una comanda numerada, que es una
 * FOTO: guarda que linea y cuanto se mando. Agregar platos despues y comandar otra vez crea otra comanda solo con
 * lo nuevo (RN-062). Idempotente por id: reenviar el mismo devuelve la misma comanda.
 */
@Service
public class ComandarPedido {

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;
    private final ComandaRepositorio comandas;
    private final DetalleComandaRepositorio detalles;
    private final RegistradorAuditoria auditoria;
    private final pe.rikos.operacion.inventario.aplicacion.DescontarInventarioPorComanda descontarInventarioPorComanda;

    public ComandarPedido(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas, DetallePedidoRepositorio lineas,
                          ComandaRepositorio comandas, DetalleComandaRepositorio detalles,
                          RegistradorAuditoria auditoria,
                          pe.rikos.operacion.inventario.aplicacion.DescontarInventarioPorComanda descontarInventarioPorComanda) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
        this.comandas = comandas;
        this.detalles = detalles;
        this.auditoria = auditoria;
        this.descontarInventarioPorComanda = descontarInventarioPorComanda;
    }

    @Transactional
    public Resultado<Comanda, ErrorPedido> ejecutar(String pedidoId, String comandaId, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        Pedido pedido = pedidos.findByIdAndLocalId(pedidoId, localId).orElse(null);
        if (pedido == null) {
            return Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
        }
        if (comandaId != null) {
            // save() con un id asignado hace merge: un id que ya existe en otro pedido (o local) se pisaria.
            Comanda existente = comandas.findById(comandaId).orElse(null);
            if (existente != null) {
                return existente.getPedidoId().equals(pedidoId) ? Resultado.exito(existente)
                        : Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
            }
        }
        if (!List.of("abierto", "comandado").contains(pedido.getEstado())) {
            return Resultado.fracaso(ErrorPedido.PEDIDO_NO_COMANDABLE);
        }
        List<CuentaPersona> delPedido = personas.findByPedidoIdOrderByOrden(pedidoId);
        List<String> personasIds = delPedido.stream().map(CuentaPersona::getId).toList();
        Map<String, String> etiquetas = delPedido.stream()
                .collect(Collectors.toMap(CuentaPersona::getId, CuentaPersona::getEtiqueta));
        List<DetallePedido> pendientes = personasIds.isEmpty() ? List.of()
                : lineas.findByCuentaPersonaIdInOrderByCreadoEn(personasIds).stream()
                        .filter(l -> "pendiente".equals(l.getEstado())).toList();
        if (pendientes.isEmpty()) {
            return Resultado.fracaso(ErrorPedido.SIN_LINEAS_PENDIENTES);
        }

        int numero = comandas.maxNumero(pedidoId) + 1;
        Comanda comanda = comandas.save(new Comanda(comandaId != null ? comandaId : Identificador.nuevo().valor(),
                pedidoId, localId, numero, actorId));
        for (DetallePedido linea : pendientes) {
            linea.comandar();
            detalles.save(new DetalleComanda(Identificador.nuevo().valor(), comanda.getId(), linea.getId(),
                    linea.getCantidad(), etiquetas.get(linea.getCuentaPersonaId())));
        }
        pedido.comandar();
        pedidos.save(pedido);
        descontarInventarioPorComanda.descontar(localId, comanda.getId(), pendientes, actorId);
        auditoria.registrar("comanda_emitida", "comanda", comanda.getId(), null,
                RegistradorAuditoria.campos("nombre", "Pedido " + pedido.getNumero(), "numero", numero, "productos",
                        pendientes.size()));
        return Resultado.exito(comanda);
    }
}
