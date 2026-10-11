package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Receta;
import pe.rikos.operacion.catalogo.infraestructura.RecetaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

import java.math.BigDecimal;
import java.util.List;

/**
 * US-010 CA1-CA3, CA6: alta de la receta de un producto vendible o de una
 * subreceta. Al guardarla se calcula su costo, y como el costo por unidad
 * base de su producto cambio (antes no tenia receta), se recalculan en
 * cascada las recetas que ya la usaban como subreceta.
 */
@Service
public class CrearReceta {

    private final RecetaRepositorio recetas;
    private final ProductoRepositorio productos;
    private final ComposicionReceta composicion;
    private final CalculadoraCostoReceta calculadora;
    private final RegistradorAuditoria auditoria;

    public CrearReceta(RecetaRepositorio recetas, ProductoRepositorio productos, ComposicionReceta composicion,
                       CalculadoraCostoReceta calculadora, RegistradorAuditoria auditoria) {
        this.recetas = recetas;
        this.productos = productos;
        this.composicion = composicion;
        this.calculadora = calculadora;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Receta, FalloReceta> ejecutar(String productoId, BigDecimal rendimiento, int porciones,
            List<ComposicionReceta.IngredienteEntrada> ingredientes, String actorId) {
        Producto producto = productos.findById(productoId).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(FalloReceta.de(ErrorReceta.PRODUCTO_NO_ENCONTRADO));
        }
        if (!producto.isEsVendible() && !producto.isEsSubreceta()) {
            return Resultado.fracaso(FalloReceta.de(ErrorReceta.PRODUCTO_NO_ADMITE_RECETA));
        }
        if (recetas.findByProductoIdAndActivoTrue(productoId).isPresent()) {
            return Resultado.fracaso(FalloReceta.de(ErrorReceta.RECETA_YA_EXISTE));
        }
        var resueltos = composicion.resolver(producto, rendimiento, porciones, ingredientes);
        if (!resueltos.esExito()) {
            return Resultado.fracaso(((Resultado.Fracaso<?, FalloReceta>) resueltos).error());
        }
        Receta receta = new Receta(Identificador.nuevo().valor(), producto, rendimiento, porciones, actorId);
        receta.componer(rendimiento, porciones, resueltos.valorOLanza(), null);
        calculadora.recalcular(receta);
        Receta guardada = recetas.save(receta);
        calculadora.recalcularEnCascada(productoId);
        auditoria.registrar("cambio_receta", "receta", guardada.getId(), null, ComposicionReceta.foto(guardada));
        return Resultado.exito(guardada);
    }
}
