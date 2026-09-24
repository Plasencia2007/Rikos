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

/**
 * 3.9: permisos otorgados o retirados a un usuario concreto, por encima de
 * su rol. Ver la nota en UsuarioRol sobre por que los @Id son String planos.
 */
@Entity
@Table(name = "usuario_permiso_excepcion")
@IdClass(UsuarioPermisoExcepcionId.class)
public class UsuarioPermisoExcepcion {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "usuario_id", length = 26)
    private String usuarioId;

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "permiso_id", length = 26)
    private String permisoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", insertable = false, updatable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permiso_id", insertable = false, updatable = false)
    private Permiso permiso;

    @Column(nullable = false)
    private boolean otorgado;

    @Column(length = 200)
    private String motivo;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creado_por", length = 26)
    private String creadoPor;

    protected UsuarioPermisoExcepcion() {
    }

    public UsuarioPermisoExcepcion(Usuario usuario, Permiso permiso, boolean otorgado, String motivo,
                                    String creadoPor) {
        this.usuario = usuario;
        this.usuarioId = usuario.getId();
        this.permiso = permiso;
        this.permisoId = permiso.getId();
        this.otorgado = otorgado;
        this.motivo = motivo;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Permiso getPermiso() {
        return permiso;
    }

    public boolean isOtorgado() {
        return otorgado;
    }

    public String getMotivo() {
        return motivo;
    }
}
