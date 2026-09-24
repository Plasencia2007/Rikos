package pe.rikos.operacion.identidad.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "rol")
public class Rol {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(length = 200)
    private String descripcion;

    @Column(name = "es_sistema", nullable = false)
    private boolean esSistema;

    @Column(length = 7)
    private String color;

    @Column(nullable = false)
    private boolean activo = true;

    protected Rol() {
    }

    public Rol(String id, String nombre, String descripcion, boolean esSistema) {
        this(id, nombre, descripcion, esSistema, null);
    }

    public Rol(String id, String nombre, String descripcion, boolean esSistema, String color) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.esSistema = esSistema;
        this.color = color;
        this.activo = true;
    }

    /** Los seis roles de sistema no se pueden renombrar ni borrar (3.4), pero sus permisos y su color sí. */
    public void actualizar(String nombre, String descripcion, String color) {
        if (!esSistema) {
            this.nombre = nombre;
        }
        this.descripcion = descripcion;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isEsSistema() {
        return esSistema;
    }

    public String getColor() {
        return color;
    }

    public boolean isActivo() {
        return activo;
    }
}
