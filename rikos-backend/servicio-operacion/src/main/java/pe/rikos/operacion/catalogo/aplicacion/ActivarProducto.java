package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/** Reactivar un producto previamente desactivado (US-008). */
@Service
public class ActivarProducto {

    private final ProductoRepositorio productos;
    private final RegistradorAuditoria auditoria;

    public ActivarProducto(ProductoRepositorio productos, RegistradorAuditoria auditoria) {
        this.productos = productos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Producto, ErrorCatalogo> ejecutar(String id, String actorId) {
        return productos.findById(id)
                .map(producto -> {
                    producto.activar(actorId);
                    Producto guardado = productos.save(producto);
                    auditoria.registrar("cambio_estado", "producto", id,
                            RegistradorAuditoria.campos("activo", false, "nombre", producto.getNombre()),
                            RegistradorAuditoria.campos("activo", true, "nombre", producto.getNombre()));
                    return Resultado.<Producto, ErrorCatalogo>exito(guardado);
                })
                .orElse(Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO));
    }
}
