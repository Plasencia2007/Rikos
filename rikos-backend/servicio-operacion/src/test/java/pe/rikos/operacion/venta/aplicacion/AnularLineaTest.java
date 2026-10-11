package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;

import static org.assertj.core.api.Assertions.assertThat;

/** US-031: anular con motivo, antes o despues de comandar. Lo anulado no suma pero sigue a la vista. */
@SpringBootTest
@Transactional
class AnularLineaTest extends ComandasBase {

    @Autowired
    ComandarPedido comandar;
    @Autowired
    AnularLinea anular;
    @Autowired
    ConsultaPedidos consulta;
    @Autowired
    DetallePedidoRepositorio lineas;

    @Test
    void anular_una_comandada_queda_marcada_despues_de_comandar_y_no_suma() {
        var chicha = linea("Chicha", 2);
        var lomo = linea("Lomo", 1);
        comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        var motivo = motivoDeAnulacion("Error del mozo", false);

        anular.ejecutar(pedido.getId(), chicha.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var guardada = lineas.findById(chicha.getId()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo("anulado");
        assertThat(guardada.isAnuladoPostComanda()).isTrue();
        assertThat(guardada.getMotivoAnulacionId()).isEqualTo(motivo.getId());
        assertThat(guardada.getAnuladoPor()).isEqualTo(ADMIN_SEMBRADO_ID);
        assertThat(guardada.getAnuladoEn()).isNotNull();
        var detalle = consulta.pedido(pedido.getId()).valorOLanza();
        assertThat(detalle.total()).isEqualTo(lomo.getPrecioUnitario() * lomo.getCantidad());
        var vista = detalle.personas().get(0).lineas().stream()
                .filter(l -> l.linea().getId().equals(chicha.getId())).findFirst().orElseThrow();
        assertThat(vista.motivoAnulacion()).isEqualTo(motivo.getDescripcion());
    }

    @Test
    void anular_una_pendiente_no_queda_como_despues_de_comandar() {
        var chicha = linea("Chicha", 1);
        var motivo = motivoDeAnulacion("Cliente se retiró", false);

        anular.ejecutar(pedido.getId(), chicha.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var guardada = lineas.findById(chicha.getId()).orElseThrow();
        assertThat(guardada.getEstado()).isEqualTo("anulado");
        assertThat(guardada.isAnuladoPostComanda()).isFalse();
    }

    @Test
    void con_todo_anulado_el_pedido_queda_en_cero_sin_fallar() {
        var chicha = linea("Chicha", 1);
        var motivo = motivoDeAnulacion("Error del mozo", false);
        anular.ejecutar(pedido.getId(), chicha.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var detalle = consulta.pedido(pedido.getId()).valorOLanza();

        assertThat(detalle.total()).isZero();
        assertThat(detalle.personas().get(0).total()).isZero();
        assertThat(detalle.personas().get(0).lineas()).hasSize(1);
    }

    @Test
    void una_linea_ya_anulada_no_se_anula_otra_vez() {
        var chicha = linea("Chicha", 1);
        var motivo = motivoDeAnulacion("Error del mozo", false);
        anular.ejecutar(pedido.getId(), chicha.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(anular.ejecutar(pedido.getId(), chicha.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.LINEA_ANULADA);
    }

    @Test
    void el_motivo_tiene_que_existir_estar_activo_y_ser_de_anulacion() {
        var chicha = linea("Chicha", 1);
        var deDescuento = motivos.save(new pe.rikos.operacion.catalogo.infraestructura.Motivo(
                pe.rikos.contratos.Identificador.nuevo().valor(), "descuento", "Promo", false));
        var inactivo = motivoDeAnulacion("Viejo", false);
        inactivo.desactivar();
        motivos.save(inactivo);

        assertThat(error(anular.ejecutar(pedido.getId(), chicha.getId(), "no-existe", null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MOTIVO_NO_ENCONTRADO);
        assertThat(error(anular.ejecutar(pedido.getId(), chicha.getId(), null, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MOTIVO_NO_ENCONTRADO);
        assertThat(error(anular.ejecutar(pedido.getId(), chicha.getId(), deDescuento.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MOTIVO_INVALIDO);
        assertThat(error(anular.ejecutar(pedido.getId(), chicha.getId(), inactivo.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MOTIVO_INVALIDO);
        assertThat(lineas.findById(chicha.getId()).orElseThrow().getEstado()).isEqualTo("pendiente");
    }

    @Test
    void un_motivo_que_exige_observacion_no_se_acepta_sin_ella() {
        var chicha = linea("Chicha", 1);
        var motivo = motivoDeAnulacion("Otro", true);

        assertThat(error(anular.ejecutar(pedido.getId(), chicha.getId(), motivo.getId(), "  ", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.OBSERVACION_OBLIGATORIA);

        anular.ejecutar(pedido.getId(), chicha.getId(), motivo.getId(), "Se cayó el plato", ADMIN_SEMBRADO_ID)
                .valorOLanza();
        assertThat(lineas.findById(chicha.getId()).orElseThrow().getObservacionAnulacion())
                .isEqualTo("Se cayó el plato");
    }

    @Test
    void una_linea_de_otro_pedido_o_de_otro_local_no_se_encuentra() {
        var chicha = linea("Chicha", 1);
        var motivo = motivoDeAnulacion("Error del mozo", false);

        assertThat(error(anular.ejecutar(pedido.getId(), "no-existe", motivo.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.LINEA_NO_ENCONTRADA);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        assertThat(error(anular.ejecutar(pedido.getId(), chicha.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.NO_ENCONTRADO);
    }

    @Test
    void la_anulacion_queda_en_la_bitacora_con_el_motivo() {
        var chicha = linea("Chicha", 1);
        comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        var motivo = motivoDeAnulacion("Error del mozo", false);

        anular.ejecutar(pedido.getId(), chicha.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var eventos = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("detalle_pedido", chicha.getId());
        var anulacion = eventos.stream().filter(a -> "anulacion".equals(a.getEvento())).findFirst().orElseThrow();
        assertThat(anulacion.getMotivoId()).isEqualTo(motivo.getId());
        assertThat(anulacion.getValorNuevo()).contains("\"despuesDeComandar\":true");
    }

    @Test
    void una_linea_anulada_no_cuenta_como_segundo_consumo_del_dia() {
        var plan = nuevoPlan(500, 800, 800);
        var pensionista = nuevoPensionista(plan, "Anula");
        // La cuenta del segundo consumo ya excluye lo anulado (US-028); aqui solo se comprueba con una anulada real.
        var chicha = linea("Chicha", 1);
        var motivo = motivoDeAnulacion("Error del mozo", false);
        anular.ejecutar(pedido.getId(), chicha.getId(), motivo.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(lineas.consumosDelPlanDelDia(pensionista.getId(), java.time.LocalDate.now(), "almuerzo")).isZero();
    }
}
