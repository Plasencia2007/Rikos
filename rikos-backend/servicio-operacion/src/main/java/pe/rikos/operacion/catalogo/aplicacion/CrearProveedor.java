package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;

import java.util.regex.Pattern;

@Service
public class CrearProveedor {

    private static final Pattern FORMATO_RUC = Pattern.compile("^\\d{11}$");

    private final ProveedorRepositorio proveedores;

    public CrearProveedor(ProveedorRepositorio proveedores) {
        this.proveedores = proveedores;
    }

    @Transactional
    public Resultado<Proveedor, ErrorCatalogo> ejecutar(String ruc, String razonSocial, String nombreComercial,
            String telefono, String correo, String direccion, String contacto, String actorId) {

        if (ruc != null && !FORMATO_RUC.matcher(ruc).matches()) {
            return Resultado.fracaso(ErrorCatalogo.RUC_INVALIDO);
        }
        if (ruc != null && proveedores.existsByRuc(ruc)) {
            return Resultado.fracaso(ErrorCatalogo.RUC_DUPLICADO);
        }

        Proveedor proveedor = new Proveedor(Identificador.nuevo().valor(), ruc, razonSocial, nombreComercial,
                telefono, correo, direccion, contacto, actorId);
        return Resultado.exito(proveedores.save(proveedor));
    }
}
