package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;

import java.util.List;

@Service
public class ListarRoles {

    private final RolRepositorio roles;

    public ListarRoles(RolRepositorio roles) {
        this.roles = roles;
    }

    @Transactional(readOnly = true)
    public List<Rol> ejecutar() {
        return roles.findAll();
    }
}
