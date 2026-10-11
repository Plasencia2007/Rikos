package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.MarcarAgotado;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;
import pe.rikos.operacion.pensionistas.aplicacion.CambiarParametro;
import pe.rikos.operacion.pensionistas.aplicacion.ClaveParametro;
import pe.rikos.operacion.pensionistas.aplicacion.PreciosPlan;
import pe.rikos.operacion.pensionistas.aplicacion.ReglaSegundoConsumo;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** US-028: agregar lineas de carta, plan y extra, con el precio del motor. "Hoy" es el miercoles 11/06/2031. */
@SpringBootTest
@Transactional
class LineasDelPedidoTest extends PedidosBase {

    private static final LocalDate HOY = LocalDate.of(2031, 6, 11);

    @MockitoBean
    Clock reloj;

    @Autowired
    AbrirPedido abrirPedido;
    @Autowired
    AsignarPensionista asignarPensionista;
    @Autowired
    QuitarPersona quitarPersona;
    @Autowired
    AgregarLinea agregarLinea;
    @Autowired
    CuentaPersonaRepositorio personas;
    @Autowired
    MarcarAgotado marcarAgotado;
    @Autowired
    CambiarParametro cambiarParametro;
    @Autowired
    AuditoriaRepositorio auditorias;
    @Autowired
    pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio lineasRepositorio;
    @Autowired
    AgregarPersona agregarPersona;

    String localA;
    String localB;
    PlanTarifario plan;
    Pedido pedido;
    String persona;

    @BeforeEach
    void preparar() {
        when(reloj.getZone()).thenReturn(ConfiguracionReloj.ZONA_DEL_RESTAURANTE);
        when(reloj.instant()).thenReturn(HOY.atTime(12, 0).atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toInstant());
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = nuevoLocal("la");
        localB = nuevoLocal("lb");
        plan = nuevoPlan(500, 800, 800);
        cambiarParametro.ejecutar(ClaveParametro.TAPER_PRECIO, "100", ADMIN_SEMBRADO_ID);
        cambiarParametro.ejecutar(ClaveParametro.SEGUNDO_CONSUMO_REGLA, ReglaSegundoConsumo.PRECIO_DEL_PLAN.codigo(),
                ADMIN_SEMBRADO_ID);
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
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

    SolicitudDeLinea carta(String productoId, int cantidad) {
        return new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.PRODUCTO, productoId, cantidad, null,
                List.of(), null, null);
    }

    @Test
    void agrega_un_producto_de_la_carta_con_su_precio_normal_y_su_observacion() {
        var chicha = productoDeCarta("Chicha", 500L);

        DetallePedido linea = agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona,
                TipoDeLinea.PRODUCTO, chicha.getId(), 2, "  Sin hielo ", List.of(), null, null), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(linea.getPrecioUnitario()).isEqualTo(500L);
        assertThat(linea.getTarifaOrigen()).isEqualTo("publico");
        assertThat(linea.getCantidad()).isEqualTo(2);
        assertThat(linea.getObservacion()).isEqualTo("Sin hielo");
        assertThat(linea.getEstado()).isEqualTo("pendiente");
    }

    @Test
    void un_producto_sin_precio_un_no_vendible_uno_del_menu_o_inexistente_no_se_agrega() {
        var sinPrecio = productoDeCarta("Sin precio", null);

        assertThat(error(agregarLinea.ejecutar(carta(sinPrecio.getId(), 1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.SIN_PRECIO);
        assertThat(error(agregarLinea.ejecutar(carta("no-existe", 1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PRODUCTO_NO_ENCONTRADO);
        var insumo = productos.findAll().stream().filter(p -> !p.isEsVendible()).findFirst().orElseThrow();
        assertThat(error(agregarLinea.ejecutar(carta(insumo.getId(), 1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PRODUCTO_NO_VENDIBLE);
        var menuNormal = productos.findByTipoMenu("normal").orElseThrow();
        assertThat(error(agregarLinea.ejecutar(carta(menuNormal.getId(), 1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PRODUCTO_NO_VENDIBLE);
    }

    @Test
    void cantidad_cero_o_negativa_y_observacion_larga_se_rechazan() {
        var chicha = productoDeCarta("Chicha", 500L);

        assertThat(error(agregarLinea.ejecutar(carta(chicha.getId(), 0), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.CANTIDAD_INVALIDA);
        assertThat(error(agregarLinea.ejecutar(carta(chicha.getId(), -1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.CANTIDAD_INVALIDA);
        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.PRODUCTO,
                chicha.getId(), 1, "x".repeat(251), List.of(), null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.OBSERVACION_LARGA);
    }

    @Test
    void un_producto_agotado_en_este_local_no_se_ofrece_pero_en_otro_local_si() {
        var chicha = productoDeCarta("Chicha", 500L);
        marcarAgotado.ejecutar(chicha.getId()).valorOLanza();

        assertThat(error(agregarLinea.ejecutar(carta(chicha.getId(), 1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PRODUCTO_AGOTADO);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        Pedido deB = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaB = personas.findByPedidoIdOrderByOrden(deB.getId()).get(0).getId();
        assertThat(agregarLinea.ejecutar(new SolicitudDeLinea(null, deB.getId(), personaB, TipoDeLinea.PRODUCTO,
                chicha.getId(), 1, null, List.of(), null, null), ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }

    @Test
    void enviar_dos_veces_el_mismo_id_de_linea_no_la_duplica() {
        var chicha = productoDeCarta("Chicha", 500L);
        String id = Identificador.nuevo().valor();
        var solicitud = new SolicitudDeLinea(id, pedido.getId(), persona, TipoDeLinea.PRODUCTO, chicha.getId(), 1, null,
                List.of(), null, null);

        DetallePedido primera = agregarLinea.ejecutar(solicitud, ADMIN_SEMBRADO_ID).valorOLanza();
        DetallePedido repetida = agregarLinea.ejecutar(solicitud, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(repetida.getId()).isEqualTo(primera.getId());
    }

    @Test
    void un_pedido_o_una_persona_de_otro_local_no_se_encuentra() {
        var chicha = productoDeCarta("Chicha", 500L);
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);

        assertThat(error(agregarLinea.ejecutar(carta(chicha.getId(), 1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.NO_ENCONTRADO);
    }

    @Test
    void la_comida_del_plan_es_solo_para_pensionistas_y_solo_en_su_servicio() {
        Pensionista luis = nuevoPensionista(plan, "Plan");
        Pedido desayuno = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "desayuno", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaDesayuno = personas.findByPedidoIdOrderByOrden(desayuno.getId()).get(0).getId();

        var comoNormal = new SolicitudDeLinea(null, desayuno.getId(), personaDesayuno, TipoDeLinea.PLAN_DESAYUNO, null, 1,
                null, List.of(), null, null);
        assertThat(error(agregarLinea.ejecutar(comoNormal, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PERSONA_NO_ES_PENSIONISTA);

        asignarPensionista.ejecutar(desayuno.getId(), personaDesayuno, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        DetallePedido linea = agregarLinea.ejecutar(comoNormal, ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(linea.getPrecioUnitario()).isEqualTo(500L);
        assertThat(linea.getTarifaOrigen()).isEqualTo("plan");
        // Es un pedido para llevar: la comida del plan lleva el taper por menu (RN-009).
        assertThat(linea.getTaperUnitario()).isEqualTo(100L);

        var cenaEnDesayuno = new SolicitudDeLinea(null, desayuno.getId(), personaDesayuno, TipoDeLinea.PLAN_CENA, null, 1,
                null, List.of(), null, null);
        assertThat(error(agregarLinea.ejecutar(cenaEnDesayuno, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PLAN_FUERA_DE_SERVICIO);
    }

    @Test
    void el_segundo_consumo_del_dia_se_cuenta_entre_locales() {
        Pensionista luis = nuevoPensionista(plan, "Segundo");
        // Primer desayuno del plan del dia, en el local B: precio del plan y no es el segundo.
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        Pedido enB = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "desayuno", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaB = personas.findByPedidoIdOrderByOrden(enB.getId()).get(0).getId();
        asignarPensionista.ejecutar(enB.getId(), personaB, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        DetallePedido primero = agregarLinea.ejecutar(new SolicitudDeLinea(null, enB.getId(), personaB,
                TipoDeLinea.PLAN_DESAYUNO, null, 1, null, List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(primero.isSegundoDelDia()).isFalse();
        assertThat(primero.getPrecioUnitario()).isEqualTo(500L);

        // Segundo desayuno del mismo dia, ahora en el local A: la regla (precio del plan) lo cobra al plan,
        // pero queda marcado como segundo consumo.
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        Pedido enA = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "desayuno", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaA = personas.findByPedidoIdOrderByOrden(enA.getId()).get(0).getId();
        asignarPensionista.ejecutar(enA.getId(), personaA, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        DetallePedido segundo = agregarLinea.ejecutar(new SolicitudDeLinea(null, enA.getId(), personaA,
                TipoDeLinea.PLAN_DESAYUNO, null, 1, null, List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(segundo.isSegundoDelDia()).isTrue();
        assertThat(segundo.getPrecioUnitario()).isEqualTo(500L);
    }

    @Test
    void el_extra_libre_lleva_descripcion_y_precio_del_mozo_y_queda_en_la_bitacora() {
        var solicitud = new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.EXTRA_LIBRE, null, 1, null,
                List.of(), "  Huevo frito ", 300L);

        DetallePedido linea = agregarLinea.ejecutar(solicitud, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(linea.getDescripcion()).isEqualTo("Huevo frito");
        assertThat(linea.getPrecioUnitario()).isEqualTo(300L);
        assertThat(linea.getTarifaOrigen()).isEqualTo("manual");
        assertThat(auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("detalle_pedido", linea.getId()))
                .anyMatch(a -> "precio_manual".equals(a.getEvento()));

        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.EXTRA_LIBRE,
                null, 1, null, List.of(), "  ", 300L), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.DESCRIPCION_OBLIGATORIA);
        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.EXTRA_LIBRE,
                null, 1, null, List.of(), "Huevo", 0L), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.PRECIO_INVALIDO);
        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.EXTRA_LIBRE,
                null, 1, null, List.of(), "Huevo", null), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.PRECIO_INVALIDO);
    }

    // ---- Personas con lineas (pruebas movidas desde PersonasDelPedidoTest) ---------------------------------

    @Test
    void no_se_asigna_un_pensionista_a_una_persona_que_ya_tiene_lineas() {
        Pensionista luis = nuevoPensionista(plan, "Quispe");
        agregarLinea.ejecutar(carta(productoDeCarta("Chicha", 500L).getId(), 1), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(asignarPensionista.ejecutar(pedido.getId(), persona, luis.getId(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PERSONA_CON_LINEAS);
    }

    @Test
    void una_persona_con_lineas_no_se_quita() {
        agregarLinea.ejecutar(carta(productoDeCarta("Chicha", 500L).getId(), 1), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(quitarPersona.ejecutar(pedido.getId(), persona))).isEqualTo(ErrorPedido.PERSONA_CON_LINEAS);
    }

    @Test
    void un_id_de_linea_que_ya_existe_en_otra_persona_no_se_reutiliza_ni_la_mueve() {
        String otra = agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        String id = Identificador.nuevo().valor();
        var chicha = productoDeCarta("Chicha", 500L);
        agregarLinea.ejecutar(new SolicitudDeLinea(id, pedido.getId(), persona, TipoDeLinea.PRODUCTO, chicha.getId(), 1,
                null, List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        // El mismo id, pero pidiendo que quede en OTRA persona: no se reutiliza (y la original no se mueve).
        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(id, pedido.getId(), otra, TipoDeLinea.PRODUCTO,
                chicha.getId(), 5, null, List.of(), null, null), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.NO_ENCONTRADO);

        DetallePedido original = lineasRepositorio.findById(id).orElseThrow();
        assertThat(original.getCuentaPersonaId()).isEqualTo(persona);
        assertThat(original.getCantidad()).isEqualTo(1);
    }
}
