package pe.rikos.operacion.inventario.infraestructura;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clave compuesta para existencia por local y producto (RN-002, US-043 CA1).
 */
public class ExistenciaId implements Serializable {

    private String localId;
    private String productoId;

    public ExistenciaId() {
    }

    public ExistenciaId(String localId, String productoId) {
        this.localId = localId;
        this.productoId = productoId;
    }

    public String getLocalId() {
        return localId;
    }

    public String getProductoId() {
        return productoId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExistenciaId that)) return false;
        return Objects.equals(localId, that.localId) && Objects.equals(productoId, that.productoId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localId, productoId);
    }
}
