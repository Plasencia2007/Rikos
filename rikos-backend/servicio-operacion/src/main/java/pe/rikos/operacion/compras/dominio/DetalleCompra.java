package pe.rikos.operacion.compras.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.rikos.operacion.catalogo.infraestructura.PresentacionCompra;
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.inventario.infraestructura.MovimientoInventario;

import java.math.BigDecimal;

@Entity
@Table(name = "detalle_compra")
public class DetalleCompra {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "compra_id", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "presentacion_id")
    private PresentacionCompra presentacion;

    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal cantidad;

    @Column(name = "cantidad_base", nullable = false, precision = 14, scale = 6)
    private BigDecimal cantidadBase;

    @Column(name = "precio_unitario", nullable = false)
    private Long precioUnitario;

    @Column(nullable = false)
    private Long subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "movimiento_id")
    private MovimientoInventario movimiento;

    protected DetalleCompra() {
    }

    public DetalleCompra(String id, Producto producto, PresentacionCompra presentacion,
                         BigDecimal cantidad, BigDecimal cantidadBase,
                         Long precioUnitario, Long subtotal) {
        this.id = id;
        this.producto = producto;
        this.presentacion = presentacion;
        this.cantidad = cantidad;
        this.cantidadBase = cantidadBase;
        this.precioUnitario = precioUnitario;
        this.subtotal = subtotal;
    }

    public void asignarCompra(Compra compra) {
        this.compra = compra;
    }

    public void asignarMovimiento(MovimientoInventario movimiento) {
        this.movimiento = movimiento;
    }

    public String getId() { return id; }
    public Compra getCompra() { return compra; }
    public Producto getProducto() { return producto; }
    public PresentacionCompra getPresentacion() { return presentacion; }
    public BigDecimal getCantidad() { return cantidad; }
    public BigDecimal getCantidadBase() { return cantidadBase; }
    public Long getPrecioUnitario() { return precioUnitario; }
    public Long getSubtotal() { return subtotal; }
    public MovimientoInventario getMovimiento() { return movimiento; }
}
