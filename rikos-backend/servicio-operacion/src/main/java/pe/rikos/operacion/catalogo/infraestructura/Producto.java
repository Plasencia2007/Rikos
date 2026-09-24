package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 4.3: tres banderas de naturaleza, no un tipo unico -- el arroz es insumo,
 * entra en recetas, y se vende como guarnicion, las tres a la vez.
 *
 * origen_costo (decision del sprint 2, no esta en el modelo original):
 * 'manual' mientras el costo lo cargo a mano el usuario ("estimado, se
 * reemplaza con la primera compra"); 'compra' desde que una compra real
 * (sprint 9) lo confirma, momento en el que deja de ser editable a mano.
 */
@Entity
@Table(name = "producto")
public class Producto {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(nullable = false, length = 30)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaProducto categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "unidad_base_id", nullable = false)
    private UnidadMedida unidadBase;

    @Column(name = "es_vendible", nullable = false)
    private boolean esVendible;

    @Column(name = "es_insumo", nullable = false)
    private boolean esInsumo;

    @Column(name = "es_subreceta", nullable = false)
    private boolean esSubreceta;

    @Column(name = "precio_venta")
    private Long precioVenta;

    @Column(name = "costo_ultima_compra", precision = 14, scale = 6)
    private BigDecimal costoUltimaCompra;

    @Column(name = "origen_costo", length = 10)
    private String origenCosto;

    @Column(name = "fecha_ultima_compra")
    private LocalDate fechaUltimaCompra;

    @Column(name = "controla_stock", nullable = false)
    private boolean controlaStock = true;

    @Column(nullable = false)
    private boolean activo = true;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creado_por", length = 26)
    private String creadoPor;

    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "actualizado_por", length = 26)
    private String actualizadoPor;

    protected Producto() {
    }

    public Producto(String id, String codigo, String nombre, CategoriaProducto categoria, UnidadMedida unidadBase,
                     boolean esVendible, boolean esInsumo, boolean esSubreceta, Long precioVenta,
                     boolean controlaStock, String creadoPor) {
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.categoria = categoria;
        this.unidadBase = unidadBase;
        this.esVendible = esVendible;
        this.esInsumo = esInsumo;
        this.esSubreceta = esSubreceta;
        this.precioVenta = precioVenta;
        this.controlaStock = controlaStock;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public record DatosActualizacion(String nombre, CategoriaProducto categoria, UnidadMedida unidadBase,
                                      boolean esVendible, boolean esInsumo, boolean esSubreceta, Long precioVenta,
                                      boolean controlaStock) {
    }

    public void actualizar(DatosActualizacion datos, String actualizadoPor) {
        this.nombre = datos.nombre();
        this.categoria = datos.categoria();
        this.unidadBase = datos.unidadBase();
        this.esVendible = datos.esVendible();
        this.esInsumo = datos.esInsumo();
        this.esSubreceta = datos.esSubreceta();
        this.precioVenta = datos.precioVenta();
        this.controlaStock = datos.controlaStock();
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public void desactivar(String actualizadoPor) {
        this.activo = false;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    /** Costo a mano, mientras no exista una compra real (RN-047, sprint 9). */
    public void registrarCostoManual(BigDecimal costo, String actualizadoPor) {
        this.costoUltimaCompra = costo;
        this.origenCosto = "manual";
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    /** Lo llamara el caso de uso de confirmar-compra del sprint 9. Desde
     * aqui el costo deja de ser editable a mano (lo hace cumplir el caso
     * de uso de actualizar producto, no esta entidad). */
    public void registrarCostoDeCompra(BigDecimal costo, LocalDate fecha, String actualizadoPor) {
        this.costoUltimaCompra = costo;
        this.origenCosto = "compra";
        this.fechaUltimaCompra = fecha;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public boolean costoEsEditableAMano() {
        return !"compra".equals(origenCosto);
    }

    public String getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public CategoriaProducto getCategoria() {
        return categoria;
    }

    public UnidadMedida getUnidadBase() {
        return unidadBase;
    }

    public boolean isEsVendible() {
        return esVendible;
    }

    public boolean isEsInsumo() {
        return esInsumo;
    }

    public boolean isEsSubreceta() {
        return esSubreceta;
    }

    public Long getPrecioVenta() {
        return precioVenta;
    }

    public BigDecimal getCostoUltimaCompra() {
        return costoUltimaCompra;
    }

    public String getOrigenCosto() {
        return origenCosto;
    }

    public LocalDate getFechaUltimaCompra() {
        return fechaUltimaCompra;
    }

    public boolean isControlaStock() {
        return controlaStock;
    }

    public boolean isActivo() {
        return activo;
    }

    public int getVersion() {
        return version;
    }
}
