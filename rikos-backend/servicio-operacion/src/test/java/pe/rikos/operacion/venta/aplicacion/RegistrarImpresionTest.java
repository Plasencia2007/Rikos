package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.venta.infraestructura.Comanda;

import static org.assertj.core.api.Assertions.assertThat;

/** US-030 / RN-063: la primera impresion sale limpia; desde la segunda es COPIA y queda en la bitacora. */
@SpringBootTest
@Transactional
class RegistrarImpresionTest extends ComandasBase {

    @Autowired
    ComandarPedido comandar;
    @Autowired
    RegistrarImpresion registrarImpresion;

    private Comanda comandaLista() {
        linea("Chicha", 1);
        return comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
    }

    @Test
    void la_primera_impresion_no_es_copia_y_deja_la_comanda_impresa() {
        Comanda comanda = comandaLista();

        var impresion = registrarImpresion.ejecutar(comanda.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(impresion.copia()).isFalse();
        assertThat(impresion.comanda().getImpresiones()).isEqualTo(1);
        assertThat(impresion.comanda().getEstadoImpresion()).isEqualTo("impresa");
        assertThat(impresion.comanda().getUltimaImpresionEn()).isNotNull();
        assertThat(auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("comanda", comanda.getId()))
                .extracting("evento").doesNotContain("reimpresion_comanda");
    }

    @Test
    void desde_la_segunda_impresion_es_copia_y_se_registra_en_la_bitacora() {
        Comanda comanda = comandaLista();
        registrarImpresion.ejecutar(comanda.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        var segunda = registrarImpresion.ejecutar(comanda.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        // Es la misma entidad refrescada: se lee el contador en el momento, antes de la siguiente impresion.
        int contadorDeLaSegunda = segunda.comanda().getImpresiones();
        var tercera = registrarImpresion.ejecutar(comanda.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(segunda.copia()).isTrue();
        assertThat(contadorDeLaSegunda).isEqualTo(2);
        assertThat(tercera.copia()).isTrue();
        assertThat(tercera.comanda().getImpresiones()).isEqualTo(3);
        assertThat(auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("comanda", comanda.getId()))
                .filteredOn(a -> "reimpresion_comanda".equals(a.getEvento())).hasSize(2);
    }

    @Test
    void una_comanda_de_otro_local_o_inexistente_no_se_imprime() {
        Comanda comanda = comandaLista();

        assertThat(error(registrarImpresion.ejecutar("no-existe", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.COMANDA_NO_ENCONTRADA);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        assertThat(error(registrarImpresion.ejecutar(comanda.getId(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.COMANDA_NO_ENCONTRADA);
    }
}
