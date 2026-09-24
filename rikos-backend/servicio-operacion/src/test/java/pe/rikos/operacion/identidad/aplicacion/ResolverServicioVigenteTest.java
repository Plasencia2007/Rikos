package pe.rikos.operacion.identidad.aplicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecial;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.HorarioDiaEspecial;
import pe.rikos.operacion.identidad.infraestructura.HorarioDiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicioRepositorio;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Seccion 2 de la ampliacion de US-007: "el dia especial manda sobre el
 * horario semanal". Este es el caso que hay que probar explicitamente,
 * porque es facil de romper agregando "un OR mas" al resolver.
 */
@ExtendWith(MockitoExtension.class)
class ResolverServicioVigenteTest {

    private static final String LOCAL_ID = "local-1";

    @Mock
    private DiaEspecialRepositorio diasEspeciales;
    @Mock
    private HorarioDiaEspecialRepositorio franjasDiaEspecial;
    @Mock
    private HorarioServicioRepositorio horarios;

    private ResolverServicioVigente resolver;

    @BeforeEach
    void preparar() {
        resolver = new ResolverServicioVigente(diasEspeciales, franjasDiaEspecial, horarios);
    }

    @Test
    void sinDiaEspecialUsaElHorarioSemanalDelDiaCorrespondiente() {
        LocalDate unMartes = LocalDate.of(2026, 9, 22);
        when(diasEspeciales.findByLocalIdAndFecha(LOCAL_ID, unMartes)).thenReturn(Optional.empty());
        when(horarios.findByLocalIdAndActivoTrue(LOCAL_ID)).thenReturn(List.of(
                new HorarioServicio("h1", LOCAL_ID, "almuerzo", "martes", LocalTime.of(12, 0), LocalTime.of(15, 0))));

        Optional<String> servicio = resolver.resolver(LOCAL_ID, LocalDateTime.of(unMartes, LocalTime.of(13, 0)));

        assertEquals(Optional.of("almuerzo"), servicio);
    }

    @Test
    void unDiaEspecialCerradoGanaAunSiElHorarioSemanalTendriaAlmuerzoEseDia() {
        LocalDate feriado = LocalDate.of(2026, 7, 28);
        DiaEspecial diaEspecial = new DiaEspecial("d1", LOCAL_ID, feriado, true, "Fiestas Patrias");
        when(diasEspeciales.findByLocalIdAndFecha(LOCAL_ID, feriado)).thenReturn(Optional.of(diaEspecial));

        Optional<String> servicio = resolver.resolver(LOCAL_ID, LocalDateTime.of(feriado, LocalTime.of(13, 0)));

        assertEquals(Optional.empty(), servicio, "cerrado por dia especial: no importa lo que diga el horario semanal");
        // El horario semanal ni siquiera se debio consultar: el dia especial ya resolvio la pregunta.
        org.mockito.Mockito.verifyNoInteractions(horarios);
    }

    @Test
    void unDiaEspecialAbiertoConHorarioPropioGanaSobreElHorarioSemanalDelMismoDia() {
        // Un martes cualquiera el local abre 12-15 de almuerzo, pero esta fecha puntual
        // es un dia especial con un horario reducido de 12-13:30. Debe usarse el del
        // dia especial, no el semanal, aunque ambos existan.
        LocalDate fechaEspecial = LocalDate.of(2026, 12, 24);
        DiaEspecial diaEspecial = new DiaEspecial("d2", LOCAL_ID, fechaEspecial, false, "Nochebuena, horario corto");
        when(diasEspeciales.findByLocalIdAndFecha(LOCAL_ID, fechaEspecial)).thenReturn(Optional.of(diaEspecial));
        when(franjasDiaEspecial.findByDiaEspecialIdOrderByHoraInicio("d2")).thenReturn(List.of(
                new HorarioDiaEspecial("f1", "d2", "almuerzo", LocalTime.of(12, 0), LocalTime.of(13, 30))));

        Optional<String> dentroDelHorarioCorto =
                resolver.resolver(LOCAL_ID, LocalDateTime.of(fechaEspecial, LocalTime.of(13, 0)));
        Optional<String> fueraDelHorarioCorto =
                resolver.resolver(LOCAL_ID, LocalDateTime.of(fechaEspecial, LocalTime.of(14, 0)));

        assertEquals(Optional.of("almuerzo"), dentroDelHorarioCorto);
        assertEquals(Optional.empty(), fueraDelHorarioCorto,
                "a las 14:00 el horario semanal normal diria almuerzo, pero el dia especial ya cerro a las 13:30");
        org.mockito.Mockito.verifyNoInteractions(horarios);
    }
}
