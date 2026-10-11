package pe.rikos.operacion.caja.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.CobroRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.pensionistas.aplicacion.ConsultaCuenta;
import pe.rikos.operacion.venta.aplicacion.AsignarPensionista;
import pe.rikos.operacion.venta.aplicacion.Canal;
import pe.rikos.operacion.venta.aplicacion.ComandasBase;
import pe.rikos.operacion.venta.aplicacion.ConsultaMesas;
import pe.rikos.operacion.venta.aplicacion.EstadoDeMesa;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-035 (sprint 7, entrega 5): reabrir un cobro mientras su turno de origen siga abierto. Las lineas vuelven a "comandado",
 * el pedido se reabre (la mesa se ocupa de nuevo), el cargo al pensionista se revierte con un ajuste y el dinero del cobro
 * sale de la caja. Despues del cierre del turno ya no se reabre: se corrige con una operacion compensatoria (sprint 11).
 */
@SpringBootTest
@Transactional
class ReabrirCobroTest extends ComandasBase {

    @Autowired
    CobrarPedido cobrar;
    @Autowired
    ReabrirCobro reabrir;
    @Autowired
    AbrirTurno abrirTurno;
    @Autowired
    CerrarTurno cerrarTurno;
    @Autowired
    ConsultaCierre consultaCierre;
    @Autowired
    ConsultaCajas consultaCajas;
    @Autowired
    CobroRepositorio cobros;
    @Autowired
    DetallePedidoRepositorio lineas;
    @Autowired
    PedidoRepositorio pedidos;
    @Autowired
    ConsultaMesas consultaMesas;
    @Autowired
    ConsultaCuenta consultaCuenta;
    @Autowired
    AsignarPensionista asignarPensionista;

    TurnoCaja turno;
    Motivo motivoReapertura;

    @BeforeEach
    void preparar() {
        var caja = consultaCajas.listar().valorOLanza().get(0).caja();
        turno = abrirTurno.ejecutar(caja.getId(), 10000L, ADMIN_SEMBRADO_ID).valorOLanza();
        motivoReapertura = motivos.save(new Motivo(pe.rikos.contratos.Identificador.nuevo().valor(), "reapertura",
                "Error al cobrar " + pe.rikos.contratos.Identificador.nuevo().valor().substring(18), false));
    }

    static ErrorCobro error(Resultado<?, ErrorCobro> resultado) {
        assertThat(resultado.esFracaso()).as("se esperaba un fracaso").isTrue();
        return ((Resultado.Fracaso<?, ErrorCobro>) resultado).error();
    }

    private CobroRegistrado cobrarTodo(DetallePedido l, long recibido) {
        comandarPedido.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        return cobrar.ejecutar(pedido.getId(), List.of(l.getId()), List.of(new PagoSolicitado("efectivo", recibido, null)),
                ADMIN_SEMBRADO_ID).valorOLanza();
    }

    private String estadoDeLaMesa() {
        return consultaMesas.todas().valorOLanza().stream().filter(f -> f.mesa().getId().equals(pedido.getMesaId())).findFirst()
                .orElseThrow().estado().name();
    }

    @Test
    void reabrir_devuelve_las_lineas_a_comandado_reabre_el_pedido_y_ocupa_la_mesa() {
        var plato = linea("Lomo", 2);
        var cobro = cobrarTodo(plato, 1000L);
        assertThat(pedidos.findById(pedido.getId()).orElseThrow().getEstado()).isEqualTo("cerrado");
        assertThat(estadoDeLaMesa()).isEqualTo(EstadoDeMesa.LIBRE.name());

        reabrir.ejecutar(cobro.cobro().getId(), motivoReapertura.getId(), "me equivoque", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(lineas.findById(plato.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
        var reabierto = pedidos.findById(pedido.getId()).orElseThrow();
        assertThat(reabierto.getEstado()).isEqualTo("comandado");
        assertThat(reabierto.getCerradoEn()).isNull();
        assertThat(estadoDeLaMesa()).isEqualTo(EstadoDeMesa.OCUPADA.name());
        assertThat(cobros.findById(cobro.cobro().getId()).orElseThrow().getRevertidoEn()).isNotNull();
    }

    @Test
    void el_dinero_del_cobro_reabierto_sale_del_efectivo_esperado() {
        var plato = linea("Lomo", 2);
        var cobro = cobrarTodo(plato, 1000L);
        assertThat(consultaCierre.previo(ADMIN_SEMBRADO_ID).valorOLanza().efectivoEsperado()).isEqualTo(11000L);

        reabrir.ejecutar(cobro.cobro().getId(), motivoReapertura.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var resumen = consultaCierre.previo(ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(resumen.efectivoEsperado()).isEqualTo(10000L);
        assertThat(resumen.cobros()).isZero();
    }

    @Test
    void despues_de_reabrir_la_linea_se_puede_cobrar_de_nuevo() {
        var plato = linea("Lomo", 2);
        var cobro = cobrarTodo(plato, 1000L);
        reabrir.ejecutar(cobro.cobro().getId(), motivoReapertura.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var otraVez = cobrar.ejecutar(pedido.getId(), List.of(plato.getId()),
                List.of(new PagoSolicitado("yape", 1000L, null)), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(otraVez.pedidoCerrado()).isTrue();
        assertThat(consultaCierre.previo(ADMIN_SEMBRADO_ID).valorOLanza().totalYape()).isEqualTo(1000L);
    }

    @Test
    void reabrir_un_cobro_con_pensionista_devuelve_su_cargo() {
        var plan = nuevoPlan(500L, 900L, 600L);
        var pensionista = nuevoPensionista(plan, "Reabierto");
        agregarPersona.ejecutar(pedido.getId(), null, "Luis", ADMIN_SEMBRADO_ID);
        var cuenta = personas.findByPedidoIdOrderByOrden(pedido.getId()).stream().filter(p -> p.getEtiqueta().equals("Luis"))
                .findFirst().orElseThrow();
        asignarPensionista.ejecutar(pedido.getId(), cuenta.getId(), pensionista.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        var suya = linea(cuenta.getId(), "Plato de Luis", 2);
        comandarPedido.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        long saldoInicial = consultaCuenta.resumen(pensionista.getId()).orElseThrow().saldo();
        var cobro = cobrar.ejecutar(pedido.getId(), List.of(suya.getId()), List.of(), ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(consultaCuenta.resumen(pensionista.getId()).orElseThrow().saldo()).isEqualTo(saldoInicial - 1000L);

        reabrir.ejecutar(cobro.cobro().getId(), motivoReapertura.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(consultaCuenta.resumen(pensionista.getId()).orElseThrow().saldo()).isEqualTo(saldoInicial);
    }

    @Test
    void con_el_turno_de_origen_cerrado_no_se_reabre() {
        var plato = linea("Lomo", 1);
        var cobro = cobrarTodo(plato, 500L);
        cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, 10500L, null).valorOLanza();

        assertThat(error(reabrir.ejecutar(cobro.cobro().getId(), motivoReapertura.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCobro.TURNO_CERRADO);
        assertThat(lineas.findById(plato.getId()).orElseThrow().getEstado()).isEqualTo("cobrado");
    }

    @Test
    void un_cobro_ya_reabierto_no_se_reabre_otra_vez() {
        var cobro = cobrarTodo(linea("Lomo", 1), 500L);
        reabrir.ejecutar(cobro.cobro().getId(), motivoReapertura.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(reabrir.ejecutar(cobro.cobro().getId(), motivoReapertura.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCobro.COBRO_YA_REVERTIDO);
    }

    @Test
    void el_motivo_es_obligatorio_y_de_tipo_reapertura() {
        var cobro = cobrarTodo(linea("Lomo", 1), 500L);
        var deAnulacion = motivoDeAnulacion("Otra", false);
        var exigente = motivos.save(new Motivo(pe.rikos.contratos.Identificador.nuevo().valor(), "reapertura",
                "Exigente " + pe.rikos.contratos.Identificador.nuevo().valor().substring(18), true));

        assertThat(error(reabrir.ejecutar(cobro.cobro().getId(), null, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCobro.MOTIVO_NO_ENCONTRADO);
        assertThat(error(reabrir.ejecutar(cobro.cobro().getId(), deAnulacion.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCobro.MOTIVO_INVALIDO);
        assertThat(error(reabrir.ejecutar(cobro.cobro().getId(), exigente.getId(), " ", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCobro.OBSERVACION_OBLIGATORIA);
        assertThat(cobros.findById(cobro.cobro().getId()).orElseThrow().getRevertidoEn()).isNull();
    }

    @Test
    void si_la_mesa_ya_la_ocupa_otro_pedido_no_se_reabre() {
        var cobro = cobrarTodo(linea("Lomo", 1), 500L);
        // La mesa quedo libre al cerrar y otro mozo abre un pedido nuevo en ella.
        abrirPedido.ejecutar(null, Canal.MESA, pedido.getMesaId(), "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(reabrir.ejecutar(cobro.cobro().getId(), motivoReapertura.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCobro.MESA_OCUPADA);
    }

    @Test
    void un_cobro_de_otro_local_no_se_encuentra() {
        var cobro = cobrarTodo(linea("Lomo", 1), 500L);
        pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);

        assertThat(error(reabrir.ejecutar(cobro.cobro().getId(), motivoReapertura.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCobro.COBRO_NO_ENCONTRADO);
        assertThat(error(reabrir.ejecutar("no-existe", motivoReapertura.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCobro.COBRO_NO_ENCONTRADO);
    }

    @Test
    void la_reapertura_deja_traza_con_el_motivo() {
        var cobro = cobrarTodo(linea("Lomo", 1), 500L);

        reabrir.ejecutar(cobro.cobro().getId(), motivoReapertura.getId(), "me equivoque", ADMIN_SEMBRADO_ID).valorOLanza();

        var eventos = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("pedido", pedido.getId());
        var e = eventos.stream().filter(x -> x.getEvento().equals("reapertura")).findFirst().orElseThrow();
        assertThat(e.getMotivoId()).isEqualTo(motivoReapertura.getId());
        assertThat(e.getObservacion()).isEqualTo("me equivoque");
    }
}
