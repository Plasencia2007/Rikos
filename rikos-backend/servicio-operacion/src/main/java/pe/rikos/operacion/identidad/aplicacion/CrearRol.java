package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;

/** Se permite crear roles nuevos ademas de los seis del negocio (3.4); los nuevos no son "de sistema". */
@Service
public class CrearRol {

    private final RolRepositorio roles;

    public CrearRol(RolRepositorio roles) {
        this.roles = roles;
    }

    @Transactional
    public Resultado<Rol, ErrorRol> ejecutar(String nombre, String descripcion, String color) {
        if (roles.findByNombre(nombre).isPresent()) {
            return Resultado.fracaso(ErrorRol.NOMBRE_DUPLICADO);
        }
        Rol rol = new Rol(Identificador.nuevo().valor(), nombre, descripcion, false, color);
        return Resultado.exito(roles.save(rol));
    }
}
