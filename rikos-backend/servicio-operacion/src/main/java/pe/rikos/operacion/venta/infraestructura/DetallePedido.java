package pe.rikos.operacion.venta.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 8.4: una linea del pedido. El precio (y el taper por unidad) se guardan al agregarla, con su origen; el costo y
 * la receta se congelan al cerrar (sprint 7). producto_id es nulo en la comida del plan y en el extra libre.
 */
@Entity
@Table(name = "detalle_pedido")
public class DetallePedido {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cuenta_persona_id", nullable = false, length = 26)
    private String cuentaPersonaId;

    @Column(name = "tipo_linea", nullable = false, length = 14)
    private String tipoLinea;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "producto_id", length = 26)
    private String productoId;

    @Column(length = 120)
    private String descripcion;

    @Column(nullable = false)
    private int cantidad;

    @Column(length = 250)
    private String observacion;

    @Column(nullable = false, length = 10)
    private String estado;

    @Column(name = "precio_unitario", nullable = false)
    private long precioUnitario;

    @Column(name = "taper_unitario", nullable = false)
    private long taperUnitario;

    @Column(name = "tarifa_origen", nullable = false, length = 10)
    private String tarifaOrigen;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "plan_tarifario_id", length = 26)
    private String planTarifarioId;

    @Column(name = "segundo_del_dia", nullable = false)
    private boolean segundoDelDia;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creado_por", length = 26)
    private String creadoPor;

    /** Dos escrituras sobre la misma linea no se pisan: la segunda en confirmar choca (409 en el controlador). */
    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "anulado_en")
    private OffsetDateTime anuladoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "anulado_por", length = 26)
    private String anuladoPor;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "motivo_anulacion_id", length = 26)
    private String motivoAnulacionId;

    @Column(name = "observacion_anulacion", length = 250)
    private String observacionAnulacion;

    @Column(name = "anulado_post_comanda", nullable = false)
    private boolean anuladoPostComanda;

    protected DetallePedido() {
    }

    public DetallePedido(String id, String cuentaPersonaId, String tipoLinea, String productoId, String descripcion,
                         int cantidad, String observacion, long precioUnitario, long taperUnitario,
                         String tarifaOrigen, String planTarifarioId, boolean segundoDelDia, String creadoPor) {
        this.id = id;
        this.cuentaPersonaId = cuentaPersonaId;
        this.tipoLinea = tipoLinea;
        this.productoId = productoId;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.observacion = observacion;
        this.estado = "pendiente";
        this.precioUnitario = precioUnitario;
        this.taperUnitario = taperUnitario;
        this.tarifaOrigen = tarifaOrigen;
        this.planTarifarioId = planTarifarioId;
        this.segundoDelDia = segundoDelDia;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    /** Solo mientras esta pendiente (el caso de uso lo verifica). */
    public void cambiar(int cantidad, String observacion) {
        this.cantidad = cantidad;
        this.observacion = observacion;
    }

    /**
     * US-029: la linea pasa a otra persona. El precio, el origen y el plan se reemplazan solo cuando el motor los
     * recalculo para la persona de destino; las demas lineas conservan su snapshot.
     */
    public void pasarA(String personaId, long precio, long taperUnitario, String tarifaOrigen, String planId,
                       boolean segundoDelDia) {
        this.cuentaPersonaId = personaId;
        this.precioUnitario = precio;
        this.taperUnitario = taperUnitario;
        this.tarifaOrigen = tarifaOrigen;
        this.planTarifarioId = planId;
        this.segundoDelDia = segundoDelDia;
    }

    /** US-029: la linea pasa a otra persona conservando su precio. */
    public void pasarA(String personaId) {
        this.cuentaPersonaId = personaId;
    }

    /** Sale a cocina: de aqui en adelante ya no se modifica ni se quita, solo se anula. */
    public void comandar() {
        this.estado = "comandado";
    }

    /** Sprint 7: la caja la cobro. De aqui en adelante no se modifica, ni se quita, ni se anula, ni se traslada. */
    public void cobrar() {
        this.estado = "cobrado";
    }

    /** Se anula con permiso y motivo. Si ya estaba comandada, la receta ya se gasto (RN-065). */
    public void anular(String motivoId, String observacion, String actorId) {
        this.anuladoPostComanda = "comandado".equals(this.estado);
        this.estado = "anulado";
        this.anuladoEn = OffsetDateTime.now();
        this.anuladoPor = actorId;
        this.motivoAnulacionId = motivoId;
        this.observacionAnulacion = observacion;
    }

    public int getVersion() {
        return version;
    }

    public OffsetDateTime getAnuladoEn() {
        return anuladoEn;
    }

    public String getAnuladoPor() {
        return anuladoPor;
    }

    public String getMotivoAnulacionId() {
        return motivoAnulacionId;
    }

    public String getObservacionAnulacion() {
        return observacionAnulacion;
    }

    public boolean isAnuladoPostComanda() {
        return anuladoPostComanda;
    }

    public String getId() {
        return id;
    }

    public String getCuentaPersonaId() {
        return cuentaPersonaId;
    }

    public String getTipoLinea() {
        return tipoLinea;
    }

    public String getProductoId() {
        return productoId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public int getCantidad() {
        return cantidad;
    }

    public String getObservacion() {
        return observacion;
    }

    public String getEstado() {
        return estado;
    }

    public long getPrecioUnitario() {
        return precioUnitario;
    }

    public long getTaperUnitario() {
        return taperUnitario;
    }

    public String getTarifaOrigen() {
        return tarifaOrigen;
    }

    public String getPlanTarifarioId() {
        return planTarifarioId;
    }

    public boolean isSegundoDelDia() {
        return segundoDelDia;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public String getCreadoPor() {
        return creadoPor;
    }
}
