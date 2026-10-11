package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.infraestructura.Sesion;
import pe.rikos.operacion.identidad.infraestructura.SesionRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sin @Transactional a nivel de clase a proposito: EliminarUsuario intenta
 * el DELETE en su propia transaccion (REQUIRES_NEW) para poder capturar la
 * violacion de FK sin contaminar la del llamador -- eso implica una
 * conexion fisica separada, que solo ve datos ya comprometidos. Si esta
 * clase envolviera todo en una transaccion de test sin commit real, la
 * sub-transaccion nunca veria el usuario de prueba. Cada test comprometeel
 * setup con save() normal (fuera de una transaccion de test) y limpia a
 * mano en @AfterEach en vez de depender de un rollback automatico.
 */
@SpringBootTest
class EliminarUsuarioTest {

    @Autowired
    private EliminarUsuario eliminarUsuario;
    @Autowired
    private UsuarioRepositorio usuarios;
    @Autowired
    private SesionRepositorio sesiones;

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    private String usuarioDePruebaId;

    @BeforeEach
    void autenticar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    @AfterEach
    void limpiar() {
        if (usuarioDePruebaId != null) {
            sesiones.findAll().stream()
                    .filter(s -> usuarioDePruebaId.equals(s.getUsuarioId()))
                    .forEach(sesiones::delete);
            usuarios.findById(usuarioDePruebaId).ifPresent(usuarios::delete);
            usuarioDePruebaId = null;
        }
    }

    private Usuario crear(String nombreUsuario) {
        Usuario usuario = new Usuario(Identificador.nuevo().valor(), nombreUsuario, "Nombre", "Apellido", null,
                null, null, "hash-de-prueba", null);
        usuarios.save(usuario);
        usuarioDePruebaId = usuario.getId();
        return usuario;
    }

    @Test
    void borra_un_usuario_que_nunca_se_uso() {
        Usuario usuario = crear("elimtest1");

        Resultado<Void, ErrorUsuario> resultado = eliminarUsuario.ejecutar(usuario.getId());

        assertThat(resultado.esExito()).isTrue();
        assertThat(usuarios.findById(usuario.getId())).isEmpty();
        usuarioDePruebaId = null; // ya se borro solo, nada que limpiar
    }

    @Test
    void rechaza_borrar_un_usuario_con_sesiones_registradas() {
        Usuario usuario = crear("elimtest2");
        sesiones.save(new Sesion(Identificador.nuevo().valor(), usuario.getId(), null, "contrasena", "exitoso",
                null, "127.0.0.1"));

        Resultado<Void, ErrorUsuario> resultado = eliminarUsuario.ejecutar(usuario.getId());

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorUsuario>) resultado).error())
                .isEqualTo(ErrorUsuario.TIENE_HISTORIAL);
        assertThat(usuarios.findById(usuario.getId())).isPresent();
    }

    @Test
    void falla_con_no_encontrado_si_el_usuario_no_existe() {
        Resultado<Void, ErrorUsuario> resultado = eliminarUsuario.ejecutar("no-existe");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorUsuario>) resultado).error())
                .isEqualTo(ErrorUsuario.NO_ENCONTRADO);
    }
}
