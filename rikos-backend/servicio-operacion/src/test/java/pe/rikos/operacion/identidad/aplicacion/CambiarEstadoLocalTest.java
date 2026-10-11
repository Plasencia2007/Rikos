package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.Sesion;
import pe.rikos.operacion.identidad.infraestructura.SesionRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocal;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-001 CA3: "activo = falso impide nuevas operaciones y conserva integro
 * el historial." Cubre las dos mitades de la regla -- hasta ahora ninguna
 * tenia prueba automatizada.
 */
@SpringBootTest
@Transactional
class CambiarEstadoLocalTest {

    private static final String PIN = "1234";
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
    private CrearLocal crearLocal;
    @Autowired
    private CambiarEstadoLocal cambiarEstadoLocal;
    @Autowired
    private AutenticarConPin autenticarConPin;
    @Autowired
    private UsuarioRepositorio usuarios;
    @Autowired
    private UsuarioLocalRepositorio usuarioLocales;
    @Autowired
    private SesionRepositorio sesiones;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private Local crear(String codigo) {
        return crearLocal.ejecutar(codigo, "Local de prueba", "Direccion de prueba", "01M2QZ606763Q7XC00M8VD7WM1").valorOLanza();
    }

    @Test
    void desactivarUnLocalConHistorialNoBorraNadaDeEseHistorial() {
        Local local = crear("historial-test");
        Usuario mozo = usuarios.save(new Usuario(Identificador.nuevo().valor(), "mozohistorial", "Nombre",
                "Apellido", null, null, null, "hash-contrasena", "01M2QZ606763Q7XC00M8VD7WM1"));
        usuarioLocales.save(new UsuarioLocal(mozo, local, true, "01M2QZ606763Q7XC00M8VD7WM1"));
        sesiones.save(new Sesion(Identificador.nuevo().valor(), mozo.getId(), local.getId(), "pin", "exitoso",
                "tablet-1", "127.0.0.1"));

        Resultado<Local, ErrorLocal> resultado = cambiarEstadoLocal.ejecutar(local.getId(), false, "01M2QZ606763Q7XC00M8VD7WM1");

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().isActivo()).isFalse();
        assertThat(usuarioLocales.existsByLocalId(local.getId())).isTrue();
        assertThat(sesiones.existsByLocalId(local.getId())).isTrue();
    }

    @Test
    void unLocalDesactivadoImpideNuevasOperaciones_NoDejaEntrarPorPin() {
        Local local = crear("bloqueo-test");
        Usuario mozo = usuarios.save(new Usuario(Identificador.nuevo().valor(), "mozobloqueo", "Nombre",
                "Apellido", null, null, null, "hash-contrasena", "01M2QZ606763Q7XC00M8VD7WM1"));
        mozo.asignarPin(passwordEncoder.encode(PIN), "01M2QZ606763Q7XC00M8VD7WM1");
        usuarios.save(mozo);
        usuarioLocales.save(new UsuarioLocal(mozo, local, true, "01M2QZ606763Q7XC00M8VD7WM1"));

        cambiarEstadoLocal.ejecutar(local.getId(), false, "01M2QZ606763Q7XC00M8VD7WM1");

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                autenticarConPin.ejecutar(local.getCodigo(), PIN, "tablet-1", "127.0.0.1");

        assertThat(resultado.esFracaso()).isTrue();
        FalloAutenticacion fallo = ((Resultado.Fracaso<SesionIniciada, FalloAutenticacion>) resultado).error();
        assertThat(fallo.tipo()).isEqualTo(ErrorAutenticacion.LOCAL_INACTIVO);
    }

    @Test
    void reactivarUnLocalVuelveADejarEntrarPorPin() {
        Local local = crear("reactivar-test");
        Usuario mozo = usuarios.save(new Usuario(Identificador.nuevo().valor(), "mozoreactivar", "Nombre",
                "Apellido", null, null, null, "hash-contrasena", "01M2QZ606763Q7XC00M8VD7WM1"));
        mozo.asignarPin(passwordEncoder.encode(PIN), "01M2QZ606763Q7XC00M8VD7WM1");
        usuarios.save(mozo);
        usuarioLocales.save(new UsuarioLocal(mozo, local, true, "01M2QZ606763Q7XC00M8VD7WM1"));
        cambiarEstadoLocal.ejecutar(local.getId(), false, "01M2QZ606763Q7XC00M8VD7WM1");

        cambiarEstadoLocal.ejecutar(local.getId(), true, "01M2QZ606763Q7XC00M8VD7WM1");

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                autenticarConPin.ejecutar(local.getCodigo(), PIN, "tablet-1", "127.0.0.1");

        assertThat(resultado.esExito()).isTrue();
    }

    @Test
    void fallaConNoEncontradoSiElLocalNoExiste() {
        Resultado<Local, ErrorLocal> resultado = cambiarEstadoLocal.ejecutar("no-existe", false, "01M2QZ606763Q7XC00M8VD7WM1");

        assertThat(resultado.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<Local, ErrorLocal>) resultado).error()).isEqualTo(ErrorLocal.NO_ENCONTRADO);
    }
}
