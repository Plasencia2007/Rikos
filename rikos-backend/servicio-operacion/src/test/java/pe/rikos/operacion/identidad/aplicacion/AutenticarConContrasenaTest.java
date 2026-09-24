package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.identidad.infraestructura.seguridad.PropiedadesSeguridad;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticarConContrasenaTest {

    private static final String CONTRASENA_CORRECTA = "correcta123";

    @Mock
    private UsuarioRepositorio usuarios;
    @Mock
    private ResolutorSesionUsuario resolutorSesion;
    @Mock
    private RegistradorSesion registradorSesion;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private PropiedadesSeguridad propiedades;
    private AutenticarConContrasena caso;

    @BeforeEach
    void preparar() {
        propiedades = new PropiedadesSeguridad();
        propiedades.getPin().setUmbralIntentos(5);
        propiedades.getPin().setMinutosBloqueo(15);
        caso = new AutenticarConContrasena(usuarios, passwordEncoder, resolutorSesion, registradorSesion, propiedades);
    }

    private Usuario usuarioActivo() {
        return new Usuario("id-1", "mozo1", "Nombre", "Apellido", null, null, null,
                passwordEncoder.encode(CONTRASENA_CORRECTA), null);
    }

    private FalloAutenticacion errorDe(Resultado<SesionIniciada, FalloAutenticacion> resultado) {
        return ((Resultado.Fracaso<SesionIniciada, FalloAutenticacion>) resultado).error();
    }

    @Test
    void loginValidoDevuelveExitoYRegistraSesionExitosa() {
        Usuario usuario = usuarioActivo();
        when(usuarios.findByNombreUsuario("mozo1")).thenReturn(Optional.of(usuario));
        SesionIniciada sesionEsperada = new SesionIniciada("token", "id-1", "mozo1", List.of("mozo"), List.of(),
                "local-1", List.of("venta.crear"));
        when(resolutorSesion.construir(usuario, null)).thenReturn(sesionEsperada);

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                caso.ejecutar("mozo1", CONTRASENA_CORRECTA, "dispositivo", "127.0.0.1");

        assertTrue(resultado.esExito());
        assertEquals(sesionEsperada, resultado.valorOLanza());
        verify(registradorSesion).registrar("id-1", "local-1", "contrasena", "exitoso", "dispositivo", "127.0.0.1");
    }

    @Test
    void usuarioInactivoNoPuedeEntrar() {
        Usuario usuario = usuarioActivo();
        usuario.desactivar("otro-actor");
        when(usuarios.findByNombreUsuario("mozo1")).thenReturn(Optional.of(usuario));

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                caso.ejecutar("mozo1", CONTRASENA_CORRECTA, "dispositivo", "127.0.0.1");

        assertTrue(resultado.esFracaso());
        assertEquals(ErrorAutenticacion.USUARIO_INACTIVO, errorDe(resultado).tipo());
        verify(resolutorSesion, never()).construir(any(), any());
    }

    @Test
    void seBloqueaTrasCincoIntentosFallidosYaNiConLaContrasenaCorrectaEntraYDiceCuantoFalta() {
        Usuario usuario = usuarioActivo();
        when(usuarios.findByNombreUsuario("mozo1")).thenReturn(Optional.of(usuario));

        for (int i = 0; i < 5; i++) {
            Resultado<SesionIniciada, FalloAutenticacion> intento =
                    caso.ejecutar("mozo1", "contrasena-incorrecta", "dispositivo", "127.0.0.1");
            assertEquals(ErrorAutenticacion.CREDENCIALES_INVALIDAS, errorDe(intento).tipo());
        }

        Resultado<SesionIniciada, FalloAutenticacion> sextoIntento =
                caso.ejecutar("mozo1", CONTRASENA_CORRECTA, "dispositivo", "127.0.0.1");

        assertTrue(sextoIntento.esFracaso());
        FalloAutenticacion fallo = errorDe(sextoIntento);
        assertEquals(ErrorAutenticacion.BLOQUEADO, fallo.tipo());
        assertNotNull(fallo.segundosRestantes());
        assertTrue(fallo.segundosRestantes() > 0);
        assertTrue(usuario.estaBloqueado());
    }

    @Test
    void unNombreDeUsuarioInexistenteNoRevelaSiExisteYQuedaRegistrado() {
        when(usuarios.findByNombreUsuario("fantasma")).thenReturn(Optional.empty());

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                caso.ejecutar("fantasma", "cualquiera", "dispositivo", "127.0.0.1");

        assertTrue(resultado.esFracaso());
        assertEquals(ErrorAutenticacion.CREDENCIALES_INVALIDAS, errorDe(resultado).tipo());
        verify(registradorSesion).registrar(null, null, "contrasena", "fallido", "dispositivo", "127.0.0.1");
    }
}
