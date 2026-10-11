package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;

import static org.assertj.core.api.Assertions.assertThat;

/** US-030: la lista de comandas de un pedido y el detalle imprimible (la foto de lo que se mando). */
@SpringBootTest
@Transactional
class ConsultaDeComandasTest extends ComandasBase {

    @Autowired
    ComandarPedido comandar;
    @Autowired
    ConsultaComandas consulta;
    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired
    jakarta.persistence.EntityManager em;

    @Test
    void la_lista_de_un_pedido_sale_ordenada_por_numero() {
        linea("Chicha", 1);
        comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        linea("Lomo", 1);
        comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var lista = consulta.delPedido(pedido.getId()).valorOLanza();

        assertThat(lista).extracting("numero").containsExactly(1, 2);
    }

    @Test
    void el_detalle_trae_pedido_mesa_mozo_y_los_productos_por_persona_con_su_nota() {
        agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID);
        String segunda = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(1).getId();
        var chicha = productoDeCarta("Chicha", 500L);
        agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), persona, TipoDeLinea.PRODUCTO,
                chicha.getId(), 2, "sin hielo", java.util.List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();
        linea(segunda, "Lomo", 1);
        var comanda = comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var detalle = consulta.detalle(comanda.getId()).valorOLanza();

        assertThat(detalle.pedidoNumero()).isEqualTo(pedido.getNumero());
        assertThat(detalle.canal()).isEqualTo("mesa");
        assertThat(detalle.mesa()).isEqualTo("1");
        assertThat(detalle.servicio()).isEqualTo("almuerzo");
        assertThat(detalle.mozo()).isNotBlank();
        assertThat(detalle.personas()).extracting("etiqueta").containsExactly("Persona 1", "Persona 2");
        var primera = detalle.personas().get(0).lineas().get(0);
        assertThat(primera.cantidad()).isEqualTo(2);
        assertThat(primera.nombre()).startsWith("Chicha");
        assertThat(primera.observacion()).isEqualTo("sin hielo");
    }

    @Test
    void la_comanda_de_un_pedido_para_llevar_no_tiene_mesa() {
        var llevar = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaDelLlevar = personas.findByPedidoIdOrderByOrden(llevar.getId()).get(0).getId();
        var producto = productoDeCarta("Gaseosa", 400L);
        agregarLinea.ejecutar(new SolicitudDeLinea(null, llevar.getId(), personaDelLlevar, TipoDeLinea.PRODUCTO,
                producto.getId(), 1, null, java.util.List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();
        var comanda = comandar.ejecutar(llevar.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var detalle = consulta.detalle(comanda.getId()).valorOLanza();

        assertThat(detalle.canal()).isEqualTo("para_llevar");
        assertThat(detalle.mesa()).isNull();
    }

    @Test
    void la_comanda_conserva_la_etiqueta_de_la_persona_aunque_la_linea_cambie_de_persona() {
        agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID);
        String segunda = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(1).getId();
        var chicha = linea("Chicha", 1);
        var comanda = comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        // La linea pasa a otra persona despues de comandar (lo que hara trasladar): la foto no cambia.
        jdbc.update("update detalle_pedido set cuenta_persona_id = ? where id = ?", segunda, chicha.getId());
        em.flush();
        em.clear();

        var detalle = consulta.detalle(comanda.getId()).valorOLanza();
        assertThat(detalle.personas()).extracting("etiqueta").containsExactly("Persona 1");
        assertThat(detalle.personas().get(0).lineas()).hasSize(1);
    }

    @Test
    void solo_se_ve_una_comanda_del_local_de_la_sesion() {
        linea("Chicha", 1);
        var comanda = comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);

        assertThat(error(consulta.detalle(comanda.getId()))).isEqualTo(ErrorPedido.COMANDA_NO_ENCONTRADA);
        assertThat(error(consulta.delPedido(pedido.getId()))).isEqualTo(ErrorPedido.NO_ENCONTRADO);
    }
}
