package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;

import java.math.BigDecimal;

@Service
public class CrearConversion {

    private final ConversionUnidadRepositorio conversiones;
    private final ProductoRepositorio productos;
    private final UnidadMedidaRepositorio unidades;

    public CrearConversion(ConversionUnidadRepositorio conversiones, ProductoRepositorio productos,
                            UnidadMedidaRepositorio unidades) {
        this.conversiones = conversiones;
        this.productos = productos;
        this.unidades = unidades;
    }

    @Transactional
    public Resultado<ConversionUnidad, ErrorCatalogo> ejecutar(String productoId, String unidadOrigenId,
            String unidadDestinoId, BigDecimal factor, String actorId) {

        if (factor == null || factor.compareTo(BigDecimal.ZERO) <= 0) {
            return Resultado.fracaso(ErrorCatalogo.FACTOR_INVALIDO);
        }

        Producto producto = productos.findById(productoId).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }

        UnidadMedida origen = unidades.findById(unidadOrigenId).orElse(null);
        UnidadMedida destino = unidades.findById(unidadDestinoId).orElse(null);
        if (origen == null || destino == null) {
            return Resultado.fracaso(ErrorCatalogo.UNIDAD_OBLIGATORIA);
        }

        if (conversiones.existsByProductoIdAndUnidadOrigenIdAndUnidadDestinoId(productoId, unidadOrigenId,
                unidadDestinoId)) {
            return Resultado.fracaso(ErrorCatalogo.PAR_UNIDADES_DUPLICADO);
        }

        ConversionUnidad conversion =
                new ConversionUnidad(Identificador.nuevo().valor(), producto, origen, destino, factor);
        return Resultado.exito(conversiones.save(conversion));
    }
}
