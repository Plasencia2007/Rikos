package pe.rikos.operacion.caja.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** RN-069: solo existen la venta y (sprint 8) el abono en efectivo del pensionista; nada de gastos, retiros ni ingresos extraordinarios. */
@Entity
@Table(name = "movimiento_caja")
public class MovimientoCaja {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "turno_caja_id", nullable = false, length = 26)
    private String turnoCajaId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 10)
    private String medio;

    @Column(nullable = false)
    private long importe;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pago_id", length = 26)
    private String pagoId;

    @Column(name = "ocurrido_en", nullable = false)
    private OffsetDateTime ocurridoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "registrado_por", nullable = false, length = 26)
    private String registradoPor;

    protected MovimientoCaja() {
    }

    public MovimientoCaja(String id, String turnoCajaId, String localId, String tipo, String medio, long importe, String pagoId, OffsetDateTime ocurridoEn, String registradoPor) {
        this.id = id;
        this.turnoCajaId = turnoCajaId;
        this.localId = localId;
        this.tipo = tipo;
        this.medio = medio;
        this.importe = importe;
        this.pagoId = pagoId;
        this.ocurridoEn = ocurridoEn;
        this.registradoPor = registradoPor;
    }

    public String getId() {
        return id;
    }

    public String getTurnoCajaId() {
        return turnoCajaId;
    }

    public String getLocalId() {
        return localId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getMedio() {
        return medio;
    }

    public long getImporte() {
        return importe;
    }

    public String getPagoId() {
        return pagoId;
    }

    public OffsetDateTime getOcurridoEn() {
        return ocurridoEn;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }
}
