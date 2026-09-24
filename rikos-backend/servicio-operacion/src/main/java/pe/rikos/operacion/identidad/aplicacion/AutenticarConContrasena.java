package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.identidad.infraestructura.seguridad.PropiedadesSeguridad;

import java.time.Duration;
import java.util.Optional;

/**
 * US-002. El bloqueo temporal por intentos fallidos (RN-091) se aplica
 * aqui -- ver la nota en LimitadorIntentosPin sobre por que el PIN usa un
 * mecanismo distinto.
 */
@Service
public class AutenticarConContrasena {

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder passwordEncoder;
    private final ResolutorSesionUsuario resolutorSesion;
    private final RegistradorSesion registradorSesion;
    private final PropiedadesSeguridad propiedades;

    public AutenticarConContrasena(UsuarioRepositorio usuarios, PasswordEncoder passwordEncoder,
                                    ResolutorSesionUsuario resolutorSesion, RegistradorSesion registradorSesion,
                                    PropiedadesSeguridad propiedades) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.resolutorSesion = resolutorSesion;
        this.registradorSesion = registradorSesion;
        this.propiedades = propiedades;
    }

    @Transactional
    public Resultado<SesionIniciada, FalloAutenticacion> ejecutar(
            String nombreUsuario, String contrasena, String dispositivo, String direccionIp) {

        Optional<Usuario> encontrado = usuarios.findByNombreUsuario(nombreUsuario);

        if (encontrado.isEmpty()) {
            registradorSesion.registrar(null, null, "contrasena", "fallido", dispositivo, direccionIp);
            return Resultado.fracaso(FalloAutenticacion.de(ErrorAutenticacion.CREDENCIALES_INVALIDAS));
        }

        Usuario usuario = encontrado.get();

        if (!usuario.isActivo()) {
            registradorSesion.registrar(usuario.getId(), null, "contrasena", "fallido", dispositivo, direccionIp);
            return Resultado.fracaso(FalloAutenticacion.de(ErrorAutenticacion.USUARIO_INACTIVO));
        }

        if (usuario.estaBloqueado()) {
            registradorSesion.registrar(usuario.getId(), null, "contrasena", "bloqueado", dispositivo, direccionIp);
            return Resultado.fracaso(FalloAutenticacion.bloqueado(usuario.segundosRestantesBloqueo()));
        }

        if (!passwordEncoder.matches(contrasena, usuario.getHashContrasena())) {
            usuario.registrarIntentoFallido(
                    propiedades.getPin().getUmbralIntentos(),
                    Duration.ofMinutes(propiedades.getPin().getMinutosBloqueo()));
            registradorSesion.registrar(usuario.getId(), null, "contrasena", "fallido", dispositivo, direccionIp);
            return Resultado.fracaso(FalloAutenticacion.de(ErrorAutenticacion.CREDENCIALES_INVALIDAS));
        }

        usuario.registrarIntentoExitoso();
        SesionIniciada sesion = resolutorSesion.construir(usuario, null);
        registradorSesion.registrar(usuario.getId(), sesion.localActivoId(), "contrasena", "exitoso", dispositivo,
                direccionIp);

        return Resultado.exito(sesion);
    }
}
