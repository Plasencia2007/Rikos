package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;

/** Desactivar impide usarlo en compras nuevas y conserva las anteriores
 * (US-011) -- nunca se borra. */
@Service
public class DesactivarProveedor {

    private final ProveedorRepositorio proveedores;

    public DesactivarProveedor(ProveedorRepositorio proveedores) {
        this.proveedores = proveedores;
    }

    @Transactional
    public Resultado<Proveedor, ErrorCatalogo> ejecutar(String id, String actorId) {
        return proveedores.findById(id)
                .map(proveedor -> {
                    proveedor.desactivar(actorId);
                    return Resultado.<Proveedor, ErrorCatalogo>exito(proveedores.save(proveedor));
                })
                .orElse(Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO));
    }
}
