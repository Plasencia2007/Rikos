package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;

/** Los seis roles de sistema se pueden ver y ajustar (descripcion, color, permisos) pero no renombrar (3.4). */
@Service
public class ActualizarRol {

    private final RolRepositorio roles;

    public ActualizarRol(RolRepositorio roles) {
        this.roles = roles;
    }

    @Transactional
    public Resultado<Rol, ErrorRol> ejecutar(String id, String nombre, String descripcion, String color) {
        return roles.findById(id)
                .map(rol -> {
                    rol.actualizar(nombre, descripcion, color);
                    return Resultado.<Rol, ErrorRol>exito(rol);
                })
                .orElseGet(() -> Resultado.fracaso(ErrorRol.NO_ENCONTRADO));
    }
}
