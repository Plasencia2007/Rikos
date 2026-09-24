package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicio;
import pe.rikos.operacion.identidad.infraestructura.HorarioServicioRepositorio;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reemplaza el horario semanal completo de un local en una sola operacion
 * (seccion 3 de la ampliacion: "boton para agregar otro turno", "boton para
 * copiar el horario de un dia a otros" -- ambos se resuelven armando la
 * lista completa en el cliente y guardandola de una vez, no turno a turno).
 *
 * No hay historial que preservar en horario_servicio (no esta en la lista
 * de "nunca se borra" de 2.5), asi que reemplazar es borrar todo lo
 * anterior del local e insertar lo nuevo, no un diff turno por turno.
 */
@Service
public class GuardarHorarioSemanal {

    public record Turno(String diaSemana, String servicio, LocalTime horaInicio, LocalTime horaFin) {
    }

    private final LocalRepositorio locales;
    private final HorarioServicioRepositorio horarios;

    public GuardarHorarioSemanal(LocalRepositorio locales, HorarioServicioRepositorio horarios) {
        this.locales = locales;
        this.horarios = horarios;
    }

    @Transactional
    public Resultado<List<HorarioServicio>, ErrorHorario> ejecutar(String localId, List<Turno> turnos) {
        if (locales.findById(localId).isEmpty()) {
            return Resultado.fracaso(ErrorHorario.LOCAL_NO_ENCONTRADO);
        }

        if (haySolapamiento(turnos)) {
            return Resultado.fracaso(ErrorHorario.FRANJAS_SOLAPADAS);
        }

        // Hibernate encola los DELETE derivados de "deleteBy" despues de los
        // INSERT en el orden normal de flush, sin importar el orden en que
        // se llaman -- sin este flush explicito, los INSERT de abajo chocan
        // con las filas viejas que todavia no se borraron y violan el
        // indice parcial (ux_horario_servicio_activo).
        horarios.deleteByLocalId(localId);
        horarios.flush();
        List<HorarioServicio> guardados = new ArrayList<>();
        for (Turno turno : turnos) {
            guardados.add(horarios.save(new HorarioServicio(Identificador.nuevo().valor(), localId, turno.servicio(),
                    turno.diaSemana(), turno.horaInicio(), turno.horaFin())));
        }
        return Resultado.exito(guardados);
    }

    /** Solo se comparan turnos del mismo dia y el mismo servicio entre si (seccion 2). */
    private boolean haySolapamiento(List<Turno> turnos) {
        for (int i = 0; i < turnos.size(); i++) {
            for (int j = i + 1; j < turnos.size(); j++) {
                Turno a = turnos.get(i);
                Turno b = turnos.get(j);
                if (!a.diaSemana().equals(b.diaSemana()) || !a.servicio().equals(b.servicio())) {
                    continue;
                }
                RangoHorario rangoA = new RangoHorario(a.horaInicio(), a.horaFin());
                RangoHorario rangoB = new RangoHorario(b.horaInicio(), b.horaFin());
                if (rangoA.seSolapaCon(rangoB)) {
                    return true;
                }
            }
        }
        return false;
    }
}
