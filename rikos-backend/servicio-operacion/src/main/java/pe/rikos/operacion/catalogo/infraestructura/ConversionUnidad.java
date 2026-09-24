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

/** 4.5: por producto, nunca global -- un litro de aceite y un litro de
 * chicha no pesan lo mismo. */
@Entity
@Table(name = "conversion_unidad")
public class ConversionUnidad {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "unidad_origen_id", nullable = false)
    private UnidadMedida unidadOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "unidad_destino_id", nullable = false)
    private UnidadMedida unidadDestino;

    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal factor;

    protected ConversionUnidad() {
    }

    public ConversionUnidad(String id, Producto producto, UnidadMedida unidadOrigen, UnidadMedida unidadDestino,
                             BigDecimal factor) {
        this.id = id;
        this.producto = producto;
        this.unidadOrigen = unidadOrigen;
        this.unidadDestino = unidadDestino;
        this.factor = factor;
    }

    public String getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public UnidadMedida getUnidadOrigen() {
        return unidadOrigen;
    }

    public UnidadMedida getUnidadDestino() {
        return unidadDestino;
    }

    public BigDecimal getFactor() {
        return factor;
    }
}
