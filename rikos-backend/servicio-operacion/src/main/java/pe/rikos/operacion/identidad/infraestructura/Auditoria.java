package pe.rikos.operacion.identidad.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 13.1: registro de por que el sistema esta como esta. Nunca se modifica
 * ni se borra -- por eso no hay metodos de escritura mas alla del
 * constructor, solo getters.
 */
@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", length = 26)
    private String localId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "usuario_id", nullable = false, length = 26)
    private String usuarioId;

    @Column(nullable = false, length = 30)
    private String evento;

    @Column(nullable = false, length = 60)
    private String entidad;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "entidad_id", nullable = false, length = 26)
    private String entidadId;

    // Cada evento afecta a una entidad distinta y no hay una forma comun de
    // representarla -- se guardan ya serializados como texto JSON y solo se
    // muestran, nunca se consultan por su contenido (13.1).
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_anterior")
    private String valorAnterior;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_nuevo")
    private String valorNuevo;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "motivo_id", length = 26)
    private String motivoId;

    @Column(length = 500)
    private String observacion;

    @Column(name = "ocurrido_en", nullable = false)
    private OffsetDateTime ocurridoEn;

    protected Auditoria() {
    }

    public Auditoria(String id, String localId, String usuarioId, String evento, String entidad, String entidadId,
                      String valorAnterior, String valorNuevo, String motivoId, String observacion) {
        this.id = id;
        this.localId = localId;
        this.usuarioId = usuarioId;
        this.evento = evento;
        this.entidad = entidad;
        this.entidadId = entidadId;
        this.valorAnterior = valorAnterior;
        this.valorNuevo = valorNuevo;
        this.motivoId = motivoId;
        this.observacion = observacion;
        this.ocurridoEn = OffsetDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public String getEvento() {
        return evento;
    }

    public String getEntidad() {
        return entidad;
    }

    public String getEntidadId() {
        return entidadId;
    }

    public String getValorAnterior() {
        return valorAnterior;
    }

    public String getValorNuevo() {
        return valorNuevo;
    }

    public OffsetDateTime getOcurridoEn() {
        return ocurridoEn;
    }
}
