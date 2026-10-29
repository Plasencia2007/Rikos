package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.AgregarOpcionMenu;
import pe.rikos.operacion.catalogo.aplicacion.CambiarPrecioMenu;
import pe.rikos.operacion.catalogo.aplicacion.ConfirmarSemanaMenu;
import pe.rikos.operacion.catalogo.aplicacion.CrearSemanaMenu;
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
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponente;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoComponenteRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

/**
 * US-028 CA3 y RN-038: el menu del dia en el pedido. "Hoy" es el miercoles 11/06/2031, con el menu de esa
 * semana confirmado: una entrada, dos segundos normales y un segundo extra. Menu normal 1200, extra 1800,
 * plan almuerzo 800, taper 100.
 */
@SpringBootTest
@Transactional
class MenuDelDiaEnPedidoTest extends PedidosBase {

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
    AsignarPensionista asignarPensionista;
    @Autowired
    AgregarLinea agregarLinea;
    @Autowired
    CuentaPersonaRepositorio personas;
    @Autowired
    DetallePedidoComponenteRepositorio componentes;
    @Autowired
    pe.rikos.operacion.catalogo.aplicacion.MarcarAgotado marcarAgotado;
    @Autowired
    ConsultaPedidos consulta;
    @Autowired
    ComandarPedido comandarPedido;
    @Autowired
    ConsultaComandas consultaComandas;

    PlanTarifario plan;
    Pedido pedido;
    String persona;
    Producto entrada;
    Producto seco;
    Producto estofado;
    Producto lomo;
    OpcionMenu opcionEntrada;
    OpcionMenu opcionSeco;
    OpcionMenu opcionEstofado;
    OpcionMenu opcionLomo;

    @BeforeEach
    void preparar() {
        when(reloj.getZone()).thenReturn(ConfiguracionReloj.ZONA_DEL_RESTAURANTE);
        when(reloj.instant()).thenReturn(HOY.atTime(12, 0).atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toInstant());
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        String local = nuevoLocal("md");
        plan = nuevoPlan(500, 800, 800);
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "100", ADMIN_SEMBRADO_ID);
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, ReglaSegundoConsumo.PRECIO_DEL_PLAN.codigo(),
                ADMIN_SEMBRADO_ID);
        cambiarPrecioMenu.ejecutar("normal", 1200L, ADMIN_SEMBRADO_ID).valorOLanza();
        cambiarPrecioMenu.ejecutar("extra", 1800L, ADMIN_SEMBRADO_ID).valorOLanza();

        SemanaMenu semana = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza();
        entrada = plato("Sopa de casa", CATEGORIA_ENTRADA);
        seco = plato("Seco de res", CATEGORIA_SEGUNDO);
        estofado = plato("Estofado de res", CATEGORIA_SEGUNDO);
        lomo = plato("Lomo saltado", CATEGORIA_SEGUNDO);
        opcionEntrada = agregarOpcion.ejecutar(semana.getId(), HOY, entrada.getId(), "normal", null).valorOLanza();
        opcionSeco = agregarOpcion.ejecutar(semana.getId(), HOY, seco.getId(), "normal", null).valorOLanza();
        opcionEstofado = agregarOpcion.ejecutar(semana.getId(), HOY, estofado.getId(), "normal", null).valorOLanza();
        opcionLomo = agregarOpcion.ejecutar(semana.getId(), HOY, lomo.getId(), "extra", null).valorOLanza();
        confirmarSemana.ejecutar(semana.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, local);
        pedido = abrirPedido.ejecutar(null, Canal.MESA, nuevaMesa("1"), "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        persona = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(0).getId();
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    static ErrorPedido error(Resultado<?, ErrorPedido> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorPedido>) resultado).error();
    }

    SolicitudDeLinea menu(TipoDeLinea tipo, List<ComponenteElegido> elegidos) {
        return new SolicitudDeLinea(null, pedido.getId(), persona, tipo, null, 1, null, elegidos, null, null);
    }

    @Test
    void el_menu_normal_asigna_solo_el_componente_con_una_opcion_y_pide_elegir_el_que_tiene_varias() {
        assertThat(error(agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL, List.of()), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.COMPONENTE_OBLIGATORIO);

        DetallePedido linea = agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL,
                List.of(new ComponenteElegido("segundo", opcionSeco.getId()))), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(linea.getPrecioUnitario()).isEqualTo(1200L);
        assertThat(linea.getTipoLinea()).isEqualTo("menu_normal");
        // La fila del componente con una sola opcion se guarda igual (RN-038).
        assertThat(componentes.findByDetallePedidoIdIn(List.of(linea.getId())))
                .extracting(DetallePedidoComponente::getComponente, DetallePedidoComponente::getProductoId)
                .containsExactlyInAnyOrder(tuple("entrada", entrada.getId()), tuple("segundo", seco.getId()));
    }

    @Test
    void elegir_una_opcion_que_no_es_de_hoy_ni_de_ese_componente_se_rechaza() {
        assertThat(error(agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL,
                List.of(new ComponenteElegido("segundo", opcionEntrada.getId()))), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.OPCION_INVALIDA);
        assertThat(error(agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL,
                List.of(new ComponenteElegido("segundo", "no-existe"))), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.OPCION_INVALIDA);
        // El segundo "extra" no es una opcion del menu normal.
        assertThat(error(agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL,
                List.of(new ComponenteElegido("segundo", opcionLomo.getId()))), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.OPCION_INVALIDA);
    }

    @Test
    void el_menu_extra_toma_su_segundo_de_las_opciones_extra_y_se_cobra_completo_incluso_al_pensionista() {
        Pensionista luis = nuevoPensionista(plan, "Extra");
        asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        DetallePedido linea = agregarLinea.ejecutar(menu(TipoDeLinea.MENU_EXTRA, List.of()), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(linea.getPrecioUnitario()).isEqualTo(1800L);
        assertThat(linea.getTarifaOrigen()).isEqualTo("extra");
        assertThat(componentes.findByDetallePedidoIdIn(List.of(linea.getId())))
                .extracting(DetallePedidoComponente::getProductoId).contains(lomo.getId());
    }

    @Test
    void al_pensionista_el_menu_normal_le_sale_al_precio_de_su_plan_y_para_llevar_lleva_taper() {
        Pensionista luis = nuevoPensionista(plan, "Menu");
        asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        Pedido paraLlevar = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID)
                .valorOLanza();
        String personaLlevar = personas.findByPedidoIdOrderByOrden(paraLlevar.getId()).get(0).getId();

        DetallePedido enMesa = agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL,
                List.of(new ComponenteElegido("segundo", opcionSeco.getId()))), ADMIN_SEMBRADO_ID).valorOLanza();
        DetallePedido llevar = agregarLinea.ejecutar(new SolicitudDeLinea(null, paraLlevar.getId(), personaLlevar,
                TipoDeLinea.MENU_NORMAL, null, 2, null, List.of(new ComponenteElegido("segundo", opcionSeco.getId())),
                null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(enMesa.getPrecioUnitario()).isEqualTo(800L);
        assertThat(enMesa.getTarifaOrigen()).isEqualTo("plan");
        assertThat(enMesa.getTaperUnitario()).isZero();
        assertThat(llevar.getPrecioUnitario()).isEqualTo(1200L);   // cliente normal
        assertThat(llevar.getTaperUnitario()).isEqualTo(100L);     // por menu, para llevar
        assertThat(llevar.getCantidad()).isEqualTo(2);
    }

    @Test
    void un_pensionista_no_pide_dos_menus_en_una_sola_linea() {
        Pensionista luis = nuevoPensionista(plan, "Cantidad");
        asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona,
                TipoDeLinea.MENU_NORMAL, null, 2, null, List.of(new ComponenteElegido("segundo", opcionSeco.getId())),
                null, null), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.CANTIDAD_INVALIDA);
    }

    @Test
    void el_menu_solo_se_pide_en_el_almuerzo() {
        Pedido desayuno = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "desayuno", ADMIN_SEMBRADO_ID)
                .valorOLanza();
        String personaDesayuno = personas.findByPedidoIdOrderByOrden(desayuno.getId()).get(0).getId();

        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, desayuno.getId(), personaDesayuno,
                TipoDeLinea.MENU_NORMAL, null, 1, null, List.of(), null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MENU_FUERA_DE_SERVICIO);
    }

    @Test
    void un_dia_sin_menu_confirmado_no_ofrece_el_menu() {
        // Otro dia, sin semana programada.
        when(reloj.instant()).thenReturn(LocalDate.of(2031, 8, 6).atTime(12, 0)
                .atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toInstant());
        Pedido sinMenu = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID)
                .valorOLanza();
        String personaSinMenu = personas.findByPedidoIdOrderByOrden(sinMenu.getId()).get(0).getId();

        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, sinMenu.getId(), personaSinMenu,
                TipoDeLinea.MENU_NORMAL, null, 1, null, List.of(), null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MENU_NO_PROGRAMADO);
    }

    @Test
    void una_opcion_agotada_en_este_local_no_se_ofrece_ni_se_acepta() {
        marcarAgotado.ejecutar(seco.getId()).valorOLanza();

        // Con una sola opcion disponible (el estofado) se asigna sola, sin pedir eleccion.
        DetallePedido linea = agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL, List.of()), ADMIN_SEMBRADO_ID)
                .valorOLanza();
        assertThat(componentes.findByDetallePedidoIdIn(List.of(linea.getId())))
                .extracting(DetallePedidoComponente::getProductoId).doesNotContain(seco.getId());
        // Elegir la agotada se rechaza.
        assertThat(error(agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL,
                List.of(new ComponenteElegido("segundo", opcionSeco.getId()))), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PRODUCTO_AGOTADO);
        // Y la oferta ya no la lista.
        assertThat(consulta.oferta(pedido.getId()).valorOLanza().normal().get(1).opciones())
                .extracting(OpcionOfrecida::opcionMenuId).doesNotContain(opcionSeco.getId());
    }

    @Test
    void si_todas_las_opciones_de_un_componente_estan_agotadas_no_se_puede_pedir_el_menu() {
        marcarAgotado.ejecutar(seco.getId()).valorOLanza();
        marcarAgotado.ejecutar(estofado.getId()).valorOLanza();

        assertThat(error(agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL, List.of()), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PRODUCTO_AGOTADO);
    }

    @Test
    void una_eleccion_sin_opcion_se_toma_como_sin_eleccion_y_una_repetida_o_sin_componente_se_rechaza() {
        // {"entrada": null} en un componente de una sola opcion: sin eleccion, se asigna sola (no es un 500).
        assertThat(agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL, java.util.Arrays.asList(
                new ComponenteElegido("entrada", null), new ComponenteElegido("segundo", opcionSeco.getId()))),
                ADMIN_SEMBRADO_ID).esExito()).isTrue();
        assertThat(error(agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL, List.of(
                new ComponenteElegido("segundo", opcionSeco.getId()),
                new ComponenteElegido("segundo", opcionEstofado.getId()))), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.OPCION_INVALIDA);
        assertThat(error(agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL, List.of(
                new ComponenteElegido(null, opcionSeco.getId()))), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.OPCION_INVALIDA);
    }

    @Test
    void el_mismo_menu_con_las_mismas_opciones_suma_cantidad_y_con_otra_opcion_va_aparte() {
        var conSeco = List.of(new ComponenteElegido("segundo", opcionSeco.getId()));
        DetallePedido primero = agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL, conSeco), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        DetallePedido igual = agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL, conSeco), ADMIN_SEMBRADO_ID)
                .valorOLanza();
        DetallePedido otro = agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL,
                List.of(new ComponenteElegido("segundo", opcionEstofado.getId()))), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(igual.getId()).isEqualTo(primero.getId());
        assertThat(igual.getCantidad()).isEqualTo(2);
        assertThat(otro.getId()).isNotEqualTo(primero.getId());
        assertThat(otro.getCantidad()).isEqualTo(1);
    }

    @Test
    void el_menu_de_un_pensionista_no_se_junta_porque_va_uno_por_linea() {
        Pensionista luis = nuevoPensionista(plan, "Juntar");
        asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        var conSeco = List.of(new ComponenteElegido("segundo", opcionSeco.getId()));

        DetallePedido primero = agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL, conSeco), ADMIN_SEMBRADO_ID)
                .valorOLanza();
        DetallePedido segundo = agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL, conSeco), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(segundo.getId()).isNotEqualTo(primero.getId());
        assertThat(segundo.getCantidad()).isEqualTo(1);
    }

    @Test
    void la_comanda_de_un_menu_dice_que_es_cada_opcion_en_el_orden_del_menu() {
        agregarLinea.ejecutar(menu(TipoDeLinea.MENU_NORMAL, List.of(new ComponenteElegido("segundo", opcionSeco.getId()))),
                ADMIN_SEMBRADO_ID).valorOLanza();
        var comanda = comandarPedido.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var linea = consultaComandas.detalle(comanda.getId()).valorOLanza().personas().get(0).lineas().get(0);

        assertThat(linea.partes()).extracting(ComponenteDetallado::componente, ComponenteDetallado::productoNombre)
                .containsExactly(tuple("entrada", entrada.getNombre()), tuple("segundo", seco.getNombre()));
        assertThat(linea.componentes()).containsExactly(entrada.getNombre(), seco.getNombre());
    }
}
