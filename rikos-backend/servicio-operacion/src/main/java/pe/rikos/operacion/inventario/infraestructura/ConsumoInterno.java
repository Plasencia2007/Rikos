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
import pe.rikos.operacion.catalogo.infraestructura.Producto;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "consumo_interno")
public class ConsumoInterno {

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
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false, length = 20)
    private String servicio;

    @Column(length = 20)
    private String turno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal cantidad;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal costo;

    @Column(name = "requirio_autorizacion", nullable = false)
    private boolean requirioAutorizacion = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "autorizado_por")
    private Usuario autorizadoPor;

    @Column(length = 250)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "movimiento_id")
    private MovimientoInventario movimiento;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    protected ConsumoInterno() {}

    public ConsumoInterno(String id, Local local, Usuario usuario, LocalDate fecha, String servicio,
                          String turno, Producto producto, BigDecimal cantidad, BigDecimal costo,
                          boolean requirioAutorizacion, Usuario autorizadoPor, String observacion,
                          MovimientoInventario movimiento) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("El id es obligatorio");
        if (local == null) throw new IllegalArgumentException("El local es obligatorio");
        if (usuario == null) throw new IllegalArgumentException("El usuario (trabajador) es obligatorio");
        if (fecha == null) throw new IllegalArgumentException("La fecha es obligatoria");
        if (servicio == null || servicio.isBlank()) throw new IllegalArgumentException("El servicio es obligatorio");
        if (producto == null) throw new IllegalArgumentException("El producto es obligatorio");
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
        if (costo == null || costo.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El costo no puede ser negativo");
        }

        this.id = id;
        this.local = local;
        this.usuario = usuario;
        this.fecha = fecha;
        this.servicio = servicio.trim().toLowerCase();
        this.turno = turno;
        this.producto = producto;
        this.cantidad = cantidad;
        this.costo = costo;
        this.requirioAutorizacion = requirioAutorizacion;
        this.autorizadoPor = autorizadoPor;
        this.observacion = observacion;
        this.movimiento = movimiento;
        this.creadoEn = OffsetDateTime.now();
    }

    public String getId() {
        return id;
    }

    public Local getLocal() {
        return local;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getServicio() {
        return servicio;
    }

    public String getTurno() {
        return turno;
    }

    public Producto getProducto() {
        return producto;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getCosto() {
        return costo;
    }

    public boolean isRequirioAutorizacion() {
        return requirioAutorizacion;
    }

    public Usuario getAutorizadoPor() {
        return autorizadoPor;
    }

    public String getObservacion() {
        return observacion;
    }

    public MovimientoInventario getMovimiento() {
        return movimiento;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
