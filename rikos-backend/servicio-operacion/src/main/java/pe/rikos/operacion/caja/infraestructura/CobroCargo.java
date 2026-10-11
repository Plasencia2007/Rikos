package pe.rikos.operacion.caja.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Lo cargado a la cuenta de un pensionista en un cobro (RN-015); enlaza el cobro con el movimiento del libro. */
@Entity
@Table(name = "cobro_cargo")
public class CobroCargo {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cobro_id", nullable = false, length = 26)
    private String cobroId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pensionista_id", nullable = false, length = 26)
    private String pensionistaId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "movimiento_cuenta_id", nullable = false, length = 26)
    private String movimientoCuentaId;

    @Column(nullable = false)
    private long importe;

    @Column(name = "supera_tope", nullable = false)
    private boolean superaTope;

    protected CobroCargo() {
    }

    public CobroCargo(String id, String cobroId, String pensionistaId, String movimientoCuentaId, long importe, boolean superaTope) {
        this.id = id;
        this.cobroId = cobroId;
        this.pensionistaId = pensionistaId;
        this.movimientoCuentaId = movimientoCuentaId;
        this.importe = importe;
        this.superaTope = superaTope;
    }

    public String getId() {
        return id;
    }

    public String getCobroId() {
        return cobroId;
    }

    public String getPensionistaId() {
        return pensionistaId;
    }

    public String getMovimientoCuentaId() {
        return movimientoCuentaId;
    }

    public long getImporte() {
        return importe;
    }

    public boolean isSuperaTope() {
        return superaTope;
    }
}
