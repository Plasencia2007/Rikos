package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

/** 4.4: el saco de 50 kg, la caja de 24 unidades. Una sola predeterminada
 * activa por producto -- lo garantiza el indice unico parcial de la BD
 * (V14), esta clase solo expone el estado. */
@Entity
@Table(name = "presentacion_compra")
public class PresentacionCompra {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, length = 80)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "unidad_id", nullable = false)
    private UnidadMedida unidad;

    @Column(name = "factor_a_unidad_base", nullable = false, precision = 14, scale = 6)
    private BigDecimal factorAUnidadBase;

    @Column(name = "es_predeterminada", nullable = false)
    private boolean esPredeterminada;

    @Column(nullable = false)
    private boolean activo = true;

    protected PresentacionCompra() {
    }

    public PresentacionCompra(String id, Producto producto, String nombre, UnidadMedida unidad,
                               BigDecimal factorAUnidadBase, boolean esPredeterminada) {
        this.id = id;
        this.producto = producto;
        this.nombre = nombre;
        this.unidad = unidad;
        this.factorAUnidadBase = factorAUnidadBase;
        this.esPredeterminada = esPredeterminada;
    }

    public void desactivar() {
        this.activo = false;
        this.esPredeterminada = false;
    }

    public String getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getNombre() {
        return nombre;
    }

    public UnidadMedida getUnidad() {
        return unidad;
    }

    public BigDecimal getFactorAUnidadBase() {
        return factorAUnidadBase;
    }

    public boolean isEsPredeterminada() {
        return esPredeterminada;
    }

    public boolean isActivo() {
        return activo;
    }
}
