package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/**
 * Borrado real de un proveedor -- distinto de {@link DesactivarProveedor},
 * que es la via normal para sacarlo de circulacion sin perder su historial.
 * Este caso de uso es la excepcion explicita: solo procede si el proveedor
 * nunca llego a usarse. Hoy ninguna tabla referencia proveedor todavia (las
 * compras reales son del sprint 9) -- cuando esa tabla exista, agregar aqui
 * el mismo chequeo de existencia que EliminarProducto hace con
 * presentacion_compra, antes de deleteById.
 *
 * US-006: se audita con evento "eliminacion" -- es la unica prueba de que
 * este proveedor existio, una vez borrado.
 */
@Service
public class EliminarProveedor {

    private final ProveedorRepositorio proveedores;
    private final RegistradorAuditoria auditoria;

    public EliminarProveedor(ProveedorRepositorio proveedores, RegistradorAuditoria auditoria) {
        this.proveedores = proveedores;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Void, ErrorCatalogo> ejecutar(String id) {
        Proveedor proveedor = proveedores.findById(id).orElse(null);
        if (proveedor == null) {
            return Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO);
        }
        proveedores.deleteById(id);
        auditoria.registrar("eliminacion", "proveedor", id,
                RegistradorAuditoria.campos("razonSocial", proveedor.getRazonSocial(), "ruc", proveedor.getRuc()),
                null);
        return Resultado.<Void, ErrorCatalogo>exito(null);
    }
}
