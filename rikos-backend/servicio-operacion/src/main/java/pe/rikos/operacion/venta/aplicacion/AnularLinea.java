package pe.rikos.operacion.venta.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.MotivoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

/**
 * US-031: anula una linea con motivo (tipo "anulacion", RN-072). El permiso venta.anular lo exige el controlador.
 * Antes de comandar no toca inventario; despues de comandar queda marcada anulado_post_comanda porque la receta ya
 * se gasto (RN-065; el descuento real es del sprint 9). Lo anulado no suma al total y sigue a la vista con su motivo.
 */
@Service
public class AnularLinea {

    private static final int MAXIMO_OBSERVACION = 250;

    private final PedidoRepositorio pedidos;
    private final CuentaPersonaRepositorio personas;
    private final DetallePedidoRepositorio lineas;
    private final MotivoRepositorio motivos;
    private final ProductoRepositorio productos;
    private final RegistradorAuditoria auditoria;

    public AnularLinea(PedidoRepositorio pedidos, CuentaPersonaRepositorio personas, DetallePedidoRepositorio lineas,
                       MotivoRepositorio motivos, ProductoRepositorio productos, RegistradorAuditoria auditoria) {
        this.pedidos = pedidos;
        this.personas = personas;
        this.lineas = lineas;
        this.motivos = motivos;
        this.productos = productos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Void, ErrorPedido> ejecutar(String pedidoId, String lineaId, String motivoId, String observacion,
                                                 String actorId) {
        String localId = LocalDeSesion.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorPedido.SIN_LOCAL_ACTIVO);
        }
        Pedido pedido = pedidos.findByIdAndLocalId(pedidoId, localId).orElse(null);
        if (pedido == null) {
            return Resultado.fracaso(ErrorPedido.NO_ENCONTRADO);
        }
        if (AbrirPedido.estaCerrado(pedido)) {
            return Resultado.fracaso(ErrorPedido.PEDIDO_CERRADO);
        }
        DetallePedido linea = lineas.findByIdAndCuentaPersonaIdIn(lineaId,
                personas.findByPedidoIdOrderByOrden(pedidoId).stream().map(CuentaPersona::getId).toList()).orElse(null);
        if (linea == null) {
            return Resultado.fracaso(ErrorPedido.LINEA_NO_ENCONTRADA);
        }
        if ("anulado".equals(linea.getEstado())) {
            return Resultado.fracaso(ErrorPedido.LINEA_ANULADA);
        }
        if ("cobrado".equals(linea.getEstado())) {
            return Resultado.fracaso(ErrorPedido.LINEA_COBRADA);
        }
        Motivo motivo = motivoId == null ? null : motivos.findById(motivoId).orElse(null);
        if (motivo == null) {
            return Resultado.fracaso(ErrorPedido.MOTIVO_NO_ENCONTRADO);
        }
        if (!motivo.isActivo() || !"anulacion".equals(motivo.getTipo())) {
            return Resultado.fracaso(ErrorPedido.MOTIVO_INVALIDO);
        }
        String nota = observacion == null || observacion.isBlank() ? null : observacion.trim();
        if (motivo.isRequiereObservacion() && nota == null) {
            return Resultado.fracaso(ErrorPedido.OBSERVACION_OBLIGATORIA);
        }
        if (nota != null && nota.length() > MAXIMO_OBSERVACION) {
            return Resultado.fracaso(ErrorPedido.OBSERVACION_LARGA);
        }

        String estadoAnterior = linea.getEstado();
        linea.anular(motivo.getId(), nota, actorId);
        lineas.save(linea);
        auditoria.registrar("anulacion", "detalle_pedido", linea.getId(),
                RegistradorAuditoria.campos("estado", estadoAnterior),
                RegistradorAuditoria.campos("nombre", "Pedido " + pedido.getNumero(), "producto", nombreDe(linea),
                        "cantidad", linea.getCantidad(), "despuesDeComandar", linea.isAnuladoPostComanda(),
                        "motivo", motivo.getDescripcion()),
                motivo.getId(), nota);
        return Resultado.exito(null);
    }

    private String nombreDe(DetallePedido linea) {
        return switch (linea.getTipoLinea()) {
            case "plan_desayuno" -> "Desayuno del plan";
            case "plan_cena" -> "Cena del plan";
            case "extra_libre" -> linea.getDescripcion();
            default -> productos.findById(linea.getProductoId()).map(Producto::getNombre).orElse("Producto");
        };
    }
}
