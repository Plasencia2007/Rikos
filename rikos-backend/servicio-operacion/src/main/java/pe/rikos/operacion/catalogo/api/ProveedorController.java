package pe.rikos.operacion.catalogo.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.ActualizarProveedor;
import pe.rikos.operacion.catalogo.aplicacion.CrearProveedor;
import pe.rikos.operacion.catalogo.aplicacion.DesactivarProveedor;
import pe.rikos.operacion.catalogo.aplicacion.ErrorCatalogo;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.catalogo.infraestructura.ProveedorRepositorio;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/proveedores")
public class ProveedorController {

    private final CrearProveedor crearProveedor;
    private final ActualizarProveedor actualizarProveedor;
    private final DesactivarProveedor desactivarProveedor;
    private final ProveedorRepositorio proveedores;

    public ProveedorController(CrearProveedor crearProveedor, ActualizarProveedor actualizarProveedor,
                                DesactivarProveedor desactivarProveedor, ProveedorRepositorio proveedores) {
        this.crearProveedor = crearProveedor;
        this.actualizarProveedor = actualizarProveedor;
        this.desactivarProveedor = desactivarProveedor;
        this.proveedores = proveedores;
    }

    public record ProveedorRequest(String ruc, String razonSocial, String nombreComercial, String telefono,
                                    String correo, String direccion, String contacto) {
        Proveedor.DatosActualizacion aDatos() {
            return new Proveedor.DatosActualizacion(ruc, razonSocial, nombreComercial, telefono, correo, direccion,
                    contacto);
        }
    }

    public record ProveedorRespuesta(String id, String ruc, String razonSocial, String nombreComercial,
                                      String telefono, String correo, String direccion, String contacto,
                                      boolean activo) {
        static ProveedorRespuesta de(Proveedor p) {
            return new ProveedorRespuesta(p.getId(), p.getRuc(), p.getRazonSocial(), p.getNombreComercial(),
                    p.getTelefono(), p.getCorreo(), p.getDireccion(), p.getContacto(), p.isActivo());
        }
    }

    @GetMapping
    public List<ProveedorRespuesta> listar() {
        return proveedores.findAll().stream().map(ProveedorRespuesta::de).toList();
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody ProveedorRequest cuerpo, @AuthenticationPrincipal String actorId) {
        Resultado<Proveedor, ErrorCatalogo> resultado = crearProveedor.ejecutar(cuerpo.ruc(), cuerpo.razonSocial(),
                cuerpo.nombreComercial(), cuerpo.telefono(), cuerpo.correo(), cuerpo.direccion(), cuerpo.contacto(),
                actorId);
        return resultado.esExito()
                ? ResponseEntity.status(HttpStatus.CREATED).body(ProveedorRespuesta.de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable String id, @RequestBody ProveedorRequest cuerpo,
                                         @AuthenticationPrincipal String actorId) {
        Resultado<Proveedor, ErrorCatalogo> resultado =
                actualizarProveedor.ejecutar(id, cuerpo.aDatos(), actorId);
        return resultado.esExito()
                ? ResponseEntity.ok(ProveedorRespuesta.de(resultado.valorOLanza()))
                : errorDe(resultado);
    }

    @PostMapping("/{id}/desactivar")
    public ResponseEntity<?> desactivar(@PathVariable String id, @AuthenticationPrincipal String actorId) {
        Resultado<Proveedor, ErrorCatalogo> resultado = desactivarProveedor.ejecutar(id, actorId);
        return resultado.esExito() ? ResponseEntity.noContent().build() : errorDe(resultado);
    }

    private ResponseEntity<?> errorDe(Resultado<?, ErrorCatalogo> resultado) {
        ErrorCatalogo error = ((Resultado.Fracaso<?, ErrorCatalogo>) resultado).error();
        HttpStatus estado = switch (error) {
            case NO_ENCONTRADO -> HttpStatus.NOT_FOUND;
            case RUC_DUPLICADO -> HttpStatus.CONFLICT;
            default -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(estado).body(Map.of("error", error.name()));
    }
}
