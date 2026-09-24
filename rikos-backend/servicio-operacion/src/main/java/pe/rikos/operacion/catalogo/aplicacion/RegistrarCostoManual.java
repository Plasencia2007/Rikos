package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;

import java.math.BigDecimal;

/** El costo inicial a mano (sprint 2). Deja de ser posible en cuanto el
 * producto ya tiene origen_costo='compra' (RegistrarCostoDeCompra, sprint 9). */
@Service
public class RegistrarCostoManual {

    private final ProductoRepositorio productos;

    public RegistrarCostoManual(ProductoRepositorio productos) {
        this.productos = productos;
    }

    @Transactional
    public Resultado<Producto, ErrorCatalogo> ejecutar(String productoId, BigDecimal costo, String actorId) {
        Producto producto = productos.findById(productoId).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }
        if (!producto.costoEsEditableAMano()) {
            return Resultado.fracaso(ErrorCatalogo.COSTO_YA_CONFIRMADO_POR_COMPRA);
        }
        producto.registrarCostoManual(costo, actorId);
        return Resultado.exito(productos.save(producto));
    }
}
