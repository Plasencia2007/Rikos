package pe.rikos.operacion.catalogo.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.*;
import pe.rikos.operacion.comun.auditoria.AuditoriaRepositorio;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.comun.tiempo.ConfiguracionReloj;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * US-013. "Hoy" es el miercoles 11/06/2031 (reloj fijo): la semana del
 * lunes 09/06 es la actual, lunes y martes ya estan cerrados. Fechas lejanas
 * a proposito, para no chocar con semanas reales de la base de desarrollo
 * (fecha_inicio es unica).
 */
@SpringBootTest
@Transactional
class ProgramacionSemanalTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final LocalDate LUNES = LocalDate.of(2031, 6, 9);
    private static final LocalDate MARTES = LUNES.plusDays(1);
    private static final LocalDate MIERCOLES = LUNES.plusDays(2);
    private static final LocalDate JUEVES = LUNES.plusDays(3);
    private static final LocalDate VIERNES = LUNES.plusDays(4);
    private static final LocalDate SABADO = LUNES.plusDays(5);
    private static final LocalDate LUNES_SIGUIENTE = LUNES.plusDays(7);

    @MockitoBean
    private Clock reloj;

    @Autowired
    private CrearSemanaMenu crearSemana;
    @Autowired
    private AgregarOpcionMenu agregarOpcion;
    @Autowired
    private ActualizarOpcionMenu actualizarOpcion;
    @Autowired
    private QuitarOpcionMenu quitarOpcion;
    @Autowired
    private ConfirmarSemanaMenu confirmarSemana;
    @Autowired
    private EliminarSemanaMenu eliminarSemana;
    @Autowired
    private ConsultaMenu consulta;
    @Autowired
    private ProductoRepositorio productos;
    @Autowired
    private CategoriaProductoRepositorio categorias;
    @Autowired
    private UnidadMedidaRepositorio unidades;
    @Autowired
    private AuditoriaRepositorio auditorias;

    private Producto seco;
    private Producto lomo;
    private Producto sopa;
    private Producto cebolla;

    @BeforeEach
    void preparar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
        when(reloj.getZone()).thenReturn(ConfiguracionReloj.ZONA_DEL_RESTAURANTE);
        when(reloj.instant()).thenReturn(MIERCOLES.atTime(12, 0).atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE)
                .toInstant());
        seco = plato("Seco de res", "01M39A0RZ0000000000000S005");
        lomo = plato("Estofado de res", "01M39A0RZ0000000000000S005");
        sopa = plato("Sopa de casa", "01M39A0RZ0000000000000S001");
        cebolla = plato("Cebolla (insumo)", "01M39A0RZ0000000000000S021");
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    // ---- Semana -------------------------------------------------------------

    @Test
    void crea_la_semana_en_borrador_de_lunes_a_domingo() {
        SemanaMenu semana = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza();

        assertThat(semana.getEstado()).isEqualTo("borrador");
        assertThat(semana.getFechaFin()).isEqualTo(LUNES.plusDays(6));
    }

    @Test
    void la_semana_empieza_en_lunes_es_unica_y_no_puede_haber_terminado() {
        crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID);

        assertThat(error(crearSemana.ejecutar(MARTES.plusDays(7), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorProgramacion.NO_ES_LUNES);
        assertThat(error(crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorProgramacion.SEMANA_YA_EXISTE);
        assertThat(error(crearSemana.ejecutar(LUNES.minusDays(7), ADMIN_SEMBRADO_ID)))
                .isEqualTo(ErrorProgramacion.SEMANA_PASADA);
    }

    @Test
    void el_estado_en_servicio_y_pasada_se_calculan_por_fecha() {
        SemanaMenu semana = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza();
        assertThat(semana.estadoEn(MIERCOLES)).isEqualTo("borrador");

        agregarOpcion.ejecutar(semana.getId(), JUEVES, seco.getId(), "normal", null);
        confirmarSemana.ejecutar(semana.getId(), ADMIN_SEMBRADO_ID);

        assertThat(semana.estadoEn(LUNES.minusDays(1))).isEqualTo("confirmado");
        assertThat(semana.estadoEn(MIERCOLES)).isEqualTo("en_servicio");
        assertThat(semana.estadoEn(LUNES_SIGUIENTE)).isEqualTo("pasada");
    }

    // ---- Opciones (CA1, CA2) --------------------------------------------------

    @Test
    void guarda_la_opcion_con_su_componente_derivado_de_la_categoria_y_sus_porciones() {
        SemanaMenu semana = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza();

        OpcionMenu segundo = agregarOpcion.ejecutar(semana.getId(), JUEVES, seco.getId(), "normal", 40).valorOLanza();
        OpcionMenu entrada = agregarOpcion.ejecutar(semana.getId(), JUEVES, sopa.getId(), "normal", null).valorOLanza();

        assertThat(segundo.getComponente()).isEqualTo("segundo");
        assertThat(segundo.getPorcionesEstimadas()).isEqualTo(40);
        assertThat(entrada.getComponente()).isEqualTo("entrada");
        assertThat(entrada.getPorcionesEstimadas()).isNull();
    }

    @Test
    void rechaza_lo_que_no_se_puede_programar() {
        String id = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        agregarOpcion.ejecutar(id, JUEVES, seco.getId(), "normal", null);

        assertThat(error(agregarOpcion.ejecutar(id, JUEVES, cebolla.getId(), "normal", null)))
                .isEqualTo(ErrorProgramacion.PRODUCTO_NO_ES_DEL_MENU);
        assertThat(error(agregarOpcion.ejecutar(id, JUEVES, sopa.getId(), "extra", null)))
                .isEqualTo(ErrorProgramacion.EXTRA_SOLO_EN_SEGUNDO);
        assertThat(error(agregarOpcion.ejecutar(id, JUEVES, seco.getId(), "extra", null)))
                .isEqualTo(ErrorProgramacion.OPCION_REPETIDA);
        assertThat(error(agregarOpcion.ejecutar(id, LUNES_SIGUIENTE, lomo.getId(), "normal", null)))
                .isEqualTo(ErrorProgramacion.FECHA_FUERA_DE_SEMANA);
        assertThat(error(agregarOpcion.ejecutar(id, JUEVES, lomo.getId(), "normal", 0)))
                .isEqualTo(ErrorProgramacion.PORCIONES_INVALIDAS);
        assertThat(error(agregarOpcion.ejecutar(id, JUEVES, lomo.getId(), "especial", null)))
                .isEqualTo(ErrorProgramacion.TIPO_MENU_INVALIDO);
    }

    @Test
    void los_dias_anteriores_a_hoy_estan_cerrados() {
        String id = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza().getId();

        assertThat(error(agregarOpcion.ejecutar(id, MARTES, seco.getId(), "normal", null)))
                .isEqualTo(ErrorProgramacion.DIA_CERRADO);
        // Hoy si se puede: el almuerzo de hoy todavia se esta sirviendo.
        assertThat(agregarOpcion.ejecutar(id, MIERCOLES, seco.getId(), "normal", null).esExito()).isTrue();
    }

    @Test
    void cambia_las_porciones_y_quita_opciones_pero_no_en_un_dia_cerrado() {
        String id = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        OpcionMenu opcion = agregarOpcion.ejecutar(id, MIERCOLES, seco.getId(), "normal", 30).valorOLanza();

        OpcionMenu cambiada = actualizarOpcion.ejecutar(opcion.getId(), "extra", 25).valorOLanza();
        assertThat(cambiada.getPorcionesEstimadas()).isEqualTo(25);
        assertThat(cambiada.getTipoMenu()).isEqualTo("extra");

        assertThat(quitarOpcion.ejecutar(opcion.getId()).esExito()).isTrue();
        assertThat(consulta.opcionesDeLaSemana(id)).isEmpty();

        // Al dia siguiente, el miercoles ya es pasado.
        OpcionMenu deHoy = agregarOpcion.ejecutar(id, MIERCOLES, seco.getId(), "normal", 30).valorOLanza();
        when(reloj.instant()).thenReturn(JUEVES.atTime(9, 0).atZone(ConfiguracionReloj.ZONA_DEL_RESTAURANTE)
                .toInstant());
        assertThat(error(actualizarOpcion.ejecutar(deHoy.getId(), "normal", 10)))
                .isEqualTo(ErrorProgramacion.DIA_CERRADO);
        assertThat(error(quitarOpcion.ejecutar(deHoy.getId()))).isEqualTo(ErrorProgramacion.DIA_CERRADO);
    }

    // ---- Advertencia por repeticion (CA3, RN-041) ---------------------------

    @Test
    void advierte_si_un_segundo_se_repite_en_los_tres_dias_anteriores() {
        String id = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        agregarOpcion.ejecutar(id, JUEVES, seco.getId(), "normal", null);
        agregarOpcion.ejecutar(id, SABADO, seco.getId(), "extra", null);
        agregarOpcion.ejecutar(id, SABADO, lomo.getId(), "normal", null);

        List<ConsultaMenu.Advertencia> advertencias = consulta.advertenciasDeLaSemana(id);

        assertThat(advertencias).containsExactly(
                new ConsultaMenu.Advertencia(SABADO, seco.getId(), seco.getNombre(), JUEVES));
    }

    @Test
    void la_repeticion_cruza_semanas_pero_no_pasa_de_tres_dias() {
        String esta = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        String siguiente = crearSemana.ejecutar(LUNES_SIGUIENTE, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        agregarOpcion.ejecutar(esta, VIERNES, seco.getId(), "normal", null);
        agregarOpcion.ejecutar(esta, JUEVES, lomo.getId(), "normal", null);
        agregarOpcion.ejecutar(siguiente, LUNES_SIGUIENTE, seco.getId(), "normal", null);
        agregarOpcion.ejecutar(siguiente, LUNES_SIGUIENTE, lomo.getId(), "normal", null);

        // Seco: viernes -> lunes son 3 dias, se advierte. Estofado: jueves -> lunes son 4, no.
        assertThat(consulta.advertenciasDeLaSemana(siguiente)).containsExactly(
                new ConsultaMenu.Advertencia(LUNES_SIGUIENTE, seco.getId(), seco.getNombre(), VIERNES));
    }

    // ---- Confirmar (CA4, CA5) -----------------------------------------------

    @Test
    void confirma_aunque_haya_repeticiones_y_las_devuelve() {
        String id = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        agregarOpcion.ejecutar(id, JUEVES, seco.getId(), "normal", null);
        agregarOpcion.ejecutar(id, VIERNES, seco.getId(), "normal", null);

        Resultado<List<ConsultaMenu.Advertencia>, ErrorProgramacion> resultado =
                confirmarSemana.ejecutar(id, ADMIN_SEMBRADO_ID);

        assertThat(resultado.valorOLanza()).hasSize(1);
        assertThat(consulta.semana(id).orElseThrow().isConfirmada()).isTrue();
        assertThat(auditorias.findByEntidadAndEntidadIdOrderByOcurridoEnDesc("semana_menu", id))
                .anyMatch(f -> f.getEvento().equals("cambio_estado"));
        assertThat(error(confirmarSemana.ejecutar(id, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorProgramacion.YA_CONFIRMADA);
    }

    @Test
    void no_confirma_una_semana_sin_opciones() {
        String id = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza().getId();

        assertThat(error(confirmarSemana.ejecutar(id, ADMIN_SEMBRADO_ID))).isEqualTo(ErrorProgramacion.SEMANA_VACIA);
    }

    @Test
    void la_semana_confirmada_se_sigue_editando_sin_tocar_el_catalogo() {
        String id = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        agregarOpcion.ejecutar(id, JUEVES, seco.getId(), "normal", 30);
        confirmarSemana.ejecutar(id, ADMIN_SEMBRADO_ID);
        String nombreAntes = seco.getNombre();

        assertThat(agregarOpcion.ejecutar(id, JUEVES, lomo.getId(), "extra", 10).esExito()).isTrue();

        Producto recargado = productos.findById(seco.getId()).orElseThrow();
        assertThat(recargado.getNombre()).isEqualTo(nombreAntes);
        assertThat(recargado.isActivo()).isTrue();
    }

    @Test
    void el_pos_solo_ve_el_menu_de_una_semana_confirmada() {
        String id = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        agregarOpcion.ejecutar(id, JUEVES, seco.getId(), "normal", null);

        assertThat(consulta.menuConfirmadoDel(JUEVES)).isEmpty();

        confirmarSemana.ejecutar(id, ADMIN_SEMBRADO_ID);

        assertThat(consulta.menuConfirmadoDel(JUEVES).orElseThrow())
                .extracting(o -> o.getProducto().getId()).containsExactly(seco.getId());
        assertThat(consulta.menuConfirmadoDel(VIERNES).orElseThrow()).isEmpty();
    }

    @Test
    void el_pos_recibe_las_opciones_en_el_orden_del_menu_y_el_normal_antes_que_el_extra() {
        String id = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        agregarOpcion.ejecutar(id, JUEVES, lomo.getId(), "extra", null);
        agregarOpcion.ejecutar(id, JUEVES, seco.getId(), "normal", null);
        agregarOpcion.ejecutar(id, JUEVES, sopa.getId(), "normal", null);
        confirmarSemana.ejecutar(id, ADMIN_SEMBRADO_ID);

        assertThat(consulta.menuConfirmadoDel(JUEVES).orElseThrow())
                .extracting(o -> o.getProducto().getId())
                .containsExactly(sopa.getId(), seco.getId(), lomo.getId());
    }

    // ---- Borrar la semana ---------------------------------------------------

    @Test
    void solo_se_borra_una_semana_en_borrador() {
        String borrador = crearSemana.ejecutar(LUNES, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        agregarOpcion.ejecutar(borrador, JUEVES, seco.getId(), "normal", null);
        String confirmada = crearSemana.ejecutar(LUNES_SIGUIENTE, ADMIN_SEMBRADO_ID).valorOLanza().getId();
        agregarOpcion.ejecutar(confirmada, LUNES_SIGUIENTE, seco.getId(), "normal", null);
        confirmarSemana.ejecutar(confirmada, ADMIN_SEMBRADO_ID);

        assertThat(eliminarSemana.ejecutar(borrador).esExito()).isTrue();
        assertThat(consulta.semana(borrador)).isEmpty();
        assertThat(error(eliminarSemana.ejecutar(confirmada))).isEqualTo(ErrorProgramacion.SEMANA_CONFIRMADA);
    }

    private static ErrorProgramacion error(Resultado<?, ErrorProgramacion> resultado) {
        assertThat(resultado.esExito()).isFalse();
        return ((Resultado.Fracaso<?, ErrorProgramacion>) resultado).error();
    }

    private Producto plato(String nombre, String categoriaId) {
        CategoriaProducto categoria = categorias.findById(categoriaId).orElseThrow();
        UnidadMedida und = unidades.findAll().stream().filter(u -> u.getCodigo().equals("und")).findFirst()
                .orElseThrow();
        String codigo = "PS-" + Identificador.nuevo().valor().substring(16);
        boolean esInsumo = "insumo".equals(categoria.getNaturaleza());
        return productos.save(new Producto(Identificador.nuevo().valor(), codigo, nombre + " " + codigo, categoria, und,
                !esInsumo, esInsumo, false, esInsumo ? null : 1500L, false, null));
    }
}
