package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;
import pe.rikos.operacion.pensionistas.aplicacion.CambiarParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ClaveParametro;
import pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan;
import pe.rikos.operacion.pensionistas.aplicacion.ReglaSegundoConsumo;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import java.time.Clock;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * 3.4 del sprint 6: el precio de una linea sale SIEMPRE del motor. "Hoy" es el miercoles 11/06/2031.
 * Plan del ejemplo: desayuno 500, almuerzo 800, cena 800. Taper: 100.
 */
@SpringBootTest
@Transactional
class PreciosDeLineaTest extends PedidosBase {

    private static final LocalDate HOY = LocalDate.of(2031, 6, 11);

    @MockitoBean
    Clock reloj;

    @Autowired
    CalculadorDePrecioDeLinea calculador;
    @Autowired
    AbrirPedido abrirPedido;
    @Autowired
    AsignarPensionista asignarPensionista;
    @Autowired
    CuentaPersonaRepositorio personas;
    @Autowired
    CambiarParametro cambiarParametro;
    @Autowired
    AgregarLinea agregarLinea;

    String localA;
    PlanTarifario plan;
    Pedido pedido;
    CuentaPersona persona;

    @BeforeEach
    void preparar() {
        when(reloj.getZone()).thenReturn(ConfiguracionReloj.ZONA_DEL_RESTAURANTE);
        when(reloj.instant()).thenReturn(HOY.atTime(12, 0).atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toInstant());
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = nuevoLocal("pl");
        plan = nuevoPlan(500, 800, 800);
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "100", ADMIN_SEMBRADO_ID);
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, ReglaSegundoConsumo.PRECIO_DEL_PLAN.codigo(),
                ADMIN_SEMBRADO_ID);
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        pedido = abrirPedido.ejecutar(null, Canal.MESA, nuevaMesa("1"), "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        persona = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(0);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private PrecioDeLinea precio(TipoDeLinea tipo, Long normal, boolean paraLlevar) {
        return calculador.calcular(tipo, "almuerzo", normal, persona, paraLlevar, HOY).valorOLanza();
    }

    private static ErrorPedido errorDe(Resultado<?, ErrorPedido> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorPedido>) resultado).error();
    }

    @Test
    void un_cliente_normal_paga_el_precio_normal_del_menu_y_de_la_carta() {
        assertThat(precio(TipoDeLinea.MENU_NORMAL, 1200L, false).precio()).isEqualTo(1200L);
        assertThat(precio(TipoDeLinea.MENU_NORMAL, 1200L, false).tarifaOrigen()).isEqualTo("publico");
        assertThat(precio(TipoDeLinea.PRODUCTO, 500L, false).precio()).isEqualTo(500L);
    }

    @Test
    void un_pensionista_paga_el_precio_de_su_plan_en_el_menu_pero_normal_en_la_carta_y_en_el_extra() {
        Pensionista luis = nuevoPensionista(plan, "Plan");
        persona = asignarPensionista.ejecutar(pedido.getId(), persona.getId(), luis.getId(), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        PrecioDeLinea menu = precio(TipoDeLinea.MENU_NORMAL, 1200L, false);
        assertThat(menu.precio()).isEqualTo(800L);
        assertThat(menu.tarifaOrigen()).isEqualTo("plan");
        assertThat(menu.planId()).isEqualTo(plan.getId());
        assertThat(precio(TipoDeLinea.MENU_EXTRA, 1800L, false).precio()).isEqualTo(1800L);
        assertThat(precio(TipoDeLinea.MENU_EXTRA, 1800L, false).tarifaOrigen()).isEqualTo("extra");
        assertThat(precio(TipoDeLinea.PRODUCTO, 500L, false).precio()).isEqualTo(500L);
    }

    @Test
    void el_taper_va_por_menu_solo_para_llevar_y_no_en_la_carta() {
        assertThat(precio(TipoDeLinea.MENU_NORMAL, 1200L, true).taperUnitario()).isEqualTo(100L);
        assertThat(precio(TipoDeLinea.MENU_EXTRA, 1800L, true).taperUnitario()).isEqualTo(100L);
        assertThat(precio(TipoDeLinea.PRODUCTO, 500L, true).taperUnitario()).isZero();
        assertThat(precio(TipoDeLinea.MENU_NORMAL, 1200L, false).taperUnitario()).isZero();
    }

    @Test
    void sin_precio_normal_no_hay_precio_y_nunca_sale_cero() {
        assertThat(errorDe(calculador.calcular(TipoDeLinea.PRODUCTO, "almuerzo", null, persona, false, HOY)))
                .isEqualTo(ErrorPedido.SIN_PRECIO);
        assertThat(errorDe(calculador.calcular(TipoDeLinea.MENU_NORMAL, "almuerzo", null, persona, false, HOY)))
                .isEqualTo(ErrorPedido.SIN_PRECIO);
    }

    @Test
    void el_desayuno_y_la_cena_del_plan_usan_el_precio_del_plan_de_esa_comida() {
        Pensionista luis = nuevoPensionista(plan, "Plan");
        persona = asignarPensionista.ejecutar(pedido.getId(), persona.getId(), luis.getId(), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(calculador.calcular(TipoDeLinea.PLAN_DESAYUNO, "desayuno", null, persona, false, HOY)
                .valorOLanza().precio()).isEqualTo(500L);
        assertThat(calculador.calcular(TipoDeLinea.PLAN_CENA, "cena", null, persona, false, HOY)
                .valorOLanza().precio()).isEqualTo(800L);
    }

    @Test
    void un_plan_sin_precio_para_esa_comida_avisa_en_vez_de_inventar_uno() {
        PlanTarifario sinDesayuno = crearPlan.ejecutar("Plan sin desayuno " + System.nanoTime(), null,
                new PreciosPlan(null, 800L, 800L), null, ADMIN_SEMBRADO_ID).valorOLanza();
        Pensionista luis = nuevoPensionista(sinDesayuno, "SinDes");
        persona = asignarPensionista.ejecutar(pedido.getId(), persona.getId(), luis.getId(), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(errorDe(calculador.calcular(TipoDeLinea.PLAN_DESAYUNO, "desayuno", null, persona, false, HOY)))
                .isEqualTo(ErrorPedido.PLAN_SIN_PRECIO);
    }

    @Test
    void si_el_precio_del_plan_coincide_con_el_normal_la_linea_sigue_siendo_del_plan() {
        PlanTarifario igual = nuevoPlan(500, 1200, 800);   // el almuerzo del plan cuesta lo mismo que el menu normal
        Pensionista luis = nuevoPensionista(igual, "Igual");
        persona = asignarPensionista.ejecutar(pedido.getId(), persona.getId(), luis.getId(), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        PrecioDeLinea menu = precio(TipoDeLinea.MENU_NORMAL, 1200L, false);

        assertThat(menu.precio()).isEqualTo(1200L);
        assertThat(menu.tarifaOrigen()).isEqualTo("plan");
        assertThat(menu.planId()).isEqualTo(igual.getId());
    }

    @Test
    void el_segundo_desayuno_del_plan_se_cobra_al_plan_aunque_la_regla_diga_precio_normal() {
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, ReglaSegundoConsumo.PRECIO_NORMAL.codigo(),
                ADMIN_SEMBRADO_ID);
        Pensionista luis = nuevoPensionista(plan, "Segundo");
        Pedido desayuno = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "desayuno", ADMIN_SEMBRADO_ID).valorOLanza();
        CuentaPersona enDesayuno = asignarPensionista.ejecutar(desayuno.getId(),
                personas.findByPedidoIdOrderByOrden(desayuno.getId()).get(0).getId(), luis.getId(), ADMIN_SEMBRADO_ID)
                .valorOLanza();
        agregarLinea.ejecutar(new SolicitudDeLinea(null, desayuno.getId(), enDesayuno.getId(),
                TipoDeLinea.PLAN_DESAYUNO, null, 1, null, java.util.List.of(), null, null), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        // No existe un "precio normal" del desayuno del plan: el segundo se cobra igual al plan (y se marca segundo).
        PrecioDeLinea segundo = calculador.calcular(TipoDeLinea.PLAN_DESAYUNO, "desayuno", null, enDesayuno, false, HOY)
                .valorOLanza();

        assertThat(segundo.precio()).isEqualTo(500L);
        assertThat(segundo.tarifaOrigen()).isEqualTo("plan");
        assertThat(segundo.segundoDelDia()).isTrue();
    }
}
