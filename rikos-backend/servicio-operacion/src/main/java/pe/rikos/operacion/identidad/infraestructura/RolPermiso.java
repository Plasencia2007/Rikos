package pe.rikos.operacion.identidad.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** 3.6: relacion N-N entre rol y permiso. Ver la nota en UsuarioRol sobre por que los @Id son String planos. */
@Entity
@Table(name = "rol_permiso")
@IdClass(RolPermisoId.class)
public class RolPermiso {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "rol_id", length = 26)
    private String rolId;

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "permiso_id", length = 26)
    private String permisoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", insertable = false, updatable = false)
    private Rol rol;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permiso_id", insertable = false, updatable = false)
    private Permiso permiso;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creado_por", length = 26)
    private String creadoPor;

    protected RolPermiso() {
    }

    public RolPermiso(Rol rol, Permiso permiso, String creadoPor) {
        this.rol = rol;
        this.rolId = rol.getId();
        this.permiso = permiso;
        this.permisoId = permiso.getId();
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public Rol getRol() {
        return rol;
    }

    public Permiso getPermiso() {
        return permiso;
    }
}
