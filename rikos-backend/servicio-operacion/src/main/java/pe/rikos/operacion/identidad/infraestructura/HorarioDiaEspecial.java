package pe.rikos.operacion.identidad.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;

/** modelo-de-datos.md 3.3 ter. Solo existen filas si el dia especial no esta cerrado. */
@Entity
@Table(name = "horario_dia_especial")
public class HorarioDiaEspecial {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "dia_especial_id", nullable = false, length = 26)
    private String diaEspecialId;

    @Column(nullable = false, length = 20)
    private String servicio;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    protected HorarioDiaEspecial() {
    }

    public HorarioDiaEspecial(String id, String diaEspecialId, String servicio, LocalTime horaInicio,
                               LocalTime horaFin) {
        this.id = id;
        this.diaEspecialId = diaEspecialId;
        this.servicio = servicio;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public String getId() {
        return id;
    }

    public String getDiaEspecialId() {
        return diaEspecialId;
    }

    public String getServicio() {
        return servicio;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }
}
