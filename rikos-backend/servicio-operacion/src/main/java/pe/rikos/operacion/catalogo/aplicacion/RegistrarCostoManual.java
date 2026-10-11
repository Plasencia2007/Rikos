package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;

import java.math.BigDecimal;

/** El costo inicial a mano (sprint 2). Deja de ser posible en cuanto el
 * producto ya tiene origen_costo='compra' (RegistrarCostoDeCompra, sprint 9).
 *
 * US-010 CA4: el costo nuevo se propaga en cascada a las recetas que usan
 * el producto. La compra del sprint 9 tendra que hacer lo mismo. */
@Service
public class RegistrarCostoManual {

    private final ProductoRepositorio productos;
    private final CalculadoraCostoReceta calculadora;

    public RegistrarCostoManual(ProductoRepositorio productos, CalculadoraCostoReceta calculadora) {
        this.productos = productos;
        this.calculadora = calculadora;
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
        Producto guardado = productos.save(producto);
        calculadora.recalcularEnCascada(productoId);
        return Resultado.exito(guardado);
    }
}
