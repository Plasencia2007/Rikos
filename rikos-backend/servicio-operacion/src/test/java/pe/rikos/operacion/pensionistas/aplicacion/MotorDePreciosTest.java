package pe.rikos.operacion.pensionistas.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Sprint 5, seccion 3.5: la tabla de casos del motor de precios. Precios en
 * centimos. Plan: desayuno 500, almuerzo 800, cena 800. Menu normal 1200,
 * menu extra 1800, huevo frito (agregado) 300, taper 100.
 *
 * "Hoy" es el miercoles 11/06/2031 (reloj fijo). La regla del segundo
 * consumo y el taper se fijan en cada prueba: no dependen de lo que tenga la
 * base de desarrollo.
 */
@SpringBootTest
@Transactional
class MotorDePreciosTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final LocalDate HOY = LocalDate.of(2031, 6, 11);
    private static final LocalDate MANANA = HOY.plusDays(1);

    @MockitoBean
    private Clock reloj;

    @Autowired
    private MotorDePrecios motor;
    @Autowired
    private CrearPlanTarifario crearPlan;
    @Autowired
    private CambiarPreciosPlan cambiarPrecios;
    @Autowired
    private CambiarParametro cambiarParametro;

    private PlanTarifario plan;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        when(reloj.getZone()).thenReturn(ConfiguracionReloj.ZONA_DEL_RESTAURANTE);
        when(reloj.instant()).thenReturn(HOY.atTime(12, 0).atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toInstant());
        plan = crearPlan.ejecutar("Plan motor " + Identificador.nuevo().valor().substring(16), null,
                new PreciosPlan(500L, 800L, 800L), null, ADMIN_SEMBRADO_ID).valorOLanza();
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "100", ADMIN_SEMBRADO_ID);
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, "precio_del_plan", ADMIN_SEMBRADO_ID);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private PrecioCalculado calcular(TipoLinea tipo, Comida comida, Long precioNormal, String planId,
                                     boolean segundo, boolean paraLlevar, LocalDate fecha) {
        return motor.calcular(new SolicitudDePrecio(tipo, comida, precioNormal, planId, segundo, paraLlevar, fecha))
                .valorOLanza();
    }

    private PrecioCalculado almuerzo(String planId, boolean segundo, boolean paraLlevar) {
        return calcular(TipoLinea.MENU_DEL_DIA, Comida.ALMUERZO, 1200L, planId, segundo, paraLlevar, null);
    }

    // ---- Menu del dia ----------------------------------------------------------

    @Test
    void cliente_normal_paga_el_precio_normal_del_menu() {
        PrecioCalculado precio = almuerzo(null, false, false);

        assertThat(precio.precio()).isEqualTo(1200L);
        assertThat(precio.taper()).isNull();
        assertThat(precio.avisos()).isEmpty();
        assertThat(precio.explicacion()).isNotEmpty();
    }

    @Test
    void pensionista_con_ticket_paga_el_precio_de_su_plan_para_esa_comida() {
        assertThat(almuerzo(plan.getId(), false, false).precio()).isEqualTo(800L);
        assertThat(calcular(TipoLinea.MENU_DEL_DIA, Comida.DESAYUNO, null, plan.getId(), false, false, null).precio())
                .isEqualTo(500L);
        assertThat(calcular(TipoLinea.MENU_DEL_DIA, Comida.CENA, null, plan.getId(), false, false, null).precio())
                .isEqualTo(800L);
    }

    @Test
    void la_explicacion_dice_que_se_aplico_el_plan() {
        assertThat(String.join(" ", almuerzo(plan.getId(), false, false).explicacion())).containsIgnoringCase("plan");
    }

    // ---- Menu extra (RN-023) ---------------------------------------------------

    @Test
    void el_menu_extra_se_cobra_completo_tambien_al_pensionista() {
        PrecioCalculado conTicket = calcular(TipoLinea.MENU_EXTRA, Comida.ALMUERZO, 1800L, plan.getId(), false, false, null);
        PrecioCalculado sinTicket = calcular(TipoLinea.MENU_EXTRA, Comida.ALMUERZO, 1800L, null, false, false, null);

        assertThat(conTicket.precio()).isEqualTo(1800L);
        assertThat(sinTicket.precio()).isEqualTo(1800L);
        assertThat(String.join(" ", conTicket.explicacion())).containsIgnoringCase("extra");
    }

    // ---- Segundo consumo del dia ----------------------------------------------

    @Test
    void el_segundo_almuerzo_va_con_el_precio_del_plan_por_defecto() {
        assertThat(almuerzo(plan.getId(), true, false).precio()).isEqualTo(800L);
    }

    @Test
    void con_la_configuracion_en_precio_normal_el_segundo_almuerzo_del_pensionista_va_a_precio_normal() {
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, "precio_normal", ADMIN_SEMBRADO_ID);

        PrecioCalculado segundo = almuerzo(plan.getId(), true, false);

        assertThat(segundo.precio()).isEqualTo(1200L);
        assertThat(String.join(" ", segundo.explicacion())).containsIgnoringCase("segundo");
        // El primero del dia sigue con el plan.
        assertThat(almuerzo(plan.getId(), false, false).precio()).isEqualTo(800L);
    }

    @Test
    void la_regla_del_segundo_consumo_no_toca_al_cliente_normal() {
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, "precio_normal", ADMIN_SEMBRADO_ID);

        assertThat(almuerzo(null, true, false).precio()).isEqualTo(1200L);
    }

    // ---- Agregados --------------------------------------------------------------

    @Test
    void un_agregado_se_cobra_a_precio_normal_tambien_al_pensionista() {
        PrecioCalculado conTicket = calcular(TipoLinea.AGREGADO, Comida.ALMUERZO, 300L, plan.getId(), false, false, null);

        assertThat(conTicket.precio()).isEqualTo(300L);
        assertThat(String.join(" ", conTicket.explicacion())).containsIgnoringCase("agregado");
    }

    @Test
    void el_agregado_del_segundo_almuerzo_tampoco_tiene_tarifa() {
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, "precio_del_plan", ADMIN_SEMBRADO_ID);

        assertThat(calcular(TipoLinea.AGREGADO, Comida.ALMUERZO, 300L, plan.getId(), true, false, null).precio())
                .isEqualTo(300L);
    }

    // ---- Taper ----------------------------------------------------------------

    @Test
    void para_llevar_suma_el_taper_por_menu_al_cliente_normal_y_al_pensionista() {
        PrecioCalculado normal = almuerzo(null, false, true);
        PrecioCalculado pensionista = almuerzo(plan.getId(), false, true);

        assertThat(normal.precio()).isEqualTo(1200L);
        assertThat(normal.taper()).isEqualTo(100L);
        assertThat(normal.total()).isEqualTo(1300L);
        assertThat(pensionista.precio()).isEqualTo(800L);
        assertThat(pensionista.taper()).isEqualTo(100L);
        assertThat(pensionista.total()).isEqualTo(900L);
    }

    @Test
    void el_menu_extra_para_llevar_tambien_lleva_taper_pero_el_agregado_no() {
        PrecioCalculado extra = calcular(TipoLinea.MENU_EXTRA, Comida.ALMUERZO, 1800L, null, false, true, null);
        PrecioCalculado agregado = calcular(TipoLinea.AGREGADO, Comida.ALMUERZO, 300L, null, false, true, null);

        assertThat(extra.taper()).isEqualTo(100L);
        assertThat(agregado.taper()).isNull();
        assertThat(agregado.avisos()).isEmpty();
    }

    @Test
    void en_el_local_no_hay_taper() {
        assertThat(almuerzo(null, false, false).taper()).isNull();
    }

    @Test
    void sin_monto_de_taper_no_se_inventa_uno_y_se_avisa() {
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, null, ADMIN_SEMBRADO_ID);

        PrecioCalculado precio = almuerzo(plan.getId(), false, true);

        assertThat(precio.precio()).isEqualTo(800L);
        assertThat(precio.taper()).isNull();
        assertThat(precio.avisos()).containsExactly(AvisoPrecio.TAPER_SIN_MONTO);
        assertThat(precio.total()).isEqualTo(800L);
    }

    // ---- Sin precio: nunca se inventa ------------------------------------------

    @Test
    void si_el_plan_no_tiene_precio_para_esa_comida_no_se_cobra_otro_en_silencio() {
        PlanTarifario soloAlmuerzo = crearPlan.ejecutar("Plan parcial " + Identificador.nuevo().valor().substring(16),
                null, new PreciosPlan(null, 800L, null), null, ADMIN_SEMBRADO_ID).valorOLanza();

        PrecioCalculado precio = calcular(TipoLinea.MENU_DEL_DIA, Comida.DESAYUNO, 500L, soloAlmuerzo.getId(), false,
                false, null);

        assertThat(precio.precio()).isNull();
        assertThat(precio.total()).isNull();
        assertThat(precio.avisos()).containsExactly(AvisoPrecio.PLAN_SIN_PRECIO);
    }

    @Test
    void si_el_producto_no_tiene_precio_normal_se_avisa() {
        PrecioCalculado precio = calcular(TipoLinea.MENU_DEL_DIA, Comida.ALMUERZO, null, null, false, false, null);

        assertThat(precio.precio()).isNull();
        assertThat(precio.avisos()).containsExactly(AvisoPrecio.SIN_PRECIO_NORMAL);
    }

    @Test
    void un_plan_inexistente_es_un_error_no_un_precio() {
        Resultado<PrecioCalculado, ErrorPrecio> resultado = motor.calcular(new SolicitudDePrecio(
                TipoLinea.MENU_DEL_DIA, Comida.ALMUERZO, 1200L, "no-existe", false, false, null));

        assertThat(resultado.esExito()).isFalse();
        assertThat(((Resultado.Fracaso<?, ErrorPrecio>) resultado).error()).isEqualTo(ErrorPrecio.PLAN_NO_ENCONTRADO);
    }

    // ---- Cambio de plan (CA3) -------------------------------------------------

    @Test
    void el_precio_depende_de_la_fecha_del_consumo_asi_un_cambio_no_toca_lo_anterior() {
        cambiarPrecios.ejecutar(plan.getId(), new PreciosPlan(500L, 900L, 800L), MANANA, ADMIN_SEMBRADO_ID);

        assertThat(calcular(TipoLinea.MENU_DEL_DIA, Comida.ALMUERZO, 1200L, plan.getId(), false, false, HOY).precio())
                .isEqualTo(800L);
        assertThat(calcular(TipoLinea.MENU_DEL_DIA, Comida.ALMUERZO, 1200L, plan.getId(), false, false, MANANA).precio())
                .isEqualTo(900L);
        // Sin fecha: hoy.
        assertThat(almuerzo(plan.getId(), false, false).precio()).isEqualTo(800L);
    }

    @Test
    void los_avisos_y_la_explicacion_vienen_siempre_en_listas_no_nulas() {
        PrecioCalculado precio = almuerzo(plan.getId(), false, true);

        assertThat(precio.avisos()).isEqualTo(List.of());
        assertThat(precio.explicacion()).allMatch(linea -> !linea.isBlank());
    }
}
