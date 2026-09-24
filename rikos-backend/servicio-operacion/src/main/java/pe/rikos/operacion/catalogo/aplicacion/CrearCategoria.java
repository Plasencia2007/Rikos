package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;

import java.util.Optional;
import java.util.Set;

@Service
public class CrearCategoria {

    private static final Set<String> NATURALEZAS_VALIDAS =
            Set.of("componente_menu", "producto_fijo", "extra", "insumo", "subreceta");

    private final CategoriaProductoRepositorio categorias;

    public CrearCategoria(CategoriaProductoRepositorio categorias) {
        this.categorias = categorias;
    }

    @Transactional
    public Resultado<CategoriaProducto, ErrorCatalogo> ejecutar(String nombre, String naturaleza,
            String componenteMenu, String categoriaPadreId, int orden, String actorId) {

        if (!NATURALEZAS_VALIDAS.contains(naturaleza)) {
            return Resultado.fracaso(ErrorCatalogo.NATURALEZA_INVALIDA);
        }

        if (categoriaPadreId == null) {
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
        if (categorias.findByCategoriaPadreIdOrderByOrden(categoriaPadreId).stream()
                .anyMatch(c -> c.getNombre().equals(nombre))) {
            return Resultado.fracaso(ErrorCatalogo.NOMBRE_DUPLICADO);
        }

        CategoriaProducto hija =
                new CategoriaProducto(Identificador.nuevo().valor(), padre.get(), nombre, orden, actorId);
        return Resultado.exito(categorias.save(hija));
    }
}
