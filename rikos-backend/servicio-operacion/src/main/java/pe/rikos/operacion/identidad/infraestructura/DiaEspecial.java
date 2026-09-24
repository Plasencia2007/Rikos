package pe.rikos.operacion.identidad.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/**
 * modelo-de-datos.md 3.3 bis. Feriados y dias con horario distinto del
 * habitual. Manda sobre horario_servicio: si existe una fila para la
 * fecha, se ignora por completo lo que diga el horario semanal ese dia.
 *
 * Sin navegacion de objeto hacia sus franjas (HorarioDiaEspecial): se
 * consultan aparte por dia_especial_id, igual que el resto del modulo
 * evita @OneToMany para no acoplar la lectura a un unico caso de uso.
 */
@Entity
@Table(name = "dia_especial")
public class DiaEspecial {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private boolean cerrado;

    @Column(length = 120)
    private String descripcion;

    protected DiaEspecial() {
    }

    public DiaEspecial(String id, String localId, LocalDate fecha, boolean cerrado, String descripcion) {
        this.id = id;
        this.localId = localId;
        this.fecha = fecha;
        this.cerrado = cerrado;
        this.descripcion = descripcion;
    }

    public String getId() {
        return id;
    }

    public String getLocalId() {
        return localId;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public boolean isCerrado() {
        return cerrado;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
