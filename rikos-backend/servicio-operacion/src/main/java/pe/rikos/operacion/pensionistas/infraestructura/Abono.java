package pe.rikos.operacion.pensionistas.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 7.11: Un abono realizado a la cuenta corriente del pensionista.
 * - Registrado en efectivo en caja: aprobado de inmediato, genera movimiento de caja y de cuenta.
 * - Registrado por medios digitales (Yape, Plin, transferencia): queda pendiente hasta aprobación (US-024).
 */
@Entity
@Table(name = "abono")
public class Abono {

    public static final String ESTADO_PENDIENTE = "pendiente";
    public static final String ESTADO_APROBADO = "aprobado";
    public static final String ESTADO_RECHAZADO = "rechazado";

    public static final String ORIGEN_CAJA = "caja";
    public static final String ORIGEN_PORTAL = "portal";

    public static final String MEDIO_EFECTIVO = "efectivo";
    public static final String MEDIO_YAPE = "yape";
    public static final String MEDIO_PLIN = "plin";
    public static final String MEDIO_TRANSFERENCIA = "transferencia";

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pensionista_id", nullable = false, length = 26)
    private String pensionistaId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @Column(nullable = false, length = 20)
    private String medio;

    @Column(nullable = false)
    private long monto;

    @Column(name = "fecha_operacion", nullable = false)
    private LocalDate fechaOperacion;

    @Column(name = "numero_operacion", length = 50)
    private String numeroOperacion;

    @Column(name = "cuenta_destino", length = 80)
    private String cuentaDestino;

    @Column(name = "ruta_adjunto", length = 500)
    private String rutaAdjunto;

    @Column(nullable = false, length = 20)
    private String origen;

    @Column(nullable = false, length = 20)
    private String estado;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "registrado_por", length = 26)
    private String registradoPor;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "aprobado_por", length = 26)
    private String aprobadoPor;

    @Column(name = "aprobado_en")
    private OffsetDateTime aprobadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "motivo_rechazo_id", length = 26)
    private String motivoRechazoId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "turno_caja_id", length = 26)
    private String turnoCajaId;

    @Column(length = 500)
    private String observacion;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    protected Abono() {
    }

    public Abono(String id, String pensionistaId, String localId, String medio, long monto,
                 LocalDate fechaOperacion, String numeroOperacion, String cuentaDestino, String rutaAdjunto,
                 String origen, String estado, String registradoPor, String aprobadoPor,
                 OffsetDateTime aprobadoEn, String motivoRechazoId, String turnoCajaId,
                 String observacion, OffsetDateTime creadoEn) {
        this.id = id;
        this.pensionistaId = pensionistaId;
        this.localId = localId;
        this.medio = medio;
        this.monto = monto;
        this.fechaOperacion = fechaOperacion;
        this.numeroOperacion = numeroOperacion;
        this.cuentaDestino = cuentaDestino;
        this.rutaAdjunto = rutaAdjunto;
        this.origen = origen;
        this.estado = estado;
        this.registradoPor = registradoPor;
        this.aprobadoPor = aprobadoPor;
        this.aprobadoEn = aprobadoEn;
        this.motivoRechazoId = motivoRechazoId;
        this.turnoCajaId = turnoCajaId;
        this.observacion = observacion;
        this.creadoEn = creadoEn != null ? creadoEn : OffsetDateTime.now();
    }

    public static Abono digitalPendiente(String id, String pensionistaId, String localId, String medio,
                                         long monto, LocalDate fechaOperacion, String numeroOperacion,
                                         String cuentaDestino, String rutaAdjunto, String origen,
                                         String registradoPor, String observacion, OffsetDateTime creadoEn) {
        return new Abono(id, pensionistaId, localId, medio, monto, fechaOperacion, numeroOperacion,
                cuentaDestino, rutaAdjunto, origen, ESTADO_PENDIENTE, registradoPor, null, null, null, null,
                observacion, creadoEn);
    }

    public boolean esAprobado() {
        return ESTADO_APROBADO.equals(estado);
    }

    public boolean esPendiente() {
        return ESTADO_PENDIENTE.equals(estado);
    }

    public void aprobar(String actorId, OffsetDateTime ahora) {
        this.estado = ESTADO_APROBADO;
        this.aprobadoPor = actorId;
        this.aprobadoEn = ahora;
    }

    public void rechazar(String actorId, String motivoId, OffsetDateTime ahora) {
        rechazar(actorId, motivoId, null, ahora);
    }

    public void rechazar(String actorId, String motivoId, String observacion, OffsetDateTime ahora) {
        this.estado = ESTADO_RECHAZADO;
        this.aprobadoPor = actorId;
        this.motivoRechazoId = motivoId;
        if (observacion != null && !observacion.isBlank()) {
            this.observacion = observacion.strip();
        }
        this.aprobadoEn = ahora;
    }

    public String getId() {
        return id;
    }

    public String getPensionistaId() {
        return pensionistaId;
    }

    public String getLocalId() {
        return localId;
    }

    public String getMedio() {
        return medio;
    }

    public long getMonto() {
        return monto;
    }

    public LocalDate getFechaOperacion() {
        return fechaOperacion;
    }

    public String getNumeroOperacion() {
        return numeroOperacion;
    }

    public String getCuentaDestino() {
        return cuentaDestino;
    }

    public String getRutaAdjunto() {
        return rutaAdjunto;
    }

    public String getOrigen() {
        return origen;
    }

    public String getEstado() {
        return estado;
    }

    public String getRegistradoPor() {
        return registradoPor;
    }

    public String getAprobadoPor() {
        return aprobadoPor;
    }

    public OffsetDateTime getAprobadoEn() {
        return aprobadoEn;
    }

    public String getMotivoRechazoId() {
        return motivoRechazoId;
    }

    public String getTurnoCajaId() {
        return turnoCajaId;
    }

    public String getObservacion() {
        return observacion;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
