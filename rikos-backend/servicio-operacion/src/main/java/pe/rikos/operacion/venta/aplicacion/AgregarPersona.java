package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.List;

/** Suma una persona (cliente normal) a la mesa: "Persona N" o el nombre que se le de. Idempotente por id. */
@Service
public class AgregarPersona {

    private static final int LARGO_MAXIMO_ETIQUETA = 120;

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;

    public AgregarPersona(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas) {
        this.pedidos = pedidos;
        this.personas = personas;
    }

    @Transactional
    public Resultado<CuentaPersona, ErrorPedido> ejecutar(String pedidoId, String personaId, String etiqueta,
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
        if (personaId != null) {
            // Un id que ya existe en OTRO pedido (o local) no se reutiliza: save() con un id asignado haria un merge
            // y sobrescribiria esa fila. Uno de este mismo pedido es un reenvio: se devuelve tal cual.
            var existente = personas.findById(personaId);
            if (existente.isPresent()) {
                return existente.get().getPedidoId().equals(pedidoId) ? Resultado.exito(existente.get())
                        : Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
            }
        }
        List<CuentaPersona> actuales = personas.findByPedidoIdOrderByOrden(pedidoId);
        int orden = actuales.stream().mapToInt(CuentaPersona::getOrden).max().orElse(0) + 1;
        String texto = etiqueta == null ? "" : etiqueta.trim();
        if (texto.isEmpty()) {
            texto = "Persona " + orden;
        } else if (texto.length() > LARGO_MAXIMO_ETIQUETA) {
            texto = texto.substring(0, LARGO_MAXIMO_ETIQUETA);
        }
        return Resultado.exito(personas.save(new CuentaPersona(
                personaId != null ? personaId : Identificador.nuevo().valor(), pedidoId, texto, orden)));
    }
}
