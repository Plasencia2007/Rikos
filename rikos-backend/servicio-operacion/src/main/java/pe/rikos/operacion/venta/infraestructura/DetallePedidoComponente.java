package pe.rikos.operacion.venta.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 8.5: que opcion eligio el comensal para cada componente del menu. Con una sola opcion se asigna sola, pero la
 * fila se guarda igual (RN-038): el registro historico de que comio no depende de si hubo o no eleccion.
 */
@Entity
@Table(name = "detalle_pedido_componente")
public class DetallePedidoComponente {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "detalle_pedido_id", nullable = false, length = 26)
    private String detallePedidoId;

    @Column(nullable = false, length = 20)
    private String componente;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "producto_id", nullable = false, length = 26)
    private String productoId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "opcion_menu_id", length = 26)
    private String opcionMenuId;

    protected DetallePedidoComponente() {
    }

    public DetallePedidoComponente(String id, String detallePedidoId, String componente, String productoId,
                                   String opcionMenuId) {
        this.id = id;
        this.detallePedidoId = detallePedidoId;
        this.componente = componente;
        this.productoId = productoId;
        this.opcionMenuId = opcionMenuId;
    }

    public String getId() {
        return id;
    }

    public String getDetallePedidoId() {
        return detallePedidoId;
    }

    public String getComponente() {
        return componente;
    }

    public String getProductoId() {
        return productoId;
    }

    public String getOpcionMenuId() {
        return opcionMenuId;
    }
}
