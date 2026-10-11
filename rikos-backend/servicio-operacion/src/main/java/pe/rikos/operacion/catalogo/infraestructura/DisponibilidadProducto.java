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

import java.time.OffsetDateTime;

/**
 * 6.4 (US-015): el ultimo estado de un producto en un local. localId es un
 * id plano y no una relacion a Local -- Local vive en el modulo identidad y
 * catalogo no depende de el (Spring Modulith).
 */
@Entity
@Table(name = "disponibilidad_producto")
public class DisponibilidadProducto {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private boolean agotado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "motivo_id")
    private Motivo motivo;

    @Column(length = 250)
    private String observacion;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "marcado_por", nullable = false, length = 26)
    private String marcadoPor;

    @Column(name = "marcado_en", nullable = false)
    private OffsetDateTime marcadoEn;

    protected DisponibilidadProducto() {
    }

    public DisponibilidadProducto(String id, String localId, Producto producto) {
        this.id = id;
        this.localId = localId;
        this.producto = producto;
    }

    public void marcarAgotado(String actorId) {
        this.agotado = true;
        this.motivo = null;
        this.observacion = null;
        this.marcadoPor = actorId;
        this.marcadoEn = OffsetDateTime.now();
    }

    public void reactivar(Motivo motivo, String observacion, String actorId) {
        this.agotado = false;
        this.motivo = motivo;
        this.observacion = observacion;
        this.marcadoPor = actorId;
        this.marcadoEn = OffsetDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getLocalId() {
        return localId;
    }

    public Producto getProducto() {
        return producto;
    }

    public boolean isAgotado() {
        return agotado;
    }

    public Motivo getMotivo() {
        return motivo;
    }

    public String getObservacion() {
        return observacion;
    }

    public String getMarcadoPor() {
        return marcadoPor;
    }

    public OffsetDateTime getMarcadoEn() {
        return marcadoEn;
    }
}
