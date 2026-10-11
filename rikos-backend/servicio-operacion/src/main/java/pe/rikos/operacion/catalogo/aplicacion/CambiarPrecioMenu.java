package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

import java.util.Objects;

/**
 * US-014 CA2/CA3: el precio unico del menu normal y el precio propio del
 * extra. Unica via para cambiarlo -- ActualizarProducto lo rechaza en un
 * producto de menu, porque ese endpoint lo puede usar cocina
 * (catalogo.modificar) y este exige menu.modificar.
 *
 * Audita "cambio_precio" con la misma forma que ActualizarProducto, asi la
 * bitacora lo muestra igual ("cambio el precio de ... de S/ x a S/ y").
 */
@Service
public class CambiarPrecioMenu {

    private final ProductoRepositorio productos;
    private final RegistradorAuditoria auditoria;

    public CambiarPrecioMenu(ProductoRepositorio productos, RegistradorAuditoria auditoria) {
        this.productos = productos;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Producto, ErrorMenu> ejecutar(String tipo, Long precioVenta, String actorId) {
        TipoMenu tipoMenu = TipoMenu.desde(tipo).orElse(null);
        if (tipoMenu == null) {
            return Resultado.fracaso(ErrorMenu.TIPO_INVALIDO);
        }
        if (precioVenta == null || precioVenta <= 0) {
            return Resultado.fracaso(ErrorMenu.PRECIO_INVALIDO);
        }
        // La semilla de V21 garantiza que existe; si alguien la borro a mano
        // en la base, esto es un error de datos, no del usuario.
        Producto producto = productos.findByTipoMenu(tipoMenu.codigo())
                .orElseThrow(() -> new IllegalStateException("Falta el producto del menu " + tipoMenu.codigo()));
        Long anterior = producto.getPrecioVenta();
        if (Objects.equals(anterior, precioVenta)) {
            return Resultado.exito(producto);
        }
        producto.cambiarPrecio(precioVenta, actorId);
        Producto guardado = productos.save(producto);
        auditoria.registrar("cambio_precio", "producto", producto.getId(),
                RegistradorAuditoria.campos("precioVenta", anterior, "nombre", producto.getNombre()),
                RegistradorAuditoria.campos("precioVenta", precioVenta, "nombre", producto.getNombre()));
        return Resultado.exito(guardado);
    }
}
