package pe.rikos.operacion.venta.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.pensionistas.aplicacion.CambiarParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ClaveParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ReglaSegundoConsumo;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Identificar a un pensionista en una persona que YA tiene productos (2026-10-29): antes se rechazaba con
 * PERSONA_CON_LINEAS porque no habia recalculo. Ahora se recalculan sus lineas con las mismas reglas que al pasarlas a
 * un pensionista (US-029 CA5): el menu normal va al precio de su plan; la carta, el menu extra y el extra libre
 * conservan su precio. Si alguna linea no puede ser de un pensionista, no cambia nada.
 */
@SpringBootTest
@Transactional
class RecalculoAlIdentificarPensionistaTest extends ComandasBase {

    private static final long MENU_PUBLICO = 1200L;
    private static final long ALMUERZO_DEL_PLAN = 800L;

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

    @BeforeEach
    void preparar() {
        plan = nuevoPlan(500, ALMUERZO_DEL_PLAN, 900);
        luis = nuevoPensionista(plan, "Identificado");
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, ReglaSegundoConsumo.PRECIO_DEL_PLAN.codigo(),
                ADMIN_SEMBRADO_ID);
        jdbc.update("update producto set precio_venta = ? where tipo_menu = 'normal'", MENU_PUBLICO);
        em.flush();
        em.clear();
    }

    private DetallePedido menuPublico(String personaId, int cantidad) {
        var menu = productos.findByTipoMenu("normal").orElseThrow();
        var guardada = lineas.save(new DetallePedido(Identificador.nuevo().valor(), personaId, "menu_normal", menu.getId(),
                null, cantidad, null, MENU_PUBLICO, 0, "publico", null, false, ADMIN_SEMBRADO_ID));
        // Se escribe ya en la base: algunas pruebas la cambian despues con SQL directo.
        em.flush();
        return guardada;
    }

    private DetallePedido recargada(String id) {
        em.flush();
        em.clear();
        return lineas.findById(id).orElseThrow();
    }

    private CuentaPersona personaRecargada() {
        em.flush();
        em.clear();
        return personas.findById(persona).orElseThrow();
    }

    @Test
    void identificar_a_una_persona_con_menu_lo_pasa_al_precio_de_su_plan() {
        var menu = menuPublico(persona, 1);

        assertThat(asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID).esExito()).isTrue();

        var recalculada = recargada(menu.getId());
        assertThat(recalculada.getPrecioUnitario()).isEqualTo(ALMUERZO_DEL_PLAN);
        assertThat(recalculada.getTarifaOrigen()).isEqualTo("plan");
        assertThat(recalculada.getPlanTarifarioId()).isEqualTo(plan.getId());
        assertThat(personaRecargada().getPensionistaId()).isEqualTo(luis.getId());
        // La bitacora deja rastro de cuanto cambio el total: 12.00 -> 8.00 = -4.00.
        assertThat(auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("pedido", pedido.getId()).get(0)
                .getValorNuevo()).contains("\"ajuste\": -400");
    }

    @Test
    void la_carta_conserva_su_precio_al_identificar_al_pensionista() {
        var chicha = linea("Chicha", 1);

        asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(recargada(chicha.getId()).getPrecioUnitario()).isEqualTo(chicha.getPrecioUnitario());
        assertThat(personaRecargada().getPensionistaId()).isEqualTo(luis.getId());
    }

    @Test
    void un_menu_de_varias_unidades_impide_identificar_y_no_cambia_nada() {
        var menu = menuPublico(persona, 2);

        assertThat(error(asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.CANTIDAD_INVALIDA);
        assertThat(personaRecargada().getPensionistaId()).isNull();
        assertThat(recargada(menu.getId()).getPrecioUnitario()).isEqualTo(MENU_PUBLICO);
    }

    @Test
    void si_su_plan_no_tiene_precio_no_se_identifica_ni_cambia_ningun_precio() {
        var menu = menuPublico(persona, 1);
        jdbc.update("update plan_tarifario_version set precio_almuerzo = null where plan_id = ?", plan.getId());
        em.flush();
        em.clear();

        assertThat(asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID).esExito()).isFalse();
        assertThat(personaRecargada().getPensionistaId()).isNull();
        assertThat(recargada(menu.getId()).getPrecioUnitario()).isEqualTo(MENU_PUBLICO);
    }

    @Test
    void una_persona_con_algo_ya_cobrado_no_cambia_de_tipo() {
        var menu = menuPublico(persona, 1);
        jdbc.update("update detalle_pedido set estado = 'cobrado' where id = ?", menu.getId());
        em.flush();
        em.clear();

        assertThat(error(asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.LINEA_COBRADA);
        assertThat(personaRecargada().getPensionistaId()).isNull();
    }

    @Test
    void una_linea_anulada_no_impide_identificar_ni_se_recalcula() {
        var menu = menuPublico(persona, 2);
        var motivo = motivoDeAnulacion("Se equivoco de mesa", false);
        em.flush();
        jdbc.update("update detalle_pedido set estado = 'anulado', anulado_en = now(), anulado_por = ?, "
                + "motivo_anulacion_id = ? where id = ?", ADMIN_SEMBRADO_ID, motivo.getId(), menu.getId());
        em.flush();
        em.clear();

        asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(recargada(menu.getId()).getPrecioUnitario()).isEqualTo(MENU_PUBLICO);
    }
}
