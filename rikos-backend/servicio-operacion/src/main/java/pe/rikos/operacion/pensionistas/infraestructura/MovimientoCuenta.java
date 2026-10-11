package pe.rikos.operacion.pensionistas.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 7.6: una fila del libro de la cuenta. SOLO SE AGREGA: sin setters, marcada
 * @Immutable, y un disparador de la base (V28) rechaza UPDATE y DELETE. El
 * saldo no se guarda en ningun lado: es la suma de los creditos menos la de
 * los cargos.
 *
 * ocurridoEn es cuando ocurrio el hecho (el estado de cuenta se arma por esta);
 * registradoEn, cuando llego a la base (la sincronizacion razona con esta): en
 * un corte de internet pueden diferir en horas.
 */
@Entity
@Immutable
@Table(name = "movimiento_cuenta")
public class MovimientoCuenta {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cuenta_id", nullable = false, length = 26)
    private String cuentaId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @Column(nullable = false, length = 14)
    private String tipo;

    @Column(nullable = false, length = 7)
    private String signo;

    @Column(nullable = false)
    private long importe;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pedido_id", length = 26)
    private String pedidoId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "motivo_id", length = 26)
    private String motivoId;

    @Column(length = 250)
    private String descripcion;

    @Column(name = "ocurrido_en", nullable = false)
    private OffsetDateTime ocurridoEn;

    @Column(name = "registrado_en", nullable = false)
    private OffsetDateTime registradoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "registrado_por", nullable = false, length = 26)
    private String registradoPor;

    protected MovimientoCuenta() {
    }

    public MovimientoCuenta(String id, String cuentaId, String localId, String tipo, String signo, long importe,
                            String pedidoId, String motivoId, String descripcion, OffsetDateTime ocurridoEn,
                            String registradoPor) {
        this.id = id;
        this.cuentaId = cuentaId;
        this.localId = localId;
        this.tipo = tipo;
        this.signo = signo;
        this.importe = importe;
        this.pedidoId = pedidoId;
        this.motivoId = motivoId;
        this.descripcion = descripcion;
        this.ocurridoEn = ocurridoEn;
        this.registradoEn = OffsetDateTime.now();
        this.registradoPor = registradoPor;
    }

    public String getId() {
        return id;
    }

    public String getCuentaId() {
        return cuentaId;
    }

    public String getLocalId() {
        return localId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getSigno() {
        return signo;
    }

    public long getImporte() {
        return importe;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public String getMotivoId() {
        return motivoId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public OffsetDateTime getOcurridoEn() {
        return ocurridoEn;
    }

    public OffsetDateTime getRegistradoEn() {
        return registradoEn;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }
}
