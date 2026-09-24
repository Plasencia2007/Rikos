package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;

/** Desactivar impide usarlo en registros nuevos y conserva su historial
 * -- nunca se borra (US-008). */
@Service
public class DesactivarProducto {

    private final ProductoRepositorio productos;

    public DesactivarProducto(ProductoRepositorio productos) {
        this.productos = productos;
    }

    @Transactional
    public Resultado<Producto, ErrorCatalogo> ejecutar(String id, String actorId) {
        return productos.findById(id)
                .map(producto -> {
                    producto.desactivar(actorId);
                    return Resultado.<Producto, ErrorCatalogo>exito(productos.save(producto));
                })
                .orElse(Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO));
    }
}
