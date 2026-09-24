package pe.rikos.operacion.identidad.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 3.11: lista configurable compartida por todo el sistema (US-012). Sin datos semilla todavia (07 § Pendientes). */
@Entity
@Table(name = "motivo")
public class Motivo {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 120)
    private String descripcion;

    @Column(name = "requiere_observacion", nullable = false)
    private boolean requiereObservacion;

    @Column(nullable = false)
    private boolean activo = true;

    protected Motivo() {
    }

    public Motivo(String id, String tipo, String descripcion, boolean requiereObservacion) {
        this.id = id;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.requiereObservacion = requiereObservacion;
        this.activo = true;
    }

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isRequiereObservacion() {
        return requiereObservacion;
    }

    public boolean isActivo() {
        return activo;
    }
}
