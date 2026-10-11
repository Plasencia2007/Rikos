package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocal;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.seguridad.LimitadorIntentosPin;

import java.util.List;
import java.util.Optional;

/**
 * US-003: pantalla de bloqueo del POS. El PIN se ingresa sin elegir usuario
 * antes, asi que se compara contra el PIN de cada usuario activo asignado
 * a ese local hasta encontrar coincidencia (ver LimitadorIntentosPin para
 * la nota sobre el bloqueo).
 *
 * Recibe el CODIGO del local (DT-029, p. ej. "sanisidro"), no su ULID: el
 * equipo del POS conoce su propio codigo de despliegue de entrada, no un
 * identificador opaco que tendria que resolver antes de poder autenticar.
 */
@Service
public class AutenticarConPin {

    private final LocalRepositorio locales;
    private final UsuarioLocalRepositorio usuarioLocales;
    private final PasswordEncoder passwordEncoder;
    private final ResolutorSesionUsuario resolutorSesion;
    private final RegistradorSesion registradorSesion;
    private final LimitadorIntentosPin limitador;

    public AutenticarConPin(LocalRepositorio locales, UsuarioLocalRepositorio usuarioLocales,
                             PasswordEncoder passwordEncoder, ResolutorSesionUsuario resolutorSesion,
                             RegistradorSesion registradorSesion, LimitadorIntentosPin limitador) {
        this.locales = locales;
        this.usuarioLocales = usuarioLocales;
        this.passwordEncoder = passwordEncoder;
        this.resolutorSesion = resolutorSesion;
        this.registradorSesion = registradorSesion;
        this.limitador = limitador;
    }

    @Transactional
    public Resultado<SesionIniciada, FalloAutenticacion> ejecutar(
            String localCodigo, String pin, String dispositivo, String direccionIp) {

        Optional<Local> localEncontrado = locales.findByCodigo(localCodigo);
        if (localEncontrado.isEmpty()) {
            return Resultado.fracaso(FalloAutenticacion.de(ErrorAutenticacion.CREDENCIALES_INVALIDAS));
        }
        Local local = localEncontrado.get();
        String localId = local.getId();

        // US-001 CA3: activo = falso impide nuevas operaciones. El PIN es la
        // puerta de entrada de las operaciones de ese local, asi que se
        // corta aca -- sin esto, un local desactivado seguia dejando entrar.
        if (!local.isActivo()) {
            registradorSesion.registrar(null, localId, "pin", "fallido", dispositivo, direccionIp);
            return Resultado.fracaso(FalloAutenticacion.de(ErrorAutenticacion.LOCAL_INACTIVO));
        }

        if (limitador.estaBloqueado(localId, dispositivo)) {
            registradorSesion.registrar(null, localId, "pin", "bloqueado", dispositivo, direccionIp);
            long restantes = limitador.segundosRestantes(localId, dispositivo).orElse(0L);
            return Resultado.fracaso(FalloAutenticacion.bloqueado(restantes));
        }

        List<UsuarioLocal> candidatos = usuarioLocales.findByLocalIdAndUsuario_ActivoTrue(localId);
        Usuario encontrado = null;
        for (UsuarioLocal asignacion : candidatos) {
            Usuario usuario = asignacion.getUsuario();
            if (usuario.getHashPin() != null && passwordEncoder.matches(pin, usuario.getHashPin())) {
                encontrado = usuario;
                break;
            }
        }

        if (encontrado == null) {
            limitador.registrarFallo(localId, dispositivo);
            registradorSesion.registrar(null, localId, "pin", "fallido", dispositivo, direccionIp);
            return Resultado.fracaso(FalloAutenticacion.de(ErrorAutenticacion.CREDENCIALES_INVALIDAS));
        }

        limitador.registrarExito(localId, dispositivo);
        SesionIniciada sesion = resolutorSesion.construir(encontrado, localId);
        registradorSesion.registrar(encontrado.getId(), localId, "pin", "exitoso", dispositivo, direccionIp);

        return Resultado.exito(sesion);
    }
}
