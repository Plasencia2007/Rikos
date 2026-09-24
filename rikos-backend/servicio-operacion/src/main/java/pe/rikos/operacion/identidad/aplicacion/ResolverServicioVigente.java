package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecial;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.HorarioDiaEspecial;
import pe.rikos.operacion.identidad.infraestructura.HorarioDiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicioRepositorio;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * US-019 CA2: que el kiosco pueda determinar el servicio vigente sin
 * preguntar. Seccion 2 de la ampliacion de US-007: "el dia especial manda
 * sobre el horario semanal" -- primero se busca dia_especial para la fecha;
 * si existe y esta cerrado, no hay servicio; si existe y no esta cerrado,
 * valen sus franjas; solo si no existe se usa horario_servicio del dia de
 * la semana.
 */
@Service
public class ResolverServicioVigente {

    private static final Map<DayOfWeek, String> DIAS = Map.of(
            DayOfWeek.MONDAY, "lunes",
            DayOfWeek.TUESDAY, "martes",
            DayOfWeek.WEDNESDAY, "miercoles",
            DayOfWeek.THURSDAY, "jueves",
            DayOfWeek.FRIDAY, "viernes",
            DayOfWeek.SATURDAY, "sabado",
            DayOfWeek.SUNDAY, "domingo");

    private final DiaEspecialRepositorio diasEspeciales;
    private final HorarioDiaEspecialRepositorio franjasDiaEspecial;
    private final HorarioServicioRepositorio horarios;

    public ResolverServicioVigente(DiaEspecialRepositorio diasEspeciales,
                                    HorarioDiaEspecialRepositorio franjasDiaEspecial,
                                    HorarioServicioRepositorio horarios) {
        this.diasEspeciales = diasEspeciales;
        this.franjasDiaEspecial = franjasDiaEspecial;
        this.horarios = horarios;
    }

    public static String diaSemanaDe(LocalDateTime momento) {
        String dia = DIAS.get(momento.getDayOfWeek());
        return dia == null ? momento.getDayOfWeek().toString().toLowerCase(Locale.ROOT) : dia;
    }

    /** Vacio = el local no atiende a ese momento (cerrado, sin franja vigente, o fuera de todo turno). */
    @Transactional(readOnly = true)
    public Optional<String> resolver(String localId, LocalDateTime momento) {
        Optional<DiaEspecial> diaEspecial = diasEspeciales.findByLocalIdAndFecha(localId, momento.toLocalDate());

        if (diaEspecial.isPresent()) {
            if (diaEspecial.get().isCerrado()) {
                return Optional.empty();
            }
            return franjasDiaEspecial.findByDiaEspecialIdOrderByHoraInicio(diaEspecial.get().getId()).stream()
                    .filter(f -> new RangoHorario(f.getHoraInicio(), f.getHoraFin()).contiene(momento.toLocalTime()))
                    .map(HorarioDiaEspecial::getServicio)
                    .findFirst();
        }

        String diaSemana = diaSemanaDe(momento);
        List<HorarioServicio> delDia = horarios.findByLocalIdAndActivoTrue(localId).stream()
                .filter(h -> h.getDiaSemana().equals(diaSemana))
                .toList();
        return delDia.stream()
                .filter(h -> new RangoHorario(h.getHoraInicio(), h.getHoraFin()).contiene(momento.toLocalTime()))
                .map(HorarioServicio::getServicio)
                .findFirst();
    }
}
