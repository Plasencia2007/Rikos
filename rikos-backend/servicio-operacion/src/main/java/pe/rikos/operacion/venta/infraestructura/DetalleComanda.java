package pe.rikos.operacion.venta.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 8.7: una linea de la foto de la comanda. Una linea sale en una sola comanda. */
@Entity
@Table(name = "detalle_comanda")
public class DetalleComanda {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "comanda_id", nullable = false, length = 26)
    private String comandaId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "detalle_pedido_id", nullable = false, length = 26)
    private String detallePedidoId;

    @Column(nullable = false)
    private int cantidad;

    /** La etiqueta de la persona TAL COMO ERA al comandar: si la linea cambia de persona, la comanda no cambia. */
    @Column(name = "etiqueta_persona", nullable = false, length = 120)
    private String etiquetaPersona;

    protected DetalleComanda() {
    }

    public DetalleComanda(String id, String comandaId, String detallePedidoId, int cantidad, String etiquetaPersona) {
        this.etiquetaPersona = etiquetaPersona;
        this.id = id;
        this.comandaId = comandaId;
        this.detallePedidoId = detallePedidoId;
        this.cantidad = cantidad;
    }

    public String getId() {
        return id;
    }

    public String getComandaId() {
        return comandaId;
    }

    public String getDetallePedidoId() {
        return detallePedidoId;
    }

    public String getEtiquetaPersona() {
        return etiquetaPersona;
    }

    public int getCantidad() {
        return cantidad;
    }
}
