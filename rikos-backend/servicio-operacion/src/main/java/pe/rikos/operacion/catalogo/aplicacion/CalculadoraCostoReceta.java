package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.operacion.catalogo.infraestructura.ConversionUnidad;
import pe.rikos.operacion.catalogo.infraestructura.ConversionUnidadRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.IngredienteReceta;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.Receta;
import pe.rikos.operacion.catalogo.infraestructura.RecetaRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

/**
 * §5.3 (US-010 CA2, CA3, CA4). Un ingrediente se costea de una sola forma,
 * sea insumo o subreceta:
 *
 *   cantidad convertida a la unidad base x costo por unidad base
 *
 * donde el costo por unidad base de una subreceta es costo_total /
 * rendimiento (el rendimiento esta en su unidad base), y el de un insumo
 * es costo_ultima_compra. Si algun ingrediente no tiene costo, la receta
 * queda con costo nulo -- nunca cero, que seria mentir un margen.
 *
 * No abre transaccion: corre dentro de la del caso de uso que la llama.
 */
@Component
public class CalculadoraCostoReceta {

    /** La escala de las columnas NUMERIC(14,6). */
    private static final int ESCALA = 6;

    private final RecetaRepositorio recetas;
    private final ConversionUnidadRepositorio conversiones;

    public CalculadoraCostoReceta(RecetaRepositorio recetas, ConversionUnidadRepositorio conversiones) {
        this.recetas = recetas;
        this.conversiones = conversiones;
    }

    /**
     * Cantidad expresada en la unidad base del producto, o null si la unidad
     * no es la base y el producto no tiene conversion entre las dos. La
     * conversion se lee "1 origen = factor destino", en cualquiera de los
     * dos sentidos.
     */
    public BigDecimal aUnidadBase(Producto producto, BigDecimal cantidad, UnidadMedida unidad) {
        String base = producto.getUnidadBase().getId();
        if (base.equals(unidad.getId())) {
            return cantidad;
        }
        for (ConversionUnidad c : conversiones.findByProductoId(producto.getId())) {
            String origen = c.getUnidadOrigen().getId();
            String destino = c.getUnidadDestino().getId();
            if (origen.equals(unidad.getId()) && destino.equals(base)) {
                return cantidad.multiply(c.getFactor());
            }
            if (origen.equals(base) && destino.equals(unidad.getId())) {
                return cantidad.divide(c.getFactor(), ESCALA, RoundingMode.HALF_UP);
            }
        }
        return null;
    }

    /** Costo por unidad base del producto, o null si todavia no tiene. */
    public BigDecimal costoUnitario(Producto producto) {
        Receta receta = recetas.findByProductoIdAndActivoTrue(producto.getId()).orElse(null);
        if (receta != null) {
            return receta.getCostoTotal() == null
                    ? null
                    : receta.getCostoTotal().divide(receta.getRendimiento(), ESCALA, RoundingMode.HALF_UP);
        }
        return producto.getCostoUltimaCompra();
    }

    /** Recalcula y guarda en la receta costo_total y costo_por_porcion. */
    public void recalcular(Receta receta) {
        BigDecimal total = BigDecimal.ZERO;
        for (IngredienteReceta ingrediente : receta.getIngredientes()) {
            BigDecimal cantidad = aUnidadBase(ingrediente.getProducto(), ingrediente.getCantidad(),
                    ingrediente.getUnidad());
            BigDecimal unitario = costoUnitario(ingrediente.getProducto());
            if (cantidad == null || unitario == null) {
                receta.registrarCosto(null, null);
                return;
            }
            total = total.add(cantidad.multiply(unitario));
        }
        total = total.setScale(ESCALA, RoundingMode.HALF_UP);
        receta.registrarCosto(total,
                total.divide(BigDecimal.valueOf(receta.getPorciones()), ESCALA, RoundingMode.HALF_UP));
    }

    /**
     * CA4: cambio el costo por unidad base de este producto -- recalcula las
     * recetas que lo usan y, hacia arriba, las que usan a esas como
     * subreceta. Termina porque al guardar se rechazan los ciclos; el
     * conjunto de visitados es solo una red de seguridad.
     */
    public void recalcularEnCascada(String productoId) {
        recalcularEnCascada(productoId, new HashSet<>());
    }

    private void recalcularEnCascada(String productoId, Set<String> visitados) {
        if (!visitados.add(productoId)) {
            return;
        }
        for (Receta receta : recetas.findActivasQueUsan(productoId)) {
            recalcular(receta);
            recetas.save(receta);
            recalcularEnCascada(receta.getProducto().getId(), visitados);
        }
    }

    /**
     * §5.2: true si incluir este ingrediente en la receta de productoReceta
     * la haria contenerse a si misma -- directo (es el mismo producto) o a
     * traves de las recetas activas de sus subrecetas.
     */
    public boolean formaCiclo(String productoRecetaId, Producto ingrediente) {
        return alcanza(ingrediente.getId(), productoRecetaId, new HashSet<>());
    }

    private boolean alcanza(String desdeProductoId, String objetivoId, Set<String> visitados) {
        if (desdeProductoId.equals(objetivoId)) {
            return true;
        }
        if (!visitados.add(desdeProductoId)) {
            return false;
        }
        Receta receta = recetas.findByProductoIdAndActivoTrue(desdeProductoId).orElse(null);
        if (receta == null) {
            return false;
        }
        for (IngredienteReceta i : receta.getIngredientes()) {
            if (alcanza(i.getProducto().getId(), objetivoId, visitados)) {
                return true;
            }
        }
        return false;
    }
}
