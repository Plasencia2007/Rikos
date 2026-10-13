package pe.rikos.operacion.inventario.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.rikos.operacion.catalogo.infraestructura.Motivo;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.catalogo.infraestructura.UnidadMedida;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.Usuario;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 11.2 Kardex de inventario (movimiento_inventario).
 * Solo se agregan filas (append-only).
 * RN-057: El Kardex es esta tabla; el stock es su suma histórica.
 */
@Entity
@Table(name = "movimiento_inventario")
public class MovimientoInventario {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "local_id", nullable = false)
    private Local local;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, length = 30)
    private String origen;

    @Column(nullable = false, length = 10)
    private String signo;

    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal cantidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "unidad_id", nullable = false)
    private UnidadMedida unidad;

    @Column(name = "cantidad_base", nullable = false, precision = 14, scale = 6)
    private BigDecimal cantidadBase;

    @Column(name = "costo_unitario", precision = 14, scale = 6)
    private BigDecimal costoUnitario;

    @Column(name = "stock_anterior", nullable = false, precision = 14, scale = 6)
    private BigDecimal stockAnterior;

    @Column(name = "stock_nuevo", nullable = false, precision = 14, scale = 6)
    private BigDecimal stockNuevo;

    @Column(name = "referencia_tipo", length = 40)
    private String referenciaTipo;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "referencia_id", length = 26)
    private String referenciaId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "motivo_id")
    private Motivo motivo;

    @Column(length = 250)
    private String observacion;

    @Column(name = "ocurrido_en", nullable = false)
    private OffsetDateTime ocurridoEn;

    @Column(name = "registrado_en", nullable = false)
    private OffsetDateTime registradoEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "registrado_por", nullable = false)
    private Usuario registradoPor;

    protected MovimientoInventario() {
    }

    public MovimientoInventario(String id, Local local, Producto producto, String origen, String signo,
                                BigDecimal cantidad, UnidadMedida unidad, BigDecimal cantidadBase,
                                BigDecimal costoUnitario, BigDecimal stockAnterior, BigDecimal stockNuevo,
                                String referenciaTipo, String referenciaId, Motivo motivo, String observacion,
                                OffsetDateTime ocurridoEn, Usuario registradoPor) {
        this.id = id;
        this.local = local;
        this.producto = producto;
        this.origen = origen;
        this.signo = signo;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.cantidadBase = cantidadBase;
        this.costoUnitario = costoUnitario;
        this.stockAnterior = stockAnterior;
        this.stockNuevo = stockNuevo;
        this.referenciaTipo = referenciaTipo;
        this.referenciaId = referenciaId;
        this.motivo = motivo;
        this.observacion = observacion;
        this.ocurridoEn = ocurridoEn;
        this.registradoEn = OffsetDateTime.now();
        this.registradoPor = registradoPor;
    }

    public String getId() {
        return id;
    }

    public Local getLocal() {
        return local;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getOrigen() {
        return origen;
    }

    public String getSigno() {
        return signo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public UnidadMedida getUnidad() {
        return unidad;
    }

    public BigDecimal getCantidadBase() {
        return cantidadBase;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public BigDecimal getStockAnterior() {
        return stockAnterior;
    }

    public BigDecimal getStockNuevo() {
        return stockNuevo;
    }

    public String getReferenciaTipo() {
        return referenciaTipo;
    }

    public String getReferenciaId() {
        return referenciaId;
    }

    public Motivo getMotivo() {
        return motivo;
    }

    public String getObservacion() {
        return observacion;
    }

    public OffsetDateTime getOcurridoEn() {
        return ocurridoEn;
    }

    public OffsetDateTime getRegistradoEn() {
        return registradoEn;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }
}
