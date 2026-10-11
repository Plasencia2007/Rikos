package pe.rikos.operacion.venta.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

/**
 * 8.6: la foto de lo que se mando a cocina, numerada por pedido. Sin estado "listo" (RN-064: cocina avisa de
 * palabra). Las impresiones se cuentan con un UPDATE atomico (ComandaRepositorio), no desde esta entidad, para no
 * perder cuentas cuando dos personas imprimen a la vez.
 */
@Entity
@Table(name = "comanda")
public class Comanda {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pedido_id", nullable = false, length = 26)
    private String pedidoId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "local_id", nullable = false, length = 26)
    private String localId;

    @Column(nullable = false)
    private int numero;

    @Column(name = "emitida_en", nullable = false)
    private OffsetDateTime emitidaEn;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "emitida_por", nullable = false, length = 26)
    private String emitidaPor;

    @Column(nullable = false)
    private int impresiones;

    @Column(name = "ultima_impresion_en")
    private OffsetDateTime ultimaImpresionEn;

    @Column(name = "estado_impresion", nullable = false, length = 10)
    private String estadoImpresion;

    protected Comanda() {
    }

    public Comanda(String id, String pedidoId, String localId, int numero, String emitidaPor) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.localId = localId;
        this.numero = numero;
        this.emitidaEn = OffsetDateTime.now();
        this.emitidaPor = emitidaPor;
        this.impresiones = 0;
        this.estadoImpresion = "pendiente";
    }

    public String getId() {
        return id;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public String getLocalId() {
        return localId;
    }

    public int getNumero() {
        return numero;
    }

    public OffsetDateTime getEmitidaEn() {
        return emitidaEn;
    }

    public String getEmitidaPor() {
        return emitidaPor;
    }

    public int getImpresiones() {
        return impresiones;
    }

    public OffsetDateTime getUltimaImpresionEn() {
        return ultimaImpresionEn;
    }

    public String getEstadoImpresion() {
        return estadoImpresion;
    }
}
