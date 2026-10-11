package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Guarda de un solo golpe el nuevo orden de un grupo de hermanas (todas las
 * raices, o todas las hijas de una misma categoria) -- lo que en la
 * pantalla es "arrastra para cambiar el orden". No mueve categorias de
 * padre; para eso hace falta crear/borrar, no reordenar.
 */
@Service
public class ReordenarCategorias {

    private final CategoriaProductoRepositorio categorias;

    public ReordenarCategorias(CategoriaProductoRepositorio categorias) {
        this.categorias = categorias;
    }

    @Transactional
    public Resultado<List<CategoriaProducto>, ErrorCatalogo> ejecutar(String categoriaPadreId, List<String> ids,
            String actorId) {
        List<CategoriaProducto> hermanas = categoriaPadreId == null
                ? categorias.findByCategoriaPadreIdIsNullOrderByOrden()
                : categorias.findByCategoriaPadreIdOrderByOrden(categoriaPadreId);

        Set<String> idsEsperados = new HashSet<>();
        for (CategoriaProducto c : hermanas) {
            idsEsperados.add(c.getId());
        }
        // La lista debe traer exactamente las mismas hermanas, ni una mas ni
        // una de menos -- si no, alguien esta reordenando con datos viejos
        // (otra pestaña ya agrego o borro una hermana) y aplicar el orden
        // a ciegas dejaria huecos o duplicados en `orden`.
        if (ids == null || ids.size() != idsEsperados.size() || !idsEsperados.equals(new HashSet<>(ids))) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }

        Map<String, CategoriaProducto> porId = new HashMap<>();
        for (CategoriaProducto c : hermanas) {
            porId.put(c.getId(), c);
        }
        for (int i = 0; i < ids.size(); i++) {
            CategoriaProducto c = porId.get(ids.get(i));
            c.actualizar(c.getNombre(), i + 1, actorId);
        }
        return Resultado.exito(categorias.saveAll(hermanas));
    }
}
