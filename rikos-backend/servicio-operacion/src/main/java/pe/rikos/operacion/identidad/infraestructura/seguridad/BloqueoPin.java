package pe.rikos.operacion.identidad.infraestructura.seguridad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * DT-039 / 3.7 ter: contador de intentos fallidos del ingreso por PIN,
 * persistido y por (local, dispositivo) -- ver LimitadorIntentosPin.
 */
@Entity
@Table(name = "bloqueo_pin")
public class BloqueoPin {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @Column(nullable = false, length = 120)
    private String dispositivo;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private OffsetDateTime bloqueadoHasta;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected BloqueoPin() {
    }

    public BloqueoPin(String id, String localId, String dispositivo) {
        this.id = id;
        this.localId = localId;
        this.dispositivo = dispositivo;
        this.actualizadoEn = OffsetDateTime.now();
    }

    public void registrarFallo(int umbralIntentos, Duration duracionBloqueo) {
        this.intentosFallidos++;
        if (this.intentosFallidos >= umbralIntentos) {
            this.bloqueadoHasta = OffsetDateTime.now().plus(duracionBloqueo);
        }
        this.actualizadoEn = OffsetDateTime.now();
    }

    public void registrarExito() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
        this.actualizadoEn = OffsetDateTime.now();
    }

    public boolean estaBloqueado() {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(OffsetDateTime.now());
    }

    public long segundosRestantes() {
        if (!estaBloqueado()) {
            return 0;
        }
        return Duration.between(OffsetDateTime.now(), bloqueadoHasta).getSeconds();
    }

    public String getId() {
        return id;
    }

    public String getLocalId() {
        return localId;
    }

    public String getDispositivo() {
        return dispositivo;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public OffsetDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
