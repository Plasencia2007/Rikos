package pe.rikos.operacion.caja.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** Un acto de cobrar lineas de un pedido en el turno de un cajero (V41). De solo agregado: no se modifica. */
@Entity
@Table(name = "cobro")
public class Cobro {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pedido_id", nullable = false, length = 26)
    private String pedidoId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "turno_caja_id", nullable = false, length = 26)
    private String turnoCajaId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cobrado_por", nullable = false, length = 26)
    private String cobradoPor;

    @Column(name = "cobrado_en", nullable = false)
    private OffsetDateTime cobradoEn;

    @Column(nullable = false)
    private long total;

    @Column(name = "total_medios", nullable = false)
    private long totalMedios;

    @Column(name = "total_cuentas", nullable = false)
    private long totalCuentas;

    @Column(nullable = false)
    private long vuelto;

    protected Cobro() {
    }

    public Cobro(String id, String pedidoId, String localId, String turnoCajaId, String cobradoPor, OffsetDateTime cobradoEn, long total, long totalMedios, long totalCuentas, long vuelto) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.localId = localId;
        this.turnoCajaId = turnoCajaId;
        this.cobradoPor = cobradoPor;
        this.cobradoEn = cobradoEn;
        this.total = total;
        this.totalMedios = totalMedios;
        this.totalCuentas = totalCuentas;
        this.vuelto = vuelto;
    }

    public String getId() {
        return id;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public String getLocalId() {
        return localId;
    }

    public String getTurnoCajaId() {
        return turnoCajaId;
    }

    public String getCobradoPor() {
        return cobradoPor;
    }

    public OffsetDateTime getCobradoEn() {
        return cobradoEn;
    }

    public long getTotal() {
        return total;
    }

    public long getTotalMedios() {
        return totalMedios;
    }

    public long getTotalCuentas() {
        return totalCuentas;
    }

    public long getVuelto() {
        return vuelto;
    }
}
