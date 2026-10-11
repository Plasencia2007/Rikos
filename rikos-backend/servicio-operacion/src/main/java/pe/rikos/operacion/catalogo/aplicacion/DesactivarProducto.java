package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/** Desactivar impide usarlo en registros nuevos y conserva su historial
 * -- nunca se borra (US-008). */
@Service
public class DesactivarProducto {

    private final ProductoRepositorio productos;
    private final RegistradorAuditoria auditoria;

    public DesactivarProducto(ProductoRepositorio productos, RegistradorAuditoria auditoria) {
        this.productos = productos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Producto, ErrorCatalogo> ejecutar(String id, String actorId) {
        return productos.findById(id)
                .map(producto -> {
                    // US-014: sin el menu no se vende el almuerzo.
                    if (producto.esDeMenu()) {
                        return Resultado.<Producto, ErrorCatalogo>fracaso(ErrorCatalogo.PRODUCTO_DE_MENU);
                    }
                    producto.desactivar(actorId);
                    Producto guardado = productos.save(producto);
                    auditoria.registrar("cambio_estado", "producto", id,
                            RegistradorAuditoria.campos("activo", true, "nombre", producto.getNombre()),
                            RegistradorAuditoria.campos("activo", false, "nombre", producto.getNombre()));
                    return Resultado.<Producto, ErrorCatalogo>exito(guardado);
                })
                .orElse(Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO));
    }
}
