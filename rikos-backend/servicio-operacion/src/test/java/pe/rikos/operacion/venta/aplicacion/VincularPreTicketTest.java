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
import pe.rikos.operacion.pensionistas.aplicacion.Comida;
import pe.rikos.operacion.pensionistas.aplicacion.DatosPensionista;
import pe.rikos.operacion.pensionistas.aplicacion.DatosResponsable;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.pensionistas.infraestructura.PreTicket;
import pe.rikos.operacion.pensionistas.infraestructura.PreTicketRepositorio;
import pe.rikos.operacion.venta.infraestructura.CuentaPersona;
import pe.rikos.operacion.venta.infraestructura.CuentaPersonaRepositorio;
import pe.rikos.operacion.venta.infraestructura.Pedido;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-020: Vincular el pre-ticket al pedido del pensionista en la mesa.
 */
@SpringBootTest
@Transactional
class VincularPreTicketTest extends PedidosBase {

    @Autowired
    AbrirPedido abrirPedido;
    @Autowired
    AgregarPersona agregarPersona;
    @Autowired
    VincularPreTicket vincularPreTicket;
    @Autowired
    QuitarPersona quitarPersona;
    @Autowired
    AgregarLinea agregarLinea;
    @Autowired
    CambiarEstadoPensionista cambiarEstadoPensionista;
    @Autowired
    PreTicketRepositorio preTickets;
    @Autowired
    CuentaPersonaRepositorio personas;

    String localA;
    String localB;
    Pedido pedido;
    String primeraPersona;
    PlanTarifario plan;
    Pensionista pensionista;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = nuevoLocal("va");
        localB = nuevoLocal("vb");
        plan = nuevoPlan(500, 800, 800);
        pensionista = nuevoPensionista(plan, "Lopez");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
        pedido = abrirPedido.ejecutar(null, Canal.MESA, nuevaMesa("10"), "almuerzo", ADMIN_SEMBRADO_ID).valorOLanza();
        primeraPersona = personas.findByPedidoIdOrderByOrden(pedido.getId()).get(0).getId();
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private PreTicket emitirPreTicket(String pId, String localId, String servicio, OffsetDateTime venceEn) {
        String codigo = String.format("%06d", ThreadLocalRandom.current().nextInt(100000, 999999));
        PreTicket pt = new PreTicket(Identificador.nuevo().valor(), codigo, pId, localId, servicio, venceEn);
        return preTickets.save(pt);
    }

    static ErrorPedido error(Resultado<?, ErrorPedido> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorPedido>) resultado).error();
    }

    @Test
    void vincula_exitosamente_preticket_pendiente_a_persona_del_pedido() {
        PreTicket pt = emitirPreTicket(pensionista.getId(), localA, "almuerzo", OffsetDateTime.now().plusHours(2));

        CuentaPersona personaActualizada = vincularPreTicket.ejecutar(pedido.getId(), primeraPersona, pt.getCodigo(), ADMIN_SEMBRADO_ID)
                .valorOLanza();

        assertThat(personaActualizada.getTipoCliente()).isEqualTo("pensionista");
        assertThat(personaActualizada.getPensionistaId()).isEqualTo(pensionista.getId());
        assertThat(personaActualizada.getPreTicketId()).isEqualTo(pt.getId());
        assertThat(personaActualizada.getEtiqueta()).isEqualTo(pensionista.nombreCompleto());

        PreTicket ptRecargado = preTickets.findById(pt.getId()).orElseThrow();
        assertThat(ptRecargado.getEstado()).isEqualTo(PreTicket.ESTADO_VINCULADO);
        assertThat(ptRecargado.getVinculadoEn()).isNotNull();
        assertThat(ptRecargado.getVinculadoPor()).isEqualTo(ADMIN_SEMBRADO_ID);
    }

    @Test
    void falla_si_preticket_no_existe() {
        var res = vincularPreTicket.ejecutar(pedido.getId(), primeraPersona, "000000", ADMIN_SEMBRADO_ID);
        assertThat(error(res)).isEqualTo(ErrorPedido.PRE_TICKET_NO_ENCONTRADO);
    }

    @Test
    void falla_si_preticket_es_de_otro_local() {
        PreTicket pt = emitirPreTicket(pensionista.getId(), localB, "almuerzo", OffsetDateTime.now().plusHours(2));

        var res = vincularPreTicket.ejecutar(pedido.getId(), primeraPersona, pt.getCodigo(), ADMIN_SEMBRADO_ID);
        assertThat(error(res)).isEqualTo(ErrorPedido.PRE_TICKET_DE_OTRO_LOCAL);
    }

    @Test
    void falla_si_servicio_no_coincide() {
        PreTicket pt = emitirPreTicket(pensionista.getId(), localA, "desayuno", OffsetDateTime.now().plusHours(2));

        var res = vincularPreTicket.ejecutar(pedido.getId(), primeraPersona, pt.getCodigo(), ADMIN_SEMBRADO_ID);
        assertThat(error(res)).isEqualTo(ErrorPedido.PRE_TICKET_SERVICIO_DISTINTO);
    }

    @Test
    void falla_si_preticket_esta_vencido() {
        PreTicket pt = emitirPreTicket(pensionista.getId(), localA, "almuerzo", OffsetDateTime.now().minusMinutes(5));

        var res = vincularPreTicket.ejecutar(pedido.getId(), primeraPersona, pt.getCodigo(), ADMIN_SEMBRADO_ID);
        assertThat(error(res)).isEqualTo(ErrorPedido.PRE_TICKET_VENCIDO);
    }

    @Test
    void falla_si_preticket_ya_esta_vinculado() {
        PreTicket pt = emitirPreTicket(pensionista.getId(), localA, "almuerzo", OffsetDateTime.now().plusHours(2));
        pt.vincular(ADMIN_SEMBRADO_ID);
        preTickets.save(pt);

        var res = vincularPreTicket.ejecutar(pedido.getId(), primeraPersona, pt.getCodigo(), ADMIN_SEMBRADO_ID);
        assertThat(error(res)).isEqualTo(ErrorPedido.PRE_TICKET_YA_VINCULADO);
    }

    @Test
    void falla_si_preticket_esta_cerrado_o_anulado() {
        PreTicket ptCerrado = emitirPreTicket(pensionista.getId(), localA, "almuerzo", OffsetDateTime.now().plusHours(2));
        ptCerrado.cerrar();
        preTickets.save(ptCerrado);

        var resCerrado = vincularPreTicket.ejecutar(pedido.getId(), primeraPersona, ptCerrado.getCodigo(), ADMIN_SEMBRADO_ID);
        assertThat(error(resCerrado)).isEqualTo(ErrorPedido.PRE_TICKET_CERRADO);

        PreTicket ptAnulado = emitirPreTicket(pensionista.getId(), localA, "almuerzo", OffsetDateTime.now().plusHours(2));
        ptAnulado.anular();
        preTickets.save(ptAnulado);

        var resAnulado = vincularPreTicket.ejecutar(pedido.getId(), primeraPersona, ptAnulado.getCodigo(), ADMIN_SEMBRADO_ID);
        assertThat(error(resAnulado)).isEqualTo(ErrorPedido.PRE_TICKET_ANULADO);
    }

    @Test
    void falla_si_pensionista_esta_inactivo() {
        PreTicket pt = emitirPreTicket(pensionista.getId(), localA, "almuerzo", OffsetDateTime.now().plusHours(2));
        cambiarEstadoPensionista.ejecutar(pensionista.getId(), false, ADMIN_SEMBRADO_ID);

        var res = vincularPreTicket.ejecutar(pedido.getId(), primeraPersona, pt.getCodigo(), ADMIN_SEMBRADO_ID);
        assertThat(error(res)).isEqualTo(ErrorPedido.PENSIONISTA_INACTIVO);
    }

    @Test
    void falla_si_pensionista_ya_esta_en_el_pedido() {
        PreTicket pt1 = emitirPreTicket(pensionista.getId(), localA, "almuerzo", OffsetDateTime.now().plusHours(2));
        vincularPreTicket.ejecutar(pedido.getId(), primeraPersona, pt1.getCodigo(), ADMIN_SEMBRADO_ID).valorOLanza();

        CuentaPersona segundaPersona = agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID).valorOLanza();
        PreTicket pt2 = emitirPreTicket(pensionista.getId(), localA, "almuerzo", OffsetDateTime.now().plusHours(2));

        var res = vincularPreTicket.ejecutar(pedido.getId(), segundaPersona.getId(), pt2.getCodigo(), ADMIN_SEMBRADO_ID);
        assertThat(error(res)).isEqualTo(ErrorPedido.PENSIONISTA_YA_EN_EL_PEDIDO);
    }

    @Test
    void falla_si_persona_ya_tiene_lineas() {
        var prod = productoDeCarta("Gaseosa", 600L);
        agregarLinea.ejecutar(new SolicitudDeLinea(null, pedido.getId(), primeraPersona, TipoDeLinea.PRODUCTO, prod.getId(),
                1, null, java.util.List.of(), null, null), ADMIN_SEMBRADO_ID).valorOLanza();

        PreTicket pt = emitirPreTicket(pensionista.getId(), localA, "almuerzo", OffsetDateTime.now().plusHours(2));

        var res = vincularPreTicket.ejecutar(pedido.getId(), primeraPersona, pt.getCodigo(), ADMIN_SEMBRADO_ID);
        assertThat(error(res)).isEqualTo(ErrorPedido.PERSONA_CON_LINEAS);
    }

    @Test
    void desvincula_preticket_al_quitar_persona() {
        CuentaPersona segundaPersona = agregarPersona.ejecutar(pedido.getId(), null, null, ADMIN_SEMBRADO_ID).valorOLanza();
        PreTicket pt = emitirPreTicket(pensionista.getId(), localA, "almuerzo", OffsetDateTime.now().plusHours(2));

        vincularPreTicket.ejecutar(pedido.getId(), segundaPersona.getId(), pt.getCodigo(), ADMIN_SEMBRADO_ID).valorOLanza();

        PreTicket vinculado = preTickets.findById(pt.getId()).orElseThrow();
        assertThat(vinculado.getEstado()).isEqualTo(PreTicket.ESTADO_VINCULADO);

        // Se quita la persona
        quitarPersona.ejecutar(pedido.getId(), segundaPersona.getId()).valorOLanza();

        // El pre-ticket vuelve a estar pendiente
        PreTicket liberado = preTickets.findById(pt.getId()).orElseThrow();
        assertThat(liberado.getEstado()).isEqualTo(PreTicket.ESTADO_PENDIENTE);
        assertThat(liberado.getVinculadoEn()).isNull();
        assertThat(liberado.getVinculadoPor()).isNull();
    }
}
