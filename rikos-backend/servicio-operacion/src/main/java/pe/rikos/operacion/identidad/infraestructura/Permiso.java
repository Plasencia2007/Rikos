package pe.rikos.operacion.identidad.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "permiso")
public class Permiso {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(nullable = false, length = 40)
    private String modulo;

    @Column(nullable = false, length = 30)
    private String accion;

    @Column(nullable = false, length = 80)
    private String codigo;

    @Column(length = 200)
    private String descripcion;

    protected Permiso() {
    }

    public Permiso(String id, String modulo, String accion, String codigo, String descripcion) {
        this.id = id;
        this.modulo = modulo;
        this.accion = accion;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public String getId() {
        return id;
    }

    public String getModulo() {
        return modulo;
    }

    public String getAccion() {
        return accion;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
