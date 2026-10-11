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

/** 5.2 (RN-043): el producto puede ser un insumo o una subreceta -- ahi
 * esta la recursividad producto -> receta -> ingrediente -> producto. */
@Entity
@Table(name = "ingrediente_receta")
public class IngredienteReceta {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal cantidad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "unidad_id", nullable = false)
    private UnidadMedida unidad;

    @Column(nullable = false)
    private int orden;

    protected IngredienteReceta() {
    }

    IngredienteReceta(String id, Receta receta, Producto producto, BigDecimal cantidad, UnidadMedida unidad,
                      int orden) {
        this.id = id;
        this.receta = receta;
        this.producto = producto;
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.orden = orden;
    }

    void actualizar(BigDecimal cantidad, UnidadMedida unidad, int orden) {
        this.cantidad = cantidad;
        this.unidad = unidad;
        this.orden = orden;
    }

    public String getId() {
        return id;
    }

    public Receta getReceta() {
        return receta;
    }

    public Producto getProducto() {
        return producto;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public UnidadMedida getUnidad() {
        return unidad;
    }

    public int getOrden() {
        return orden;
    }
}
