package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;

import java.util.Optional;
import java.util.Set;

@Service
public class CrearCategoria {

    private static final Set<String> NATURALEZAS_VALIDAS =
            Set.of("componente_menu", "producto_fijo", "extra", "insumo", "subreceta");

    private final CategoriaProductoRepositorio categorias;
    private final ProductoRepositorio productos;

    public CrearCategoria(CategoriaProductoRepositorio categorias, ProductoRepositorio productos) {
        this.categorias = categorias;
        this.productos = productos;
    }

    @Transactional
    public Resultado<CategoriaProducto, ErrorCatalogo> ejecutar(String nombre, String naturaleza,
            String componenteMenu, String categoriaPadreId, int orden, String actorId) {

        // La rama primero, la validacion de naturaleza despues: una
        // subcategoria hereda naturaleza del padre (el constructor de hija
        // la ignora) y el frontend real ni la manda -- llega null. Validarla
        // antes de saber en que rama estamos revienta con NPE cada vez que
        // se agrega una subcategoria (Set.of(...).contains(null) no
        // devuelve false, lanza).
        if (categoriaPadreId == null) {
            if (!NATURALEZAS_VALIDAS.contains(naturaleza)) {
                return Resultado.fracaso(ErrorCatalogo.NATURALEZA_INVALIDA);
            }
            if (categorias.findByCategoriaPadreIdIsNullOrderByOrden().stream()
                    .anyMatch(c -> c.getNombre().equals(nombre))) {
                return Resultado.fracaso(ErrorCatalogo.NOMBRE_DUPLICADO);
            }
            CategoriaProducto raiz = new CategoriaProducto(
                    Identificador.nuevo().valor(), nombre, naturaleza, componenteMenu, orden, actorId);
            return Resultado.exito(categorias.save(raiz));
        }

        Optional<CategoriaProducto> padre = categorias.findById(categoriaPadreId);
        if (padre.isEmpty()) {
            return Resultado.fracaso(ErrorCatalogo.CATEGORIA_PADRE_NO_ENCONTRADA);
        }
        // 4.1: el arbol tiene dos niveles nada mas. Sin esto, una hija de
        // hija quedaria guardada con nivel 2 igual que su padre (el
        // constructor de hija siempre pone nivel = 2) y el arbol real
        // tendria tres niveles escondidos que nada mas en esta capa detecta.
        if (padre.get().getNivel() != 1) {
            return Resultado.fracaso(ErrorCatalogo.PADRE_NO_ES_RAIZ);
        }
        // 4.1, regla de hoja: si el padre ya tiene productos colgados
        // directamente, dejarlo tener una subcategoria lo saca de "hoja"
        // sin mover esos productos a ningun lado -- se rechaza en vez de
        // dejar productos huerfanos de la regla.
        if (productos.existsByCategoriaIdAndActivoTrue(categoriaPadreId)) {
            return Resultado.fracaso(ErrorCatalogo.PADRE_TIENE_PRODUCTOS);
        }
        if (categorias.findByCategoriaPadreIdOrderByOrden(categoriaPadreId).stream()
                .anyMatch(c -> c.getNombre().equals(nombre))) {
            return Resultado.fracaso(ErrorCatalogo.NOMBRE_DUPLICADO);
        }

        CategoriaProducto hija =
                new CategoriaProducto(Identificador.nuevo().valor(), padre.get(), nombre, orden, actorId);
        return Resultado.exito(categorias.save(hija));
    }
}
