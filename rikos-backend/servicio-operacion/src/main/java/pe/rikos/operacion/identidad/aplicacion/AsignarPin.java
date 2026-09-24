package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

/**
 * US-003: el PIN es una credencial distinta de la contrasena general
 * (3.7). Lo asigna un administrador; el propio mozo no se autoregistra.
 */
@Service
public class AsignarPin {

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder passwordEncoder;

    public AsignarPin(UsuarioRepositorio usuarios, PasswordEncoder passwordEncoder) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Resultado<Usuario, ErrorUsuario> ejecutar(String id, String pin, String actorId) {
        return usuarios.findById(id)
                .map(usuario -> {
                    usuario.asignarPin(passwordEncoder.encode(pin), actorId);
                    return Resultado.<Usuario, ErrorUsuario>exito(usuario);
                })
                .orElseGet(() -> Resultado.fracaso(ErrorUsuario.NO_ENCONTRADO));
    }
}
