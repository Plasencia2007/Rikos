package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

/** 6.2 (US-013). Se crea al programarle la primera opcion: un dia sin opciones es un dia sin almuerzo. */
@Entity
@Table(name = "dia_menu")
public class DiaMenu {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "semana_menu_id", nullable = false)
    private SemanaMenu semana;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 250)
    private String observacion;

    protected DiaMenu() {
    }

    public DiaMenu(String id, SemanaMenu semana, LocalDate fecha) {
        this.id = id;
        this.semana = semana;
        this.fecha = fecha;
    }

    public String getId() {
        return id;
    }

    public SemanaMenu getSemana() {
        return semana;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getObservacion() {
        return observacion;
    }
}
