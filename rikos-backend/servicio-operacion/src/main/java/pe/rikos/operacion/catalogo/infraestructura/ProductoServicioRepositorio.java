package pe.rikos.operacion.catalogo.infraestructura;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoServicioRepositorio extends JpaRepository<ProductoServicio, ProductoServicioId> {
    List<ProductoServicio> findByProductoId(String productoId);

    void deleteByProductoId(String productoId);
}
