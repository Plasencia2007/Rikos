package pe.rikos.operacion.identidad.infraestructura;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class UsuarioTest {

    private Usuario nuevoUsuario() {
        return new Usuario("id-usuario", "mozo1", "Nombre", "Apellido", null, null, null, "hash", null);
    }

    @Test
    void noEstaBloqueadoRecienCreado() {
        assertFalse(nuevoUsuario().estaBloqueado());
    }

    @Test
    void seBloqueaAlAlcanzarElUmbralDeIntentos() {
        Usuario usuario = nuevoUsuario();
        for (int i = 0; i < 5; i++) {
            usuario.registrarIntentoFallido(5, Duration.ofMinutes(15));
        }
        assertTrue(usuario.estaBloqueado());
    }

    @Test
    void noSeBloqueaAntesDeAlcanzarElUmbral() {
        Usuario usuario = nuevoUsuario();
        for (int i = 0; i < 4; i++) {
            usuario.registrarIntentoFallido(5, Duration.ofMinutes(15));
        }
        assertFalse(usuario.estaBloqueado());
    }

    @Test
    void unIntentoExitosoReiniciaElContadorYElBloqueo() {
        Usuario usuario = nuevoUsuario();
        for (int i = 0; i < 5; i++) {
            usuario.registrarIntentoFallido(5, Duration.ofMinutes(15));
        }
        assertTrue(usuario.estaBloqueado());

        usuario.registrarIntentoExitoso();

        assertFalse(usuario.estaBloqueado());
        assertEquals(0, usuario.getIntentosFallidos());
    }
}
