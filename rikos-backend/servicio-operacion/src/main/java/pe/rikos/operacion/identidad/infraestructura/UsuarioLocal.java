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
 * Ver la nota en UsuarioRol sobre por que los @Id son String planos y no
 * @ManyToOne (JdbcTypeCode no se propaga dentro de una clave compuesta con
 * @IdClass).
 */
@Entity
@Table(name = "usuario_local")
@IdClass(UsuarioLocalId.class)
public class UsuarioLocal {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "usuario_id", length = 26)
    private String usuarioId;

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", length = 26)
    private String localId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", insertable = false, updatable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_id", insertable = false, updatable = false)
    private Local local;

    @Column(name = "es_principal", nullable = false)
    private boolean esPrincipal;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creado_por", length = 26)
    private String creadoPor;

    protected UsuarioLocal() {
    }

    public UsuarioLocal(Usuario usuario, Local local, boolean esPrincipal, String creadoPor) {
        this.usuario = usuario;
        this.usuarioId = usuario.getId();
        this.local = local;
        this.localId = local.getId();
        this.esPrincipal = esPrincipal;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Local getLocal() {
        return local;
    }

    public boolean isEsPrincipal() {
        return esPrincipal;
    }
}
