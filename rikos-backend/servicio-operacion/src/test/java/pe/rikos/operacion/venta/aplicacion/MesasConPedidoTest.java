package pe.rikos.operacion.venta.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import static org.assertj.core.api.Assertions.assertThat;

/** La mesa deja de estar "libre" en cuanto tiene un pedido en curso, y una mesa con pedido no se bloquea ni se desactiva. */
@SpringBootTest
@Transactional
class MesasConPedidoTest extends PedidosBase {

    @Autowired
    ConsultaMesas consultaMesas;
    @Autowired
    AbrirPedido abrirPedido;
    @Autowired
    BloquearMesa bloquearMesa;
    @Autowired
    CambiarEstadoMesa cambiarEstadoMesa;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManager em;

    String mesa;
    String otraMesa;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        String local = nuevoLocal("mp");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, local);
        mesa = nuevaMesa("1");
        otraMesa = nuevaMesa("2");
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private EstadoDeMesa estadoDe(String mesaId) {
        return consultaMesas.mapa().valorOLanza().stream().filter(f -> f.mesa().getId().equals(mesaId))
                .findFirst().orElseThrow().estado();
    }

    private static ErrorMesa error(Resultado<?, ErrorMesa> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorMesa>) resultado).error();
    }

    private void pasarAComandado(Pedido pedido) {
        em.flush();
        jdbc.update("update pedido set estado = 'comandado' where id = ?", pedido.getId());
        em.clear();
    }

    private boolean tomandoPedido(String mesaId) {
        return consultaMesas.mapa().valorOLanza().stream().filter(f -> f.mesa().getId().equals(mesaId))
                .findFirst().orElseThrow().tomandoPedido();
    }

    @Test
    void una_mesa_con_pedido_solo_abierto_sigue_libre_pero_marcada_tomando_pedido() {
        assertThat(estadoDe(mesa)).isEqualTo(EstadoDeMesa.LIBRE);
        assertThat(tomandoPedido(mesa)).isFalse();

        abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(estadoDe(mesa)).isEqualTo(EstadoDeMesa.LIBRE);
        assertThat(tomandoPedido(mesa)).isTrue();
        assertThat(estadoDe(otraMesa)).isEqualTo(EstadoDeMesa.LIBRE);
        assertThat(tomandoPedido(otraMesa)).isFalse();
    }

    @Test
    void una_mesa_solo_esta_ocupada_cuando_su_pedido_se_comanda() {
        Pedido pedido = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        pasarAComandado(pedido);

        assertThat(estadoDe(mesa)).isEqualTo(EstadoDeMesa.OCUPADA);
        assertThat(tomandoPedido(mesa)).isFalse();
    }

    @Test
    void la_marca_tomando_pedido_solo_va_en_una_mesa_libre() {
        Pedido pedido = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        em.flush();
        jdbc.update("update pedido set estado = 'pendiente_cierre' where id = ?", pedido.getId());
        em.clear();

        assertThat(estadoDe(mesa)).isEqualTo(EstadoDeMesa.PENDIENTE_CIERRE);
        assertThat(tomandoPedido(mesa)).isFalse();
    }

    @Test
    void la_mesa_ocupada_trae_la_hora_en_que_se_abrio_su_pedido_y_las_libres_no() {
        Pedido pedido = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        var filas = consultaMesas.mapa().valorOLanza();
        var ocupada = filas.stream().filter(f -> f.mesa().getId().equals(mesa)).findFirst().orElseThrow();
        var libre = filas.stream().filter(f -> f.mesa().getId().equals(otraMesa)).findFirst().orElseThrow();

        assertThat(ocupada.abiertaEn()).isEqualTo(pedido.getAbiertoEn());
        assertThat(libre.abiertaEn()).isNull();
    }

    @Test
    void un_pedido_para_llevar_no_ocupa_ninguna_mesa() {
        abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(estadoDe(mesa)).isEqualTo(EstadoDeMesa.LIBRE);
        assertThat(estadoDe(otraMesa)).isEqualTo(EstadoDeMesa.LIBRE);
    }

    @Test
    void una_mesa_con_un_pedido_pendiente_de_cierre_sale_pendiente_de_cierre() {
        Pedido pedido = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        em.flush();
        jdbc.update("update pedido set estado = 'pendiente_cierre' where id = ?", pedido.getId());
        em.clear();

        assertThat(estadoDe(mesa)).isEqualTo(EstadoDeMesa.PENDIENTE_CIERRE);
    }

    @Test
    void una_mesa_con_pedido_abierto_sin_comandar_no_se_bloquea_ni_se_desactiva_hasta_terminar_el_pedido() {
        abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(bloquearMesa.ejecutar(mesa, "Reservada", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.TIENE_PEDIDO_ABIERTO);
        assertThat(error(cambiarEstadoMesa.ejecutar(mesa, false, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorMesa.TIENE_PEDIDO_ABIERTO);
        // Activar una mesa que ya esta activa sigue siendo un no-op sin error.
        assertThat(cambiarEstadoMesa.ejecutar(mesa, true, ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }

    @Test
    void una_mesa_con_pedido_comandado_no_se_bloquea_ni_se_desactiva() {
        Pedido pedido = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        pasarAComandado(pedido);

        assertThat(error(bloquearMesa.ejecutar(mesa, "Reservada", ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.MESA_NO_LIBRE);
        assertThat(error(cambiarEstadoMesa.ejecutar(mesa, false, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorMesa.MESA_NO_LIBRE);
    }

    @Test
    void al_no_haber_pedido_en_curso_la_mesa_vuelve_a_libre() {
        Pedido pedido = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        em.flush();
        jdbc.update("delete from cuenta_persona where pedido_id = ?", pedido.getId());
        jdbc.update("delete from pedido where id = ?", pedido.getId());
        em.clear();

        assertThat(estadoDe(mesa)).isEqualTo(EstadoDeMesa.LIBRE);
        assertThat(bloquearMesa.ejecutar(mesa, "Reservada", ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }
}
