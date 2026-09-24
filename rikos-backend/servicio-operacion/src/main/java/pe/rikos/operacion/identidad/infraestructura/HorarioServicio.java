package pe.rikos.operacion.identidad.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalTime;

/**
 * modelo-de-datos.md 3.3. Un turno de un servicio en un dia de la semana de
 * un local. Un dia con dos turnos del mismo servicio (12-15 y 19-23:30) son
 * dos filas, no una columna con rango multiple.
 */
@Entity
@Table(name = "horario_servicio")
public class HorarioServicio {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @Column(nullable = false, length = 20)
    private String servicio;

    @Column(name = "dia_semana", nullable = false, length = 20)
    private String diaSemana;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    private boolean activo = true;

    protected HorarioServicio() {
    }

    public HorarioServicio(String id, String localId, String servicio, String diaSemana, LocalTime horaInicio,
                            LocalTime horaFin) {
        this.id = id;
        this.localId = localId;
        this.servicio = servicio;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.activo = true;
    }

    public void desactivar() {
        this.activo = false;
    }

    public String getId() {
        return id;
    }

    public String getLocalId() {
        return localId;
    }

    public String getServicio() {
        return servicio;
    }

    public String getDiaSemana() {
        return diaSemana;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public boolean isActivo() {
        return activo;
    }
}
