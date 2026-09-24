package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;

import java.math.BigDecimal;

@Service
public class CrearPresentacion {

    private final PresentacionCompraRepositorio presentaciones;
    private final ProductoRepositorio productos;
    private final UnidadMedidaRepositorio unidades;

    public CrearPresentacion(PresentacionCompraRepositorio presentaciones, ProductoRepositorio productos,
                              UnidadMedidaRepositorio unidades) {
        this.presentaciones = presentaciones;
        this.productos = productos;
        this.unidades = unidades;
    }

    @Transactional
    public Resultado<PresentacionCompra, ErrorCatalogo> ejecutar(String productoId, String nombre,
            String unidadId, BigDecimal factor, boolean esPredeterminada, String actorId) {

        if (factor == null || factor.compareTo(BigDecimal.ZERO) <= 0) {
            return Resultado.fracaso(ErrorCatalogo.FACTOR_INVALIDO);
        }

        Producto producto = productos.findById(productoId).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }

        UnidadMedida unidad = unidades.findById(unidadId).orElse(null);
        if (unidad == null) {
            return Resultado.fracaso(ErrorCatalogo.UNIDAD_OBLIGATORIA);
        }

        if (esPredeterminada
                && presentaciones.existsByProductoIdAndEsPredeterminadaTrueAndActivoTrue(productoId)) {
            return Resultado.fracaso(ErrorCatalogo.PREDETERMINADA_DUPLICADA);
        }

        PresentacionCompra presentacion = new PresentacionCompra(Identificador.nuevo().valor(), producto, nombre,
                unidad, factor, esPredeterminada);
        return Resultado.exito(presentaciones.save(presentacion));
    }
}
