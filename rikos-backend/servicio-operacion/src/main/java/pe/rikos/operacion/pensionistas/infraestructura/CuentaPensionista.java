package pe.rikos.operacion.pensionistas.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 7.5 (RN-002, caso critico 20): una cuenta por pensionista, valida en todos
 * los locales. No se cierra ni se recrea al desactivarlo (RN-034). El libro de
 * movimientos llega en la entrega 4.
 */
@Entity
@Table(name = "cuenta_pensionista")
public class CuentaPensionista {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pensionista_id", nullable = false, length = 26)
    private String pensionistaId;

    @Column(name = "abierta_en", nullable = false)
    private OffsetDateTime abiertaEn;

    @Column(nullable = false, length = 12)
    private String estado = "abierta";

    protected CuentaPensionista() {
    }

    public CuentaPensionista(String id, String pensionistaId) {
        this.id = id;
        this.pensionistaId = pensionistaId;
        this.abiertaEn = OffsetDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getPensionistaId() {
        return pensionistaId;
    }

    public OffsetDateTime getAbiertaEn() {
        return abiertaEn;
    }

    public String getEstado() {
        return estado;
    }
}
