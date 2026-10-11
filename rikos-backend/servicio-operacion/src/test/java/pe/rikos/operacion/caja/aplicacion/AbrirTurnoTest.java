package pe.rikos.operacion.caja.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.caja.infraestructura.Caja;
import pe.rikos.operacion.caja.infraestructura.CajaRepositorio;
import pe.rikos.operacion.caja.infraestructura.TurnoCaja;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.infraestructura.Usuario;
import pe.rikos.operacion.identidad.infraestructura.UsuarioRepositorio;
import pe.rikos.operacion.venta.aplicacion.PedidosBase;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * US-032 (sprint 7, entrega 1): abrir el turno de una caja con su fondo inicial. Cada caja tiene a lo sumo un turno
 * abierto, un cajero tiene a lo sumo uno abierto (su dinero no se cruza) y cajas distintas del mismo local son
 * independientes.
 */
@SpringBootTest
@Transactional
class AbrirTurnoTest extends PedidosBase {

    @Autowired
    AbrirTurno abrirTurno;
    @Autowired
    ConsultaCajas consulta;
    @Autowired
    CajaRepositorio cajas;
    @Autowired
    CrearCaja crearCaja;
    @Autowired
    AuditoriaRepositorio auditorias;
    @Autowired
    UsuarioRepositorio usuarios;

    String localA;
    String localB;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = nuevoLocal("ta");
        localB = nuevoLocal("tb");
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);
    }

    @AfterEach
    void limpiar() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    static ErrorCaja error(Resultado<?, ErrorCaja> resultado) {
        assertThat(resultado.esFracaso()).as("se esperaba un fracaso").isTrue();
        return ((Resultado.Fracaso<?, ErrorCaja>) resultado).error();
    }

    private String otroUsuarioId() {
        String sufijo = Identificador.nuevo().valor().substring(16);
        return usuarios.save(new Usuario(Identificador.nuevo().valor(), "cajero-" + sufijo, "Otro", "Cajero", null, null,
                null, "x", null)).getId();
    }

    private Caja primeraCaja() {
        return consulta.listar().valorOLanza().get(0).caja();
    }

    @Test
    void un_local_nuevo_tiene_una_caja_sembrada_sin_turno() {
        var lista = consulta.listar().valorOLanza();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).caja().getNombre()).isEqualTo("Caja 1");
        assertThat(lista.get(0).caja().isActivo()).isTrue();
        assertThat(lista.get(0).turnoAbierto()).isNull();
        // Pedirla otra vez no crea otra.
        assertThat(consulta.listar().valorOLanza()).hasSize(1);
    }

    @Test
    void abrir_el_turno_guarda_fondo_cajero_caja_y_local_y_audita() {
        Caja caja = primeraCaja();

        TurnoCaja turno = abrirTurno.ejecutar(caja.getId(), 20000L, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(turno.getEstado()).isEqualTo("abierto");
        assertThat(turno.getFondoInicial()).isEqualTo(20000L);
        assertThat(turno.getCajeroId()).isEqualTo(ADMIN_SEMBRADO_ID);
        assertThat(turno.getCajaId()).isEqualTo(caja.getId());
        assertThat(turno.getLocalId()).isEqualTo(localA);
        assertThat(turno.getCerradoEn()).isNull();
        assertThat(auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("turno_caja", turno.getId()))
                .extracting(e -> e.getEvento()).containsExactly("turno_abierto");
        assertThat(consulta.turnoActual(ADMIN_SEMBRADO_ID).valorOLanza()).isPresent()
                .get().extracting(TurnoCaja::getId).isEqualTo(turno.getId());
    }

    @Test
    void el_fondo_puede_ser_cero_pero_no_negativo() {
        Caja caja = primeraCaja();

        assertThat(error(abrirTurno.ejecutar(caja.getId(), -1L, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCaja.FONDO_INVALIDO);
        assertThat(error(abrirTurno.ejecutar(caja.getId(), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCaja.FONDO_INVALIDO);
        assertThat(abrirTurno.ejecutar(caja.getId(), 0L, ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }

    @Test
    void una_caja_con_turno_abierto_no_abre_un_segundo() {
        Caja caja = primeraCaja();
        abrirTurno.ejecutar(caja.getId(), 10000L, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(abrirTurno.ejecutar(caja.getId(), 5000L, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCaja.CAJA_CON_TURNO_ABIERTO);
    }

    @Test
    void un_cajero_no_tiene_dos_turnos_abiertos_a_la_vez() {
        Caja uno = primeraCaja();
        Caja dos = crearCaja.ejecutar("Caja 2", ADMIN_SEMBRADO_ID).valorOLanza();
        abrirTurno.ejecutar(uno.getId(), 10000L, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(abrirTurno.ejecutar(dos.getId(), 10000L, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCaja.CAJERO_CON_TURNO_ABIERTO);
    }

    @Test
    void cajas_distintas_del_mismo_local_son_independientes() {
        Caja uno = primeraCaja();
        Caja dos = crearCaja.ejecutar("Caja 2", ADMIN_SEMBRADO_ID).valorOLanza();
        String otroCajero = otroUsuarioId();

        TurnoCaja a = abrirTurno.ejecutar(uno.getId(), 10000L, ADMIN_SEMBRADO_ID).valorOLanza();
        TurnoCaja b = abrirTurno.ejecutar(dos.getId(), 30000L, otroCajero).valorOLanza();

        assertThat(a.getCajaId()).isNotEqualTo(b.getCajaId());
        var lista = consulta.listar().valorOLanza();
        assertThat(lista).hasSize(2);
        assertThat(lista).allSatisfy(f -> assertThat(f.turnoAbierto()).isNotNull());
        assertThat(consulta.turnoActual(otroCajero).valorOLanza().orElseThrow().getFondoInicial()).isEqualTo(30000L);
    }

    @Test
    void una_caja_de_otro_local_no_se_encuentra() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localB);
        Caja deB = consulta.listar().valorOLanza().get(0).caja();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localA);

        assertThat(error(abrirTurno.ejecutar(deB.getId(), 100L, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCaja.CAJA_NO_ENCONTRADA);
        assertThat(error(abrirTurno.ejecutar("no-existe", 100L, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCaja.CAJA_NO_ENCONTRADA);
    }

    @Test
    void una_caja_inactiva_no_abre_turno() {
        Caja caja = primeraCaja();
        caja.desactivar();
        cajas.save(caja);

        assertThat(error(abrirTurno.ejecutar(caja.getId(), 100L, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCaja.CAJA_INACTIVA);
    }

    @Test
    void dos_cajas_del_mismo_local_no_comparten_nombre() {
        crearCaja.ejecutar("Caja 2", ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(error(crearCaja.ejecutar("caja 2", ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.NOMBRE_REPETIDO);
        assertThat(error(crearCaja.ejecutar("  ", ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.NOMBRE_INVALIDO);
    }

    @Test
    void sin_local_activo_no_hay_cajas() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);

        assertThat(error(consulta.listar())).isEqualTo(ErrorCaja.SIN_LOCAL_ACTIVO);
        assertThat(error(abrirTurno.ejecutar("x", 100L, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCaja.SIN_LOCAL_ACTIVO);
    }
}
