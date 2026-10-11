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
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.pensionistas.infraestructura.VersionPlan;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * US-017. "Hoy" es el miercoles 11/06/2031 (reloj fijo). Precios en
 * centimos: 800 = S/ 8.00.
 */
@SpringBootTest
@Transactional
class PlanTarifarioTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final LocalDate HOY = LocalDate.of(2031, 6, 11);
    private static final LocalDate MANANA = HOY.plusDays(1);
    private static final PreciosPlan PRECIOS = new PreciosPlan(500L, 800L, 800L);

    @MockitoBean
    private Clock reloj;

    @Autowired
    private CrearPlanTarifario crearPlan;
    @Autowired
    private CambiarPreciosPlan cambiarPrecios;
    @Autowired
    private ConsultaPlanes consulta;
    @Autowired
    private AuditoriaRepositorio auditorias;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        when(reloj.getZone()).thenReturn(ConfiguracionReloj.ZONA_DEL_RESTAURANTE);
        when(reloj.instant()).thenReturn(HOY.atTime(12, 0).atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE).toInstant());
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private String nombre() {
        return "Plan " + Identificador.nuevo().valor().substring(16);
    }

    private PlanTarifario plan() {
        return crearPlan.ejecutar(nombre(), null, PRECIOS, null, ADMIN_SEMBRADO_ID).valorOLanza();
    }

    // ---- Crear (CA1) ---------------------------------------------------------

    @Test
    void crea_el_plan_con_los_tres_precios_vigentes_desde_hoy() {
        PlanTarifario plan = plan();

        List<VersionPlan> versiones = consulta.versiones(plan.getId());
        assertThat(versiones).hasSize(1);
        assertThat(versiones.get(0).getVigenteDesde()).isEqualTo(HOY);
        assertThat(versiones.get(0).getVigenteHasta()).isNull();
        assertThat(consulta.precio(plan.getId(), Comida.DESAYUNO, HOY)).contains(500L);
        assertThat(consulta.precio(plan.getId(), Comida.ALMUERZO, HOY)).contains(800L);
        assertThat(consulta.precio(plan.getId(), Comida.CENA, HOY)).contains(800L);
    }

    @Test
    void una_comida_sin_precio_devuelve_vacio_y_no_un_cero() {
        PlanTarifario plan = crearPlan.ejecutar(nombre(), null, new PreciosPlan(null, 800L, null), null,
                ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(consulta.precio(plan.getId(), Comida.DESAYUNO, HOY)).isEmpty();
        assertThat(consulta.precio(plan.getId(), Comida.ALMUERZO, HOY)).contains(800L);
    }

    @Test
    void rechaza_nombre_vacio_repetido_y_precios_no_positivos() {
        PlanTarifario existente = plan();

        assertThat(error(crearPlan.ejecutar("  ", null, PRECIOS, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPlan.NOMBRE_OBLIGATORIO);
        assertThat(error(crearPlan.ejecutar(existente.getNombre(), null, PRECIOS, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPlan.NOMBRE_DUPLICADO);
        assertThat(error(crearPlan.ejecutar(nombre(), null, new PreciosPlan(0L, 800L, 800L), null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPlan.PRECIO_INVALIDO);
        assertThat(error(crearPlan.ejecutar(nombre(), null, new PreciosPlan(500L, -1L, 800L), null,
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPlan.PRECIO_INVALIDO);
    }

    @Test
    void un_plan_nuevo_no_empieza_en_el_pasado() {
        assertThat(error(crearPlan.ejecutar(nombre(), null, PRECIOS, HOY.minusDays(1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPlan.VIGENCIA_INVALIDA);
    }

    // ---- Vigencia e historial (CA2, CA3) -------------------------------------

    @Test
    void un_precio_nuevo_cierra_la_version_anterior_y_abre_otra_sin_reescribirla() {
        PlanTarifario plan = plan();

        cambiarPrecios.ejecutar(plan.getId(), new PreciosPlan(500L, 900L, 800L), MANANA, ADMIN_SEMBRADO_ID);

        List<VersionPlan> versiones = consulta.versiones(plan.getId());
        assertThat(versiones).hasSize(2);
        VersionPlan nueva = versiones.get(0);
        VersionPlan vieja = versiones.get(1);
        assertThat(nueva.getVigenteDesde()).isEqualTo(MANANA);
        assertThat(nueva.getVigenteHasta()).isNull();
        assertThat(vieja.getVigenteHasta()).isEqualTo(HOY);
        // La version vieja conserva sus precios: es lo que garantiza el historial.
        assertThat(vieja.getPrecioAlmuerzo()).isEqualTo(800L);
        // Hoy rige el precio viejo; desde manana, el nuevo.
        assertThat(consulta.precio(plan.getId(), Comida.ALMUERZO, HOY)).contains(800L);
        assertThat(consulta.precio(plan.getId(), Comida.ALMUERZO, MANANA)).contains(900L);
        // Antes de que existiera el plan no hay precio: la consulta no inventa uno.
        assertThat(consulta.precio(plan.getId(), Comida.ALMUERZO, HOY.minusDays(30))).isEmpty();
    }

    @Test
    void un_cambio_para_hoy_sobre_una_version_que_empezo_hoy_la_corrige_en_su_lugar() {
        PlanTarifario plan = plan();

        cambiarPrecios.ejecutar(plan.getId(), new PreciosPlan(500L, 850L, 800L), null, ADMIN_SEMBRADO_ID);

        List<VersionPlan> versiones = consulta.versiones(plan.getId());
        assertThat(versiones).hasSize(1);
        assertThat(versiones.get(0).getPrecioAlmuerzo()).isEqualTo(850L);
    }

    @Test
    void se_puede_programar_un_cambio_futuro_y_despues_corregirlo() {
        PlanTarifario plan = plan();
        LocalDate enUnaSemana = HOY.plusDays(7);
        cambiarPrecios.ejecutar(plan.getId(), new PreciosPlan(500L, 900L, 800L), enUnaSemana, ADMIN_SEMBRADO_ID);

        // Mismo dia de inicio que la version futura: es su correccion, no una tercera version.
        cambiarPrecios.ejecutar(plan.getId(), new PreciosPlan(500L, 950L, 800L), enUnaSemana, ADMIN_SEMBRADO_ID);

        assertThat(consulta.versiones(plan.getId())).hasSize(2);
        assertThat(consulta.precio(plan.getId(), Comida.ALMUERZO, enUnaSemana)).contains(950L);
        assertThat(consulta.precio(plan.getId(), Comida.ALMUERZO, HOY)).contains(800L);
    }

    @Test
    void rechaza_vigencias_que_reescriben_el_pasado_y_cambios_que_no_cambian_nada() {
        PlanTarifario plan = plan();
        cambiarPrecios.ejecutar(plan.getId(), new PreciosPlan(500L, 900L, 800L), HOY.plusDays(7), ADMIN_SEMBRADO_ID);

        assertThat(error(cambiarPrecios.ejecutar(plan.getId(), PRECIOS, HOY.minusDays(1), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPlan.VIGENCIA_INVALIDA);
        // Antes de la version futura pero despues de hoy: quedaria un hueco / cruce.
        assertThat(error(cambiarPrecios.ejecutar(plan.getId(), PRECIOS, HOY.plusDays(3), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPlan.VIGENCIA_INVALIDA);
        assertThat(error(cambiarPrecios.ejecutar(plan.getId(), new PreciosPlan(500L, 900L, 800L), HOY.plusDays(9),
                ADMIN_SEMBRADO_ID))).isEqualTo(ErrorPlan.SIN_CAMBIOS);
        assertThat(error(cambiarPrecios.ejecutar("no-existe", PRECIOS, null, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPlan.NO_ENCONTRADO);
    }

    @Test
    void crear_y_cambiar_precios_quedan_en_la_bitacora_con_el_antes_y_el_despues() {
        PlanTarifario plan = plan();
        cambiarPrecios.ejecutar(plan.getId(), new PreciosPlan(500L, 900L, 800L), MANANA, ADMIN_SEMBRADO_ID);

        List<Auditoria> filas = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("plan_tarifario",
                plan.getId());
        assertThat(filas).hasSize(2).allMatch(f -> f.getEvento().equals("cambio_plan"));
        Auditoria cambio = filas.get(0);
        assertThat(cambio.getValorAnterior()).contains("800");
        assertThat(cambio.getValorNuevo()).contains("900");
    }

    // ---- Consulta -------------------------------------------------------------

    @Test
    void lista_los_planes_con_su_version_vigente() {
        PlanTarifario plan = plan();

        Optional<VersionPlan> vigente = consulta.versionVigente(plan.getId(), HOY);

        assertThat(vigente).isPresent();
        assertThat(consulta.planes()).extracting(PlanTarifario::getId).contains(plan.getId());
    }

    private static ErrorPlan error(Resultado<?, ErrorPlan> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorPlan>) resultado).error();
    }
}
