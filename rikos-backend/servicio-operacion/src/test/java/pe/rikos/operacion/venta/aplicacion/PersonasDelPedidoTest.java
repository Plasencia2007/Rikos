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
import pe.rikos.operacion.pensionistas.aplicacion.CambiarEstadoPensionista;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import static org.assertj.core.api.Assertions.assertThat;

/** US-028 CA1 y CA2: personas de la mesa, clientes normales y pensionistas (buscados por nombre o DNI). */
@SpringBootTest
@Transactional
class PersonasDelPedidoTest extends PedidosBase {

    @Autowired
    AbrirPedido abrirPedido;
    @Autowired
    AgregarPersona agregarPersona;
    @Autowired
    AsignarPensionista asignarPensionista;
    @Autowired
    QuitarPersona quitarPersona;
    @Autowired
    CambiarEstadoPensionista cambiarEstadoPensionista;
    @Autowired
    CuentaPersonaRepositorio personas;

    String localA;
    String localB;
    Pedido pedido;
    String primeraPersona;
    PlanTarifario plan;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = nuevoLocal("pa");
        localB = nuevoLocal("pb");
        plan = nuevoPlan(500, 800, 800);
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        pedido = abrirPedido.ejecutar(null, Canal.MESA, nuevaMesa("1"), "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        primeraPersona = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(0).getId();
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
    void agrega_personas_numeradas_o_con_el_nombre_que_se_de() {
        CuentaPersona dos = agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID).valorOLanza();
        CuentaPersona tres = agregarPersona.ejecutar(pedido.getId(), null, "  Sra. Rosa ", ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(dos.getEtiqueta()).isEqualTo("Persona 2");
        assertThat(tres.getEtiqueta()).isEqualTo("Sra. Rosa");
        assertThat(personas.findByPedidoIdOrderByOrden(pedido.getId())).extracting(CuentaPersona::getOrden)
                .containsExactly(1, 2, 3);
    }

    @Test
    void enviar_dos_veces_el_mismo_id_de_persona_no_la_duplica() {
        String id = Identificador.nuevo().valor();

        agregarPersona.ejecutar(pedido.getId(), id, null, ADMIN_SEMBRADO_ID).valorOLanza();
        agregarPersona.ejecutar(pedido.getId(), id, null, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(personas.findByPedidoIdOrderByOrden(pedido.getId())).hasSize(2);
    }

    @Test
    void asigna_un_pensionista_a_una_persona_y_la_etiqueta_pasa_a_ser_su_nombre() {
        Pensionista luis = nuevoPensionista(plan, "Quispe");

        CuentaPersona persona = asignarPensionista.ejecutar(pedido.getId(), primeraPersona, luis.getId(),
                ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(persona.getTipoCliente()).isEqualTo("pensionista");
        assertThat(persona.getPensionistaId()).isEqualTo(luis.getId());
        assertThat(persona.getEtiqueta()).isEqualTo(luis.nombreCompleto());
    }

    @Test
    void un_pensionista_inactivo_o_inexistente_no_se_asigna_ni_se_repite_en_el_pedido() {
        Pensionista luis = nuevoPensionista(plan, "Quispe");
        Pensionista inactivo = nuevoPensionista(plan, "Inactivo");
        cambiarEstadoPensionista.ejecutar(inactivo.getId(), false, ADMIN_SEMBRADO_ID).valorOLanza();
        String otra = agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID).valorOLanza().getId();

        assertThat(error(asignarPensionista.ejecutar(pedido.getId(), primeraPersona, inactivo.getId(),
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPedido.PENSIONISTA_INACTIVO);
        assertThat(error(asignarPensionista.ejecutar(pedido.getId(), primeraPersona, "no-existe", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PENSIONISTA_NO_ENCONTRADO);

        asignarPensionista.ejecutar(pedido.getId(), primeraPersona, luis.getId(), ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(error(asignarPensionista.ejecutar(pedido.getId(), otra, luis.getId(), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.PENSIONISTA_YA_EN_EL_PEDIDO);
    }

    @Test
    void quita_una_persona_sin_lineas_pero_nunca_la_ultima() {
        String otra = agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID).valorOLanza().getId();

        assertThat(quitarPersona.ejecutar(pedido.getId(), otra).esExito()).isTrue();
        assertThat(personas.findByPedidoIdOrderByOrden(pedido.getId())).hasSize(1);
        // La unica que queda no se puede quitar: un pedido nunca se queda sin personas.
        assertThat(error(quitarPersona.ejecutar(pedido.getId(), primeraPersona))).isEqualTo(ErrorPedido.PERSONA_CON_LINEAS);
    }

    @Test
    void una_persona_o_un_pedido_de_otro_local_no_se_encuentra() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);

        assertThat(error(agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.NO_ENCONTRADO);
        assertThat(error(quitarPersona.ejecutar(pedido.getId(), primeraPersona))).isEqualTo(ErrorPedido.NO_ENCONTRADO);
    }

    @Test
    void un_id_de_persona_que_ya_existe_en_otro_pedido_no_se_reutiliza_ni_lo_pisa() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        Pedido deB = abrirPedido.ejecutar(null, Canal.PARA_LLEVAR, null, "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();

        // Un id de persona del pedido del local A, enviado desde el local B, no mueve esa persona a otro pedido.
        assertThat(error(agregarPersona.ejecutar(deB.getId(), primeraPersona, "Intruso", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPedido.NO_ENCONTRADO);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        assertThat(personas.findByIdAndPedidoId(primeraPersona, pedido.getId())).isPresent()
                .get().extracting(CuentaPersona::getEtiqueta).isEqualTo("Persona 1");
    }
}
