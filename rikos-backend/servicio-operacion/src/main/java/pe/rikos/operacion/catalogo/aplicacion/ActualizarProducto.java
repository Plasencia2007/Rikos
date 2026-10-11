package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

import java.util.Objects;

@Service
public class ActualizarProducto {

    private final ProductoRepositorio productos;
    private final CategoriaProductoRepositorio categorias;
    private final RegistradorAuditoria auditoria;

    public ActualizarProducto(ProductoRepositorio productos, CategoriaProductoRepositorio categorias,
                               RegistradorAuditoria auditoria) {
        this.productos = productos;
        this.categorias = categorias;
        this.auditoria = auditoria;
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
        // US-014: en un producto de menu la ficha solo cambia nombre, unidad y
        // control de stock. Precio, usos y categoria son del menu: el precio
        // se cambia con CambiarPrecioMenu (menu.modificar), no con
        // catalogo.modificar, que tiene cocina.
        if (producto.esDeMenu()) {
            boolean tocaLoDelMenu = !Objects.equals(producto.getPrecioVenta(), datos.precioVenta())
                    || !datos.esVendible() || datos.esInsumo() || datos.esSubreceta()
                    || !producto.getCategoria().getId().equals(datos.categoria().getId());
            if (tocaLoDelMenu) {
                return Resultado.fracaso(ErrorCatalogo.PRODUCTO_DE_MENU);
            }
        } else {
            if ("menu".equals(datos.categoria().getNaturaleza())) {
                return Resultado.fracaso(ErrorCatalogo.CATEGORIA_RESERVADA);
            }
            if (datos.esVendible() && datos.precioVenta() == null) {
                return Resultado.fracaso(ErrorCatalogo.PRECIO_VENTA_OBLIGATORIO);
            }
        }
        Long precioAnterior = producto.getPrecioVenta();
        producto.actualizar(datos, actorId);
        Producto guardado = productos.save(producto);
        // US-006: solo audita si el precio realmente cambio -- una edicion
        // que toca otros campos y deja el mismo precio no es un "cambio de
        // precio".
        if (!Objects.equals(precioAnterior, datos.precioVenta())) {
            auditoria.registrar("cambio_precio", "producto", id,
                    RegistradorAuditoria.campos("precioVenta", precioAnterior, "nombre", producto.getNombre()),
                    RegistradorAuditoria.campos("precioVenta", datos.precioVenta(), "nombre", producto.getNombre()));
        }
        return Resultado.exito(guardado);
    }
}
