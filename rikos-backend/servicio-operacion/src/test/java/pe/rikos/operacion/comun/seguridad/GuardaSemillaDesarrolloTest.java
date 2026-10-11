package pe.rikos.operacion.comun.seguridad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuardaSemillaDesarrolloTest {

    @Mock
    private UsuarioRepositorio usuarios;
    @Mock
    private ApplicationArguments args;

    private Usuario admin() {
        return new Usuario(GuardaSemillaDesarrollo.ID_ADMIN_SEMILLA, "admin", "Administrador", "Sistema",
                null, null, null, "hash-de-desarrollo", null);
    }

    @Test
    void desactivaElUsuarioSemillaSiEstaPresenteYActivo_NoFrenaElArranque() {
        Usuario admin = admin();
        when(usuarios.findById(GuardaSemillaDesarrollo.ID_ADMIN_SEMILLA)).thenReturn(Optional.of(admin));
        GuardaSemillaDesarrollo guarda = new GuardaSemillaDesarrollo(usuarios);

        guarda.run(args); // no debe lanzar nada -- P-29: no puede frenar el arranque

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(guardado.capture());
        assertThat(guardado.getValue().isActivo()).isFalse();
    }

    @Test
    void noTocaNadaSiElUsuarioSemillaYaEstaDesactivado() {
        Usuario admin = admin();
        admin.desactivar(null);
        when(usuarios.findById(GuardaSemillaDesarrollo.ID_ADMIN_SEMILLA)).thenReturn(Optional.of(admin));
        GuardaSemillaDesarrollo guarda = new GuardaSemillaDesarrollo(usuarios);

        guarda.run(args);

        verify(usuarios, never()).save(any());
    }

    @Test
    void noHaceNadaSiElUsuarioSemillaNoExiste() {
        when(usuarios.findById(GuardaSemillaDesarrollo.ID_ADMIN_SEMILLA)).thenReturn(Optional.empty());
        GuardaSemillaDesarrollo guarda = new GuardaSemillaDesarrollo(usuarios);

        guarda.run(args);

        verify(usuarios, never()).save(any());
    }
}
