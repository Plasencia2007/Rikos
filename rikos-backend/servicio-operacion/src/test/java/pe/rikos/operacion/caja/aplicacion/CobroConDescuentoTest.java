package pe.rikos.operacion.caja.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.CobroLineaRepositorio;
import pe.rikos.operacion.caja.infraestructura.MovimientoCajaRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.venta.aplicacion.AnularLinea;
import pe.rikos.operacion.venta.aplicacion.ComandasBase;
import pe.rikos.operacion.venta.aplicacion.ErrorPedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-034 (sprint 7, entrega 3): descuentos por linea al cobrar (monto o porcentaje) con motivo obligatorio y bitacora, y la
 * anulacion de lo ultimo que queda por cobrar, que cierra el pedido si ya habia algo cobrado. El PERMISO para descontar
 * (caja.descontar) lo exige el controlador; aqui se prueba la regla.
 */
@SpringBootTest
@Transactional
class CobroConDescuentoTest extends ComandasBase {

    @Autowired
    CobrarPedido cobrar;
    @Autowired
    AbrirTurno abrirTurno;
    @Autowired
    ConsultaCajas consultaCajas;
    @Autowired
    CobroLineaRepositorio cobroLineas;
    @Autowired
    MovimientoCajaRepositorio movimientos;
    @Autowired
    DetallePedidoRepositorio lineas;
    @Autowired
    PedidoRepositorio pedidos;
    @Autowired
    AnularLinea anularLinea;

    TurnoCaja turno;
    Motivo motivoDescuento;

    @BeforeEach
    void preparar() {
        var caja = consultaCajas.listar().valorOLanza().get(0).caja();
        turno = abrirTurno.ejecutar(caja.getId(), 0L, ADMIN_SEMBRADO_ID).valorOLanza();
        motivoDescuento = motivos.save(new Motivo(pe.rikos.contratos.Identificador.nuevo().valor(), "descuento",
                "Cortesia " + pe.rikos.contratos.Identificador.nuevo().valor().substring(18), false));
    }

    private void comandar() {
        comandarPedido.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
    }

    private Resultado<CobroRegistrado, ErrorCobro> cobrarCon(List<String> ids, List<DescuentoSolicitado> descuentos,
                                                             PagoSolicitado... pagos) {
        return cobrar.ejecutar(pedido.getId(), ids, List.of(pagos), descuentos, ADMIN_SEMBRADO_ID);
    }

    static ErrorCobro error(Resultado<?, ErrorCobro> resultado) {
        assertThat(resultado.esFracaso()).as("se esperaba un fracaso").isTrue();
        return ((Resultado.Fracaso<?, ErrorCobro>) resultado).error();
    }

    @Test
    void un_descuento_en_monto_baja_lo_que_se_cobra_y_queda_en_el_snapshot() {
        DetallePedido plato = linea("Lomo", 2);
        comandar();

        var cobro = cobrarCon(List.of(plato.getId()),
                List.of(new DescuentoSolicitado(plato.getId(), "monto", 300L, motivoDescuento.getId(), null)),
                new PagoSolicitado("efectivo", 1000L, null)).valorOLanza();

        assertThat(cobro.cobro().getTotal()).isEqualTo(700L);
        assertThat(cobro.cobro().getVuelto()).isEqualTo(300L);
        var snapshot = cobroLineas.findAll().stream().filter(l -> l.getDetallePedidoId().equals(plato.getId())).findFirst()
                .orElseThrow();
        assertThat(snapshot.getPrecioUnitario()).isEqualTo(500L);
        assertThat(snapshot.getDescuento()).isEqualTo(300L);
        assertThat(snapshot.getImporte()).isEqualTo(700L);
        assertThat(snapshot.getMotivoDescuentoId()).isEqualTo(motivoDescuento.getId());
        assertThat(movimientos.findByTurnoCajaId(turno.getId()).stream().mapToLong(m -> m.getImporte()).sum()).isEqualTo(700L);
    }

    @Test
    void un_descuento_en_porcentaje_se_calcula_sobre_el_importe_de_la_linea() {
        DetallePedido plato = linea("Lomo", 2);
        comandar();

        var cobro = cobrarCon(List.of(plato.getId()),
                List.of(new DescuentoSolicitado(plato.getId(), "porcentaje", 10L, motivoDescuento.getId(), "amigo de la casa")),
                new PagoSolicitado("yape", 900L, null)).valorOLanza();

        assertThat(cobro.cobro().getTotal()).isEqualTo(900L);
        assertThat(cobroLineas.findAll().stream().filter(l -> l.getDetallePedidoId().equals(plato.getId())).findFirst()
                .orElseThrow().getDescuento()).isEqualTo(100L);
    }

    @Test
    void el_descuento_deja_traza_con_valores_anterior_y_nuevo_y_el_motivo() {
        DetallePedido plato = linea("Lomo", 2);
        comandar();

        cobrarCon(List.of(plato.getId()),
                List.of(new DescuentoSolicitado(plato.getId(), "monto", 300L, motivoDescuento.getId(), "ok")),
                new PagoSolicitado("efectivo", 700L, null)).valorOLanza();

        var eventos = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("detalle_pedido", plato.getId());
        assertThat(eventos).extracting(e -> e.getEvento()).contains("descuento");
        var d = eventos.stream().filter(e -> e.getEvento().equals("descuento")).findFirst().orElseThrow();
        assertThat(d.getMotivoId()).isEqualTo(motivoDescuento.getId());
        assertThat(d.getValorAnterior()).contains("1000");
        assertThat(d.getValorNuevo()).contains("300").contains("700");
    }

    @Test
    void sin_motivo_o_con_un_motivo_que_no_es_de_descuento_no_se_cobra() {
        DetallePedido plato = linea("Lomo", 2);
        comandar();
        var deAnulacion = motivoDeAnulacion("Otra cosa", false);

        assertThat(error(cobrarCon(List.of(plato.getId()),
                List.of(new DescuentoSolicitado(plato.getId(), "monto", 100L, null, null)),
                new PagoSolicitado("efectivo", 1000L, null)))).isEqualTo(ErrorCobro.MOTIVO_NO_ENCONTRADO);
        assertThat(error(cobrarCon(List.of(plato.getId()),
                List.of(new DescuentoSolicitado(plato.getId(), "monto", 100L, deAnulacion.getId(), null)),
                new PagoSolicitado("efectivo", 1000L, null)))).isEqualTo(ErrorCobro.MOTIVO_INVALIDO);
        assertThat(lineas.findById(plato.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
    }

    @Test
    void un_motivo_que_exige_observacion_la_pide() {
        DetallePedido plato = linea("Lomo", 2);
        comandar();
        var exigente = motivos.save(new Motivo(pe.rikos.contratos.Identificador.nuevo().valor(), "descuento",
                "Otro " + pe.rikos.contratos.Identificador.nuevo().valor().substring(18), true));

        assertThat(error(cobrarCon(List.of(plato.getId()),
                List.of(new DescuentoSolicitado(plato.getId(), "monto", 100L, exigente.getId(), " ")),
                new PagoSolicitado("efectivo", 1000L, null)))).isEqualTo(ErrorCobro.OBSERVACION_OBLIGATORIA);
    }

    @Test
    void un_descuento_mal_formado_se_rechaza() {
        DetallePedido plato = linea("Lomo", 2);
        DetallePedido otra = linea("Arroz", 1);
        comandar();
        var pago = new PagoSolicitado("efectivo", 1000L, null);
        String m = motivoDescuento.getId();

        assertThat(error(cobrarCon(List.of(plato.getId()), List.of(new DescuentoSolicitado(plato.getId(), "regalo", 100L, m, null)), pago)))
                .isEqualTo(ErrorCobro.DESCUENTO_INVALIDO);
        assertThat(error(cobrarCon(List.of(plato.getId()), List.of(new DescuentoSolicitado(plato.getId(), "monto", 0L, m, null)), pago)))
                .isEqualTo(ErrorCobro.DESCUENTO_INVALIDO);
        assertThat(error(cobrarCon(List.of(plato.getId()), List.of(new DescuentoSolicitado(plato.getId(), "monto", 1001L, m, null)), pago)))
                .isEqualTo(ErrorCobro.DESCUENTO_INVALIDO);
        assertThat(error(cobrarCon(List.of(plato.getId()), List.of(new DescuentoSolicitado(plato.getId(), "porcentaje", 101L, m, null)), pago)))
                .isEqualTo(ErrorCobro.DESCUENTO_INVALIDO);
        // De una linea que no se esta cobrando ahora.
        assertThat(error(cobrarCon(List.of(plato.getId()), List.of(new DescuentoSolicitado(otra.getId(), "monto", 100L, m, null)), pago)))
                .isEqualTo(ErrorCobro.DESCUENTO_INVALIDO);
        // Dos descuentos a la misma linea.
        assertThat(error(cobrarCon(List.of(plato.getId()), List.of(new DescuentoSolicitado(plato.getId(), "monto", 100L, m, null),
                new DescuentoSolicitado(plato.getId(), "monto", 100L, m, null)), pago))).isEqualTo(ErrorCobro.DESCUENTO_INVALIDO);
        assertThat(lineas.findById(plato.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
    }

    @Test
    void no_se_cobra_un_total_en_cero() {
        DetallePedido plato = linea("Lomo", 1);
        comandar();

        assertThat(error(cobrarCon(List.of(plato.getId()),
                List.of(new DescuentoSolicitado(plato.getId(), "porcentaje", 100L, motivoDescuento.getId(), null)))))
                .isEqualTo(ErrorCobro.TOTAL_EN_CERO);
    }

    @Test
    void se_descuenta_una_linea_y_la_otra_se_cobra_completa() {
        DetallePedido uno = linea("Lomo", 1);
        DetallePedido dos = linea("Arroz", 1);
        comandar();

        var cobro = cobrarCon(List.of(uno.getId(), dos.getId()),
                List.of(new DescuentoSolicitado(uno.getId(), "monto", 500L, motivoDescuento.getId(), null)),
                new PagoSolicitado("efectivo", 500L, null)).valorOLanza();

        assertThat(cobro.cobro().getTotal()).isEqualTo(500L);
        assertThat(cobroLineas.findAll().stream().filter(l -> l.getDetallePedidoId().equals(dos.getId())).findFirst()
                .orElseThrow().getDescuento()).isZero();
    }

    // ---- anular lo ultimo que queda por cobrar ----

    @Test
    void anular_la_ultima_linea_por_cobrar_de_un_pedido_ya_cobrado_en_parte_lo_cierra() {
        DetallePedido cobrada = linea("Lomo", 1);
        DetallePedido pendiente = linea("Arroz", 1);
        comandar();
        cobrar.ejecutar(pedido.getId(), List.of(cobrada.getId()), List.of(new PagoSolicitado("efectivo", 500L, null)),
                ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(pedidos.findById(pedido.getId()).orElseThrow().getEstado()).isEqualTo("comandado");

        anularLinea.ejecutar(pedido.getId(), pendiente.getId(), motivoDeAnulacion("Se fue", false).getId(), null,
                ADMIN_SEMBRADO_ID).valorOLanza();

        var cerrado = pedidos.findById(pedido.getId()).orElseThrow();
        assertThat(cerrado.getEstado()).isEqualTo("cerrado");
        assertThat(cerrado.getCerradoPor()).isEqualTo(ADMIN_SEMBRADO_ID);
    }

    @Test
    void anular_con_otras_lineas_aun_por_cobrar_no_cierra_el_pedido() {
        DetallePedido cobrada = linea("Lomo", 1);
        DetallePedido unaPendiente = linea("Arroz", 1);
        linea("Chicha", 1);
        comandar();
        cobrar.ejecutar(pedido.getId(), List.of(cobrada.getId()), List.of(new PagoSolicitado("efectivo", 500L, null)),
                ADMIN_SEMBRADO_ID).valorOLanza();

        anularLinea.ejecutar(pedido.getId(), unaPendiente.getId(), motivoDeAnulacion("Se fue", false).getId(), null,
                ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(pedidos.findById(pedido.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
    }

    @Test
    void anular_todo_sin_haber_cobrado_nada_deja_el_pedido_abierto_para_terminarlo() {
        DetallePedido unica = linea("Lomo", 1);
        comandar();

        anularLinea.ejecutar(pedido.getId(), unica.getId(), motivoDeAnulacion("Se fue", false).getId(), null,
                ADMIN_SEMBRADO_ID).valorOLanza();

        // Sin nada cobrado no hay venta: el pedido se termina a mano ("Terminar pedido"), no se cierra solo.
        assertThat(pedidos.findById(pedido.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
    }

    static ErrorPedido errorPedido(Resultado<?, ErrorPedido> r) {
        return ((Resultado.Fracaso<?, ErrorPedido>) r).error();
    }
}
