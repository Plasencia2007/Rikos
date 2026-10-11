package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Pantalla de usuarios: "actividad reciente" mezcla accesos y lo que la persona hizo (auditoria). */
@SpringBootTest
@Transactional
class ObtenerActividadUsuarioTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    private ObtenerActividadUsuario obtenerActividad;
    @Autowired
    private CrearLocal crearLocal;
    @Autowired
    private EliminarLocal eliminarLocal;

    @BeforeEach
    void autenticar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    @Test
    void incluyeLoQueLaPersonaHizoConLoMasRecientePrimero() {
        var local = crearLocal.ejecutar("actividad-test", "Local de actividad", "Direccion", ADMIN_SEMBRADO_ID)
                .valorOLanza();
        eliminarLocal.ejecutar(local.getId());

        List<ObtenerActividadUsuario.Actividad> actividad = obtenerActividad.ejecutar(ADMIN_SEMBRADO_ID);

        assertThat(actividad).isNotEmpty();
        assertThat(actividad.get(0).tipo()).isEqualTo("auditoria");
        assertThat(actividad.get(0).evento()).isEqualTo("eliminacion");
        assertThat(actividad.get(0).entidad()).isEqualTo("local");
        assertThat(actividad).extracting(ObtenerActividadUsuario.Actividad::ocurridoEn)
                .isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    @Test
    void noPasaDeCincoEntradas() {
        for (int i = 0; i < 7; i++) {
            var local = crearLocal.ejecutar("actividad-tope-" + i, "Local " + i, "Direccion", ADMIN_SEMBRADO_ID)
                    .valorOLanza();
            eliminarLocal.ejecutar(local.getId());
        }

        assertThat(obtenerActividad.ejecutar(ADMIN_SEMBRADO_ID)).hasSize(5);
    }

    @Test
    void unaPersonaSinActividadDevuelveListaVacia() {
        assertThat(obtenerActividad.ejecutar("01M2QZ60NOEXISTE0000000000")).isEmpty();
    }
}
