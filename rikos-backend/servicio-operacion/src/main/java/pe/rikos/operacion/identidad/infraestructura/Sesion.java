package pe.rikos.operacion.identidad.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "sesion")
public class Sesion {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    // Sin FK objeto a Usuario/Local a proposito: sesion admite usuario_id
    // nulo (intento con usuario inexistente, 3.10) y no necesita navegar
    // la relacion, solo dejar constancia.
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "usuario_id", length = 26)
    private String usuarioId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", length = 26)
    private String localId;

    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(nullable = false, length = 20)
    private String resultado;

    // 255, no 120: el valor real es el User-Agent del navegador (ver V10).
    @Column(length = 255)
    private String dispositivo;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(name = "iniciada_en", nullable = false)
    private OffsetDateTime iniciadaEn;

    @Column(name = "finalizada_en")
    private OffsetDateTime finalizadaEn;

    protected Sesion() {
    }

    public Sesion(String id, String usuarioId, String localId, String tipo, String resultado,
                  String dispositivo, String direccionIp) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.localId = localId;
        this.tipo = tipo;
        this.resultado = resultado;
        this.dispositivo = dispositivo;
        this.direccionIp = direccionIp;
        this.iniciadaEn = OffsetDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public String getResultado() {
        return resultado;
    }

    public OffsetDateTime getIniciadaEn() {
        return iniciadaEn;
    }

    public String getDireccionIp() {
        return direccionIp;
    }
}
