package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;

import java.util.regex.Pattern;

@Service
public class ActualizarProveedor {

    private static final Pattern FORMATO_RUC = Pattern.compile("^\\d{11}$");

    private final ProveedorRepositorio proveedores;

    public ActualizarProveedor(ProveedorRepositorio proveedores) {
        this.proveedores = proveedores;
    }

    @Transactional
    public Resultado<Proveedor, ErrorCatalogo> ejecutar(String id, Proveedor.DatosActualizacion datos,
            String actorId) {
        if (datos.ruc() != null && !FORMATO_RUC.matcher(datos.ruc()).matches()) {
            return Resultado.fracaso(ErrorCatalogo.RUC_INVALIDO);
        }
        return proveedores.findById(id)
                .map(proveedor -> {
                    proveedor.actualizar(datos, actorId);
                    return Resultado.<Proveedor, ErrorCatalogo>exito(proveedores.save(proveedor));
                })
                .orElse(Resultado.fracaso(ErrorCatalogo.NO_ENCONTRADO));
    }
}
