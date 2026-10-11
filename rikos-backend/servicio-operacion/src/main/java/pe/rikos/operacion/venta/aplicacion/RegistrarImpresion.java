package pe.rikos.operacion.venta.aplicacion;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Comanda;
import pe.rikos.operacion.venta.infraestructura.ComandaRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

/**
 * US-030: registra que una comanda se imprimio. La primera impresion sale limpia; desde la segunda es una COPIA
 * (RN-063: evita que cocina prepare dos veces el mismo plato) y queda en la bitacora. El contador se suma con un
 * UPDATE atomico: dos personas imprimiendo a la vez cuentan dos.
 */
@Service
public class RegistrarImpresion {

    private final ComandaRepositorio comandas;
    private final PedidoRepositorio pedidos;
    private final RegistradorAuditoria auditoria;
    private final EntityManager entityManager;

    public RegistrarImpresion(ComandaRepositorio comandas, PedidoRepositorio pedidos, RegistradorAuditoria auditoria,
                              EntityManager entityManager) {
        this.comandas = comandas;
        this.pedidos = pedidos;
        this.auditoria = auditoria;
        this.entityManager = entityManager;
    }

    @Transactional
    public Resultado<ImpresionRegistrada, ErrorPedido> ejecutar(String comandaId, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        Comanda comanda = comandas.findByIdAndLocalId(comandaId, localId).orElse(null);
        if (comanda == null) {
            return Resultado.fracaso(ErrorPedido.COMANDA_NO_ENCONTRADA);
        }
        Integer antes = comandas.sumarImpresion(comandaId);
        entityManager.refresh(comanda);
        boolean copia = antes != null && antes >= 1;
        if (copia) {
            String numeroDePedido = pedidos.findById(comanda.getPedidoId()).map(p -> p.getNumero()).orElse("");
            auditoria.registrar("reimpresion_comanda", "comanda", comandaId, null,
                    RegistradorAuditoria.campos("nombre", "Pedido " + numeroDePedido, "numero", comanda.getNumero(),
                            "impresiones", comanda.getImpresiones()));
        }
        return Resultado.exito(new ImpresionRegistrada(comanda, copia));
    }
}
