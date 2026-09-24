package pe.rikos.operacion.catalogo.infraestructura;

import java.io.Serializable;
import java.util.Objects;

public class ProductoServicioId implements Serializable {
    private String productoId;
    private String servicio;

    public ProductoServicioId() {
    }

    public ProductoServicioId(String productoId, String servicio) {
        this.productoId = productoId;
        this.servicio = servicio;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProductoServicioId that)) return false;
        return Objects.equals(productoId, that.productoId) && Objects.equals(servicio, that.servicio);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productoId, servicio);
    }
}
