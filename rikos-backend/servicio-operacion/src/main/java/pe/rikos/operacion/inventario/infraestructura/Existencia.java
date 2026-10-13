package pe.rikos.operacion.inventario.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.identidad.infraestructura.Local;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * 11.1 Existencia por local y producto (RN-002, US-043).
 * stock_actual es una proyección derivada del libro de movimientos.
 * RN-050 / RN-056: Se permite stock negativo con alerta, sin bloquear movimientos.
 * RN-051: stock_minimo configurable por producto y local en unidad base.
 */
@Entity
@Table(name = "existencia")
@IdClass(ExistenciaId.class)
public class Existencia {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", length = 26)
    private String localId;

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "producto_id", length = 26)
    private String productoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "local_id", insertable = false, updatable = false)
    private Local local;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", insertable = false, updatable = false)
    private Producto producto;

    @Column(name = "stock_actual", nullable = false, precision = 14, scale = 6)
    private BigDecimal stockActual = BigDecimal.ZERO;

    @Column(name = "stock_minimo", precision = 14, scale = 6)
    private BigDecimal stockMinimo;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {jakarta.persistence.CascadeType.PERSIST, jakarta.persistence.CascadeType.MERGE})
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "ultimo_movimiento_id")
    private MovimientoInventario ultimoMovimiento;

    @Column(name = "recalculado_en", nullable = false)
    private OffsetDateTime recalculadoEn;

    protected Existencia() {
    }

    public Existencia(String localId, String productoId, BigDecimal stockMinimo) {
        this.localId = localId;
        this.productoId = productoId;
        this.stockActual = BigDecimal.ZERO;
        this.stockMinimo = stockMinimo;
        this.recalculadoEn = OffsetDateTime.now();
    }

    public Existencia(String localId, String productoId, BigDecimal stockActual, BigDecimal stockMinimo,
                      MovimientoInventario ultimoMovimiento) {
        this.localId = localId;
        this.productoId = productoId;
        this.stockActual = stockActual != null ? stockActual : BigDecimal.ZERO;
        this.stockMinimo = stockMinimo;
        this.ultimoMovimiento = ultimoMovimiento;
        this.recalculadoEn = OffsetDateTime.now();
    }

    public void actualizarStockMinimo(BigDecimal nuevoStockMinimo) {
        if (nuevoStockMinimo != null && nuevoStockMinimo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
        }
        this.stockMinimo = nuevoStockMinimo;
        this.recalculadoEn = OffsetDateTime.now();
    }

    public void actualizarStock(BigDecimal nuevoStock, MovimientoInventario movimiento) {
        this.stockActual = nuevoStock != null ? nuevoStock : BigDecimal.ZERO;
        this.ultimoMovimiento = movimiento;
        this.recalculadoEn = OffsetDateTime.now();
    }

    /**
     * Calcula el estado de alerta del inventario según reglas CA3 y CA4:
     * - "negativo": si stockActual < 0 (RN-050 / RN-056)
     * - "bajo_minimo": si stockMinimo != null y stockActual <= stockMinimo (RN-051)
     * - "sin_stock": si stockMinimo == null y stockActual == 0
     * - "normal": si stockActual > 0 (y superior al stockMinimo si está definido)
     */
    public String getEstadoAlerta() {
        if (stockActual.compareTo(BigDecimal.ZERO) < 0) {
            return "negativo";
        }
        if (stockMinimo != null && stockActual.compareTo(stockMinimo) <= 0) {
            return "bajo_minimo";
        }
        if (stockMinimo == null && stockActual.compareTo(BigDecimal.ZERO) == 0) {
            return "sin_stock";
        }
        return "normal";
    }

    public String getLocalId() {
        return localId;
    }

    public String getProductoId() {
        return productoId;
    }

    public Local getLocal() {
        return local;
    }

    public Producto getProducto() {
        return producto;
    }

    public BigDecimal getStockActual() {
        return stockActual;
    }

    public BigDecimal getStockMinimo() {
        return stockMinimo;
    }

    public MovimientoInventario getUltimoMovimiento() {
        return ultimoMovimiento;
    }

    public OffsetDateTime getRecalculadoEn() {
        return recalculadoEn;
    }
}
