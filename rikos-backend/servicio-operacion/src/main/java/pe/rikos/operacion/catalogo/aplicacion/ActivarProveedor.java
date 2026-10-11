package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;
import pe.rikos.operacion.comun.auditoria.RegistradorAuditoria;

/** Reactivar un proveedor previamente desactivado (US-011). */
@Service
public class ActivarProveedor {

    private final ProveedorRepositorio proveedores;
    private final RegistradorAuditoria auditoria;

    public ActivarProveedor(ProveedorRepositorio proveedores, RegistradorAuditoria auditoria) {
        this.proveedores = proveedores;
        this.auditoria = auditoria;
    }

    @Transactional
    public Resultado<Proveedor, ErrorCatalogo> ejecutar(String id, String actorId) {
        return proveedores.findById(id)
                .map(proveedor -> {
                    proveedor.activar(actorId);
                    Proveedor guardado = proveedores.save(proveedor);
                    auditoria.registrar("cambio_estado", "proveedor", id,
                            RegistradorAuditoria.campos("activo", false, "nombre", proveedor.getRazonSocial()),
                            RegistradorAuditoria.campos("activo", true, "nombre", proveedor.getRazonSocial()));
                    return Resultado.<Proveedor, ErrorCatalogo>exito(guardado);
                })
                .orElse(Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO));
    }
}
