package pe.rikos.operacion.caja.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.caja.infraestructura.TurnoCajaRepositorio;
import pe.rikos.operacion.venta.aplicacion.ComandasBase;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * US-036 (sprint 7, entrega 4): cerrar el turno. El cajero ve el efectivo esperado ANTES de contar (RN-067), cuenta, y el
 * sistema calcula la diferencia: desde S/ 0,01 alerta, pero nunca bloquea el cierre (RN-070). El cierre queda inalterable.
 */
@SpringBootTest
@Transactional
class CerrarTurnoTest extends ComandasBase {

    @Autowired
    CobrarPedido cobrar;
    @Autowired
    AbrirTurno abrirTurno;
    @Autowired
    CerrarTurno cerrarTurno;
    @Autowired
    ConsultaCierre consultaCierre;
    @Autowired
    ConsultaCajas consultaCajas;
    @Autowired
    TurnoCajaRepositorio turnos;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManager em;

    TurnoCaja turno;

    @BeforeEach
    void abrir() {
        var caja = consultaCajas.listar().valorOLanza().get(0).caja();
        turno = abrirTurno.ejecutar(caja.getId(), 20000L, ADMIN_SEMBRADO_ID).valorOLanza();
    }

    static ErrorCaja error(Resultado<?, ErrorCaja> resultado) {
        assertThat(resultado.esFracaso()).as("se esperaba un fracaso").isTrue();
        return ((Resultado.Fracaso<?, ErrorCaja>) resultado).error();
    }

    /** Cobra una linea de la cantidad dada (precio 500) pagando la lista de pagos. */
    private void cobrarLinea(int cantidad, PagoSolicitado... pagos) {
        DetallePedido l = linea("Plato " + cantidad, cantidad);
        comandarPedido.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        cobrar.ejecutar(pedido.getId(), List.of(l.getId()), List.of(pagos), ADMIN_SEMBRADO_ID).valorOLanza();
    }

    @Test
    void el_resumen_previo_muestra_el_efectivo_esperado_antes_de_contar() {
        // 2 platos = S/ 10: S/ 4 en yape y el resto en efectivo (recibe S/ 10 y da S/ 4 de vuelto).
        cobrarLinea(2, new PagoSolicitado("yape", 400L, "OP-1"), new PagoSolicitado("efectivo", 1000L, null));

        var resumen = consultaCierre.previo(ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(resumen.fondoInicial()).isEqualTo(20000L);
        assertThat(resumen.efectivoCobrado()).isEqualTo(600L);
        assertThat(resumen.efectivoEsperado()).isEqualTo(20600L);
        assertThat(resumen.totalYape()).isEqualTo(400L);
        assertThat(resumen.totalPlin()).isZero();
        assertThat(resumen.totalTarjeta()).isZero();
        assertThat(resumen.cobros()).isEqualTo(1);
    }

    @Test
    void un_conteo_exacto_cierra_el_turno_sin_diferencia_ni_alerta() {
        cobrarLinea(2, new PagoSolicitado("efectivo", 1000L, null));

        var cierre = cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, 21000L, "todo bien").valorOLanza();

        assertThat(cierre.cierre().getEfectivoEsperado()).isEqualTo(21000L);
        assertThat(cierre.cierre().getEfectivoContado()).isEqualTo(21000L);
        assertThat(cierre.cierre().getDiferencia()).isZero();
        assertThat(cierre.alerta()).isFalse();
        assertThat(cierre.cierre().getObservacion()).isEqualTo("todo bien");
        TurnoCaja cerrado = turnos.findById(turno.getId()).orElseThrow();
        assertThat(cerrado.getEstado()).isEqualTo("cerrado");
        assertThat(cerrado.getCerradoEn()).isNotNull();
    }

    @Test
    void una_diferencia_de_un_centimo_alerta_pero_deja_cerrar() {
        cobrarLinea(1, new PagoSolicitado("efectivo", 500L, null));

        var faltan = cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, 20499L, null).valorOLanza();

        assertThat(faltan.cierre().getDiferencia()).isEqualTo(-1L);
        assertThat(faltan.alerta()).isTrue();
        assertThat(turnos.findById(turno.getId()).orElseThrow().getEstado()).isEqualTo("cerrado");
    }

    @Test
    void una_diferencia_a_favor_tambien_alerta() {
        cobrarLinea(1, new PagoSolicitado("efectivo", 500L, null));

        var sobra = cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, 20700L, "sobraron 2 soles").valorOLanza();

        assertThat(sobra.cierre().getDiferencia()).isEqualTo(200L);
        assertThat(sobra.alerta()).isTrue();
    }

    @Test
    void el_cierre_guarda_los_totales_digitales_por_medio() {
        cobrarLinea(4, new PagoSolicitado("yape", 700L, null), new PagoSolicitado("plin", 600L, null),
                new PagoSolicitado("tarjeta", 500L, null), new PagoSolicitado("efectivo", 200L, null));

        var cierre = cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, 20200L, null).valorOLanza().cierre();

        assertThat(cierre.getTotalYape()).isEqualTo(700L);
        assertThat(cierre.getTotalPlin()).isEqualTo(600L);
        assertThat(cierre.getTotalTarjeta()).isEqualTo(500L);
        assertThat(cierre.getEfectivoEsperado()).isEqualTo(20200L);
    }

    @Test
    void con_el_turno_cerrado_no_se_cobra_mas_y_la_caja_puede_abrir_otro() {
        cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, 20000L, null).valorOLanza();
        DetallePedido l = linea("Plato", 1);
        comandarPedido.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var intento = cobrar.ejecutar(pedido.getId(), List.of(l.getId()), List.of(new PagoSolicitado("efectivo", 500L, null)),
                ADMIN_SEMBRADO_ID);

        assertThat(((Resultado.Fracaso<?, ErrorCobro>) intento).error()).isEqualTo(ErrorCobro.SIN_TURNO_ABIERTO);
        assertThat(abrirTurno.ejecutar(turno.getCajaId(), 5000L, ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }

    @Test
    void sin_turno_o_con_un_conteo_invalido_no_se_cierra() {
        assertThat(error(cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, -1L, null))).isEqualTo(ErrorCaja.CONTADO_INVALIDO);
        assertThat(error(cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, null, null))).isEqualTo(ErrorCaja.CONTADO_INVALIDO);
        assertThat(error(cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, 100L, "x".repeat(501)))).isEqualTo(ErrorCaja.OBSERVACION_LARGA);
        assertThat(turnos.findById(turno.getId()).orElseThrow().getEstado()).isEqualTo("abierto");
        cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, 20000L, null).valorOLanza();
        assertThat(error(cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, 20000L, null))).isEqualTo(ErrorCaja.SIN_TURNO_ABIERTO);
        assertThat(error(consultaCierre.previo(ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.SIN_TURNO_ABIERTO);
    }

    @Test
    void el_cierre_es_inalterable_incluso_desde_sql() {
        var cierre = cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, 20000L, null).valorOLanza().cierre();
        em.flush();

        assertThatThrownBy(() -> jdbc.update("UPDATE cierre_caja SET efectivo_contado = 1 WHERE id = ?", cierre.getId()))
                .hasMessageContaining("inalterable");
    }

    @Test
    void el_cierre_deja_traza_en_la_bitacora() {
        cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, 19000L, "faltan 10").valorOLanza();

        var eventos = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("turno_caja", turno.getId());
        assertThat(eventos).extracting(e -> e.getEvento()).contains("cierre_caja");
        var e = eventos.stream().filter(x -> x.getEvento().equals("cierre_caja")).findFirst().orElseThrow();
        assertThat(e.getValorNuevo()).contains("20000").contains("19000").contains("-1000");
    }

    @Test
    void el_cierre_se_consulta_despues() {
        var cierre = cerrarTurno.ejecutar(ADMIN_SEMBRADO_ID, 20000L, null).valorOLanza().cierre();

        assertThat(consultaCierre.deTurno(turno.getId()).valorOLanza().getId()).isEqualTo(cierre.getId());
        assertThat(error(consultaCierre.deTurno("no-existe"))).isEqualTo(ErrorCaja.CAJA_NO_ENCONTRADA);
    }
}
