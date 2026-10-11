package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.infraestructura.Rol;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRol;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EliminarRolTest {

    // usuario_rol.asignado_por es NOT NULL -- se usa el admin real sembrado
    // por V9 (ver CatalogoAccesoPorRolTest), no un actorId de prueba.
    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @BeforeEach
    void autenticar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    @Autowired
    private CrearRol crearRol;
    @Autowired
    private EliminarRol eliminarRol;
    @Autowired
    private RolRepositorio roles;
    @Autowired
    private UsuarioRepositorio usuarios;
    @Autowired
    private UsuarioRolRepositorio usuarioRoles;

    @Test
    void borra_un_rol_personalizado_sin_usuarios_asignados() {
        Rol rol = crearRol.ejecutar("rol elim test 1", null, null).valorOLanza();

        Resultado<Void, ErrorRol> resultado = eliminarRol.ejecutar(rol.getId());

        assertThat(resultado.esExito()).isTrue();
        assertThat(roles.findById(rol.getId())).isEmpty();
    }

    @Test
    void rechaza_borrar_un_rol_de_sistema() {
        Rol rolDeSistema = roles.findByNombre("mozo").orElseThrow();

        Resultado<Void, ErrorRol> resultado = eliminarRol.ejecutar(rolDeSistema.getId());

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorRol>) resultado).error()).isEqualTo(ErrorRol.ES_SISTEMA);
        assertThat(roles.findById(rolDeSistema.getId())).isPresent();
    }

    @Test
    void rechaza_borrar_un_rol_con_usuarios_asignados() {
        Rol rol = crearRol.ejecutar("rol elim test 2", null, null).valorOLanza();
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), "elimroltest", "Nombre",
                "Apellido", null, null, null, "hash-de-prueba", null));
        usuarioRoles.save(new UsuarioRol(usuario, rol, ADMIN_SEMBRADO_ID));

        Resultado<Void, ErrorRol> resultado = eliminarRol.ejecutar(rol.getId());

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorRol>) resultado).error()).isEqualTo(ErrorRol.TIENE_HISTORIAL);
        assertThat(roles.findById(rol.getId())).isPresent();
    }

    @Test
    void falla_con_no_encontrado_si_el_rol_no_existe() {
        Resultado<Void, ErrorRol> resultado = eliminarRol.ejecutar("no-existe");

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorRol>) resultado).error()).isEqualTo(ErrorRol.NO_ENCONTRADO);
    }
}
