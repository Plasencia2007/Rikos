package pe.rikos.operacion.pensionistas.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 7.10 del modelo de datos: Pre-ticket emitido por el kiosco de autoservicio.
 * Codigo unico de 6 digitos, de un solo uso. No genera cargo a la cuenta.
 */
@Entity
@Table(name = "pre_ticket")
public class PreTicket {

    public static final String ESTADO_PENDIENTE = "pendiente";
    public static final String ESTADO_VINCULADO = "vinculado";
    public static final String ESTADO_CERRADO = "cerrado";
    public static final String ESTADO_VENCIDO = "vencido";
    public static final String ESTADO_ANULADO = "anulado";

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(nullable = false, unique = true, length = 12)
    private String codigo;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pensionista_id", nullable = false, length = 26)
    private String pensionistaId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @Column(nullable = false, length = 20)
    private String servicio;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(name = "emitido_en", nullable = false)
    private OffsetDateTime emitidoEn;

    @Column(name = "vence_en", nullable = false)
    private OffsetDateTime venceEn;

    @Column(name = "vinculado_en")
    private OffsetDateTime vinculadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "vinculado_por", length = 26)
    private String vinculadoPor;

    protected PreTicket() {
    }

    public PreTicket(String id, String codigo, String pensionistaId, String localId, String servicio,
                     OffsetDateTime emitidoEn, OffsetDateTime venceEn) {
        this.id = id;
        this.codigo = codigo;
        this.pensionistaId = pensionistaId;
        this.localId = localId;
        this.servicio = servicio;
        this.estado = ESTADO_PENDIENTE;
        this.emitidoEn = emitidoEn;
        this.venceEn = venceEn;
    }

    public PreTicket(String id, String codigo, String pensionistaId, String localId, String servicio,
                     OffsetDateTime venceEn) {
        this(id, codigo, pensionistaId, localId, servicio, OffsetDateTime.now(), venceEn);
    }

    public boolean estaPendiente() {
        return estaPendiente(OffsetDateTime.now());
    }

    public boolean estaPendiente(OffsetDateTime momento) {
        return ESTADO_PENDIENTE.equals(estado) && !estaVencidoPorTiempo(momento);
    }

    public boolean estaVencidoPorTiempo() {
        return estaVencidoPorTiempo(OffsetDateTime.now());
    }

    public boolean estaVencidoPorTiempo(OffsetDateTime momento) {
        return momento != null && momento.isAfter(venceEn);
    }

    public void marcarVencido() {
        this.estado = ESTADO_VENCIDO;
    }

    public void vincular(String usuarioId) {
        this.estado = ESTADO_VINCULADO;
        this.vinculadoEn = OffsetDateTime.now();
        this.vinculadoPor = usuarioId;
    }

    public void desvincular() {
        this.estado = ESTADO_PENDIENTE;
        this.vinculadoEn = null;
        this.vinculadoPor = null;
    }

    public void cerrar() {
        this.estado = ESTADO_CERRADO;
    }

    public void anular() {
        this.estado = ESTADO_ANULADO;
    }

    public String getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getPensionistaId() {
        return pensionistaId;
    }

    public String getLocalId() {
        return localId;
    }

    public String getServicio() {
        return servicio;
    }

    public String getEstado() {
        // Si esta en pendiente pero ya paso venceEn, se expone como vencido
        if (ESTADO_PENDIENTE.equals(estado) && estaVencidoPorTiempo()) {
            return ESTADO_VENCIDO;
        }
        return estado;
    }

    public OffsetDateTime getEmitidoEn() {
        return emitidoEn;
    }

    public OffsetDateTime getVenceEn() {
        return venceEn;
    }

    public OffsetDateTime getVinculadoEn() {
        return vinculadoEn;
    }

    public String getVinculadoPor() {
        return vinculadoPor;
    }
}
