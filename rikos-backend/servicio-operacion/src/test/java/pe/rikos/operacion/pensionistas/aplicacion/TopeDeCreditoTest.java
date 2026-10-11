package pe.rikos.operacion.pensionistas.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.Auditoria;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Sprint 5, seccion 3.8 y 3.10: el tope de credito. Calculado por comida y por
 * dia de atencion, editable a mano, y solo un recordatorio.
 *
 * "Hoy" es el miercoles 11/06/2031 (junio: 30 dias). Los dias de atencion los
 * fija la prueba (ServiciosDeAtencion falso); su lectura real de horarios se
 * prueba aparte. Plan: desayuno 500, almuerzo 800, cena 800. El plan y el
 * pensionista se crean con el reloj en el 1 de junio, para que el plan rija
 * desde el inicio del mes.
 */
@SpringBootTest
@Transactional
class TopeDeCreditoTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final LocalDate PRIMERO = LocalDate.of(2031, 6, 1);
    private static final LocalDate HOY = LocalDate.of(2031, 6, 11);

    @MockitoBean
    private Clock reloj;
    @MockitoBean
    private ServiciosDeAtencion atencion;

    @Autowired
    private ConsultaCuenta consulta;
    @Autowired
    private FijarTopeDeCredito fijarTope;
    @Autowired
    private RegistrarConsumo registrarConsumo;
    @Autowired
    private CrearPensionista crearPensionista;
    @Autowired
    private CrearPlanTarifario crearPlan;
    @Autowired
    private CambiarPreciosPlan cambiarPrecios;
    @Autowired
    private CrearLocal crearLocal;
    @Autowired
    private AuditoriaRepositorio auditorias;

    private PlanTarifario plan;
    private String local;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        fijarReloj(PRIMERO);
        String codigo = "tp" + Identificador.nuevo().valor().substring(20).toLowerCase();
        local = crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID).valorOLanza().getId();
        plan = crearPlan.ejecutar("Plan tope " + Identificador.nuevo().valor().substring(16), null,
                new PreciosPlan(500L, 800L, 800L), null, ADMIN_SEMBRADO_ID).valorOLanza();
        fijarReloj(HOY);
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID, local);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private void fijarReloj(LocalDate dia) {
        when(reloj.getZone()).thenReturn(ConfiguracionReloj.ZONA_DEL_RESTAURANTE);
        when(reloj.instant()).thenReturn(dia.atTime(12, 0).atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toInstant());
    }

    /** Todos los dias entre las fechas sirven las comidas dadas, salvo los que se excluyan. */
    private void atiende(LocalDate desde, LocalDate hasta, Set<String> servicios) {
        Map<LocalDate, Set<String>> dias = new HashMap<>();
        for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
            dias.put(d, servicios);
        }
        when(atencion.serviciosPorDia(any(), any())).thenReturn(dias);
    }

    private Pensionista pensionista(Set<Comida> comidas) {
        String dni = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        var datos = new DatosPensionista(dni, "Luis", "Tope", null, null, null, null, null, null, plan.getId(), comidas,
                null, new DatosResponsable("Rosa Tope", "12345678", "999", "r@c.pe", "Madre", "Av. 1"));
        return crearPensionista.ejecutar(datos, ADMIN_SEMBRADO_ID).valorOLanza().pensionista();
    }

    private TopeDeCredito tope(Pensionista p) {
        return consulta.resumen(p.getId()).orElseThrow().tope();
    }

    // ---- Calculado ------------------------------------------------------------

    @Test
    void es_el_precio_del_plan_por_cada_dia_de_atencion_del_mes_completo() {
        atiende(PRIMERO, LocalDate.of(2031, 6, 30), Set.of("almuerzo"));

        TopeDeCredito t = tope(pensionista(Set.of(Comida.ALMUERZO)));

        // 30 dias x almuerzo 800.
        assertThat(t.monto()).isEqualTo(24_000L);
        assertThat(t.origen()).isEqualTo(TopeDeCredito.Origen.CALCULADO);
        assertThat(t.aviso()).isNull();
    }

    @Test
    void suma_cada_comida_habitual_solo_los_dias_en_que_algun_local_la_sirve() {
        Map<LocalDate, Set<String>> dias = new HashMap<>();
        for (int d = 1; d <= 30; d++) {
            // Almuerzo los 30 dias; cena solo los primeros 20.
            dias.put(LocalDate.of(2031, 6, d), d <= 20 ? Set.of("almuerzo", "cena") : Set.of("almuerzo"));
        }
        when(atencion.serviciosPorDia(any(), any())).thenReturn(dias);

        TopeDeCredito t = tope(pensionista(Set.of(Comida.ALMUERZO, Comida.CENA)));

        assertThat(t.monto()).isEqualTo(30 * 800L + 20 * 800L);
    }

    @Test
    void una_comida_que_no_es_habitual_no_cuenta_aunque_el_local_la_sirva() {
        atiende(PRIMERO, LocalDate.of(2031, 6, 30), Set.of("desayuno", "almuerzo", "cena"));

        assertThat(tope(pensionista(Set.of(Comida.ALMUERZO))).monto()).isEqualTo(24_000L);
    }

    @Test
    void un_cambio_de_precio_a_mitad_de_mes_se_refleja_dia_por_dia() {
        atiende(PRIMERO, LocalDate.of(2031, 6, 30), Set.of("almuerzo"));
        Pensionista p = pensionista(Set.of(Comida.ALMUERZO));
        cambiarPrecios.ejecutar(plan.getId(), new PreciosPlan(500L, 900L, 800L), LocalDate.of(2031, 6, 16),
                ADMIN_SEMBRADO_ID);

        // Del 1 al 15 a 800 y del 16 al 30 a 900.
        assertThat(tope(p).monto()).isEqualTo(15 * 800L + 15 * 900L);
    }

    @Test
    void si_el_pensionista_empezo_este_mes_cuenta_desde_su_alta() {
        // Alta "hoy": la fecha real. El reloj vuelve a la fecha real para este caso.
        LocalDate hoyReal = LocalDate.now(ConfiguracionReloj.ZONA_DEL_RESTAURANTE);
        fijarReloj(hoyReal);
        atiende(hoyReal.withDayOfMonth(1), hoyReal.withDayOfMonth(hoyReal.lengthOfMonth()), Set.of("almuerzo"));
        PlanTarifario planReal = crearPlan.ejecutar("Plan alta " + Identificador.nuevo().valor().substring(16), null,
                new PreciosPlan(500L, 800L, 800L), null, ADMIN_SEMBRADO_ID).valorOLanza();
        String dni = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        Pensionista p = crearPensionista.ejecutar(new DatosPensionista(dni, "Luis", "Alta", null, null, null, null, null,
                null, planReal.getId(), Set.of(Comida.ALMUERZO), null,
                new DatosResponsable("R", "12345678", "9", "r@c.pe", "Madre", "A")), ADMIN_SEMBRADO_ID)
                .valorOLanza().pensionista();

        int diasDesdeAlta = hoyReal.lengthOfMonth() - hoyReal.getDayOfMonth() + 1;

        assertThat(tope(p).monto()).isEqualTo(diasDesdeAlta * 800L);
    }

    // ---- Nunca un tope falso ------------------------------------------------------

    @Test
    void si_el_plan_no_tiene_precio_de_una_comida_contada_el_tope_queda_sin_calcular() {
        // El plan rige desde el 1 de junio: se crea con el reloj ese dia y despues vuelve al 11.
        fijarReloj(PRIMERO);
        PlanTarifario sinCena = crearPlan.ejecutar("Plan sin cena " + Identificador.nuevo().valor().substring(16), null,
                new PreciosPlan(500L, 800L, null), null, ADMIN_SEMBRADO_ID).valorOLanza();
        fijarReloj(HOY);
        atiende(PRIMERO, LocalDate.of(2031, 6, 30), Set.of("almuerzo", "cena"));
        String dni = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
        Pensionista p = crearPensionista.ejecutar(new DatosPensionista(dni, "Luis", "SinCena", null, null, null, null,
                null, null, sinCena.getId(), Set.of(Comida.ALMUERZO, Comida.CENA), null,
                new DatosResponsable("R", "12345678", "9", "r@c.pe", "Madre", "A")), ADMIN_SEMBRADO_ID)
                .valorOLanza().pensionista();

        TopeDeCredito t = tope(p);

        assertThat(t.monto()).isNull();
        assertThat(t.aviso()).isEqualTo(TopeDeCredito.Aviso.PLAN_SIN_PRECIO);
    }

    @Test
    void si_ningun_local_atiende_ese_mes_no_se_inventa_un_tope() {
        when(atencion.serviciosPorDia(any(), any())).thenReturn(Map.of());

        TopeDeCredito t = tope(pensionista(Set.of(Comida.ALMUERZO)));

        assertThat(t.monto()).isNull();
        assertThat(t.aviso()).isEqualTo(TopeDeCredito.Aviso.SIN_DIAS_DE_ATENCION);
    }

    // ---- Fijado a mano -------------------------------------------------------------

    @Test
    void se_puede_fijar_a_mano_y_volver_al_calculo_y_cada_cambio_queda_en_la_bitacora() {
        atiende(PRIMERO, LocalDate.of(2031, 6, 30), Set.of("almuerzo"));
        Pensionista p = pensionista(Set.of(Comida.ALMUERZO));

        fijarTope.ejecutar(p.getId(), 15_000L, ADMIN_SEMBRADO_ID);
        assertThat(tope(p).monto()).isEqualTo(15_000L);
        assertThat(tope(p).origen()).isEqualTo(TopeDeCredito.Origen.FIJADO);

        fijarTope.ejecutar(p.getId(), null, ADMIN_SEMBRADO_ID);
        assertThat(tope(p).monto()).isEqualTo(24_000L);
        assertThat(tope(p).origen()).isEqualTo(TopeDeCredito.Origen.CALCULADO);

        List<Auditoria> filas = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("pensionista", p.getId());
        assertThat(filas).filteredOn(f -> f.getEvento().equals("cambio_limite")).hasSize(2);
    }

    @Test
    void un_tope_fijado_no_necesita_horarios_ni_precios() {
        when(atencion.serviciosPorDia(any(), any())).thenReturn(Map.of());
        Pensionista p = pensionista(Set.of(Comida.ALMUERZO));

        fijarTope.ejecutar(p.getId(), 9_000L, ADMIN_SEMBRADO_ID);

        assertThat(tope(p).monto()).isEqualTo(9_000L);
        assertThat(tope(p).aviso()).isNull();
    }

    @Test
    void el_tope_fijado_tiene_que_ser_positivo_y_repetirlo_no_escribe_otra_vez() {
        Pensionista p = pensionista(Set.of(Comida.ALMUERZO));

        for (long malo : new long[] {0L, -100L}) {
            Resultado<Pensionista, ErrorCuenta> r = fijarTope.ejecutar(p.getId(), malo, ADMIN_SEMBRADO_ID);
            assertThat(((Resultado.Fracaso<?, ErrorCuenta>) r).error()).isEqualTo(ErrorCuenta.LIMITE_INVALIDO);
        }
        assertThat(((Resultado.Fracaso<?, ErrorCuenta>) fijarTope.ejecutar("no-existe", 100L, ADMIN_SEMBRADO_ID))
                .error()).isEqualTo(ErrorCuenta.NO_ENCONTRADO);

        fijarTope.ejecutar(p.getId(), 5_000L, ADMIN_SEMBRADO_ID);
        fijarTope.ejecutar(p.getId(), 5_000L, ADMIN_SEMBRADO_ID);
        assertThat(auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("pensionista", p.getId()))
                .filteredOn(f -> f.getEvento().equals("cambio_limite")).hasSize(1);
    }

    // ---- Credito disponible y recordatorio (RN-016, RN-017) -----------------------

    @Test
    void el_credito_disponible_es_el_tope_menos_la_deuda_y_el_recordatorio_salta_solo_al_superarlo() {
        Pensionista p = pensionista(Set.of(Comida.ALMUERZO));
        fijarTope.ejecutar(p.getId(), 2_000L, ADMIN_SEMBRADO_ID);

        registrarConsumo.ejecutar(null, p.getId(), local, 1_500L, null, OffsetDateTime.now(), ADMIN_SEMBRADO_ID);
        ResumenDeCuenta debajo = consulta.resumen(p.getId()).orElseThrow();
        assertThat(debajo.creditoDisponible()).isEqualTo(500L);
        assertThat(debajo.superaTope()).isFalse();

        registrarConsumo.ejecutar(null, p.getId(), local, 500L, null, OffsetDateTime.now(), ADMIN_SEMBRADO_ID);
        // Deuda == tope: todavia no lo supera.
        assertThat(consulta.resumen(p.getId()).orElseThrow().superaTope()).isFalse();

        registrarConsumo.ejecutar(null, p.getId(), local, 1L, null, OffsetDateTime.now(), ADMIN_SEMBRADO_ID);
        ResumenDeCuenta encima = consulta.resumen(p.getId()).orElseThrow();
        assertThat(encima.superaTope()).isTrue();
        assertThat(encima.creditoDisponible()).isEqualTo(-1L);
    }

    @Test
    void sin_tope_no_hay_credito_disponible_ni_recordatorio() {
        when(atencion.serviciosPorDia(any(), any())).thenReturn(Map.of());
        Pensionista p = pensionista(Set.of(Comida.ALMUERZO));
        registrarConsumo.ejecutar(null, p.getId(), local, 9_999L, null, OffsetDateTime.now(), ADMIN_SEMBRADO_ID);

        ResumenDeCuenta r = consulta.resumen(p.getId()).orElseThrow();

        assertThat(r.creditoDisponible()).isNull();
        assertThat(r.superaTope()).isFalse();
    }
}
