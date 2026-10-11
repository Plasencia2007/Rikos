package pe.rikos.operacion.caja.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** 9.1: una caja de un local. Cada caja tiene su propio turno y su propio dinero: no se cruzan entre si. */
@Entity
@Table(name = "caja")
public class Caja {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @Column(nullable = false, length = 60)
    private String nombre;

    /** Codigo corto unico en el local (CAJ-01...): lo que se ve en el selector al abrir turno. */
    @Column(nullable = false, length = 20)
    private String codigo;

    /** La caja que viene elegida al abrir turno; a lo sumo una por local. */
    @Column(nullable = false)
    private boolean principal;

    @Column(nullable = false)
    private boolean activo = true;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creado_por", length = 26)
    private String creadoPor;

    protected Caja() {
    }

    public Caja(String id, String localId, String nombre, String codigo, String creadoPor) {
        this.id = id;
        this.localId = localId;
        this.nombre = nombre;
        this.codigo = codigo;
        this.creadoPor = creadoPor;
        this.creadoEn = OffsetDateTime.now();
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }

    public void actualizar(String nombre, String codigo) {
        this.nombre = nombre;
        this.codigo = codigo;
    }

    public void marcarPrincipal(boolean principal) {
        this.principal = principal;
    }

    public String getCodigo() {
        return codigo;
    }

    public boolean isPrincipal() {
        return principal;
    }

    public String getId() {
        return id;
    }

    public String getLocalId() {
        return localId;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActivo() {
        return activo;
    }
}
