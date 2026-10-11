package pe.rikos.operacion.pensionistas.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.comun.auditoria.Auditoria;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.pensionistas.infraestructura.CuentaPensionista;
import pe.rikos.operacion.pensionistas.infraestructura.PlanTarifario;
import pe.rikos.operacion.pensionistas.infraestructura.Pensionista;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

/** US-016: la ficha del pensionista, su responsable, su PIN y su cuenta. */
@SpringBootTest
@Transactional
class PensionistaTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";

    @Autowired
    private CrearPensionista crearPensionista;
    @Autowired
    private ActualizarPensionista actualizarPensionista;
    @Autowired
    private CambiarEstadoPensionista cambiarEstado;
    @Autowired
    private RegenerarPin regenerarPin;
    @Autowired
    private ConsultaPensionistas consulta;
    @Autowired
    private CrearPlanTarifario crearPlan;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuditoriaRepositorio auditorias;

    private PlanTarifario plan;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        plan = crearPlan.ejecutar("Plan ficha " + Identificador.nuevo().valor().substring(16), null,
                new PreciosPlan(500L, 800L, 800L), null, ADMIN_SEMBRADO_ID).valorOLanza();
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private static String dniAlAzar() {
        return String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 99_999_999));
    }

    private DatosResponsable responsable() {
        return new DatosResponsable("Rosa Pérez Díaz", dniAlAzar(), "999111222", "rosa@correo.pe", "Madre",
                "Av. Los Olivos 123");
    }

    private DatosPensionista datos(String dni) {
        return new DatosPensionista(dni, "Luis", "Quispe Rojas", "988777666", "luis@correo.pe", "UNI", "20241234",
                "Ingeniería", "5", plan.getId(), Set.of(Comida.ALMUERZO), "mensual", responsable());
    }

    private CrearPensionista.Creado crear() {
        return crearPensionista.ejecutar(datos(dniAlAzar()), ADMIN_SEMBRADO_ID).valorOLanza();
    }

    // ---- Alta (CA1, CA5) ------------------------------------------------------

    @Test
    void el_alta_guarda_la_ficha_el_responsable_y_abre_la_cuenta() {
        CrearPensionista.Creado creado = crear();
        Pensionista p = creado.pensionista();

        assertThat(p.isActivo()).isTrue();
        assertThat(p.getNombres()).isEqualTo("Luis");
        assertThat(p.getPlanTarifarioId()).isEqualTo(plan.getId());
        assertThat(p.getComidas()).containsExactly(Comida.ALMUERZO);
        assertThat(consulta.responsable(p.getId()).orElseThrow().getNombreCompleto()).isEqualTo("Rosa Pérez Díaz");
        CuentaPensionista cuenta = consulta.cuenta(p.getId()).orElseThrow();
        assertThat(cuenta.getEstado()).isEqualTo("abierta");
    }

    // ---- Credenciales (CA4, RN-090) -------------------------------------------

    @Test
    void el_pin_lo_genera_el_sistema_de_cuatro_digitos_y_se_guarda_cifrado() {
        CrearPensionista.Creado creado = crear();

        assertThat(creado.pin()).matches("\\d{4}");
        assertThat(creado.pensionista().getHashPin()).isNotEqualTo(creado.pin());
        assertThat(passwordEncoder.matches(creado.pin(), creado.pensionista().getHashPin())).isTrue();
    }

    @Test
    void la_contrasena_del_portal_no_existe_todavia_y_es_una_credencial_distinta_del_pin() {
        Pensionista p = crear().pensionista();

        assertThat(p.getHashContrasenaPortal()).isNull();
    }

    @Test
    void regenerar_el_pin_cambia_la_credencial_y_queda_en_la_bitacora_sin_el_pin() {
        CrearPensionista.Creado creado = crear();
        String hashAnterior = creado.pensionista().getHashPin();

        String nuevo = regenerarPin.ejecutar(creado.pensionista().getId(), ADMIN_SEMBRADO_ID).valorOLanza();

        Pensionista recargado = consulta.pensionista(creado.pensionista().getId()).orElseThrow();
        assertThat(nuevo).matches("\\d{4}");
        assertThat(recargado.getHashPin()).isNotEqualTo(hashAnterior);
        assertThat(passwordEncoder.matches(nuevo, recargado.getHashPin())).isTrue();
        List<Auditoria> filas = auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("pensionista",
                creado.pensionista().getId());
        assertThat(filas).anyMatch(f -> f.getEvento().equals("cambio_credencial"));
        assertThat(filas).noneMatch(f -> String.valueOf(f.getValorNuevo()).contains(nuevo)
                && f.getValorNuevo().contains("pin"));
    }

    // ---- Validaciones (CA2, CA3) ----------------------------------------------

    @Test
    void el_dni_es_unico_en_toda_la_empresa() {
        String dni = dniAlAzar();
        crearPensionista.ejecutar(datos(dni), ADMIN_SEMBRADO_ID);

        assertThat(error(crearPensionista.ejecutar(datos(dni), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPensionista.DNI_DUPLICADO);
    }

    @Test
    void rechaza_un_dni_que_no_tiene_ocho_digitos() {
        for (String dni : new String[] {"1234567", "123456789", "12A45678", "", null}) {
            assertThat(error(crearPensionista.ejecutar(datos(dni), ADMIN_SEMBRADO_ID)))
                    .isEqualTo(ErrorPensionista.DNI_INVALIDO);
        }
    }

    @Test
    void sin_responsable_no_se_guarda_la_ficha() {
        DatosPensionista sin = new DatosPensionista(dniAlAzar(), "Luis", "Quispe", null, null, null, null, null, null,
                plan.getId(), Set.of(Comida.ALMUERZO), null, null);

        assertThat(error(crearPensionista.ejecutar(sin, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPensionista.RESPONSABLE_OBLIGATORIO);
    }

    @Test
    void los_seis_datos_del_responsable_son_obligatorios() {
        DatosResponsable ok = responsable();
        List<DatosResponsable> incompletos = List.of(
                new DatosResponsable(" ", ok.dni(), ok.telefono(), ok.correo(), ok.parentesco(), ok.direccion()),
                new DatosResponsable(ok.nombreCompleto(), ok.dni(), null, ok.correo(), ok.parentesco(), ok.direccion()),
                new DatosResponsable(ok.nombreCompleto(), ok.dni(), ok.telefono(), "", ok.parentesco(), ok.direccion()),
                new DatosResponsable(ok.nombreCompleto(), ok.dni(), ok.telefono(), ok.correo(), null, ok.direccion()),
                new DatosResponsable(ok.nombreCompleto(), ok.dni(), ok.telefono(), ok.correo(), ok.parentesco(), " "));

        for (DatosResponsable r : incompletos) {
            DatosPensionista d = new DatosPensionista(dniAlAzar(), "Luis", "Quispe", null, null, null, null, null,
                    null, plan.getId(), Set.of(Comida.ALMUERZO), null, r);
            assertThat(error(crearPensionista.ejecutar(d, ADMIN_SEMBRADO_ID)))
                    .isEqualTo(ErrorPensionista.RESPONSABLE_INCOMPLETO);
        }
        DatosPensionista dniMalo = new DatosPensionista(dniAlAzar(), "Luis", "Quispe", null, null, null, null, null,
                null, plan.getId(), Set.of(Comida.ALMUERZO), null,
                new DatosResponsable(ok.nombreCompleto(), "123", ok.telefono(), ok.correo(), ok.parentesco(),
                        ok.direccion()));
        assertThat(error(crearPensionista.ejecutar(dniMalo, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPensionista.RESPONSABLE_DNI_INVALIDO);
    }

    @Test
    void exige_nombres_apellidos_plan_existente_y_al_menos_una_comida() {
        DatosPensionista base = datos(dniAlAzar());

        assertThat(error(crearPensionista.ejecutar(conNombres(base, " ", "Quispe"), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPensionista.NOMBRES_OBLIGATORIOS);
        assertThat(error(crearPensionista.ejecutar(conNombres(base, "Luis", null), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPensionista.NOMBRES_OBLIGATORIOS);
        assertThat(error(crearPensionista.ejecutar(conPlan(base, "no-existe"), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPensionista.PLAN_NO_ENCONTRADO);
        assertThat(error(crearPensionista.ejecutar(conComidas(base, Set.of()), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPensionista.SIN_COMIDAS);
        assertThat(error(crearPensionista.ejecutar(conFrecuencia(base, "diaria"), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPensionista.FRECUENCIA_INVALIDA);
    }

    @Test
    void las_comidas_habituales_pueden_ser_las_tres() {
        DatosPensionista todas = conComidas(datos(dniAlAzar()), Set.of(Comida.DESAYUNO, Comida.ALMUERZO, Comida.CENA));

        Pensionista p = crearPensionista.ejecutar(todas, ADMIN_SEMBRADO_ID).valorOLanza().pensionista();

        assertThat(p.getComidas()).containsExactlyInAnyOrder(Comida.DESAYUNO, Comida.ALMUERZO, Comida.CENA);
    }

    // ---- Edicion ---------------------------------------------------------------

    @Test
    void edita_los_datos_y_el_responsable() {
        Pensionista p = crear().pensionista();
        DatosPensionista nuevos = new DatosPensionista(p.getDni(), "Luis Alberto", "Quispe Rojas", "911222333", null,
                "UNMSM", null, "Derecho", null, plan.getId(), Set.of(Comida.ALMUERZO, Comida.CENA), "semanal",
                new DatosResponsable("Pedro Pérez", dniAlAzar(), "955444333", "pedro@correo.pe", "Tío", "Jr. Lima 45"));

        actualizarPensionista.ejecutar(p.getId(), nuevos, ADMIN_SEMBRADO_ID);

        Pensionista recargado = consulta.pensionista(p.getId()).orElseThrow();
        assertThat(recargado.getNombres()).isEqualTo("Luis Alberto");
        assertThat(recargado.getInstitucion()).isEqualTo("UNMSM");
        assertThat(recargado.getComidas()).containsExactlyInAnyOrder(Comida.ALMUERZO, Comida.CENA);
        assertThat(recargado.getFrecuenciaAbono()).isEqualTo("semanal");
        assertThat(consulta.responsable(p.getId()).orElseThrow().getNombreCompleto()).isEqualTo("Pedro Pérez");
    }

    @Test
    void cambiar_de_plan_queda_en_la_bitacora_y_no_toca_el_pin() {
        CrearPensionista.Creado creado = crear();
        PlanTarifario otro = crearPlan.ejecutar("Otro plan " + Identificador.nuevo().valor().substring(16), null,
                new PreciosPlan(400L, 700L, 700L), null, ADMIN_SEMBRADO_ID).valorOLanza();

        actualizarPensionista.ejecutar(creado.pensionista().getId(),
                conPlan(datos(creado.pensionista().getDni()), otro.getId()), ADMIN_SEMBRADO_ID);

        Pensionista recargado = consulta.pensionista(creado.pensionista().getId()).orElseThrow();
        assertThat(recargado.getPlanTarifarioId()).isEqualTo(otro.getId());
        assertThat(recargado.getHashPin()).isEqualTo(creado.pensionista().getHashPin());
        assertThat(auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("pensionista",
                creado.pensionista().getId())).anyMatch(f -> f.getEvento().equals("cambio_plan"));
    }

    @Test
    void no_se_puede_cambiar_el_dni_por_el_de_otro_pensionista() {
        Pensionista a = crear().pensionista();
        Pensionista b = crear().pensionista();

        assertThat(error(actualizarPensionista.ejecutar(b.getId(), datos(a.getDni()), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPensionista.DNI_DUPLICADO);
        // Conservar el propio dni no es un duplicado.
        assertThat(actualizarPensionista.ejecutar(b.getId(), datos(b.getDni()), ADMIN_SEMBRADO_ID).esExito()).isTrue();
        assertThat(error(actualizarPensionista.ejecutar("no-existe", datos(dniAlAzar()), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPensionista.NO_ENCONTRADO);
    }

    // ---- Estado (RN-034) ------------------------------------------------------

    @Test
    void desactivar_y_reactivar_conserva_la_misma_cuenta_y_queda_en_la_bitacora() {
        Pensionista p = crear().pensionista();
        String cuentaId = consulta.cuenta(p.getId()).orElseThrow().getId();

        cambiarEstado.ejecutar(p.getId(), false, ADMIN_SEMBRADO_ID);
        assertThat(consulta.pensionista(p.getId()).orElseThrow().isActivo()).isFalse();
        assertThat(consulta.cuenta(p.getId()).orElseThrow().getEstado()).isEqualTo("abierta");

        cambiarEstado.ejecutar(p.getId(), true, ADMIN_SEMBRADO_ID);

        assertThat(consulta.pensionista(p.getId()).orElseThrow().isActivo()).isTrue();
        assertThat(consulta.cuenta(p.getId()).orElseThrow().getId()).isEqualTo(cuentaId);
        assertThat(auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("pensionista", p.getId()))
                .filteredOn(f -> f.getEvento().equals("cambio_estado")).hasSize(2);
        assertThat(error(cambiarEstado.ejecutar("no-existe", false, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorPensionista.NO_ENCONTRADO);
    }

    // ---- Consulta --------------------------------------------------------------

    @Test
    void la_lista_incluye_activos_e_inactivos_y_busca_por_nombre_o_dni() {
        Pensionista p = crear().pensionista();
        cambiarEstado.ejecutar(p.getId(), false, ADMIN_SEMBRADO_ID);

        assertThat(consulta.listar()).extracting(Pensionista::getId).contains(p.getId());
        assertThat(consulta.buscar(p.getDni())).extracting(Pensionista::getId).containsExactly(p.getId());
        assertThat(consulta.buscar("quispe rojas")).extracting(Pensionista::getId).contains(p.getId());
        assertThat(consulta.buscar("zzzz-no-existe")).isEmpty();
    }

    // ---- Ayudas ----------------------------------------------------------------

    private static DatosPensionista conNombres(DatosPensionista d, String nombres, String apellidos) {
        return new DatosPensionista(d.dni(), nombres, apellidos, d.telefono(), d.correo(), d.institucion(),
                d.codigoEstudiante(), d.carrera(), d.ciclo(), d.planId(), d.comidas(), d.frecuenciaAbono(),
                d.responsable());
    }

    private static DatosPensionista conPlan(DatosPensionista d, String planId) {
        return new DatosPensionista(d.dni(), d.nombres(), d.apellidos(), d.telefono(), d.correo(), d.institucion(),
                d.codigoEstudiante(), d.carrera(), d.ciclo(), planId, d.comidas(), d.frecuenciaAbono(),
                d.responsable());
    }

    private static DatosPensionista conComidas(DatosPensionista d, Set<Comida> comidas) {
        return new DatosPensionista(d.dni(), d.nombres(), d.apellidos(), d.telefono(), d.correo(), d.institucion(),
                d.codigoEstudiante(), d.carrera(), d.ciclo(), d.planId(), comidas, d.frecuenciaAbono(),
                d.responsable());
    }

    private static DatosPensionista conFrecuencia(DatosPensionista d, String frecuencia) {
        return new DatosPensionista(d.dni(), d.nombres(), d.apellidos(), d.telefono(), d.correo(), d.institucion(),
                d.codigoEstudiante(), d.carrera(), d.ciclo(), d.planId(), d.comidas(), frecuencia, d.responsable());
    }

    private static ErrorPensionista error(Resultado<?, ErrorPensionista> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorPensionista>) resultado).error();
    }
}
