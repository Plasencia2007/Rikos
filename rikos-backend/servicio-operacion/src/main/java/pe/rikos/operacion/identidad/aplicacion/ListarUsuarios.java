package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

import java.util.List;

@Service
public class ListarUsuarios {

    private final UsuarioRepositorio usuarios;

    public ListarUsuarios(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(readOnly = true)
    public List<Usuario> ejecutar() {
        return usuarios.findAllByOrderByApellidosAscNombresAsc();
    }
}
