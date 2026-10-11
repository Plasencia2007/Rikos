package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.infraestructura.Local;

import static org.assertj.core.api.Assertions.assertThat;

/** Paso 4 del asistente de locales: color principal y portada se guardan y se leen de vuelta. */
@SpringBootTest
@Transactional
class ActualizarLocalTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    private CrearLocal crearLocal;
    @Autowired
    private ActualizarLocal actualizarLocal;

    @BeforeEach
    void autenticar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private Local.DatosActualizacion datos(String color, String portada) {
        return new Local.DatosActualizacion("Chosica", "Av. Lima 123", "+51987654321", "chosica@rikos.pe",
                null, null, null, null, null, null, color, portada, true, true, 30, 45);
    }

    @Test
    void guardaColorPrincipalYPortada() {
        Local local = crearLocal.ejecutar("chosica-" + Identificador.nuevo().valor().substring(20).toLowerCase(),
                "Chosica", "Av. Lima 123", ADMIN_SEMBRADO_ID).valorOLanza();

        Local actualizado = actualizarLocal.ejecutar(local.getId(), datos("#2B7A68", "https://cdn.rikos.pe/p.jpg"),
                ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(actualizado.getColorPrincipal()).isEqualTo("#2B7A68");
        assertThat(actualizado.getPortadaUrl()).isEqualTo("https://cdn.rikos.pe/p.jpg");
    }

    @Test
    void colorYPortadaSonOpcionales() {
        Local local = crearLocal.ejecutar("chosica-" + Identificador.nuevo().valor().substring(20).toLowerCase(),
                "Chosica", "Av. Lima 123", ADMIN_SEMBRADO_ID).valorOLanza();

        Local actualizado = actualizarLocal.ejecutar(local.getId(), datos(null, null), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(actualizado.getColorPrincipal()).isNull();
        assertThat(actualizado.getPortadaUrl()).isNull();
    }
}
