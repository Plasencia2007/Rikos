package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** US-029: pasar productos de una persona a otra (o a una persona nueva: dividir la cuenta). */
@SpringBootTest
@Transactional
class TrasladarLineasTest extends ComandasBase {

    @Autowired
    TrasladarLineas trasladar;
    @Autowired
    ComandarPedido comandar;
    @Autowired
    ConsultaPedidos consulta;
    @Autowired
    DetallePedidoRepositorio lineas;
    @Autowired
    PedidoRepositorio pedidos;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    jakarta.persistence.EntityManager em;

    String segunda() {
        agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID);
        return personas.findByPedidoIdOrderByOrden(pedido.getId()).get(1).getId();
    }

    @Test
    void pasa_una_linea_a_otra_persona_y_el_total_del_pedido_no_cambia() {
        String otra = segunda();
        var chicha = linea("Chicha", 2);
        var lomo = linea("Lomo", 1);
        long antes = consulta.pedido(pedido.getId()).valorOLanza().total();

        trasladar.ejecutar(pedido.getId(), List.of(chicha.getId()), otra, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(lineas.findById(chicha.getId()).orElseThrow().getCuentaPersonaId()).isEqualTo(otra);
        assertThat(lineas.findById(lomo.getId()).orElseThrow().getCuentaPersonaId()).isEqualTo(persona);
        var detalle = consulta.pedido(pedido.getId()).valorOLanza();
        assertThat(detalle.total()).isEqualTo(antes);
        assertThat(detalle.personas().get(1).total()).isEqualTo(chicha.getPrecioUnitario() * 2);
        assertThat(detalle.personas().get(0).total()).isEqualTo(lomo.getPrecioUnitario());
    }

    @Test
    void pasa_varias_lineas_juntas() {
        String otra = segunda();
        var uno = linea("Chicha", 1);
        var dos = linea("Lomo", 1);

        trasladar.ejecutar(pedido.getId(), List.of(uno.getId(), dos.getId()), otra, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(lineas.findById(uno.getId()).orElseThrow().getCuentaPersonaId()).isEqualTo(otra);
        assertThat(lineas.findById(dos.getId()).orElseThrow().getCuentaPersonaId()).isEqualTo(otra);
    }

    @Test
    void sin_destino_crea_una_persona_nueva_con_la_etiqueta_y_el_orden_siguientes() {
        var chicha = linea("Chicha", 1);

        trasladar.ejecutar(pedido.getId(), List.of(chicha.getId()), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var todas = personas.findByPedidoIdOrderByOrden(pedido.getId());
        assertThat(todas).extracting("etiqueta").containsExactly("Persona 1", "Persona 2");
        assertThat(todas.get(1).getTipoCliente()).isEqualTo("regular");
        assertThat(lineas.findById(chicha.getId()).orElseThrow().getCuentaPersonaId()).isEqualTo(todas.get(1).getId());
    }

    @Test
    void la_persona_que_queda_sin_lineas_se_conserva() {
        var chicha = linea("Chicha", 1);

        trasladar.ejecutar(pedido.getId(), List.of(chicha.getId()), null, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(personas.findByPedidoIdOrderByOrden(pedido.getId())).hasSize(2);
        assertThat(lineas.findByCuentaPersonaIdInOrderByCreadoEn(List.of(persona))).isEmpty();
    }

    @Test
    void una_linea_ya_comandada_tambien_se_mueve_y_sigue_comandada() {
        String otra = segunda();
        var chicha = linea("Chicha", 1);
        comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        trasladar.ejecutar(pedido.getId(), List.of(chicha.getId()), otra, ADMIN_SEMBRADO_ID).valorOLanza();

        var movida = lineas.findById(chicha.getId()).orElseThrow();
        assertThat(movida.getCuentaPersonaId()).isEqualTo(otra);
        assertThat(movida.getEstado()).isEqualTo("comandado");
    }

    @Test
    void una_lista_vacia_o_un_destino_que_ya_tiene_todo_no_mueve_nada() {
        var chicha = linea("Chicha", 1);

        assertThat(error(trasladar.ejecutar(pedido.getId(), List.of(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.SIN_LINEAS_PARA_MOVER);
        assertThat(error(trasladar.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.SIN_LINEAS_PARA_MOVER);
        assertThat(error(trasladar.ejecutar(pedido.getId(), List.of(chicha.getId()), persona, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MISMO_DESTINO);
    }

    @Test
    void si_una_linea_no_es_valida_no_se_mueve_ninguna() {
        String otra = segunda();
        var buena = linea("Chicha", 1);
        var anulada = linea("Lomo", 1);
        var motivo = motivoDeAnulacion("Error", false);
        em.flush();
        jdbc.update("update detalle_pedido set estado='anulado', anulado_en=now(), anulado_por=?, motivo_anulacion_id=? where id=?",
                ADMIN_SEMBRADO_ID, motivo.getId(), anulada.getId());
        em.flush();
        em.clear();

        assertThat(error(trasladar.ejecutar(pedido.getId(), List.of(buena.getId(), "no-existe"), otra,
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.LINEA_NO_ENCONTRADA);
        assertThat(error(trasladar.ejecutar(pedido.getId(), List.of(buena.getId(), anulada.getId()), otra,
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.LINEA_ANULADA);

        assertThat(lineas.findById(buena.getId()).orElseThrow().getCuentaPersonaId()).isEqualTo(persona);
    }

    @Test
    void un_id_nulo_en_la_lista_no_rompe_y_no_mueve_nada() {
        var chicha = linea("Chicha", 1);

        assertThat(error(trasladar.ejecutar(pedido.getId(), java.util.Arrays.asList(chicha.getId(), null), null,
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.LINEA_NO_ENCONTRADA);
        assertThat(personas.findByPedidoIdOrderByOrden(pedido.getId())).hasSize(1);
    }

    @Test
    void el_destino_tiene_que_ser_una_persona_del_mismo_pedido() {
        var chicha = linea("Chicha", 1);
        var otroPedido = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaAjena = personas.findByPedidoIdOrderByOrden(otroPedido.getId()).get(0).getId();

        assertThat(error(trasladar.ejecutar(pedido.getId(), List.of(chicha.getId()), personaAjena, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PERSONA_NO_ENCONTRADA);
        assertThat(error(trasladar.ejecutar(pedido.getId(), List.of(chicha.getId()), "no-existe", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PERSONA_NO_ENCONTRADA);
    }

    @Test
    void solo_se_reorganiza_antes_del_cierre() {
        String otra = segunda();
        var chicha = linea("Chicha", 1);
        jdbc.update("update pedido set estado = 'pendiente_cierre' where id = ?", pedido.getId());
        em.flush();
        em.clear();

        assertThat(error(trasladar.ejecutar(pedido.getId(), List.of(chicha.getId()), otra, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PEDIDO_NO_REORGANIZABLE);
    }

    @Test
    void un_pedido_de_otro_local_no_se_reorganiza() {
        String otra = segunda();
        var chicha = linea("Chicha", 1);
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);

        assertThat(error(trasladar.ejecutar(pedido.getId(), List.of(chicha.getId()), otra, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.NO_ENCONTRADO);
    }

    @Test
    void el_traslado_queda_en_la_bitacora() {
        String otra = segunda();
        var chicha = linea("Chicha", 1);
        var lomo = linea("Lomo", 1);

        trasladar.ejecutar(pedido.getId(), List.of(chicha.getId(), lomo.getId()), otra, ADMIN_SEMBRADO_ID)
                .valorOLanza();

        var eventos = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("pedido", pedido.getId());
        var traslado = eventos.stream().filter(a -> "transferencia".equals(a.getEvento())).findFirst().orElseThrow();
        assertThat(traslado.getValorNuevo()).contains("\"tipo\":\"traslado_de_lineas\"").contains("\"productos\":2")
                .contains("Persona 1").contains("Persona 2");
    }
}
