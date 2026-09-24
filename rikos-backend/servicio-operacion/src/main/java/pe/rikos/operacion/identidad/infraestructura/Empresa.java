package pe.rikos.operacion.identidad.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "empresa")
public class Empresa {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(nullable = false, length = 11)
    private String ruc;

    @Column(name = "razon_social", nullable = false, length = 200)
    private String razonSocial;

    @Column(name = "nombre_comercial", length = 120)
    private String nombreComercial;

    @Column(name = "domicilio_fiscal", nullable = false, length = 250)
    private String domicilioFiscal;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime creadoEn;

    @Column(name = "actualizado_en")
    private OffsetDateTime actualizadoEn;

    protected Empresa() {
    }

    public Empresa(String id, String ruc, String razonSocial, String nombreComercial, String domicilioFiscal) {
        this.id = id;
        this.ruc = ruc;
        this.razonSocial = razonSocial;
        this.nombreComercial = nombreComercial;
        this.domicilioFiscal = domicilioFiscal;
        this.creadoEn = OffsetDateTime.now();
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

    public String getDomicilioFiscal() {
        return domicilioFiscal;
    }
}
