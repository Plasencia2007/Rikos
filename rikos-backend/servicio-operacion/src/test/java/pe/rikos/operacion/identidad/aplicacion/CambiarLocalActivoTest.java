package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.RolRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocal;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRol;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRolRepositorio;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hallazgo de revision: cambiar de local en el sidebar no reemitia el
 * token, asi que el local auditado quedaba desactualizado. Este caso de
 * uso es el arreglo -- cubre el camino feliz y el intento de "colarse" a
 * un local que no es propio.
 */
@SpringBootTest
@Transactional
class CambiarLocalActivoTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    private CambiarLocalActivo cambiarLocalActivo;
    @Autowired
    private CrearLocal crearLocal;
    @Autowired
    private UsuarioRepositorio usuarios;
    @Autowired
    private UsuarioLocalRepositorio usuarioLocales;
    @Autowired
    private UsuarioRolRepositorio usuarioRoles;
    @Autowired
    private RolRepositorio roles;

    private Usuario usuarioConAccesoA(Local... localesAsignados) {
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), "cambialocal-" + Identificador.nuevo().valor().substring(0, 8).toLowerCase(),
                "Nombre", "Apellido", null, null, null, "hash-de-prueba", ADMIN_SEMBRADO_ID));
        for (Local local : localesAsignados) {
            usuarioLocales.save(new UsuarioLocal(usuario, local, false, ADMIN_SEMBRADO_ID));
        }
        return usuario;
    }

    @Test
    void reemiteElTokenConElNuevoLocalSiElUsuarioTieneAccesoAEl() {
        Local localA = crearLocal.ejecutar("cl-a-" + Identificador.nuevo().valor().substring(0, 6).toLowerCase(), "Local A",
                "Direccion", ADMIN_SEMBRADO_ID).valorOLanza();
        Local localB = crearLocal.ejecutar("cl-b-" + Identificador.nuevo().valor().substring(0, 6).toLowerCase(), "Local B",
                "Direccion", ADMIN_SEMBRADO_ID).valorOLanza();
        Usuario usuario = usuarioConAccesoA(localA, localB);

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                cambiarLocalActivo.ejecutar(usuario.getId(), localB.getId());

        assertThat(resultado.esExito()).isTrue();
        assertThat(resultado.valorOLanza().localActivoId()).isEqualTo(localB.getId());
    }

    @Test
    void rechazaCambiarAUnLocalQueElUsuarioNoTieneAsignado() {
        Local localA = crearLocal.ejecutar("cl-c-" + Identificador.nuevo().valor().substring(0, 6).toLowerCase(), "Local C",
                "Direccion", ADMIN_SEMBRADO_ID).valorOLanza();
        Local localAjeno = crearLocal.ejecutar("cl-d-" + Identificador.nuevo().valor().substring(0, 6).toLowerCase(), "Local D",
                "Direccion", ADMIN_SEMBRADO_ID).valorOLanza();
        Usuario usuario = usuarioConAccesoA(localA);

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                cambiarLocalActivo.ejecutar(usuario.getId(), localAjeno.getId());

        assertThat(resultado.esFracaso()).isTrue();
        FalloAutenticacion fallo = ((Resultado.Fracaso<SesionIniciada, FalloAutenticacion>) resultado).error();
        assertThat(fallo.tipo()).isEqualTo(ErrorAutenticacion.LOCAL_NO_ASIGNADO);
    }

    @Test
    void elAdministradorCentralPuedeCambiarACualquierLocalActivoAunSinTenerloAsignado() {
        Local propio = crearLocal.ejecutar("cl-e-" + Identificador.nuevo().valor().substring(0, 6).toLowerCase(),
                "Local E", "Direccion", ADMIN_SEMBRADO_ID).valorOLanza();
        Local ajeno = crearLocal.ejecutar("cl-f-" + Identificador.nuevo().valor().substring(0, 6).toLowerCase(),
                "Local F", "Direccion", ADMIN_SEMBRADO_ID).valorOLanza();
        Usuario central = usuarioConAccesoA(propio);
        usuarioRoles.save(new UsuarioRol(central, roles.findByNombre("administrador central").orElseThrow(),
                ADMIN_SEMBRADO_ID));

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                cambiarLocalActivo.ejecutar(central.getId(), ajeno.getId());

        assertThat(resultado.esExito()).isTrue();
        SesionIniciada sesion = resultado.valorOLanza();
        assertThat(sesion.localActivoId()).isEqualTo(ajeno.getId());
        assertThat(sesion.locales()).extracting(LocalResumen::id).contains(propio.getId(), ajeno.getId());
    }

    @Test
    void ElAdministradorCentralNoPuedeCambiarAUnLocalDesactivado() {
        Local propio = crearLocal.ejecutar("cl-g-" + Identificador.nuevo().valor().substring(0, 6).toLowerCase(),
                "Local G", "Direccion", ADMIN_SEMBRADO_ID).valorOLanza();
        Local cerrado = crearLocal.ejecutar("cl-h-" + Identificador.nuevo().valor().substring(0, 6).toLowerCase(),
                "Local H", "Direccion", ADMIN_SEMBRADO_ID).valorOLanza();
        cerrado.desactivar(ADMIN_SEMBRADO_ID);
        Usuario central = usuarioConAccesoA(propio);
        usuarioRoles.save(new UsuarioRol(central, roles.findByNombre("administrador central").orElseThrow(),
                ADMIN_SEMBRADO_ID));

        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                cambiarLocalActivo.ejecutar(central.getId(), cerrado.getId());

        assertThat(resultado.esFracaso()).isTrue();
        FalloAutenticacion fallo = ((Resultado.Fracaso<SesionIniciada, FalloAutenticacion>) resultado).error();
        assertThat(fallo.tipo()).isEqualTo(ErrorAutenticacion.LOCAL_NO_ASIGNADO);
    }

    @Test
    void fallaConUsuarioInactivoSiElUsuarioNoExisteOEstaDesactivado() {
        Resultado<SesionIniciada, FalloAutenticacion> resultado =
                cambiarLocalActivo.ejecutar("no-existe", "cualquiera");

        assertThat(resultado.esFracaso()).isTrue();
        FalloAutenticacion fallo = ((Resultado.Fracaso<SesionIniciada, FalloAutenticacion>) resultado).error();
        assertThat(fallo.tipo()).isEqualTo(ErrorAutenticacion.USUARIO_INACTIVO);
    }
}
