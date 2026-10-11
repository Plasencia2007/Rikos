package pe.rikos.operacion.pensionistas.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 7.4: el responsable del pensionista. Obligatorio, con sus seis datos; relacion 1-1. */
@Entity
@Table(name = "responsable")
public class Responsable {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pensionista_id", nullable = false, length = 26)
    private String pensionistaId;

    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 8)
    private String dni;

    @Column(nullable = false, length = 20)
    private String telefono;

    @Column(nullable = false, length = 160)
    private String correo;

    @Column(nullable = false, length = 60)
    private String parentesco;

    @Column(nullable = false, length = 250)
    private String direccion;

    protected Responsable() {
    }

    public Responsable(String id, String pensionistaId, String nombreCompleto, String dni, String telefono,
                       String correo, String parentesco, String direccion) {
        this.id = id;
        this.pensionistaId = pensionistaId;
        actualizar(nombreCompleto, dni, telefono, correo, parentesco, direccion);
    }

    public void actualizar(String nombreCompleto, String dni, String telefono, String correo, String parentesco,
                           String direccion) {
        this.nombreCompleto = nombreCompleto;
        this.dni = dni;
        this.telefono = telefono;
        this.correo = correo;
        this.parentesco = parentesco;
        this.direccion = direccion;
    }

    public String getId() {
        return id;
    }

    public String getPensionistaId() {
        return pensionistaId;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getDni() {
        return dni;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public String getParentesco() {
        return parentesco;
    }

    public String getDireccion() {
        return direccion;
    }
}
