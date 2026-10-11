package pe.rikos.operacion.caja.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.CajaRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCajaRepositorio;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.venta.aplicacion.PedidosBase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cajas registradoras (Configuracion): cada caja tiene nombre y codigo unicos en su local, una sola es la principal (la que viene
 * elegida al abrir turno), se activan y desactivan y se borran solo si nunca se usaron. El POS solo elige entre las registradas.
 */
@SpringBootTest
@Transactional
class CajasRegistradorasTest extends PedidosBase {

    @Autowired
    ConsultaCajas consulta;
    @Autowired
    CrearCaja crearCaja;
    @Autowired
    ActualizarCaja actualizarCaja;
    @Autowired
    CambiarEstadoCaja cambiarEstado;
    @Autowired
    DefinirCajaPrincipal definirPrincipal;
    @Autowired
    EliminarCaja eliminarCaja;
    @Autowired
    AbrirTurno abrirTurno;
    @Autowired
    CajaRepositorio cajas;
    @Autowired
    TurnoCajaRepositorio turnos;
    @Autowired
    AuditoriaRepositorio auditorias;

    String localA;
    String localB;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = nuevoLocal("cr");
        localB = nuevoLocal("cs");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
    }

    @AfterEach
    void limpiar() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    static ErrorCaja error(Resultado<?, ErrorCaja> r) {
        assertThat(r.esFracaso()).as("se esperaba un fracaso").isTrue();
        return ((Resultado.Fracaso<?, ErrorCaja>) r).error();
    }

    private Caja principal() {
        return consulta.listar().valorOLanza().stream().map(FilaDeCaja::caja).filter(Caja::isPrincipal).findFirst().orElseThrow();
    }

    @Test
    void la_caja_sembrada_es_la_principal_con_su_codigo() {
        var lista = consulta.listar().valorOLanza();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).caja().getNombre()).isEqualTo("Caja Principal");
        assertThat(lista.get(0).caja().getCodigo()).isEqualTo("CAJ-01");
        assertThat(lista.get(0).caja().isPrincipal()).isTrue();
    }

    @Test
    void se_crea_una_caja_con_su_codigo_y_no_es_la_principal() {
        principal();

        Caja barra = crearCaja.ejecutar("Caja Barra", "caj-ba", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(barra.getNombre()).isEqualTo("Caja Barra");
        assertThat(barra.getCodigo()).isEqualTo("CAJ-BA");
        assertThat(barra.isPrincipal()).isFalse();
        assertThat(barra.isActivo()).isTrue();
    }

    @Test
    void sin_codigo_se_asigna_el_siguiente() {
        principal();

        Caja dos = crearCaja.ejecutar("Caja Delivery", null, ADMIN_SEMBRADO_ID).valorOLanza();
        Caja tres = crearCaja.ejecutar("Caja Terraza", " ", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(dos.getCodigo()).isEqualTo("CAJ-02");
        assertThat(tres.getCodigo()).isEqualTo("CAJ-03");
    }

    @Test
    void el_nombre_y_el_codigo_no_se_repiten_en_el_local_pero_si_entre_locales() {
        principal();
        crearCaja.ejecutar("Caja Barra", "CAJ-BA", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(crearCaja.ejecutar("caja barra", "CAJ-XX", ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.NOMBRE_REPETIDO);
        assertThat(error(crearCaja.ejecutar("Otra", "caj-ba", ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.CODIGO_REPETIDO);
        assertThat(error(crearCaja.ejecutar("Otra", "x".repeat(21), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.CODIGO_INVALIDO);
        assertThat(error(crearCaja.ejecutar(" ", "CAJ-ZZ", ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.NOMBRE_INVALIDO);

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        consulta.listar().valorOLanza();
        assertThat(crearCaja.ejecutar("Caja Barra", "CAJ-BA", ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }

    @Test
    void se_edita_el_nombre_y_el_codigo_y_queda_en_la_bitacora() {
        Caja barra = crearCaja.ejecutar("Caja Barra", "CAJ-BA", ADMIN_SEMBRADO_ID).valorOLanza();

        Caja editada = actualizarCaja.ejecutar(barra.getId(), "Caja de la barra", "CAJ-B2", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(editada.getNombre()).isEqualTo("Caja de la barra");
        assertThat(editada.getCodigo()).isEqualTo("CAJ-B2");
        assertThat(auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("caja", barra.getId()))
                .extracting(e -> e.getEvento()).contains("caja_modificada");
        // Editar sin cambiar el codigo ni el nombre no choca consigo misma.
        assertThat(actualizarCaja.ejecutar(barra.getId(), "Caja de la barra", "CAJ-B2", ADMIN_SEMBRADO_ID).esExito()).isTrue();
        // Pero no puede tomar el de otra.
        assertThat(error(actualizarCaja.ejecutar(barra.getId(), "Caja de la barra", "CAJ-01", ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCaja.CODIGO_REPETIDO);
        assertThat(error(actualizarCaja.ejecutar("no-existe", "X", "Y", ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.CAJA_NO_ENCONTRADA);
    }

    @Test
    void una_sola_caja_es_la_principal() {
        Caja vieja = principal();
        Caja barra = crearCaja.ejecutar("Caja Barra", "CAJ-BA", ADMIN_SEMBRADO_ID).valorOLanza();

        definirPrincipal.ejecutar(barra.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(cajas.findById(barra.getId()).orElseThrow().isPrincipal()).isTrue();
        assertThat(cajas.findById(vieja.getId()).orElseThrow().isPrincipal()).isFalse();
        assertThat(consulta.listar().valorOLanza().stream().filter(f -> f.caja().isPrincipal())).hasSize(1);
    }

    @Test
    void una_caja_desactivada_no_puede_ser_la_principal() {
        principal();
        Caja barra = crearCaja.ejecutar("Caja Barra", "CAJ-BA", ADMIN_SEMBRADO_ID).valorOLanza();
        cambiarEstado.ejecutar(barra.getId(), false, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(definirPrincipal.ejecutar(barra.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.CAJA_INACTIVA);
    }

    @Test
    void se_desactiva_y_se_reactiva_pero_no_la_principal_ni_una_con_turno_abierto() {
        Caja principal = principal();
        Caja barra = crearCaja.ejecutar("Caja Barra", "CAJ-BA", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(cambiarEstado.ejecutar(principal.getId(), false, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.CAJA_PRINCIPAL);

        abrirTurno.ejecutar(barra.getId(), 1000L, ADMIN_SEMBRADO_ID, null).valorOLanza();
        assertThat(error(cambiarEstado.ejecutar(barra.getId(), false, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.CAJA_CON_TURNO_ABIERTO);
        assertThat(cajas.findById(barra.getId()).orElseThrow().isActivo()).isTrue();
    }

    @Test
    void una_caja_desactivada_se_reactiva() {
        principal();
        Caja barra = crearCaja.ejecutar("Caja Barra", "CAJ-BA", ADMIN_SEMBRADO_ID).valorOLanza();

        cambiarEstado.ejecutar(barra.getId(), false, ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(cajas.findById(barra.getId()).orElseThrow().isActivo()).isFalse();
        cambiarEstado.ejecutar(barra.getId(), true, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(cajas.findById(barra.getId()).orElseThrow().isActivo()).isTrue();
    }

    @Test
    void se_elimina_una_caja_que_nunca_se_uso_pero_no_la_principal_ni_una_con_turnos() {
        Caja principal = principal();
        Caja barra = crearCaja.ejecutar("Caja Barra", "CAJ-BA", ADMIN_SEMBRADO_ID).valorOLanza();
        Caja usada = crearCaja.ejecutar("Caja Usada", "CAJ-US", ADMIN_SEMBRADO_ID).valorOLanza();
        abrirTurno.ejecutar(usada.getId(), 1000L, ADMIN_SEMBRADO_ID, null).valorOLanza();

        assertThat(error(eliminarCaja.ejecutar(principal.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.CAJA_PRINCIPAL);
        assertThat(error(eliminarCaja.ejecutar(usada.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.TIENE_HISTORIAL);

        eliminarCaja.ejecutar(barra.getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(cajas.findById(barra.getId())).isEmpty();
        assertThat(error(eliminarCaja.ejecutar(barra.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.CAJA_NO_ENCONTRADA);
    }

    @Test
    void una_caja_con_historial_se_desactiva_en_vez_de_borrarse() {
        principal();
        Caja usada = crearCaja.ejecutar("Caja Usada", "CAJ-US", ADMIN_SEMBRADO_ID).valorOLanza();
        var turno = abrirTurno.ejecutar(usada.getId(), 1000L, ADMIN_SEMBRADO_ID, null).valorOLanza();
        turno.cerrar(ADMIN_SEMBRADO_ID, java.time.OffsetDateTime.now());
        turnos.save(turno);

        // Con el turno cerrado sigue sin poder borrarse (hay historial), pero si desactivarse.
        assertThat(error(eliminarCaja.ejecutar(usada.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.TIENE_HISTORIAL);
        assertThat(cambiarEstado.ejecutar(usada.getId(), false, ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }

    @Test
    void el_turno_guarda_la_nota_de_apertura() {
        Caja caja = principal();

        var turno = abrirTurno.ejecutar(caja.getId(), 5000L, ADMIN_SEMBRADO_ID, "  Turno mañana, billetes de 10 y 20 ").valorOLanza();

        assertThat(turno.getNotaApertura()).isEqualTo("Turno mañana, billetes de 10 y 20");
        assertThat(abrirTurno.ejecutar(caja.getId(), 5000L, ADMIN_SEMBRADO_ID, null).esFracaso()).isTrue();
    }

    @Test
    void una_nota_demasiado_larga_se_rechaza() {
        Caja caja = principal();

        assertThat(error(abrirTurno.ejecutar(caja.getId(), 5000L, ADMIN_SEMBRADO_ID, "x".repeat(251)))).isEqualTo(ErrorCaja.NOTA_LARGA);
    }

    @Test
    void una_caja_de_otro_local_no_se_toca() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        Caja deB = consulta.listar().valorOLanza().get(0).caja();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);

        assertThat(error(actualizarCaja.ejecutar(deB.getId(), "X", "Y", ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.CAJA_NO_ENCONTRADA);
        assertThat(error(cambiarEstado.ejecutar(deB.getId(), false, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.CAJA_NO_ENCONTRADA);
        assertThat(error(definirPrincipal.ejecutar(deB.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.CAJA_NO_ENCONTRADA);
        assertThat(error(eliminarCaja.ejecutar(deB.getId(), ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.CAJA_NO_ENCONTRADA);
    }
}
