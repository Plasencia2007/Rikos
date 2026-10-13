package pe.rikos.operacion.compras.dominio;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pe.rikos.operacion.catalogo.infraestructura.Proveedor;
import pe.rikos.operacion.identidad.infraestructura.Local;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compra")
public class Compra {

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
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Proveedor proveedor;

    @Convert(converter = TipoDocumentoCompraConverter.class)
    @Column(name = "tipo_documento", nullable = false, length = 20)
    private TipoDocumentoCompra tipoDocumento;

    @Column(length = 10)
    private String serie;

    @Column(length = 20)
    private String numero;

    @Column(name = "fecha_documento", nullable = false)
    private LocalDate fechaDocumento;

    @Column(nullable = false)
    private Long total;

    @Column(name = "ruta_adjunto", length = 500)
    private String rutaAdjunto;

    @Convert(converter = EstadoCompraConverter.class)
    @Column(nullable = false, length = 20)
    private EstadoCompra estado = EstadoCompra.BORRADOR;

    @Column(name = "confirmada_en")
    private OffsetDateTime confirmadaEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "confirmada_por", length = 26)
    private String confirmadaPor;

    @Column(length = 250)
    private String observacion;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "creado_por", nullable = false, length = 26)
    private String creadoPor;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleCompra> detalles = new ArrayList<>();

    protected Compra() {
    }

    public Compra(String id, Local local, Proveedor proveedor, TipoDocumentoCompra tipoDocumento,
                  String serie, String numero, LocalDate fechaDocumento, Long total,
                  String observacion, String creadoPor) {
        this.id = id;
        this.local = local;
        this.proveedor = proveedor;
        this.tipoDocumento = tipoDocumento;
        this.serie = serie;
        this.numero = numero;
        this.fechaDocumento = fechaDocumento;
        this.total = total != null ? total : 0L;
        this.observacion = observacion;
        this.creadoPor = creadoPor;
        this.creadoEn = OffsetDateTime.now();
        this.estado = EstadoCompra.BORRADOR;
    }

    public void agregarDetalle(DetalleCompra detalle) {
        detalles.add(detalle);
        detalle.asignarCompra(this);
    }

    public void limpiarDetalles() {
        detalles.clear();
    }

    public void confirmar(String confirmadoPorUsuarioId) {
        if (this.estado != EstadoCompra.BORRADOR) {
            throw new IllegalStateException("Solo se puede confirmar una compra en estado borrador.");
        }
        if (this.detalles.isEmpty()) {
            throw new IllegalStateException("No se puede confirmar una compra sin detalles.");
        }
        this.estado = EstadoCompra.CONFIRMADA;
        this.confirmadaEn = OffsetDateTime.now();
        this.confirmadaPor = confirmadoPorUsuarioId;
    }

    public void anular() {
        if (this.estado != EstadoCompra.BORRADOR) {
            throw new IllegalStateException("Solo se puede anular una compra en borrador.");
        }
        this.estado = EstadoCompra.ANULADA;
    }

    public void actualizarCabecera(Proveedor proveedor, TipoDocumentoCompra tipoDocumento,
                                   String serie, String numero, LocalDate fechaDocumento,
                                   String observacion) {
        if (this.estado != EstadoCompra.BORRADOR) {
            throw new IllegalStateException("Solo se puede editar una compra en estado borrador.");
        }
        this.proveedor = proveedor;
        this.tipoDocumento = tipoDocumento;
        this.serie = serie;
        this.numero = numero;
        this.fechaDocumento = fechaDocumento;
        this.observacion = observacion;
    }

    public void recalcularTotal() {
        long suma = 0L;
        for (DetalleCompra d : detalles) {
            suma += (d.getSubtotal() != null ? d.getSubtotal() : 0L);
        }
        this.total = suma;
    }

    public void asignarRutaAdjunto(String rutaAdjunto) {
        this.rutaAdjunto = rutaAdjunto;
    }

    // Getters
    public String getId() { return id; }
    public Local getLocal() { return local; }
    public Proveedor getProveedor() { return proveedor; }
    public TipoDocumentoCompra getTipoDocumento() { return tipoDocumento; }
    public String getSerie() { return serie; }
    public String getNumero() { return numero; }
    public LocalDate getFechaDocumento() { return fechaDocumento; }
    public Long getTotal() { return total; }
    public String getRutaAdjunto() { return rutaAdjunto; }
    public EstadoCompra getEstado() { return estado; }
    public OffsetDateTime getConfirmadaEn() { return confirmadaEn; }
    public String getConfirmadaPor() { return confirmadaPor; }
    public String getObservacion() { return observacion; }
    public OffsetDateTime getCreadoEn() { return creadoEn; }
    public String getCreadoPor() { return creadoPor; }
    public List<DetalleCompra> getDetalles() { return detalles; }
}
