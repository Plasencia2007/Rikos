package pe.rikos.operacion.pensionistas.aplicacion;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecial;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.HorarioDiaEspecial;
import pe.rikos.operacion.identidad.infraestructura.HorarioDiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicioRepositorio;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Que comidas sirve algun local cada dia: lo que hace falta para el tope de
 * credito (sprint 5, 3.10). Lee los horarios que ya cargo cada local (por
 * servicio y dia de la semana) y sus dias especiales: uno cerrado no sirve
 * nada, y uno abierto sirve solo lo que dice. Un local inactivo no cuenta.
 *
 * Un dia sin ninguna comida no aparece en el mapa.
 */
@Component
public class ServiciosDeAtencion {

    private final LocalRepositorio locales;
    private final HorarioServicioRepositorio horarios;
    private final DiaEspecialRepositorio diasEspeciales;
    private final HorarioDiaEspecialRepositorio horariosEspeciales;

    public ServiciosDeAtencion(LocalRepositorio locales, HorarioServicioRepositorio horarios,
                               DiaEspecialRepositorio diasEspeciales, HorarioDiaEspecialRepositorio horariosEspeciales) {
        this.locales = locales;
        this.horarios = horarios;
        this.diasEspeciales = diasEspeciales;
        this.horariosEspeciales = horariosEspeciales;
    }

    /** Con todos los locales activos. */
    @Transactional(readOnly = true)
    public Map<LocalDate, Set<String>> serviciosPorDia(LocalDate desde, LocalDate hasta) {
        return serviciosPorDia(desde, hasta, locales.findAll().stream().map(Local::getId).toList());
    }

    /** Solo con los locales dados (los inactivos se descartan). */
    @Transactional(readOnly = true)
    public Map<LocalDate, Set<String>> serviciosPorDia(LocalDate desde, LocalDate hasta, Collection<String> localIds) {
        Map<LocalDate, Set<String>> resultado = new TreeMap<>();
        for (Local local : locales.findAllById(localIds)) {
            if (local.isActivo()) {
                agregarLocal(local.getId(), desde, hasta, resultado);
            }
        }
        return resultado;
    }

    private void agregarLocal(String localId, LocalDate desde, LocalDate hasta, Map<LocalDate, Set<String>> resultado) {
        Map<String, Set<String>> semanal = new HashMap<>();
        for (HorarioServicio h : horarios.findByLocalIdAndActivoTrue(localId)) {
            semanal.computeIfAbsent(h.getDiaSemana(), d -> new HashSet<>()).add(h.getServicio());
        }
        Map<LocalDate, DiaEspecial> especiales = diasEspeciales.findByLocalIdOrderByFecha(localId).stream()
                .filter(d -> !d.getFecha().isBefore(desde) && !d.getFecha().isAfter(hasta))
                .collect(Collectors.toMap(DiaEspecial::getFecha, d -> d));
        Map<String, Set<String>> deEspeciales = new HashMap<>();
        if (!especiales.isEmpty()) {
            List<String> ids = especiales.values().stream().map(DiaEspecial::getId).toList();
            for (HorarioDiaEspecial h : horariosEspeciales.findByDiaEspecialIdIn(ids)) {
                deEspeciales.computeIfAbsent(h.getDiaEspecialId(), d -> new HashSet<>()).add(h.getServicio());
            }
        }
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            DiaEspecial especial = especiales.get(dia);
            Set<String> servicios;
            if (especial != null) {
                servicios = especial.isCerrado() ? Set.of() : deEspeciales.getOrDefault(especial.getId(), Set.of());
            } else {
                servicios = semanal.getOrDefault(nombreDelDia(dia.getDayOfWeek()), Set.of());
            }
            if (!servicios.isEmpty()) {
                resultado.computeIfAbsent(dia, d -> new HashSet<>()).addAll(servicios);
            }
        }
    }

    /** Los nombres con que se guardan en horario_servicio.dia_semana (sin tildes). */
    private static String nombreDelDia(DayOfWeek dia) {
        if (dia == DayOfWeek.MONDAY) return "lunes";
        if (dia == DayOfWeek.TUESDAY) return "martes";
        if (dia == DayOfWeek.WEDNESDAY) return "miercoles";
        if (dia == DayOfWeek.THURSDAY) return "jueves";
        if (dia == DayOfWeek.FRIDAY) return "viernes";
        if (dia == DayOfWeek.SATURDAY) return "sabado";
        return "domingo";
    }
}
