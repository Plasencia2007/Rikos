package pe.rikos.operacion.pensionistas.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * La lista de cuentas corrientes: todos los pensionistas con su saldo, deuda y
 * tope, la mayor deuda primero. Incluye a los inactivos (pueden deber). Los
 * dias de atencion se piden una sola vez para toda la lista.
 */
@SpringBootTest
@Transactional
class CuentasCorrientesTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @MockitoBean
    private ServiciosDeAtencion atencion;

    @Autowired
    private ConsultaCuenta consulta;
    @Autowired
    private CargarSaldoInicial cargarSaldoInicial;
    @Autowired
    private RegistrarConsumo registrarConsumo;
    @Autowired
    private FijarTopeDeCredito fijarTope;
    @Autowired
    private CambiarEstadoPensionista cambiarEstado;
    @Autowired
    private CrearPensionista crearPensionista;
    @Autowired
    private CrearPlanTarifario crearPlan;
    @Autowired
    private CrearLocal crearLocal;

    private PlanTarifario plan;
    private String localId;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        String codigo = "cc" + Identificador.nuevo().valor().substring(20).toLowerCase();
        localId = crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID).valorOLanza().getId();
        plan = crearPlan.ejecutar("Plan lista " + Identificador.nuevo().valor().substring(16), null,
                new PreciosPlan(500L, 800L, 800L), null, ADMIN_SEMBRADO_ID).valorOLanza();
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localId);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private Pensionista pensionista() {
        String dni = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        var datos = new DatosPensionista(dni, "Luis", "Lista", null, null, null, null, null, null, plan.getId(),
                Set.of(Comida.ALMUERZO), null,
                new DatosResponsable("Rosa Lista", "12345678", "999", "r@c.pe", "Madre", "Av. 1"));
        return crearPensionista.ejecutar(datos, ADMIN_SEMBRADO_ID).valorOLanza().pensionista();
    }

    private FilaDeCuentaCorriente fila(List<FilaDeCuentaCorriente> filas, Pensionista p) {
        return filas.stream().filter(f -> f.pensionista().getId().equals(p.getId())).findFirst().orElseThrow();
    }

    @Test
    void lista_a_todos_con_su_saldo_y_la_mayor_deuda_primero() {
        Pensionista debeMucho = pensionista();
        Pensionista debePoco = pensionista();
        Pensionista aFavor = pensionista();
        Pensionista enCero = pensionista();
        cargarSaldoInicial.ejecutar(debeMucho.getId(), 9000L, Signo.CARGO, null, null, ADMIN_SEMBRADO_ID);
        cargarSaldoInicial.ejecutar(debePoco.getId(), 1500L, Signo.CARGO, null, null, ADMIN_SEMBRADO_ID);
        cargarSaldoInicial.ejecutar(aFavor.getId(), 2000L, Signo.CREDITO, null, null, ADMIN_SEMBRADO_ID);

        List<FilaDeCuentaCorriente> filas = consulta.todas();

        assertThat(fila(filas, debeMucho).resumen().deuda()).isEqualTo(9000L);
        assertThat(fila(filas, debePoco).resumen().deuda()).isEqualTo(1500L);
        assertThat(fila(filas, aFavor).resumen().saldoAFavor()).isEqualTo(2000L);
        assertThat(fila(filas, enCero).resumen().saldo()).isZero();
        List<String> ids = filas.stream().map(f -> f.pensionista().getId()).toList();
        assertThat(ids.indexOf(debeMucho.getId())).isLessThan(ids.indexOf(debePoco.getId()));
        assertThat(ids.indexOf(debePoco.getId())).isLessThan(ids.indexOf(aFavor.getId()));
        assertThat(ids.indexOf(debePoco.getId())).isLessThan(ids.indexOf(enCero.getId()));
    }

    @Test
    void marca_a_quien_supera_su_tope_y_no_inventa_un_tope_que_no_puede_calcular() {
        Pensionista supera = pensionista();
        Pensionista sinCalcular = pensionista();
        fijarTope.ejecutar(supera.getId(), 1000L, ADMIN_SEMBRADO_ID);
        registrarConsumo.ejecutar(null, supera.getId(), localId, 3000L, null, OffsetDateTime.now(), ADMIN_SEMBRADO_ID);
        registrarConsumo.ejecutar(null, sinCalcular.getId(), localId, 3000L, null, OffsetDateTime.now(),
                ADMIN_SEMBRADO_ID);

        List<FilaDeCuentaCorriente> filas = consulta.todas();

        assertThat(fila(filas, supera).resumen().superaTope()).isTrue();
        assertThat(fila(filas, supera).resumen().tope().origen()).isEqualTo(TopeDeCredito.Origen.FIJADO);
        // Los horarios estan mockeados vacios: sin dias de atencion no hay tope, y no se marca a nadie.
        assertThat(fila(filas, sinCalcular).resumen().tope().monto()).isNull();
        assertThat(fila(filas, sinCalcular).resumen().superaTope()).isFalse();
    }

    @Test
    void incluye_a_los_inactivos_con_su_deuda() {
        Pensionista inactivo = pensionista();
        cargarSaldoInicial.ejecutar(inactivo.getId(), 700L, Signo.CARGO, null, null, ADMIN_SEMBRADO_ID);
        cambiarEstado.ejecutar(inactivo.getId(), false, ADMIN_SEMBRADO_ID);

        FilaDeCuentaCorriente f = fila(consulta.todas(), inactivo);

        assertThat(f.pensionista().isActivo()).isFalse();
        assertThat(f.resumen().deuda()).isEqualTo(700L);
    }

    @Test
    void los_dias_de_atencion_se_piden_una_sola_vez_para_toda_la_lista() {
        pensionista();
        pensionista();
        pensionista();

        consulta.todas();

        verify(atencion, times(1)).serviciosPorDia(any(), any());
    }

    @Test
    void el_resumen_de_la_lista_es_el_mismo_que_el_de_una_sola_cuenta() {
        Pensionista p = pensionista();
        cargarSaldoInicial.ejecutar(p.getId(), 4200L, Signo.CARGO, null, null, ADMIN_SEMBRADO_ID);

        assertThat(fila(consulta.todas(), p).resumen()).isEqualTo(consulta.resumen(p.getId()).orElseThrow());
    }
}
