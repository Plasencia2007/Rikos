package pe.rikos.operacion.venta.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Agregar dos veces lo mismo a la misma persona (2026-10-29): en vez de dos lineas iguales, la que ya estaba por enviar
 * sube su cantidad. Solo se junta lo que es identico y todavia no fue a cocina: el mismo producto, sin nota y al mismo
 * precio. Lo comandado, lo que lleva nota y lo de otra persona queda en otra linea.
 */
@SpringBootTest
@Transactional
class JuntarLineasIgualesTest extends ComandasBase {

    @Autowired
    DetallePedidoRepositorio lineas;
    @Autowired
    EntityManager em;

    private DetallePedido agregar(String personaId, String productoId, int cantidad, String nota) {
        return agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), personaId, TipoDeLinea.PRODUCTO,
                productoId, cantidad, nota, List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();
    }

    private List<DetallePedido> lineasDe(String personaId) {
        em.flush();
        em.clear();
        return lineas.findByCuentaPersonaIdInOrderByCreadoEn(List.of(personaId));
    }

    @Test
    void el_mismo_producto_dos_veces_queda_en_una_linea_con_cantidad_dos() {
        var huevo = productoDeCarta("Huevo frito", 200L);

        var primera = agregar(persona, huevo.getId(), 1, null);
        var segunda = agregar(persona, huevo.getId(), 1, null);

        assertThat(segunda.getId()).isEqualTo(primera.getId());
        assertThat(lineasDe(persona)).singleElement().satisfies(l -> {
            assertThat(l.getCantidad()).isEqualTo(2);
            assertThat(l.getPrecioUnitario()).isEqualTo(200L);
        });
    }

    @Test
    void se_suma_la_cantidad_que_se_agrega() {
        var cafe = productoDeCarta("Cafe pasado", 400L);

        agregar(persona, cafe.getId(), 2, null);
        agregar(persona, cafe.getId(), 3, null);

        assertThat(lineasDe(persona)).singleElement().extracting(DetallePedido::getCantidad).isEqualTo(5);
    }

    @Test
    void con_nota_para_cocina_va_en_otra_linea() {
        var huevo = productoDeCarta("Huevo frito", 200L);

        agregar(persona, huevo.getId(), 1, null);
        agregar(persona, huevo.getId(), 1, "Bien cocido");

        assertThat(lineasDe(persona)).hasSize(2);
    }

    @Test
    void lo_que_ya_fue_a_cocina_no_se_junta_con_lo_nuevo() {
        var huevo = productoDeCarta("Huevo frito", 200L);
        agregar(persona, huevo.getId(), 1, null);
        comandarPedido.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        agregar(persona, huevo.getId(), 1, null);

        assertThat(lineasDe(persona)).extracting(DetallePedido::getEstado, DetallePedido::getCantidad)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("comandado", 1),
                        org.assertj.core.groups.Tuple.tuple("pendiente", 1));
    }

    @Test
    void cada_persona_tiene_su_propia_linea() {
        var huevo = productoDeCarta("Huevo frito", 200L);
        String otra = agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID).valorOLanza().getId();

        agregar(persona, huevo.getId(), 1, null);
        agregar(otra, huevo.getId(), 1, null);

        assertThat(lineasDe(persona)).hasSize(1);
        assertThat(lineasDe(otra)).hasSize(1);
    }
}
