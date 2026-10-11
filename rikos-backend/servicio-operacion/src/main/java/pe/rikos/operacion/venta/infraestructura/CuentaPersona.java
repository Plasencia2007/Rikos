package pe.rikos.operacion.venta.infraestructura;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 8.3: una persona de la mesa. Aqui se separan los consumos de una mesa mixta desde la toma del pedido
 * (RN-060). Sin subtotal ni total guardados: se derivan de sus lineas.
 */
@Entity
@Table(name = "cuenta_persona")
public class CuentaPersona {

    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 26)
    private String id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pedido_id", nullable = false, length = 26)
    private String pedidoId;

    @Column(nullable = false, length = 120)
    private String etiqueta;

    @Column(nullable = false)
    private int orden;

    @Column(name = "tipo_cliente", nullable = false, length = 12)
    private String tipoCliente;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "pensionista_id", length = 26)
    private String pensionistaId;

    @Column(nullable = false, length = 10)
    private String estado;

    protected CuentaPersona() {
    }

    /** Un cliente normal. */
    public CuentaPersona(String id, String pedidoId, String etiqueta, int orden) {
        this.id = id;
        this.pedidoId = pedidoId;
        this.etiqueta = etiqueta;
        this.orden = orden;
        this.tipoCliente = "regular";
        this.estado = "abierta";
    }

    /** Pasa a ser un pensionista; la etiqueta pasa a ser su nombre. */
    public void asignarPensionista(String pensionistaId, String etiqueta) {
        this.tipoCliente = "pensionista";
        this.pensionistaId = pensionistaId;
        this.etiqueta = etiqueta;
    }

    public String getId() {
        return id;
    }

    public String getPedidoId() {
        return pedidoId;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public int getOrden() {
        return orden;
    }

    public String getTipoCliente() {
        return tipoCliente;
    }

    public String getPensionistaId() {
        return pensionistaId;
    }

    public String getEstado() {
        return estado;
    }
}
