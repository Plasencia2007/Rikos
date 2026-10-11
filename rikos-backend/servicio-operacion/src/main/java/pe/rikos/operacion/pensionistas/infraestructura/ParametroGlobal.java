package pe.rikos.operacion.pensionistas.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/** Parametro editable (taper, regla del segundo consumo). Los siembra V26; la aplicacion nunca los crea ni los borra. */
@Entity
@Table(name = "parametro_global")
public class ParametroGlobal {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @Column(nullable = false, length = 60)
    private String clave;

    @Column(length = 200)
    private String valor;

    @Column(name = "cambiado_en")
    private OffsetDateTime cambiadoEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "cambiado_por", length = 26)
    private String cambiadoPor;

    protected ParametroGlobal() {
    }

    public void cambiar(String valor, String actorId) {
        this.valor = valor;
        this.cambiadoEn = OffsetDateTime.now();
        this.cambiadoPor = actorId;
    }

    public String getId() {
        return id;
    }

    public String getClave() {
        return clave;
    }

    public String getValor() {
        return valor;
    }

    public OffsetDateTime getCambiadoEn() {
        return cambiadoEn;
    }

    public String getCambiadoPor() {
        return cambiadoPor;
    }
}
