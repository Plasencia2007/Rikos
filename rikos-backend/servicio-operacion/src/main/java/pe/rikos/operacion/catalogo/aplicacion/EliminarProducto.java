package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.DisponibilidadProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenuRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.PresentacionCompraRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.RecetaRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/**
 * Borrado real de un producto -- distinto de {@link DesactivarProducto}, que
 * es la via normal para sacarlo de circulacion sin perder su historial. Este
 * caso de uso es la excepcion explicita: solo procede si el producto nunca
 * llego a usarse (sin presentaciones de compra registradas). Con historial,
 * la base de datos igual lo impediria -- presentacion_compra referencia
 * producto con ON DELETE RESTRICT -- pero se valida antes para devolver un
 * error claro en vez de una excepcion SQL.
 *
 * US-006: se audita con evento "eliminacion" ANTES de borrar (con el
 * producto todavia en mano) -- desde que se permite borrar, esa fila de
 * auditoria es la unica prueba de que este registro existio.
 */
@Service
public class EliminarProducto {

    private final ProductoRepositorio productos;
    private final PresentacionCompraRepositorio presentaciones;
    private final RecetaRepositorio recetas;
    private final OpcionMenuRepositorio opcionesMenu;
    private final DisponibilidadProductoRepositorio disponibilidades;
    private final RegistradorAuditoria auditoria;

    public EliminarProducto(ProductoRepositorio productos, PresentacionCompraRepositorio presentaciones,
                             RecetaRepositorio recetas, OpcionMenuRepositorio opcionesMenu,
                             DisponibilidadProductoRepositorio disponibilidades, RegistradorAuditoria auditoria) {
        this.disponibilidades = disponibilidades;
        this.productos = productos;
        this.presentaciones = presentaciones;
        this.recetas = recetas;
        this.opcionesMenu = opcionesMenu;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Void, ErrorCatalogo> ejecutar(String id) {
        Producto producto = productos.findById(id).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }
        if (producto.esDeMenu()) {
            return Resultado.fracaso(ErrorCatalogo.PRODUCTO_DE_MENU);
        }
        // US-010: una receta (propia o como ingrediente de otra) tambien es
        // historial -- sin esto el FK RESTRICT reventaria como error SQL.
        // US-013: haber estado en el menu de algun dia tambien es historial.
        if (presentaciones.existsByProductoId(id) || recetas.existsByProductoId(id)
                || recetas.usadoComoIngrediente(id) || opcionesMenu.existsByProductoId(id)
                || disponibilidades.existsByProductoId(id)) {
            return Resultado.fracaso(ErrorCatalogo.TIENE_HISTORIAL);
        }
        productos.deleteById(id);
        auditoria.registrar("eliminacion", "producto", id,
                RegistradorAuditoria.campos("codigo", producto.getCodigo(), "nombre", producto.getNombre()), null);
        return Resultado.<Void, ErrorCatalogo>exito(null);
    }
}
