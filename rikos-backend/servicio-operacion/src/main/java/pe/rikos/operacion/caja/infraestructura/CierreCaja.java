package pe.rikos.operacion.caja.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 9.4: el cierre de un turno: lo esperado, lo contado y la diferencia (contado - esperado, negativa si falta dinero).
 * INALTERABLE (V43): un trigger de la base rechaza cualquier UPDATE o DELETE.
 */
@Entity
@Table(name = "cierre_caja")
public class CierreCaja {

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

    @Column(name = "efectivo_esperado", nullable = false)
    private long efectivoEsperado;

    @Column(name = "efectivo_contado", nullable = false)
    private long efectivoContado;

    @Column(nullable = false)
    private long diferencia;

    @Column(name = "total_yape", nullable = false)
    private long totalYape;

    @Column(name = "total_plin", nullable = false)
    private long totalPlin;

    @Column(name = "total_tarjeta", nullable = false)
    private long totalTarjeta;

    @Column(length = 500)
    private String observacion;

    @Column(name = "cerrado_en", nullable = false)
    private OffsetDateTime cerradoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cerrado_por", nullable = false, length = 26)
    private String cerradoPor;

    protected CierreCaja() {
    }

    public CierreCaja(String id, String turnoCajaId, String localId, long efectivoEsperado, long efectivoContado,
                      long totalYape, long totalPlin, long totalTarjeta, String observacion, OffsetDateTime cerradoEn,
                      String cerradoPor) {
        this.id = id;
        this.turnoCajaId = turnoCajaId;
        this.localId = localId;
        this.efectivoEsperado = efectivoEsperado;
        this.efectivoContado = efectivoContado;
        this.diferencia = efectivoContado - efectivoEsperado;
        this.totalYape = totalYape;
        this.totalPlin = totalPlin;
        this.totalTarjeta = totalTarjeta;
        this.observacion = observacion;
        this.cerradoEn = cerradoEn;
        this.cerradoPor = cerradoPor;
    }

    public String getId() { return id; }
    public String getTurnoCajaId() { return turnoCajaId; }
    public String getLocalId() { return localId; }
    public long getEfectivoEsperado() { return efectivoEsperado; }
    public long getEfectivoContado() { return efectivoContado; }
    public long getDiferencia() { return diferencia; }
    public long getTotalYape() { return totalYape; }
    public long getTotalPlin() { return totalPlin; }
    public long getTotalTarjeta() { return totalTarjeta; }
    public String getObservacion() { return observacion; }
    public OffsetDateTime getCerradoEn() { return cerradoEn; }
    public String getCerradoPor() { return cerradoPor; }
}
