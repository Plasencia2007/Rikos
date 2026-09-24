package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.ProductoRepositorio;
import pe.rikos.operacion.catalogo.infraestructura.ProductoServicio;
import pe.rikos.operacion.catalogo.infraestructura.ProductoServicioRepositorio;

import java.util.Set;

/** 4.1 bis: solo aplica a productos cuya categoria tiene
 * naturaleza = producto_fijo -- los componentes de menu no la necesitan,
 * su servicio es siempre el almuerzo y lo fija la programacion semanal. */
@Service
public class AsignarServiciosProducto {

    private final ProductoRepositorio productos;
    private final ProductoServicioRepositorio productoServicios;

    public AsignarServiciosProducto(ProductoRepositorio productos,
                                     ProductoServicioRepositorio productoServicios) {
        this.productos = productos;
        this.productoServicios = productoServicios;
    }

    @Transactional
    public Resultado<Void, ErrorCatalogo> ejecutar(String productoId, Set<String> servicios, String actorId) {
        Producto producto = productos.findById(productoId).orElse(null);
        if (producto == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }
        if (!"producto_fijo".equals(producto.getCategoria().getNaturaleza())) {
            return Resultado.fracaso(ErrorCatalogo.NATURALEZA_INVALIDA);
        }

        // Reemplaza el conjunto completo. El delete debe hacer flush antes
        // del insert -- Hibernate reordena DELETE despues de INSERT en la
        // misma transaccion (bug conocido, ver GuardarHorarioSemanal /
        // AsignarPermisosARol de sprints anteriores).
        productoServicios.deleteByProductoId(productoId);
        productoServicios.flush();

        for (String servicio : servicios) {
            productoServicios.save(new ProductoServicio(producto, servicio));
        }

        return Resultado.exito(null);
    }
}
