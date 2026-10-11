package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaPensionistas;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

/**
 * El mozo busca al pensionista (por nombre o DNI) y lo asigna a una persona de la mesa. Paso provisional
 * hasta el ticket del sprint 8. Un inactivo no se asigna (RN-034); uno ya presente en el pedido, tampoco;
 * y una persona con lineas no cambia de tipo aqui (el recalculo es de la entrega 4, US-029 CA5).
 */
@Service
public class AsignarPensionista {

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;
    private final ConsultaPensionistas consultaPensionistas;

    public AsignarPensionista(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas,
                              DetallePedidoRepositorio lineas, ConsultaPensionistas consultaPensionistas) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
        this.consultaPensionistas = consultaPensionistas;
    }

    @Transactional
    public Resultado<CuentaPersona, ErrorPedido> ejecutar(String pedidoId, String personaId, String pensionistaId,
                                                          String actorId) {
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
        Pensionista pensionista = consultaPensionistas.pensionista(pensionistaId).orElse(null);
        if (pensionista == null) {
            return Resultado.fracaso(ErrorPedido.PENSIONISTA_NO_ENCONTRADO);
        }
        if (!pensionista.isActivo()) {
            return Resultado.fracaso(ErrorPedido.PENSIONISTA_INACTIVO);
        }
        if (personas.existsByPedidoIdAndPensionistaId(pedidoId, pensionistaId)) {
            return Resultado.fracaso(ErrorPedido.PENSIONISTA_YA_EN_EL_PEDIDO);
        }
        if (lineas.existsByCuentaPersonaId(personaId)) {
            return Resultado.fracaso(ErrorPedido.PERSONA_CON_LINEAS);
        }
        persona.asignarPensionista(pensionistaId, pensionista.nombreCompleto());
        return Resultado.exito(personas.save(persona));
    }
}
