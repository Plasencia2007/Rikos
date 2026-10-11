package pe.rikos.operacion.venta.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.venta.infraestructura.DetallePedido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Revision de las entregas 3 y 4: dos escrituras sobre la misma linea no pueden pisarse en silencio (anular contra
 * trasladar, comandar contra modificar). La linea lleva version: la segunda en confirmar choca y se contesta 409.
 */
@SpringBootTest
@Transactional
class VersionDeLineasTest extends ComandasBase {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManager em;

    @Test
    void una_linea_leida_antes_de_que_otro_la_cambie_no_pisa_el_cambio() {
        var chicha = linea("Chicha", 1);
        em.flush();
        em.clear();
        DetallePedido leida = em.find(DetallePedido.class, chicha.getId());

        // "Otro usuario" confirma un cambio sobre la misma linea (sube su version).
        jdbc.update("update detalle_pedido set estado = 'comandado', version = version + 1 where id = ?", chicha.getId());

        leida.cambiar(5, "sin hielo");
        assertThatThrownBy(() -> em.flush()).isInstanceOf(jakarta.persistence.OptimisticLockException.class);
    }

    @Test
    void cada_cambio_de_una_linea_sube_su_version() {
        var chicha = linea("Chicha", 1);
        em.flush();
        int antes = chicha.getVersion();

        chicha.cambiar(2, null);
        em.flush();

        assertThat(chicha.getVersion()).isGreaterThan(antes);
    }
}
