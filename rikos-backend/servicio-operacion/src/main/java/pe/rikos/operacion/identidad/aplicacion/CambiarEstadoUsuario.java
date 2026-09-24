package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

/**
 * US-005: un usuario desactivado pierde el acceso (lo verifica
 * AutenticarConContrasena / AutenticarConPin) pero conserva su atribucion
 * en las operaciones pasadas -- nunca se borra (2.5).
 */
@Service
public class CambiarEstadoUsuario {

    private final UsuarioRepositorio usuarios;

    public CambiarEstadoUsuario(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional
    public Resultado<Usuario, ErrorUsuario> ejecutar(String id, boolean activo, String actorId) {
        return usuarios.findById(id)
                .map(usuario -> {
                    if (activo) {
                        usuario.activar(actorId);
                    } else {
                        usuario.desactivar(actorId);
                    }
                    return Resultado.<Usuario, ErrorUsuario>exito(usuario);
                })
                .orElseGet(() -> Resultado.fracaso(ErrorUsuario.NO_ENCONTRADO));
    }
}
