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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
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
        return franjaVigente(localId, momento).map(Franja::servicio);
    }

    public record Franja(String servicio, LocalDateTime inicio, LocalDateTime fin) {
        public boolean contiene(LocalDateTime momento) {
            return !momento.isBefore(inicio) && momento.isBefore(fin);
        }
    }

    @Transactional(readOnly = true)
    public Optional<Franja> franjaVigente(String localId, LocalDateTime momento) {
        LocalDate fecha = momento.toLocalDate();
        Optional<Franja> actual = franjasDe(localId, fecha).stream().filter(f -> f.contiene(momento)).findFirst();
        // Un dia especial sustituye el dia civil completo, incluso el arrastre de ayer.
        if (actual.isPresent() || diasEspeciales.findByLocalIdAndFecha(localId, fecha).isPresent()) return actual;
        return franjasDe(localId, fecha.minusDays(1)).stream().filter(f -> f.contiene(momento)).findFirst();
    }

    @Transactional(readOnly = true)
    public Optional<Franja> siguienteFranja(String localId, LocalDateTime momento) {
        for (int i = 0; i < 8; i++) {
            Optional<Franja> siguiente = franjasDe(localId, momento.toLocalDate().plusDays(i)).stream()
                    .filter(f -> f.inicio().isAfter(momento)).min(Comparator.comparing(Franja::inicio));
            if (siguiente.isPresent()) return siguiente;
        }
        return Optional.empty();
    }

    private List<Franja> franjasDe(String localId, LocalDate fecha) {
        Optional<DiaEspecial> especial = diasEspeciales.findByLocalIdAndFecha(localId, fecha);
        if (especial.isPresent()) {
            if (especial.get().isCerrado()) return List.of();
            return franjasDiaEspecial.findByDiaEspecialIdOrderByHoraInicio(especial.get().getId()).stream()
                    .map(h -> franja(localId, fecha, h.getServicio(), h.getHoraInicio(), h.getHoraFin())).toList();
        }
        return horarios.findByLocalIdAndActivoTrue(localId).stream()
                .filter(h -> h.getDiaSemana().equals(diaSemanaDe(fecha.atStartOfDay())))
                .map(h -> franja(localId, fecha, h.getServicio(), h.getHoraInicio(), h.getHoraFin())).toList();
    }

    private Franja franja(String localId, LocalDate fecha, String servicio, LocalTime inicio, LocalTime fin) {
        LocalDateTime termina = fecha.atTime(fin);
        if (!fin.isAfter(inicio)) {
            termina = termina.plusDays(1);
            if (diasEspeciales.findByLocalIdAndFecha(localId, fecha.plusDays(1)).isPresent()) {
                termina = fecha.plusDays(1).atStartOfDay();
            }
        }
        return new Franja(servicio, fecha.atTime(inicio), termina);
    }
}
