package pe.rikos.operacion.identidad.aplicacion;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.rikos.contratos.Identificador;
import pe.rikos.contratos.Resultado;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecial;
import pe.rikos.operacion.identidad.infraestructura.DiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.HorarioDiaEspecial;
import pe.rikos.operacion.identidad.infraestructura.HorarioDiaEspecialRepositorio;
import pe.rikos.operacion.identidad.infraestructura.LocalRepositorio;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** modelo-de-datos.md 3.3 bis / 3.3 ter. */
@Service
public class CrearDiaEspecial {

    public record Franja(String servicio, LocalTime horaInicio, LocalTime horaFin) {
    }

    private final LocalRepositorio locales;
    private final DiaEspecialRepositorio diasEspeciales;
    private final HorarioDiaEspecialRepositorio franjas;

    public CrearDiaEspecial(LocalRepositorio locales, DiaEspecialRepositorio diasEspeciales,
                             HorarioDiaEspecialRepositorio franjas) {
        this.locales = locales;
        this.diasEspeciales = diasEspeciales;
        this.franjas = franjas;
    }

    @Transactional
    public Resultado<DiaEspecial, ErrorHorario> ejecutar(String localId, LocalDate fecha, boolean cerrado,
                                                           String descripcion, List<Franja> franjasEntrada) {
        if (locales.findById(localId).isEmpty()) {
            return Resultado.fracaso(ErrorHorario.LOCAL_NO_ENCONTRADO);
        }
        if (diasEspeciales.existsByLocalIdAndFecha(localId, fecha)) {
            return Resultado.fracaso(ErrorHorario.FECHA_DUPLICADA);
        }
        // 3.3 ter: "solo existen filas si el dia especial no esta cerrado".
        List<Franja> franjasAGuardar = cerrado ? List.of() : franjasEntrada;
        if (haySolapamiento(franjasAGuardar)) {
            return Resultado.fracaso(ErrorHorario.FRANJAS_SOLAPADAS);
        }

        DiaEspecial diaEspecial = diasEspeciales.save(
                new DiaEspecial(Identificador.nuevo().valor(), localId, fecha, cerrado, descripcion));
        for (Franja franja : franjasAGuardar) {
            this.franjas.save(new HorarioDiaEspecial(Identificador.nuevo().valor(), diaEspecial.getId(),
                    franja.servicio(), franja.horaInicio(), franja.horaFin()));
        }
        return Resultado.exito(diaEspecial);
    }

    private boolean haySolapamiento(List<Franja> franjas) {
        for (int i = 0; i < franjas.size(); i++) {
            for (int j = i + 1; j < franjas.size(); j++) {
                Franja a = franjas.get(i);
                Franja b = franjas.get(j);
                if (!a.servicio().equals(b.servicio())) {
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
