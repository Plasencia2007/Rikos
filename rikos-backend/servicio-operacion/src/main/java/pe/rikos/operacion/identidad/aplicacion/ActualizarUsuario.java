package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

@Service
public class ActualizarUsuario {

    private final UsuarioRepositorio usuarios;

    public ActualizarUsuario(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional
    public Resultado<Usuario, ErrorUsuario> ejecutar(
            String id, String nombres, String apellidos, String dni, String correo, String telefono,
            String actorId) {

        return usuarios.findById(id)
                .map(usuario -> {
                    usuario.actualizarDatos(nombres, apellidos, dni, correo, telefono, actorId);
                    return Resultado.<Usuario, ErrorUsuario>exito(usuario);
                })
                .orElseGet(() -> Resultado.fracaso(ErrorUsuario.NO_ENCONTRADO));
    }
}
