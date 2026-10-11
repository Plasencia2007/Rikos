package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

/**
 * Desactivar una mesa la saca del mapa sin borrarla (tiene historial de
 * pedidos); reactivarla la devuelve. Idempotente. Queda en la bitacora.
 */
@Service
public class CambiarEstadoMesa {

    private final MesaRepositorio mesas;
    private final PedidoRepositorio pedidos;
    private final RegistradorAuditoria auditoria;

    public CambiarEstadoMesa(MesaRepositorio mesas, PedidoRepositorio pedidos, RegistradorAuditoria auditoria) {
        this.mesas = mesas;
        this.pedidos = pedidos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Mesa, ErrorMesa> ejecutar(String id, boolean activo, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        Mesa mesa = mesas.findByIdAndLocalId(id, localId).orElse(null);
        if (mesa == null) {
            return Resultado.fracaso(ErrorMesa.NO_ENCONTRADA);
        }
        if (mesa.isActivo() == activo) {
            return Resultado.exito(mesa);
        }
        // Una mesa con un pedido en curso no se desactiva: se perderia de vista con gente sentada.
        var enCurso = pedidos.findByMesaIdAndEstadoIn(id, AbrirPedido.ESTADOS_EN_CURSO);
        if (!activo && enCurso.isPresent()) {
            return Resultado.fracaso("abierto".equals(enCurso.get().getEstado()) ? ErrorMesa.TIENE_PEDIDO_ABIERTO
                    : ErrorMesa.MESA_NO_LIBRE);
        }
        // Una mesa unida a otra tampoco se desactiva: saldria del mapa y dejaria a su principal con una mesa fantasma.
        if (!activo && mesa.getMesaPrincipalId() != null) {
            return Resultado.fracaso(ErrorMesa.MESA_NO_LIBRE);
        }
        if (!activo && mesas.existsByMesaPrincipalId(id)) {
            return Resultado.fracaso(ErrorMesa.TIENE_MESAS_UNIDAS);
        }
        boolean estabaActiva = mesa.isActivo();
        mesa.cambiarEstado(activo, actorId);
        mesas.save(mesa);
        auditoria.registrar("cambio_estado", "mesa", id,
                RegistradorAuditoria.campos("activo", estabaActiva, "nombre", "Mesa " + mesa.getNumero()),
                RegistradorAuditoria.campos("activo", activo, "nombre", "Mesa " + mesa.getNumero()));
        return Resultado.exito(mesa);
    }
}
