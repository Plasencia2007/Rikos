package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;

@Service
public class ActualizarCategoria {

    private final CategoriaProductoRepositorio categorias;

    public ActualizarCategoria(CategoriaProductoRepositorio categorias) {
        this.categorias = categorias;
    }

    @Transactional
    public Resultado<CategoriaProducto, ErrorCatalogo> ejecutar(String id, String nombre, int orden,
            String actorId) {
        return categorias.findById(id)
                .map(categoria -> {
                    categoria.actualizar(nombre, orden, actorId);
                    return Resultado.<CategoriaProducto, ErrorCatalogo>exito(categorias.save(categoria));
                })
                .orElse(Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO));
    }
}
