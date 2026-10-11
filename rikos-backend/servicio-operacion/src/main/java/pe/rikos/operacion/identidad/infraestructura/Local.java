package pe.rikos.operacion.identidad.infraestructura;

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
import java.time.OffsetDateTime;

@Entity
@Table(name = "local")
public class Local {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.CHAR)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 20)
    private String codigo;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 250)
    private String direccion;

    @Column(length = 20)
    private String telefono;

    @Column(name = "serie_boleta", length = 4)
    private String serieBoleta;

    @Column(name = "serie_factura", length = 4)
    private String serieFactura;

    @Column(length = 160)
    private String correo;

    // 3.2: informativas hoy, el delivery es gratuito y sin zonas (RN-081) y
    // ningun calculo las usa todavia.
    @Column(precision = 10, scale = 7)
    private BigDecimal latitud;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitud;

    @Column(name = "facebook_url", length = 255)
    private String facebookUrl;

    @Column(name = "instagram_url", length = 255)
    private String instagramUrl;

    @Column(name = "tiktok_url", length = 255)
    private String tiktokUrl;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "color_principal", length = 7)
    private String colorPrincipal;

    @Column(name = "portada_url", length = 500)
    private String portadaUrl;

    @Column(name = "delivery_habilitado", nullable = false)
    private boolean deliveryHabilitado = true;

    @Column(name = "recojo_habilitado", nullable = false)
    private boolean recojoHabilitado = true;

    @Column(name = "tiempo_min_entrega_min")
    private Integer tiempoMinEntregaMin;

    @Column(name = "tiempo_max_entrega_min")
    private Integer tiempoMaxEntregaMin;

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

    protected Local() {
    }

    public Local(String id, Empresa empresa, String codigo, String nombre, String direccion, String creadoPor) {
        this.id = id;
        this.empresa = empresa;
        this.codigo = codigo;
        this.nombre = nombre;
        this.direccion = direccion;
        this.activo = true;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    /** Agrupa los campos editables: la firma individual ya pasaba de diez parametros. */
    public record DatosActualizacion(
            String nombre, String direccion, String telefono, String correo,
            BigDecimal latitud, BigDecimal longitud,
            String facebookUrl, String instagramUrl, String tiktokUrl, String logoUrl,
            String colorPrincipal, String portadaUrl,
            boolean deliveryHabilitado, boolean recojoHabilitado,
            Integer tiempoMinEntregaMin, Integer tiempoMaxEntregaMin) {
    }

    public void actualizar(DatosActualizacion datos, String actualizadoPor) {
        this.nombre = datos.nombre();
        this.direccion = datos.direccion();
        this.telefono = datos.telefono();
        this.correo = datos.correo();
        this.latitud = datos.latitud();
        this.longitud = datos.longitud();
        this.facebookUrl = datos.facebookUrl();
        this.instagramUrl = datos.instagramUrl();
        this.tiktokUrl = datos.tiktokUrl();
        this.logoUrl = datos.logoUrl();
        this.colorPrincipal = datos.colorPrincipal();
        this.portadaUrl = datos.portadaUrl();
        this.deliveryHabilitado = datos.deliveryHabilitado();
        this.recojoHabilitado = datos.recojoHabilitado();
        this.tiempoMinEntregaMin = datos.tiempoMinEntregaMin();
        this.tiempoMaxEntregaMin = datos.tiempoMaxEntregaMin();
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public void desactivar(String actualizadoPor) {
        this.activo = false;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public void activar(String actualizadoPor) {
        this.activo = true;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public String getId() {
        return id;
    }

    public Empresa getEmpresa() {
        return empresa;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public String getFacebookUrl() {
        return facebookUrl;
    }

    public String getInstagramUrl() {
        return instagramUrl;
    }

    public String getTiktokUrl() {
        return tiktokUrl;
    }

    public String getColorPrincipal() {
        return colorPrincipal;
    }

    public String getPortadaUrl() {
        return portadaUrl;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public boolean isDeliveryHabilitado() {
        return deliveryHabilitado;
    }

    public boolean isRecojoHabilitado() {
        return recojoHabilitado;
    }

    public Integer getTiempoMinEntregaMin() {
        return tiempoMinEntregaMin;
    }

    public Integer getTiempoMaxEntregaMin() {
        return tiempoMaxEntregaMin;
    }

    public boolean isActivo() {
        return activo;
    }

    public int getVersion() {
        return version;
    }
}
