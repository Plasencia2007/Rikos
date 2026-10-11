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
import pe.rikos.operacion.identidad.infraestructura.DiaEspecial;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicioRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocal;
import pe.rikos.operacion.identidad.infraestructura.UsuarioLocalRepositorio;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class EliminarLocalTest {

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
    private EliminarLocal eliminarLocal;
    @Autowired
    private LocalRepositorio locales;
    @Autowired
    private UsuarioRepositorio usuarios;
    @Autowired
    private UsuarioLocalRepositorio usuarioLocales;

    @Test
    void borraUnLocalQueNuncaSeUso() {
        Local local = crearLocal.ejecutar("elim-local-test", "Local de prueba", "Direccion", "01M2QZ606763Q7XC00M8VD7WM1").valorOLanza();

        Resultado<Void, ErrorLocal> resultado = eliminarLocal.ejecutar(local.getId());

        assertThat(resultado.esExito()).isTrue();
        assertThat(locales.findById(local.getId())).isEmpty();
    }

    @Autowired
    private HorarioServicioRepositorio horarios;
    @Autowired
    private DiaEspecialRepositorio diasEspeciales;

    /** El asistente de locales carga horarios y feriados al crear: son configuracion, no historial. */
    @Test
    void borraUnLocalQueSoloTieneHorariosYDiasEspeciales() {
        Local local = crearLocal.ejecutar("elim-local-horarios", "Local de prueba", "Direccion", ADMIN_SEMBRADO_ID).valorOLanza();
        horarios.save(new HorarioServicio(Identificador.nuevo().valor(), local.getId(), "almuerzo", "lunes",
                LocalTime.of(12, 0), LocalTime.of(15, 0)));
        diasEspeciales.save(new DiaEspecial(Identificador.nuevo().valor(), local.getId(), LocalDate.of(2026, 12, 24),
                true, "Nochebuena"));

        Resultado<Void, ErrorLocal> resultado = eliminarLocal.ejecutar(local.getId());

        assertThat(resultado.esExito()).isTrue();
        assertThat(locales.findById(local.getId())).isEmpty();
        assertThat(horarios.findByLocalIdAndActivoTrue(local.getId())).isEmpty();
        assertThat(diasEspeciales.findByLocalIdOrderByFecha(local.getId())).isEmpty();
    }

    @Test
    void rechazaBorrarUnLocalConUsuariosAsignados() {
        Local local = crearLocal.ejecutar("elim-local-hist", "Local de prueba", "Direccion", "01M2QZ606763Q7XC00M8VD7WM1").valorOLanza();
        Usuario usuario = usuarios.save(new Usuario(Identificador.nuevo().valor(), "elimlocaltest", "Nombre",
                "Apellido", null, null, null, "hash-de-prueba", "01M2QZ606763Q7XC00M8VD7WM1"));
        usuarioLocales.save(new UsuarioLocal(usuario, local, true, "01M2QZ606763Q7XC00M8VD7WM1"));

        Resultado<Void, ErrorLocal> resultado = eliminarLocal.ejecutar(local.getId());

        assertThat(resultado.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<Void, ErrorLocal>) resultado).error()).isEqualTo(ErrorLocal.TIENE_HISTORIAL);
        assertThat(locales.findById(local.getId())).isPresent();
    }

    @Test
    void fallaConNoEncontradoSiElLocalNoExiste() {
        Resultado<Void, ErrorLocal> resultado = eliminarLocal.ejecutar("no-existe");

        assertThat(resultado.esFracaso()).isTrue();
        assertThat(((Resultado.Fracaso<Void, ErrorLocal>) resultado).error()).isEqualTo(ErrorLocal.NO_ENCONTRADO);
    }
}
