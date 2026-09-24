package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;

@Service
public class ActualizarProducto {

    private final ProductoRepositorio productos;
    private final CategoriaProductoRepositorio categorias;

    public ActualizarProducto(ProductoRepositorio productos, CategoriaProductoRepositorio categorias) {
        this.productos = productos;
        this.categorias = categorias;
    }

    @Transactional
    public Resultado<Producto, ErrorCatalogo> ejecutar(String id, Producto.DatosActualizacion datos,
            String actorId) {
        Producto producto = productos.findById(id).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }
        if (categorias.existsByCategoriaPadreIdAndActivoTrue(datos.categoria().getId())) {
            return Resultado.fracaso(ErrorCatalogo.CATEGORIA_NO_ES_HOJA);
        }
        if (datos.esVendible() && datos.precioVenta() == null) {
            return Resultado.fracaso(ErrorCatalogo.PRECIO_VENTA_OBLIGATORIO);
        }
        producto.actualizar(datos, actorId);
        return Resultado.exito(productos.save(producto));
    }
}
