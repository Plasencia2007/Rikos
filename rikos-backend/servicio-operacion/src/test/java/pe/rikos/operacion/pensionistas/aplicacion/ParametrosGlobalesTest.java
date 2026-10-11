package pe.rikos.operacion.pensionistas.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.Auditoria;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.pensionistas.infraestructura.ParametroGlobal;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sprint 5, seccion 3.6. Estas pruebas cambian el taper y la regla del
 * segundo consumo, pero dentro de una transaccion que se deshace: no tocan
 * los valores reales de la base de desarrollo.
 */
@SpringBootTest
@Transactional
class ParametrosGlobalesTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    private ParametrosGlobales parametros;
    @Autowired
    private CambiarParametro cambiarParametro;
    @Autowired
    private AuditoriaRepositorio auditorias;

    @BeforeEach
    void autenticar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    @Test
    void el_taper_arranca_sin_monto_y_el_segundo_consumo_con_el_precio_del_plan() {
        // Estado de la semilla de V26. Si el dueno ya puso un monto en la base
        // de desarrollo, se limpia primero: lo que se prueba es "sin monto".
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, null, ADMIN_SEMBRADO_ID);
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, "precio_del_plan", ADMIN_SEMBRADO_ID);

        assertThat(parametros.taperPrecio()).isEmpty();
        assertThat(parametros.reglaSegundoConsumo()).isEqualTo(ReglaSegundoConsumo.PRECIO_DEL_PLAN);
    }

    @Test
    void cambia_el_precio_del_taper_y_se_puede_volver_a_dejar_sin_monto() {
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "100", ADMIN_SEMBRADO_ID);
        assertThat(parametros.taperPrecio()).contains(100L);

        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, null, ADMIN_SEMBRADO_ID);
        assertThat(parametros.taperPrecio()).isEmpty();
    }

    @Test
    void cambia_la_regla_del_segundo_consumo() {
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, "precio_normal", ADMIN_SEMBRADO_ID);

        assertThat(parametros.reglaSegundoConsumo()).isEqualTo(ReglaSegundoConsumo.PRECIO_NORMAL);
    }

    @Test
    void rechaza_valores_que_no_corresponden_al_parametro() {
        assertThat(error(cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "abc", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorParametro.VALOR_INVALIDO);
        assertThat(error(cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "0", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorParametro.VALOR_INVALIDO);
        assertThat(error(cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "-50", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorParametro.VALOR_INVALIDO);
        assertThat(error(cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, "gratis", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorParametro.VALOR_INVALIDO);
        // La regla del segundo consumo no admite quedar vacia: siempre hay una.
        assertThat(error(cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorParametro.VALOR_INVALIDO);
    }

    @Test
    void cada_cambio_queda_en_la_bitacora_y_repetir_el_mismo_valor_no() {
        // No depende de lo que tenga la base de desarrollo: se parte de "sin
        // monto" (eso puede escribir una fila) y se cuenta solo lo que sigue.
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, null, ADMIN_SEMBRADO_ID);
        ParametroGlobal parametro = cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "120", ADMIN_SEMBRADO_ID)
                .valorOLanza();
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "120", ADMIN_SEMBRADO_ID);

        List<Auditoria> filas = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("parametro_global",
                parametro.getId());
        assertThat(filas).allMatch(f -> f.getEvento().equals("cambio_parametro"));
        // Un solo cambio a 120, aunque se repitio dos veces.
        assertThat(filas).filteredOn(f -> f.getValorNuevo().contains("\"120\"")).hasSize(1);
        assertThat(parametro.getCambiadoPor()).isEqualTo(ADMIN_SEMBRADO_ID);
    }

    private static ErrorParametro error(Resultado<?, ErrorParametro> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorParametro>) resultado).error();
    }
}
