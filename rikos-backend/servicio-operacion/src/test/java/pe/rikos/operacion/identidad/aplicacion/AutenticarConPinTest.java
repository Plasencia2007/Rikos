package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Empresa;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocal;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.seguridad.LimitadorIntentosPin;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutenticarConPinTest {

    private static final String PIN_CORRECTO = "1234";

    @Mock
    private LocalRepositorio locales;
    @Mock
    private UsuarioLocalRepositorio usuarioLocales;
    @Mock
    private ResolutorSesionUsuario resolutorSesion;
    @Mock
    private RegistradorSesion registradorSesion;
    @Mock
    private LimitadorIntentosPin limitador;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AutenticarConPin caso;

    @BeforeEach
    void preparar() {
        caso = new AutenticarConPin(locales, usuarioLocales, passwordEncoder, resolutorSesion, registradorSesion,
                limitador);
    }

    private Local localActivo() {
        Empresa empresa = new Empresa("empresa-1", "20123456789", "RIKO'S S.A.C.", "RIKO'S", "Av. Siempre Viva 100");
        return new Local("local-1", empresa, "sanisidro", "San Isidro", "Av. Siempre Viva 123", "actor-1");
    }

    private UsuarioLocal asignacionCon(Usuario usuario, Local local) {
        return new UsuarioLocal(usuario, local, true, "actor-1");
    }

    private Usuario mozoConPin() {
        Usuario usuario = new Usuario("usuario-1", "mozo1", "Nombre", "Apellido", null, null, null,
                "hash-contrasena", "actor-1");
        usuario.asignarPin(passwordEncoder.encode(PIN_CORRECTO), "actor-1");
        return usuario;
    }

    private FalloAutenticacion errorDe(Resultado<SesionIniciada, FalloAutenticacion> resultado) {
        return ((Resultado.Fracaso<SesionIniciada, FalloAutenticacion>) resultado).error();
    }

    @Test
    void pinCorrectoEnLocalActivoDevuelveExito() {
        Local local = localActivo();
        Usuario usuario = mozoConPin();
        when(locales.findByCodigo("sanisidro")).thenReturn(Optional.of(local));
        when(limitador.estaBloqueado("local-1", "dispositivo-1")).thenReturn(false);
        when(usuarioLocales.findByLocalIdAndUsuario_ActivoTrue("local-1"))
                .thenReturn(List.of(asignacionCon(usuario, local)));
        SesionIniciada sesionEsperada = new SesionIniciada("token", "usuario-1", "mozo1", List.of("mozo"), List.of(),
                "local-1", List.of("venta.crear"));
        when(resolutorSesion.construir(usuario, "local-1")).thenReturn(sesionEsperada);

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                caso.ejecutar("sanisidro", PIN_CORRECTO, "dispositivo-1", "127.0.0.1");

        assertTrue(resultado.esExito());
        assertEquals(sesionEsperada, resultado.valorOLanza());
        verify(limitador).registrarExito("local-1", "dispositivo-1");
    }

    @Test
    void localDesactivadoRechazaElPinSinLlegarAProbarUsuarios() {
        Local local = localActivo();
        local.desactivar("actor-1");
        when(locales.findByCodigo("sanisidro")).thenReturn(Optional.of(local));

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                caso.ejecutar("sanisidro", PIN_CORRECTO, "dispositivo-1", "127.0.0.1");

        assertTrue(resultado.esFracaso());
        assertEquals(ErrorAutenticacion.LOCAL_INACTIVO, errorDe(resultado).tipo());
        verify(limitador, never()).estaBloqueado(any(), any());
        verify(usuarioLocales, never()).findByLocalIdAndUsuario_ActivoTrue(any());
        verify(registradorSesion).registrar(null, "local-1", "pin", "fallido", "dispositivo-1", "127.0.0.1");
    }

    @Test
    void codigoDeLocalInexistenteNoRevelaSiExiste() {
        when(locales.findByCodigo("noexiste")).thenReturn(Optional.empty());

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                caso.ejecutar("noexiste", PIN_CORRECTO, "dispositivo-1", "127.0.0.1");

        assertTrue(resultado.esFracaso());
        assertEquals(ErrorAutenticacion.CREDENCIALES_INVALIDAS, errorDe(resultado).tipo());
    }

    @Test
    void dispositivoBloqueadoRechazaAunConElPinCorrecto() {
        Local local = localActivo();
        when(locales.findByCodigo("sanisidro")).thenReturn(Optional.of(local));
        when(limitador.estaBloqueado("local-1", "dispositivo-1")).thenReturn(true);
        when(limitador.segundosRestantes("local-1", "dispositivo-1")).thenReturn(Optional.of(300L));

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                caso.ejecutar("sanisidro", PIN_CORRECTO, "dispositivo-1", "127.0.0.1");

        assertTrue(resultado.esFracaso());
        FalloAutenticacion fallo = errorDe(resultado);
        assertEquals(ErrorAutenticacion.BLOQUEADO, fallo.tipo());
        assertEquals(300L, fallo.segundosRestantes());
        verify(usuarioLocales, never()).findByLocalIdAndUsuario_ActivoTrue(any());
    }

    @Test
    void pinIncorrectoRegistraFalloEnElLimitadorPorDispositivo() {
        Local local = localActivo();
        Usuario usuario = mozoConPin();
        when(locales.findByCodigo("sanisidro")).thenReturn(Optional.of(local));
        when(limitador.estaBloqueado("local-1", "dispositivo-1")).thenReturn(false);
        when(usuarioLocales.findByLocalIdAndUsuario_ActivoTrue("local-1"))
                .thenReturn(List.of(asignacionCon(usuario, local)));

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                caso.ejecutar("sanisidro", "0000", "dispositivo-1", "127.0.0.1");

        assertTrue(resultado.esFracaso());
        assertEquals(ErrorAutenticacion.CREDENCIALES_INVALIDAS, errorDe(resultado).tipo());
        verify(limitador).registrarFallo("local-1", "dispositivo-1");
    }
}
