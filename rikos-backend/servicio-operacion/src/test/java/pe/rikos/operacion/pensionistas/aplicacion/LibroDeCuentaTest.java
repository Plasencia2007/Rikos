package pe.rikos.operacion.pensionistas.aplicacion;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.aplicacion.CambiarEstadoMotivo;
import pe.rikos.operacion.catalogo.aplicacion.CrearMotivo;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuenta;
import pe.rikos.operacion.pensionistas.infraestructura.MovimientoCuentaRepositorio;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * US-018: el libro de la cuenta. Saldos en centimos. El saldo no se guarda:
 * sale de sumar el libro (creditos - cargos), asi que estas pruebas leen el
 * resumen y no una columna.
 */
@SpringBootTest
@Transactional
class LibroDeCuentaTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final String MOTIVO_AJUSTE_SEMBRADO_ID = "01M39A0MT0000000000000D002";
    private static final String MOTIVO_DISPONIBILIDAD_SEMBRADO_ID = "01M39A0MT0000000000000D001";

    // El tope no es lo que se prueba aca: sin dias de atencion queda "sin calcular".
    @MockitoBean
    private ServiciosDeAtencion atencion;

    @Autowired
    private CargarSaldoInicial cargarSaldoInicial;
    @Autowired
    private RegistrarConsumo registrarConsumo;
    @Autowired
    private RegistrarAjuste registrarAjuste;
    @Autowired
    private FijarTopeDeCredito fijarTope;
    @Autowired
    private ConsultaCuenta consulta;
    @Autowired
    private CrearPensionista crearPensionista;
    @Autowired
    private CambiarEstadoPensionista cambiarEstado;
    @Autowired
    private CrearPlanTarifario crearPlan;
    @Autowired
    private CrearLocal crearLocal;
    @Autowired
    private CrearMotivo crearMotivo;
    @Autowired
    private CambiarEstadoMotivo cambiarEstadoMotivo;
    @Autowired
    private MovimientoCuentaRepositorio movimientos;
    @Autowired
    private EntityManager em;
    @Autowired
    private JdbcTemplate jdbc;

    private PlanTarifario plan;
    private String localA;
    private String localB;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        localA = local("la");
        localB = local("lb");
        plan = crearPlan.ejecutar("Plan libro " + Identificador.nuevo().valor().substring(16), null,
                new PreciosPlan(500L, 800L, 800L), null, ADMIN_SEMBRADO_ID).valorOLanza();
        enLocal(localA);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private void enLocal(String localId) {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, localId);
    }

    private String local(String prefijo) {
        String codigo = prefijo + Identificador.nuevo().valor().substring(20).toLowerCase();
        return crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID).valorOLanza().getId();
    }

    private Pensionista pensionista() {
        String dni = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        var datos = new DatosPensionista(dni, "Luis", "Libro", null, null, null, null, null, null, plan.getId(),
                Set.of(Comida.ALMUERZO), null,
                new DatosResponsable("Rosa Libro", "12345678", "999", "r@c.pe", "Madre", "Av. 1"));
        return crearPensionista.ejecutar(datos, ADMIN_SEMBRADO_ID).valorOLanza().pensionista();
    }

    private ResumenDeCuenta resumen(Pensionista p) {
        return consulta.resumen(p.getId()).orElseThrow();
    }

    private MovimientoCuenta consumo(Pensionista p, String localId, long importe) {
        return registrarConsumo.ejecutar(null, p.getId(), localId, importe, null, OffsetDateTime.now(),
                ADMIN_SEMBRADO_ID).valorOLanza();
    }

    // ---- Saldo inicial ---------------------------------------------------------

    @Test
    void el_saldo_inicial_de_un_deudor_es_deuda_y_el_de_uno_con_saldo_a_favor_es_saldo_a_favor() {
        Pensionista debe = pensionista();
        Pensionista favor = pensionista();

        cargarSaldoInicial.ejecutar(debe.getId(), 5000L, Signo.CARGO, null, "Cuaderno de septiembre", ADMIN_SEMBRADO_ID);
        cargarSaldoInicial.ejecutar(favor.getId(), 3000L, Signo.CREDITO, null, null, ADMIN_SEMBRADO_ID);

        ResumenDeCuenta d = resumen(debe);
        assertThat(d.saldo()).isEqualTo(-5000L);
        assertThat(d.deuda()).isEqualTo(5000L);
        assertThat(d.saldoAFavor()).isZero();
        ResumenDeCuenta f = resumen(favor);
        assertThat(f.saldo()).isEqualTo(3000L);
        assertThat(f.deuda()).isZero();
        assertThat(f.saldoAFavor()).isEqualTo(3000L);
    }

    @Test
    void una_cuenta_nueva_arranca_en_cero() {
        ResumenDeCuenta r = resumen(pensionista());

        assertThat(r.saldo()).isZero();
        assertThat(r.deuda()).isZero();
        assertThat(r.saldoAFavor()).isZero();
    }

    @Test
    void el_saldo_inicial_se_carga_una_sola_vez_y_no_en_el_futuro() {
        Pensionista p = pensionista();
        cargarSaldoInicial.ejecutar(p.getId(), 5000L, Signo.CARGO, null, null, ADMIN_SEMBRADO_ID);

        assertThat(error(cargarSaldoInicial.ejecutar(p.getId(), 100L, Signo.CARGO, null, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCuenta.SALDO_INICIAL_YA_REGISTRADO);
        Pensionista otro = pensionista();
        assertThat(error(cargarSaldoInicial.ejecutar(otro.getId(), 100L, Signo.CARGO,
                LocalDate.now().plusDays(3), null, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCuenta.FECHA_FUTURA);
        assertThat(resumen(p).deuda()).isEqualTo(5000L);
    }

    // ---- Consumos (RN-018, RN-019) --------------------------------------------

    @Test
    void el_saldo_a_favor_se_aplica_primero_y_solo_el_remanente_sube_la_deuda() {
        Pensionista favorMayor = pensionista();
        cargarSaldoInicial.ejecutar(favorMayor.getId(), 3000L, Signo.CREDITO, null, null, ADMIN_SEMBRADO_ID);
        consumo(favorMayor, localA, 800L);
        // Saldo a favor mayor que el consumo: queda saldo a favor, sin deuda.
        assertThat(resumen(favorMayor).saldoAFavor()).isEqualTo(2200L);
        assertThat(resumen(favorMayor).deuda()).isZero();

        Pensionista favorMenor = pensionista();
        cargarSaldoInicial.ejecutar(favorMenor.getId(), 500L, Signo.CREDITO, null, null, ADMIN_SEMBRADO_ID);
        consumo(favorMenor, localA, 800L);
        // CA3: primero se aplica el saldo a favor; la deuda es solo el remanente.
        assertThat(resumen(favorMenor).deuda()).isEqualTo(300L);
        assertThat(resumen(favorMenor).saldoAFavor()).isZero();
    }

    @Test
    void se_permiten_varios_consumos_del_mismo_dia_y_cada_uno_es_su_propio_movimiento() {
        Pensionista p = pensionista();

        consumo(p, localA, 800L);
        consumo(p, localA, 800L);

        assertThat(consulta.movimientos(p.getId())).hasSize(2);
        assertThat(resumen(p).deuda()).isEqualTo(1600L);
    }

    @Test
    void el_consumo_es_siempre_un_cargo_con_su_local_y_su_pedido() {
        Pensionista p = pensionista();
        MovimientoCuenta m = registrarConsumo.ejecutar(null, p.getId(), localB, 800L, "01M3PEDIDO00000000000000A1",
                OffsetDateTime.now(), ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(m.getTipo()).isEqualTo("consumo");
        assertThat(m.getSigno()).isEqualTo("cargo");
        assertThat(m.getLocalId()).isEqualTo(localB);
        assertThat(m.getPedidoId()).isEqualTo("01M3PEDIDO00000000000000A1");
        assertThat(m.getRegistradoPor()).isEqualTo(ADMIN_SEMBRADO_ID);
        assertThat(m.getRegistradoEn()).isNotNull();
    }

    @Test
    void un_pensionista_inactivo_no_consume_pero_su_cuenta_se_puede_ajustar() {
        Pensionista p = pensionista();
        cambiarEstado.ejecutar(p.getId(), false, ADMIN_SEMBRADO_ID);

        assertThat(error(registrarConsumo.ejecutar(null, p.getId(), localA, 800L, null, OffsetDateTime.now(),
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCuenta.PENSIONISTA_INACTIVO);
        assertThat(registrarAjuste.ejecutar(p.getId(), 100L, Signo.CARGO, MOTIVO_AJUSTE_SEMBRADO_ID, null,
                ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }

    @Test
    void el_tope_solo_recuerda_nunca_impide_un_consumo() {
        Pensionista p = pensionista();
        fijarTope.ejecutar(p.getId(), 1000L, ADMIN_SEMBRADO_ID);

        // 3000 de deuda contra un tope de 1000: se registra igual (RN-017).
        assertThat(registrarConsumo.ejecutar(null, p.getId(), localA, 3000L, null, OffsetDateTime.now(),
                ADMIN_SEMBRADO_ID).esExito()).isTrue();
        assertThat(resumen(p).superaTope()).isTrue();
        assertThat(resumen(p).deuda()).isEqualTo(3000L);
    }

    // ---- Ajustes ---------------------------------------------------------------

    @Test
    void un_ajuste_lleva_motivo_de_cuenta_activo_y_mueve_el_saldo_en_su_sentido() {
        Pensionista p = pensionista();
        consumo(p, localA, 1000L);

        registrarAjuste.ejecutar(p.getId(), 300L, Signo.CREDITO, MOTIVO_AJUSTE_SEMBRADO_ID, "Cobro de más", ADMIN_SEMBRADO_ID);

        ResumenDeCuenta r = resumen(p);
        assertThat(r.deuda()).isEqualTo(700L);
        MovimientoCuenta ajuste = consulta.movimientos(p.getId()).get(0);
        assertThat(ajuste.getTipo()).isEqualTo("ajuste");
        assertThat(ajuste.getMotivoId()).isEqualTo(MOTIVO_AJUSTE_SEMBRADO_ID);
        assertThat(ajuste.getDescripcion()).isEqualTo("Cobro de más");
    }

    @Test
    void rechaza_un_ajuste_sin_motivo_con_un_motivo_de_otro_tipo_o_desactivado() {
        Pensionista p = pensionista();
        Motivo inactivo = crearMotivo.ejecutar("ajuste_cuenta", "Prueba inactivo cuenta", false).valorOLanza();
        cambiarEstadoMotivo.ejecutar(inactivo.getId(), false);

        assertThat(error(registrarAjuste.ejecutar(p.getId(), 100L, Signo.CARGO, null, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCuenta.MOTIVO_OBLIGATORIO);
        assertThat(error(registrarAjuste.ejecutar(p.getId(), 100L, Signo.CARGO, MOTIVO_DISPONIBILIDAD_SEMBRADO_ID,
                null, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCuenta.MOTIVO_INVALIDO);
        assertThat(error(registrarAjuste.ejecutar(p.getId(), 100L, Signo.CARGO, inactivo.getId(), null,
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCuenta.MOTIVO_INVALIDO);
        assertThat(error(registrarAjuste.ejecutar(p.getId(), 100L, Signo.CARGO, "no-existe", null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCuenta.MOTIVO_INVALIDO);
        assertThat(consulta.movimientos(p.getId())).isEmpty();
    }

    @Test
    void si_el_motivo_pide_observacion_se_exige() {
        Pensionista p = pensionista();
        Motivo conObservacion = crearMotivo.ejecutar("ajuste_cuenta", "Prueba con observación", true).valorOLanza();

        assertThat(error(registrarAjuste.ejecutar(p.getId(), 100L, Signo.CARGO, conObservacion.getId(), "  ",
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCuenta.OBSERVACION_OBLIGATORIA);
        assertThat(registrarAjuste.ejecutar(p.getId(), 100L, Signo.CARGO, conObservacion.getId(), "Error de digitación",
                ADMIN_SEMBRADO_ID).esExito()).isTrue();
    }

    // ---- Validaciones -----------------------------------------------------------

    @Test
    void el_importe_es_siempre_mayor_que_cero() {
        Pensionista p = pensionista();

        for (long importe : new long[] {0L, -5L}) {
            assertThat(error(cargarSaldoInicial.ejecutar(p.getId(), importe, Signo.CARGO, null, null,
                    ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCuenta.IMPORTE_INVALIDO);
            assertThat(error(registrarConsumo.ejecutar(null, p.getId(), localA, importe, null, OffsetDateTime.now(),
                    ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCuenta.IMPORTE_INVALIDO);
            assertThat(error(registrarAjuste.ejecutar(p.getId(), importe, Signo.CARGO, MOTIVO_AJUSTE_SEMBRADO_ID, null,
                    ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCuenta.IMPORTE_INVALIDO);
        }
    }

    @Test
    void la_cuenta_tiene_que_existir_y_el_saldo_inicial_y_el_ajuste_piden_local_activo() {
        Pensionista p = pensionista();

        assertThat(error(cargarSaldoInicial.ejecutar("no-existe", 100L, Signo.CARGO, null, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCuenta.NO_ENCONTRADO);
        assertThat(consulta.resumen("no-existe")).isEmpty();

        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        assertThat(error(cargarSaldoInicial.ejecutar(p.getId(), 100L, Signo.CARGO, null, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorCuenta.SIN_LOCAL_ACTIVO);
        assertThat(error(registrarAjuste.ejecutar(p.getId(), 100L, Signo.CARGO, MOTIVO_AJUSTE_SEMBRADO_ID, null,
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorCuenta.SIN_LOCAL_ACTIVO);
    }

    // ---- Operacion sin internet (3.9) -------------------------------------------

    @Test
    void enviar_dos_veces_el_mismo_movimiento_no_lo_duplica() {
        Pensionista p = pensionista();
        String id = Identificador.nuevo().valor();
        OffsetDateTime ocurrido = OffsetDateTime.now().minusHours(2);

        MovimientoCuenta primero = registrarConsumo.ejecutar(id, p.getId(), localA, 800L, null, ocurrido,
                ADMIN_SEMBRADO_ID).valorOLanza();
        MovimientoCuenta repetido = registrarConsumo.ejecutar(id, p.getId(), localA, 800L, null, ocurrido,
                ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(repetido.getId()).isEqualTo(primero.getId());
        assertThat(consulta.movimientos(p.getId())).hasSize(1);
        assertThat(resumen(p).deuda()).isEqualTo(800L);
    }

    @Test
    void dos_locales_sobre_la_misma_cuenta_dan_el_mismo_saldo_en_cualquier_orden_y_cada_movimiento_dice_su_local() {
        Pensionista uno = pensionista();
        Pensionista otro = pensionista();
        cargarSaldoInicial.ejecutar(uno.getId(), 500L, Signo.CREDITO, null, null, ADMIN_SEMBRADO_ID);
        cargarSaldoInicial.ejecutar(otro.getId(), 500L, Signo.CREDITO, null, null, ADMIN_SEMBRADO_ID);

        // El mismo conjunto de consumos de los dos locales, en orden distinto.
        consumo(uno, localA, 800L);
        consumo(uno, localB, 900L);
        consumo(uno, localA, 400L);
        consumo(otro, localA, 400L);
        consumo(otro, localA, 800L);
        consumo(otro, localB, 900L);

        assertThat(resumen(uno).saldo()).isEqualTo(resumen(otro).saldo());
        assertThat(resumen(uno).deuda()).isEqualTo(1600L);
        // CA6: una sola lista, con el local de cada movimiento.
        assertThat(consulta.movimientos(uno.getId())).extracting(MovimientoCuenta::getLocalId)
                .contains(localA, localB);
    }

    @Test
    void los_movimientos_vienen_del_mas_reciente_al_mas_antiguo_segun_cuando_ocurrieron() {
        Pensionista p = pensionista();
        OffsetDateTime ahora = OffsetDateTime.now();
        // Llega primero el mas viejo... y despues uno que ocurrio antes (un local que estuvo sin internet).
        registrarConsumo.ejecutar(null, p.getId(), localA, 100L, null, ahora.minusHours(1), ADMIN_SEMBRADO_ID);
        registrarConsumo.ejecutar(null, p.getId(), localB, 200L, null, ahora.minusHours(5), ADMIN_SEMBRADO_ID);
        registrarConsumo.ejecutar(null, p.getId(), localA, 300L, null, ahora, ADMIN_SEMBRADO_ID);

        assertThat(consulta.movimientos(p.getId())).extracting(MovimientoCuenta::getImporte)
                .containsExactly(300L, 100L, 200L);
    }

    // ---- Solo se agrega -----------------------------------------------------------

    @Test
    void el_repositorio_del_libro_no_expone_borrado_ni_actualizacion() {
        for (Method m : MovimientoCuentaRepositorio.class.getMethods()) {
            assertThat(m.getName().toLowerCase()).doesNotContain("delete").doesNotContain("remove");
        }
    }

    @Test
    void la_base_rechaza_modificar_o_borrar_una_fila_del_libro() {
        Pensionista p = pensionista();
        MovimientoCuenta m = consumo(p, localA, 800L);
        em.flush();

        assertThatThrownBy(() -> jdbc.update("update movimiento_cuenta set importe = 1 where id = ?", m.getId()))
                .hasMessageContaining("solo de agregado");
    }

    @Test
    void la_base_rechaza_borrar_una_fila_del_libro() {
        Pensionista p = pensionista();
        MovimientoCuenta m = consumo(p, localA, 800L);
        em.flush();

        assertThatThrownBy(() -> jdbc.update("delete from movimiento_cuenta where id = ?", m.getId()))
                .hasMessageContaining("solo de agregado");
    }

    private static ErrorCuenta error(Resultado<?, ErrorCuenta> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorCuenta>) resultado).error();
    }
}
