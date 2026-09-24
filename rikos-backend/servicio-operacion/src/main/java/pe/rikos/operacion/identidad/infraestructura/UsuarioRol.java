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
 * Los campos @Id son String planos, no @ManyToOne: Hibernate no propaga
 * @JdbcTypeCode(CHAR) a una asociacion que forma parte de una clave
 * compuesta con @IdClass (queda como varchar y la validacion del esquema
 * falla contra el char(26) real de la tabla). Se agregan @ManyToOne de
 * solo lectura (insertable=false, updatable=false) sobre las mismas
 * columnas para poder navegar a Usuario/Rol sin volver a consultarlos.
 */
@Entity
@Table(name = "usuario_rol")
@IdClass(UsuarioRolId.class)
public class UsuarioRol {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "usuario_id", length = 26)
    private String usuarioId;

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "rol_id", length = 26)
    private String rolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", insertable = false, updatable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rol_id", insertable = false, updatable = false)
    private Rol rol;

    @Column(name = "asignado_en", nullable = false)
    private OffsetDateTime asignadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "asignado_por", nullable = false, length = 26)
    private String asignadoPor;

    protected UsuarioRol() {
    }

    public UsuarioRol(Usuario usuario, Rol rol, String asignadoPor) {
        this.usuario = usuario;
        this.usuarioId = usuario.getId();
        this.rol = rol;
        this.rolId = rol.getId();
        this.asignadoEn = OffsetDateTime.now();
        this.asignadoPor = asignadoPor;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Rol getRol() {
        return rol;
    }
}
