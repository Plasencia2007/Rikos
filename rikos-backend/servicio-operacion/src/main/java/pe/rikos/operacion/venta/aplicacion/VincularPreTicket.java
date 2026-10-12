package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaPensionistas;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PreTicket;
import pe.rikos.operacion.pensionistas.infraestructura.PreTicketRepositorio;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.time.OffsetDateTime;

/**
 * US-020: Vincular el pre-ticket emitido en el kiosco al pedido del pensionista en la mesa.
 */
@Service
public class VincularPreTicket {

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;
    private final PreTicketRepositorio preTickets;
    private final ConsultaPensionistas consultaPensionistas;

    public VincularPreTicket(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas,
                             DetallePedidoRepositorio lineas, PreTicketRepositorio preTickets,
                             ConsultaPensionistas consultaPensionistas) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
        this.preTickets = preTickets;
        this.consultaPensionistas = consultaPensionistas;
    }

    @Transactional
    public Resultado<CuentaPersona, ErrorPedido> ejecutar(String pedidoId, String personaId, String codigo,
                                                          String actorId) {
        if (codigo == null || codigo.isBlank()) {
            return Resultado.fracaso(ErrorPedido.PRE_TICKET_REQUERIDO);
        }
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        Pedido pedidoActual = pedidos.findByIdAndLocalId(pedidoId, localId).orElse(null);
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
        if (lineas.existsByCuentaPersonaId(personaId)) {
            return Resultado.fracaso(ErrorPedido.PERSONA_CON_LINEAS);
        }

        PreTicket pt = preTickets.findByCodigo(codigo.trim()).orElse(null);
        if (pt == null) {
            return Resultado.fracaso(ErrorPedido.PRE_TICKET_NO_ENCONTRADO);
        }
        if (!localId.equals(pt.getLocalId())) {
            return Resultado.fracaso(ErrorPedido.PRE_TICKET_DE_OTRO_LOCAL);
        }
        if (!pedidoActual.getServicio().equals(pt.getServicio())) {
            return Resultado.fracaso(ErrorPedido.PRE_TICKET_SERVICIO_DISTINTO);
        }
        if (pt.estaVencidoPorTiempo(OffsetDateTime.now()) || PreTicket.ESTADO_VENCIDO.equals(pt.getEstado())) {
            return Resultado.fracaso(ErrorPedido.PRE_TICKET_VENCIDO);
        }
        if (PreTicket.ESTADO_VINCULADO.equals(pt.getEstado())) {
            return Resultado.fracaso(ErrorPedido.PRE_TICKET_YA_VINCULADO);
        }
        if (PreTicket.ESTADO_CERRADO.equals(pt.getEstado())) {
            return Resultado.fracaso(ErrorPedido.PRE_TICKET_CERRADO);
        }
        if (PreTicket.ESTADO_ANULADO.equals(pt.getEstado())) {
            return Resultado.fracaso(ErrorPedido.PRE_TICKET_ANULADO);
        }

        Pensionista pensionista = consultaPensionistas.pensionista(pt.getPensionistaId()).orElse(null);
        if (pensionista == null) {
            return Resultado.fracaso(ErrorPedido.PENSIONISTA_NO_ENCONTRADO);
        }
        if (!pensionista.isActivo()) {
            return Resultado.fracaso(ErrorPedido.PENSIONISTA_INACTIVO);
        }
        if (personas.existsByPedidoIdAndPensionistaId(pedidoId, pensionista.getId())) {
            return Resultado.fracaso(ErrorPedido.PENSIONISTA_YA_EN_EL_PEDIDO);
        }

        pt.vincular(actorId);
        preTickets.save(pt);

        persona.asignarPensionista(pensionista.getId(), pensionista.nombreCompleto(), pt.getId());
        CuentaPersona guardada = personas.save(persona);

        return Resultado.exito(guardada);
    }
}
