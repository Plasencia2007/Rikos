package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.DisponibilidadProducto;
import pe.rikos.operacion.catalogo.infraestructura.DisponibilidadProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/**
 * US-015 CA1: marcar agotado en el local activo. Un toque, sin motivo --
 * pasa en pleno servicio; el motivo se pide al reactivar (RN-066). Queda en
 * la bitacora como "marcado_agotado", con el local (lo pone el registrador).
 */
@Service
public class MarcarAgotado {

    private final DisponibilidadProductoRepositorio disponibilidades;
    private final ProductoRepositorio productos;
    private final RegistradorAuditoria auditoria;

    public MarcarAgotado(DisponibilidadProductoRepositorio disponibilidades, ProductoRepositorio productos,
                         RegistradorAuditoria auditoria) {
        this.disponibilidades = disponibilidades;
        this.productos = productos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<DisponibilidadProducto, ErrorDisponibilidad> ejecutar(String productoId) {
        String localId = LocalActivo.actual().orElse(null);
        if (localId == null) {
            return Resultado.fracaso(ErrorDisponibilidad.SIN_LOCAL_ACTIVO);
        }
        Producto producto = productos.findById(productoId).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorDisponibilidad.PRODUCTO_NO_ENCONTRADO);
        }
        if (!producto.isEsVendible() || !producto.isActivo()) {
            return Resultado.fracaso(ErrorDisponibilidad.PRODUCTO_NO_VENDIBLE);
        }
        DisponibilidadProducto disponibilidad = disponibilidades.findByLocalIdAndProductoId(localId, productoId)
                .orElseGet(() -> new DisponibilidadProducto(Identificador.nuevo().valor(), localId, producto));
        if (disponibilidad.isAgotado()) {
            return Resultado.fracaso(ErrorDisponibilidad.YA_AGOTADO);
        }
        disponibilidad.marcarAgotado(LocalActivo.usuario().orElse(null));
        DisponibilidadProducto guardada = disponibilidades.save(disponibilidad);
        auditoria.registrar("marcado_agotado", "producto", productoId,
                RegistradorAuditoria.campos("agotado", false, "nombre", producto.getNombre()),
                RegistradorAuditoria.campos("agotado", true, "nombre", producto.getNombre()));
        return Resultado.exito(guardada);
    }
}
