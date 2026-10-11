package pe.rikos.operacion.venta.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;

import static org.assertj.core.api.Assertions.assertThat;

/** US-029: mover el pedido a otra mesa LIBRE; ambas mesas cambian de estado solas (el estado se deriva). */
@SpringBootTest
@Transactional
class MoverPedidoDeMesaTest extends ComandasBase {

    @Autowired
    MoverPedidoDeMesa mover;
    @Autowired
    ConsultaMesas consultaMesas;
    @Autowired
    UnirMesas unirMesas;
    @Autowired
    BloquearMesa bloquearMesa;
    @Autowired
    CambiarEstadoMesa cambiarEstadoMesa;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManager em;

    private EstadoDeMesa estadoDe(String mesaId) {
        return consultaMesas.mapa().valorOLanza().stream().filter(f -> f.mesa().getId().equals(mesaId))
                .findFirst().orElseThrow().estado();
    }

    private String mesaDelPedido() {
        return pedidos().findById(pedido.getId()).orElseThrow().getMesaId();
    }

    @Autowired
    pe.rikos.operacion.venta.infraestructura.PedidoRepositorio pedidosRepositorio;

    private pe.rikos.operacion.venta.infraestructura.PedidoRepositorio pedidos() {
        return pedidosRepositorio;
    }

    @Test
    void el_pedido_pasa_a_la_mesa_libre_y_la_vieja_queda_libre() {
        String vieja = mesaDelPedido();
        String nueva = nuevaMesa("2");
        comandarElPedido();
        assertThat(estadoDe(vieja)).isEqualTo(EstadoDeMesa.OCUPADA);

        mover.ejecutar(pedido.getId(), nueva, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(mesaDelPedido()).isEqualTo(nueva);
        assertThat(estadoDe(nueva)).isEqualTo(EstadoDeMesa.OCUPADA);
        assertThat(estadoDe(vieja)).isEqualTo(EstadoDeMesa.LIBRE);
    }

    @Test
    void el_pedido_conserva_sus_personas_lineas_y_comandas() {
        var chicha = linea("Chicha", 2);
        String nueva = nuevaMesa("2");

        mover.ejecutar(pedido.getId(), nueva, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(personas.findByPedidoIdOrderByOrden(pedido.getId())).hasSize(1);
        assertThat(pedidos().findById(pedido.getId()).orElseThrow().getNumero()).isEqualTo(pedido.getNumero());
        assertThat(chicha.getId()).isNotNull();
    }

    @Test
    void una_mesa_ocupada_bloqueada_inactiva_o_unida_no_recibe_el_pedido() {
        String ocupada = nuevaMesa("2");
        abrirPedido.ejecutar(null, Canal.MESA, ocupada, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        String bloqueada = nuevaMesa("3");
        bloquearMesa.ejecutar(bloqueada, "Rota", ADMIN_SEMBRADO_ID).valorOLanza();
        String inactiva = nuevaMesa("4");
        cambiarEstadoMesa.ejecutar(inactiva, false, ADMIN_SEMBRADO_ID).valorOLanza();
        String unida = nuevaMesa("5");
        unirMesas.ejecutar(unida, nuevaMesa("6"), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(mover.ejecutar(pedido.getId(), ocupada, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.MESA_OCUPADA);
        assertThat(error(mover.ejecutar(pedido.getId(), bloqueada, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_NO_DISPONIBLE);
        assertThat(error(mover.ejecutar(pedido.getId(), inactiva, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_NO_DISPONIBLE);
        assertThat(error(mover.ejecutar(pedido.getId(), unida, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_NO_DISPONIBLE);
    }

    @Test
    void una_mesa_inexistente_o_de_otro_local_no_se_encuentra() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        String deOtroLocal = nuevaMesa("9");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);

        assertThat(error(mover.ejecutar(pedido.getId(), deOtroLocal, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_NO_ENCONTRADA);
        assertThat(error(mover.ejecutar(pedido.getId(), "no-existe", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_NO_ENCONTRADA);
    }

    @Test
    void la_misma_mesa_un_pedido_para_llevar_o_uno_en_cierre_no_se_mueven() {
        String nueva = nuevaMesa("2");
        assertThat(error(mover.ejecutar(pedido.getId(), mesaDelPedido(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MISMO_DESTINO);

        var llevar = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(error(mover.ejecutar(llevar.getId(), nueva, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.CANAL_INVALIDO);

        em.flush();
        jdbc.update("update pedido set estado = 'pendiente_cierre' where id = ?", pedido.getId());
        em.flush();
        em.clear();
        assertThat(error(mover.ejecutar(pedido.getId(), nueva, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PEDIDO_NO_REORGANIZABLE);
    }

    @Test
    void un_pedido_de_otro_local_no_se_mueve() {
        String nueva = nuevaMesa("2");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);

        assertThat(error(mover.ejecutar(pedido.getId(), nueva, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.NO_ENCONTRADO);
    }

    @Test
    void una_mesa_con_mesas_unidas_no_se_deja_atras_al_mover_el_pedido() {
        String vieja = mesaDelPedido();
        unirMesas.ejecutar(nuevaMesa("2"), vieja, ADMIN_SEMBRADO_ID).valorOLanza();
        String nueva = nuevaMesa("3");

        assertThat(error(mover.ejecutar(pedido.getId(), nueva, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.TIENE_MESAS_UNIDAS);
        assertThat(mesaDelPedido()).isEqualTo(vieja);
    }

    @Test
    void el_movimiento_queda_en_la_bitacora_con_las_dos_mesas() {
        String nueva = nuevaMesa("2");

        mover.ejecutar(pedido.getId(), nueva, ADMIN_SEMBRADO_ID).valorOLanza();

        var eventos = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("pedido", pedido.getId());
        var movimiento = eventos.stream().filter(a -> "transferencia".equals(a.getEvento())).findFirst().orElseThrow();
        assertThat(movimiento.getValorNuevo()).contains("\"tipo\":\"cambio_de_mesa\"").contains("\"desde\":\"1\"")
                .contains("\"hacia\":\"2\"");
    }
}
