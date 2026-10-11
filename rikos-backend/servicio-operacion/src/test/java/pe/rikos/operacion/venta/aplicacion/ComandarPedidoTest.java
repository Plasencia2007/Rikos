package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.venta.infraestructura.Comanda;
import pe.rikos.operacion.venta.infraestructura.ComandaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetalleComandaRepositorio;
import pe.rikos.operacion.venta.infraestructura.DetallePedidoRepositorio;
import pe.rikos.operacion.venta.infraestructura.PedidoRepositorio;

import static org.assertj.core.api.Assertions.assertThat;

/** US-030: comandar manda a cocina las lineas pendientes de todas las personas, en una comanda que es una foto. */
@SpringBootTest
@Transactional
class ComandarPedidoTest extends ComandasBase {

    @Autowired
    ComandarPedido comandar;
    @Autowired
    ComandaRepositorio comandas;
    @Autowired
    DetalleComandaRepositorio detalles;
    @Autowired
    DetallePedidoRepositorio lineas;
    @Autowired
    PedidoRepositorio pedidos;

    @Test
    void comandar_pasa_las_lineas_de_todas_las_personas_a_comandado_y_guarda_la_foto() {
        String otra = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(0).getId();
        agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID);
        String segunda = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(1).getId();
        var chicha = linea(otra, "Chicha", 2);
        var lomo = linea(segunda, "Lomo", 1);

        Comanda comanda = comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(comanda.getNumero()).isEqualTo(1);
        assertThat(comanda.getImpresiones()).isZero();
        assertThat(comanda.getEstadoImpresion()).isEqualTo("pendiente");
        assertThat(lineas.findById(chicha.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
        assertThat(lineas.findById(lomo.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
        var foto = detalles.findByComandaId(comanda.getId());
        assertThat(foto).hasSize(2);
        assertThat(foto.stream().filter(d -> d.getDetallePedidoId().equals(chicha.getId())).findFirst().orElseThrow()
                .getCantidad()).isEqualTo(2);
        assertThat(pedidos.findById(pedido.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
    }

    @Test
    void sin_lineas_pendientes_no_se_puede_comandar() {
        assertThat(error(comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.SIN_LINEAS_PENDIENTES);

        linea("Chicha", 1);
        comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.SIN_LINEAS_PENDIENTES);
    }

    @Test
    void agregar_platos_despues_y_comandar_crea_otra_comanda_solo_con_lo_nuevo() {
        var primera = linea("Chicha", 1);
        Comanda uno = comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();
        var segunda = linea("Lomo", 3);

        Comanda dos = comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(dos.getNumero()).isEqualTo(2);
        assertThat(detalles.findByComandaId(uno.getId())).extracting("detallePedidoId").containsExactly(primera.getId());
        assertThat(detalles.findByComandaId(dos.getId())).extracting("detallePedidoId").containsExactly(segunda.getId());
    }

    @Test
    void reenviar_el_mismo_id_devuelve_la_misma_comanda_sin_duplicar() {
        linea("Chicha", 1);
        String id = Identificador.nuevo().valor();

        Comanda una = comandar.ejecutar(pedido.getId(), id, ADMIN_SEMBRADO_ID).valorOLanza();
        Comanda otra = comandar.ejecutar(pedido.getId(), id, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(una.getId()).isEqualTo(id);
        assertThat(otra.getId()).isEqualTo(id);
        assertThat(comandas.findByPedidoIdOrderByNumero(pedido.getId())).hasSize(1);
    }

    @Test
    void un_id_de_comanda_de_otro_pedido_o_local_no_se_reusa_ni_se_pisa() {
        linea("Chicha", 1);
        Comanda ajena = comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        // Otro pedido del mismo local intenta usar el id de esa comanda.
        var otroPedido = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        String personaDelOtro = personas.findByPedidoIdOrderByOrden(otroPedido.getId()).get(0).getId();
        var producto = productoDeCarta("Gaseosa", 400L);
        agregarLinea.ejecutar(new SolicitudDeLinea(null, otroPedido.getId(), personaDelOtro, TipoDeLinea.PRODUCTO,
                producto.getId(), 1, null, java.util.List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(comandar.ejecutar(otroPedido.getId(), ajena.getId(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.NO_ENCONTRADO);
        assertThat(comandas.findById(ajena.getId()).orElseThrow().getPedidoId()).isEqualTo(pedido.getId());
    }

    @Test
    void un_pedido_de_otro_local_no_se_comanda() {
        linea("Chicha", 1);
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);

        assertThat(error(comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.NO_ENCONTRADO);
    }

    @Test
    void comandar_queda_en_la_bitacora() {
        linea("Chicha", 2);
        linea("Lomo", 1);

        Comanda comanda = comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        var eventos = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("comanda", comanda.getId());
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).getEvento()).isEqualTo("comanda_emitida");
        assertThat(eventos.get(0).getValorNuevo()).contains("\"numero\":1").contains("\"productos\":2");
    }

    @Test
    void una_linea_comandada_ya_no_se_modifica_ni_se_quita() {
        var chicha = linea("Chicha", 1);
        comandar.ejecutar(pedido.getId(), null, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(lineas.findById(chicha.getId()).orElseThrow().getEstado()).isEqualTo("comandado");
    }
}
