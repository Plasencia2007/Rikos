package pe.rikos.operacion.catalogo.infraestructura;

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

/**
 * 4.1 bis: en que servicios se ofrece un producto_fijo. Ver la nota en
 * UsuarioRol (identidad) sobre por que productoId es un @Id String plano
 * y no una asociacion @ManyToOne dentro de la clave compuesta -- Hibernate
 * no propaga @JdbcTypeCode(CHAR) ahi (DT-041/2.8 bis). "servicio" no
 * necesita el mismo tratamiento: es un enum-texto, no un ULID con FK.
 */
@Entity
@Table(name = "producto_servicio")
@IdClass(ProductoServicioId.class)
public class ProductoServicio {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "producto_id", length = 26)
    private String productoId;

    @Id
    @Column(length = 10)
    private String servicio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", insertable = false, updatable = false)
    private Producto producto;

    @Column(nullable = false)
    private boolean activo = true;

    protected ProductoServicio() {
    }

    public ProductoServicio(Producto producto, String servicio) {
        this.producto = producto;
        this.productoId = producto.getId();
        this.servicio = servicio;
    }

    public Producto getProducto() {
        return producto;
    }

    public String getServicio() {
        return servicio;
    }

    public boolean isActivo() {
        return activo;
    }
}
