package pe.rikos.operacion.caja.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** El snapshot economico de una linea cobrada (RN-005, RN-046): lo que se cobro y a que costo. De solo agregado. */
@Entity
@Table(name = "cobro_linea")
public class CobroLinea {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cobro_id", nullable = false, length = 26)
    private String cobroId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "detalle_pedido_id", nullable = false, length = 26)
    private String detallePedidoId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cuenta_persona_id", nullable = false, length = 26)
    private String cuentaPersonaId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pensionista_id", length = 26)
    private String pensionistaId;

    @Column(nullable = false, length = 10)
    private String destino;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "producto_id", length = 26)
    private String productoId;

    @Column(name = "tipo_linea", nullable = false, length = 14)
    private String tipoLinea;

    @Column(nullable = false, length = 160)
    private String nombre;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false)
    private long precioUnitario;

    @Column(name = "taper_unitario", nullable = false)
    private long taperUnitario;

    @Column(nullable = false)
    private long descuento;

    @Column(nullable = false)
    private long importe;

    @Column(name = "costo_unitario", precision = 14, scale = 6)
    private BigDecimal costoUnitario;

    @Column(precision = 14, scale = 6)
    private BigDecimal utilidad;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column()
    private String composicion;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "motivo_descuento_id", length = 26)
    private String motivoDescuentoId;

    @Column(name = "observacion_descuento", length = 250)
    private String observacionDescuento;

    @Column(nullable = false)
    private boolean vigente = true;

    protected CobroLinea() {
    }

    public CobroLinea(String id, String cobroId, String detallePedidoId, String cuentaPersonaId, String pensionistaId, String destino, String productoId, String tipoLinea, String nombre, int cantidad, long precioUnitario, long taperUnitario, long descuento, long importe, BigDecimal costoUnitario, BigDecimal utilidad, String composicion,
                      String motivoDescuentoId, String observacionDescuento) {
        this.id = id;
        this.cobroId = cobroId;
        this.detallePedidoId = detallePedidoId;
        this.cuentaPersonaId = cuentaPersonaId;
        this.pensionistaId = pensionistaId;
        this.destino = destino;
        this.productoId = productoId;
        this.tipoLinea = tipoLinea;
        this.nombre = nombre;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.taperUnitario = taperUnitario;
        this.descuento = descuento;
        this.importe = importe;
        this.costoUnitario = costoUnitario;
        this.utilidad = utilidad;
        this.composicion = composicion;
        this.motivoDescuentoId = motivoDescuentoId;
        this.observacionDescuento = observacionDescuento;
    }

    public String getId() {
        return id;
    }

    public String getCobroId() {
        return cobroId;
    }

    public String getDetallePedidoId() {
        return detallePedidoId;
    }

    public String getCuentaPersonaId() {
        return cuentaPersonaId;
    }

    public String getPensionistaId() {
        return pensionistaId;
    }

    public String getDestino() {
        return destino;
    }

    public String getProductoId() {
        return productoId;
    }

    public String getTipoLinea() {
        return tipoLinea;
    }

    public String getNombre() {
        return nombre;
    }

    public int getCantidad() {
        return cantidad;
    }

    public long getPrecioUnitario() {
        return precioUnitario;
    }

    public long getTaperUnitario() {
        return taperUnitario;
    }

    public long getDescuento() {
        return descuento;
    }

    public long getImporte() {
        return importe;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public BigDecimal getUtilidad() {
        return utilidad;
    }

    /** El cobro se reabrio: esta fila queda como historia y la linea se puede cobrar de nuevo. */
    public void dejarDeSerVigente() {
        this.vigente = false;
    }

    public boolean isVigente() {
        return vigente;
    }

    public String getComposicion() {
        return composicion;
    }

    public String getMotivoDescuentoId() {
        return motivoDescuentoId;
    }

    public String getObservacionDescuento() {
        return observacionDescuento;
    }
}
