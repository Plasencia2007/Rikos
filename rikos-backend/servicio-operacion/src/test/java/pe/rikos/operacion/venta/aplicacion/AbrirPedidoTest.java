package pe.rikos.operacion.venta.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import static org.assertj.core.api.Assertions.assertThat;

/** US-028: abrir un pedido en una mesa libre o para llevar. */
@SpringBootTest
@Transactional
class AbrirPedidoTest extends PedidosBase {

    @Autowired
    AbrirPedido abrirPedido;
    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbcDePrueba;
    @Autowired
    jakarta.persistence.EntityManager emDePrueba;
    @Autowired
    CuentaPersonaRepositorio personas;
    @Autowired
    BloquearMesa bloquearMesa;
    @Autowired
    CambiarEstadoMesa cambiarEstadoMesa;

    String localA;
    String localB;
    String mesa;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = nuevoLocal("aa");
        localB = nuevoLocal("ab");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        mesa = nuevaMesa("1");
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    static ErrorPedido error(Resultado<?, ErrorPedido> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorPedido>) resultado).error();
    }

    @Test
    void abre_un_pedido_de_mesa_con_su_servicio_su_numero_y_una_primera_persona() {
        Pedido pedido = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(pedido.getLocalId()).isEqualTo(localA);
        assertThat(pedido.getCanal()).isEqualTo("mesa");
        assertThat(pedido.getMesaId()).isEqualTo(mesa);
        assertThat(pedido.getServicio()).isEqualTo("almuerzo");
        assertThat(pedido.getEstado()).isEqualTo("abierto");
        assertThat(pedido.getNumero()).isEqualTo("0001");
        assertThat(personas.findByPedidoIdOrderByOrden(pedido.getId())).singleElement().satisfies(p -> {
            assertThat(p.getEtiqueta()).isEqualTo("Persona 1");
            assertThat(p.getTipoCliente()).isEqualTo("regular");
        });
    }

    @Test
    void los_numeros_van_en_orden_dentro_del_local_y_del_dia() {
        String otraMesa = nuevaMesa("2");

        Pedido uno = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        Pedido dos = abrirPedido.ejecutar(null, Canal.MESA, otraMesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        Pedido tres = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(uno.getNumero()).isEqualTo("0001");
        assertThat(dos.getNumero()).isEqualTo("0002");
        assertThat(tres.getNumero()).isEqualTo("0003");
    }

    @Test
    void un_pedido_para_llevar_no_lleva_mesa_y_uno_de_mesa_la_exige() {
        assertThat(abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).esExito()).isTrue();
        assertThat(error(abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, mesa, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.CANAL_INVALIDO);
        assertThat(error(abrirPedido.ejecutar(null, Canal.MESA, null, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_REQUERIDA);
    }

    @Test
    void una_mesa_con_pedido_bloqueada_inactiva_o_de_otro_local_no_admite_otro_pedido() {
        Pedido abierto = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        // Abierto sin comandar no ocupa la mesa: quien llega despues entra a ESE pedido, no se crea otro.
        assertThat(abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza().getId())
                .isEqualTo(abierto.getId());
        assertThat(personas.findByPedidoIdOrderByOrden(abierto.getId())).hasSize(1);
        // Comandado (o pendiente de cierre): la mesa esta ocupada.
        emDePrueba.flush();
        jdbcDePrueba.update("update pedido set estado = 'comandado' where id = ?", abierto.getId());
        emDePrueba.clear();
        assertThat(error(abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_OCUPADA);

        String bloqueada = nuevaMesa("2");
        bloquearMesa.ejecutar(bloqueada, "Reservada", ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(error(abrirPedido.ejecutar(null, Canal.MESA, bloqueada, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_NO_DISPONIBLE);

        String inactiva = nuevaMesa("3");
        cambiarEstadoMesa.ejecutar(inactiva, false, ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(error(abrirPedido.ejecutar(null, Canal.MESA, inactiva, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_NO_DISPONIBLE);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        assertThat(error(abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.MESA_NO_ENCONTRADA);
    }

    @Test
    void el_servicio_se_valida_y_sin_horario_vigente_se_pide() {
        assertThat(error(abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "merienda", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.SERVICIO_INVALIDO);
        // El local recien creado no tiene horarios: no hay servicio vigente que deducir.
        assertThat(error(abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.SERVICIO_REQUERIDO);
    }

    @Test
    void enviar_dos_veces_el_mismo_id_no_duplica_y_el_id_de_otro_local_no_se_reutiliza() {
        String id = Identificador.nuevo().valor();

        Pedido primero = abrirPedido.ejecutar(id, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        Pedido repetido = abrirPedido.ejecutar(id, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(repetido.getId()).isEqualTo(primero.getId());
        assertThat(personas.findByPedidoIdOrderByOrden(id)).hasSize(1);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        assertThat(error(abrirPedido.ejecutar(id, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.NO_ENCONTRADO);
    }

    @Test
    void con_comensales_se_crean_tantas_personas_y_se_guarda_el_nombre_del_cliente() {
        Pedido pedido = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", 4, "  Familia García ",
                ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(pedido.getNombreCliente()).isEqualTo("Familia García");
        assertThat(personas.findByPedidoIdOrderByOrden(pedido.getId())).extracting("etiqueta")
                .containsExactly("Persona 1", "Persona 2", "Persona 3", "Persona 4");
    }

    @Test
    void sin_indicar_comensales_ni_nombre_queda_una_persona_y_sin_nombre() {
        Pedido pedido = abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(pedido.getNombreCliente()).isNull();
        assertThat(personas.findByPedidoIdOrderByOrden(pedido.getId())).hasSize(1);
        assertThat(abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", 1, "   ", ADMIN_SEMBRADO_ID)
                .valorOLanza().getNombreCliente()).isNull();
    }

    @Test
    void los_comensales_van_de_1_a_30_y_el_nombre_no_pasa_de_120() {
        assertThat(error(abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", 0, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.COMENSALES_INVALIDOS);
        assertThat(error(abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", 31, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.COMENSALES_INVALIDOS);
        assertThat(error(abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", 1, "x".repeat(121),
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.NOMBRE_DE_CLIENTE_LARGO);
        // Nada quedo a medias: la mesa sigue libre.
        assertThat(abrirPedido.ejecutar(null, Canal.MESA, mesa, "almuerzo", 30, null, ADMIN_SEMBRADO_ID).esExito())
                .isTrue();
    }

    @Test
    void sin_local_activo_no_se_abre_nada() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);

        assertThat(error(abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.SIN_LOCAL_ACTIVO);
    }
}
