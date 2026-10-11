package pe.rikos.operacion.pensionistas.aplicacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.operacion.comun.auditoria.ContextoDeSeguridadDePrueba;
import pe.rikos.operacion.identidad.aplicacion.CambiarEstadoLocal;
import pe.rikos.operacion.identidad.aplicacion.CrearLocal;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecial;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.HorarioDiaEspecial;
import pe.rikos.operacion.identidad.infraestructura.HorarioDiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicioRepositorio;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Que comidas sirve un local cada dia, leyendo sus horarios reales (por dia de
 * la semana) y sus dias especiales. Semana del lunes 09/06/2031 al domingo
 * 15/06/2031. Se acota a los locales de la prueba: la base de desarrollo tiene
 * otros con sus propios horarios.
 */
@SpringBootTest
@Transactional
class ServiciosDeAtencionTest {

    private static final String ADMIN_SEMBRADO_ID = "01M2QZ606763Q7XC00M8VD7WM1";
    private static final LocalDate LUNES = LocalDate.of(2031, 6, 9);
    private static final LocalDate DOMINGO = LocalDate.of(2031, 6, 15);

    @Autowired
    private ServiciosDeAtencion atencion;
    @Autowired
    private CrearLocal crearLocal;
    @Autowired
    private CambiarEstadoLocal cambiarEstadoLocal;
    @Autowired
    private HorarioServicioRepositorio horarios;
    @Autowired
    private DiaEspecialRepositorio diasEspeciales;
    @Autowired
    private HorarioDiaEspecialRepositorio horariosEspeciales;

    @BeforeEach
    void autenticar() {
        ContextoDeSeguridadDePrueba.autenticarComo(ADMIN_SEMBRADO_ID);
    }

    @AfterEach
    void limpiarContexto() {
        ContextoDeSeguridadDePrueba.limpiar();
    }

    private String local(String prefijo) {
        String codigo = prefijo + Identificador.nuevo().valor().substring(20).toLowerCase();
        return crearLocal.ejecutar(codigo, "Local " + codigo, "Direccion", ADMIN_SEMBRADO_ID).valorOLanza().getId();
    }

    private void sirve(String localId, String servicio, String... dias) {
        for (String dia : dias) {
            horarios.save(new HorarioServicio(Identificador.nuevo().valor(), localId, servicio, dia,
                    LocalTime.of(12, 0), LocalTime.of(15, 0)));
        }
    }

    @Test
    void cuenta_las_comidas_de_cada_dia_segun_el_horario_semanal() {
        String local = local("sa");
        sirve(local, "almuerzo", "lunes", "martes", "miercoles", "jueves", "viernes");
        sirve(local, "cena", "lunes", "martes");

        Map<LocalDate, Set<String>> dias = atencion.serviciosPorDia(LUNES, DOMINGO, List.of(local));

        assertThat(dias.get(LUNES)).containsExactlyInAnyOrder("almuerzo", "cena");
        assertThat(dias.get(LUNES.plusDays(1))).containsExactlyInAnyOrder("almuerzo", "cena");
        assertThat(dias.get(LUNES.plusDays(2))).containsExactly("almuerzo");
        assertThat(dias.get(LUNES.plusDays(4))).containsExactly("almuerzo");
        // Sabado y domingo no se sirve nada: el dia no esta en el mapa o esta vacio.
        assertThat(dias.getOrDefault(LUNES.plusDays(5), Set.of())).isEmpty();
        assertThat(dias.getOrDefault(DOMINGO, Set.of())).isEmpty();
    }

    @Test
    void un_dia_especial_cerrado_no_cuenta_y_uno_abierto_usa_sus_propias_comidas() {
        String local = local("sb");
        sirve(local, "almuerzo", "lunes", "martes", "miercoles", "jueves", "viernes");
        LocalDate miercoles = LUNES.plusDays(2);
        LocalDate sabado = LUNES.plusDays(5);
        diasEspeciales.save(new DiaEspecial(Identificador.nuevo().valor(), local, miercoles, true, "Feriado"));
        DiaEspecial abierto = diasEspeciales.save(
                new DiaEspecial(Identificador.nuevo().valor(), local, sabado, false, "Día de la Canción Criolla"));
        horariosEspeciales.save(new HorarioDiaEspecial(Identificador.nuevo().valor(), abierto.getId(), "almuerzo",
                LocalTime.of(12, 0), LocalTime.of(16, 0)));

        Map<LocalDate, Set<String>> dias = atencion.serviciosPorDia(LUNES, DOMINGO, List.of(local));

        assertThat(dias.getOrDefault(miercoles, Set.of())).isEmpty();
        assertThat(dias.get(sabado)).containsExactly("almuerzo");
        assertThat(dias.get(LUNES)).containsExactly("almuerzo");
    }

    @Test
    void basta_con_que_un_local_sirva_la_comida_ese_dia_y_un_local_inactivo_no_cuenta() {
        String uno = local("sc");
        String otro = local("sd");
        String inactivo = local("se");
        sirve(uno, "almuerzo", "lunes");
        sirve(otro, "cena", "lunes");
        sirve(inactivo, "desayuno", "lunes");
        cambiarEstadoLocal.ejecutar(inactivo, false, ADMIN_SEMBRADO_ID);

        Map<LocalDate, Set<String>> dias = atencion.serviciosPorDia(LUNES, LUNES, List.of(uno, otro, inactivo));

        assertThat(dias.get(LUNES)).containsExactlyInAnyOrder("almuerzo", "cena");
    }

    @Test
    void sin_indicar_locales_usa_todos_los_activos() {
        String local = local("sf");
        sirve(local, "desayuno", "lunes");

        assertThat(atencion.serviciosPorDia(LUNES, LUNES).get(LUNES)).contains("desayuno");
    }
}
