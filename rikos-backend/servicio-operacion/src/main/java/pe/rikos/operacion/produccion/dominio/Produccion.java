package pe.rikos.operacion.produccion.dominio;

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
import pe.rikos.operacion.catalogo.infraestructura.Receta;
import pe.rikos.operacion.identidad.infraestructura.Local;
import pe.rikos.operacion.identidad.infraestructura.Usuario;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "produccion")
public class Produccion {

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "receta_id", nullable = false)
    private Receta receta;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(length = 20)
    private String servicio; // desayuno, almuerzo, cena

    @Column(name = "porciones_planificadas")
    private Integer porcionesPlanificadas;

    @Column(name = "porciones_preparadas", nullable = false)
    private int porcionesPreparadas;

    @Column(name = "porciones_vendidas", nullable = false)
    private int porcionesVendidas = 0;

    @Column(name = "rendimiento_obtenido", nullable = false, precision = 14, scale = 6)
    private BigDecimal rendimientoObtenido;

    @Column(precision = 14, scale = 6)
    private BigDecimal sobrante;

    @Column(name = "destino_sobrante", length = 20)
    private String destinoSobrante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "motivo_id")
    private Motivo motivo;

    @Column(nullable = false, length = 20)
    private String estado = "confirmada";

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "registrado_por", nullable = false)
    private Usuario registradoPor;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    protected Produccion() {}

    public Produccion(String id, Local local, Producto producto, Receta receta, LocalDate fecha,
                      String servicio, Integer porcionesPlanificadas, int porcionesPreparadas,
                      BigDecimal rendimientoObtenido, Usuario registradoPor) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("El id es obligatorio");
        if (local == null) throw new IllegalArgumentException("El local es obligatorio");
        if (producto == null) throw new IllegalArgumentException("El producto es obligatorio");
        if (receta == null) throw new IllegalArgumentException("La receta es obligatoria");
        if (fecha == null) throw new IllegalArgumentException("La fecha es obligatoria");
        if (porcionesPreparadas <= 0) throw new IllegalArgumentException("Las porciones preparadas deben ser mayores a 0");
        if (rendimientoObtenido == null || rendimientoObtenido.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El rendimiento obtenido debe ser mayor a 0");
        }
        if (registradoPor == null) throw new IllegalArgumentException("El usuario que registra es obligatorio");

        this.id = id;
        this.local = local;
        this.producto = producto;
        this.receta = receta;
        this.fecha = fecha;
        this.servicio = servicio;
        this.porcionesPlanificadas = porcionesPlanificadas;
        this.porcionesPreparadas = porcionesPreparadas;
        this.rendimientoObtenido = rendimientoObtenido;
        this.registradoPor = registradoPor;
        this.estado = "confirmada";
        this.creadoEn = OffsetDateTime.now();
    }

    public void registrarVentaPorciones(int cantidad) {
        if (cantidad > 0) {
            this.porcionesVendidas += cantidad;
        }
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

    public Receta getReceta() {
        return receta;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getServicio() {
        return servicio;
    }

    public Integer getPorcionesPlanificadas() {
        return porcionesPlanificadas;
    }

    public int getPorcionesPreparadas() {
        return porcionesPreparadas;
    }

    public int getPorcionesVendidas() {
        return porcionesVendidas;
    }

    public BigDecimal getRendimientoObtenido() {
        return rendimientoObtenido;
    }

    public BigDecimal getSobrante() {
        return sobrante;
    }

    public String getDestinoSobrante() {
        return destinoSobrante;
    }

    public Motivo getMotivo() {
        return motivo;
    }

    public String getEstado() {
        return estado;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public OffsetDateTime getCreadoEn() {
        return creadoEn;
    }
}
