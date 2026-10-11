package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.catalogo.aplicacion.MarcarAgotado;
import pe.rikos.operacion.venta.infraestructura.Comanda;
import pe.rikos.operacion.venta.infraestructura.ComandaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetalleComandaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-04 CA2 (backlog v3): marcar un producto como agotado oculta el producto en los pedidos NUEVOS, pero no modifica ni
 * cancela las comandas que ya lo incluyen.
 */
@SpringBootTest
@Transactional
class ComandasIntactasAlAgotarTest extends ComandasBase {

    @Autowired
    MarcarAgotado marcarAgotado;
    @Autowired
    ComandaRepositorio comandas;
    @Autowired
    DetalleComandaRepositorio detalles;
    @Autowired
    DetallePedidoRepositorio lineas;
    @Autowired
    ConsultaPedidos consulta;

    @Test
    void agotar_un_producto_ya_comandado_no_toca_la_comanda_ni_la_linea() {
        var lomo = linea(persona, "Lomo agotable", 2);
        Comanda comanda = comandarPedido.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        int impresionesAntes = comanda.getImpresiones();
        var fotoAntes = detalles.findByComandaId(comanda.getId());

        marcarAgotado.ejecutar(lomo.getProductoId()).valorOLanza();

        var comandaDespues = comandas.findById(comanda.getId()).orElseThrow();
        var fotoDespues = detalles.findByComandaId(comanda.getId());
        assertThat(comandaDespues.getNumero()).isEqualTo(comanda.getNumero());
        assertThat(comandaDespues.getImpresiones()).isEqualTo(impresionesAntes);
        assertThat(fotoDespues).hasSameSizeAs(fotoAntes);
        assertThat(fotoDespues.get(0).getCantidad()).isEqualTo(2);
        var lineaDespues = lineas.findById(lomo.getId()).orElseThrow();
        assertThat(lineaDespues.getEstado()).isEqualTo("comandado");
        assertThat(lineaDespues.getCantidad()).isEqualTo(2);
    }

    @Test
    void el_producto_agotado_deja_de_ofrecerse_en_pedidos_nuevos() {
        var lomo = linea(persona, "Lomo oculto", 1);
        comandarPedido.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        marcarAgotado.ejecutar(lomo.getProductoId()).valorOLanza();

        assertThat(consulta.oferta(pedido.getId()).valorOLanza().carta())
                .extracting(ProductoOfrecido::id).doesNotContain(lomo.getProductoId());
    }
}
