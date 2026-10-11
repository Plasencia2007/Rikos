package pe.rikos.operacion.venta.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.pensionistas.aplicacion.CambiarParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ClaveParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ReglaSegundoConsumo;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-029 CA5: al pasar una linea a una persona de OTRO tipo, el precio se recalcula con el motor. Solo depende de la
 * persona el menu normal y la comida del plan; la carta, el menu extra y el extra libre conservan su precio.
 */
@SpringBootTest
@Transactional
class RecalculoAlTrasladarTest extends ComandasBase {

    private static final long MENU_PUBLICO = 1200L;
    private static final long ALMUERZO_DEL_PLAN = 800L;

    @Autowired
    TrasladarLineas trasladar;
    @Autowired
    AsignarPensionista asignarPensionista;
    @Autowired
    CambiarParametro cambiarParametro;
    @Autowired
    DetallePedidoRepositorio lineas;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManager em;

    PlanTarifario plan;
    Pensionista luis;
    String pensionistaPersona;

    @BeforeEach
    void preparar() {
        plan = nuevoPlan(500, ALMUERZO_DEL_PLAN, 900);
        luis = nuevoPensionista(plan, "Recalculo");
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "100", ADMIN_SEMBRADO_ID);
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, ReglaSegundoConsumo.PRECIO_DEL_PLAN.codigo(),
                ADMIN_SEMBRADO_ID);
        jdbc.update("update producto set precio_venta = ? where tipo_menu = 'normal'", MENU_PUBLICO);
        em.flush();
        em.clear();
        agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID);
        pensionistaPersona = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(1).getId();
        asignarPensionista.ejecutar(pedido.getId(), pensionistaPersona, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
    }

    /** Un menu normal de un cliente normal, tal como lo dejaria AgregarLinea (sin armar el menu del dia). */
    private DetallePedido menuPublico(String personaId) {
        var menu = productos.findByTipoMenu("normal").orElseThrow();
        return lineas.save(new DetallePedido(Identificador.nuevo().valor(), personaId, "menu_normal", menu.getId(),
                null, 1, null, MENU_PUBLICO, 0, "publico", null, false, ADMIN_SEMBRADO_ID));
    }

    private DetallePedido recargada(String id) {
        em.flush();
        em.clear();
        return lineas.findById(id).orElseThrow();
    }

    @Test
    void un_menu_de_cliente_normal_pasa_a_un_pensionista_al_precio_de_su_plan() {
        var menu = menuPublico(persona);

        trasladar.ejecutar(pedido.getId(), List.of(menu.getId()), pensionistaPersona, ADMIN_SEMBRADO_ID).valorOLanza();

        var movida = recargada(menu.getId());
        assertThat(movida.getPrecioUnitario()).isEqualTo(ALMUERZO_DEL_PLAN);
        assertThat(movida.getTarifaOrigen()).isEqualTo("plan");
        assertThat(movida.getPlanTarifarioId()).isEqualTo(plan.getId());
        assertThat(movida.isSegundoDelDia()).isFalse();
        // La bitacora deja rastro de cuanto cambio el total por el recalculo: 12.00 -> 8.00 = -4.00.
        assertThat(auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("pedido", pedido.getId()).get(0)
                .getValorNuevo()).contains("\"ajuste\": -400");
    }

    @Test
    void un_menu_de_pensionista_vuelve_al_precio_publico_al_pasar_a_un_cliente_normal() {
        var menu = menuPublico(persona);
        trasladar.ejecutar(pedido.getId(), List.of(menu.getId()), pensionistaPersona, ADMIN_SEMBRADO_ID).valorOLanza();

        trasladar.ejecutar(pedido.getId(), List.of(menu.getId()), persona, ADMIN_SEMBRADO_ID).valorOLanza();

        var devuelta = recargada(menu.getId());
        assertThat(devuelta.getPrecioUnitario()).isEqualTo(MENU_PUBLICO);
        assertThat(devuelta.getTarifaOrigen()).isEqualTo("publico");
        assertThat(devuelta.getPlanTarifarioId()).isNull();
        assertThat(devuelta.isSegundoDelDia()).isFalse();
    }

    @Test
    void el_segundo_menu_del_dia_de_un_pensionista_queda_marcado_como_segundo() {
        var primero = menuPublico(persona);
        var segundo = menuPublico(persona);
        trasladar.ejecutar(pedido.getId(), List.of(primero.getId()), pensionistaPersona, ADMIN_SEMBRADO_ID).valorOLanza();

        trasladar.ejecutar(pedido.getId(), List.of(segundo.getId()), pensionistaPersona, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(recargada(primero.getId()).isSegundoDelDia()).isFalse();
        assertThat(recargada(segundo.getId()).isSegundoDelDia()).isTrue();
    }

    @Test
    void un_pensionista_no_recibe_un_menu_de_varias_unidades() {
        var menu = lineas.save(new DetallePedido(Identificador.nuevo().valor(), persona, "menu_normal",
                productos.findByTipoMenu("normal").orElseThrow().getId(), null, 2, null, MENU_PUBLICO, 0, "publico",
                null, false, ADMIN_SEMBRADO_ID));

        assertThat(error(trasladar.ejecutar(pedido.getId(), List.of(menu.getId()), pensionistaPersona,
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.CANTIDAD_INVALIDA);
        assertThat(recargada(menu.getId()).getCuentaPersonaId()).isEqualTo(persona);
    }

    @Test
    void la_comida_del_plan_no_pasa_a_un_cliente_normal_pero_si_a_otro_pensionista() {
        // Un pedido de desayuno para que la comida del plan sea valida.
        var desayuno = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "desayuno", ADMIN_SEMBRADO_ID).valorOLanza();
        String normal = personas.findByPedidoIdOrderByOrden(desayuno.getId()).get(0).getId();
        agregarPersona.ejecutar(desayuno.getId(), null, null, ADMIN_SEMBRADO_ID);
        String conPlan = personas.findByPedidoIdOrderByOrden(desayuno.getId()).get(1).getId();
        asignarPensionista.ejecutar(desayuno.getId(), conPlan, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        agregarPersona.ejecutar(desayuno.getId(), null, null, ADMIN_SEMBRADO_ID);
        String otroConPlan = personas.findByPedidoIdOrderByOrden(desayuno.getId()).get(2).getId();
        var otroPlan = nuevoPlan(700, 1000, 1000);
        var maria = nuevoPensionista(otroPlan, "Otra");
        asignarPensionista.ejecutar(desayuno.getId(), otroConPlan, maria.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        var comida = agregarLinea.ejecutar(new SolicitudDeLinea(null, desayuno.getId(), conPlan,
                TipoDeLinea.PLAN_DESAYUNO, null, 1, null, List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(trasladar.ejecutar(desayuno.getId(), List.of(comida.getId()), normal, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.SOLO_PARA_PENSIONISTA);

        trasladar.ejecutar(desayuno.getId(), List.of(comida.getId()), otroConPlan, ADMIN_SEMBRADO_ID).valorOLanza();
        var movida = recargada(comida.getId());
        assertThat(movida.getPrecioUnitario()).isEqualTo(700L);
        assertThat(movida.getPlanTarifarioId()).isEqualTo(otroPlan.getId());
    }

    @Test
    void la_carta_y_el_extra_libre_conservan_su_precio_aunque_cambie_el_tipo_de_persona() {
        var chicha = linea("Chicha", 1);
        var extra = agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona,
                TipoDeLinea.EXTRA_LIBRE, null, 1, null, List.of(), "Huevo", 300L), ADMIN_SEMBRADO_ID).valorOLanza();
        // El precio de la carta cambia despues de agregarla: el snapshot no se toca.
        jdbc.update("update producto set precio_venta = 9999 where id = ?", chicha.getProductoId());

        trasladar.ejecutar(pedido.getId(), List.of(chicha.getId(), extra.getId()), pensionistaPersona,
                ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(recargada(chicha.getId()).getPrecioUnitario()).isEqualTo(500L);
        assertThat(recargada(extra.getId()).getPrecioUnitario()).isEqualTo(300L);
        assertThat(recargada(extra.getId()).getTarifaOrigen()).isEqualTo("manual");
    }

    @Test
    void si_no_se_puede_recalcular_no_se_mueve_ninguna_linea() {
        var menu = menuPublico(persona);
        var carta = linea("Chicha", 1);
        // El plan del pensionista pierde el precio del almuerzo: el motor no puede cobrar su menu.
        jdbc.update("update plan_tarifario_version set precio_almuerzo = null where plan_id = ?", plan.getId());
        em.flush();
        em.clear();

        var resultado = trasladar.ejecutar(pedido.getId(), List.of(carta.getId(), menu.getId()), pensionistaPersona,
                ADMIN_SEMBRADO_ID);

        assertThat(resultado.esExito()).isFalse();
        assertThat(recargada(carta.getId()).getCuentaPersonaId()).isEqualTo(persona);
        assertThat(recargada(menu.getId()).getCuentaPersonaId()).isEqualTo(persona);
    }

    @Test
    void el_taper_de_un_menu_para_llevar_se_recalcula_con_el_menu() {
        var llevar = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        String normal = personas.findByPedidoIdOrderByOrden(llevar.getId()).get(0).getId();
        agregarPersona.ejecutar(llevar.getId(), null, null, ADMIN_SEMBRADO_ID);
        String conPlan = personas.findByPedidoIdOrderByOrden(llevar.getId()).get(1).getId();
        asignarPensionista.ejecutar(llevar.getId(), conPlan, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        var menu = menuPublico(normal);

        trasladar.ejecutar(llevar.getId(), List.of(menu.getId()), conPlan, ADMIN_SEMBRADO_ID).valorOLanza();

        var movida = recargada(menu.getId());
        assertThat(movida.getPrecioUnitario()).isEqualTo(ALMUERZO_DEL_PLAN);
        assertThat(movida.getTaperUnitario()).isEqualTo(100L);
    }

    @Test
    void entre_clientes_normales_el_menu_conserva_el_precio_con_el_que_se_agrego() {
        var menu = menuPublico(persona);
        // El duenio sube el menu despues: la linea ya agregada no cambia al dividir la cuenta.
        jdbc.update("update producto set precio_venta = 1500 where tipo_menu = 'normal'");
        em.flush();
        em.clear();

        trasladar.ejecutar(pedido.getId(), List.of(menu.getId()), null, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(recargada(menu.getId()).getPrecioUnitario()).isEqualTo(MENU_PUBLICO);
    }

    @Test
    void entre_clientes_normales_no_hace_falta_que_exista_el_precio_del_menu() {
        var menu = menuPublico(persona);
        jdbc.update("update producto set precio_venta = null where tipo_menu = 'normal'");
        em.flush();
        em.clear();

        assertThat(trasladar.ejecutar(pedido.getId(), List.of(menu.getId()), null, ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }

    @Test
    void al_pasar_dos_menus_juntos_a_un_pensionista_el_segundo_queda_marcado() {
        var uno = menuPublico(persona);
        var dos = menuPublico(persona);
        // Los menus no pueden ser mas de uno por persona a la vez en el pedido real, pero el traslado los cuenta juntos.
        trasladar.ejecutar(pedido.getId(), List.of(uno.getId(), dos.getId()), pensionistaPersona, ADMIN_SEMBRADO_ID)
                .valorOLanza();

        long marcados = List.of(recargada(uno.getId()), recargada(dos.getId())).stream()
                .filter(DetallePedido::isSegundoDelDia).count();
        assertThat(marcados).isEqualTo(1);
    }
}
