package pe.rikos.operacion.caja.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 9.2: el turno de un cajero en una caja, con su fondo inicial en centimos. Una caja tiene a lo sumo un turno abierto
 * y un cajero tambien: lo garantizan dos indices unicos parciales en la base (V40), no solo el codigo.
 */
@Entity
@Table(name = "turno_caja")
public class TurnoCaja {

    public static final String ABIERTO = "abierto";
    public static final String CERRADO = "cerrado";

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "caja_id", nullable = false, length = 26)
    private String cajaId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cajero_id", nullable = false, length = 26)
    private String cajeroId;

    @Column(name = "fondo_inicial", nullable = false)
    private long fondoInicial;

    @Column(name = "abierto_en", nullable = false)
    private OffsetDateTime abiertoEn;

    @Column(name = "cerrado_en")
    private OffsetDateTime cerradoEn;

    @Column(nullable = false, length = 10)
    private String estado;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cerrado_por", length = 26)
    private String cerradoPor;

    @Version
    @Column(nullable = false)
    private int version;

    protected TurnoCaja() {
    }

    public TurnoCaja(String id, String cajaId, String localId, String cajeroId, long fondoInicial) {
        this.id = id;
        this.cajaId = cajaId;
        this.localId = localId;
        this.cajeroId = cajeroId;
        this.fondoInicial = fondoInicial;
        this.abiertoEn = OffsetDateTime.now();
        this.estado = ABIERTO;
    }

    /** El cajero cierra su turno: queda "cerrado" con quien y cuando (el cierre en si vive en cierre_caja). */
    public void cerrar(String cajeroId, OffsetDateTime cuando) {
        this.estado = CERRADO;
        this.cerradoEn = cuando;
        this.cerradoPor = cajeroId;
    }

    public String getCerradoPor() {
        return cerradoPor;
    }

    public String getId() {
        return id;
    }

    public String getCajaId() {
        return cajaId;
    }

    public String getLocalId() {
        return localId;
    }

    public String getCajeroId() {
        return cajeroId;
    }

    public long getFondoInicial() {
        return fondoInicial;
    }

    public OffsetDateTime getAbiertoEn() {
        return abiertoEn;
    }

    public OffsetDateTime getCerradoEn() {
        return cerradoEn;
    }

    public String getEstado() {
        return estado;
    }
}
