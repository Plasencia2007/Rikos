package pe.rikos.operacion.catalogo.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.catalogo.infraestructura.SemanaMenu;
import pe.rikos.operacion.catalogo.infraestructura.SemanaMenuRepositorio;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** US-013: una semana nace en borrador, de lunes a domingo, sin dias (se crean al programarlos). */
@Service
public class CrearSemanaMenu {

    private final SemanaMenuRepositorio semanas;
    private final ConsultaMenu consulta;

    public CrearSemanaMenu(SemanaMenuRepositorio semanas, ConsultaMenu consulta) {
        this.semanas = semanas;
        this.consulta = consulta;
    }

    @Transactional
    public Resultado<SemanaMenu, ErrorProgramacion> ejecutar(LocalDate fechaInicio, String actorId) {
        if (fechaInicio == null || fechaInicio.getDayOfWeek() != DayOfWeek.MONDAY) {
            return Resultado.fracaso(ErrorProgramacion.NO_ES_LUNES);
        }
        if (fechaInicio.plusDays(6).isBefore(consulta.hoy())) {
            return Resultado.fracaso(ErrorProgramacion.SEMANA_PASADA);
        }
        if (semanas.existsByFechaInicio(fechaInicio)) {
            return Resultado.fracaso(ErrorProgramacion.SEMANA_YA_EXISTE);
        }
        return Resultado.exito(semanas.save(new SemanaMenu(Identificador.nuevo().valor(), fechaInicio, actorId)));
    }
}
