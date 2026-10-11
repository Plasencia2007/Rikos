package pe.rikos.operacion.caja.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Una parte del cobro en un medio. importe = lo que entra a la caja; en efectivo, montoRecibido puede ser mayor (la diferencia es el vuelto). */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cobro_id", nullable = false, length = 26)
    private String cobroId;

    @Column(nullable = false, length = 10)
    private String medio;

    @Column(nullable = false)
    private long importe;

    @Column(name = "monto_recibido")
    private Long montoRecibido;

    @Column(length = 50)
    private String referencia;

    protected Pago() {
    }

    public Pago(String id, String cobroId, String medio, long importe, Long montoRecibido, String referencia) {
        this.id = id;
        this.cobroId = cobroId;
        this.medio = medio;
        this.importe = importe;
        this.montoRecibido = montoRecibido;
        this.referencia = referencia;
    }

    public String getId() {
        return id;
    }

    public String getCobroId() {
        return cobroId;
    }

    public String getMedio() {
        return medio;
    }

    public long getImporte() {
        return importe;
    }

    public Long getMontoRecibido() {
        return montoRecibido;
    }

    public String getReferencia() {
        return referencia;
    }
}
