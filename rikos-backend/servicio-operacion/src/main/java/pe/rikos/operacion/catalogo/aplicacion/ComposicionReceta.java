package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Component;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.IngredienteReceta;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Receta;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedidaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validacion comun de CrearReceta y ActualizarReceta: lo que la pantalla
 * manda (ids y cantidades) pasa a ingredientes resueltos, o falla con el
 * ingrediente culpable. Tambien arma la foto de la receta para la
 * bitacora (CA6), igual en ambos casos de uso.
 */
@Component
public class ComposicionReceta {

    public record IngredienteEntrada(String productoId, BigDecimal cantidad, String unidadId) {
    }

    private final ProductoRepositorio productos;
    private final UnidadMedidaRepositorio unidades;
    private final CalculadoraCostoReceta calculadora;

    public ComposicionReceta(ProductoRepositorio productos, UnidadMedidaRepositorio unidades,
                             CalculadoraCostoReceta calculadora) {
        this.productos = productos;
        this.unidades = unidades;
        this.calculadora = calculadora;
    }

    public Resultado<List<Receta.DatosIngrediente>, FalloReceta> resolver(Producto productoReceta,
            BigDecimal rendimiento, int porciones, List<IngredienteEntrada> entradas) {
        if (rendimiento == null || rendimiento.signum() <= 0) {
            return Resultado.fracaso(FalloReceta.de(ErrorReceta.RENDIMIENTO_INVALIDO));
        }
        if (porciones <= 0) {
            return Resultado.fracaso(FalloReceta.de(ErrorReceta.PORCIONES_INVALIDAS));
        }
        if (entradas == null || entradas.isEmpty()) {
            return Resultado.fracaso(FalloReceta.de(ErrorReceta.SIN_INGREDIENTES));
        }
        List<Receta.DatosIngrediente> resueltos = new ArrayList<>();
        Set<String> vistos = new HashSet<>();
        for (IngredienteEntrada entrada : entradas) {
            String id = entrada.productoId();
            Producto ingrediente = id == null ? null : productos.findById(id).orElse(null);
            if (ingrediente == null) {
                return Resultado.fracaso(FalloReceta.en(ErrorReceta.PRODUCTO_NO_ENCONTRADO, id));
            }
            if (!vistos.add(id)) {
                return Resultado.fracaso(FalloReceta.en(ErrorReceta.INGREDIENTE_REPETIDO, id));
            }
            if (!ingrediente.isActivo() || !(ingrediente.isEsInsumo() || ingrediente.isEsSubreceta())) {
                return Resultado.fracaso(FalloReceta.en(ErrorReceta.INGREDIENTE_INVALIDO, id));
            }
            if (entrada.cantidad() == null || entrada.cantidad().signum() <= 0) {
                return Resultado.fracaso(FalloReceta.en(ErrorReceta.CANTIDAD_INVALIDA, id));
            }
            UnidadMedida unidad = entrada.unidadId() == null ? null
                    : unidades.findById(entrada.unidadId()).orElse(null);
            if (unidad == null) {
                return Resultado.fracaso(FalloReceta.en(ErrorReceta.UNIDAD_NO_ENCONTRADA, id));
            }
            if (calculadora.aUnidadBase(ingrediente, entrada.cantidad(), unidad) == null) {
                return Resultado.fracaso(FalloReceta.en(ErrorReceta.SIN_CONVERSION, id));
            }
            if (calculadora.formaCiclo(productoReceta.getId(), ingrediente)) {
                return Resultado.fracaso(FalloReceta.en(ErrorReceta.CICLO, id));
            }
            resueltos.add(new Receta.DatosIngrediente(Identificador.nuevo().valor(), ingrediente,
                    entrada.cantidad(), unidad));
        }
        return Resultado.exito(resueltos);
    }

    /** CA6: lo que se guarda en la bitacora, antes y despues del cambio. */
    public static Map<String, Object> foto(Receta receta) {
        List<String> ingredientes = new ArrayList<>();
        for (IngredienteReceta i : receta.getIngredientes()) {
            ingredientes.add(i.getProducto().getNombre() + ": " + i.getCantidad().stripTrailingZeros().toPlainString()
                    + " " + i.getUnidad().getCodigo());
        }
        return RegistradorAuditoria.campos(
                "nombre", receta.getProducto().getNombre(),
                "rendimiento", receta.getRendimiento().stripTrailingZeros().toPlainString(),
                "porciones", receta.getPorciones(),
                "ingredientes", ingredientes);
    }
}
