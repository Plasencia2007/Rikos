package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProducto;
import pe.rikos.operacion.catalogo.infraestructura.CategoriaProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/**
 * Borrado real de una categoria o subcategoria -- solo cuando esta
 * realmente vacia, para nunca chocar con el ON DELETE RESTRICT de
 * categoria_producto.categoria_padre_id ni de producto.categoria_id
 * (mismo criterio que {@link EliminarProducto}: se valida antes para
 * devolver un error claro en vez de una excepcion SQL).
 *
 * Sin equivalente a "desactivar": una categoria vacia no tiene historial
 * que conservar, asi que no hace falta el concepto de baja logica que si
 * tiene producto (activo/inactivo). Si mas adelante se necesita ocultar
 * una categoria CON productos sin borrarla, ese es un caso de uso nuevo,
 * no este.
 *
 * US-006: se audita con evento "eliminacion".
 */
@Service
public class EliminarCategoria {

    private final CategoriaProductoRepositorio categorias;
    private final ProductoRepositorio productos;
    private final RegistradorAuditoria auditoria;

    public EliminarCategoria(CategoriaProductoRepositorio categorias, ProductoRepositorio productos,
                              RegistradorAuditoria auditoria) {
        this.categorias = categorias;
        this.productos = productos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Void, ErrorCatalogo> ejecutar(String id) {
        CategoriaProducto categoria = categorias.findById(id).orElse(null);
        if (categoria == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }
        if (categorias.existsByCategoriaPadreId(id)) {
            return Resultado.fracaso(ErrorCatalogo.CATEGORIA_TIENE_SUBCATEGORIAS);
        }
        if (productos.existsByCategoriaId(id)) {
            return Resultado.fracaso(ErrorCatalogo.CATEGORIA_TIENE_PRODUCTOS);
        }
        categorias.deleteById(id);
        auditoria.registrar("eliminacion", "categoria", id,
                RegistradorAuditoria.campos("nombre", categoria.getNombre(), "naturaleza", categoria.getNaturaleza()),
                null);
        return Resultado.<Void, ErrorCatalogo>exito(null);
    }
}
