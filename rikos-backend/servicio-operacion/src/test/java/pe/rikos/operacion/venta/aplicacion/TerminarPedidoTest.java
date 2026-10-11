package pe.rikos.operacion.venta.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Sprint 6b, adenda: "Terminar pedido" es un cierre SIN cobro; libera la mesa y el pedido pasa al historial. */
@SpringBootTest
@Transactional
class TerminarPedidoTest extends ComandasBase {

    @Autowired
    TerminarPedido terminar;
    @Autowired
    ConsultaMesas consultaMesas;
    @Autowired
    ListaDePedidos lista;
    @Autowired
    UnirMesas unirMesas;
    @Autowired
    ModificarLinea modificarLinea;
    @Autowired
    QuitarLinea quitarLinea;
    @Autowired
    QuitarPersona quitarPersona;
    @Autowired
    AnularLinea anularLinea;
    @Autowired
    ComandarPedido comandar;
    @Autowired
    ConsultaComandas consultaComandas;
    @Autowired
    PedidoRepositorio pedidosRepositorio;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManager em;

    private EstadoDeMesa estadoDe(String mesaId) {
        return consultaMesas.mapa().valorOLanza().stream().filter(f -> f.mesa().getId().equals(mesaId))
                .findFirst().orElseThrow().estado();
    }

    @Test
    void termina_el_pedido_con_quien_y_cuando_y_la_mesa_queda_libre() {
        linea("Chicha", 1);
        comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(estadoDe(pedido.getMesaId())).isEqualTo(EstadoDeMesa.OCUPADA);

        var terminado = terminar.ejecutar(pedido.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(terminado.getEstado()).isEqualTo("cerrado");
        assertThat(terminado.getCerradoPor()).isEqualTo(ADMIN_SEMBRADO_ID);
        assertThat(terminado.getCerradoEn()).isNotNull();
        assertThat(estadoDe(pedido.getMesaId())).isEqualTo(EstadoDeMesa.LIBRE);
    }

    @Test
    void sale_de_en_curso_y_aparece_en_el_historial_sin_lineas() {
        linea("Chicha", 2);
        em.flush();
        assertThat(lista.resumen().valorOLanza().enCurso()).isEqualTo(1);

        terminar.ejecutar(pedido.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        em.flush();
        em.clear();

        assertThat(lista.resumen().valorOLanza().enCurso()).isZero();
        assertThat(lista.listar(new FiltroDePedidos(null, null, null, null, null, null, 0, 20)).valorOLanza().contenido())
                .isEmpty();
        var cerrados = lista.listar(new FiltroDePedidos("cerrados", null, null, null, null, null, 0, 20)).valorOLanza();
        assertThat(cerrados.total()).isEqualTo(1);
        var fila = cerrados.contenido().get(0);
        assertThat(fila.estado()).isEqualTo("cerrado");
        assertThat(fila.total()).isEqualTo(1000);
        assertThat(fila.lineas()).isNull();
    }

    @Test
    void en_la_misma_mesa_se_puede_abrir_otro_pedido() {
        terminar.ejecutar(pedido.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        em.flush();

        var otro = abrirPedido.ejecutar(null, Canal.MESA, pedido.getMesaId(), "almuerzo", ADMIN_SEMBRADO_ID);

        assertThat(otro.esExito()).isTrue();
    }

    @Test
    void un_pedido_pendiente_de_cierre_o_para_llevar_tambien_se_termina() {
        em.flush();
        jdbc.update("update pedido set estado = 'pendiente_cierre' where id = ?", pedido.getId());
        em.flush();
        em.clear();
        var llevar = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(terminar.ejecutar(pedido.getId(), ADMIN_SEMBRADO_ID).valorOLanza().getEstado()).isEqualTo("cerrado");
        assertThat(terminar.ejecutar(llevar.getId(), ADMIN_SEMBRADO_ID).valorOLanza().getEstado()).isEqualTo("cerrado");
    }

    @Test
    void uno_ya_cerrado_no_se_termina_otra_vez_y_uno_de_otro_local_no_se_encuentra() {
        terminar.ejecutar(pedido.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        em.flush();
        assertThat(error(terminar.ejecutar(pedido.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.PEDIDO_NO_TERMINABLE);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        assertThat(error(terminar.ejecutar(pedido.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.NO_ENCONTRADO);
        assertThat(error(terminar.ejecutar("no-existe", ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.NO_ENCONTRADO);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        assertThat(error(terminar.ejecutar(pedido.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.SIN_LOCAL_ACTIVO);
    }

    @Test
    void las_mesas_unidas_a_la_del_pedido_se_separan_solas() {
        String secundaria = nuevaMesa("2");
        unirMesas.ejecutar(secundaria, pedido.getMesaId(), ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(estadoDe(secundaria)).isEqualTo(EstadoDeMesa.UNIDA);

        terminar.ejecutar(pedido.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(estadoDe(secundaria)).isEqualTo(EstadoDeMesa.LIBRE);
        assertThat(estadoDe(pedido.getMesaId())).isEqualTo(EstadoDeMesa.LIBRE);
    }

    @Test
    void un_pedido_cerrado_no_se_modifica_pero_su_comanda_se_sigue_viendo() {
        var chicha = linea("Chicha", 1);
        var comanda = comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        var motivo = motivoDeAnulacion("Error", false);
        terminar.ejecutar(pedido.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        em.flush();
        em.clear();
        var otroProducto = productoDeCarta("Lomo", 800L);

        assertThat(error(agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.PRODUCTO,
                otroProducto.getId(), 1, null, List.of(), null, null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PEDIDO_CERRADO);
        assertThat(error(agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PEDIDO_CERRADO);
        assertThat(error(modificarLinea.ejecutar(pedido.getId(), chicha.getId(), 3, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PEDIDO_CERRADO);
        assertThat(error(quitarLinea.ejecutar(pedido.getId(), chicha.getId(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PEDIDO_CERRADO);
        assertThat(error(quitarPersona.ejecutar(pedido.getId(), persona)))
                .isEqualTo(ErrorPedido.PEDIDO_CERRADO);
        assertThat(error(anularLinea.ejecutar(pedido.getId(), chicha.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PEDIDO_CERRADO);
        // La comanda de un pedido cerrado se sigue consultando (el Historial la reimprime).
        assertThat(consultaComandas.detalle(comanda.getId()).esExito()).isTrue();
    }

    @Test
    void terminar_queda_en_la_bitacora_como_cierre_sin_cobro() {
        terminar.ejecutar(pedido.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        var eventos = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("pedido", pedido.getId());
        var evento = eventos.stream().filter(a -> "pedido_terminado".equals(a.getEvento())).findFirst().orElseThrow();
        assertThat(evento.getValorNuevo()).contains("\"nombre\"").contains("\"mesa\"");
    }
}
