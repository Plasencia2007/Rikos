package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 5.1 (US-010). El rendimiento va siempre en la unidad base del producto:
 * asi una subreceta se costea por unidad base (costo_total / rendimiento),
 * igual que un insumo con su costo_ultima_compra. Los costos son derivados
 * -- los escribe CalculadoraCostoReceta, nunca el usuario.
 */
@Entity
@Table(name = "receta")
public class Receta {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false, precision = 14, scale = 6)
    private BigDecimal rendimiento;

    @Column(nullable = false)
    private int porciones;

    @Column(name = "costo_total", precision = 14, scale = 6)
    private BigDecimal costoTotal;

    @Column(name = "costo_por_porcion", precision = 14, scale = 6)
    private BigDecimal costoPorPorcion;

    @Column(nullable = false)
    private boolean activo = true;

    @Version
    @Column(nullable = false)
    private int version;

    @OneToMany(mappedBy = "receta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<IngredienteReceta> ingredientes = new ArrayList<>();

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

    protected Receta() {
    }

    public Receta(String id, Producto producto, BigDecimal rendimiento, int porciones, String creadoPor) {
        this.id = id;
        this.producto = producto;
        this.rendimiento = rendimiento;
        this.porciones = porciones;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public record DatosIngrediente(String id, Producto producto, BigDecimal cantidad, UnidadMedida unidad) {
    }

    /**
     * Reemplaza la composicion completa: la pantalla siempre manda la lista
     * entera. Los ingredientes que siguen se actualizan en su lugar en vez de
     * borrar y volver a insertar -- Hibernate ejecuta los INSERT antes que los
     * DELETE, y el reinsertado chocaria con uq_ingrediente_receta_producto.
     */
    public void componer(BigDecimal rendimiento, int porciones, List<DatosIngrediente> nuevos, String actorId) {
        this.rendimiento = rendimiento;
        this.porciones = porciones;
        java.util.Map<String, IngredienteReceta> actuales = new java.util.HashMap<>();
        for (IngredienteReceta i : ingredientes) {
            actuales.put(i.getProducto().getId(), i);
        }
        List<IngredienteReceta> resultado = new ArrayList<>();
        int orden = 0;
        for (DatosIngrediente datos : nuevos) {
            IngredienteReceta existente = actuales.remove(datos.producto().getId());
            if (existente != null) {
                existente.actualizar(datos.cantidad(), datos.unidad(), orden++);
                resultado.add(existente);
            } else {
                resultado.add(new IngredienteReceta(datos.id(), this, datos.producto(), datos.cantidad(),
                        datos.unidad(), orden++));
            }
        }
        this.ingredientes.removeIf(i -> actuales.containsKey(i.getProducto().getId()));
        for (IngredienteReceta i : resultado) {
            if (!this.ingredientes.contains(i)) {
                this.ingredientes.add(i);
            }
        }
        this.ingredientes.sort(java.util.Comparator.comparingInt(IngredienteReceta::getOrden));
        if (actorId != null) {
            this.actualizadoEn = OffsetDateTime.now();
            this.actualizadoPor = actorId;
        }
    }

    public void registrarCosto(BigDecimal costoTotal, BigDecimal costoPorPorcion) {
        this.costoTotal = costoTotal;
        this.costoPorPorcion = costoPorPorcion;
    }

    public String getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public BigDecimal getRendimiento() {
        return rendimiento;
    }

    public int getPorciones() {
        return porciones;
    }

    public BigDecimal getCostoTotal() {
        return costoTotal;
    }

    public BigDecimal getCostoPorPorcion() {
        return costoPorPorcion;
    }

    public boolean isActivo() {
        return activo;
    }

    public int getVersion() {
        return version;
    }

    public List<IngredienteReceta> getIngredientes() {
        return ingredientes;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }

    public OffsetDateTime getActualizadoEn() {
        return actualizadoEn;
    }
}
