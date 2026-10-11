package pe.rikos.operacion.venta.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.AgregarOpcionMenu;
import pe.rikos.operacion.catalogo.aplicacion.CambiarPrecioMenu;
import pe.rikos.operacion.catalogo.aplicacion.ConfirmarSemanaMenu;
import pe.rikos.operacion.catalogo.aplicacion.CrearSemanaMenu;
import pe.rikos.operacion.catalogo.aplicacion.MarcarAgotado;
import pe.rikos.operacion.catalogo.infraestructura.OpcionMenu;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.SemanaMenu;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;
import pe.rikos.operacion.pensionistas.aplicacion.CambiarParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ClaveParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ReglaSegundoConsumo;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * US-028: leer el pedido (totales derivados por persona), corregir lineas pendientes y ver la oferta del dia.
 * "Hoy" es el miercoles 11/06/2031 con el menu de esa semana confirmado. Menu normal 1200, plan almuerzo 800,
 * taper 100.
 */
@SpringBootTest
@Transactional
class ConsultaDelPedidoTest extends PedidosBase {

    private static final LocalDate HOY = LocalDate.of(2031, 6, 11);
    private static final LocalDate LUNES = LocalDate.of(2031, 6, 9);

    @MockitoBean
    Clock reloj;

    @Autowired
    CrearSemanaMenu crearSemana;
    @Autowired
    AgregarOpcionMenu agregarOpcion;
    @Autowired
    ConfirmarSemanaMenu confirmarSemana;
    @Autowired
    CambiarPrecioMenu cambiarPrecioMenu;
    @Autowired
    CambiarParametro cambiarParametro;
    @Autowired
    AbrirPedido abrirPedido;
    @Autowired
    AgregarPersona agregarPersona;
    @Autowired
    AsignarPensionista asignarPensionista;
    @Autowired
    AgregarLinea agregarLinea;
    @Autowired
    ModificarLinea modificarLinea;
    @Autowired
    QuitarLinea quitarLinea;
    @Autowired
    ConsultaPedidos consulta;
    @Autowired
    MarcarAgotado marcarAgotado;
    @Autowired
    CuentaPersonaRepositorio personas;
    @Autowired
    DetallePedidoRepositorio lineas;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManager em;

    String localA;
    String localB;
    PlanTarifario plan;
    Pedido pedido;
    String persona1;
    OpcionMenu opcionSeco;
    Producto chicha;

    @BeforeEach
    void preparar() {
        when(reloj.getZone()).thenReturn(ConfiguracionReloj.ZONA_DEL_RESTAURANTE);
        when(reloj.instant()).thenReturn(HOY.atTime(12, 0).atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toInstant());
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = nuevoLocal("cp");
        localB = nuevoLocal("cq");
        plan = nuevoPlan(500, 800, 800);
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "100", ADMIN_SEMBRADO_ID);
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, ReglaSegundoConsumo.PRECIO_DEL_PLAN.codigo(),
                ADMIN_SEMBRADO_ID);
        cambiarPrecioMenu.ejecutar("normal", 1200L, ADMIN_SEMBRADO_ID).valorOLanza();
        cambiarPrecioMenu.ejecutar("extra", 1800L, ADMIN_SEMBRADO_ID).valorOLanza();
        SemanaMenu semana = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza();
        Producto entrada = plato("Sopa de casa", CATEGORIA_ENTRADA);
        Producto seco = plato("Seco de res", CATEGORIA_SEGUNDO);
        Producto estofado = plato("Estofado de res", CATEGORIA_SEGUNDO);
        Producto lomo = plato("Lomo saltado", CATEGORIA_SEGUNDO);
        agregarOpcion.ejecutar(semana.getId(), HOY, entrada.getId(), "normal", null).valorOLanza();
        opcionSeco = agregarOpcion.ejecutar(semana.getId(), HOY, seco.getId(), "normal", null).valorOLanza();
        agregarOpcion.ejecutar(semana.getId(), HOY, estofado.getId(), "normal", null).valorOLanza();
        agregarOpcion.ejecutar(semana.getId(), HOY, lomo.getId(), "extra", null).valorOLanza();
        confirmarSemana.ejecutar(semana.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        chicha = productoDeCarta("Chicha", 500L);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        pedido = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        persona1 = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(0).getId();
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    static ErrorPedido error(Resultado<?, ErrorPedido> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorPedido>) resultado).error();
    }

    private DetallePedido chicha(int cantidad) {
        return agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona1, TipoDeLinea.PRODUCTO,
                chicha.getId(), cantidad, null, List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();
    }

    @Test
    void el_total_reparte_por_persona_suma_el_taper_y_no_cuenta_lo_quitado() {
        Pensionista luis = nuevoPensionista(plan, "Total");
        String persona2 = agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        asignarPensionista.ejecutar(pedido.getId(), persona2, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        chicha(2);                                                                     // persona 1: 2 x 500 = 1000
        agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona2, TipoDeLinea.MENU_NORMAL, null, 1,
                null, List.of(new ComponenteElegido("segundo", opcionSeco.getId())), null, null), ADMIN_SEMBRADO_ID)
                .valorOLanza();                                                        // persona 2: plan 800 + taper 100
        DetallePedido quitada = agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona1,
                TipoDeLinea.EXTRA_LIBRE, null, 1, null, List.of(), "Se equivoco", 999L), ADMIN_SEMBRADO_ID).valorOLanza();
        quitarLinea.ejecutar(pedido.getId(), quitada.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        PedidoDetallado detalle = consulta.pedido(pedido.getId()).valorOLanza();

        assertThat(detalle.personas()).hasSize(2);
        PersonaDetallada uno = detalle.personas().get(0);
        PersonaDetallada dos = detalle.personas().get(1);
        assertThat(uno.subtotal()).isEqualTo(1000L);
        assertThat(uno.taper()).isZero();
        assertThat(uno.total()).isEqualTo(1000L);
        assertThat(dos.subtotal()).isEqualTo(800L);
        assertThat(dos.taper()).isEqualTo(100L);
        assertThat(dos.total()).isEqualTo(900L);
        assertThat(detalle.subtotal()).isEqualTo(1800L);
        assertThat(detalle.taper()).isEqualTo(100L);
        assertThat(detalle.total()).isEqualTo(1900L);
        // El menu trae sus componentes con el nombre del producto elegido.
        assertThat(dos.lineas().get(0).componentes()).extracting(ComponenteDetallado::componente)
                .containsExactlyInAnyOrder("entrada", "segundo");
        assertThat(uno.lineas().get(0).productoNombre()).isEqualTo(chicha.getNombre());
    }

    @Test
    void una_linea_pendiente_se_modifica_y_se_quita() {
        DetallePedido linea = chicha(1);

        DetallePedido cambiada = modificarLinea.ejecutar(pedido.getId(), linea.getId(), 3, " Sin hielo ",
                ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(cambiada.getCantidad()).isEqualTo(3);
        assertThat(cambiada.getObservacion()).isEqualTo("Sin hielo");
        assertThat(error(modificarLinea.ejecutar(pedido.getId(), linea.getId(), 0, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.CANTIDAD_INVALIDA);
        assertThat(error(modificarLinea.ejecutar(pedido.getId(), linea.getId(), 1, "x".repeat(251),
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.OBSERVACION_LARGA);

        assertThat(quitarLinea.ejecutar(pedido.getId(), linea.getId(), ADMIN_SEMBRADO_ID).esExito()).isTrue();
        assertThat(lineas.findById(linea.getId())).isEmpty();
        assertThat(error(quitarLinea.ejecutar(pedido.getId(), linea.getId(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.LINEA_NO_ENCONTRADA);
    }

    @Test
    void una_linea_ya_comandada_no_se_modifica_ni_se_quita() {
        DetallePedido linea = chicha(1);
        em.flush();
        jdbc.update("update detalle_pedido set estado = 'comandado' where id = ?", linea.getId());
        em.clear();

        assertThat(error(modificarLinea.ejecutar(pedido.getId(), linea.getId(), 2, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.LINEA_YA_COMANDADA);
        assertThat(error(quitarLinea.ejecutar(pedido.getId(), linea.getId(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.LINEA_YA_COMANDADA);
        assertThat(lineas.findById(linea.getId())).isPresent();
    }

    @Test
    void una_linea_o_un_pedido_de_otro_local_no_se_lee_ni_se_toca() {
        DetallePedido linea = chicha(1);
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);

        assertThat(error(consulta.pedido(pedido.getId()))).isEqualTo(ErrorPedido.NO_ENCONTRADO);
        assertThat(error(modificarLinea.ejecutar(pedido.getId(), linea.getId(), 2, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.NO_ENCONTRADO);
        assertThat(error(quitarLinea.ejecutar(pedido.getId(), linea.getId(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.NO_ENCONTRADO);
        assertThat(error(consulta.oferta(pedido.getId()))).isEqualTo(ErrorPedido.NO_ENCONTRADO);
    }

    @Test
    void la_oferta_trae_el_menu_de_hoy_por_componente_la_carta_y_esconde_los_agotados_de_este_local() {
        Producto agotada = productoDeCarta("Gaseosa agotada", 400L);
        marcarAgotado.ejecutar(agotada.getId()).valorOLanza();

        OfertaDelDia oferta = consulta.oferta(pedido.getId()).valorOLanza();

        assertThat(oferta.servicio()).isEqualTo("almuerzo");
        assertThat(oferta.hayMenu()).isTrue();
        assertThat(oferta.precioMenuNormal()).isEqualTo(1200L);
        assertThat(oferta.precioMenuExtra()).isEqualTo(1800L);
        assertThat(oferta.normal()).extracting(ComponenteOfrecido::componente).containsExactly("entrada", "segundo");
        assertThat(oferta.normal().get(1).opciones()).hasSize(2);
        assertThat(oferta.extra()).extracting(ComponenteOfrecido::componente).contains("segundo");
        assertThat(oferta.carta()).extracting(ProductoOfrecido::id).contains(chicha.getId())
                .doesNotContain(agotada.getId());
        assertThat(oferta.carta()).filteredOn(p -> p.id().equals(chicha.getId())).singleElement()
                .satisfies(p -> assertThat(p.precio()).isEqualTo(500L));
    }

    @Test
    void la_oferta_de_un_pedido_que_no_es_de_almuerzo_no_trae_menu() {
        Pedido desayuno = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "desayuno", ADMIN_SEMBRADO_ID)
                .valorOLanza();

        OfertaDelDia oferta = consulta.oferta(desayuno.getId()).valorOLanza();

        assertThat(oferta.hayMenu()).isFalse();
        assertThat(oferta.normal()).isEmpty();
        assertThat(oferta.carta()).isNotEmpty();
    }

    @Test
    void el_pedido_abierto_de_una_mesa_se_encuentra_por_la_mesa_y_uno_de_otro_local_no() {
        String mesa = nuevaMesa("7");
        Pedido enMesa = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(consulta.abiertoDeMesa(mesa).valorOLanza().pedido().getId()).isEqualTo(enMesa.getId());
        assertThat(error(consulta.abiertoDeMesa(nuevaMesa("8")))).isEqualTo(ErrorPedido.NO_ENCONTRADO);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        assertThat(error(consulta.abiertoDeMesa(mesa))).isEqualTo(ErrorPedido.NO_ENCONTRADO);
    }

    @Test
    void un_pedido_para_llevar_avisa_cuando_el_taper_no_tiene_monto() {
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, null, ADMIN_SEMBRADO_ID);
        agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona1, TipoDeLinea.MENU_NORMAL, null, 1, null,
                List.of(new ComponenteElegido("segundo", opcionSeco.getId())), null, null), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        PedidoDetallado sinMonto = consulta.pedido(pedido.getId()).valorOLanza();
        assertThat(sinMonto.taper()).isZero();
        assertThat(sinMonto.avisos()).containsExactly("TAPER_SIN_MONTO");

        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "100", ADMIN_SEMBRADO_ID);
        assertThat(consulta.pedido(pedido.getId()).valorOLanza().avisos()).isEmpty();
    }

    @Test
    void un_pedido_de_mesa_o_sin_menus_no_avisa_del_taper() {
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, null, ADMIN_SEMBRADO_ID);
        chicha(1);   // solo carta: no lleva taper aunque sea para llevar

        assertThat(consulta.pedido(pedido.getId()).valorOLanza().avisos()).isEmpty();
    }
}
