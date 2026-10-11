package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.Mesa;
import pe.rikos.operacion.venta.infraestructura.MesaRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

/**
 * RF-028 (resuelto en el sprint 6): mozo, cajero o administrador bloquean una
 * mesa LIBRE, siempre con motivo (texto libre: "mesa rota", "reservada"). Una
 * mesa unida a otra, inactiva o con un pedido en curso no se bloquea (spec 3.2).
 */
@Service
public class BloquearMesa {

    private static final int LARGO_MAXIMO_MOTIVO = 250;

    private final MesaRepositorio mesas;
    private final PedidoRepositorio pedidos;
    private final RegistradorAuditoria auditoria;

    public BloquearMesa(MesaRepositorio mesas, PedidoRepositorio pedidos, RegistradorAuditoria auditoria) {
        this.mesas = mesas;
        this.pedidos = pedidos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Mesa, ErrorMesa> ejecutar(String id, String motivo, String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorMesa.SIN_LOCAL_ACTIVO);
        }
        Mesa mesa = mesas.findByIdAndLocalId(id, localId).orElse(null);
        if (mesa == null) {
            return Resultado.fracaso(ErrorMesa.NO_ENCONTRADA);
        }
        String texto = ValidadorDeMesa.limpio(motivo);
        if (texto == null) {
            return Resultado.fracaso(ErrorMesa.MOTIVO_OBLIGATORIO);
        }
        if (!mesa.isActivo()) {
            return Resultado.fracaso(ErrorMesa.MESA_INACTIVA);
        }
        if (mesa.isBloqueada()) {
            return Resultado.fracaso(ErrorMesa.YA_BLOQUEADA);
        }
        if (mesas.existsByMesaPrincipalId(id)) {
            return Resultado.fracaso(ErrorMesa.TIENE_MESAS_UNIDAS);
        }
        var enCurso = pedidos.findByMesaIdAndEstadoIn(id, AbrirPedido.ESTADOS_EN_CURSO);
        if (enCurso.isPresent() && "abierto".equals(enCurso.get().getEstado())) {
            // Un pedido abierto sin comandar no ocupa la mesa, pero tampoco se pierde de vista: primero se termina.
            return Resultado.fracaso(ErrorMesa.TIENE_PEDIDO_ABIERTO);
        }
        if (mesa.getMesaPrincipalId() != null || enCurso.isPresent()) {
            return Resultado.fracaso(ErrorMesa.MESA_NO_LIBRE);
        }
        mesa.bloquear(texto.length() > LARGO_MAXIMO_MOTIVO ? texto.substring(0, LARGO_MAXIMO_MOTIVO) : texto, actorId);
        mesas.save(mesa);
        auditoria.registrar("bloqueo_mesa", "mesa", id,
                RegistradorAuditoria.campos("bloqueada", false, "nombre", "Mesa " + mesa.getNumero()),
                RegistradorAuditoria.campos("bloqueada", true, "motivo", mesa.getMotivoBloqueo(), "nombre",
                        "Mesa " + mesa.getNumero()));
        return Resultado.exito(mesa);
    }
}
