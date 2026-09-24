package pe.rikos.operacion.catalogo.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** 4.6: RUC opcional -- el mercado de abastos existe, una compra a un
 * proveedor informal es real. No incluye ordenes de compra ni cuentas por
 * pagar (fuera de alcance, US-011). */
@Entity
@Table(name = "proveedor")
public class Proveedor {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(length = 11)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 120)
    private String nombreComercial;

    @Column(length = 20)
    private String telefono;

    @Column(length = 160)
    private String correo;

    @Column(length = 250)
    private String direccion;

    @Column(length = 120)
    private String contacto;

    @Column(nullable = false)
    private boolean activo = true;

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

    protected Proveedor() {
    }

    public Proveedor(String id, String ruc, String razonSocial, String nombreComercial, String telefono,
                      String correo, String direccion, String contacto, String creadoPor) {
        this.id = id;
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.nombreComercial = nombreComercial;
        this.telefono = telefono;
        this.correo = correo;
        this.direccion = direccion;
        this.contacto = contacto;
        this.creadoEn = OffsetDateTime.now();
        this.creadoPor = creadoPor;
    }

    public record DatosActualizacion(String ruc, String razonSocial, String nombreComercial, String telefono,
                                      String correo, String direccion, String contacto) {
    }

    public void actualizar(DatosActualizacion datos, String actualizadoPor) {
        this.ruc = datos.ruc();
        this.razonSocial = datos.razonSocial();
        this.nombreComercial = datos.nombreComercial();
        this.telefono = datos.telefono();
        this.correo = datos.correo();
        this.direccion = datos.direccion();
        this.contacto = datos.contacto();
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public void desactivar(String actualizadoPor) {
        this.activo = false;
        this.actualizadoEn = OffsetDateTime.now();
        this.actualizadoPor = actualizadoPor;
    }

    public String getId() {
        return id;
    }

    public String getRuc() {
        return ruc;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getContacto() {
        return contacto;
    }

    public boolean isActivo() {
        return activo;
    }
}
